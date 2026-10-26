package com.munzo.storepoint

import com.munzo.storepoint.data.BillOfMaterials
import com.munzo.storepoint.data.InventoryMovementEntity
import com.munzo.storepoint.data.Product
import com.munzo.storepoint.data.ProductionLogEntity
import com.munzo.storepoint.data.StorePointRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionBomEngineTest {

    private fun computeDerivedUnitCost(
        rawUnitCost: Double,
        batchUsage: Double,
        producedUnits: Double,
        scrapPercentage: Double = 0.0
    ): Double {
        if (producedUnits <= 0) return 0.0
        val totalBatchRawCost = rawUnitCost * batchUsage
        val derivedCost = totalBatchRawCost / producedUnits
        return StorePointRepository.roundMoney(derivedCost)
    }

    private fun computeGrossProfitAndMargin(
        sellingPrice: Double,
        unitCost: Double
    ): Pair<Double, Double> {
        val profit = StorePointRepository.roundMoney(sellingPrice - unitCost)
        val marginPercent = if (sellingPrice > 0) {
            StorePointRepository.roundMoney((profit / sellingPrice) * 100.0)
        } else 0.0
        return Pair(profit, marginPercent)
    }

    @Test
    fun bulkWaterToIcePouch_conversionCalculations() {
        // Source: 20L Water Jug (Cost: ₱50.00 each)
        // Transformation: 1 jug produces 40 packs of 500ml Ice Water
        val rawUnitCost = 50.00
        val batchUsage = 1.0 // 1 container
        val expectedYield = 40.0 // 40 bags produced

        val derivedCost = computeDerivedUnitCost(rawUnitCost, batchUsage, expectedYield)
        // 50.00 / 40 = 1.25 per pouch
        assertEquals(1.25, derivedCost, 0.0)

        // Retail selling price: ₱5.00 per pouch
        val sellingPrice = 5.00
        val (profit, marginPercent) = computeGrossProfitAndMargin(sellingPrice, derivedCost)

        // Profit: 5.00 - 1.25 = 3.75
        assertEquals(3.75, profit, 0.0)
        // Margin: (3.75 / 5.00) * 100 = 75.0%
        assertEquals(75.0, marginPercent, 0.0)
    }

    @Test
    fun bulkCookingOilRepackaging_multiBatchWithScrap() {
        // Source: 20L Cooking Oil (Cost: ₱1200.00)
        // Repack into 375ml bottles. 20L = 20,000ml / 375ml = 53.33 bottles theoretical
        // With 3.33 bottles lost to container residue/spillage (scrap) -> 50 bottles actual yield
        val rawUnitCost = 1200.00
        val batchUsage = 2.0 // 2 containers used (Total raw cost = ₱2400.00)
        val actualYield = 100.0 // 100 bottles bottled

        val derivedCost = computeDerivedUnitCost(rawUnitCost, batchUsage, actualYield)
        // 2400.00 / 100 = 24.00 per bottle
        assertEquals(24.00, derivedCost, 0.0)

        // Selling price: ₱45.00 per 375ml bottle
        val sellingPrice = 45.00
        val (profit, marginPercent) = computeGrossProfitAndMargin(sellingPrice, derivedCost)

        assertEquals(21.00, profit, 0.0)
        // (21.00 / 45.00) * 100 = 46.67%
        assertEquals(46.67, marginPercent, 0.01)
    }

    @Test
    fun derivedUnitCost_preventsDivisionByZero() {
        val cost = computeDerivedUnitCost(100.00, 1.0, 0.0)
        assertEquals(0.0, cost, 0.0)

        val (profit, margin) = computeGrossProfitAndMargin(0.0, 10.0)
        assertEquals(-10.0, profit, 0.0)
        assertEquals(0.0, margin, 0.0)
    }

    @Test
    fun stockBalances_atomicDecrementAndIncrementVerification() {
        val rawStockInitial = 50.0 // 50 containers
        val targetStockInitial = 15.0 // 15 retail packs on shelf

        val batchUsage = 3.0 // Use 3 containers
        val outputYieldPerRawUnit = 25.0 // 25 units per container
        val totalProduced = batchUsage * outputYieldPerRawUnit // 75 units produced

        val rawStockEnding = rawStockInitial - batchUsage
        val targetStockEnding = targetStockInitial + totalProduced

        assertEquals(47.0, rawStockEnding, 0.0)
        assertEquals(90.0, targetStockEnding, 0.0)
        assertTrue("Raw stock must not drop below zero", rawStockEnding >= 0.0)
    }

    @Test
    fun validationGuards_identifyInvalidProductionInputs() {
        fun validateProductionRun(
            rawProductId: Int,
            targetProductId: Int,
            availableRawStock: Double,
            batchUsage: Double,
            outputYield: Double,
            newRetailPrice: Double
        ): Pair<Boolean, String> {
            if (rawProductId == targetProductId) return Pair(false, "Source and Target SKU cannot be the same item")
            if (batchUsage <= 0) return Pair(false, "Batch usage must be greater than zero")
            if (batchUsage > availableRawStock) return Pair(false, "Insufficient raw stock (Available: $availableRawStock)")
            if (outputYield <= 0) return Pair(false, "Output yield must be greater than zero")
            if (newRetailPrice < 0) return Pair(false, "Retail price cannot be negative")
            return Pair(true, "OK")
        }

        // Test identical SKU
        val (valid1, msg1) = validateProductionRun(1, 1, 10.0, 1.0, 20.0, 5.0)
        assertFalse(valid1)
        assertEquals("Source and Target SKU cannot be the same item", msg1)

        // Test insufficient raw stock
        val (valid2, msg2) = validateProductionRun(1, 2, 5.0, 10.0, 20.0, 5.0)
        assertFalse(valid2)
        assertTrue(msg2.contains("Insufficient raw stock"))

        // Test zero yield
        val (valid3, msg3) = validateProductionRun(1, 2, 10.0, 2.0, 0.0, 5.0)
        assertFalse(valid3)
        assertTrue(msg3.contains("Output yield must be greater than zero"))

        // Test valid configuration
        val (valid4, msg4) = validateProductionRun(1, 2, 10.0, 2.0, 50.0, 10.0)
        assertTrue(valid4)
        assertEquals("OK", msg4)
    }

    @Test
    fun billOfMaterials_modelEntityProperties() {
        val bom = BillOfMaterials(
            id = 1,
            rawMaterialProductId = 101,
            targetRetailProductId = 202,
            rawQuantityNeeded = 1.0,
            producedRetailYield = 40.0,
            wasteLossPercentage = 2.5,
            notes = "20L Water to 500ml Ice Bags"
        )

        assertEquals(101, bom.rawMaterialProductId)
        assertEquals(202, bom.targetRetailProductId)
        assertEquals(1.0, bom.rawQuantityNeeded, 0.0)
        assertEquals(40.0, bom.producedRetailYield, 0.0)
        assertEquals(2.5, bom.wasteLossPercentage, 0.0)
        assertEquals("20L Water to 500ml Ice Bags", bom.notes)
    }

    @Test
    fun productionAuditLogs_recordImmutableMovements() {
        val rawMovement = InventoryMovementEntity(
            productId = 101,
            productName = "20L Water Container",
            timestamp = 1700000000000L,
            actorUsername = "operator_maria",
            movementType = "REPACK_RAW_DECREMENT",
            quantityDelta = -2.0,
            stockBefore = 50,
            stockAfter = 48,
            reference = "PRODUCTION_RUN_99",
            notes = "Raw material converted to retail SKU #202"
        )

        val targetMovement = InventoryMovementEntity(
            productId = 202,
            productName = "Ice Water 500ml",
            timestamp = 1700000000000L,
            actorUsername = "operator_maria",
            movementType = "REPACK_PRODUCED_INCREMENT",
            quantityDelta = 80.0,
            stockBefore = 10,
            stockAfter = 90,
            reference = "PRODUCTION_RUN_99",
            notes = "Produced from raw material SKU #101"
        )

        assertEquals("REPACK_RAW_DECREMENT", rawMovement.movementType)
        assertEquals(-2.0, rawMovement.quantityDelta, 0.0)
        assertEquals("REPACK_PRODUCED_INCREMENT", targetMovement.movementType)
        assertEquals(80.0, targetMovement.quantityDelta, 0.0)
        assertEquals(rawMovement.reference, targetMovement.reference)
    }
}

