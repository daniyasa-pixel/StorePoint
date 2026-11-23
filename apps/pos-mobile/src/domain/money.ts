/**
 * Financial precision & Philippine VAT utilities.
 * Direct TypeScript port of StorePointRepository companion helpers.
 */
export const WALLET_TYPES = {
  GCASH: 'GCASH',
  SMART: 'SMART',
  GLOBE: 'GLOBE',
} as const;

export type WalletType = (typeof WALLET_TYPES)[keyof typeof WALLET_TYPES];

export const WALLET_ENTRY_TYPES = {
  RELOAD: 'RELOAD',
  CONSUMED: 'CONSUMED',
  INFLOW: 'INFLOW',
  OUTFLOW: 'OUTFLOW',
  RECONCILE: 'RECONCILE',
  OPENING_BALANCE: 'OPENING_BALANCE',
} as const;

export type WalletEntryType = (typeof WALLET_ENTRY_TYPES)[keyof typeof WALLET_ENTRY_TYPES];

/**
 * Half-up rounding to 2 decimal places, resilient to IEEE-754 drift.
 */
export function roundMoney(amount: number): number {
  if (!Number.isFinite(amount)) {
    return 0;
  }
  const sign = amount < 0 ? -1 : 1;
  const scaled = Math.floor(Math.abs(amount) * 100 + 0.5 + 1e-9);
  return (sign * scaled) / 100;
}

/**
 * Maps UI-facing wallet labels ("Smart", "TNT", "Globe/TM", "DITO") to canonical keys.
 */
export function canonicalWalletType(label: string): WalletType {
  const lower = label.toLowerCase();
  if (lower.includes('gcash')) {
    return WALLET_TYPES.GCASH;
  }
  if (lower.includes('smart') || lower.includes('tnt') || lower.includes('dito')) {
    return WALLET_TYPES.SMART;
  }
  if (lower.includes('globe') || lower.includes('tm')) {
    return WALLET_TYPES.GLOBE;
  }
  return WALLET_TYPES.GCASH;
}

/**
 * Calculates Philippine VAT (default 12%) from a VAT-inclusive gross retail amount.
 * Formula: (amount / (1 + rate)) * rate
 */
export function calculateVatFromInclusive(amount: number, vatPercent = 12.0): number {
  if (!Number.isFinite(amount) || amount <= 0 || vatPercent <= 0) {
    return 0;
  }
  const rateFraction = vatPercent / 100;
  const vat = (amount / (1 + rateFraction)) * rateFraction;
  return roundMoney(vat);
}

/**
 * Calculates Net VATable sales from a VAT-inclusive gross retail amount.
 */
export function calculateVatableSalesFromInclusive(amount: number, vatPercent = 12.0): number {
  if (!Number.isFinite(amount) || amount <= 0) {
    return 0;
  }
  const vat = calculateVatFromInclusive(amount, vatPercent);
  return roundMoney(amount - vat);
}
