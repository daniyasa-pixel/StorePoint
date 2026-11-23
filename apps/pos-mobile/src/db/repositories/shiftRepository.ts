import type {
  CashierSession,
  DrawerTransaction,
  ShiftReconciliation,
  SqlExecutor,
} from '@/db/types';
import { roundMoney } from '@/domain/money';

export class ShiftRepository {
  constructor(private readonly db: SqlExecutor) {}

  async getActiveSession(cashierUsername?: string): Promise<CashierSession | null> {
    if (cashierUsername) {
      return this.db.getFirstAsync<CashierSession>(
        `SELECT * FROM cashier_sessions WHERE status = 'ACTIVE' AND cashierUsername = ? ORDER BY startTime DESC LIMIT 1;`,
        [cashierUsername],
      );
    }
    return this.db.getFirstAsync<CashierSession>(
      `SELECT * FROM cashier_sessions WHERE status = 'ACTIVE' ORDER BY startTime DESC LIMIT 1;`,
    );
  }

  async openShift(cashierUsername: string, startingCash: number): Promise<CashierSession> {
    const startTime = Date.now();
    const res = await this.db.runAsync(
      `INSERT INTO cashier_sessions (
        cashierUsername, startTime, endTime, startingCash, endingCash, status
      ) VALUES (?, ?, NULL, ?, NULL, 'ACTIVE');`,
      [cashierUsername, startTime, roundMoney(startingCash)],
    );
    return {
      id: res.lastInsertRowId,
      cashierUsername,
      startTime,
      endTime: null,
      startingCash: roundMoney(startingCash),
      endingCash: null,
      status: 'ACTIVE',
    };
  }

  async closeShift(sessionId: number, endingCash: number): Promise<void> {
    await this.db.runAsync(
      `UPDATE cashier_sessions SET endTime = ?, endingCash = ?, status = 'CLOSED' WHERE id = ?;`,
      [Date.now(), roundMoney(endingCash), sessionId],
    );
  }

  async recordDrawerTransaction(params: {
    readonly sessionId: number;
    readonly cashierUsername: string;
    readonly amount: number;
    readonly type: string;
    readonly reason: string;
  }): Promise<DrawerTransaction> {
    const timestamp = Date.now();
    const res = await this.db.runAsync(
      `INSERT INTO drawer_transactions (
        sessionId, timestamp, cashierUsername, amount, type, reason
      ) VALUES (?, ?, ?, ?, ?, ?);`,
      [
        params.sessionId,
        timestamp,
        params.cashierUsername,
        roundMoney(params.amount),
        params.type,
        params.reason,
      ],
    );
    return {
      id: res.lastInsertRowId,
      sessionId: params.sessionId,
      timestamp,
      cashierUsername: params.cashierUsername,
      amount: roundMoney(params.amount),
      type: params.type,
      reason: params.reason,
    };
  }

  async getDrawerTransactions(sessionId: number): Promise<DrawerTransaction[]> {
    return this.db.getAllAsync<DrawerTransaction>(
      'SELECT * FROM drawer_transactions WHERE sessionId = ? ORDER BY timestamp DESC;',
      [sessionId],
    );
  }

  async saveShiftReconciliation(rec: Omit<ShiftReconciliation, 'id'>): Promise<number> {
    const res = await this.db.runAsync(
      `INSERT INTO shift_reconciliations (
        sessionId, cashierUsername, timestamp,
        count1000, count500, count200, count100, count50, count20, countCoins,
        totalCountedCash, expectedCash, cashVariance,
        countedGCash, expectedGCash, gcashVariance,
        countedSmartLoad, expectedSmartLoad, smartLoadVariance,
        countedGlobeLoad, expectedGlobeLoad, globeLoadVariance,
        hasDiscrepancy, managerOverrideApproved, managerUsername, discrepancyReason, receiptPrinted
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);`,
      [
        rec.sessionId,
        rec.cashierUsername,
        rec.timestamp,
        rec.count1000,
        rec.count500,
        rec.count200,
        rec.count100,
        rec.count50,
        rec.count20,
        roundMoney(rec.countCoins),
        roundMoney(rec.totalCountedCash),
        roundMoney(rec.expectedCash),
        roundMoney(rec.cashVariance),
        roundMoney(rec.countedGCash),
        roundMoney(rec.expectedGCash),
        roundMoney(rec.gcashVariance),
        roundMoney(rec.countedSmartLoad),
        roundMoney(rec.expectedSmartLoad),
        roundMoney(rec.smartLoadVariance),
        roundMoney(rec.countedGlobeLoad),
        roundMoney(rec.expectedGlobeLoad),
        roundMoney(rec.globeLoadVariance),
        rec.hasDiscrepancy ? 1 : 0,
        rec.managerOverrideApproved ? 1 : 0,
        rec.managerUsername,
        rec.discrepancyReason,
        rec.receiptPrinted ? 1 : 0,
      ],
    );
    return res.lastInsertRowId;
  }
}
