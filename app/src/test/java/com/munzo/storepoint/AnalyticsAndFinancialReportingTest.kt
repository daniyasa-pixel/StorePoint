package com.munzo.storepoint

import com.munzo.storepoint.data.CashierSession
import com.munzo.storepoint.data.Product
import com.munzo.storepoint.data.StoreConfig
import com.munzo.storepoint.data.StorePointRepository
import com.munzo.storepoint.data.Transaction
import com.munzo.storepoint.data.TransactionItem
import com.munzo.storepoint.util.PdfExportUtil
import org.junit.Assert.*
import org.junit.Test

class AnalyticsAndFinancialReportingTest {

    @Test
    fun executiveMetrics_calculatesGrossSalesNetProfitAndBasketSize() {
        val sampleTransactions = listOf(
            Transaction(id = 1, timestamp = 1000L, cashierUsername = "cashier1", subtotal = 500.0, taxAmount = 60.0, totalAmount = 560.0, paymentMethod = "CASH", cashPaid = 600.0, changeAmount = 40.0),
            Transaction(id = 2, timestamp = 2000L, cashierUsername = "cashier2", subtotal = 200.0, taxAmount = 24.0, totalAmount = 224.0, paymentMethod = "GCASH", cashPaid = 224.0, changeAmount = 0.0),
            Transaction(id = 3, timestamp = 3000L, cashierUsername = "cashier1", subtotal = 750.0, taxAmount = 90.0, totalAmount = 840.0, paymentMethod = "MAYA", cashPaid = 840.0, changeAmount = 0.0)
        )

        val sampleItems = listOf(
            TransactionItem(id = 1, transactionId = 1, productId = 10, productName = "San Miguel Beer", price = 70.0, cost = 50.0, quantity = 8),
            TransactionItem(id = 2, transactionId = 2, productId = 11, productName = "Piattos Cheese", price = 35.0, cost = 25.0, quantity = 6),
            TransactionItem(id = 3, transactionId = 3, productId = 12, productName = "Spam Luncheon Meat", price = 210.0, cost = 160.0, quantity = 4)
        )

        // 1. Daily Gross Sales
        val grossSales = sampleTransactions.sumOf { it.totalAmount }
        assertEquals(1624.0, grossSales, 0.0)

        // 2. Earned Net Profit
        val netProfit = sampleItems.sumOf { (it.price - it.cost) * it.quantity }
        // (70-50)*8 + (35-25)*6 + (210-160)*4 = 160 + 60 + 200 = 420.0
        assertEquals(420.0, netProfit, 0.0)

        // 3. Average Basket Ticket Size
        val avgBasket = StorePointRepository.roundMoney(grossSales / sampleTransactions.size)
        assertEquals(541.33, avgBasket, 0.0)

        // 4. Low-stock & Out-of-stock warning counts
        val catalog = listOf(
            Product(id = 1, name = "Coke 1.5L", categoryId = 1, price = 75.0, cost = 60.0, stockCount = 20, barcode = "48001"),
            Product(id = 2, name = "Royal 500ml", categoryId = 1, price = 35.0, cost = 28.0, stockCount = 3, barcode = "48002"), // Low-stock (1-5)
            Product(id = 3, name = "SkyFlakes", categoryId = 2, price = 10.0, cost = 7.5, stockCount = 0, barcode = "48003")   // Out-of-stock (0)
        )

        val lowStock = catalog.filter { it.stockCount in 1..5 }
        val outOfStock = catalog.filter { it.stockCount <= 0 }
        assertEquals(1, lowStock.size)
        assertEquals(1, outOfStock.size)
        assertEquals(2, lowStock.size + outOfStock.size)
    }

    @Test
    fun topPerformingProductRankings_aggregatesVolumeRevenueAndProfit() {
        val sampleItems = listOf(
            TransactionItem(id = 1, transactionId = 1, productId = 1, productName = "C2 Green Tea", price = 35.0, cost = 25.0, quantity = 10),
            TransactionItem(id = 2, transactionId = 2, productId = 1, productName = "C2 Green Tea", price = 35.0, cost = 25.0, quantity = 5),
            TransactionItem(id = 3, transactionId = 1, productId = 2, productName = "Marlboro Red Pack", price = 180.0, cost = 150.0, quantity = 4),
            TransactionItem(id = 4, transactionId = 3, productId = 3, productName = "Selecta Cornetto", price = 30.0, cost = 22.0, quantity = 8)
        )

        val ranked = sampleItems
            .groupBy { it.productName }
            .map { (name, items) ->
                val totalQty = items.sumOf { it.quantity }
                val totalSales = items.sumOf { it.price * it.quantity }
                val totalProfit = items.sumOf { (it.price - it.cost) * it.quantity }
                Triple(name, totalQty, Pair(totalSales, totalProfit))
            }
            .sortedByDescending { it.third.first } // Ranked by sales revenue

        // Top 1 by revenue: Marlboro Red Pack (4 * 180 = 720.0)
        assertEquals("Marlboro Red Pack", ranked[0].first)
        assertEquals(720.0, ranked[0].third.first, 0.0)
        assertEquals(120.0, ranked[0].third.second, 0.0) // profit: 4 * 30 = 120

        // Top 2 by revenue: C2 Green Tea (15 * 35 = 525.0)
        assertEquals("C2 Green Tea", ranked[1].first)
        assertEquals(15, ranked[1].second) // 15 units sold
        assertEquals(525.0, ranked[1].third.first, 0.0)
        assertEquals(150.0, ranked[1].third.second, 0.0) // profit: 15 * 10 = 150

        // Top 3 by revenue: Selecta Cornetto (8 * 30 = 240.0)
        assertEquals("Selecta Cornetto", ranked[2].first)
        assertEquals(240.0, ranked[2].third.first, 0.0)
    }

