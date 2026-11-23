import { File, Paths } from 'expo-file-system';
import * as Sharing from 'expo-sharing';
import { getRepositories } from '@/db/repositories';
import { SCHEMA_VERSION } from '@/db/schemaV19';

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
    appVersion: '1.0.2.0',
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
  const filename = `storepoint-backup-${Date.now()}.json`;
  const file = new File(Paths.cache, filename);
  file.write(JSON.stringify(snapshot, null, 2));

  if (await Sharing.isAvailableAsync()) {
    await Sharing.shareAsync(file.uri, {
      mimeType: 'application/json',
      dialogTitle: 'Share StorePoint POS Backup',
    });
  }

  return file.uri;
}
