import { File } from 'expo-file-system';
import * as Print from 'expo-print';
import * as Sharing from 'expo-sharing';
import { getRepositories } from '@/db/repositories';
import { DEFAULT_CURRENCY_SYMBOL } from '@/domain/money';

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

export async function exportAuditReportPdf(): Promise<string> {
  const repos = await getRepositories();
  const [config, txs, products] = await Promise.all([
    repos.storeConfig.getConfig(),
    repos.checkout.getRecentTransactions(100),
    repos.catalog.getProducts(false),
  ]);

  const storeName = escapeHtml(config?.storeName ?? 'StorePoint POS');
  const curr = escapeHtml(config?.currencySymbol ?? DEFAULT_CURRENCY_SYMBOL);
  const totalRevenue = txs.reduce((sum, t) => sum + t.totalAmount, 0);

  const productRows = products
    .slice(0, 50)
    .map(
      (p) =>
        `<tr><td>${escapeHtml(p.name)}</td><td>${p.stockCount} ${escapeHtml(p.baseUom)}</td><td>${curr}${p.price.toFixed(2)}</td></tr>`,
    )
    .join('');

  const html = `
    <html>
      <head>
        <style>
          body { font-family: sans-serif; padding: 24px; color: #111827; }
          h1 { color: #006FEE; margin-bottom: 4px; }
          table { width: 100%; border-collapse: collapse; margin-top: 16px; }
          th, td { border: 1px solid #E5E7EB; padding: 8px; text-align: left; font-size: 12px; }
          th { background: #F3F4F6; }
        </style>
      </head>
      <body>
        <h1>${storeName} — Sales & Inventory Audit</h1>
        <p>Generated: ${new Date().toLocaleString()} • Transactions: ${txs.length} • Gross Revenue: ${curr}${totalRevenue.toFixed(2)}</p>
        <table>
          <thead><tr><th>Product</th><th>Stock</th><th>Price</th></tr></thead>
          <tbody>${productRows}</tbody>
        </table>
      </body>
    </html>
  `;

  const { uri } = await Print.printToFileAsync({ html });
  try {
    if (await Sharing.isAvailableAsync()) {
      await Sharing.shareAsync(uri, {
        mimeType: 'application/pdf',
        dialogTitle: 'Share StorePoint Audit PDF',
      });
    }
  } finally {
    try {
      new File(uri).delete();
    } catch {
      // Ignore temporary cache file cleanup errors
    }
  }
  return uri;
}
