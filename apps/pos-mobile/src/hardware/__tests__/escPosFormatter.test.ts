import {
  buildEscPosReceiptBytes,
  charsPerLineForWidth,
  ESC_POS,
  formatLeftRightLine,
  formatReceiptText,
} from '../escPosFormatter';
import type { CheckoutReceipt } from '@/db/repositories/checkoutRepository';

const SAMPLE_RECEIPT: CheckoutReceipt = {
  transaction: {
    id: 42,
    timestamp: 1_700_000_000_000,
    cashierUsername: 'maria',
    subtotal: 150,
    taxAmount: 16.07,
    totalAmount: 150,
    paymentMethod: 'CASH',
    cashPaid: 200,
    changeAmount: 50,
    status: 'COMPLETED',
    customerName: '',
  },
  items: [
    {
      id: 1,
      transactionId: 42,
      productId: 10,
      productName: 'Mineral Water 500ml',
      price: 25,
      quantity: 2,
      cost: 15,
      uomName: 'pc',
    },
    {
      id: 2,
      transactionId: 42,
      productId: 11,
      productName: 'Instant Coffee Twin Pack',
      price: 100,
      quantity: 1,
      cost: 75,
      uomName: 'pack',
    },
  ],
};

describe('ESC/POS Thermal Formatter', () => {
  it('enforces exact 32-char (58mm) and 48-char (80mm) line widths', () => {
    expect(charsPerLineForWidth(58)).toBe(32);
    expect(charsPerLineForWidth(80)).toBe(48);

    const line58 = formatLeftRightLine('Very Long Product Name That Exceeds Width', 'P150.00', 32);
    expect(line58.length).toBe(32);

    const text58 = formatReceiptText(SAMPLE_RECEIPT, 'StorePoint Mart', 'P', 58);
    for (const line of text58.split('\n')) {
      expect(line.length).toBeLessThanOrEqual(32);
    }

    const text80 = formatReceiptText(SAMPLE_RECEIPT, 'StorePoint Mart', 'P', 80);
    for (const line of text80.split('\n')) {
      expect(line.length).toBeLessThanOrEqual(48);
    }
  });

  it('emits ESC @ init, paper cut, and optional drawer kick bytes', () => {
    const bytes = buildEscPosReceiptBytes(SAMPLE_RECEIPT, 'StorePoint', 'P', 58, true);
    expect(bytes[0]).toBe(ESC_POS.INIT[0]);
    expect(bytes[1]).toBe(ESC_POS.INIT[1]);
    // Last 5 bytes should match DRAWER_KICK
    const tail = Array.from(bytes.slice(-5));
    expect(tail).toEqual([...ESC_POS.DRAWER_KICK]);
  });
});
