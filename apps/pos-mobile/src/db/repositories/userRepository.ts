import type { UserRole } from '@/constants/routes';
import type { SqlExecutor, User } from '@/db/types';

interface UserRow {
  username: string;
  pinHash: string;
  role: string;
  barcodeId: string;
  biometricEnrolled: number;
  pinResetRequired: number;
}

function mapUser(row: UserRow): User {
  return {
    username: row.username,
    pinHash: row.pinHash,
    role: row.role as UserRole,
    barcodeId: row.barcodeId,
    biometricEnrolled: row.biometricEnrolled === 1,
    pinResetRequired: row.pinResetRequired === 1,
  };
}

export class UserRepository {
  constructor(private readonly db: SqlExecutor) {}

  async getAllUsers(): Promise<User[]> {
    const rows = await this.db.getAllAsync<UserRow>(
      'SELECT * FROM user_accounts ORDER BY username ASC;',
    );
    return rows.map(mapUser);
  }

  async getByUsername(username: string): Promise<User | null> {
    const row = await this.db.getFirstAsync<UserRow>(
      'SELECT * FROM user_accounts WHERE username = ? LIMIT 1;',
      [username],
    );
    return row ? mapUser(row) : null;
  }

  async getByBarcode(barcodeId: string): Promise<User | null> {
    const row = await this.db.getFirstAsync<UserRow>(
      'SELECT * FROM user_accounts WHERE barcodeId = ? AND barcodeId != "" LIMIT 1;',
      [barcodeId],
    );
    return row ? mapUser(row) : null;
  }

  async getBiometricEnrolledUsers(): Promise<User[]> {
    const rows = await this.db.getAllAsync<UserRow>(
      'SELECT * FROM user_accounts WHERE biometricEnrolled = 1 ORDER BY username ASC;',
    );
    return rows.map(mapUser);
  }

  async saveUser(user: User): Promise<void> {
    await this.db.runAsync(
      `INSERT OR REPLACE INTO user_accounts (
        username, pinHash, role, barcodeId, biometricEnrolled, pinResetRequired
      ) VALUES (?, ?, ?, ?, ?, ?);`,
      [
        user.username,
        user.pinHash,
        user.role,
        user.barcodeId,
        user.biometricEnrolled ? 1 : 0,
        user.pinResetRequired ? 1 : 0,
      ],
    );
  }

  async setBiometricEnrolled(username: string, enrolled: boolean): Promise<void> {
    await this.db.runAsync(
      'UPDATE user_accounts SET biometricEnrolled = ? WHERE username = ?;',
      [enrolled ? 1 : 0, username],
    );
  }

  async updatePinHash(username: string, pinHash: string, clearResetFlag = true): Promise<void> {
    await this.db.runAsync(
      'UPDATE user_accounts SET pinHash = ?, pinResetRequired = ? WHERE username = ?;',
      [pinHash, clearResetFlag ? 0 : 1, username],
    );
  }

  async deleteUser(username: string): Promise<void> {
    await this.db.runAsync('DELETE FROM user_accounts WHERE username = ?;', [username]);
  }
}
