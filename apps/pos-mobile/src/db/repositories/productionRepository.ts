import type { BillOfMaterials, ProductionLogEntity, SqlExecutor } from '@/db/types';
import { computeDerivedUnitCost } from '@/domain/productionBomCoordinator';

export interface RunProductionInput {
  readonly rawMaterialProductId: number;
  readonly targetRetailProductId: number;
  readonly rawQtyUsed: number;
  readonly producedQty: number;
  readonly wasteLossPercentage?: number;
  readonly operatorUsername: string;
  readonly notes?: string;
}

export class ProductionRepository {
  constructor(private readonly db: SqlExecutor) {}

  async getBomRecipes(): Promise<BillOfMaterials[]> {
    return this.db.getAllAsync<BillOfMaterials>('SELECT * FROM bill_of_materials ORDER BY id DESC;');
  }

  async getProductionLogs(limit = 50): Promise<ProductionLogEntity[]> {
    return this.db.getAllAsync<ProductionLogEntity>(
      'SELECT * FROM production_runs ORDER BY timestamp DESC LIMIT ?;',
      [limit],
    );
  }

  async executeProductionRun(input: RunProductionInput): Promise<ProductionLogEntity> {
    return this.db.withTransactionAsync(async () => {
      const raw = await this.db.getFirstAsync<{
        id: number;
        name: string;
        stockCount: number;
        cost: number;
      }>('SELECT id, name, stockCount, cost FROM products WHERE id = ?;', [
        input.rawMaterialProductId,
      ]);
      const target = await this.db.getFirstAsync<{
        id: number;
        name: string;
        stockCount: number;
        price: number;
      }>('SELECT id, name, stockCount, price FROM products WHERE id = ?;', [
        input.targetRetailProductId,
      ]);

      if (!raw || !target) {
        throw new Error('Raw material or target retail product not found.');
      }
      const rawDeduct = Math.ceil(input.rawQtyUsed);
      if (raw.stockCount < rawDeduct) {
        throw new Error(
          `Insufficient raw material "${raw.name}": need ${rawDeduct}, available ${raw.stockCount}.`,
        );
      }

      const derivedCost = computeDerivedUnitCost(
        raw.cost,
        input.rawQtyUsed,
        input.producedQty,
        input.wasteLossPercentage ?? 0,
      );
      const producedInt = Math.floor(input.producedQty);
      const timestamp = Date.now();
      const productionCode = `PROD-${timestamp}`;

      await this.db.runAsync('UPDATE products SET stockCount = ? WHERE id = ?;', [
        raw.stockCount - rawDeduct,
        raw.id,
      ]);
      await this.db.runAsync('UPDATE products SET stockCount = ?, cost = ? WHERE id = ?;', [
        target.stockCount + producedInt,
        derivedCost,
        target.id,
      ]);

      const res = await this.db.runAsync(
        `INSERT INTO production_runs (
          productionCode, rawMaterialProductId, rawMaterialName, rawMaterialQtyUsed,
          rawMaterialUnitCost, targetRetailProductId, targetRetailProductName,
          targetRetailQtyProduced, derivedRetailUnitCost, retailSellingPrice,
          wasteLossQty, operatorUsername, timestamp, notes
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);`,
        [
          productionCode,
          raw.id,
          raw.name,
          input.rawQtyUsed,
          raw.cost,
          target.id,
          target.name,
          input.producedQty,
          derivedCost,
          target.price,
          0,
          input.operatorUsername,
          timestamp,
          input.notes ?? '',
        ],
      );

      return {
        id: res.lastInsertRowId,
        productionCode,
        rawMaterialProductId: raw.id,
        rawMaterialName: raw.name,
        rawMaterialQtyUsed: input.rawQtyUsed,
        rawMaterialUnitCost: raw.cost,
        targetRetailProductId: target.id,
        targetRetailProductName: target.name,
        targetRetailQtyProduced: input.producedQty,
        derivedRetailUnitCost: derivedCost,
        retailSellingPrice: target.price,
        wasteLossQty: 0,
        operatorUsername: input.operatorUsername,
        timestamp,
        notes: input.notes ?? '',
      };
    });
  }
}
