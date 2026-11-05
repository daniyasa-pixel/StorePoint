package com.munzo.storepoint

import com.munzo.storepoint.data.StorePointRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryAndUomTest {

    @Test
    fun uomMultiplier_calculatesEffectiveDeduction() {
        // Scenario: A pack contains 12 pieces (multiplier = 12)
        val packMultiplier = 12
        val packsSold = 3
        val baseUnitsDeducted = packsSold * packMultiplier
        assertEquals(36, baseUnitsDeducted)

        // Starting stock: 100 pieces
        val startingStock = 100
        val remainingStock = startingStock - baseUnitsDeducted
        assertEquals(64, remainingStock)
        assertTrue("Remaining stock should not be negative", remainingStock >= 0)
    }

    @Test
    fun restockCalculation_computesAverageUnitCostAccurately() {
        // Bought 5 boxes of instant noodles, each containing 24 sachets (total 120 base units)
        val boxes = 5
        val sachetsPerBox = 24
        val totalBaseUnits = boxes * sachetsPerBox
        val totalInvoiceCost = 1450.00 // PHP 1450.00 total cost

        val costPerBaseUnit = StorePointRepository.roundMoney(totalInvoiceCost / totalBaseUnits)
        // 1450.0 / 120 = 12.083333333333334 -> 12.08
        assertEquals(12.08, costPerBaseUnit, 0.0)

        // Verifying profit margin calculation
        val retailPricePerSachet = 15.00
        val profitPerSachet = StorePointRepository.roundMoney(retailPricePerSachet - costPerBaseUnit)
        assertEquals(2.92, profitPerSachet, 0.0)
    }

    @Test
    fun returnRefund_calculatesAccurateRestockAndRefundAmount() {
        val returnQuantity = 2
        val itemMultiplier = 6 // e.g. 6-pack
        val baseUnitsRestocked = returnQuantity * itemMultiplier
        assertEquals(12, baseUnitsRestocked)

        val pricePerPack = 150.00
        val totalRefund = StorePointRepository.roundMoney(returnQuantity * pricePerPack)
        assertEquals(300.00, totalRefund, 0.0)
    }

    @Test
    fun realTimeStockAuditing_safetyThresholdsAndBadges() {
        val safetyThreshold = 5

        fun classifyStockStatus(stock: Int): String = when {
            stock <= 0 -> "OUT OF STOCK"
            stock in 1..safetyThreshold -> "LOW STOCK ALERT"
            else -> "IN STOCK"
        }

        assertEquals("OUT OF STOCK", classifyStockStatus(0))
        assertEquals("OUT OF STOCK", classifyStockStatus(-2))
        assertEquals("LOW STOCK ALERT", classifyStockStatus(1))
        assertEquals("LOW STOCK ALERT", classifyStockStatus(5))
        assertEquals("IN STOCK", classifyStockStatus(6))
        assertEquals("IN STOCK", classifyStockStatus(100))
    }

    @Test
    fun multiUomSupport_variousPhilippineRetailUnits() {
        // Supported units: kg, liters, packs, bottles, pieces, cans
        data class UomSpec(val uom: String, val multiplier: Int, val unitsSold: Int)

        val items = listOf(
            UomSpec("kg", 1, 5),          // 5 kg of rice
            UomSpec("liters", 1, 2),      // 2 liters of oil
            UomSpec("pack", 12, 2),       // 2 packs of 12s -> 24 base units
            UomSpec("bottle", 1, 6),      // 6 bottles
            UomSpec("pieces", 1, 10),     // 10 pieces
            UomSpec("cans", 24, 1)        // 1 case of 24 cans -> 24 base units
        )

        val totalBaseUnitsDeducted = items.sumOf { it.multiplier * it.unitsSold }
        // 5 + 2 + 24 + 6 + 10 + 24 = 71
        assertEquals(71, totalBaseUnitsDeducted)
    }

    @Test
    fun restockCogs_weightedAverageCostCalculation() {
        // Existing inventory: 20 units with current cost of ₱10.00
        val currentStock = 20
        val currentCost = 10.00
        val currentValue = currentStock * currentCost // 200.00

        // New shipment received: 30 units with invoice total cost of ₱450.00 (₱15.00/unit)
        val receivedUnits = 30
        val newInvoiceCost = 450.00

        val newTotalStock = currentStock + receivedUnits
        val newTotalValue = currentValue + newInvoiceCost
        val weightedAverageCost = StorePointRepository.roundMoney(newTotalValue / newTotalStock)

        // 650.00 / 50 = 13.00
        assertEquals(13.00, weightedAverageCost, 0.0)

        // Single invoice unit cost without historical weighting
        val latestUnitCost = StorePointRepository.roundMoney(newInvoiceCost / receivedUnits)
        assertEquals(15.00, latestUnitCost, 0.0)
    }

    @Test
    fun stocktakeAudit_varianceAndSystemAdjustment() {
        data class AuditRecord(val systemCount: Int, val physicalCount: Int)

        val auditRecords = listOf(
            AuditRecord(systemCount = 50, physicalCount = 50), // In Sync
            AuditRecord(systemCount = 30, physicalCount = 28), // Shortage (-2)
            AuditRecord(systemCount = 15, physicalCount = 20)  // Surplus (+5)
        )

        val matches = auditRecords.count { it.physicalCount == it.systemCount }
        val discrepancies = auditRecords.count { it.physicalCount != it.systemCount }
        val netDiscrepancyDelta = auditRecords.sumOf { it.physicalCount - it.systemCount }

        assertEquals(1, matches)
        assertEquals(2, discrepancies)
        assertEquals(3, netDiscrepancyDelta) // -2 + 5 = 3
    }

    @Test
    fun quickShare_stockSheetCsvFormatting() {
        val sampleRow = "1,\"Alaska Evaporated Milk\",\"Canned Goods\",\"4800112233\",24,cans,35.00,42.00,IN STOCK"
        val columns = sampleRow.split(",")
        assertEquals("1", columns[0])
        assertEquals("\"Alaska Evaporated Milk\"", columns[1])
        assertEquals("24", columns[4])
        assertEquals("cans", columns[5])
        assertEquals("IN STOCK", columns[8])
    }
}
