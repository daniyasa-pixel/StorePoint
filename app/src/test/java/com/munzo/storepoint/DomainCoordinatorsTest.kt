package com.munzo.storepoint

import com.munzo.storepoint.data.CartItemDetails
import com.munzo.storepoint.data.Product
import com.munzo.storepoint.data.StoreConfig
import com.munzo.storepoint.data.Transaction
import com.munzo.storepoint.data.TransactionItem
import com.munzo.storepoint.domain.CashDrawerCoordinator
import com.munzo.storepoint.domain.HardwarePrinterCoordinator
import com.munzo.storepoint.domain.PosCheckoutCoordinator
import com.munzo.storepoint.domain.ProductionBomCoordinator
import org.junit.Assert.*
import org.junit.Test

class DomainCoordinatorsTest {

    // =========================================================================
    // 1. PosCheckoutCoordinator Tests
    // =========================================================================

    @Test
    fun posCheckoutCoordinator_calculatesLineTotalAndMultiUomDeduction() {
        val product = Product(id = 1, name = "Coke 1.5L", categoryId = 1, price = 75.0, stockCount = 20, barcode = "12345")
        val cartItem = CartItemDetails(product = product, quantity = 3, uomName = "pc", multiplier = 1, selectedPrice = 75.0)

        assertEquals(225.0, PosCheckoutCoordinator.computeLineTotal(cartItem), 0.0)
        assertEquals(3, PosCheckoutCoordinator.computeBaseUnitsNeeded(cartItem))

        // Multi-UOM Pack (Case of 6)
        val caseItem = CartItemDetails(product = product, quantity = 2, uomName = "Case (6s)", multiplier = 6, selectedPrice = 420.0)
        assertEquals(840.0, PosCheckoutCoordinator.computeLineTotal(caseItem), 0.0)
        assertEquals(12, PosCheckoutCoordinator.computeBaseUnitsNeeded(caseItem))
    }

    @Test
    fun posCheckoutCoordinator_calculatesVatAndDiscounts() {
        val product = Product(id = 1, name = "Item A", categoryId = 1, price = 100.0, stockCount = 10, barcode = "111")
        val items = listOf(CartItemDetails(product = product, quantity = 2, uomName = "pc", multiplier = 1, selectedPrice = 100.0))

        // Standard 10% discount
        val totals = PosCheckoutCoordinator.computeCheckoutTotals(items, discountType = "PERCENTAGE", discountValue = 10.0)
        assertEquals(200.0, totals.subtotal, 0.01)
        assertEquals(20.0, totals.discountAmount, 0.01)
        assertEquals(180.0, totals.netTotal, 0.01)
        // 180 / 1.12 = 160.71 vatable, 19.29 vat
        assertEquals(160.71, totals.vatableSales, 0.01)
        assertEquals(19.29, totals.vatAmount, 0.01)
        assertEquals(0.0, totals.vatExemptSales, 0.01)

        // Senior/PWD statutory BIR calculation (RA 9994/RA 10754):
        // For ₱112.00 gross VAT-inclusive (₱100 base + ₱12 VAT):
        val seniorItem = listOf(CartItemDetails(product = product, quantity = 1, uomName = "pc", multiplier = 1, selectedPrice = 112.0))
        val seniorTotals = PosCheckoutCoordinator.computeCheckoutTotals(seniorItem, discountType = "SENIOR_PWD")
        assertEquals(112.0, seniorTotals.subtotal, 0.01)
        assertEquals(20.0, seniorTotals.discountAmount, 0.01) // 20% of ₱100 base = ₱20.00
        assertEquals(80.0, seniorTotals.netTotal, 0.01)       // Senior pays ₱80.00
        assertEquals(0.0, seniorTotals.vatAmount, 0.01)
        assertEquals(80.0, seniorTotals.vatExemptSales, 0.01)

        // Type-safe enum invocation
        val enumTotals = PosCheckoutCoordinator.computeCheckoutTotals(
            seniorItem,
            type = PosCheckoutCoordinator.DiscountType.SENIOR_PWD
        )
        assertEquals(seniorTotals, enumTotals)
    }

    @Test
    fun posCheckoutCoordinator_handlesSplitTender() {
        val netTotal = 450.00
        val nonCashGcash = 200.00
        val cashTendered = 300.00

        val split = PosCheckoutCoordinator.computeSplitPayment(netTotal, nonCashGcash, cashTendered)
        assertEquals(200.00, split.nonCashAmount, 0.0)
        assertEquals(250.00, split.cashRequired, 0.0)
        assertEquals(300.00, split.cashPaid, 0.0)
        assertEquals(50.00, split.changeAmount, 0.0)
        assertTrue(split.isFullySettled)
    }

    // =========================================================================
    // 2. CashDrawerCoordinator Tests
    // =========================================================================

