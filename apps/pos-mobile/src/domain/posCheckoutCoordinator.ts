import type { CartItemDetails } from '@/db/types';
import {
  calculateVatFromInclusive,
  calculateVatableSalesFromInclusive,
  roundMoney,
} from './money';

export type DiscountType = 'NONE' | 'PERCENTAGE' | 'FIXED' | 'SENIOR_PWD';

export interface CheckoutTotals {
  readonly subtotal: number;
  readonly discountAmount: number;
  readonly netTotal: number;
  readonly vatableSales: number;
  readonly vatAmount: number;
  readonly vatExemptSales: number;
}

export interface SplitPaymentResult {
  readonly nonCashAmount: number;
  readonly cashRequired: number;
  readonly cashPaid: number;
  readonly changeAmount: number;
  readonly isFullySettled: boolean;
}

export function parseDiscountType(value: string): DiscountType {
  const normalized = value.trim().toUpperCase();
  if (normalized === 'PERCENTAGE') return 'PERCENTAGE';
  if (normalized === 'FIXED') return 'FIXED';
  if (normalized === 'SENIOR_PWD' || normalized === 'SENIOR' || normalized === 'PWD') {
    return 'SENIOR_PWD';
  }
  return 'NONE';
}

export function computeLineTotal(item: CartItemDetails): number {
  return roundMoney(item.quantity * item.selectedPrice);
}

export function computeBaseUnitsNeeded(item: CartItemDetails): number {
  const multiplier = item.multiplier > 0 ? item.multiplier : 1;
  return item.quantity * multiplier;
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}

/**
 * Computes complete checkout totals including statutory RA 9994 / RA 10754
 * Senior/PWD VAT exemption + 20% discount and 12% VAT breakdown.
 */
export function computeCheckoutTotals(
  items: readonly CartItemDetails[],
  discountType: DiscountType | string = 'NONE',
  discountValue = 0,
  isVatInclusive = true,
  vatRate = 12.0,
): CheckoutTotals {
  const subtotal = roundMoney(items.reduce((sum, item) => sum + computeLineTotal(item), 0));
  if (subtotal <= 0) {
    return {
      subtotal: 0,
      discountAmount: 0,
      netTotal: 0,
      vatableSales: 0,
      vatAmount: 0,
      vatExemptSales: 0,
    };
  }

  const type = parseDiscountType(discountType);
  let discountAmount = 0;
  let netTotal = subtotal;

  if (type === 'SENIOR_PWD') {
    const vatExemptBase =
      isVatInclusive && vatRate > 0
        ? calculateVatableSalesFromInclusive(subtotal, vatRate)
        : subtotal;
    discountAmount = roundMoney(vatExemptBase * 0.2);
    netTotal = roundMoney(vatExemptBase - discountAmount);
  } else if (type === 'PERCENTAGE') {
    const clampedPercent = clamp(discountValue, 0, 100);
    discountAmount = roundMoney(subtotal * (clampedPercent / 100));
    netTotal = roundMoney(subtotal - discountAmount);
  } else if (type === 'FIXED') {
    discountAmount = roundMoney(clamp(discountValue, 0, subtotal));
    netTotal = roundMoney(subtotal - discountAmount);
  }

  if (type === 'SENIOR_PWD') {
    return {
      subtotal,
      discountAmount,
      netTotal,
      vatableSales: 0,
      vatAmount: 0,
      vatExemptSales: netTotal,
    };
  }

  if (isVatInclusive && vatRate > 0) {
    const vatAmount = calculateVatFromInclusive(netTotal, vatRate);
    const vatableSales = calculateVatableSalesFromInclusive(netTotal, vatRate);
    return {
      subtotal,
      discountAmount,
      netTotal,
      vatableSales,
      vatAmount,
      vatExemptSales: 0,
    };
  }

  return {
    subtotal,
    discountAmount,
    netTotal,
    vatableSales: netTotal,
    vatAmount: 0,
    vatExemptSales: 0,
  };
}

export function computeSplitPayment(
  netTotal: number,
  nonCashTendered: number,
  cashTendered: number,
): SplitPaymentResult {
  const roundedNet = roundMoney(Math.max(0, netTotal));
  const nonCashAmount = roundMoney(clamp(nonCashTendered, 0, roundedNet));
  const cashRequired = roundMoney(roundedNet - nonCashAmount);
  const cashPaid = roundMoney(Math.max(0, cashTendered));
  const isFullySettled = cashPaid >= cashRequired;
  const changeAmount = isFullySettled ? roundMoney(cashPaid - cashRequired) : 0;

  return {
    nonCashAmount,
    cashRequired,
    cashPaid,
    changeAmount,
    isFullySettled,
  };
}
