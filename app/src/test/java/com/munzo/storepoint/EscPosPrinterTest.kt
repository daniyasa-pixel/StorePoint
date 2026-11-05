package com.munzo.storepoint

import com.munzo.storepoint.data.StoreConfig
import com.munzo.storepoint.data.Transaction
import com.munzo.storepoint.data.TransactionItem
import com.munzo.storepoint.data.ReturnTransaction
import com.munzo.storepoint.data.ReturnItem
import com.munzo.storepoint.util.EscPosHelper
import org.junit.Assert.*
import org.junit.Test

class EscPosPrinterTest {

    private val sampleConfig = StoreConfig(
        id = 1,
        storeName = "StorePoint Sari-Sari Store",
        currencySymbol = "₱",
        taxPercentage = 12.0
    )

    private val sampleTx = Transaction(
        id = 101,
        timestamp = 1790937600000L,
        subtotal = 100.0,
        taxAmount = 12.0,
        totalAmount = 112.0,
        cashPaid = 200.0,
        changeAmount = 88.0,
        paymentMethod = "CASH",
        cashierUsername = "cashier1",
        customerName = "Customer Juan"
    )

    private val sampleItems = listOf(
        TransactionItem(id = 1, transactionId = 101, productId = 10, productName = "Coca-Cola 1.5L", price = 75.0, quantity = 1),
        TransactionItem(id = 2, transactionId = 101, productId = 11, productName = "Piattos Cheese 85g", price = 37.0, quantity = 1)
    )

    @Test
    fun printerTypeEnum_containsUsbEscPos() {
        val types = EscPosHelper.PrinterType.values().map { it.name }
        assertTrue(types.contains("USB_ESCPOS"))
        assertTrue(types.contains("BLUETOOTH_ESCPOS"))
        assertTrue(types.contains("NETWORK_ESCPOS"))
        assertTrue(types.contains("SYSTEM_SPOOLER"))
    }

    @Test
    fun sanitizeForEscPos_convertsMultiBytePesoToP() {
        val input = "Total: ₱150.00"
        val sanitized = EscPosHelper.sanitizeForEscPos(input, "₱")
        assertEquals("Total: P150.00", sanitized)
        assertFalse(sanitized.contains("₱"))
    }

    @Test
    fun sanitizeForEscPos_replacesSmartPunctuationWithAscii() {
        val input = "“Special” item—discounted"
        val sanitized = EscPosHelper.sanitizeForEscPos(input)
        assertEquals("\"Special\" item-discounted", sanitized)
    }

    @Test
    fun safeCurrencySymbol_handlesPesoAndBlanks() {
        assertEquals("P", EscPosHelper.safeCurrencySymbol("₱"))
        assertEquals("P", EscPosHelper.safeCurrencySymbol(""))
        assertEquals("$", EscPosHelper.safeCurrencySymbol("$"))
        assertEquals("PHP", EscPosHelper.safeCurrencySymbol("PHP"))
    }

    @Test
    fun receipt58mm_xp58PlusManualTear_doesNotContainPaperCutBytes() {
        val bytes = EscPosHelper.buildReceiptEscPos(
            storeConfig = sampleConfig,
            transaction = sampleTx,
            items = sampleItems,
            is80mm = false,
            kickDrawerOnPrint = true,
            hasAutoCutter = false // XP-58 Plus default: manual tear bar
        )

        // Ensure GS V 66 0 (Paper cut) is NOT in the stream (which causes 'VB@' print garbage on XP-58)
        assertFalse("58mm receipt for XP-58 Plus must not include cut command bytes", containsSubsequence(bytes, EscPosHelper.CMD_PAPER_CUT))

        // Ensure ESC d 5 (Feed lines) IS included to clear the tear bar
        assertTrue("58mm receipt must include feed lines for manual tear bar", containsSubsequence(bytes, EscPosHelper.CMD_FEED_LINES))
    }

    @Test
    fun receipt80mm_withAutoCutter_containsPaperCutBytes() {
        val bytes = EscPosHelper.buildReceiptEscPos(
            storeConfig = sampleConfig,
            transaction = sampleTx,
            items = sampleItems,
            is80mm = true,
            kickDrawerOnPrint = true,
            hasAutoCutter = true
        )

        assertTrue("80mm cutter receipt must include paper cut bytes", containsSubsequence(bytes, EscPosHelper.CMD_PAPER_CUT))
    }

