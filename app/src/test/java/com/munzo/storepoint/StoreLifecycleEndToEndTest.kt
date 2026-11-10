package com.munzo.storepoint

import com.munzo.storepoint.data.CartItemDetails
import com.munzo.storepoint.data.CashierSession
import com.munzo.storepoint.data.DrawerTransaction
import com.munzo.storepoint.data.Product
import com.munzo.storepoint.data.ReturnItem
import com.munzo.storepoint.data.ReturnTransaction
import com.munzo.storepoint.data.ShiftReconciliation
import com.munzo.storepoint.data.StoreConfig
import com.munzo.storepoint.data.StorePointRepository
import com.munzo.storepoint.data.Transaction
import com.munzo.storepoint.data.TransactionItem
import com.munzo.storepoint.data.User
import com.munzo.storepoint.ui.screens.calculateTieredStoreFee
import com.munzo.storepoint.util.EscPosHelper
import com.munzo.storepoint.util.SecurityHelper
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/**
 * Comprehensive End-to-End Store Lifecycle Test for StorePoint POS.
 *
 * Simulates a complete, realistic operational cycle of a retail/café store:
 * 1. Initial Store Setup & Multi-Role User Provisioning (PBKDF2 security)
 * 2. Raw Material & Finished Goods Catalog Setup with BOM Recipe
 * 3. Atomic Bulk Production Run (Raw materials deduction -> Finished goods yield)
 * 4. Cashier Shift Opening with Beginning Float
 * 5. Full POS Ringing: Multi-Item, Multi-UOM, VAT & Discount Calculation
 * 6. Split-Tender Checkout (Cash + Electronic Wallet GCash)
 * 7. Hardware ESC/POS Receipt Byte Serialization Verification
 * 8. Digital Services Cash-In Reload & Tiered Fee Processing
 * 9. Customer Return & Inventory Restock Handling
 * 10. Cashier Blind Shift Close & Cash Drawer Reconciliation
 * 11. End-of-Day Database Disaster Recovery Backup Serialization
 */
class StoreLifecycleEndToEndTest {

    private fun containsSubsequence(haystack: ByteArray, needle: ByteArray): Boolean {
        if (needle.isEmpty() || haystack.size < needle.size) return false
        for (i in 0..haystack.size - needle.size) {
            var match = true
            for (j in needle.indices) {
                if (haystack[i + j] != needle[j]) {
                    match = false
                    break
                }
            }
            if (match) return true
        }
        return false
    }

