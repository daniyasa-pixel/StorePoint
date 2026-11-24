import type {
  CartItemDetails,
  ParkedTransaction,
  ParkedTransactionItem,
  SqlExecutor,
  Transaction,
  TransactionItem,
} from '@/db/types';
import { roundMoney } from '@/domain/money';
import { computeBaseUnitsNeeded, computeCheckoutTotals } from '@/domain/posCheckoutCoordinator';

export interface CheckoutRequest {
  readonly items: readonly CartItemDetails[];
  readonly cashierUsername: string;
  readonly paymentMethod: string;
  readonly cashPaid: number;
  readonly customerName?: string;
  readonly discountType?: string;
  readonly discountValue?: number;
  readonly vatRate?: number;
}

export interface CheckoutReceipt {
  readonly transaction: Transaction;
  readonly items: readonly TransactionItem[];
}

export class CheckoutRepository {
  constructor(private readonly db: SqlExecutor) {}

  /**
   * Executes an atomic POS checkout matching StorePointRepository.checkout():
   * 1. Validates live stock for every tracked product (no clamping).
   * 2. Rejects underpayment.
   * 3. Inserts transaction header + line items.
   * 4. Decrements product stock by (quantity * uomMultiplier) and logs movements.
   */
  async checkout(req: CheckoutRequest): Promise<CheckoutReceipt> {
    if (req.items.length === 0) {
      throw new Error('Cannot checkout an empty cart.');
    }

    const totals = computeCheckoutTotals(
      req.items,
      req.discountType ?? 'NONE',
      req.discountValue ?? 0,
      true,
      req.vatRate ?? 12.0,
    );

    const cashPaid = roundMoney(req.cashPaid);
    if (cashPaid < totals.netTotal) {
      throw new Error(
        `Insufficient payment: received ${cashPaid.toFixed(2)}, required ${totals.netTotal.toFixed(2)}.`,
      );
    }
    const changeAmount = roundMoney(cashPaid - totals.netTotal);

    return this.db.withTransactionAsync(async () => {
      for (const line of req.items) {
        const live = await this.db.getFirstAsync<{
          id: number;
          name: string;
          stockCount: number;
          trackInventory: number;
        }>('SELECT id, name, stockCount, trackInventory FROM products WHERE id = ? LIMIT 1;', [
          line.product.id,
        ]);

        if (!live) {
          throw new Error(`Product "${line.product.name}" no longer exists.`);
        }
        const unitsNeeded = computeBaseUnitsNeeded(line);
        if (live.trackInventory === 1 && live.stockCount < unitsNeeded) {
          throw new Error(
            `Insufficient stock for "${live.name}": need ${unitsNeeded}, available ${live.stockCount}.`,
          );
        }
      }

      const timestamp = Date.now();
      const txRes = await this.db.runAsync(
        `INSERT INTO transactions (
          timestamp, cashierUsername, subtotal, taxAmount, totalAmount,
          paymentMethod, cashPaid, changeAmount, status, customerName
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'COMPLETED', ?);`,
        [
          timestamp,
          req.cashierUsername,
          totals.subtotal,
          totals.vatAmount,
          totals.netTotal,
          req.paymentMethod,
          cashPaid,
          changeAmount,
          req.customerName ?? '',
        ],
      );
      const transactionId = txRes.lastInsertRowId;

      const savedItems: TransactionItem[] = [];
      for (const line of req.items) {
        const unitsNeeded = computeBaseUnitsNeeded(line);
        const itemRes = await this.db.runAsync(
          `INSERT INTO transaction_items (
            transactionId, productId, productName, price, quantity, cost, uomName
          ) VALUES (?, ?, ?, ?, ?, ?, ?);`,
          [
            transactionId,
            line.product.id,
            line.product.name,
            roundMoney(line.selectedPrice),
            line.quantity,
            roundMoney(line.product.cost),
            line.uomName,
          ],
        );

        savedItems.push({
          id: itemRes.lastInsertRowId,
          transactionId,
          productId: line.product.id,
          productName: line.product.name,
          price: roundMoney(line.selectedPrice),
          quantity: line.quantity,
          cost: roundMoney(line.product.cost),
          uomName: line.uomName,
        });

        if (line.product.trackInventory) {
          const live = await this.db.getFirstAsync<{ stockCount: number }>(
            'SELECT stockCount FROM products WHERE id = ?;',
            [line.product.id],
          );
          const stockBefore = live?.stockCount ?? 0;
          const stockAfter = stockBefore - unitsNeeded;
          await this.db.runAsync('UPDATE products SET stockCount = ? WHERE id = ?;', [
            stockAfter,
            line.product.id,
          ]);
          await this.db.runAsync(
            `INSERT INTO inventory_movements (
              productId, productName, timestamp, actorUsername, movementType,
              quantityDelta, stockBefore, stockAfter, reference, notes
            ) VALUES (?, ?, ?, ?, 'SALE', ?, ?, ?, ?, '');`,
            [
              line.product.id,
              line.product.name,
              timestamp,
              req.cashierUsername,
              -unitsNeeded,
              stockBefore,
              stockAfter,
              `TX-${transactionId}`,
            ],
          );
        }
      }

      const transaction: Transaction = {
        id: transactionId,
        timestamp,
        cashierUsername: req.cashierUsername,
        subtotal: totals.subtotal,
        taxAmount: totals.vatAmount,
        totalAmount: totals.netTotal,
        paymentMethod: req.paymentMethod,
        cashPaid,
        changeAmount,
        status: 'COMPLETED',
        customerName: req.customerName ?? '',
      };

      return { transaction, items: savedItems };
    });
  }