    @Test
    fun drawerKickBytes_containsBothPin2AndPin5Pulses() {
        val kickBytes = EscPosHelper.buildDrawerKickBytes()
        assertTrue(containsSubsequence(kickBytes, EscPosHelper.CMD_DRAWER_KICK_PIN2))
        assertTrue(containsSubsequence(kickBytes, EscPosHelper.CMD_DRAWER_KICK_PIN5))
    }

    @Test
    fun testTicket_58mmXP58_tailorsModelTextAndFeedsTearBar() {
        val testBytes = EscPosHelper.buildTestTicket(
            is80mm = false,
            kickDrawer = false,
            hasAutoCutter = false
        )
        val text = String(testBytes, Charsets.US_ASCII)
        assertTrue(text.contains("XP-58 Plus"))
        assertTrue(text.contains("Manual Tear"))
        assertFalse(containsSubsequence(testBytes, EscPosHelper.CMD_PAPER_CUT))
        assertTrue(containsSubsequence(testBytes, EscPosHelper.CMD_FEED_LINES))
    }

    @Test
    fun shiftZReadingTicket_58mm_respectsAutoCutterSetting() {
        val zTicketNoCut = EscPosHelper.buildShiftZReadingTicket(
            storeConfig = sampleConfig,
            cashierUsername = "cashier1",
            startTime = 1790930000000L,
            endTime = 1790937600000L,
            startingCash = 1000.0,
            endingCash = 1112.0,
            expectedCash = 1112.0,
            totalSales = 112.0,
            cashSales = 112.0,
            nonCashSales = 0.0,
            vatableSales = 100.0,
            vatAmount = 12.0,
            payouts = 0.0,
            transactionCount = 1,
            is80mm = false,
            hasAutoCutter = false
        )
        assertFalse(containsSubsequence(zTicketNoCut, EscPosHelper.CMD_PAPER_CUT))
        assertTrue(containsSubsequence(zTicketNoCut, EscPosHelper.CMD_FEED_LINES))

        val zTicketCut = EscPosHelper.buildShiftZReadingTicket(
            storeConfig = sampleConfig,
            cashierUsername = "cashier1",
            startTime = 1790930000000L,
            endTime = 1790937600000L,
            startingCash = 1000.0,
            endingCash = 1112.0,
            expectedCash = 1112.0,
            totalSales = 112.0,
            cashSales = 112.0,
            nonCashSales = 0.0,
            vatableSales = 100.0,
            vatAmount = 12.0,
            payouts = 0.0,
            transactionCount = 1,
            is80mm = true,
            hasAutoCutter = true
        )
        assertTrue(containsSubsequence(zTicketCut, EscPosHelper.CMD_PAPER_CUT))
    }

    @Test
    fun buildRefundReceiptEscPos_formats58mmCorrectly() {
        val returnTx = ReturnTransaction(
            id = 42,
            originalTransactionId = 101,
            timestamp = 1790937600000L,
            cashierUsername = "cashier1",
            totalRefundAmount = 75.0,
            refundMethod = "CASH",
            returnReason = "Defective / Spoiled",
            customerName = "Juan",
            notes = "Bottle cap broken"
        )
        val returnItems = listOf(
            ReturnItem(
                id = 1,
                returnTransactionId = 42,
                productId = 10,
                productName = "Coca-Cola 1.5L",
                quantity = 1,
                unitPrice = 75.0,
                refundAmount = 75.0,
                uomName = "pc",
                restockToInventory = false
            )
        )

        val bytes = EscPosHelper.buildRefundReceiptEscPos(
            storeConfig = sampleConfig,
            returnTransaction = returnTx,
            items = returnItems,
            is80mm = false,
            kickDrawerOnPrint = true,
            hasAutoCutter = false
        )

        val receiptText = String(bytes, Charsets.US_ASCII)
        assertTrue(receiptText.contains("StorePoint Sari-Sari Store"))
        assertTrue(receiptText.contains("RETURN / REFUND VOUCHER"))
        assertTrue(receiptText.contains("Return Slip #: 42"))
        assertTrue(receiptText.contains("Original Sale #: 101"))
        assertTrue(receiptText.contains("Coca-Cola 1.5L"))
        assertTrue(receiptText.contains("[Damaged/Write-Off]"))
        assertTrue(receiptText.contains("TOTAL REFUNDED:"))
        assertTrue(receiptText.contains("P75.00"))

        // Drawer kick bytes should be present
        assertTrue(containsSubsequence(bytes, EscPosHelper.CMD_DRAWER_KICK_PIN2))
        // Manual tear feed lines should be present, not auto-cut
        assertFalse(containsSubsequence(bytes, EscPosHelper.CMD_PAPER_CUT))
        assertTrue(containsSubsequence(bytes, EscPosHelper.CMD_FEED_LINES))
    }

