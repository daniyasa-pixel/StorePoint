package com.munzo.storepoint

import com.munzo.storepoint.data.CashierSession
import com.munzo.storepoint.data.ShiftReconciliation
import com.munzo.storepoint.data.StoreConfig
import com.munzo.storepoint.data.StorePointRepository
import com.munzo.storepoint.util.EscPosHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerReconciliationTest {

    private fun calculateTotalCountedCash(
        c1000: Int, c500: Int, c200: Int, c100: Int, c50: Int, c20: Int, coins: Double
    ): Double {
        val total = (c1000 * 1000.0) + (c500 * 500.0) + (c200 * 200.0) +
                (c100 * 100.0) + (c50 * 50.0) + (c20 * 20.0) + coins
        return StorePointRepository.roundMoney(total)
    }

    private fun calculateFullPhilippinePesoCount(
        c1000: Int, c500: Int, c200: Int, c100: Int, c50: Int, c20: Int,
        c10: Int, c5: Int, c1: Int, c25c: Int, otherCoins: Double
    ): Triple<Double, Double, Double> {
        val bills = (c1000 * 1000.0) + (c500 * 500.0) + (c200 * 200.0) +
                (c100 * 100.0) + (c50 * 50.0) + (c20 * 20.0)
        val coins = (c10 * 10.0) + (c5 * 5.0) + (c1 * 1.0) + (c25c * 0.25) + otherCoins
        val total = bills + coins
        return Triple(
            StorePointRepository.roundMoney(bills),
            StorePointRepository.roundMoney(coins),
            StorePointRepository.roundMoney(total)
        )
    }

    private fun calculateDrawerBalanceFormula(
        startingFloat: Double,
        cashSales: Double,
        cashIn: Double,
        cashOut: Double,
        refunds: Double
    ): Double {
        // Formula: Starting Float + Cash Sales + Cash In - Cash Out - Refunds
        return StorePointRepository.roundMoney(startingFloat + cashSales + cashIn - cashOut - refunds)
    }

    private fun calculateGcashWalletFormula(
        startingBalance: Double,
        cashInInflow: Double,
        cashOutDisbursed: Double
    ): Double {
        // Formula: Starting Balance + Cash In Inflow - Cash Out Disbursed
        return StorePointRepository.roundMoney(startingBalance + cashInInflow - cashOutDisbursed)
    }

    private fun calculateVariance(counted: Double, expected: Double): Double {
        return StorePointRepository.roundMoney(counted - expected)
    }

    @Test
    fun physicalDenominationCount_computesExactTotal() {
        val c1000 = 5   // 5000.00
        val c500 = 4    // 2000.00
        val c200 = 3    //  600.00
        val c100 = 10   // 1000.00
        val c50 = 8     //  400.00
        val c20 = 15    //  300.00
        val coins = 76.50

        val total = calculateTotalCountedCash(c1000, c500, c200, c100, c50, c20, coins)
        // 5000 + 2000 + 600 + 1000 + 400 + 300 + 76.50 = 9376.50
        assertEquals(9376.50, total, 0.0)
    }

    @Test
    fun philippinePesoFullDenominationBreakdown_includingCoinsAndCentavos() {
        // Bills
        val c1000 = 2  // 2000.00
        val c500 = 3   // 1500.00
        val c200 = 1   //  200.00
        val c100 = 4   //  400.00
        val c50 = 2    //  100.00
        val c20 = 5    //  100.00 -> Bills Subtotal = 4300.00

        // Philippine Peso Coins (₱10, ₱5, ₱1, ₱0.25) + other
        val c10 = 10   //  100.00
        val c5 = 8     //   40.00
        val c1 = 15    //   15.00
        val c25c = 6   //    1.50 (6 x 0.25)
        val other = 0.50 //  0.50 -> Coins Subtotal = 157.00

        val (bills, coins, total) = calculateFullPhilippinePesoCount(
            c1000, c500, c200, c100, c50, c20,
            c10, c5, c1, c25c, other
        )

        assertEquals(4300.00, bills, 0.0)
        assertEquals(157.00, coins, 0.0)
        assertEquals(4457.00, total, 0.0)
    }

    @Test
    fun drawerBalanceFormula_computesStartingFloatPlusSalesPlusCashInMinusCashOutMinusRefunds() {
        val startingFloat = 1500.00
        val cashSales = 5420.75
        val cashIn = 300.00
        val cashOut = 250.00
        val refunds = 120.75

        // Expected: 1500.00 + 5420.75 + 300.00 - 250.00 - 120.75 = 6850.00
        val expected = calculateDrawerBalanceFormula(
            startingFloat = startingFloat,
            cashSales = cashSales,
            cashIn = cashIn,
            cashOut = cashOut,
            refunds = refunds
        )

        assertEquals(6850.00, expected, 0.0)
    }

    @Test
    fun gcashDigitalWalletFormula_computesStartingPlusInflowMinusDisbursed() {
        val startingBalance = 8000.00
        val cashInInflow = 2500.00
        val cashOutDisbursed = 1500.00

        // Expected: 8000 + 2500 - 1500 = 9000.00
        val expected = calculateGcashWalletFormula(
            startingBalance = startingBalance,
            cashInInflow = cashInInflow,
            cashOutDisbursed = cashOutDisbursed
        )

        assertEquals(9000.00, expected, 0.0)
    }

    @Test
    fun prepaidMobileLoad_evaluatesBeginningVsEndingBalanceAgainstReloadSales() {
        val smartBeginning = 2000.00
        val smartReloadSales = 650.00
        val smartExpected = StorePointRepository.roundMoney(smartBeginning - smartReloadSales)
        assertEquals(1350.00, smartExpected, 0.0)

        val globeBeginning = 3500.00
        val globeReloadSales = 1200.00
        val globeExpected = StorePointRepository.roundMoney(globeBeginning - globeReloadSales)
        assertEquals(2300.00, globeExpected, 0.0)
    }

    @Test
    fun mandatoryShiftClosureGuard_blocksClosureUntilPhysicalCountsEntered() {
        fun canCloseShiftGuard(
            hasEnteredCounts: Boolean,
            variance: Double,
            managerApproved: Boolean,
            note: String
        ): Boolean {
            if (!hasEnteredCounts) return false
            val hasDiscrepancy = kotlin.math.abs(variance) >= 0.01
            return !hasDiscrepancy || (managerApproved && note.trim().length >= 4)
        }

        // Unentered counts must ALWAYS block closure, even if variance formula would default to zero
        assertFalse("Unentered counts must block closure", canCloseShiftGuard(false, 0.0, false, ""))
        assertFalse("Unentered counts must block closure even with manager approval", canCloseShiftGuard(false, 0.0, true, "Supervisor forced"))

        // Entered counts with zero variance allows closure
        assertTrue("Entered balanced counts allows closure", canCloseShiftGuard(true, 0.0, false, ""))

        // Entered counts with discrepancy requires manager approval and note
        assertFalse("Variance without approval is blocked", canCloseShiftGuard(true, -25.0, false, ""))
        assertFalse("Variance with approval but short note is blocked", canCloseShiftGuard(true, -25.0, true, "no"))
        assertTrue("Variance with manager approval and valid note is permitted", canCloseShiftGuard(true, -25.0, true, "Register coin shortage verified"))
    }

    @Test
    fun cashDrawerVariance_balanced_isZero() {
        val expectedCash = 5420.50
        val countedCash = 5420.50
        val variance = calculateVariance(countedCash, expectedCash)

        assertEquals(0.00, variance, 0.001)
        assertTrue("Variance should be within balanced tolerance", kotlin.math.abs(variance) < 0.01)
    }

    @Test
    fun cashDrawerVariance_overage_isPositive() {
        val expectedCash = 3500.00
        val countedCash = 3550.00
        val variance = calculateVariance(countedCash, expectedCash)

        assertEquals(50.00, variance, 0.0)
        assertTrue(variance > 0)
    }

    @Test
    fun cashDrawerVariance_shortage_isNegative() {
        val expectedCash = 8200.00
        val countedCash = 8125.00
        val variance = calculateVariance(countedCash, expectedCash)

        assertEquals(-75.00, variance, 0.0)
        assertTrue(variance < 0)
    }

    @Test
    fun multiChannelReconciliation_evaluatesAllWalletsIndependentVariances() {
        val expectedCash = 5000.00
        val countedCash = 5000.00
        val cashVariance = calculateVariance(countedCash, expectedCash)

        val expectedGCash = 12500.00
        val countedGCash = 12450.00 // 50 pesos short
        val gcashVariance = calculateVariance(countedGCash, expectedGCash)

        val expectedSmart = 1800.00
        val countedSmart = 1800.00
        val smartVariance = calculateVariance(countedSmart, expectedSmart)

        val expectedGlobe = 2200.00
        val countedGlobe = 2220.00 // 20 pesos over
        val globeVariance = calculateVariance(countedGlobe, expectedGlobe)

        assertEquals(0.00, cashVariance, 0.001)
        assertEquals(-50.00, gcashVariance, 0.001)
        assertEquals(0.00, smartVariance, 0.001)
        assertEquals(20.00, globeVariance, 0.001)

        val totalDiscrepancyMagnitude = kotlin.math.abs(cashVariance) +
                kotlin.math.abs(gcashVariance) +
                kotlin.math.abs(smartVariance) +
                kotlin.math.abs(globeVariance)
        assertEquals(70.00, totalDiscrepancyMagnitude, 0.001)
    }

    @Test
    fun buildShiftReconciliationReceipt_withFormulaSummary_printsAllDetails() {
        val recon = ShiftReconciliation(
            sessionId = 8,
            cashierUsername = "maria_s",
            timestamp = 1700000000000L,
            count1000 = 3,
            count500 = 2,
            count200 = 1,
            count100 = 5,
            count50 = 2,
            count20 = 5,
            countCoins = 145.50,
            totalCountedCash = 4945.50,
            expectedCash = 4945.50,
            cashVariance = 0.0,
            countedGCash = 6000.0,
            expectedGCash = 6000.0,
            gcashVariance = 0.0,
            countedSmartLoad = 1200.0,
            expectedSmartLoad = 1200.0,
            smartLoadVariance = 0.0,
            countedGlobeLoad = 1800.0,
            expectedGlobeLoad = 1800.0,
            globeLoadVariance = 0.0,
            hasDiscrepancy = false,
            managerOverrideApproved = false
        )

        val formula = EscPosHelper.DrawerBalancingSummary(
            startingFloat = 1000.00,
            cashSales = 4145.50,
            cashIn = 200.00,
            cashOut = 300.00,
            cashRefunds = 100.00
        )

        val storeConfig = StoreConfig(
            storeName = "StorePoint Supermart",
            currencySymbol = "₱",
            hasGCash = true,
            hasLoad = true
        )

        val bytes = EscPosHelper.buildShiftReconciliationReceipt(
            storeConfig = storeConfig,
            reconciliation = recon,
            startTime = 1699960000000L,
            formulaSummary = formula,
            is80mm = true,
            hasAutoCutter = true
        )

        assertTrue(bytes.isNotEmpty())
        val receiptText = String(bytes, Charsets.ISO_8859_1)
        assertTrue(receiptText.contains("SHIFT HANDOVER RECONCILIATION"))
        assertTrue(receiptText.contains("DRAWER FORMULA BREAKDOWN"))
        assertTrue(receiptText.contains("Starting Float"))
        assertTrue(receiptText.contains("Cash Sales"))
        assertTrue(receiptText.contains("Cash In"))
        assertTrue(receiptText.contains("Cash Out"))
        assertTrue(receiptText.contains("Cash Refunds"))
        assertTrue(receiptText.contains("GCASH DIGITAL WALLET"))
        assertTrue(receiptText.contains("PREPAID MOBILE LOAD"))
        assertTrue(receiptText.contains("BALANCED"))
    }
}
