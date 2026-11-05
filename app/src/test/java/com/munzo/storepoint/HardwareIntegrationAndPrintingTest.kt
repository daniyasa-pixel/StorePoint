package com.munzo.storepoint

import com.munzo.storepoint.data.DrawerTransaction
import com.munzo.storepoint.data.ReturnItem
import com.munzo.storepoint.data.ReturnTransaction
import com.munzo.storepoint.data.ShiftReconciliation
import com.munzo.storepoint.data.StoreConfig
import com.munzo.storepoint.data.StorePointRepository
import com.munzo.storepoint.data.Transaction
import com.munzo.storepoint.data.TransactionItem
import com.munzo.storepoint.util.EscPosHelper
import org.junit.Assert.*
import org.junit.Test

class HardwareIntegrationAndPrintingTest {

    private val sampleStoreConfig = StoreConfig(
        id = 1,
        storeName = "Munzo Mart Retail POS",
        currencySymbol = "₱",
        taxPercentage = 12.0
    )

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
    fun thermalPrinters_supports58mmAnd80mmWidthsViaUsbAndBluetooth() {
        val tx = Transaction(
            id = 501,
            timestamp = 1700000000000L,
            cashierUsername = "cashier_maria",
            customerName = "Pedro Cruz",
            subtotal = 200.0,
            taxAmount = 24.0,
            totalAmount = 224.0,
            cashPaid = 300.0,
            changeAmount = 76.0,
            paymentMethod = "CASH"
        )
        val items = listOf(
            TransactionItem(id = 1, transactionId = 501, productId = 1, productName = "San Miguel Pale Pilsen 330ml", price = 65.0, quantity = 2),
            TransactionItem(id = 2, transactionId = 501, productId = 2, productName = "Chippy BBQ 110g", price = 34.0, quantity = 2),
            TransactionItem(id = 3, transactionId = 501, productId = 3, productName = "Wilkins Pure Water 500ml", price = 26.0, quantity = 1)
        )

        // 1. 58mm printer (XP-58 Plus) - 32 columns, manual tear feed lines
        val bytes58mm = EscPosHelper.buildReceiptEscPos(
            storeConfig = sampleStoreConfig,
            transaction = tx,
            items = items,
            is80mm = false,
            kickDrawerOnPrint = true,
            hasAutoCutter = false
        )
        val text58mm = String(bytes58mm, Charsets.US_ASCII)
        assertTrue(text58mm.contains("Munzo Mart Retail POS"))
        assertTrue(text58mm.contains("Cashier: cashier_maria"))
        assertTrue(text58mm.contains("Customer: Pedro Cruz"))
        assertTrue(text58mm.contains("Receipt #: 501"))
        assertTrue(text58mm.contains("San Miguel Pale Pilsen"))
        assertTrue(text58mm.contains("TOTAL AMOUNT: P224.00"))
        // Pin 2 & Pin 5 drawer kick present
        assertTrue(containsSubsequence(bytes58mm, EscPosHelper.CMD_DRAWER_KICK_PIN2))
        assertTrue(containsSubsequence(bytes58mm, EscPosHelper.CMD_DRAWER_KICK_PIN5))
        // 58mm: No auto-cut command (prevents VB@ artifact), uses feed lines
        assertFalse(containsSubsequence(bytes58mm, EscPosHelper.CMD_PAPER_CUT))
        assertTrue(containsSubsequence(bytes58mm, EscPosHelper.CMD_FEED_LINES))

        // 2. 80mm printer - 48 columns, auto paper cut
        val bytes80mm = EscPosHelper.buildReceiptEscPos(
            storeConfig = sampleStoreConfig,
            transaction = tx,
            items = items,
            is80mm = true,
            kickDrawerOnPrint = false,
            hasAutoCutter = true
        )
        val text80mm = String(bytes80mm, Charsets.US_ASCII)
        assertTrue(text80mm.contains("ITEM DESCRIPTION"))
        assertTrue(containsSubsequence(bytes80mm, EscPosHelper.CMD_PAPER_CUT))
        assertFalse(containsSubsequence(bytes80mm, EscPosHelper.CMD_FEED_LINES))
    }