    @Test
    fun buildRefundReceiptEscPos_formats80mmWithAutoCutter() {
        val returnTx = ReturnTransaction(
            id = 55,
            originalTransactionId = 202,
            timestamp = 1790937600000L,
            cashierUsername = "supervisor",
            totalRefundAmount = 150.0,
            refundMethod = "GCASH",
            returnReason = "Wrong Item",
            customerName = "Maria",
            notes = "Customer exchange request"
        )
        val returnItems = listOf(
            ReturnItem(
                id = 2,
                returnTransactionId = 55,
                productId = 20,
                productName = "Corned Beef 150g",
                quantity = 2,
                unitPrice = 75.0,
                refundAmount = 150.0,
                uomName = "can",
                restockToInventory = true
            )
        )

        val bytes = EscPosHelper.buildRefundReceiptEscPos(
            storeConfig = sampleConfig,
            returnTransaction = returnTx,
            items = returnItems,
            is80mm = true,
            kickDrawerOnPrint = false,
            hasAutoCutter = true
        )

        val receiptText = String(bytes, Charsets.US_ASCII)
        assertTrue(receiptText.contains("Return Slip #: 55"))
        assertTrue(receiptText.contains("[Restocked]"))
        assertTrue(receiptText.contains("TOTAL REFUNDED:"))
        assertTrue(receiptText.contains("P150.00"))
        assertTrue(containsSubsequence(bytes, EscPosHelper.CMD_PAPER_CUT))
    }

    @Test
    fun buildBarcodeEscPos_generatesCode128AndCode39Bytes() {
        // 1. Code 128
        val code128Bytes = EscPosHelper.buildBarcodeEscPos("TX-1001", isCode128 = true, height = 50, width = 2)
        // Verify GS H 2 (HRI below barcode)
        assertTrue(containsSubsequence(code128Bytes, byteArrayOf(0x1D, 0x48, 0x02)))
        // Verify GS k 73 (Code 128)
        assertTrue(containsSubsequence(code128Bytes, byteArrayOf(0x1D, 0x6B, 73)))
        // Verify Code 128 subset B prefix '{B' (0x7B, 0x42)
        assertTrue(containsSubsequence(code128Bytes, byteArrayOf(0x7B, 0x42)))
        // Verify text payload
        assertTrue(containsSubsequence(code128Bytes, "TX-1001".toByteArray(Charsets.US_ASCII)))

        // 2. Code 39
        val code39Bytes = EscPosHelper.buildBarcodeEscPos("TX-1001", isCode128 = false, height = 50, width = 2)
        assertTrue(containsSubsequence(code39Bytes, byteArrayOf(0x1D, 0x6B, 69)))
        assertTrue(containsSubsequence(code39Bytes, "TX-1001".toByteArray(Charsets.US_ASCII)))
    }

    @Test
    fun buildReceiptEscPos_withCustomHeaderFooterTinAndBarcode() {
        val bytes = EscPosHelper.buildReceiptEscPos(
            storeConfig = sampleConfig,
            transaction = sampleTx,
            items = sampleItems,
            is80mm = false,
            kickDrawerOnPrint = true,
            hasAutoCutter = false,
            customHeader = "Welcome to Munzo Mart Branch 1",
            customFooter = "Items can be exchanged within 7 days with receipt",
            storeTin = "123-456-789-000",
            storeAddress = "123 Rizal Street, Manila",
            printBarcode = true
        )

        val receiptText = String(bytes, Charsets.US_ASCII)
        assertTrue(receiptText.contains("StorePoint Sari-Sari Store"))
        assertTrue(receiptText.contains("123 Rizal Street, Manila"))
        assertTrue(receiptText.contains("VAT Reg. TIN: 123-456-789-000"))
        assertTrue(receiptText.contains("Welcome to Munzo Mart Branch 1"))
        assertTrue(receiptText.contains("Cashier: cashier1"))
        assertTrue(receiptText.contains("Customer: Customer Juan"))
        assertTrue(receiptText.contains("VATable Sales:"))
        assertTrue(receiptText.contains("Items can be exchanged within 7 days with receipt"))

        // Verify barcode TX-101 presence
        assertTrue(containsSubsequence(bytes, "TX-101".toByteArray(Charsets.US_ASCII)))
        assertTrue(containsSubsequence(bytes, byteArrayOf(0x1D, 0x6B, 73)))
    }