    @Test
    fun cashDrawerCoordinator_computesExpectedBalanceAndReconciles() {
        val startingFloat = 1000.0
        val cashSales = 2500.0
        val cashIn = 200.0
        val cashOut = 100.0
        val cashRefunds = 150.0

        val expected = CashDrawerCoordinator.computeExpectedEndingCash(startingFloat, cashSales, cashIn, cashOut, cashRefunds)
        // 1000 + 2500 + 200 - 100 - 150 = 3450.00
        assertEquals(3450.00, expected, 0.0)

        // Exact physical count
        val denominations = CashDrawerCoordinator.DenominationCount(
            count1000 = 3, // 3000
            count200 = 2,  // 400
            count50 = 1,   // 50
            countCoins = 0.0
        )
        assertEquals(3450.00, denominations.totalCounted, 0.0)

        val result = CashDrawerCoordinator.reconcileShift(
            startingFloat, cashSales, cashIn, cashOut, cashRefunds, denominations
        )
        assertFalse(result.hasDiscrepancy)
        assertEquals(0.0, result.cashVariance, 0.0)
        assertFalse(result.requiresManagerOverride)
    }

    @Test
    fun cashDrawerCoordinator_flagsDiscrepancyRequiringOverride() {
        val startingFloat = 1000.0
        val cashSales = 500.0
        val expected = 1500.0

        // Physical count is ₱1,400.00 (short by ₱100.00)
        val shortDenominations = CashDrawerCoordinator.DenominationCount(
            count1000 = 1,
            count200 = 2
        )
        val result = CashDrawerCoordinator.reconcileShift(
            startingFloat, cashSales, denominations = shortDenominations, overrideThreshold = 50.0
        )
        assertTrue(result.hasDiscrepancy)
        assertEquals(-100.0, result.cashVariance, 0.0)
        assertTrue(result.requiresManagerOverride)
    }

    // =========================================================================
    // 3. ProductionBomCoordinator Tests
    // =========================================================================

    @Test
    fun productionBomCoordinator_computesDerivedCostAndMargin() {
        // Raw material: ₱100.00 per unit, 2 units used, produces 5 finished items with 5% scrap
        val derivedCost = ProductionBomCoordinator.computeDerivedUnitCost(
            rawUnitCost = 100.0,
            batchUsage = 2.0,
            producedUnits = 5.0,
            scrapPercentage = 5.0
        )
        // 100 * (2 * 1.05) = 210 / 5 = 42.00
        assertEquals(42.00, derivedCost, 0.0)

        val analysis = ProductionBomCoordinator.analyzeCostAndMargin(
            unitCost = 42.00,
            sellingPrice = 70.00
        )
        assertEquals(28.00, analysis.grossProfit, 0.0)
        assertEquals(40.00, analysis.marginPercentage, 0.0) // 28 / 70 = 40%
    }

    @Test
    fun productionBomCoordinator_validatesSufficiency() {
        val sufficientRequirements = listOf(
            Pair(10.0, 15.0),
            Pair(5.0, 5.0)
        )
        assertTrue(ProductionBomCoordinator.validateRawMaterialSufficiency(sufficientRequirements))

        val insufficientRequirements = listOf(
            Pair(10.0, 8.0),
            Pair(5.0, 10.0)
        )
        assertFalse(ProductionBomCoordinator.validateRawMaterialSufficiency(insufficientRequirements))

        // Typed MaterialRequirement overload
        val typedList = listOf(
            ProductionBomCoordinator.MaterialRequirement(1, "Sugar", 5.0, 10.0, true),
            ProductionBomCoordinator.MaterialRequirement(2, "Flour", 20.0, 50.0, true)
        )
        assertTrue(ProductionBomCoordinator.validateRequirementsSufficiency(typedList))
    }

    // =========================================================================
    // 4. HardwarePrinterCoordinator Tests
    // =========================================================================

    @Test
    fun hardwarePrinterCoordinator_preparesReceiptAndDrawerKick() {
        val storeConfig = StoreConfig(storeName = "Test Store", currencySymbol = "₱")
        val tx = Transaction(id = 1, timestamp = 1000L, cashierUsername = "cashier1", subtotal = 100.0, taxAmount = 12.0, totalAmount = 112.0, paymentMethod = "CASH", cashPaid = 120.0, changeAmount = 8.0)
        val items = listOf(TransactionItem(id = 1, transactionId = 1, productId = 1, productName = "Item", price = 100.0, quantity = 1))

        val bytes58mm = HardwarePrinterCoordinator.prepareReceiptPayload(
            storeConfig, tx, items, HardwarePrinterCoordinator.PaperWidth.WIDTH_58MM
        )
        assertNotNull(bytes58mm)
        assertTrue(bytes58mm.size > 50)

        val kickBytes = HardwarePrinterCoordinator.prepareCashDrawerKickSignal()
        assertNotNull(kickBytes)
        assertTrue(kickBytes.size >= 8)
    }
}