    @Test
    fun receiptBranding_customHeaderFooterTinAndBarcode() {
        val tx = Transaction(
            id = 777,
            timestamp = 1700000000000L,
            cashierUsername = "cashier_juan",
            customerName = "Corazon Aquino",
            subtotal = 100.0,
            taxAmount = 12.0,
            totalAmount = 112.0,
            cashPaid = 150.0,
            changeAmount = 38.0,
            paymentMethod = "CASH"
        )
        val items = listOf(
            TransactionItem(id = 1, transactionId = 777, productId = 1, productName = "Silver Swan Soy Sauce 1L", price = 56.0, quantity = 2)
        )

        val bytes = EscPosHelper.buildReceiptEscPos(
            storeConfig = sampleStoreConfig,
            transaction = tx,
            items = items,
            is80mm = false,
            kickDrawerOnPrint = true,
            hasAutoCutter = false,
            customHeader = "Branch #3 - Pasay Commercial Hub",
            customFooter = "Maraming Salamat Po! Keep this slip for warranty.",
            storeTin = "VAT Reg. TIN: 987-654-321-000",
            storeAddress = "99 Libertad Street, Pasay City, Metro Manila",
            printBarcode = true
        )

        val text = String(bytes, Charsets.US_ASCII)
        // Store details & TIN branding
        assertTrue(text.contains("Munzo Mart Retail POS"))
        assertTrue(text.contains("99 Libertad Street, Pasay City, Metro Manila"))
        assertTrue(text.contains("VAT Reg. TIN: 987-654-321-000"))
        assertTrue(text.contains("Branch #3 - Pasay Commercial Hub"))
        assertTrue(text.contains("Cashier: cashier_juan"))
        assertTrue(text.contains("Customer: Corazon Aquino"))
        assertTrue(text.contains("VATable Sales: P100.00"))
        assertTrue(text.contains("12% VAT (Inc.): P12.00"))
        assertTrue(text.contains("Maraming Salamat Po! Keep this slip for warranty."))

        // 1D Barcode: Code 128 format of "TX-777"
        assertTrue(containsSubsequence(bytes, "TX-777".toByteArray(Charsets.US_ASCII)))
        assertTrue(containsSubsequence(bytes, byteArrayOf(0x1D, 0x6B, 73))) // GS k 73
    }

    @Test
    fun refundReceipt_officialVoucherWithTinCashierAndBarcode() {
        val returnTx = ReturnTransaction(
            id = 99,
            originalTransactionId = 777,
            timestamp = 1700000000000L,
            cashierUsername = "supervisor_anna",
            customerName = "Corazon Aquino",
            totalRefundAmount = 56.0,
            refundMethod = "CASH",
            returnReason = "Expired / Leaking bottle",
            notes = "Replaced with fresh stock"
        )
        val returnItems = listOf(
            ReturnItem(
                id = 1,
                returnTransactionId = 99,
                productId = 1,
                productName = "Silver Swan Soy Sauce 1L",
                quantity = 1,
                unitPrice = 56.0,
                refundAmount = 56.0,
                uomName = "bottle",
                restockToInventory = false
            )
        )

        val bytes = EscPosHelper.buildRefundReceiptEscPos(
            storeConfig = sampleStoreConfig,
            returnTransaction = returnTx,
            items = returnItems,
            is80mm = false,
            kickDrawerOnPrint = true,
            hasAutoCutter = false,
            customHeader = "Official Return Voucher",
            customFooter = "Authorized by Store Supervisor",
            storeTin = "987-654-321-000",
            printBarcode = true
        )

        val text = String(bytes, Charsets.US_ASCII)
        assertTrue(text.contains("*** RETURN / REFUND VOUCHER ***"))
        assertTrue(text.contains("Return Slip #: 99"))
        assertTrue(text.contains("Original Sale #: 777"))
        assertTrue(text.contains("Cashier: supervisor_anna"))
        assertTrue(text.contains("Return Reason: Expired / Leaking bottle"))
        assertTrue(text.contains("[Damaged/Write-Off]"))
        assertTrue(text.contains("TOTAL REFUNDED:"))
        assertTrue(text.contains("P56.00"))
        assertTrue(text.contains("VAT Reg. TIN: 987-654-321-000"))
        assertTrue(text.contains("Authorized by Store Supervisor"))

        // Refund barcode
        assertTrue(containsSubsequence(bytes, "REF-99".toByteArray(Charsets.US_ASCII)))
        // Drawer kick pulse for cash refund
        assertTrue(containsSubsequence(bytes, EscPosHelper.CMD_DRAWER_KICK_PIN2))
    }

