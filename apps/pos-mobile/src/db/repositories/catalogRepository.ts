import type { Category, Product, ProductVariant, SqlExecutor } from '@/db/types';
import { roundMoney } from '@/domain/money';

interface CategoryRow {
  id: number;
  name: string;
  colorAccent: string;
  iconBadge: string;
  isArchived: number;
}

interface ProductRow {
  id: number;
  name: string;
  categoryId: number;
  price: number;
  stockCount: number;
  barcode: string;
  expirationDate: string | null;
  cost: number;
  hasStick10s: number;
  priceStick10s: number;
  hasStick20s: number;
  priceStick20s: number;
  hasReam: number;
  priceReam: number;
  hasMasterCase: number;
  priceMasterCase: number;
  hasCustomUom: number;
  customUomName: string;
  customUomMultiplier: number;
  customUomPrice: number;
  barcode10s: string;
  barcode20s: string;
  barcodeReam: string;
  barcodeMasterCase: string;
  barcodeCustomUom: string;
  baseUom: string;
  trackInventory: number;
  isArchived: number;
}

interface VariantRow {
  id: number;
  productId: number;
  variantName: string;
  uomName: string;
  multiplier: number;
  price: number;
  cost: number;
  barcode: string;
  isDefault: number;
  stockCount: number;
}

function mapCategory(r: CategoryRow): Category {
  return {
    id: r.id,
    name: r.name,
    colorAccent: r.colorAccent,
    iconBadge: r.iconBadge,
    isArchived: r.isArchived === 1,
  };
}

function mapProduct(r: ProductRow): Product {
  return {
    ...r,
    hasStick10s: r.hasStick10s === 1,
    hasStick20s: r.hasStick20s === 1,
    hasReam: r.hasReam === 1,
    hasMasterCase: r.hasMasterCase === 1,
    hasCustomUom: r.hasCustomUom === 1,
    trackInventory: r.trackInventory === 1,
    isArchived: r.isArchived === 1,
  };
}

function mapVariant(r: VariantRow): ProductVariant {
  return { ...r, isDefault: r.isDefault === 1 };
}

export class CatalogRepository {
  constructor(private readonly db: SqlExecutor) {}

  async getCategories(includeArchived = false): Promise<Category[]> {
    const sql = includeArchived
      ? 'SELECT * FROM categories ORDER BY name ASC;'
      : 'SELECT * FROM categories WHERE isArchived = 0 ORDER BY name ASC;';
    const rows = await this.db.getAllAsync<CategoryRow>(sql);
    return rows.map(mapCategory);
  }

  async saveCategory(category: Omit<Category, 'id'> & { readonly id?: number }): Promise<number> {
    if (category.id && category.id > 0) {
      await this.db.runAsync(
        'UPDATE categories SET name = ?, colorAccent = ?, iconBadge = ?, isArchived = ? WHERE id = ?;',
        [
          category.name,
          category.colorAccent,
          category.iconBadge,
          category.isArchived ? 1 : 0,
          category.id,
        ],
      );
      return category.id;
    }
    const res = await this.db.runAsync(
      'INSERT INTO categories (name, colorAccent, iconBadge, isArchived) VALUES (?, ?, ?, ?);',
      [category.name, category.colorAccent, category.iconBadge, category.isArchived ? 1 : 0],
    );
    return res.lastInsertRowId;
  }

  async getProducts(includeArchived = false): Promise<Product[]> {
    const sql = includeArchived
      ? 'SELECT * FROM products ORDER BY name ASC;'
      : 'SELECT * FROM products WHERE isArchived = 0 ORDER BY name ASC;';
    const rows = await this.db.getAllAsync<ProductRow>(sql);
    return rows.map(mapProduct);
  }

  async getProductById(id: number): Promise<Product | null> {
    const row = await this.db.getFirstAsync<ProductRow>(
      'SELECT * FROM products WHERE id = ? LIMIT 1;',
      [id],
    );
    return row ? mapProduct(row) : null;
  }

  async findProductByBarcode(barcode: string): Promise<Product | null> {
    const trimmed = barcode.trim();
    if (!trimmed) return null;
    const row = await this.db.getFirstAsync<ProductRow>(
      `SELECT * FROM products WHERE isArchived = 0 AND (
        barcode = ? OR barcode10s = ? OR barcode20s = ? OR barcodeReam = ?
        OR barcodeMasterCase = ? OR barcodeCustomUom = ?
      ) LIMIT 1;`,
      [trimmed, trimmed, trimmed, trimmed, trimmed, trimmed],
    );
    return row ? mapProduct(row) : null;
  }