  async getRecentTransactions(limit = 100): Promise<Transaction[]> {
    return this.db.getAllAsync<Transaction>(
      'SELECT * FROM transactions ORDER BY timestamp DESC LIMIT ?;',
      [limit],
    );
  }

  async getTransactionItems(transactionId: number): Promise<TransactionItem[]> {
    return this.db.getAllAsync<TransactionItem>(
      'SELECT * FROM transaction_items WHERE transactionId = ?;',
      [transactionId],
    );
  }

  async parkCart(note: string, items: readonly CartItemDetails[]): Promise<number> {
    return this.db.withTransactionAsync(async () => {
      const res = await this.db.runAsync(
        'INSERT INTO parked_transactions (timestamp, note) VALUES (?, ?);',
        [Date.now(), note.trim() || 'Held Order'],
      );
      const parkedId = res.lastInsertRowId;
      for (const line of items) {
        await this.db.runAsync(
          `INSERT INTO parked_transaction_items (
            parkedTransactionId, productId, productName, price, quantity
          ) VALUES (?, ?, ?, ?, ?);`,
          [
            parkedId,
            line.product.id,
            line.product.name,
            roundMoney(line.selectedPrice),
            line.quantity,
          ],
        );
      }
      return parkedId;
    });
  }

  async getParkedTransactions(): Promise<
    Array<{ readonly header: ParkedTransaction; readonly items: readonly ParkedTransactionItem[] }>
  > {
    const headers = await this.db.getAllAsync<ParkedTransaction>(
      'SELECT * FROM parked_transactions ORDER BY timestamp DESC;',
    );
    if (headers.length === 0) {
      return [];
    }
    const ids = headers.map((h) => h.id);
    const placeholders = ids.map(() => '?').join(', ');
    const allItems = await this.db.getAllAsync<ParkedTransactionItem>(
      `SELECT * FROM parked_transaction_items WHERE parkedTransactionId IN (${placeholders});`,
      ids,
    );
    const itemsByParkedId = new Map<number, ParkedTransactionItem[]>();
    for (const item of allItems) {
      const list = itemsByParkedId.get(item.parkedTransactionId);
      if (list) {
        list.push(item);
      } else {
        itemsByParkedId.set(item.parkedTransactionId, [item]);
      }
    }
    return headers.map((header) => ({
      header,
      items: itemsByParkedId.get(header.id) ?? [],
    }));
  }

  async deleteParkedTransaction(id: number): Promise<void> {
    await this.db.runAsync('DELETE FROM parked_transactions WHERE id = ?;', [id]);
  }
}