    @Test
    fun shiftHandoverBalanceSlips_reconciliationThermalFormatting() {
        val reconciliation = ShiftReconciliation(
            id = 1,
            sessionId = 12,
            timestamp = 1700000000000L,
            cashierUsername = "cashier1",
            count1000 = 5,
            count500 = 4,
            count200 = 5,
            count100 = 10,
            count50 = 10,
            count20 = 20,
            countCoins = 45.50,
            totalCountedCash = 9945.50,
            expectedCash = 10000.00,
            cashVariance = -54.50,
            countedGCash = 4500.00,
            expectedGCash = 4500.00,
            gcashVariance = 0.0,
            countedSmartLoad = 1200.0,
            expectedSmartLoad = 1200.0,
            countedGlobeLoad = 800.0,
            expectedGlobeLoad = 800.0,
            hasDiscrepancy = true,
            discrepancyReason = "₱54.50 loose coin shortage from change fund",
            managerOverrideApproved = true,
            managerUsername = "admin_supervisor"
        )

        val formula = EscPosHelper.DrawerBalancingSummary(
            startingFloat = 2000.0,
            cashSales = 8500.0,
            cashIn = 500.0,
            cashOut = 800.0,
            cashRefunds = 200.0
        )

        val slipBytes = EscPosHelper.buildShiftReconciliationReceipt(
            storeConfig = sampleStoreConfig,
            reconciliation = reconciliation,
            startTime = 1699971200000L,
            formulaSummary = formula,
            is80mm = false,
            hasAutoCutter = false,
            printBarcode = true
        )

        val text = String(slipBytes, Charsets.US_ASCII)
        assertTrue(text.contains("SHIFT HANDOVER RECONCILIATION"))
        assertTrue(text.contains("Session ID:  #SES-12"))
        assertTrue(text.contains("Cashier:     cashier1"))
        // Cash breakdown
        assertTrue(text.contains("1,000 x 5:"))
        assertTrue(text.contains("500 x 4:"))
        assertTrue(text.contains("Total Counted Cash:"))
        assertTrue(text.contains("P9945.50"))
        // Drawer Formula Breakdown
        assertTrue(text.contains("Starting Float:"))
        assertTrue(text.contains("P2000.00"))
        assertTrue(text.contains("(+) Cash Sales:"))
        assertTrue(text.contains("(+) Cash In:"))
        assertTrue(text.contains("(-) Cash Out:"))
        assertTrue(text.contains("(-) Cash Refunds:"))
        assertTrue(text.contains("SHORT -P54.50"))
        // Discrepancy override audit
        assertTrue(text.contains("DISCREPANCY OVERRIDE AUDIT"))
        assertTrue(text.contains("Approved by: admin_supervisor"))
        assertTrue(text.contains("54.50 loose coin shortage"))
        // Signatures
        assertTrue(text.contains("Cashier Signature"))
        assertTrue(text.contains("Manager Signature"))
        // Barcode
        assertTrue(containsSubsequence(slipBytes, "SES-12".toByteArray(Charsets.US_ASCII)))
    }