    @Test
    fun buildDrawerSlipEscPos_cashInPettyCash_formats58mmAndKicksDrawer() {
        val drawerTx = com.munzo.storepoint.data.DrawerTransaction(
            id = 1,
            sessionId = 10,
            timestamp = 1790937600000L,
            cashierUsername = "cashier1",
            amount = 500.0,
            type = "CASH_IN",
            reason = "Petty cash float injection"
        )

        val slipBytes = EscPosHelper.buildDrawerSlipEscPos(
            storeConfig = sampleConfig,
            drawerTransaction = drawerTx,
            balanceBefore = 1000.0,
            balanceAfter = 1500.0,
            is80mm = false,
            kickDrawerOnPrint = true,
            hasAutoCutter = false,
            storeTin = "123-456-789-000",
            printBarcode = true
        )

        val slipText = String(slipBytes, Charsets.US_ASCII)
        assertTrue(slipText.contains("CASH-IN / FLOAT INSERTION"))
        assertTrue(slipText.contains("Slip #: DRW-1"))
        assertTrue(slipText.contains("Session #: #SES-10"))
        assertTrue(slipText.contains("Cashier: cashier1"))
        assertTrue(slipText.contains("Petty cash float injection"))
        assertTrue(slipText.contains("CASH INFLOW:"))
        assertTrue(slipText.contains("P500.00"))
        assertTrue(slipText.contains("Drawer Before:"))
        assertTrue(slipText.contains("P1000.00"))
        assertTrue(slipText.contains("New Drawer Balance:"))
        assertTrue(slipText.contains("P1500.00"))
        assertTrue(slipText.contains("Cashier Signature"))
        assertTrue(slipText.contains("Manager Signature"))

        // Check drawer kick
        assertTrue(containsSubsequence(slipBytes, EscPosHelper.CMD_DRAWER_KICK_PIN2))
        // Check barcode DRW-1
        assertTrue(containsSubsequence(slipBytes, "DRW-1".toByteArray(Charsets.US_ASCII)))
        // 58mm manual tear
        assertFalse(containsSubsequence(slipBytes, EscPosHelper.CMD_PAPER_CUT))
        assertTrue(containsSubsequence(slipBytes, EscPosHelper.CMD_FEED_LINES))
    }

    @Test
    fun buildDrawerSlipEscPos_cashOutOperationalDisbursement_formats80mmWithAutoCut() {
        val drawerTx = com.munzo.storepoint.data.DrawerTransaction(
            id = 2,
            sessionId = 10,
            timestamp = 1790937600000L,
            cashierUsername = "cashier1",
            amount = -180.0,
            type = "CASH_OUT",
            reason = "Store Supplies: Plastic bags & tape"
        )

        val slipBytes = EscPosHelper.buildDrawerSlipEscPos(
            storeConfig = sampleConfig,
            drawerTransaction = drawerTx,
            balanceBefore = 1500.0,
            balanceAfter = 1320.0,
            is80mm = true,
            kickDrawerOnPrint = false,
            hasAutoCutter = true,
            printBarcode = true
        )

        val slipText = String(slipBytes, Charsets.US_ASCII)
        assertTrue(slipText.contains("CASH-OUT / DRAWER PAYOUT"))
        assertTrue(slipText.contains("Slip #: DRW-2"))
        assertTrue(slipText.contains("Plastic bags & tape"))
        assertTrue(slipText.contains("CASH DISBURSED:"))
        assertTrue(slipText.contains("P180.00"))
        assertTrue(slipText.contains("Adjustment:"))
        assertTrue(slipText.contains("-P180.00"))
        assertTrue(slipText.contains("P1320.00"))

        // Auto cut for 80mm
        assertTrue(containsSubsequence(slipBytes, EscPosHelper.CMD_PAPER_CUT))
    }

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
}