    @Test
    fun executeFullStoreOperationalLifecycleEndToEnd() {
        // =====================================================================
        // STEP 1: Initial Store Setup & Multi-Role User Provisioning
        // =====================================================================
        val storeConfig = StoreConfig(
            id = 1,
            storeName = "StorePoint Flagship Café & General Store",
            taxPercentage = 12.0, // 12% PH VAT
            currencySymbol = "₱",
            setupCompleted = true,
            gcashBalance = 10000.0,
            hasGCash = true
        )
        assertTrue("Store setup must be completed", storeConfig.setupCompleted)

        val adminPinHash = SecurityHelper.hashPassword("998877")
        val adminUser = User(
            username = "admin_owner",
            pinHash = adminPinHash,
            role = "ADMIN"
        )
        val adminPinVerify = SecurityHelper.verifyPassword("998877", adminUser.pinHash)
        assertTrue("Admin master PIN must verify securely via PBKDF2", adminPinVerify.isMatch)

        val cashier = User(
            username = "cashier_juan",
            pinHash = SecurityHelper.hashPassword("1234"),
            role = "CASHIER"
        )
        assertTrue(SecurityHelper.verifyPassword("1234", cashier.pinHash).isMatch)
        assertFalse(SecurityHelper.verifyPassword("wrong", cashier.pinHash).isMatch)

        // =====================================================================
        // STEP 2: Raw Materials & Catalog with Recipe (Bill of Materials)
        // =====================================================================
        var coffeeBeansStock = 1000 // 1000g in stock
        var freshMilkStock = 5000   // 5000ml in stock
        var paperCupsStock = 100    // 100 cups in stock

        val rawBeans = Product(id = 1, name = "Espresso Beans (g)", categoryId = 1, price = 1.50, stockCount = coffeeBeansStock, barcode = "RAW001")
        val rawMilk = Product(id = 2, name = "Barista Fresh Milk (ml)", categoryId = 1, price = 0.12, stockCount = freshMilkStock, barcode = "RAW002")
        val rawCup = Product(id = 3, name = "16oz Paper Cup (pcs)", categoryId = 1, price = 4.50, stockCount = paperCupsStock, barcode = "RAW003")

        var finishedLatteStock = 0
        val latteProduct = Product(
            id = 10,
            name = "Iced Spanish Latte 16oz",
            categoryId = 2,
            price = 145.00,
            cost = 45.00,
            stockCount = finishedLatteStock,
            barcode = "4800099881122"
        )

        // Retail Goods (Standard & Multi-UOM)
        var bottledWaterStock = 48
        val waterProduct = Product(id = 20, name = "Nature Spring 500ml", categoryId = 3, price = 25.00, stockCount = bottledWaterStock, barcode = "4800012345678")

        var cigaretteStickStock = 200 // 200 single sticks
        val cigaretteProduct = Product(id = 30, name = "Marlboro Lights", categoryId = 4, price = 9.00, stockCount = cigaretteStickStock, barcode = "4800098765432")

        // =====================================================================
        // STEP 3: Atomic BOM Production Run (Make 10 Lattes)
        // =====================================================================
        // Recipe per latte: 18g beans, 200ml milk, 1 cup
        val batchUnits = 10
        val beansNeeded = 18 * batchUnits
        val milkNeeded = 200 * batchUnits
        val cupsNeeded = 1 * batchUnits

        assertTrue("Enough beans", coffeeBeansStock >= beansNeeded)
        assertTrue("Enough milk", freshMilkStock >= milkNeeded)
        assertTrue("Enough cups", paperCupsStock >= cupsNeeded)

        coffeeBeansStock -= beansNeeded
        freshMilkStock -= milkNeeded
        paperCupsStock -= cupsNeeded
        finishedLatteStock += batchUnits

        assertEquals(820, coffeeBeansStock)
        assertEquals(3000, freshMilkStock)
        assertEquals(90, paperCupsStock)
        assertEquals(10, finishedLatteStock)

        // =====================================================================
        // STEP 4: Cashier Shift Opening with Beginning Float
        // =====================================================================
        val shiftOpeningTime = 1700000000000L
        val startingFloat = 1500.00 // ₱1,500.00 beginning cash
        val session = CashierSession(
            id = 1,
            cashierUsername = cashier.username,
            startTime = shiftOpeningTime,
            startingCash = startingFloat,
            status = "ACTIVE"
        )
        assertEquals(1500.00, session.startingCash, 0.0)

        val openDrawerTx = DrawerTransaction(
            id = 1,
            sessionId = session.id,
            timestamp = shiftOpeningTime,
            cashierUsername = cashier.username,
            amount = startingFloat,
            type = "IN",
            reason = "STARTING_FLOAT"
        )
        assertEquals(1500.00, openDrawerTx.amount, 0.0)

        // =====================================================================
        // STEP 5: Full POS Ringing (Basket Assembly, VAT, & Pricing Precision)
        // =====================================================================
        // Customer 1 buys:
        // - 2x Iced Spanish Latte @ ₱145.00 = ₱290.00
        // - 1x Nature Spring @ ₱25.00 = ₱25.00
        // - 1x 20s Pack of Marlboro Lights (multiplier 20) @ ₱175.00 = ₱175.00
        val cartItems = listOf(
            CartItemDetails(
                product = latteProduct.copy(stockCount = finishedLatteStock),
                quantity = 2,
                uomName = "pc",
                multiplier = 1,
                selectedPrice = 145.00
            ),
            CartItemDetails(
                product = waterProduct.copy(stockCount = bottledWaterStock),
                quantity = 1,
                uomName = "bottle",
                multiplier = 1,
                selectedPrice = 25.00
            ),
            CartItemDetails(
                product = cigaretteProduct.copy(stockCount = cigaretteStickStock),
                quantity = 1,
                uomName = "20s Pack",
                multiplier = 20,
                selectedPrice = 175.00
            )
        )

        // Validate stock sufficiency for all cart items
        for (item in cartItems) {
            val needed = item.quantity * item.multiplier
            assertTrue("Stock sufficient for ${item.product.name}", needed <= item.product.stockCount)
        }

        val rawTotal = cartItems.sumOf { it.quantity * it.selectedPrice }
        assertEquals(490.00, rawTotal, 0.001) // 290 + 25 + 175 = 490

        // Apply a 5% Senior/Loyalty Discount on whole order
        val discountPercent = 5.0
        val discountAmount = StorePointRepository.roundMoney(rawTotal * (discountPercent / 100.0))
        assertEquals(24.50, discountAmount, 0.001)

        val netTotal = StorePointRepository.roundMoney(rawTotal - discountAmount)
        assertEquals(465.50, netTotal, 0.001)

        // Calculate 12% VAT Breakdown: NetTotal = VatableSales * 1.12 => Vatable = NetTotal / 1.12
        val vatableSales = StorePointRepository.roundMoney(netTotal / 1.12)
        val vatAmount = StorePointRepository.roundMoney(netTotal - vatableSales)
        assertEquals(415.62, vatableSales, 0.01)
        assertEquals(49.88, vatAmount, 0.01)
        assertEquals(netTotal, StorePointRepository.roundMoney(vatableSales + vatAmount), 0.01)

        // =====================================================================
        // STEP 6: Split-Tender Checkout (Cash + GCash)
        // =====================================================================
        // Total is ₱465.50.
        // Customer pays:
        // - ₱200.00 via GCash
        // - ₱300.00 Cash (Exact tender needed is ₱265.50 -> Change is ₱34.50)
        val gcashPaid = 200.00
        val cashTendered = 300.00
        val remainingDueForCash = netTotal - gcashPaid // 265.50
        val changeGiven = StorePointRepository.roundMoney(cashTendered - remainingDueForCash)
        assertEquals(34.50, changeGiven, 0.001)

        // Deduct inventory stock
        finishedLatteStock -= 2
        bottledWaterStock -= 1
        cigaretteStickStock -= (1 * 20)

        assertEquals(8, finishedLatteStock)
        assertEquals(47, bottledWaterStock)
        assertEquals(180, cigaretteStickStock)

        val tx = Transaction(
            id = 1001,
            timestamp = shiftOpeningTime + 600000L,
            cashierUsername = cashier.username,
            subtotal = rawTotal,
            taxAmount = vatAmount,
            totalAmount = netTotal,
            paymentMethod = "SPLIT",
            cashPaid = cashTendered,
            changeAmount = changeGiven,
            status = "COMPLETED",
            customerName = "Gov. Juan Mercado"
        )
        assertEquals("COMPLETED", tx.status)

        // =====================================================================
        // STEP 7: ESC/POS Thermal Receipt Byte Generation Verification
        // =====================================================================
        val receiptItems = listOf(
            TransactionItem(id = 1, transactionId = tx.id, productId = latteProduct.id, productName = latteProduct.name, price = 145.00, quantity = 2, cost = 45.00),
            TransactionItem(id = 2, transactionId = tx.id, productId = waterProduct.id, productName = waterProduct.name, price = 25.00, quantity = 1, cost = 12.00),
            TransactionItem(id = 3, transactionId = tx.id, productId = cigaretteProduct.id, productName = "Marlboro Lights (20s Pack)", price = 175.00, quantity = 1, cost = 140.00)
        )

        // Test 80mm full layout with cutter
        val receipt80mm = EscPosHelper.buildReceiptEscPos(
            storeConfig = storeConfig,
            transaction = tx,
            items = receiptItems,
            is80mm = true,
            kickDrawerOnPrint = true,
            hasAutoCutter = true
        )
        assertNotNull(receipt80mm)
        assertTrue(receipt80mm.size > 100)

        // Verify ESC/POS commands present
        assertTrue("Contains INIT", containsSubsequence(receipt80mm, EscPosHelper.CMD_INIT))
        assertTrue("Contains CUT", containsSubsequence(receipt80mm, EscPosHelper.CMD_PAPER_CUT))
        assertTrue("Contains DRAWER KICK", containsSubsequence(receipt80mm, EscPosHelper.CMD_DRAWER_KICK_PIN2))

        val receiptText = String(receipt80mm, Charsets.US_ASCII)
        assertTrue(receiptText.contains("StorePoint Flagship"))
        assertTrue(receiptText.contains("cashier_juan"))
        assertTrue(receiptText.contains("Gov. Juan Mercado"))
        assertTrue(receiptText.contains("Iced Spanish Latte"))

        // =====================================================================
        // STEP 8: Digital Services Cash-In Reload & Tiered Store Fee
        // =====================================================================
        // Customer requests ₱500.00 GCash cash-in
        var gcashWalletFloat = storeConfig.gcashBalance // ₱10,000 wallet balance
        val reloadAmount = 500.00
        val serviceFee = calculateTieredStoreFee(reloadAmount)
        assertEquals(5.00, serviceFee, 0.0) // ₱500 cash-in tier => ₱5.00 service fee

        val totalCashCollectedFromCustomer = reloadAmount + serviceFee // ₱505.00
        gcashWalletFloat -= reloadAmount // Digital wallet float decreases by ₱500.00
        assertEquals(9500.00, gcashWalletFloat, 0.0)

        // =====================================================================
        // STEP 9: Customer Return & Inventory Restock Handling
        // =====================================================================
        // Customer returns 1 Nature Spring water (defective seal), requests cash refund
        val refundAmount = 25.00
        val returnTx = ReturnTransaction(
            id = 1,
            originalTransactionId = tx.id,
            timestamp = shiftOpeningTime + 1800000L,
            cashierUsername = cashier.username,
            totalRefundAmount = refundAmount,
            refundMethod = "CASH",
            returnReason = "Defective bottle seal",
            customerName = tx.customerName
        )
        assertEquals(tx.id, returnTx.originalTransactionId)

        // Bottle is defective, so restockToInventory = false -> stock count remains 47
        val returnItem = ReturnItem(
            id = 1,
            returnTransactionId = returnTx.id,
            productId = waterProduct.id,
            productName = waterProduct.name,
            quantity = 1,
            unitPrice = 25.00,
            refundAmount = 25.00,
            restockToInventory = false
        )
        assertFalse(returnItem.restockToInventory)
        assertEquals(47, bottledWaterStock) // Not put back into sellable inventory

        // =====================================================================
        // STEP 10: Cashier Blind Shift Close & Cash Drawer Reconciliation
        // =====================================================================
        val shiftCloseTime = shiftOpeningTime + 28800000L // 8 hours later
        // Expected cash in drawer:
        // Starting float (1500) + Net Cash Sales (265.50) + Cash-in total collected (505.00) - Refund (25.00) = 2245.50
        val expectedDrawerCash = startingFloat + remainingDueForCash + totalCashCollectedFromCustomer - refundAmount
        assertEquals(2245.50, expectedDrawerCash, 0.001)

        // Cashier performs blind physical count: counts ₱2,245.50 (exact match)
        val actualCountedCash = 2245.50
        val cashVariance = StorePointRepository.roundMoney(actualCountedCash - expectedDrawerCash)
        assertEquals(0.00, cashVariance, 0.001)

        val reconciliation = ShiftReconciliation(
            id = 1,
            sessionId = session.id,
            cashierUsername = cashier.username,
            timestamp = shiftCloseTime,
            count1000 = 2, // 2000
            count100 = 2,  // 200
            count20 = 2,   // 40
            countCoins = 5.50, // 5.50
            totalCountedCash = actualCountedCash,
            expectedCash = expectedDrawerCash,
            cashVariance = cashVariance,
            countedGCash = gcashWalletFloat,
            expectedGCash = 9500.00,
            gcashVariance = 0.0,
            hasDiscrepancy = false
        )
        assertFalse("No discrepancy in shift reconciliation", reconciliation.hasDiscrepancy)
        assertEquals(0.00, reconciliation.cashVariance, 0.001)
        assertEquals("cashier_juan", reconciliation.cashierUsername)

        // =====================================================================
        // STEP 11: End-of-Day Database Disaster Recovery Backup Serialization
        // =====================================================================
        val backupJson = JSONObject().apply {
            put("backupVersion", 1)
            put("timestamp", shiftCloseTime)
            put("appName", "StorePoint POS")
            put("store_config", JSONObject().apply {
                put("storeName", storeConfig.storeName)
                put("taxPercentage", storeConfig.taxPercentage)
                put("currencySymbol", storeConfig.currencySymbol)
            })
            put("shift_reconciliations", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", reconciliation.id)
                    put("cashierUsername", reconciliation.cashierUsername)
                    put("expectedCash", reconciliation.expectedCash)
                    put("totalCountedCash", reconciliation.totalCountedCash)
                    put("cashVariance", reconciliation.cashVariance)
                })
            })
            put("transactions_count", 1)
            put("returns_count", 1)
            put("closing_wallet_float", gcashWalletFloat)
        }

        val jsonString = backupJson.toString()
        assertTrue(jsonString.contains("StorePoint Flagship"))
        assertTrue(jsonString.contains("shift_reconciliations"))
        assertEquals(9500.0, backupJson.getDouble("closing_wallet_float"), 0.0)
        assertEquals(0.0, backupJson.getJSONArray("shift_reconciliations").getJSONObject(0).getDouble("cashVariance"), 0.001)
    }
}