    @Test
    fun digitalServiceFeeConfiguration_feeTuningValidation() {
        val defaultConfig = StoreConfig(
            id = 1,
            storeName = "Munzo Mart",
            currencySymbol = "₱",
            loadServiceFee = 2.0,
            gcashServiceFee = 10.0,
            mayaServiceFee = 10.0,
            mayaBankFee = 15.0
        )

        assertEquals(2.0, defaultConfig.loadServiceFee, 0.0)
        assertEquals(10.0, defaultConfig.gcashServiceFee, 0.0)
        assertEquals(10.0, defaultConfig.mayaServiceFee, 0.0)
        assertEquals(15.0, defaultConfig.mayaBankFee, 0.0)

        // Custom tuned fees
        val tunedConfig = defaultConfig.copy(
            loadServiceFee = 3.0,
            gcashServiceFee = 15.0, // ₱15 per ₱1,000 tier
            mayaServiceFee = 12.0,
            mayaBankFee = 20.0
        )

        assertEquals(3.0, tunedConfig.loadServiceFee, 0.0)
        assertEquals(15.0, tunedConfig.gcashServiceFee, 0.0)
        assertEquals(12.0, tunedConfig.mayaServiceFee, 0.0)
        assertEquals(20.0, tunedConfig.mayaBankFee, 0.0)

        // Verify tiered GCash fee calculation for ₱2,500 cash-in (3 brackets of 1k)
        val amount = 2500.0
        val brackets = Math.ceil(amount / 1000.0).toInt()
        assertEquals(3, brackets)
        val calculatedGcashFee = brackets * tunedConfig.gcashServiceFee
        assertEquals(45.0, calculatedGcashFee, 0.0)
    }

    @Test
    fun pdfExportUtil_csvExportEngineGeneratesValidFormats() {
        // 1. Sales Summary CSV
        val transactions = listOf(
            Transaction(id = 101, timestamp = 1700000000000L, cashierUsername = "admin", customerName = "Juan", subtotal = 100.0, taxAmount = 12.0, totalAmount = 112.0, paymentMethod = "CASH", cashPaid = 120.0, changeAmount = 8.0),
            Transaction(id = 102, timestamp = 1700000010000L, cashierUsername = "cashier1", customerName = "Maria", subtotal = 250.0, taxAmount = 30.0, totalAmount = 280.0, paymentMethod = "GCASH", cashPaid = 280.0, changeAmount = 0.0)
        )
        val salesCsv = PdfExportUtil.buildSalesSummaryCsv(transactions)
        assertTrue(salesCsv.startsWith("Transaction ID,Timestamp,Date Time,Cashier,Customer,Payment Method,Subtotal,Tax Amount,Total Amount,Status\n"))
        assertTrue(salesCsv.contains("#101"))
        assertTrue(salesCsv.contains("admin"))
        assertTrue(salesCsv.contains("112.00"))
        assertTrue(salesCsv.contains("#102"))
        assertTrue(salesCsv.contains("GCASH"))

        // 2. Register Journal CSV
        val sessions = listOf(
            CashierSession(id = 1, cashierUsername = "cashier1", startTime = 1700000000000L, endTime = 1700028800000L, startingCash = 1000.0, endingCash = 8500.0, status = "CLOSED"),
            CashierSession(id = 2, cashierUsername = "cashier2", startTime = 1700030000000L, endTime = null, startingCash = 1000.0, endingCash = null, status = "ACTIVE")
        )
        val journalCsv = PdfExportUtil.buildRegisterJournalCsv(sessions)
        assertTrue(journalCsv.startsWith("Session ID,Cashier Username,Start Time,End Time,Starting Cash,Ending Cash,Status\n"))
        assertTrue(journalCsv.contains("#1,cashier1"))
        assertTrue(journalCsv.contains("1000.00,8500.00,CLOSED"))
        assertTrue(journalCsv.contains("#2,cashier2"))
        assertTrue(journalCsv.contains("ACTIVE"))

        // 3. Inventory Valuation CSV
        val products = listOf(
            Product(id = 1, name = "Instant Noodles", categoryId = 1, price = 15.0, cost = 11.50, stockCount = 100, barcode = "480011", baseUom = "pack", trackInventory = true),
            Product(id = 2, name = "Sardines in Tomato", categoryId = 1, price = 25.0, cost = 19.00, stockCount = 40, barcode = "480022", baseUom = "can", trackInventory = true)
        )
        val valuationCsv = PdfExportUtil.buildInventoryValuationCsv(products)
        assertTrue(valuationCsv.startsWith("Product ID,Barcode,Product Name,Stock Count,Base UOM,Cost Price,Retail Price,Total Cost Valuation,Total Retail Valuation,Projected Profit,Track Inventory\n"))
        assertTrue(valuationCsv.contains("#1,\"480011\",\"Instant Noodles\",100,\"pack\",11.50,15.00,1150.00,1500.00,350.00,true"))
        assertTrue(valuationCsv.contains("#2,\"480022\",\"Sardines in Tomato\",40,\"can\",19.00,25.00,760.00,1000.00,240.00,true"))
    }
}
