import type {
  PurchaseOrder,
  PurchaseOrderItem,
  SqlExecutor,
  Supplier,
} from '@/db/types';
import { roundMoney } from '@/domain/money';

export class PurchasingRepository {
  constructor(private readonly db: SqlExecutor) {}

  async getSuppliers(): Promise<Supplier[]> {
    return this.db.getAllAsync<Supplier>('SELECT * FROM suppliers ORDER BY name ASC;');
  }

  async saveSupplier(supplier: Omit<Supplier, 'id'> & { readonly id?: number }): Promise<number> {
    if (supplier.id && supplier.id > 0) {
      await this.db.runAsync(
        `UPDATE suppliers SET
          name = ?, contactPerson = ?, phone = ?, email = ?, address = ?,
          deliverySchedule = ?, paymentTerms = ?, notes = ?
        WHERE id = ?;`,
        [
          supplier.name,
          supplier.contactPerson,
          supplier.phone,
          supplier.email,
          supplier.address,
          supplier.deliverySchedule,
          supplier.paymentTerms,
          supplier.notes,
          supplier.id,
        ],
      );
      return supplier.id;
    }
    const res = await this.db.runAsync(
      `INSERT INTO suppliers (
        name, contactPerson, phone, email, address, deliverySchedule, paymentTerms, notes
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?);`,
      [
        supplier.name,
        supplier.contactPerson,
        supplier.phone,
        supplier.email,
        supplier.address,
        supplier.deliverySchedule,
        supplier.paymentTerms,
        supplier.notes,
      ],
    );
    return res.lastInsertRowId;
  }

  async getPurchaseOrders(): Promise<PurchaseOrder[]> {
    return this.db.getAllAsync<PurchaseOrder>(
      'SELECT * FROM purchase_orders ORDER BY orderDate DESC;',
    );
  }

  async getPurchaseOrderItems(purchaseOrderId: number): Promise<PurchaseOrderItem[]> {
    return this.db.getAllAsync<PurchaseOrderItem>(
      'SELECT * FROM purchase_order_items WHERE purchaseOrderId = ?;',
      [purchaseOrderId],
    );
  }

  async createPurchaseOrder(
    order: Omit<PurchaseOrder, 'id'>,
    items: readonly Omit<PurchaseOrderItem, 'id' | 'purchaseOrderId'>[],
  ): Promise<number> {
    return this.db.withTransactionAsync(async () => {
      const res = await this.db.runAsync(
        `INSERT INTO purchase_orders (
          poNumber, supplierId, supplierName, orderDate, expectedDeliveryDate,
          receivedDate, totalCost, status, paymentStatus, notes
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);`,
        [
          order.poNumber,
          order.supplierId,
          order.supplierName,
          order.orderDate,
          order.expectedDeliveryDate,
          order.receivedDate,
          roundMoney(order.totalCost),
          order.status,
          order.paymentStatus,
          order.notes,
        ],
      );
      const poId = res.lastInsertRowId;
      for (const item of items) {
        await this.db.runAsync(
          `INSERT INTO purchase_order_items (
            purchaseOrderId, productId, productName, quantityOrdered,
            quantityReceived, unitCost, totalCost, uomName
          ) VALUES (?, ?, ?, ?, ?, ?, ?, ?);`,
          [
            poId,
            item.productId,
            item.productName,
            item.quantityOrdered,
            item.quantityReceived,
            roundMoney(item.unitCost),
            roundMoney(item.totalCost),
            item.uomName,
          ],
        );
      }
      return poId;
    });
  }

  async receivePurchaseOrder(
    poId: number,
    actorUsername: string,
    markPaid: boolean,
  ): Promise<void> {
    await this.db.withTransactionAsync(async () => {
      const items = await this.getPurchaseOrderItems(poId);
      const now = Date.now();
      for (const item of items) {
        const live = await this.db.getFirstAsync<{ stockCount: number }>(
          'SELECT stockCount FROM products WHERE id = ?;',
          [item.productId],
        );
        if (live) {
          const stockAfter = live.stockCount + item.quantityOrdered;
          await this.db.runAsync(
            'UPDATE products SET stockCount = ?, cost = ? WHERE id = ?;',
            [stockAfter, roundMoney(item.unitCost), item.productId],
          );
          await this.db.runAsync(
            'UPDATE purchase_order_items SET quantityReceived = ? WHERE id = ?;',
            [item.quantityOrdered, item.id],
          );
          await this.db.runAsync(
            `INSERT INTO inventory_movements (
              productId, productName, timestamp, actorUsername, movementType,
              quantityDelta, stockBefore, stockAfter, reference, notes
            ) VALUES (?, ?, ?, ?, 'PO_RECEIPT', ?, ?, ?, ?, '');`,
            [
              item.productId,
              item.productName,
              now,
              actorUsername,
              item.quantityOrdered,
              live.stockCount,
              stockAfter,
              `PO-${poId}`,
            ],
          );
        }
      }
      await this.db.runAsync(
        `UPDATE purchase_orders SET status = 'RECEIVED', receivedDate = ?, paymentStatus = ? WHERE id = ?;`,
        [now, markPaid ? 'PAID' : 'UNPAID', poId],
      );
    });
  }
}
