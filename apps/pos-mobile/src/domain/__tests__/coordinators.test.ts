import {
  calculateVatableSalesFromInclusive,
  calculateVatFromInclusive,
  canonicalWalletType,
  roundMoney,
} from '../money';
import {
  computeBaseUnitsNeeded,
  computeCheckoutTotals,
  computeSplitPayment,
} from '../posCheckoutCoordinator';
import { computeDenominationTotal, reconcileShiftDrawer } from '../cashDrawerCoordinator';
import {
  analyzeCostAndMargin,
  computeDerivedUnitCost,
  validateRequirementsSufficiency,
} from '../productionBomCoordinator';
import type { CartItemDetails, Product } from '@/db/types';

function makeProduct(overrides: Partial<Product> = {}): Product {
  return {
    id: 1,
    name: 'Test Item',
    categoryId: 1,
    price: 112,
    stockCount: 50,
    barcode: '123456',
    expirationDate: null,
    cost: 80,
    hasStick10s: false,
    priceStick10s: 0,
    hasStick20s: false,
    priceStick20s: 0,
    hasReam: false,
    priceReam: 0,
    hasMasterCase: false,
    priceMasterCase: 0,
    hasCustomUom: false,
    customUomName: 'Pack',
    customUomMultiplier: 1,
    customUomPrice: 0,
    barcode10s: '',
    barcode20s: '',
    barcodeReam: '',
    barcodeMasterCase: '',
    barcodeCustomUom: '',
    baseUom: 'pc',
    trackInventory: true,
    isArchived: false,
    ...overrides,
  };
}

describe('Money & VAT utilities', () => {
  it('rounds half-up cleanly and handles non-finite inputs', () => {
    expect(roundMoney(10.005)).toBe(10.01);
    expect(roundMoney(10.004)).toBe(10);
    expect(roundMoney(Number.NaN)).toBe(0);
    expect(roundMoney(Number.POSITIVE_INFINITY)).toBe(0);
  });

  it('computes 12% VAT inclusive breakdown accurately', () => {
    expect(calculateVatFromInclusive(112, 12)).toBe(12);
    expect(calculateVatableSalesFromInclusive(112, 12)).toBe(100);
  });

  it('maps wallet labels to canonical types', () => {
    expect(canonicalWalletType('GCash')).toBe('GCASH');
    expect(canonicalWalletType('Smart / TNT')).toBe('SMART');
    expect(canonicalWalletType('Globe/TM')).toBe('GLOBE');
  });
});

describe('PosCheckoutCoordinator', () => {
  it('computes Senior/PWD VAT-exempt 20% discount per RA 9994', () => {
    const items: CartItemDetails[] = [
      { product: makeProduct(), quantity: 1, uomName: 'pc', multiplier: 1, selectedPrice: 112 },
    ];
    const totals = computeCheckoutTotals(items, 'SENIOR_PWD', 0, true, 12);
    expect(totals).toEqual({
      subtotal: 112,
      discountAmount: 20,
      netTotal: 80,
      vatableSales: 0,
      vatAmount: 0,
      vatExemptSales: 80,
    });
  });

  it('computes multi-UOM base units and split tender settlement', () => {
    const line: CartItemDetails = {
      product: makeProduct(),
      quantity: 3,
      uomName: 'Pack of 6',
      multiplier: 6,
      selectedPrice: 100,
    };
    expect(computeBaseUnitsNeeded(line)).toBe(18);
    expect(computeSplitPayment(300, 200, 150)).toEqual({
      nonCashAmount: 200,
      cashRequired: 100,
      cashPaid: 150,
      changeAmount: 50,
      isFullySettled: true,
    });
  });
});

describe('CashDrawerCoordinator', () => {
  it('computes denomination count and flags override above ₱50 variance', () => {
    const denominations = {
      count1000: 1,
      count500: 1,
      count200: 0,
      count100: 2,
      count50: 0,
      count20: 0,
      countCoins: 25.5,
    };
    expect(computeDenominationTotal(denominations)).toBe(1725.5);

    const rec = reconcileShiftDrawer({
      startingFloat: 500,
      cashSales: 1300,
      cashOut: 0,
      denominations,
    });
    expect(rec.expectedEndingCash).toBe(1800);
    expect(rec.cashVariance).toBe(-74.5);
    expect(rec.hasDiscrepancy).toBe(true);
    expect(rec.requiresManagerOverride).toBe(true);
  });
});

describe('ProductionBomCoordinator', () => {
  it('computes derived unit cost with scrap and validates raw sufficiency', () => {
    const cost = computeDerivedUnitCost(100, 2, 10, 10); // 100 * 2.2 / 10 = 22
    expect(cost).toBe(22);
    expect(analyzeCostAndMargin(22, 44)).toEqual({
      derivedUnitCost: 22,
      suggestedSellingPrice: 44,
      grossProfit: 22,
      marginPercentage: 50,
    });
    expect(
      validateRequirementsSufficiency([
        { rawProductId: 1, rawProductName: 'Sack Rice', requiredQuantity: 5, availableStock: 5, isSufficient: true },
      ]),
    ).toBe(true);
  });
});
