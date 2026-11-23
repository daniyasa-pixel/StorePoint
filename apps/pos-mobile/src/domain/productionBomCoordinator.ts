import { roundMoney } from './money';

export interface CostMarginAnalysis {
  readonly derivedUnitCost: number;
  readonly suggestedSellingPrice: number;
  readonly grossProfit: number;
  readonly marginPercentage: number;
}

export interface MaterialRequirement {
  readonly rawProductId: number;
  readonly rawProductName: string;
  readonly requiredQuantity: number;
  readonly availableStock: number;
  readonly isSufficient: boolean;
}

export function computeDerivedUnitCost(
  rawUnitCost: number,
  batchUsage: number,
  producedUnits: number,
  scrapPercentage = 0,
): number {
  if (producedUnits <= 0) {
    return 0;
  }
  const effectiveUsage =
    scrapPercentage > 0 ? batchUsage * (1 + scrapPercentage / 100) : batchUsage;
  const totalRawCost = rawUnitCost * effectiveUsage;
  return roundMoney(totalRawCost / producedUnits);
}

export function analyzeCostAndMargin(
  unitCost: number,
  sellingPrice: number,
): CostMarginAnalysis {
  const grossProfit = roundMoney(sellingPrice - unitCost);
  const marginPercentage =
    sellingPrice > 0 ? roundMoney((grossProfit / sellingPrice) * 100) : 0;

  return {
    derivedUnitCost: unitCost,
    suggestedSellingPrice: sellingPrice,
    grossProfit,
    marginPercentage,
  };
}

export function validateRequirementsSufficiency(
  requirements: readonly MaterialRequirement[],
): boolean {
  return requirements.every((req) => req.availableStock >= req.requiredQuantity);
}
