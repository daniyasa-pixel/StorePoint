import type { CheckoutReceipt } from '@/db/repositories/checkoutRepository';

export type ThermalPaperWidth = 58 | 80;

export const ESC_POS = {
  INIT: [0x1b, 0x40],
  ALIGN_LEFT: [0x1b, 0x61, 0x00],
  ALIGN_CENTER: [0x1b, 0x61, 0x01],
  ALIGN_RIGHT: [0x1b, 0x61, 0x02],
  BOLD_ON: [0x1b, 0x45, 0x01],
  BOLD_OFF: [0x1b, 0x45, 0x00],
  DRAWER_KICK: [0x1b, 0x70, 0x00, 0x19, 0xfa],
  CUT_PAPER: [0x1d, 0x56, 0x42, 0x00],
  FEED_3: [0x1b, 0x64, 0x03],
} as const;

export function charsPerLineForWidth(paperWidth: ThermalPaperWidth): number {
  return paperWidth === 80 ? 48 : 32;
}

export function formatLeftRightLine(
  left: string,
  right: string,
  width: number,
): string {
  const maxLeft = Math.max(1, width - right.length - 1);
  const clippedLeft = left.length > maxLeft ? left.slice(0, maxLeft) : left;
  const spaces = Math.max(1, width - clippedLeft.length - right.length);
  return `${clippedLeft}${' '.repeat(spaces)}${right}`;
}

export function formatReceiptText(
  receipt: CheckoutReceipt,
  storeName: string,
  currencySymbol: string,
  paperWidth: ThermalPaperWidth = 58,
): string {
  const width = charsPerLineForWidth(paperWidth);
  const divider = '-'.repeat(width);
  const lines: string[] = [
    storeName.toUpperCase().slice(0, width),
    `Receipt #${receipt.transaction.id}`,
    `Cashier: ${receipt.transaction.cashierUsername}`,
    divider,
  ];

  for (const item of receipt.items) {
    const qtyLabel = `${item.quantity}x ${item.productName}`;
    const amt = `${currencySymbol}${(item.price * item.quantity).toFixed(2)}`;
    lines.push(formatLeftRightLine(qtyLabel, amt, width));
  }

  lines.push(divider);
  lines.push(
    formatLeftRightLine(
      'TOTAL',
      `${currencySymbol}${receipt.transaction.totalAmount.toFixed(2)}`,
      width,
    ),
  );
  lines.push(
    formatLeftRightLine(
      receipt.transaction.paymentMethod,
      `${currencySymbol}${receipt.transaction.cashPaid.toFixed(2)}`,
      width,
    ),
  );
  lines.push(
    formatLeftRightLine(
      'CHANGE',
      `${currencySymbol}${receipt.transaction.changeAmount.toFixed(2)}`,
      width,
    ),
  );
  lines.push(divider);
  lines.push('Thank you for shopping!');

  return lines.join('\n');
}

export function buildEscPosReceiptBytes(
  receipt: CheckoutReceipt,
  storeName: string,
  currencySymbol: string,
  paperWidth: ThermalPaperWidth = 58,
  kickDrawer = false,
): Uint8Array {
  const encoder = new TextEncoder();
  const body = encoder.encode(`${formatReceiptText(receipt, storeName, currencySymbol, paperWidth)}\n`);
  const chunks: number[] = [
    ...ESC_POS.INIT,
    ...ESC_POS.ALIGN_LEFT,
    ...Array.from(body),
    ...ESC_POS.FEED_3,
    ...ESC_POS.CUT_PAPER,
  ];
  if (kickDrawer) {
    chunks.push(...ESC_POS.DRAWER_KICK);
  }
  return new Uint8Array(chunks);
}
