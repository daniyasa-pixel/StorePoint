import { File, Paths } from 'expo-file-system';
import * as Sharing from 'expo-sharing';
import { APP_VERSION } from '@/constants/appInfo';
import { getDatabase } from '@/db/database';
import { createRepositories, getRepositories } from '@/db/repositories';
import { SCHEMA_VERSION } from '@/db/schemaV19';
import type {
  Category,
  PaymentSchedule,
  Product,
  StoreConfig,
  Supplier,
} from '@/db/types';
import { DEFAULT_CURRENCY_SYMBOL } from '@/domain/money';
import { useAppBootstrapStore } from '@/store/appBootstrapStore';

const PERSISTENT_BACKUP_FILENAME = 'storepoint-latest-backup.json';

export interface BackupSnapshotEnvelope {
  readonly schemaVersion: number;
  readonly exportedAt: number;
  readonly appVersion: string;
  readonly tables: {
    readonly storeConfig: unknown;
    readonly categories: readonly unknown[];
    readonly products: readonly unknown[];
    readonly transactions: readonly unknown[];
    readonly suppliers: readonly unknown[];
    readonly purchaseOrders: readonly unknown[];
    readonly schedules: readonly unknown[];
    readonly walletLedger: readonly unknown[];
  };
}

export interface PersistentBackupSummary {
  readonly storeName: string;
  readonly productsCount: number;
  readonly transactionsCount: number;
  readonly exportedAt: number;
}

export interface BackupRestoreResult {
  readonly ok: boolean;
  readonly storeName: string;
  readonly currencySymbol: string;
  readonly message: string;
}

function asRecord(value: unknown): Record<string, unknown> | null {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : null;
}

function asString(value: unknown, fallback = ''): string {
  return typeof value === 'string' ? value : fallback;
}

function asNumber(value: unknown, fallback = 0): number {
  return typeof value === 'number' && Number.isFinite(value) ? value : fallback;
}

function asBool(value: unknown, fallback = false): boolean {
  if (typeof value === 'boolean') return value;
  if (value === 1) return true;
  if (value === 0) return false;
  return fallback;
}

function coerceStoreConfig(raw: unknown): StoreConfig {
  const rec = asRecord(raw);
  const storeName = asString(rec?.storeName, 'StorePoint').trim() || 'StorePoint';
  const currencySymbol =
    asString(rec?.currencySymbol, DEFAULT_CURRENCY_SYMBOL).trim() || DEFAULT_CURRENCY_SYMBOL;

  return {
    id: 1,
    storeName,
    currencySymbol,
    taxPercentage: Math.max(0, asNumber(rec?.taxPercentage, 12.0)),
    setupCompleted: true,
    gcashBalance: Math.max(0, asNumber(rec?.gcashBalance, 0)),
    hasGCash: asBool(rec?.hasGCash, true),
    hasMaya: asBool(rec?.hasMaya, true),
    hasLoad: asBool(rec?.hasLoad, true),
    smartLoadBalance: Math.max(0, asNumber(rec?.smartLoadBalance, 0)),
    globeLoadBalance: Math.max(0, asNumber(rec?.globeLoadBalance, 0)),
    loadServiceFee: Math.max(0, asNumber(rec?.loadServiceFee, 2.0)),
    gcashServiceFee: Math.max(0, asNumber(rec?.gcashServiceFee, 10.0)),
    mayaServiceFee: Math.max(0, asNumber(rec?.mayaServiceFee, 10.0)),
    mayaBankFee: Math.max(0, asNumber(rec?.mayaBankFee, 15.0)),
  };
}

function coerceCategory(raw: unknown): (Omit<Category, 'id'> & { readonly id?: number }) | null {
  const rec = asRecord(raw);
  const name = asString(rec?.name).trim();
  if (!name) return null;
  const rawId = asNumber(rec?.id, 0);
  return {
    ...(rawId > 0 ? { id: rawId } : {}),
    name,
    colorAccent: asString(rec?.colorAccent, '#006FEE'),
    iconBadge: asString(rec?.iconBadge, 'store'),
    isArchived: asBool(rec?.isArchived, false),
  };
}

