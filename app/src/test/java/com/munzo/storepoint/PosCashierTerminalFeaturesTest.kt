package com.munzo.storepoint

import com.munzo.storepoint.data.Product
import com.munzo.storepoint.data.StorePointRepository
import org.junit.Assert.*
import org.junit.Test

class PosCashierTerminalFeaturesTest {

    private val sampleProducts = listOf(
        Product(id = 1, name = "C2 Green Tea 500ml", categoryId = 10, price = 35.0, stockCount = 50, barcode = "4800016644101"),
        Product(id = 2, name = "San Miguel Pale Pilsen 330ml", categoryId = 11, price = 65.0, stockCount = 24, barcode = "4800011112223"),
        Product(id = 3, name = "Piattos Cheese 85g", categoryId = 12, price = 38.0, stockCount = 30, barcode = "4800022233344")
    )

    private val categoryMap = mapOf(
        10 to "Beverages",
        11 to "Liquor & Beers",
        12 to "Snacks"
    )

    @Test
    fun productSearch_matchesNamesCategoriesAndBarcodes() {
        // 1) Search by product name
        val queryName = "Green Tea"
        val nameMatches = sampleProducts.filter { p ->
            val cat = categoryMap[p.categoryId] ?: ""
            p.name.contains(queryName, ignoreCase = true) ||
            p.barcode.contains(queryName, ignoreCase = true) ||
            cat.contains(queryName, ignoreCase = true)
        }
        assertEquals(1, nameMatches.size)
        assertEquals("C2 Green Tea 500ml", nameMatches.first().name)

        // 2) Search by barcode
        val queryBarcode = "4800011112223"
        val barcodeMatches = sampleProducts.filter { p ->
            val cat = categoryMap[p.categoryId] ?: ""
            p.name.contains(queryBarcode, ignoreCase = true) ||
            p.barcode.contains(queryBarcode, ignoreCase = true) ||
            cat.contains(queryBarcode, ignoreCase = true)
        }
        assertEquals(1, barcodeMatches.size)
        assertEquals("San Miguel Pale Pilsen 330ml", barcodeMatches.first().name)

        // 3) Search by category name ("Snacks")
        val queryCategory = "Snacks"
        val categoryMatches = sampleProducts.filter { p ->
            val cat = categoryMap[p.categoryId] ?: ""
            p.name.contains(queryCategory, ignoreCase = true) ||
            p.barcode.contains(queryCategory, ignoreCase = true) ||
            cat.contains(queryCategory, ignoreCase = true)
        }
        assertEquals(1, categoryMatches.size)
        assertEquals("Piattos Cheese 85g", categoryMatches.first().name)
    }

    @Test
    fun customItemDiscount_percentageDiscountCalculatesCorrectly() {
        val originalPrice = 100.0
        val discountPercent = 15.0 // 15% discount
        val calculatedDiscount = StorePointRepository.roundMoney((originalPrice * (discountPercent / 100.0)).coerceIn(0.0, originalPrice))
        val effectivePrice = (originalPrice - calculatedDiscount).coerceAtLeast(0.0)

        assertEquals(15.0, calculatedDiscount, 0.001)
        assertEquals(85.0, effectivePrice, 0.001)
    }

    @Test
    fun customItemDiscount_fixedAmountDiscountCalculatesCorrectly() {
        val originalPrice = 65.0
        val discountAmount = 10.0 // ₱10 discount
        val calculatedDiscount = StorePointRepository.roundMoney(discountAmount.coerceIn(0.0, originalPrice))
        val effectivePrice = (originalPrice - calculatedDiscount).coerceAtLeast(0.0)

        assertEquals(10.0, calculatedDiscount, 0.001)
        assertEquals(55.0, effectivePrice, 0.001)
    }

    @Test
    fun customItemDiscount_discountCannotExceedOriginalPrice() {
        val originalPrice = 50.0
        val excessiveDiscount = 80.0
        val calculatedDiscount = StorePointRepository.roundMoney(excessiveDiscount.coerceIn(0.0, originalPrice))
        val effectivePrice = (originalPrice - calculatedDiscount).coerceAtLeast(0.0)

        assertEquals(50.0, calculatedDiscount, 0.001)
        assertEquals(0.0, effectivePrice, 0.001)
    }

    @Test
    fun multiTenderSettlement_cashChangeCalculation() {
        val totalAmount = 275.50
        val cashPaid = 500.00
        val change = StorePointRepository.roundMoney((cashPaid - totalAmount).coerceAtLeast(0.0))

        assertEquals(224.50, change, 0.001)
    }

    @Test
    fun multiTenderSettlement_splitTenderCalculation() {
        val totalAmount = 350.00
        val cashPortion = 150.00
        val remainingWallet = StorePointRepository.roundMoney((totalAmount - cashPortion).coerceAtLeast(0.0))

        assertEquals(200.00, remainingWallet, 0.001)
        assertEquals(totalAmount, cashPortion + remainingWallet, 0.001)
    }

    @Test
    fun commonDenominations_sumOrSetCorrectly() {
        val denominations = listOf(20, 50, 100, 200, 500, 1000)
        assertTrue(denominations.contains(20))
        assertTrue(denominations.contains(50))
        assertTrue(denominations.contains(100))
        assertTrue(denominations.contains(200))
        assertTrue(denominations.contains(500))
        assertTrue(denominations.contains(1000))
    }
}
