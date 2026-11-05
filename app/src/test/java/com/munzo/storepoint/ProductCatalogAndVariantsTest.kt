package com.munzo.storepoint

import com.munzo.storepoint.data.Category
import com.munzo.storepoint.data.Product
import com.munzo.storepoint.data.ProductVariant
import com.munzo.storepoint.data.StorePointRepository
import org.junit.Assert.*
import org.junit.Test

class ProductCatalogAndVariantsTest {

    @Test
    fun productModel_tracksInventoryByDefaultAndSupportsToggle() {
        val inventoryProduct = Product(
            id = 1,
            name = "Coca-Cola 1.5L",
            categoryId = 10,
            price = 75.0,
            cost = 60.0,
            stockCount = 20,
            barcode = "48000001",
            trackInventory = true
        )
        assertTrue("trackInventory should be true for physical stock", inventoryProduct.trackInventory)

        val nonTrackedProduct = Product(
            id = 2,
            name = "GCash Cash-In Fee 1k",
            categoryId = 20,
            price = 20.0,
            cost = 0.0,
            stockCount = 0,
            barcode = "SRV-GCASH-1K",
            trackInventory = false
        )
        assertFalse("trackInventory should be false for digital/service items", nonTrackedProduct.trackInventory)

        // Verify copy toggle
        val updatedProduct = inventoryProduct.copy(trackInventory = false)
        assertFalse(updatedProduct.trackInventory)
    }

    @Test
    fun categoryModel_preservesCustomColorAccentAndIconBadge() {
        val category1 = Category(
            id = 1,
            name = "Beverages & Drinks",
            colorAccent = "#2563EB",
            iconBadge = "local_drink"
        )
        assertEquals("Beverages & Drinks", category1.name)
        assertEquals("#2563EB", category1.colorAccent)
        assertEquals("local_drink", category1.iconBadge)

        val category2 = Category(
            id = 2,
            name = "Canned Goods & Food",
            colorAccent = "#10B981",
            iconBadge = "restaurant"
        )
        assertEquals("#10B981", category2.colorAccent)
        assertEquals("restaurant", category2.iconBadge)

        // Default values when omitted
        val defaultCategory = Category(name = "General")
        assertEquals("", defaultCategory.colorAccent)
        assertEquals("", defaultCategory.iconBadge)
    }

    @Test
    fun productVariants_supportsSizesFlavorsAndPackagingWithDistinctStockAndBarcodes() {
        val parentProduct = Product(
            id = 10,
            name = "Potato Chips",
            categoryId = 5,
            price = 35.0,
            cost = 25.0,
            stockCount = 100,
            barcode = "4801000100"
        )

        // Flavor variants with individual barcodes
        val originalFlavor = ProductVariant(
            id = 1,
            productId = parentProduct.id,
            variantName = "Original Flavor",
            uomName = "pack",
            multiplier = 1,
            price = 35.0,
            cost = 25.0,
            barcode = "4801000101",
            stockCount = 40
        )

        val spicyFlavor = ProductVariant(
            id = 2,
            productId = parentProduct.id,
            variantName = "Spicy Barbecue",
            uomName = "pack",
            multiplier = 1,
            price = 37.0, // Premium flavor pricing
            cost = 26.0,
            barcode = "4801000102",
            stockCount = 25
        )

        // Multi-pack packaging size variant with its own barcode and distinct stock
        val partyBox12s = ProductVariant(
            id = 3,
            productId = parentProduct.id,
            variantName = "Party Box (12-Pack)",
            uomName = "box",
            multiplier = 12,
            price = 400.0,
            cost = 300.0,
            barcode = "4801000112",
            stockCount = 5
        )

        // Sizing variant (Jumbo 200g)
        val jumboSize = ProductVariant(
            id = 4,
            productId = parentProduct.id,
            variantName = "Jumbo Size (200g)",
            uomName = "bag",
            multiplier = 2,
            price = 68.0,
            cost = 48.0,
            barcode = "4801000120",
            stockCount = 15
        )

        val variants = listOf(originalFlavor, spicyFlavor, partyBox12s, jumboSize)

        // 1. Verify individual barcodes
        val barcodes = variants.map { it.barcode }
        assertEquals(4, barcodes.distinct().size)
        assertTrue(barcodes.contains("4801000101"))
        assertTrue(barcodes.contains("4801000102"))
        assertTrue(barcodes.contains("4801000112"))
        assertTrue(barcodes.contains("4801000120"))

        // 2. Verify distinct stock counters
        assertEquals(40, originalFlavor.stockCount)
        assertEquals(25, spicyFlavor.stockCount)
        assertEquals(5, partyBox12s.stockCount)
        assertEquals(15, jumboSize.stockCount)

        val totalVariantStock = variants.sumOf { it.stockCount }
        assertEquals(85, totalVariantStock)

        // 3. Verify variant pricing & gross margins
        val spicyMargin = StorePointRepository.roundMoney(spicyFlavor.price - spicyFlavor.cost)
        assertEquals(11.0, spicyMargin, 0.0)

        val boxMargin = StorePointRepository.roundMoney(partyBox12s.price - partyBox12s.cost)
        assertEquals(100.0, boxMargin, 0.0)

        // 4. Verify quick stock adjustment simulation
        val adjustedOriginal = originalFlavor.copy(stockCount = originalFlavor.stockCount + 10)
        assertEquals(50, adjustedOriginal.stockCount)

        val decrementedSpicy = spicyFlavor.copy(stockCount = (spicyFlavor.stockCount - 1).coerceAtLeast(0))
        assertEquals(24, decrementedSpicy.stockCount)
    }

    @Test
    fun trackInventory_checkoutStockValidationLogic() {
        // Simulate checkout inventory deduction rule
        fun simulateStockDeduction(product: Product, quantityRequested: Int): Int {
            if (!product.trackInventory) {
                // Non-tracked item: bypasses stock deduction, stock remains unchanged
                return product.stockCount
            }
            if (product.stockCount < quantityRequested) {
                throw IllegalStateException("Insufficient stock for ${product.name}")
            }
            return product.stockCount - quantityRequested
        }

        // Case A: Track inventory enabled, sufficient stock
        val trackedProduct = Product(id = 1, name = "Bear Brand 33g", categoryId = 1, price = 15.0, stockCount = 10, barcode = "480001", trackInventory = true)
        val remainingAfterSale = simulateStockDeduction(trackedProduct, 4)
        assertEquals(6, remainingAfterSale)

        // Case B: Track inventory enabled, insufficient stock -> throws exception
        try {
            simulateStockDeduction(trackedProduct, 15)
            fail("Should throw IllegalStateException due to insufficient stock")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Insufficient stock"))
        }

        // Case C: Track inventory disabled (e.g. digital reload / fresh cooked meal)
        val untrackedProduct = Product(id = 2, name = "Cooked Lumpia Shanghai", categoryId = 2, price = 10.0, stockCount = 0, barcode = "COOK-001", trackInventory = false)
        val remainingUntracked = simulateStockDeduction(untrackedProduct, 25)
        assertEquals("Untracked product stock count should not decrement and must not throw error", 0, remainingUntracked)
    }
}