  async saveProduct(product: Omit<Product, 'id'> & { readonly id?: number }): Promise<number> {
    const values = [
      product.name,
      product.categoryId,
      roundMoney(product.price),
      product.stockCount,
      product.barcode,
      product.expirationDate,
      roundMoney(product.cost),
      product.hasStick10s ? 1 : 0,
      roundMoney(product.priceStick10s),
      product.hasStick20s ? 1 : 0,
      roundMoney(product.priceStick20s),
      product.hasReam ? 1 : 0,
      roundMoney(product.priceReam),
      product.hasMasterCase ? 1 : 0,
      roundMoney(product.priceMasterCase),
      product.hasCustomUom ? 1 : 0,
      product.customUomName,
      product.customUomMultiplier,
      roundMoney(product.customUomPrice),
      product.barcode10s,
      product.barcode20s,
      product.barcodeReam,
      product.barcodeMasterCase,
      product.barcodeCustomUom,
      product.baseUom,
      product.trackInventory ? 1 : 0,
      product.isArchived ? 1 : 0,
    ];

    if (product.id && product.id > 0) {
      await this.db.runAsync(
        `UPDATE products SET
          name = ?, categoryId = ?, price = ?, stockCount = ?, barcode = ?, expirationDate = ?, cost = ?,
          hasStick10s = ?, priceStick10s = ?, hasStick20s = ?, priceStick20s = ?, hasReam = ?, priceReam = ?,
          hasMasterCase = ?, priceMasterCase = ?, hasCustomUom = ?, customUomName = ?, customUomMultiplier = ?,
          customUomPrice = ?, barcode10s = ?, barcode20s = ?, barcodeReam = ?, barcodeMasterCase = ?,
          barcodeCustomUom = ?, baseUom = ?, trackInventory = ?, isArchived = ?
        WHERE id = ?;`,
        [...values, product.id],
      );
      return product.id;
    }

    const res = await this.db.runAsync(
      `INSERT INTO products (
        name, categoryId, price, stockCount, barcode, expirationDate, cost,
        hasStick10s, priceStick10s, hasStick20s, priceStick20s, hasReam, priceReam,
        hasMasterCase, priceMasterCase, hasCustomUom, customUomName, customUomMultiplier,
        customUomPrice, barcode10s, barcode20s, barcodeReam, barcodeMasterCase,
        barcodeCustomUom, baseUom, trackInventory, isArchived
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);`,
      values,
    );
    return res.lastInsertRowId;
  }

  async getVariantsForProduct(productId: number): Promise<ProductVariant[]> {
    const rows = await this.db.getAllAsync<VariantRow>(
      'SELECT * FROM product_variants WHERE productId = ? ORDER BY isDefault DESC, multiplier ASC;',
      [productId],
    );
    return rows.map(mapVariant);
  }

  async saveVariant(variant: Omit<ProductVariant, 'id'> & { readonly id?: number }): Promise<number> {
    if (variant.id && variant.id > 0) {
      await this.db.runAsync(
        `UPDATE product_variants SET
          variantName = ?, uomName = ?, multiplier = ?, price = ?, cost = ?,
          barcode = ?, isDefault = ?, stockCount = ?
        WHERE id = ?;`,
        [
          variant.variantName,
          variant.uomName,
          variant.multiplier,
          roundMoney(variant.price),
          roundMoney(variant.cost),
          variant.barcode,
          variant.isDefault ? 1 : 0,
          variant.stockCount,
          variant.id,
        ],
      );
      return variant.id;
    }
    const res = await this.db.runAsync(
      `INSERT INTO product_variants (
        productId, variantName, uomName, multiplier, price, cost, barcode, isDefault, stockCount
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);`,
      [
        variant.productId,
        variant.variantName,
        variant.uomName,
        variant.multiplier,
        roundMoney(variant.price),
        roundMoney(variant.cost),
        variant.barcode,
        variant.isDefault ? 1 : 0,
        variant.stockCount,
      ],
    );
    return res.lastInsertRowId;
  }

  async adjustStock(params: {
    readonly productId: number;
    readonly delta: number;
    readonly actorUsername: string;
    readonly movementType: string;
    readonly reference?: string;
    readonly notes?: string;
  }): Promise<void> {
    await this.db.withTransactionAsync(async () => {
      const product = await this.getProductById(params.productId);
      if (!product) {
        throw new Error(`Product #${params.productId} not found.`);
      }
      const stockAfter = product.stockCount + params.delta;
      if (product.trackInventory && stockAfter < 0) {
        throw new Error(`Insufficient stock for ${product.name}.`);
      }
      await this.db.runAsync('UPDATE products SET stockCount = ? WHERE id = ?;', [
        stockAfter,
        product.id,
      ]);
      await this.db.runAsync(
        `INSERT INTO inventory_movements (
          productId, productName, timestamp, actorUsername, movementType,
          quantityDelta, stockBefore, stockAfter, reference, notes
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);`,
        [
          product.id,
          product.name,
          Date.now(),
          params.actorUsername,
          params.movementType,
          params.delta,
          product.stockCount,
          stockAfter,
          params.reference ?? '',
          params.notes ?? '',
        ],
      );
    });
  }
}