    @Test
    fun drawerKickAndCashInOut_pettyCashAndOperationalDisbursements() {
        // 1. Drawer Kick command pulses
        val kickBytes = EscPosHelper.buildDrawerKickBytes()
        assertTrue(containsSubsequence(kickBytes, EscPosHelper.CMD_INIT))
        assertTrue(containsSubsequence(kickBytes, EscPosHelper.CMD_DRAWER_KICK_PIN2))
        assertTrue(containsSubsequence(kickBytes, EscPosHelper.CMD_DRAWER_KICK_PIN5))

        // 2. Petty cash insertion (Cash In)
        val cashInTx = DrawerTransaction(
            id = 15,
            sessionId = 5,
            timestamp = 1700000000000L,
            cashierUsername = "cashier1",
            amount = 1000.0, // Positive for cash in
            type = "CASH_IN",
            reason = "Float top-up for peak evening rush"
        )
        val balBeforeIn = 3500.0
        val balAfterIn = StorePointRepository.roundMoney(balBeforeIn + cashInTx.amount)
        assertEquals(4500.0, balAfterIn, 0.0)

        val inSlipBytes = EscPosHelper.buildDrawerSlipEscPos(
            storeConfig = sampleStoreConfig,
            drawerTransaction = cashInTx,
            balanceBefore = balBeforeIn,
            balanceAfter = balAfterIn,
            is80mm = false,
            kickDrawerOnPrint = true,
            hasAutoCutter = false,
            storeTin = "987-654-321-000",
            printBarcode = true
        )
        val inText = String(inSlipBytes, Charsets.US_ASCII)
        assertTrue(inText.contains("CASH-IN / FLOAT INSERTION"))
        assertTrue(inText.contains("Slip #: DRW-15"))
        assertTrue(inText.contains("Session #: #SES-5"))
        assertTrue(inText.contains("Float top-up for peak evening rush"))
        assertTrue(inText.contains("CASH INFLOW:"))
        assertTrue(inText.contains("P1000.00"))
        assertTrue(inText.contains("Drawer Before:"))
        assertTrue(inText.contains("P3500.00"))
        assertTrue(inText.contains("Adjustment:"))
        assertTrue(inText.contains("+P1000.00"))
        assertTrue(inText.contains("New Drawer Balance:"))
        assertTrue(inText.contains("P4500.00"))
        assertTrue(containsSubsequence(inSlipBytes, "DRW-15".toByteArray(Charsets.US_ASCII)))
        assertTrue(containsSubsequence(inSlipBytes, EscPosHelper.CMD_DRAWER_KICK_PIN2))

        // 3. Operational cash disbursement (Cash Out / Petty Cash payout)
        val cashOutTx = DrawerTransaction(
            id = 16,
            sessionId = 5,
            timestamp = 1700000000000L,
            cashierUsername = "cashier1",
            amount = -250.0, // Negative for cash out
            type = "CASH_OUT",
            reason = "Paid ice delivery (5 bags for beverage chiller)"
        )
        val balBeforeOut = 4500.0
        val balAfterOut = StorePointRepository.roundMoney(balBeforeOut + cashOutTx.amount)
        assertEquals(4250.0, balAfterOut, 0.0)

        val outSlipBytes = EscPosHelper.buildDrawerSlipEscPos(
            storeConfig = sampleStoreConfig,
            drawerTransaction = cashOutTx,
            balanceBefore = balBeforeOut,
            balanceAfter = balAfterOut,
            is80mm = true,
            kickDrawerOnPrint = false,
            hasAutoCutter = true,
            printBarcode = true
        )
        val outText = String(outSlipBytes, Charsets.US_ASCII)
        assertTrue(outText.contains("CASH-OUT / DRAWER PAYOUT"))
        assertTrue(outText.contains("Slip #: DRW-16"))
        assertTrue(outText.contains("Paid ice delivery (5 bags for beverage chiller)"))
        assertTrue(outText.contains("CASH DISBURSED:"))
        assertTrue(outText.contains("P250.00"))
        assertTrue(outText.contains("Drawer Before:"))
        assertTrue(outText.contains("P4500.00"))
        assertTrue(outText.contains("Adjustment:"))
        assertTrue(outText.contains("-P250.00"))
        assertTrue(outText.contains("New Drawer Balance:"))
        assertTrue(outText.contains("P4250.00"))
        assertTrue(containsSubsequence(outSlipBytes, "DRW-16".toByteArray(Charsets.US_ASCII)))
        assertTrue(containsSubsequence(outSlipBytes, EscPosHelper.CMD_PAPER_CUT))
    }
}
