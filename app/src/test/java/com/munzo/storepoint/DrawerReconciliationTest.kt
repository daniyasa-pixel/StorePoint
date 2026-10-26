package com.munzo.storepoint

import com.munzo.storepoint.data.CashierSession
import com.munzo.storepoint.data.ShiftReconciliation
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
    fun physicalDenominationCount_withZeroCounts() {
        val total = calculateTotalCountedCash(0, 0, 0, 0, 0, 0, 0.0)
        assertEquals(0.00, total, 0.0)
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
    fun discrepancyThreshold_triggersManagerOverrideRequirement() {
        val toleranceThreshold = 10.00 // 10 pesos discrepancy tolerance

        // Small 2 peso difference within tolerance
        val minorVariance = -2.00
        val requiresOverrideMinor = kotlin.math.abs(minorVariance) > toleranceThreshold
        assertFalse(requiresOverrideMinor)

        // 50 peso shortage requires manager override
        val majorShortage = -50.00
        val requiresOverrideMajor = kotlin.math.abs(majorShortage) > toleranceThreshold
        assertTrue(requiresOverrideMajor)

        // Verification guard logic:
        // Closure allowed if NO discrepancy OR manager override verified with reason
        fun canCloseShift(
            variance: Double,
            managerApproved: Boolean,
            reason: String
        ): Boolean {
            val hasDiscrepancy = kotlin.math.abs(variance) > toleranceThreshold
            return !hasDiscrepancy || (managerApproved && reason.isNotBlank())
        }

        assertFalse("Unapproved discrepancy must block closure", canCloseShift(majorShortage, false, ""))
        assertFalse("Approved discrepancy without explanatory note must block closure", canCloseShift(majorShortage, true, ""))
        assertTrue("Approved discrepancy with note is allowed", canCloseShift(majorShortage, true, "Customer left without change, recorded"))
        assertTrue("Zero discrepancy is allowed directly", canCloseShift(0.0, false, ""))
    }

    @Test
    fun shiftReconciliationEntity_buildsExpectedFields() {
        val recon = ShiftReconciliation(
            sessionId = 12,
            cashierUsername = "john_cashier",
            timestamp = 1700000000000L,
            count1000 = 2,
            count500 = 1,
            count200 = 2,
            count100 = 5,
            count50 = 2,
            count20 = 5,
            countCoins = 25.50,
            totalCountedCash = 3525.50,
            expectedCash = 3525.50,
            cashVariance = 0.0,
            countedGCash = 5000.0,
            expectedGCash = 5000.0,
            gcashVariance = 0.0,
            countedSmartLoad = 1000.0,
            expectedSmartLoad = 1000.0,
            smartLoadVariance = 0.0,
            countedGlobeLoad = 1500.0,
            expectedGlobeLoad = 1500.0,
            globeLoadVariance = 0.0,
            managerOverrideApproved = false,
            managerUsername = "",
            discrepancyReason = ""
        )

        assertEquals(12, recon.sessionId)
        assertEquals("john_cashier", recon.cashierUsername)
        assertEquals(3525.50, recon.totalCountedCash, 0.0)
        assertEquals(0.0, recon.cashVariance, 0.0)
        assertFalse(recon.managerOverrideApproved)
    }

    @Test
    fun buildShiftReconciliationReceipt_containsReconciliationDetails() {
        val recon = ShiftReconciliation(
            sessionId = 5,
            cashierUsername = "maria_s",
            timestamp = 1700000000000L,
            count1000 = 1,
            count500 = 2,
            count200 = 0,
            count100 = 3,
            count50 = 0,
            count20 = 5,
            countCoins = 15.00,
            totalCountedCash = 2415.00,
            expectedCash = 2400.00,
            cashVariance = 15.00, // +15 overage
            countedGCash = 3000.0,
            expectedGCash = 3000.0,
            gcashVariance = 0.0,
            countedSmartLoad = 500.0,
            expectedSmartLoad = 500.0,
            smartLoadVariance = 0.0,
            countedGlobeLoad = 700.0,
            expectedGlobeLoad = 700.0,
            globeLoadVariance = 0.0,
            hasDiscrepancy = true,
            managerOverrideApproved = true,
            managerUsername = "admin_boss",
            discrepancyReason = "Customer tipped coin change"
        )

        val storeConfig = com.munzo.storepoint.data.StoreConfig(
            storeName = "StorePoint Supermart",
            currencySymbol = "₱"
        )
        val bytes = EscPosHelper.buildShiftReconciliationReceipt(
            storeConfig = storeConfig,
            reconciliation = recon,
            startTime = 1699960000000L,
            is80mm = true,
            hasAutoCutter = true
        )

        assertTrue("Receipt bytes should be generated", bytes.isNotEmpty())
        val receiptText = String(bytes, Charsets.ISO_8859_1)
        assertTrue("Should contain store name", receiptText.contains("StorePoint Supermart"))
        assertTrue("Should contain cashier username", receiptText.contains("maria_s"))
        assertTrue("Should contain shift handover header", receiptText.contains("SHIFT HANDOVER RECONCILIATION"))
        assertTrue("Should contain manager override info", receiptText.contains("admin_boss"))
        assertTrue("Should contain explanation", receiptText.contains("Customer tipped coin change"))
    }
}

