import type { ReturnItem, ReturnTransaction, SqlExecutor } from '@/db/types';
import { roundMoney } from '@/domain/money';

export interface ProcessReturnInput {
  readonly originalTransactionId: number;
  readonly cashierUsername: string;
  readonly refundMethod: string;
  readonly returnReason: string;
  readonly customerName?: string;
  readonly notes?: string;
  readonly items: readonly Omit<ReturnItem, 'id' | 'returnTransactionId'>[];
}

export class ReturnRepository {
  constructor(private readonly db: SqlExecutor) {}

  async processReturn(input: ProcessReturnInput): Promise<ReturnTransaction> {
    if (input.items.length === 0) {
      throw new Error('Select at least one item to return.');
    }

    const totalRefundAmount = roundMoney(
      input.items.reduce((sum, item) => sum + item.refundAmount, 0),
    );
    const timestamp = Date.now();

    return this.db.withTransactionAsync(async () => {
      const res = await this.db.runAsync(
        `INSERT INTO return_transactions (
          originalTransactionId, timestamp, cashierUsername, totalRefundAmount,
          refundMethod, returnReason, customerName, notes
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?);`,
        [
          input.originalTransactionId,
          timestamp,
          input.cashierUsername,
          totalRefundAmount,
          input.refundMethod,
          input.returnReason,
          input.customerName ?? '',
          input.notes ?? '',
        ],
      );
      const returnTransactionId = res.lastInsertRowId;

      for (const item of input.items) {
        await this.db.runAsync(
          `INSERT INTO return_items (
            returnTransactionId, productId, productName, quantity, unitPrice,
            refundAmount, uomName, restockToInventory
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?);`,
          [
            returnTransactionId,
            item.productId,
            item.productName,
            item.quantity,
            roundMoney(item.unitPrice),
            roundMoney(item.refundAmount),
            item.uomName,
            item.restockToInventory ? 1 : 0,
          ],
        );

        if (item.restockToInventory && item.quantity > 0) {
          const live = await this.db.getFirstAsync<{ stockCount: number }>(
            'SELECT stockCount FROM products WHERE id = ?;',
            [item.productId],
          );
          if (live) {
            const stockAfter = live.stockCount + item.quantity;
            await this.db.runAsync('UPDATE products SET stockCount = ? WHERE id = ?;', [
              stockAfter,
              item.productId,
            ]);
            await this.db.runAsync(
              `INSERT INTO inventory_movements (
                productId, productName, timestamp, actorUsername, movementType,
                quantityDelta, stockBefore, stockAfter, reference, notes
              ) VALUES (?, ?, ?, ?, 'RETURN_RESTOCK', ?, ?, ?, ?, ?);`,
              [
                item.productId,
                item.productName,
                timestamp,
                input.cashierUsername,
                item.quantity,
                live.stockCount,
                stockAfter,
                `RET-${returnTransactionId}`,
                input.returnReason,
              ],
            );
          }
        }
      }

      await this.db.runAsync(
        `UPDATE transactions SET status = 'PARTIALLY_REFUNDED' WHERE id = ? AND status = 'COMPLETED';`,
        [input.originalTransactionId],
      );

      return {
        id: returnTransactionId,
        originalTransactionId: input.originalTransactionId,
        timestamp,
        cashierUsername: input.cashierUsername,
        totalRefundAmount,
        refundMethod: input.refundMethod,
        returnReason: input.returnReason,
        customerName: input.customerName ?? '',
        notes: input.notes ?? '',
      };
    });
  }

  async getReturnHistory(limit = 50): Promise<ReturnTransaction[]> {
    return this.db.getAllAsync<ReturnTransaction>(
      'SELECT * FROM return_transactions ORDER BY timestamp DESC LIMIT ?;',
      [limit],
    );
  }
}
