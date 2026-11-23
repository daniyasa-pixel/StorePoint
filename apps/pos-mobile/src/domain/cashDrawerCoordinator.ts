import { roundMoney } from './money';

export interface DenominationCount {
  readonly count1000: number;
  readonly count500: number;
  readonly count200: number;
  readonly count100: number;
  readonly count50: number;
  readonly count20: number;
  readonly countCoins: number;
}

export interface DrawerReconciliationResult {
  readonly startingFloat: number;
  readonly cashSales: number;
  readonly cashIn: number;
  readonly cashOut: number;
  readonly cashRefunds: number;
  readonly expectedEndingCash: number;
  readonly actualCountedCash: number;
  readonly cashVariance: number;
  readonly hasDiscrepancy: boolean;
  readonly requiresManagerOverride: boolean;
}

export function computeDenominationTotal(d: DenominationCount): number {
  return roundMoney(
    d.count1000 * 1000 +
      d.count500 * 500 +
      d.count200 * 200 +
      d.count100 * 100 +
      d.count50 * 50 +
      d.count20 * 20 +
      d.countCoins,
  );
}

export function computeExpectedEndingCash(
  startingFloat: number,
  cashSales: number,
  cashIn = 0,
  cashOut = 0,
  cashRefunds = 0,
): number {
  return roundMoney(startingFloat + cashSales + cashIn - cashOut - cashRefunds);
}

export function reconcileShiftDrawer(params: {
  readonly startingFloat: number;
  readonly cashSales: number;
  readonly cashIn?: number;
  readonly cashOut?: number;
  readonly cashRefunds?: number;
  readonly denominations: DenominationCount;
  readonly overrideThreshold?: number;
}): DrawerReconciliationResult {
  const cashIn = params.cashIn ?? 0;
  const cashOut = params.cashOut ?? 0;
  const cashRefunds = params.cashRefunds ?? 0;
  const overrideThreshold = params.overrideThreshold ?? 50;

  const expectedEndingCash = computeExpectedEndingCash(
    params.startingFloat,
    params.cashSales,
    cashIn,
    cashOut,
    cashRefunds,
  );
  const actualCountedCash = computeDenominationTotal(params.denominations);
  const cashVariance = roundMoney(actualCountedCash - expectedEndingCash);
  const absVariance = Math.abs(cashVariance);

  return {
    startingFloat: params.startingFloat,
    cashSales: params.cashSales,
    cashIn,
    cashOut,
    cashRefunds,
    expectedEndingCash,
    actualCountedCash,
    cashVariance,
    hasDiscrepancy: absVariance >= 0.01,
    requiresManagerOverride: absVariance > overrideThreshold,
  };
}
