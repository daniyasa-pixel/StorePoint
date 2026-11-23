import type { SqlExecutor, StoreConfig, WalletLedgerEntry } from '@/db/types';
import { roundMoney, type WalletType } from '@/domain/money';

interface StoreConfigRow {
  id: number;
  storeName: string;
  currencySymbol: string;
  taxPercentage: number;
  setupCompleted: number;
  gcashBalance: number;
  hasGCash: number;
  hasMaya: number;
  hasLoad: number;
  smartLoadBalance: number;
  globeLoadBalance: number;
  loadServiceFee: number;
  gcashServiceFee: number;
  mayaServiceFee: number;
  mayaBankFee: number;
}

function mapStoreConfig(row: StoreConfigRow): StoreConfig {
  return {
    id: row.id,
    storeName: row.storeName,
    currencySymbol: row.currencySymbol,
    taxPercentage: row.taxPercentage,
    setupCompleted: row.setupCompleted === 1,
    gcashBalance: row.gcashBalance,
    hasGCash: row.hasGCash === 1,
    hasMaya: row.hasMaya === 1,
    hasLoad: row.hasLoad === 1,
    smartLoadBalance: row.smartLoadBalance,
    globeLoadBalance: row.globeLoadBalance,
    loadServiceFee: row.loadServiceFee,
    gcashServiceFee: row.gcashServiceFee,
    mayaServiceFee: row.mayaServiceFee,
    mayaBankFee: row.mayaBankFee,
  };
}

export class StoreConfigRepository {
  constructor(private readonly db: SqlExecutor) {}

  async getConfig(): Promise<StoreConfig | null> {
    const row = await this.db.getFirstAsync<StoreConfigRow>(
      'SELECT * FROM store_config WHERE id = 1 LIMIT 1;',
    );
    return row ? mapStoreConfig(row) : null;
  }

  async saveConfig(config: StoreConfig): Promise<void> {
    await this.db.runAsync(
      `INSERT OR REPLACE INTO store_config (
        id, storeName, currencySymbol, taxPercentage, setupCompleted,
        gcashBalance, hasGCash, hasMaya, hasLoad,
        smartLoadBalance, globeLoadBalance,
        loadServiceFee, gcashServiceFee, mayaServiceFee, mayaBankFee
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);`,
      [
        config.id,
        config.storeName,
        config.currencySymbol,
        config.taxPercentage,
        config.setupCompleted ? 1 : 0,
        roundMoney(config.gcashBalance),
        config.hasGCash ? 1 : 0,
        config.hasMaya ? 1 : 0,
        config.hasLoad ? 1 : 0,
        roundMoney(config.smartLoadBalance),
        roundMoney(config.globeLoadBalance),
        roundMoney(config.loadServiceFee),
        roundMoney(config.gcashServiceFee),
        roundMoney(config.mayaServiceFee),
        roundMoney(config.mayaBankFee),
      ],
    );
  }

  /**
   * Atomically mutates a wallet balance and appends a WalletLedgerEntry.
   * Rejects negative balances on consumption (overdraft guard).
   */
  async applyWalletDelta(params: {
    readonly walletType: WalletType;
    readonly delta: number;
    readonly actorUsername: string;
    readonly entryType: string;
    readonly reference?: string;
    readonly notes?: string;
  }): Promise<WalletLedgerEntry> {
    return this.db.withTransactionAsync(async () => {
      const config = await this.getConfig();
      if (!config) {
        throw new Error('Store configuration not initialized.');
      }
      const current =
        params.walletType === 'GCASH'
          ? config.gcashBalance
          : params.walletType === 'SMART'
            ? config.smartLoadBalance
            : config.globeLoadBalance;

      const nextBalance = roundMoney(current + params.delta);
      if (nextBalance < 0) {
        throw new Error(`Insufficient ${params.walletType} wallet balance.`);
      }

      const column =
        params.walletType === 'GCASH'
          ? 'gcashBalance'
          : params.walletType === 'SMART'
            ? 'smartLoadBalance'
            : 'globeLoadBalance';

      await this.db.runAsync(`UPDATE store_config SET ${column} = ? WHERE id = 1;`, [nextBalance]);

      const timestamp = Date.now();
      const res = await this.db.runAsync(
        `INSERT INTO wallet_ledger (
          walletType, timestamp, actorUsername, type, delta, balanceAfter, reference, notes
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?);`,
        [
          params.walletType,
          timestamp,
          params.actorUsername,
          params.entryType,
          roundMoney(params.delta),
          nextBalance,
          params.reference ?? '',
          params.notes ?? '',
        ],
      );

      return {
        id: res.lastInsertRowId,
        walletType: params.walletType,
        timestamp,
        actorUsername: params.actorUsername,
        type: params.entryType,
        delta: roundMoney(params.delta),
        balanceAfter: nextBalance,
        reference: params.reference ?? '',
        notes: params.notes ?? '',
      };
    });
  }

  async getWalletLedger(limit = 100): Promise<WalletLedgerEntry[]> {
    return this.db.getAllAsync<WalletLedgerEntry>(
      'SELECT * FROM wallet_ledger ORDER BY timestamp DESC LIMIT ?;',
      [limit],
    );
  }
}