function coerceProduct(
  raw: unknown,
  categoryIdMap: ReadonlyMap<number, number>,
  fallbackCategoryId: number,
): Omit<Product, 'id'> | null {
  const rec = asRecord(raw);
  const name = asString(rec?.name).trim();
  if (!name) return null;
  const originalCatId = asNumber(rec?.categoryId, 0);
  const mappedCategoryId = categoryIdMap.get(originalCatId) ?? fallbackCategoryId;

  return {
    name,
    categoryId: mappedCategoryId,
    price: Math.max(0, asNumber(rec?.price, 0)),
    stockCount: Math.trunc(asNumber(rec?.stockCount, 0)),
    barcode: asString(rec?.barcode, ''),
    expirationDate: typeof rec?.expirationDate === 'string' ? rec.expirationDate : null,
    cost: Math.max(0, asNumber(rec?.cost, 0)),
    hasStick10s: asBool(rec?.hasStick10s, false),
    priceStick10s: Math.max(0, asNumber(rec?.priceStick10s, 0)),
    hasStick20s: asBool(rec?.hasStick20s, false),
    priceStick20s: Math.max(0, asNumber(rec?.priceStick20s, 0)),
    hasReam: asBool(rec?.hasReam, false),
    priceReam: Math.max(0, asNumber(rec?.priceReam, 0)),
    hasMasterCase: asBool(rec?.hasMasterCase, false),
    priceMasterCase: Math.max(0, asNumber(rec?.priceMasterCase, 0)),
    hasCustomUom: asBool(rec?.hasCustomUom, false),
    customUomName: asString(rec?.customUomName, 'Pack'),
    customUomMultiplier: Math.max(1, Math.trunc(asNumber(rec?.customUomMultiplier, 1))),
    customUomPrice: Math.max(0, asNumber(rec?.customUomPrice, 0)),
    barcode10s: asString(rec?.barcode10s, ''),
    barcode20s: asString(rec?.barcode20s, ''),
    barcodeReam: asString(rec?.barcodeReam, ''),
    barcodeMasterCase: asString(rec?.barcodeMasterCase, ''),
    barcodeCustomUom: asString(rec?.barcodeCustomUom, ''),
    baseUom: asString(rec?.baseUom, 'pc') || 'pc',
    trackInventory: asBool(rec?.trackInventory, true),
    isArchived: asBool(rec?.isArchived, false),
  };
}

function coerceSupplier(raw: unknown): Omit<Supplier, 'id'> | null {
  const rec = asRecord(raw);
  const name = asString(rec?.name).trim();
  if (!name) return null;
  return {
    name,
    contactPerson: asString(rec?.contactPerson, ''),
    phone: asString(rec?.phone, ''),
    email: asString(rec?.email, ''),
    address: asString(rec?.address, ''),
    deliverySchedule: asString(rec?.deliverySchedule, ''),
    paymentTerms: asString(rec?.paymentTerms, 'Cash on Delivery'),
    notes: asString(rec?.notes, ''),
  };
}

function coerceSchedule(raw: unknown): Omit<PaymentSchedule, 'id'> | null {
  const rec = asRecord(raw);
  const title = asString(rec?.title).trim();
  if (!title) return null;
  const rawPriority = asString(rec?.priority, 'Medium');
  const priority: PaymentSchedule['priority'] =
    rawPriority === 'High' || rawPriority === 'Medium' || rawPriority === 'Low'
      ? rawPriority
      : 'Medium';

  return {
    title,
    amount: Math.max(0, asNumber(rec?.amount, 0)),
    dueDate: asString(rec?.dueDate, new Date().toISOString().slice(0, 10)),
    category: asString(rec?.category, 'General'),
    priority,
    isPaid: asBool(rec?.isPaid, false),
    paymentMethod: asString(rec?.paymentMethod, 'CASH'),
  };
}

export async function buildBackupSnapshot(): Promise<BackupSnapshotEnvelope> {
  const repos = await getRepositories();
  const [
    storeConfig,
    categories,
    products,
    transactions,
    suppliers,
    purchaseOrders,
    schedules,
    walletLedger,
  ] = await Promise.all([
    repos.storeConfig.getConfig(),
    repos.catalog.getCategories(true),
    repos.catalog.getProducts(true),
    repos.checkout.getRecentTransactions(5000),
    repos.purchasing.getSuppliers(),
    repos.purchasing.getPurchaseOrders(),
    repos.schedules.getAll(),
    repos.storeConfig.getWalletLedger(1000),
  ]);

  return {
    schemaVersion: SCHEMA_VERSION,
    exportedAt: Date.now(),
    appVersion: APP_VERSION,
    tables: {
      storeConfig,
      categories,
      products,
      transactions,
      suppliers,
      purchaseOrders,
      schedules,
      walletLedger,
    },
  };
}

export async function exportAndShareBackupSnapshot(): Promise<string> {
  const snapshot = await buildBackupSnapshot();
  const serialized = JSON.stringify(snapshot, null, 2);

  try {
    const persistentFile = new File(Paths.document, PERSISTENT_BACKUP_FILENAME);
    await persistentFile.write(serialized);
  } catch {
    // Non-fatal if internal document copy fails while still allowing external share
  }

  const filename = `storepoint-backup-${Date.now()}.json`;
  const file = new File(Paths.cache, filename);
  await file.write(serialized);

  try {
    if (await Sharing.isAvailableAsync()) {
      await Sharing.shareAsync(file.uri, {
        mimeType: 'application/json',
        dialogTitle: 'Share StorePoint POS Backup',
      });
    }
  } finally {
    try {
      file.delete();
    } catch {
      // Ignore temporary cache file cleanup errors
    }
  }

  return file.uri;
}

