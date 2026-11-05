package com.munzo.storepoint.util

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.munzo.storepoint.data.StoreConfig
import com.munzo.storepoint.data.Transaction
import com.munzo.storepoint.data.TransactionItem
import com.munzo.storepoint.data.ReturnTransaction
import com.munzo.storepoint.data.ReturnItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Universal ESC/POS helper supporting standard thermal receipt printers,
 * including 58mm models like Xprinter XP-58 Plus, XP-58IIH, POS-58, and 80mm printers,
 * matching Loyverse-grade peripheral compatibility.
 */
object EscPosHelper {

    enum class PrinterType {
        SYSTEM_SPOOLER,
        NETWORK_ESCPOS,
        BLUETOOTH_ESCPOS,
        USB_ESCPOS
    }

    // Standard Bluetooth Serial Port Profile (SPP) UUID
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // ESC/POS Command Constants
    val CMD_INIT = byteArrayOf(0x1B, 0x40) // ESC @
    val CMD_CODEPAGE_CP437 = byteArrayOf(0x1B, 0x74, 0x00) // ESC t 0 (Standard CP437 ASCII table)
    val CMD_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00) // ESC a 0
    val CMD_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01) // ESC a 1
    val CMD_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02) // ESC a 2
    val CMD_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01) // ESC E 1
    val CMD_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00) // ESC E 0
    val CMD_FONT_LARGE = byteArrayOf(0x1D, 0x21, 0x11) // GS ! 0x11 (Double Width & Height)
    val CMD_FONT_NORMAL = byteArrayOf(0x1D, 0x21, 0x00) // GS ! 0x00
    val CMD_DRAWER_KICK_PIN2 = byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte()) // ESC p 0 25 250 (12V RJ11 pin 2)
    val CMD_DRAWER_KICK_PIN5 = byteArrayOf(0x1B, 0x70, 0x01, 0x19, 0xFA.toByte()) // ESC p 1 25 250 (12V RJ11 pin 5)
    val CMD_PAPER_CUT = byteArrayOf(0x1D, 0x56, 0x42, 0x00) // GS V 66 0 (Feed and full cut)
    val CMD_FEED_LINES = byteArrayOf(0x1B, 0x64, 0x05) // ESC d 5 (Feed 5 lines for manual tear bar)
    val LF = byteArrayOf(0x0A) // Line Feed

    // Barcode command constants
    val CMD_BARCODE_HRI_BELOW = byteArrayOf(0x1D, 0x48, 0x02) // GS H 2 (HRI characters printed below)
    val CMD_BARCODE_FONT_A = byteArrayOf(0x1D, 0x66, 0x00) // GS f 0 (Font A for HRI characters)

    data class DiscoveredBluetoothPrinter(
        val name: String,
        val macAddress: String,
        val isLikelyPosPrinter: Boolean
    )

    /**
     * Sanitizes strings for thermal printers (XP-58 Plus and 58mm/80mm):
     * - Converts multi-byte currency symbols (e.g. Philippine Peso '₱') to 'P' so CP437 thermal
     *   controllers do not print 'â‚±' garbled glyphs and do not push columns out of alignment.
     * - Replaces curly quotes, non-breaking spaces, and typographic dashes with pure ASCII.
     */
    fun sanitizeForEscPos(text: String, currencySymbol: String = "P"): String {
        var clean = text
        if (clean.contains("₱") || currencySymbol == "₱") {
            clean = clean.replace("₱", "P")
        }
        return clean
            .replace('\u00A0', ' ')
            .replace('“', '"').replace('”', '"')
            .replace('‘', '\'').replace('’', '\'')
            .replace('—', '-').replace('–', '-')
    }

    /**
     * Returns a safe single-width ASCII representation of the currency symbol.
     */
    fun safeCurrencySymbol(symbol: String): String {
        return if (symbol.isBlank() || symbol == "₱") "P" else symbol.trim()
    }

    /**
     * Builds standard ESC/POS formatted bytes for cash drawer kick.
     */
    fun buildDrawerKickBytes(): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(CMD_INIT)
        out.write(CMD_DRAWER_KICK_PIN2)
        out.write(CMD_DRAWER_KICK_PIN5)
        return out.toByteArray()
    }

    /**
     * Builds ESC/POS barcode bytes for standard 1D thermal receipt printing.
     * Uses Code 128 (Format 2: GS k 73 n) or Code 39 (GS k 69 n).
     */
    fun buildBarcodeEscPos(
        data: String,
        isCode128: Boolean = true,
        height: Int = 50,
        width: Int = 2
    ): ByteArray {
        val clean = data.trim().filter { it.isLetterOrDigit() || it == '-' }
        if (clean.isBlank()) return byteArrayOf()

        val out = ByteArrayOutputStream()
        // Center alignment
        out.write(CMD_ALIGN_CENTER)
        // Select HRI character print position (2 = below barcode)
        out.write(CMD_BARCODE_HRI_BELOW)
        // Select HRI font (0 = Font A)
        out.write(CMD_BARCODE_FONT_A)
        // Set barcode height in dots
        out.write(byteArrayOf(0x1D, 0x68, height.coerceIn(20, 255).toByte()))
        // Set barcode module width
        out.write(byteArrayOf(0x1D, 0x77, width.coerceIn(1, 4).toByte()))

        if (isCode128) {
            // Code 128 Format 2 (GS k 73 n) with Code Set B prefix {B (0x7B, 0x42)
            val asciiBytes = clean.toByteArray(Charsets.US_ASCII)
            val payload = ByteArrayOutputStream()
            payload.write(0x7B) // '{'
            payload.write(0x42) // 'B'
            payload.write(asciiBytes)
            val bytes = payload.toByteArray()

            out.write(byteArrayOf(0x1D, 0x6B, 73, bytes.size.toByte()))
            out.write(bytes)
        } else {
            // Code 39 Format 2 (GS k 69 n)
            val asciiBytes = clean.uppercase(Locale.US).toByteArray(Charsets.US_ASCII)
            out.write(byteArrayOf(0x1D, 0x6B, 69, asciiBytes.size.toByte()))
            out.write(asciiBytes)
        }
        out.write(LF)
        return out.toByteArray()
    }

    /**
     * Builds a complete, formatted ESC/POS receipt for thermal printers (58mm XP-58 or 80mm width).
     * Automatically handles manual tear-bar feeding without cut-code corruption for XP-58 Plus.
     */
    fun buildReceiptEscPos(
        storeConfig: StoreConfig,
        transaction: Transaction,
        items: List<TransactionItem>,
        is80mm: Boolean = false,
        kickDrawerOnPrint: Boolean = true,
        hasAutoCutter: Boolean = is80mm,
        customHeader: String? = null,
        customFooter: String? = null,
        storeTin: String? = null,
        storeAddress: String? = null,
        printBarcode: Boolean = true
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val totalCols = if (is80mm) 48 else 32
        val curr = safeCurrencySymbol(storeConfig.currencySymbol)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val storeName = sanitizeForEscPos(storeConfig.storeName, curr)

        // Initialize Printer & Select Standard Code Page (CP437)
        out.write(CMD_INIT)
        out.write(CMD_CODEPAGE_CP437)

        // Cash Drawer Kick Pulse (if enabled)
        if (kickDrawerOnPrint) {
            out.write(CMD_DRAWER_KICK_PIN2)
            out.write(CMD_DRAWER_KICK_PIN5)
        }

        // Header: Store Name (Centered, Large, Bold)
        out.write(CMD_ALIGN_CENTER)
        out.write(CMD_FONT_LARGE)
        out.write(CMD_BOLD_ON)
        out.write("$storeName\n".toByteArray(Charsets.US_ASCII))
        out.write(CMD_FONT_NORMAL)
        out.write(CMD_BOLD_OFF)

        if (!storeAddress.isNullOrBlank()) {
            out.write("${sanitizeForEscPos(storeAddress, curr)}\n".toByteArray(Charsets.US_ASCII))
        }
        if (!storeTin.isNullOrBlank()) {
            val tinStr = if (storeTin.contains("TIN", ignoreCase = true)) storeTin else "VAT Reg. TIN: $storeTin"
            out.write("${sanitizeForEscPos(tinStr, curr)}\n".toByteArray(Charsets.US_ASCII))
        }
        if (!customHeader.isNullOrBlank()) {
            out.write("${sanitizeForEscPos(customHeader, curr)}\n".toByteArray(Charsets.US_ASCII))
        }
        out.write("Official POS Sales Receipt\n".toByteArray(Charsets.US_ASCII))
        out.write(dividerLine(totalCols).toByteArray(Charsets.US_ASCII))

        // Transaction Meta (Left Aligned)
        out.write(CMD_ALIGN_LEFT)
        out.write("Receipt #: ${transaction.id}\n".toByteArray(Charsets.US_ASCII))
        out.write("Date: ${sdf.format(Date(transaction.timestamp))}\n".toByteArray(Charsets.US_ASCII))
        out.write("Cashier: ${sanitizeForEscPos(transaction.cashierUsername, curr)}\n".toByteArray(Charsets.US_ASCII))
        if (transaction.customerName.isNotBlank()) {
            out.write("Customer: ${sanitizeForEscPos(transaction.customerName, curr)}\n".toByteArray(Charsets.US_ASCII))
        }
        out.write("Payment: ${sanitizeForEscPos(transaction.paymentMethod, curr)}\n".toByteArray(Charsets.US_ASCII))
        out.write(dividerLine(totalCols).toByteArray(Charsets.US_ASCII))

        // Column Header
        val colHeader = if (is80mm) {
            padColumns("ITEM DESCRIPTION", "QTY", "PRICE", "TOTAL", totalCols)
        } else {
            padColumns("ITEM", "QTY", "AMT", totalCols)
        }
        out.write(CMD_BOLD_ON)
        out.write("$colHeader\n".toByteArray(Charsets.US_ASCII))
        out.write(CMD_BOLD_OFF)
        out.write(dividerLine(totalCols).toByteArray(Charsets.US_ASCII))

        // Items
        items.forEach { item ->
            val lineTotalStr = String.format(Locale.US, "%.2f", item.price * item.quantity)
            val priceStr = String.format(Locale.US, "%.2f", item.price)
            val cleanName = sanitizeForEscPos(item.productName, curr)
            if (is80mm) {
                val row = padColumns(cleanName, item.quantity.toString(), priceStr, lineTotalStr, totalCols)
                out.write("$row\n".toByteArray(Charsets.US_ASCII))
            } else {
                // 32-col layout tailored for XP-58 Plus: Name on line 1, Qty x Price and Line Total on line 2
                val truncatedName = if (cleanName.length > 30) cleanName.take(28) + ".." else cleanName
                out.write("$truncatedName\n".toByteArray(Charsets.US_ASCII))
                val detail = "  ${item.quantity} x $priceStr"
                val row = padColumns(detail, lineTotalStr, totalCols)
                out.write("$row\n".toByteArray(Charsets.US_ASCII))
            }
        }
        out.write(dividerLine(totalCols).toByteArray(Charsets.US_ASCII))

        // Totals (Right Aligned - Philippine VAT standard)
        out.write(CMD_ALIGN_RIGHT)
        val subtotalStr = "VATable Sales: $curr${String.format(Locale.US, "%.2f", transaction.subtotal)}"
        out.write("$subtotalStr\n".toByteArray(Charsets.US_ASCII))

        if (transaction.taxAmount > 0.0) {
            val taxStr = "12% VAT (Inc.): $curr${String.format(Locale.US, "%.2f", transaction.taxAmount)}"
            out.write("$taxStr\n".toByteArray(Charsets.US_ASCII))
        }

        out.write(CMD_BOLD_ON)
        val totalStr = "TOTAL AMOUNT: $curr${String.format(Locale.US, "%.2f", transaction.totalAmount)}"
        out.write("$totalStr\n".toByteArray(Charsets.US_ASCII))
        out.write(CMD_BOLD_OFF)

        val paidStr = "Amount Tendered: $curr${String.format(Locale.US, "%.2f", transaction.cashPaid)}"
        out.write("$paidStr\n".toByteArray(Charsets.US_ASCII))

        val changeStr = "Change: $curr${String.format(Locale.US, "%.2f", transaction.changeAmount)}"
        out.write("$changeStr\n".toByteArray(Charsets.US_ASCII))
        out.write(dividerLine(totalCols).toByteArray(Charsets.US_ASCII))

        // Barcode printing
        if (printBarcode && transaction.id > 0) {
            val barcodeData = "TX-${transaction.id}"
            out.write(buildBarcodeEscPos(barcodeData, isCode128 = true, height = 45, width = if (is80mm) 2 else 2))
            out.write(dividerLine(totalCols).toByteArray(Charsets.US_ASCII))
        }

        // Footer (Centered)
        out.write(CMD_ALIGN_CENTER)
        if (!customFooter.isNullOrBlank()) {
            out.write("${sanitizeForEscPos(customFooter, curr)}\n".toByteArray(Charsets.US_ASCII))
        } else {
            out.write("Thank you for your business!\n".toByteArray(Charsets.US_ASCII))
            out.write("Please come again.\n".toByteArray(Charsets.US_ASCII))
        }

        // Paper Finish: Cut if auto-cutter is present; otherwise feed lines past tear-bar (XP-58 Plus)
        if (hasAutoCutter) {
            out.write("\n\n".toByteArray(Charsets.US_ASCII))
            out.write(CMD_PAPER_CUT)
        } else {
            // XP-58 Plus manual tear bar: feed 5 lines so receipt text clears the tear blade
            out.write(CMD_FEED_LINES)
            out.write("\n\n".toByteArray(Charsets.US_ASCII))
        }

        return out.toByteArray()
    }

    /**
     * Builds an official thermal return/refund slip in ESC/POS format.
     */
    fun buildRefundReceiptEscPos(
        storeConfig: StoreConfig,
        returnTransaction: ReturnTransaction,
        items: List<ReturnItem>,
        is80mm: Boolean = false,
        kickDrawerOnPrint: Boolean = false,
        hasAutoCutter: Boolean = is80mm,
        customHeader: String? = null,
        customFooter: String? = null,
        storeTin: String? = null,
        storeAddress: String? = null,
        printBarcode: Boolean = true
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val totalCols = if (is80mm) 48 else 32
        val curr = safeCurrencySymbol(storeConfig.currencySymbol)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val storeName = sanitizeForEscPos(storeConfig.storeName, curr)

        // Initialize Printer & Select Standard Code Page (CP437)
        out.write(CMD_INIT)
        out.write(CMD_CODEPAGE_CP437)

        // Cash Drawer Kick Pulse (if cash refund payout enabled)
        if (kickDrawerOnPrint) {
            out.write(CMD_DRAWER_KICK_PIN2)
            out.write(CMD_DRAWER_KICK_PIN5)
        }

        // Header: Store Name (Centered, Large, Bold)
        out.write(CMD_ALIGN_CENTER)
        out.write(CMD_FONT_LARGE)
        out.write(CMD_BOLD_ON)
        out.write("$storeName\n".toByteArray(Charsets.US_ASCII))
        out.write(CMD_FONT_NORMAL)
        out.write(CMD_BOLD_OFF)

        if (!storeAddress.isNullOrBlank()) {
            out.write("${sanitizeForEscPos(storeAddress, curr)}\n".toByteArray(Charsets.US_ASCII))
        }
        if (!storeTin.isNullOrBlank()) {
            val tinStr = if (storeTin.contains("TIN", ignoreCase = true)) storeTin else "VAT Reg. TIN: $storeTin"
            out.write("${sanitizeForEscPos(tinStr, curr)}\n".toByteArray(Charsets.US_ASCII))
        }
        if (!customHeader.isNullOrBlank()) {
            out.write("${sanitizeForEscPos(customHeader, curr)}\n".toByteArray(Charsets.US_ASCII))
        }

        out.write("*** RETURN / REFUND VOUCHER ***\n".toByteArray(Charsets.US_ASCII))
        out.write(dividerLine(totalCols).toByteArray(Charsets.US_ASCII))

        // Transaction Meta (Left Aligned)
        out.write(CMD_ALIGN_LEFT)
        out.write("Return Slip #: ${returnTransaction.id}\n".toByteArray(Charsets.US_ASCII))
        out.write("Original Sale #: ${returnTransaction.originalTransactionId}\n".toByteArray(Charsets.US_ASCII))
        out.write("Date: ${sdf.format(Date(returnTransaction.timestamp))}\n".toByteArray(Charsets.US_ASCII))
        out.write("Cashier: ${sanitizeForEscPos(returnTransaction.cashierUsername, curr)}\n".toByteArray(Charsets.US_ASCII))
        if (returnTransaction.customerName.isNotBlank()) {
            out.write("Customer: ${sanitizeForEscPos(returnTransaction.customerName, curr)}\n".toByteArray(Charsets.US_ASCII))
        }
        out.write("Refund Tender: ${sanitizeForEscPos(returnTransaction.refundMethod, curr)}\n".toByteArray(Charsets.US_ASCII))
        out.write("Return Reason: ${sanitizeForEscPos(returnTransaction.returnReason, curr)}\n".toByteArray(Charsets.US_ASCII))
        if (returnTransaction.notes.isNotBlank()) {
            out.write("Notes: ${sanitizeForEscPos(returnTransaction.notes, curr)}\n".toByteArray(Charsets.US_ASCII))
        }
        out.write(dividerLine(totalCols).toByteArray(Charsets.US_ASCII))

        // Column Header
        val colHeader = if (is80mm) {
            padColumns("RETURNED ITEM", "QTY", "UNIT", "REFUND", totalCols)
        } else {
            padColumns("ITEM", "QTY", "REFUND", totalCols)
        }
        out.write(CMD_BOLD_ON)
        out.write("$colHeader\n".toByteArray(Charsets.US_ASCII))
        out.write(CMD_BOLD_OFF)
        out.write(dividerLine(totalCols).toByteArray(Charsets.US_ASCII))

        // Items
        items.forEach { item ->
            val sanitizedName = sanitizeForEscPos(item.productName, curr)
            val conditionStr = if (item.restockToInventory) "[Restocked]" else "[Damaged/Write-Off]"
            val unitStr = "$curr${String.format(Locale.US, "%.2f", item.unitPrice)}"
            val refundStr = "$curr${String.format(Locale.US, "%.2f", item.refundAmount)}"

            if (is80mm) {
                val line = padColumns(sanitizedName, "${item.quantity}", unitStr, refundStr, totalCols)
                out.write("$line\n".toByteArray(Charsets.US_ASCII))
                out.write("  $conditionStr\n".toByteArray(Charsets.US_ASCII))
            } else {
                if (sanitizedName.length > 14) {
                    out.write("$sanitizedName\n".toByteArray(Charsets.US_ASCII))
                    val subLine = padColumns("  $conditionStr", "${item.quantity}", refundStr, totalCols)
                    out.write("$subLine\n".toByteArray(Charsets.US_ASCII))
                } else {
                    val line = padColumns(sanitizedName, "${item.quantity}", refundStr, totalCols)
                    out.write("$line\n".toByteArray(Charsets.US_ASCII))
                    out.write("  $conditionStr\n".toByteArray(Charsets.US_ASCII))
                }
            }
        }

        out.write(dividerLine(totalCols).toByteArray(Charsets.US_ASCII))

        // Total Refunded
        val totalRefundStr = "$curr${String.format(Locale.US, "%.2f", returnTransaction.totalRefundAmount)}"
        val totalLine = padColumns("TOTAL REFUNDED:", totalRefundStr, totalCols)
        out.write(CMD_BOLD_ON)
        out.write(CMD_FONT_LARGE)
        out.write("$totalLine\n".toByteArray(Charsets.US_ASCII))
        out.write(CMD_FONT_NORMAL)
        out.write(CMD_BOLD_OFF)
        out.write(dividerLine(totalCols).toByteArray(Charsets.US_ASCII))

        // Barcode
        if (printBarcode && returnTransaction.id > 0) {
            val barcodeData = "REF-${returnTransaction.id}"
            out.write(buildBarcodeEscPos(barcodeData, isCode128 = true, height = 45, width = if (is80mm) 2 else 2))
            out.write(dividerLine(totalCols).toByteArray(Charsets.US_ASCII))
        }

        // Footer
        out.write(CMD_ALIGN_CENTER)
        if (!customFooter.isNullOrBlank()) {
            out.write("${sanitizeForEscPos(customFooter, curr)}\n".toByteArray(Charsets.US_ASCII))
        } else {
            out.write("REFUND PROCESSED AT CASHIER REGISTER\n".toByteArray(Charsets.US_ASCII))
            out.write("Authorized Supervisor Audit Copy\n".toByteArray(Charsets.US_ASCII))
        }
        out.write("\n".toByteArray(Charsets.US_ASCII))

        // Cut / Tear Bar Feed
        if (hasAutoCutter) {
            out.write(CMD_PAPER_CUT)
        } else {
            out.write(CMD_FEED_LINES)
            out.write("\n\n".toByteArray(Charsets.US_ASCII))
        }

        return out.toByteArray()
    }

    /**
     * Builds an official Cash-In / Cash-Out thermal drawer slip in ESC/POS format
     * for logging petty cash insertions and operational cash disbursements.
     */
    fun buildDrawerSlipEscPos(
        storeConfig: StoreConfig,
        drawerTransaction: com.munzo.storepoint.data.DrawerTransaction,
        balanceBefore: Double? = null,
        balanceAfter: Double? = null,
        is80mm: Boolean = false,
        kickDrawerOnPrint: Boolean = true,
        hasAutoCutter: Boolean = is80mm,
        customHeader: String? = null,
        storeTin: String? = null,
        printBarcode: Boolean = true
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val totalCols = if (is80mm) 48 else 32
        val curr = safeCurrencySymbol(storeConfig.currencySymbol)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val storeName = sanitizeForEscPos(storeConfig.storeName, curr)
        val isCashIn = drawerTransaction.amount >= 0.0

        // Initialize Printer & Select Standard Code Page (CP437)
        out.write(CMD_INIT)
        out.write(CMD_CODEPAGE_CP437)

        // Cash Drawer Kick Pulse (if enabled)
        if (kickDrawerOnPrint) {
            out.write(CMD_DRAWER_KICK_PIN2)
            out.write(CMD_DRAWER_KICK_PIN5)
        }

        // Header: Store Name (Centered, Large, Bold)
        out.write(CMD_ALIGN_CENTER)
        out.write(CMD_FONT_LARGE)
        out.write(CMD_BOLD_ON)
        out.write("$storeName\n".toByteArray(Charsets.US_ASCII))
        out.write(CMD_FONT_NORMAL)
        out.write(CMD_BOLD_OFF)

        if (!storeTin.isNullOrBlank()) {
            val tinFormatted = if (storeTin.contains("TIN", ignoreCase = true)) storeTin else "VAT Reg. TIN: $storeTin"
            out.write("${sanitizeForEscPos(tinFormatted, curr)}\n".toByteArray(Charsets.US_ASCII))
        }
        if (!customHeader.isNullOrBlank()) {
            out.write("${sanitizeForEscPos(customHeader, curr)}\n".toByteArray(Charsets.US_ASCII))
        }

        out.write(CMD_BOLD_ON)
        val slipTitle = if (isCashIn) {
            "*** CASH-IN / FLOAT INSERTION ***\n"
        } else {
            "*** CASH-OUT / DRAWER PAYOUT ***\n"
        }
        out.write(slipTitle.toByteArray(Charsets.US_ASCII))
        out.write(CMD_BOLD_OFF)
        out.write(dividerLine(totalCols).toByteArray(Charsets.US_ASCII))

        // Transaction Meta (Left Aligned)
        out.write(CMD_ALIGN_LEFT)
        out.write("Slip #: DRW-${drawerTransaction.id}\n".toByteArray(Charsets.US_ASCII))
        out.write("Session #: #SES-${drawerTransaction.sessionId}\n".toByteArray(Charsets.US_ASCII))
        out.write("Date: ${sdf.format(Date(drawerTransaction.timestamp))}\n".toByteArray(Charsets.US_ASCII))
        out.write("Cashier: ${sanitizeForEscPos(drawerTransaction.cashierUsername, curr)}\n".toByteArray(Charsets.US_ASCII))
        out.write("Action: ${sanitizeForEscPos(drawerTransaction.type, curr)}\n".toByteArray(Charsets.US_ASCII))
        out.write("Reason: ${sanitizeForEscPos(drawerTransaction.reason, curr)}\n".toByteArray(Charsets.US_ASCII))
        out.write(dividerLine(totalCols).toByteArray(Charsets.US_ASCII))

        // Amount Display (Large & Bold)
        val absAmount = Math.abs(drawerTransaction.amount)
        val amountStr = "$curr${String.format(Locale.US, "%.2f", absAmount)}"
        val amountLabel = if (isCashIn) "CASH INFLOW:" else "CASH DISBURSED:"
        out.write(CMD_ALIGN_RIGHT)
        out.write(CMD_BOLD_ON)
        out.write(CMD_FONT_LARGE)
        out.write(padColumns(amountLabel, amountStr, totalCols).toByteArray(Charsets.US_ASCII))
        out.write(LF)
        out.write(CMD_FONT_NORMAL)
        out.write(CMD_BOLD_OFF)

        // Drawer Ledger Before & After
        if (balanceBefore != null && balanceAfter != null) {
            out.write(dividerLine(totalCols).toByteArray(Charsets.US_ASCII))
            out.write(padColumns("Drawer Before:", "$curr${String.format(Locale.US, "%.2f", balanceBefore)}", totalCols).toByteArray(Charsets.US_ASCII))
            out.write(LF)
            val signStr = if (isCashIn) "+$amountStr" else "-$amountStr"
            out.write(padColumns("Adjustment:", signStr, totalCols).toByteArray(Charsets.US_ASCII))
            out.write(LF)
            out.write(CMD_BOLD_ON)
            out.write(padColumns("New Drawer Balance:", "$curr${String.format(Locale.US, "%.2f", balanceAfter)}", totalCols).toByteArray(Charsets.US_ASCII))
            out.write(LF)
            out.write(CMD_BOLD_OFF)
        }

        out.write(dividerLine(totalCols).toByteArray(Charsets.US_ASCII))

        // Barcode
        if (printBarcode && drawerTransaction.id > 0) {
            val barcodeData = "DRW-${drawerTransaction.id}"
            out.write(buildBarcodeEscPos(barcodeData, isCode128 = true, height = 45, width = if (is80mm) 2 else 2))
            out.write(dividerLine(totalCols).toByteArray(Charsets.US_ASCII))
        }

        // Signatures (Cashier and Supervisor)
        out.write(CMD_ALIGN_CENTER)
        out.write("Petty Cash / Drawer Audit Copy\n\n".toByteArray(Charsets.US_ASCII))
        out.write(padColumns("Cashier Signature", "Manager Signature", totalCols).toByteArray(Charsets.US_ASCII))
        out.write(LF)
        out.write(padColumns("_________________", "_________________", totalCols).toByteArray(Charsets.US_ASCII))
        out.write(LF)

        // Paper Cut / Feed Lines
        if (hasAutoCutter) {
            out.write("\n\n".toByteArray(Charsets.US_ASCII))
            out.write(CMD_PAPER_CUT)
        } else {
            out.write(CMD_FEED_LINES)
            out.write("\n\n".toByteArray(Charsets.US_ASCII))
        }

        return out.toByteArray()
    }

    /**
     * Builds a hardware test ticket with drawer kick pulse.
     */
    fun buildTestTicket(
        is80mm: Boolean = false,
        kickDrawer: Boolean = false,
        hasAutoCutter: Boolean = is80mm
    ): ByteArray {
        val totalCols = if (is80mm) 48 else 32
        val out = ByteArrayOutputStream()
        out.write(CMD_INIT)
        out.write(CMD_CODEPAGE_CP437)
        if (kickDrawer) {
            out.write(CMD_DRAWER_KICK_PIN2)
            out.write(CMD_DRAWER_KICK_PIN5)
        }
        out.write(CMD_ALIGN_CENTER)
        out.write(CMD_BOLD_ON)
        out.write("=== STOREPOINT POS ===\n".toByteArray(Charsets.US_ASCII))
        out.write("HARDWARE PRINTER TEST\n".toByteArray(Charsets.US_ASCII))
        out.write(CMD_BOLD_OFF)
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        out.write("Test Date: ${format.format(Date())}\n".toByteArray(Charsets.US_ASCII))
        out.write("Model: ${if (is80mm) "80mm Wide (48 Cols)" else "58mm Standard (XP-58 Plus)"}\n".toByteArray(Charsets.US_ASCII))
        out.write("Drawer Kick: ${if (kickDrawer) "ENABLED" else "DISABLED"}\n".toByteArray(Charsets.US_ASCII))
        out.write("Auto Cutter: ${if (hasAutoCutter) "ENABLED" else "DISABLED (Manual Tear)"}\n".toByteArray(Charsets.US_ASCII))
        out.write(dividerLine(totalCols).toByteArray(Charsets.US_ASCII))
        out.write("ESC/POS Communication: OK\n".toByteArray(Charsets.US_ASCII))
        out.write("Thermal Head Status: OK\n".toByteArray(Charsets.US_ASCII))
        out.write(dividerLine(totalCols).toByteArray(Charsets.US_ASCII))
        out.write("Ready for High-Speed Checkout\n".toByteArray(Charsets.US_ASCII))

        if (hasAutoCutter) {
            out.write("\n\n".toByteArray(Charsets.US_ASCII))
            out.write(CMD_PAPER_CUT)
        } else {
            out.write(CMD_FEED_LINES)
            out.write("\n\n".toByteArray(Charsets.US_ASCII))
        }
        return out.toByteArray()
    }

    /**
     * Builds an End of Day Close Shift (Z-Reading) thermal receipt ticket.
     */
    fun buildShiftZReadingTicket(
        storeConfig: StoreConfig,
        cashierUsername: String,
        startTime: Long,
        endTime: Long,
        startingCash: Double,
        endingCash: Double,
        expectedCash: Double,
        totalSales: Double,
        cashSales: Double,
        nonCashSales: Double,
        vatableSales: Double,
        vatAmount: Double,
        payouts: Double,
        transactionCount: Int,
        is80mm: Boolean = false,
        hasAutoCutter: Boolean = is80mm
    ): ByteArray {
        val totalCols = if (is80mm) 48 else 32
        val out = ByteArrayOutputStream()
        val curr = safeCurrencySymbol(storeConfig.currencySymbol)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        out.write(CMD_INIT)
        out.write(CMD_CODEPAGE_CP437)
        out.write(CMD_ALIGN_CENTER)
        out.write(CMD_BOLD_ON)
        val title = if (storeConfig.storeName.isNotBlank()) sanitizeForEscPos(storeConfig.storeName, curr) else "STOREPOINT POS"
        out.write("$title\n".toByteArray(Charsets.US_ASCII))
        out.write("END OF DAY / Z-READING\n".toByteArray(Charsets.US_ASCII))
        out.write("SHIFT CLOSURE REPORT\n".toByteArray(Charsets.US_ASCII))
        out.write(CMD_BOLD_OFF)

        val divider = dividerLine(totalCols)
        out.write(divider.toByteArray(Charsets.US_ASCII))

        out.write(CMD_ALIGN_LEFT)
        out.write("Cashier: ${sanitizeForEscPos(cashierUsername, curr)}\n".toByteArray(Charsets.US_ASCII))
        out.write("Shift Start: ${dateFormat.format(Date(startTime))}\n".toByteArray(Charsets.US_ASCII))
        out.write("Shift End:   ${dateFormat.format(Date(endTime))}\n".toByteArray(Charsets.US_ASCII))
        out.write("Total Transactions: $transactionCount\n".toByteArray(Charsets.US_ASCII))
        out.write(divider.toByteArray(Charsets.US_ASCII))

        out.write(padColumns("Starting Float:", "$curr${String.format(Locale.US, "%.2f", startingCash)}", totalCols).toByteArray(Charsets.US_ASCII))
        out.write(LF)
        out.write(padColumns("Gross Sales:", "$curr${String.format(Locale.US, "%.2f", totalSales)}", totalCols).toByteArray(Charsets.US_ASCII))
        out.write(LF)
        out.write(padColumns(" - Cash Sales:", "$curr${String.format(Locale.US, "%.2f", cashSales)}", totalCols).toByteArray(Charsets.US_ASCII))
        out.write(LF)
        out.write(padColumns(" - Non-Cash / Digital:", "$curr${String.format(Locale.US, "%.2f", nonCashSales)}", totalCols).toByteArray(Charsets.US_ASCII))
        out.write(LF)
        out.write(padColumns("VATable Sales (Net):", "$curr${String.format(Locale.US, "%.2f", vatableSales)}", totalCols).toByteArray(Charsets.US_ASCII))
        out.write(LF)
        out.write(padColumns("12% VAT Collected:", "$curr${String.format(Locale.US, "%.2f", vatAmount)}", totalCols).toByteArray(Charsets.US_ASCII))
        out.write(LF)
        if (payouts != 0.0) {
            out.write(padColumns("Supplier Payouts:", "$curr${String.format(Locale.US, "%.2f", payouts)}", totalCols).toByteArray(Charsets.US_ASCII))
            out.write(LF)
        }
        out.write(divider.toByteArray(Charsets.US_ASCII))

        out.write(padColumns("Expected Drawer Cash:", "$curr${String.format(Locale.US, "%.2f", expectedCash)}", totalCols).toByteArray(Charsets.US_ASCII))
        out.write(LF)
        out.write(padColumns("Counted Drawer Cash:", "$curr${String.format(Locale.US, "%.2f", endingCash)}", totalCols).toByteArray(Charsets.US_ASCII))
        out.write(LF)
        val diff = endingCash - expectedCash
        val diffStr = if (Math.abs(diff) < 0.01) "BALANCED" else if (diff < 0) "SHORT: -$curr${String.format(Locale.US, "%.2f", -diff)}" else "OVER: +$curr${String.format(Locale.US, "%.2f", diff)}"
        out.write(CMD_BOLD_ON)
        out.write(padColumns("Drawer Variance:", diffStr, totalCols).toByteArray(Charsets.US_ASCII))
        out.write(LF)
        out.write(CMD_BOLD_OFF)
        out.write(divider.toByteArray(Charsets.US_ASCII))

        out.write(CMD_ALIGN_CENTER)
        out.write("End of Day Close Completed\n".toByteArray(Charsets.US_ASCII))

        if (hasAutoCutter) {
            out.write("\n\n".toByteArray(Charsets.US_ASCII))
            out.write(CMD_PAPER_CUT)
        } else {
            out.write(CMD_FEED_LINES)
            out.write("\n\n".toByteArray(Charsets.US_ASCII))
        }
        return out.toByteArray()
    }

    data class DrawerBalancingSummary(
        val startingFloat: Double = 0.0,
        val cashSales: Double = 0.0,
        val cashIn: Double = 0.0,
        val cashOut: Double = 0.0,
        val cashRefunds: Double = 0.0
    )

    /**
     * Builds a detailed Shift Handover & Dual Drawer/Wallet Reconciliation thermal receipt.
     */
    fun buildShiftReconciliationReceipt(
        storeConfig: StoreConfig,
        reconciliation: com.munzo.storepoint.data.ShiftReconciliation,
        startTime: Long,
        formulaSummary: DrawerBalancingSummary? = null,
        is80mm: Boolean = false,
        hasAutoCutter: Boolean = is80mm,
        printBarcode: Boolean = true
    ): ByteArray {
        val totalCols = if (is80mm) 48 else 32
        val out = ByteArrayOutputStream()
        val curr = safeCurrencySymbol(storeConfig.currencySymbol)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        out.write(CMD_INIT)
        out.write(CMD_CODEPAGE_CP437)
        out.write(CMD_ALIGN_CENTER)
        out.write(CMD_BOLD_ON)
        val title = if (storeConfig.storeName.isNotBlank()) sanitizeForEscPos(storeConfig.storeName, curr) else "STOREPOINT POS"
        out.write("$title\n".toByteArray(Charsets.US_ASCII))
        out.write("SHIFT HANDOVER RECONCILIATION\n".toByteArray(Charsets.US_ASCII))
        out.write("DUAL DRAWER & WALLET BALANCING\n".toByteArray(Charsets.US_ASCII))
        out.write(CMD_BOLD_OFF)

        val divider = dividerLine(totalCols)
        out.write(divider.toByteArray(Charsets.US_ASCII))

        out.write(CMD_ALIGN_LEFT)
        out.write("Cashier:     ${sanitizeForEscPos(reconciliation.cashierUsername, curr)}\n".toByteArray(Charsets.US_ASCII))
        out.write("Session ID:  #SES-${reconciliation.sessionId}\n".toByteArray(Charsets.US_ASCII))
        out.write("Shift Start: ${dateFormat.format(Date(startTime))}\n".toByteArray(Charsets.US_ASCII))
        out.write("Reconciled:  ${dateFormat.format(Date(reconciliation.timestamp))}\n".toByteArray(Charsets.US_ASCII))
        out.write(divider.toByteArray(Charsets.US_ASCII))

        // Physical Cash Denominations
        out.write(CMD_BOLD_ON)
        out.write("CASH DRAWER COUNT BREAKDOWN\n".toByteArray(Charsets.US_ASCII))
        out.write(CMD_BOLD_OFF)
        out.write(padColumns(" 1,000 x ${reconciliation.count1000}:", "$curr${String.format(Locale.US, "%.2f", reconciliation.count1000 * 1000.0)}", totalCols).toByteArray(Charsets.US_ASCII))
        out.write(LF)
        out.write(padColumns("   500 x ${reconciliation.count500}:", "$curr${String.format(Locale.US, "%.2f", reconciliation.count500 * 500.0)}", totalCols).toByteArray(Charsets.US_ASCII))
        out.write(LF)
        out.write(padColumns("   200 x ${reconciliation.count200}:", "$curr${String.format(Locale.US, "%.2f", reconciliation.count200 * 200.0)}", totalCols).toByteArray(Charsets.US_ASCII))
        out.write(LF)
        out.write(padColumns("   100 x ${reconciliation.count100}:", "$curr${String.format(Locale.US, "%.2f", reconciliation.count100 * 100.0)}", totalCols).toByteArray(Charsets.US_ASCII))
        out.write(LF)
        out.write(padColumns("    50 x ${reconciliation.count50}:", "$curr${String.format(Locale.US, "%.2f", reconciliation.count50 * 50.0)}", totalCols).toByteArray(Charsets.US_ASCII))
        out.write(LF)
        out.write(padColumns("    20 x ${reconciliation.count20}:", "$curr${String.format(Locale.US, "%.2f", reconciliation.count20 * 20.0)}", totalCols).toByteArray(Charsets.US_ASCII))
        out.write(LF)
        out.write(padColumns(" Coins & Loose:", "$curr${String.format(Locale.US, "%.2f", reconciliation.countCoins)}", totalCols).toByteArray(Charsets.US_ASCII))
        out.write(LF)
        out.write(divider.toByteArray(Charsets.US_ASCII))

        // Cash Balancing Summary
        if (formulaSummary != null) {
            out.write(CMD_BOLD_ON)
            out.write("DRAWER FORMULA BREAKDOWN\n".toByteArray(Charsets.US_ASCII))
            out.write(CMD_BOLD_OFF)
            out.write(padColumns(" Starting Float:", "$curr${String.format(Locale.US, "%.2f", formulaSummary.startingFloat)}", totalCols).toByteArray(Charsets.US_ASCII))
            out.write(LF)
            out.write(padColumns(" (+) Cash Sales:", "+$curr${String.format(Locale.US, "%.2f", formulaSummary.cashSales)}", totalCols).toByteArray(Charsets.US_ASCII))
            out.write(LF)
            out.write(padColumns(" (+) Cash In:", "+$curr${String.format(Locale.US, "%.2f", formulaSummary.cashIn)}", totalCols).toByteArray(Charsets.US_ASCII))
            out.write(LF)
            out.write(padColumns(" (-) Cash Out:", "-$curr${String.format(Locale.US, "%.2f", formulaSummary.cashOut)}", totalCols).toByteArray(Charsets.US_ASCII))
            out.write(LF)
            out.write(padColumns(" (-) Cash Refunds:", "-$curr${String.format(Locale.US, "%.2f", formulaSummary.cashRefunds)}", totalCols).toByteArray(Charsets.US_ASCII))
            out.write(LF)
            out.write(divider.toByteArray(Charsets.US_ASCII))
        }

        out.write(padColumns("Total Counted Cash:", "$curr${String.format(Locale.US, "%.2f", reconciliation.totalCountedCash)}", totalCols).toByteArray(Charsets.US_ASCII))
        out.write(LF)
        out.write(padColumns("Expected Cash Ledger:", "$curr${String.format(Locale.US, "%.2f", reconciliation.expectedCash)}", totalCols).toByteArray(Charsets.US_ASCII))
        out.write(LF)
        val cashVarStr = if (Math.abs(reconciliation.cashVariance) < 0.01) "BALANCED"
        else if (reconciliation.cashVariance < 0) "SHORT -$curr${String.format(Locale.US, "%.2f", -reconciliation.cashVariance)}"
        else "OVER +$curr${String.format(Locale.US, "%.2f", reconciliation.cashVariance)}"
        out.write(CMD_BOLD_ON)
        out.write(padColumns("Cash Variance:", cashVarStr, totalCols).toByteArray(Charsets.US_ASCII))
        out.write(LF)
        out.write(CMD_BOLD_OFF)
        out.write(divider.toByteArray(Charsets.US_ASCII))

        // GCash Reconciliation
        if (storeConfig.hasGCash) {
            out.write(CMD_BOLD_ON)
            out.write("GCASH DIGITAL WALLET\n".toByteArray(Charsets.US_ASCII))
            out.write(CMD_BOLD_OFF)
            out.write(padColumns(" Counted App Balance:", "$curr${String.format(Locale.US, "%.2f", reconciliation.countedGCash)}", totalCols).toByteArray(Charsets.US_ASCII))
            out.write(LF)
            out.write(padColumns(" POS Ledger Balance:", "$curr${String.format(Locale.US, "%.2f", reconciliation.expectedGCash)}", totalCols).toByteArray(Charsets.US_ASCII))
            out.write(LF)
            val gcashVarStr = if (Math.abs(reconciliation.gcashVariance) < 0.01) "BALANCED"
            else if (reconciliation.gcashVariance < 0) "SHORT -$curr${String.format(Locale.US, "%.2f", -reconciliation.gcashVariance)}"
            else "OVER +$curr${String.format(Locale.US, "%.2f", reconciliation.gcashVariance)}"
            out.write(padColumns(" GCash Variance:", gcashVarStr, totalCols).toByteArray(Charsets.US_ASCII))
            out.write(LF)
            out.write(divider.toByteArray(Charsets.US_ASCII))
        }

        // Prepaid Load Reconciliation
        if (storeConfig.hasLoad) {
            out.write(CMD_BOLD_ON)
            out.write("PREPAID MOBILE LOAD\n".toByteArray(Charsets.US_ASCII))
            out.write(CMD_BOLD_OFF)
            out.write(padColumns(" Smart Counted/Expected:", "${String.format(Locale.US, "%.0f", reconciliation.countedSmartLoad)} / ${String.format(Locale.US, "%.0f", reconciliation.expectedSmartLoad)}", totalCols).toByteArray(Charsets.US_ASCII))
            out.write(LF)
            out.write(padColumns(" Globe Counted/Expected:", "${String.format(Locale.US, "%.0f", reconciliation.countedGlobeLoad)} / ${String.format(Locale.US, "%.0f", reconciliation.expectedGlobeLoad)}", totalCols).toByteArray(Charsets.US_ASCII))
            out.write(LF)
            out.write(divider.toByteArray(Charsets.US_ASCII))
        }

        // Audit & Override
        if (reconciliation.hasDiscrepancy || reconciliation.managerOverrideApproved) {
            out.write(CMD_BOLD_ON)
            out.write("DISCREPANCY OVERRIDE AUDIT\n".toByteArray(Charsets.US_ASCII))
            out.write(CMD_BOLD_OFF)
            out.write("Approved by: ${sanitizeForEscPos(reconciliation.managerUsername, curr)}\n".toByteArray(Charsets.US_ASCII))
            val cleanReason = sanitizeForEscPos(reconciliation.discrepancyReason.take(60), curr)
            out.write("Reason: $cleanReason\n".toByteArray(Charsets.US_ASCII))
            out.write(divider.toByteArray(Charsets.US_ASCII))
        }

        // Barcode
        if (printBarcode && reconciliation.sessionId > 0) {
            val barcodeData = "SES-${reconciliation.sessionId}"
            out.write(buildBarcodeEscPos(barcodeData, isCode128 = true, height = 45, width = if (is80mm) 2 else 2))
            out.write(divider.toByteArray(Charsets.US_ASCII))
        }

        // Signatures
        out.write(LF)
        out.write(padColumns("Cashier Signature", "Manager Signature", totalCols).toByteArray(Charsets.US_ASCII))
        out.write(LF)
        out.write(padColumns("_________________", "_________________", totalCols).toByteArray(Charsets.US_ASCII))
        out.write(LF)

        if (hasAutoCutter) {
            out.write("\n\n".toByteArray(Charsets.US_ASCII))
            out.write(CMD_PAPER_CUT)
        } else {
            out.write(CMD_FEED_LINES)
            out.write("\n\n".toByteArray(Charsets.US_ASCII))
        }
        return out.toByteArray()
    }

    /**
     * Enumerates paired Bluetooth devices and filters/highlights likely thermal POS printers.
     */
    fun getPairedBluetoothPrinters(context: Context): List<DiscoveredBluetoothPrinter> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return emptyList()
            }
        }
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return try {
            adapter.bondedDevices.map { dev ->
                val devName = dev.name ?: "Unknown Bluetooth Device"
                val isLikely = devName.contains("58", ignoreCase = true) ||
                               devName.contains("XP", ignoreCase = true) ||
                               devName.contains("POS", ignoreCase = true) ||
                               devName.contains("Print", ignoreCase = true) ||
                               devName.contains("MTP", ignoreCase = true) ||
                               devName.contains("RPP", ignoreCase = true) ||
                               devName.contains("Inner", ignoreCase = true)
                DiscoveredBluetoothPrinter(devName, dev.address, isLikely)
            }.sortedByDescending { it.isLikelyPosPrinter }
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    /**
     * Sends raw ESC/POS byte payload directly to a Network Thermal Printer (TCP/IP Port 9100).
     */
    suspend fun printOverNetwork(ipAddress: String, port: Int = 9100, data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ipAddress, port), 3500)
                socket.outputStream.use { out ->
                    out.write(data)
                    out.flush()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sends raw ESC/POS byte payload directly to a paired Bluetooth Thermal Printer.
     * Uses multi-tier socket establishment (Secure -> Insecure -> Channel 1 Reflection)
     * and discovery cancellation for seamless XP-58 Plus connection.
     */
    suspend fun printOverBluetooth(macAddress: String, data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        if (macAddress.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Bluetooth MAC address is empty. Please select your printer in Settings."))
        }
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
                ?: return@withContext Result.failure(IllegalStateException("Bluetooth adapter not available on device"))

            if (!adapter.isEnabled) {
                return@withContext Result.failure(IllegalStateException("Bluetooth is disabled. Please turn on Bluetooth."))
            }

            // XP-58 connection requires cancelling discovery if running
            try {
                if (adapter.isDiscovering) {
                    adapter.cancelDiscovery()
                }
            } catch (_: SecurityException) {}

            val device: BluetoothDevice = adapter.getRemoteDevice(macAddress)
            var socket: BluetoothSocket? = null
            var lastEx: Exception? = null

            // 1. Try standard secure RFCOMM SPP socket
            try {
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
            } catch (e: Exception) {
                lastEx = e
                try { socket?.close() } catch (_: Exception) {}
                socket = null
            }

            // 2. Try insecure RFCOMM socket (common requirement on XP-58 / portable printers)
            if (socket == null) {
                try {
                    socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                    socket.connect()
                } catch (e: Exception) {
                    lastEx = e
                    try { socket?.close() } catch (_: Exception) {}
                    socket = null
                }
            }

            // 3. Fallback to RFCOMM Channel 1 via reflection (XP-58 / XP-58IIH / POS-58 standard fix)
            if (socket == null) {
                try {
                    val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    socket = m.invoke(device, 1) as BluetoothSocket
                    socket.connect()
                } catch (fallbackEx: Exception) {
                    lastEx = fallbackEx
                    try { socket?.close() } catch (_: Exception) {}
                    socket = null
                }
            }

            val activeSocket = socket ?: throw (lastEx ?: IOException("Failed to connect to Bluetooth printer ($macAddress)"))

            activeSocket.use { s ->
                s.outputStream.use { out ->
                    out.write(data)
                    out.flush()
                    // Allow printer buffer to complete printing before closing connection
                    Thread.sleep(150)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun dividerLine(totalCols: Int): String {
        return "-".repeat(totalCols) + "\n"
    }

    private fun padColumns(left: String, right: String, totalCols: Int): String {
        val space = totalCols - left.length - right.length
        return if (space > 0) left + " ".repeat(space) + right else "$left $right"
    }

    private fun padColumns(c1: String, c2: String, c3: String, totalCols: Int): String {
        val c2c3 = "$c2  $c3"
        val space = totalCols - c1.length - c2c3.length
        return if (space > 0) c1 + " ".repeat(space) + c2c3 else "$c1 $c2 $c3"
    }

    private fun padColumns(c1: String, c2: String, c3: String, c4: String, totalCols: Int): String {
        val col1Width = totalCols - 24
        val col1 = if (c1.length > col1Width) c1.take(col1Width - 2) + ".." else c1.padEnd(col1Width)
        val col2 = c2.padStart(6)
        val col3 = c3.padStart(8)
        val col4 = c4.padStart(10)
        return "$col1$col2$col3$col4"
    }
}
