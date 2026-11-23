import * as SQLite from 'expo-sqlite';
import { SCHEMA_V19_STATEMENTS, SCHEMA_VERSION } from './schemaV19';
import type { SqlBindValue, SqlExecutor, SqlRunResult } from './types';

const DB_NAME = 'storepoint_pos_db.db';

export interface DatabaseHealthReport {
  readonly isConnected: boolean;
  readonly isWritable: boolean;
  readonly integrityOk: boolean;
  readonly latencyMs: number;
  readonly schemaVersion: number;
  readonly message: string;
}

class ExpoSqliteExecutor implements SqlExecutor {
  constructor(private readonly db: SQLite.SQLiteDatabase) {}

  async execAsync(sql: string): Promise<void> {
    await this.db.execAsync(sql);
  }

  async runAsync(sql: string, params: readonly SqlBindValue[] = []): Promise<SqlRunResult> {
    const res = await this.db.runAsync(sql, ...params);
    return {
      lastInsertRowId: res.lastInsertRowId,
      changes: res.changes,
    };
  }

  async getFirstAsync<T>(sql: string, params: readonly SqlBindValue[] = []): Promise<T | null> {
    return this.db.getFirstAsync<T>(sql, ...params);
  }

  async getAllAsync<T>(sql: string, params: readonly SqlBindValue[] = []): Promise<T[]> {
    return this.db.getAllAsync<T>(sql, ...params);
  }

  async withTransactionAsync<T>(task: () => Promise<T>): Promise<T> {
    let result!: T;
    await this.db.withExclusiveTransactionAsync(async () => {
      result = await task();
    });
    return result;
  }
}

let cachedExecutor: SqlExecutor | null = null;
let initPromise: Promise<SqlExecutor> | null = null;

async function configurePragmas(executor: SqlExecutor): Promise<void> {
  await executor.execAsync(`
    PRAGMA journal_mode = WAL;
    PRAGMA foreign_keys = ON;
    PRAGMA temp_store = MEMORY;
    PRAGMA synchronous = NORMAL;
    PRAGMA busy_timeout = 5000;
    PRAGMA wal_autocheckpoint = 1000;
    PRAGMA cache_size = -16384;
  `);
}

async function applySchemaIfNeeded(executor: SqlExecutor): Promise<void> {
  const versionRow = await executor.getFirstAsync<{ user_version: number }>('PRAGMA user_version;');
  const currentVersion = versionRow?.user_version ?? 0;

  if (currentVersion > SCHEMA_VERSION) {
    throw new Error(
      `Database schema version ${currentVersion} is newer than supported v${SCHEMA_VERSION}. Fail-closed to protect POS data.`,
    );
  }

  if (currentVersion === 0) {
    await executor.withTransactionAsync(async () => {
      for (const stmt of SCHEMA_V19_STATEMENTS) {
        await executor.execAsync(stmt);
      }
      await executor.execAsync(`PRAGMA user_version = ${SCHEMA_VERSION};`);
    });
  }
}

export async function getDatabase(): Promise<SqlExecutor> {
  if (cachedExecutor) {
    return cachedExecutor;
  }
  if (!initPromise) {
    initPromise = (async () => {
      const db = await SQLite.openDatabaseAsync(DB_NAME);
      const executor = new ExpoSqliteExecutor(db);
      await configurePragmas(executor);
      await applySchemaIfNeeded(executor);
      cachedExecutor = executor;
      return executor;
    })();
  }
  return initPromise;
}

export async function checkDatabaseHealth(): Promise<DatabaseHealthReport> {
  const start = Date.now();
  try {
    const db = await getDatabase();
    const probe = await db.getFirstAsync<{ ok: number }>('SELECT 1 AS ok;');
    const check = await db.getFirstAsync<{ quick_check: string }>('PRAGMA quick_check(1);');
    const ver = await db.getFirstAsync<{ user_version: number }>('PRAGMA user_version;');
    const latencyMs = Date.now() - start;
    const integrityOk = probe?.ok === 1 && check?.quick_check?.toLowerCase() === 'ok';

    return {
      isConnected: true,
      isWritable: true,
      integrityOk,
      latencyMs,
      schemaVersion: ver?.user_version ?? SCHEMA_VERSION,
      message: integrityOk
        ? `Database connection active, verified (${latencyMs}ms latency)`
        : 'Database responsive with integrity warnings',
    };
  } catch (error) {
    return {
      isConnected: false,
      isWritable: false,
      integrityOk: false,
      latencyMs: Date.now() - start,
      schemaVersion: 0,
      message: `Connection failure: ${error instanceof Error ? error.message : String(error)}`,
    };
  }
}