export async function peekPersistentBackup(): Promise<PersistentBackupSummary | null> {
  try {
    const persistentFile = new File(Paths.document, PERSISTENT_BACKUP_FILENAME);
    if (!persistentFile.exists) {
      return null;
    }
    const raw = await persistentFile.text();
    if (!raw.trim()) {
      return null;
    }
    const parsed = asRecord(JSON.parse(raw));
    const tables = asRecord(parsed?.tables);
    if (!tables) {
      return null;
    }
    const config = asRecord(tables.storeConfig);
    const storeName = asString(config?.storeName, 'StorePoint').trim() || 'StorePoint';
    const productsCount = Array.isArray(tables.products) ? tables.products.length : 0;
    const transactionsCount = Array.isArray(tables.transactions) ? tables.transactions.length : 0;
    const exportedAt = asNumber(parsed?.exportedAt, Date.now());

    return {
      storeName,
      productsCount,
      transactionsCount,
      exportedAt,
    };
  } catch {
    return null;
  }
}

export async function restoreFromBackupJson(rawJson: string): Promise<BackupRestoreResult> {
  try {
    const parsed = asRecord(JSON.parse(rawJson));
    const tables = asRecord(parsed?.tables);
    if (!parsed || !tables) {
      return {
        ok: false,
        storeName: 'StorePoint',
        currencySymbol: DEFAULT_CURRENCY_SYMBOL,
        message: 'Invalid backup JSON structure.',
      };
    }

    const config = coerceStoreConfig(tables.storeConfig);
    const rawCategories = Array.isArray(tables.categories) ? tables.categories : [];
    const rawProducts = Array.isArray(tables.products) ? tables.products : [];
    const rawSuppliers = Array.isArray(tables.suppliers) ? tables.suppliers : [];
    const rawSchedules = Array.isArray(tables.schedules) ? tables.schedules : [];

    const db = await getDatabase();
    const repos = createRepositories(db);

    let restoredProducts = 0;

    await db.withTransactionAsync(async () => {
      await repos.storeConfig.saveConfig(config);

      await db.runAsync('DELETE FROM products;');
      await db.runAsync('DELETE FROM categories;');
      await db.runAsync('DELETE FROM payment_schedules;');
      await db.runAsync('DELETE FROM purchase_order_items;');
      await db.runAsync('DELETE FROM purchase_orders;');
      await db.runAsync('DELETE FROM suppliers;');

      const categoryIdMap = new Map<number, number>();
      let firstCategoryId = 0;

      for (const item of rawCategories) {
        const cat = coerceCategory(item);
        if (!cat) continue;
        const insertedId = await repos.catalog.saveCategory({
          name: cat.name,
          colorAccent: cat.colorAccent,
          iconBadge: cat.iconBadge,
          isArchived: cat.isArchived,
        });
        if (!firstCategoryId) {
          firstCategoryId = insertedId;
        }
        if (cat.id) {
          categoryIdMap.set(cat.id, insertedId);
        }
      }

      if (!firstCategoryId) {
        firstCategoryId = await repos.catalog.saveCategory({
          name: 'General',
          colorAccent: '#006FEE',
          iconBadge: 'store',
          isArchived: false,
        });
      }

      for (const item of rawProducts) {
        const prod = coerceProduct(item, categoryIdMap, firstCategoryId);
        if (!prod) continue;
        await repos.catalog.saveProduct(prod);
        restoredProducts += 1;
      }

      for (const item of rawSuppliers) {
        const supplier = coerceSupplier(item);
        if (!supplier) continue;
        await repos.purchasing.saveSupplier(supplier);
      }

      for (const item of rawSchedules) {
        const schedule = coerceSchedule(item);
        if (!schedule) continue;
        await repos.schedules.save(schedule);
      }
    });

    useAppBootstrapStore.getState().completeSetup(config.storeName, config.currencySymbol);

    return {
      ok: true,
      storeName: config.storeName,
      currencySymbol: config.currencySymbol,
      message: `Restored ${config.storeName} (${restoredProducts} products).`,
    };
  } catch (error) {
    return {
      ok: false,
      storeName: 'StorePoint',
      currencySymbol: DEFAULT_CURRENCY_SYMBOL,
      message: error instanceof Error ? error.message : 'Failed to restore backup.',
    };
  }
}

export async function restoreFromPersistentBackup(): Promise<BackupRestoreResult> {
  try {
    const persistentFile = new File(Paths.document, PERSISTENT_BACKUP_FILENAME);
    if (!persistentFile.exists) {
      return {
        ok: false,
        storeName: 'StorePoint',
        currencySymbol: DEFAULT_CURRENCY_SYMBOL,
        message: 'No persistent backup file found on this device.',
      };
    }
    const rawJson = await persistentFile.text();
    return restoreFromBackupJson(rawJson);
  } catch (error) {
    return {
      ok: false,
      storeName: 'StorePoint',
      currencySymbol: DEFAULT_CURRENCY_SYMBOL,
      message: error instanceof Error ? error.message : 'Unable to read persistent backup.',
    };
  }
}
