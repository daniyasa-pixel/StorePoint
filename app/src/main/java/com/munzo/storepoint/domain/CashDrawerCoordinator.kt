package com.munzo.storepoint.domain

import com.munzo.storepoint.data.StorePointRepository

/**
 * Domain Coordinator for Cash Drawer Sessions, Denomination Breakdown, and Blind Shift Reconciliation.
 *
 * Implements strict Philippine cash drawer audit standards:
 * - Independent physical denomination bill & coin counting
 * - Expected cash derivation: Starting Float + Cash Sales + Cash In - Cash Out - Refunds
 * - Blind cashier balancing: Cashiers submit physical counts without knowing expected amounts
 * - Discrepancy / Variance audit thresholds
 */
object CashDrawerCoordinator {

    data class DenominationCount(
        val count1000: Int = 0,
        val count500: Int = 0,
        val count200: Int = 0,
        val count100: Int = 0,
        val count50: Int = 0,
        val count20: Int = 0,
        val countCoins: Double = 0.0
    ) {
        val totalCounted: Double
            get() = StorePointRepository.roundMoney(
                (count1000 * 1000.0) +
                (count500 * 500.0) +
                (count200 * 200.0) +
                (count100 * 100.0) +
                (count50 * 50.0) +
                (count20 * 20.0) +
                countCoins
            )
    }

    data class DrawerReconciliationResult(
        val startingFloat: Double,
        val cashSales: Double,
        val cashIn: Double,
        val cashOut: Double,
        val cashRefunds: Double,
        val expectedEndingCash: Double,
        val actualCountedCash: Double,
        val cashVariance: Double,
        val hasDiscrepancy: Boolean,
        val requiresManagerOverride: Boolean
    )

    /**
     * Computes the mathematical expected cash in drawer at shift close.
     */
    fun computeExpectedEndingCash(
        startingFloat: Double,
        cashSales: Double,
        cashIn: Double = 0.0,
        cashOut: Double = 0.0,
        cashRefunds: Double = 0.0
    ): Double {
        val balance = startingFloat + cashSales + cashIn - cashOut - cashRefunds
        return StorePointRepository.roundMoney(balance)
    }

    /**
     * Performs blind reconciliation between counted cash and expected cash balance.
     * Flagged as a discrepancy if variance is beyond zero tolerance (±₱0.01).
     * Flagged as requiring manager override if absolute discrepancy exceeds ₱50.00.
     */
    fun reconcileShift(
        startingFloat: Double,
        cashSales: Double,
        cashIn: Double = 0.0,
        cashOut: Double = 0.0,
        cashRefunds: Double = 0.0,
        denominations: DenominationCount,
        overrideThreshold: Double = 50.0
    ): DrawerReconciliationResult {
        val expected = computeExpectedEndingCash(startingFloat, cashSales, cashIn, cashOut, cashRefunds)
        val counted = denominations.totalCounted
        val variance = StorePointRepository.roundMoney(counted - expected)
        val hasDiscrepancy = kotlin.math.abs(variance) >= 0.01
        val requiresManagerOverride = kotlin.math.abs(variance) > overrideThreshold

        return DrawerReconciliationResult(
            startingFloat = startingFloat,
            cashSales = cashSales,
            cashIn = cashIn,
            cashOut = cashOut,
            cashRefunds = cashRefunds,
            expectedEndingCash = expected,
            actualCountedCash = counted,
            cashVariance = variance,
            hasDiscrepancy = hasDiscrepancy,
            requiresManagerOverride = requiresManagerOverride
        )
    }
}
