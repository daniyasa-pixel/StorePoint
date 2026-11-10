package com.munzo.storepoint.domain

import com.munzo.storepoint.data.StorePointRepository

/**
 * Domain Coordinator for Bill of Materials (BOM), Manufacturing Production, and Ingredient Costing.
 *
 * Implements business rules for:
 * 1. Derived unit cost computation based on raw material batch usage and produced batch yield
 * 2. Gross profit and profit margin computation
 * 3. Pre-production stock validation to prevent negative inventory
 */
object ProductionBomCoordinator {

    data class CostMarginAnalysis(
        val derivedUnitCost: Double,
        val suggestedSellingPrice: Double,
        val grossProfit: Double,
        val marginPercentage: Double
    )

    data class MaterialRequirement(
        val rawProductId: Int,
        val rawProductName: String,
        val requiredQuantity: Double,
        val availableStock: Double,
        val isSufficient: Boolean
    )

    /**
     * Computes the derived unit cost of a manufactured item given raw batch cost, usage, and expected yield.
     * Incorporates scrap percentage if applicable.
     */
    fun computeDerivedUnitCost(
        rawUnitCost: Double,
        batchUsage: Double,
        producedUnits: Double,
        scrapPercentage: Double = 0.0
    ): Double {
        if (producedUnits <= 0.0) return 0.0
        val effectiveUsage = if (scrapPercentage > 0.0) {
            batchUsage * (1.0 + (scrapPercentage / 100.0))
        } else batchUsage
        val totalRawCost = rawUnitCost * effectiveUsage
        return StorePointRepository.roundMoney(totalRawCost / producedUnits)
    }

    /**
     * Computes gross profit and margin percentage for a produced good.
     */
    fun analyzeCostAndMargin(
        unitCost: Double,
        sellingPrice: Double
    ): CostMarginAnalysis {
        val profit = StorePointRepository.roundMoney(sellingPrice - unitCost)
        val margin = if (sellingPrice > 0.0) {
            StorePointRepository.roundMoney((profit / sellingPrice) * 100.0)
        } else 0.0

        return CostMarginAnalysis(
            derivedUnitCost = unitCost,
            suggestedSellingPrice = sellingPrice,
            grossProfit = profit,
            marginPercentage = margin
        )
    }

    /**
     * Checks if all raw materials required for a planned production run are sufficiently in stock.
     */
    fun validateRawMaterialSufficiency(
        requirements: List<Pair<Double, Double>> // Pair(required, available)
    ): Boolean {
        return requirements.all { (required, available) -> available >= required }
    }

    /**
     * Type-safe overload using domain [MaterialRequirement] models.
     */
    fun validateRequirementsSufficiency(
        requirements: List<MaterialRequirement>
    ): Boolean {
        return requirements.all { it.availableStock >= it.requiredQuantity }
    }
}
