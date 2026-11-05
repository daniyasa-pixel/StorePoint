package com.munzo.storepoint.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.munzo.storepoint.data.CashierSession
import com.munzo.storepoint.data.Product
import com.munzo.storepoint.data.StoreConfig
import com.munzo.storepoint.data.StorePointRepository
import com.munzo.storepoint.data.Transaction
import com.munzo.storepoint.data.TransactionItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportUtil {

    private val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun formatDate(date: Date): String = synchronized(df) {
        df.format(date)
    }

    fun shareFile(context: Context, file: File, mimeType: String = "application/pdf") {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Document"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // =========================================================================
    // CSV EXPORT ENGINE
    // =========================================================================

    fun buildSalesSummaryCsv(transactions: List<Transaction>): String {
        val sb = java.lang.StringBuilder()
        sb.append("Transaction ID,Timestamp,Date Time,Cashier,Customer,Payment Method,Subtotal,Tax Amount,Total Amount,Status\n")
        transactions.forEach { t ->
            val dateStr = formatDate(Date(t.timestamp))
            sb.append("#${t.id},${t.timestamp},\"$dateStr\",${t.cashierUsername},\"${t.customerName.replace("\"", "\"\"")}\",${t.paymentMethod},${String.format(Locale.US, "%.2f", t.subtotal)},${String.format(Locale.US, "%.2f", t.taxAmount)},${String.format(Locale.US, "%.2f", t.totalAmount)},${t.status}\n")
        }
        return sb.toString()
    }

    fun exportSalesSummaryCsv(context: Context, storeConfig: StoreConfig, transactions: List<Transaction>) {
        try {
            val csvContent = buildSalesSummaryCsv(transactions)
            val file = File(context.cacheDir, "storepoint_sales_summary.csv")
            file.writeText(csvContent, Charsets.UTF_8)
            shareFile(context, file, "text/csv")
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export Sales CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun buildRegisterJournalCsv(sessions: List<CashierSession>): String {
        val sb = java.lang.StringBuilder()
        sb.append("Session ID,Cashier Username,Start Time,End Time,Starting Cash,Ending Cash,Status\n")
        sessions.forEach { s ->
            val startStr = formatDate(Date(s.startTime))
            val endStr = if (s.endTime != null) formatDate(Date(s.endTime)) else "ACTIVE"
            val endCashStr = if (s.endingCash != null) String.format(Locale.US, "%.2f", s.endingCash) else "N/A"
            sb.append("#${s.id},${s.cashierUsername},\"$startStr\",\"$endStr\",${String.format(Locale.US, "%.2f", s.startingCash)},$endCashStr,${s.status}\n")
        }
        return sb.toString()
    }

    fun exportDailyRegisterJournalCsv(context: Context, storeConfig: StoreConfig, sessions: List<CashierSession>) {
        try {
            val csvContent = buildRegisterJournalCsv(sessions)
            val file = File(context.cacheDir, "storepoint_register_journal.csv")
            file.writeText(csvContent, Charsets.UTF_8)
            shareFile(context, file, "text/csv")
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export Register Journal CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun buildInventoryValuationCsv(products: List<Product>): String {
        val sb = java.lang.StringBuilder()
        sb.append("Product ID,Barcode,Product Name,Stock Count,Base UOM,Cost Price,Retail Price,Total Cost Valuation,Total Retail Valuation,Projected Profit,Track Inventory\n")
        products.forEach { p ->
            val totalCost = StorePointRepository.roundMoney(p.cost * p.stockCount)
            val totalRetail = StorePointRepository.roundMoney(p.price * p.stockCount)
            val potentialProfit = StorePointRepository.roundMoney((p.price - p.cost) * p.stockCount)
            sb.append("#${p.id},\"${p.barcode}\",\"${p.name.replace("\"", "\"\"")}\",${p.stockCount},\"${p.baseUom}\",${String.format(Locale.US, "%.2f", p.cost)},${String.format(Locale.US, "%.2f", p.price)},${String.format(Locale.US, "%.2f", totalCost)},${String.format(Locale.US, "%.2f", totalRetail)},${String.format(Locale.US, "%.2f", potentialProfit)},${p.trackInventory}\n")
        }
        return sb.toString()
    }

    fun exportInventoryValuationCsv(context: Context, storeConfig: StoreConfig, products: List<Product>) {
        try {
            val csvContent = buildInventoryValuationCsv(products)
            val file = File(context.cacheDir, "storepoint_inventory_valuation.csv")
            file.writeText(csvContent, Charsets.UTF_8)
            shareFile(context, file, "text/csv")
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export Inventory Valuation CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // =========================================================================
    // PDF EXPORT ENGINE
    // =========================================================================

    /**
     * Generates a printable POS Receipt as PDF and triggers the sharing interface.
     */
    fun exportReceiptPdf(
        context: Context,
        storeConfig: StoreConfig,
        transaction: Transaction,
        items: List<TransactionItem>
    ) {
        val document = PdfDocument()
        try {
            val width = 300 // Standard thermal printer width in points
            val baseHeight = 220
            val dynamicHeight = baseHeight + (items.size * 25)
            val pageInfo = PdfDocument.PageInfo.Builder(width, dynamicHeight, 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paintText = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            }

            val paintBold = Paint().apply {
                color = Color.BLACK
                textSize = 11f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }

            val paintHeader = Paint().apply {
                color = Color.BLACK
                textSize = 14f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            var y = 25f

            // Draw Header
            canvas.drawText(storeConfig.storeName.uppercase(), (width / 2).toFloat(), y, paintHeader)
            y += 15f
            paintText.textAlign = Paint.Align.CENTER
            canvas.drawText("OFFLINE TRANSACTION RECEIPT", (width / 2).toFloat(), y, paintText)
            y += 15f
            canvas.drawText("Date: ${formatDate(Date(transaction.timestamp))}", (width / 2).toFloat(), y, paintText)
            y += 12f
            canvas.drawText("Cashier: ${transaction.cashierUsername}", (width / 2).toFloat(), y, paintText)
            y += 12f
            if (transaction.customerName.isNotBlank()) {
                canvas.drawText("Customer: ${transaction.customerName}", (width / 2).toFloat(), y, paintText)
                y += 12f
            }
            canvas.drawText("Tx ID: #STP-${transaction.id}", (width / 2).toFloat(), y, paintText)
            y += 15f

            // Divider line
            paintText.textAlign = Paint.Align.LEFT
            canvas.drawLine(15f, y, (width - 15).toFloat(), y, paintBold)
            y += 15f

            // Items header
            canvas.drawText("ITEM", 15f, y, paintBold)
            canvas.drawText("QTY", 180f, y, paintBold)
            canvas.drawText("TOTAL", 240f, y, paintBold)
            y += 10f
            canvas.drawLine(15f, y, (width - 15).toFloat(), y, paintText)
            y += 15f

            // Line Items
            for (item in items) {
                val itemName = if (item.productName.length > 20) item.productName.substring(0, 18) + ".." else item.productName
                canvas.drawText(itemName, 15f, y, paintText)
                canvas.drawText("${item.quantity} ${item.uomName}", 180f, y, paintText)
                val totalItemPrice = item.price * item.quantity
                canvas.drawText(String.format(Locale.US, "%.2f", totalItemPrice), 240f, y, paintText)
                y += 15f
            }

            y += 5f
            canvas.drawLine(15f, y, (width - 15).toFloat(), y, paintText)
            y += 18f

            // Summary Totals
            canvas.drawText("Subtotal:", 140f, y, paintText)
            canvas.drawText(String.format(Locale.US, "%.2f", transaction.subtotal), 240f, y, paintText)
            y += 15f

            if (transaction.taxAmount > 0) {
                canvas.drawText("Tax (VAT):", 140f, y, paintText)
                canvas.drawText(String.format(Locale.US, "%.2f", transaction.taxAmount), 240f, y, paintText)
                y += 15f
            }

            canvas.drawText("TOTAL:", 140f, y, paintBold)
            canvas.drawText("${storeConfig.currencySymbol}${String.format(Locale.US, "%.2f", transaction.totalAmount)}", 230f, y, paintBold)
            y += 18f

            canvas.drawText("Paid (${transaction.paymentMethod}):", 140f, y, paintText)
            canvas.drawText(String.format(Locale.US, "%.2f", transaction.cashPaid), 240f, y, paintText)
            y += 15f

            canvas.drawText("Change:", 140f, y, paintText)
            canvas.drawText(String.format(Locale.US, "%.2f", transaction.changeAmount), 240f, y, paintText)
            y += 25f

            // Footer
            paintText.textAlign = Paint.Align.CENTER
            canvas.drawText("Thank you for your purchase!", (width / 2).toFloat(), y, paintText)
            y += 12f
            canvas.drawText("Powered by StorePoint POS", (width / 2).toFloat(), y, paintText)

            document.finishPage(page)

            val file = File(context.cacheDir, "receipt_STP_${transaction.id}.pdf")
            FileOutputStream(file).use { fos ->
                document.writeTo(fos)
            }
            shareFile(context, file)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to generate Receipt PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } finally {
            try {
                document.close()
            } catch (e: Exception) {
                android.util.Log.e("PdfExportUtil", "Error closing PdfDocument", e)
            }
        }
    }

    /**
     * Exports a comprehensive Sales Analytics Report for store management.
     */
    fun exportSalesAndInventoryReportPdf(
        context: Context,
        storeConfig: StoreConfig,
        transactions: List<Transaction>,
        productInventory: List<Product>
    ) {
        val document = PdfDocument()
        try {
            val width = 595 // Standard A4 page width in points
            val height = 842 // Standard A4 page height in points
            val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paintTitle = Paint().apply {
                color = Color.rgb(15, 23, 42) // Dark midnight blue
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val paintSubtitle = Paint().apply {
                color = Color.GRAY
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            val paintHeading = Paint().apply {
                color = Color.rgb(51, 65, 85)
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val paintText = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            val paintBold = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            var y = 50f

            // Title Header
            canvas.drawText("StorePoint POS Management System", 40f, y, paintTitle)
            y += 20f
            canvas.drawText("Executive Sales Performance & Operational Audit - ${storeConfig.storeName}", 40f, y, paintSubtitle)
            y += 12f
            canvas.drawText("Generated at: ${formatDate(Date())} | Currency: ${storeConfig.currencySymbol}", 40f, y, paintSubtitle)
            y += 30f

            // Section: Financial Performance
            canvas.drawText("1. EXECUTIVE FINANCIAL PERFORMANCE", 40f, y, paintHeading)
            y += 20f

            val totalTransactionsCount = transactions.size
            val totalRevenue = transactions.sumOf { it.totalAmount }
            val taxCollected = transactions.sumOf { it.taxAmount }
            val cashRevenue = transactions.filter { it.paymentMethod == "CASH" }.sumOf { it.totalAmount }
            val gcashRevenue = transactions.filter { it.paymentMethod.contains("GCASH", ignoreCase = true) }.sumOf { it.totalAmount }
            val mayaRevenue = transactions.filter { it.paymentMethod.contains("MAYA", ignoreCase = true) }.sumOf { it.totalAmount }
            val avgBasket = if (totalTransactionsCount > 0) totalRevenue / totalTransactionsCount else 0.0

            canvas.drawText("Total Transactions Processed:", 50f, y, paintText)
            canvas.drawText("$totalTransactionsCount transactions", 260f, y, paintBold)
            y += 16f

            canvas.drawText("Daily Gross Sales Revenue:", 50f, y, paintText)
            canvas.drawText("${storeConfig.currencySymbol}${String.format(Locale.US, "%.2f", totalRevenue)}", 260f, y, paintBold)
            y += 16f

            canvas.drawText("Average Basket Ticket Size:", 50f, y, paintText)
            canvas.drawText("${storeConfig.currencySymbol}${String.format(Locale.US, "%.2f", avgBasket)}", 260f, y, paintBold)
            y += 16f

            canvas.drawText("Cash Settlements:", 50f, y, paintText)
            canvas.drawText("${storeConfig.currencySymbol}${String.format(Locale.US, "%.2f", cashRevenue)}", 260f, y, paintBold)
            y += 16f

            canvas.drawText("GCash Settlements:", 50f, y, paintText)
            canvas.drawText("${storeConfig.currencySymbol}${String.format(Locale.US, "%.2f", gcashRevenue)}", 260f, y, paintBold)
            y += 16f

            canvas.drawText("Maya / Card Settlements:", 50f, y, paintText)
            canvas.drawText("${storeConfig.currencySymbol}${String.format(Locale.US, "%.2f", mayaRevenue)}", 260f, y, paintBold)
            y += 16f

            canvas.drawText("Tax (VAT) Collected:", 50f, y, paintText)
            canvas.drawText("${storeConfig.currencySymbol}${String.format(Locale.US, "%.2f", taxCollected)}", 260f, y, paintBold)
            y += 30f

            // Section: Stock Audit
            canvas.drawText("2. STOCK AUDIT & PRODUCT AVAILABILITY", 40f, y, paintHeading)
            y += 20f

            val totalProductsInStore = productInventory.size
            val totalStocksOnHand = productInventory.sumOf { it.stockCount }
            val outOfStockItems = productInventory.filter { it.stockCount == 0 }
            val lowStockItems = productInventory.filter { it.stockCount in 1..5 }

            canvas.drawText("Total registered Products catalog:", 50f, y, paintText)
            canvas.drawText("$totalProductsInStore unique items", 260f, y, paintBold)
            y += 16f

            canvas.drawText("Total cumulative Stock pieces:", 50f, y, paintText)
            canvas.drawText("$totalStocksOnHand units on hand", 260f, y, paintBold)
            y += 16f

            canvas.drawText("Out Of Stock Items (0 units):", 50f, y, paintText)
            canvas.drawText("${outOfStockItems.size} products critical warning", 260f, y, if (outOfStockItems.isNotEmpty()) paintBold.apply { color = Color.RED } else paintBold)
            paintBold.apply { color = Color.BLACK }
            y += 16f

            canvas.drawText("Low Stock Warning (1-5 units):", 50f, y, paintText)
            canvas.drawText("${lowStockItems.size} products flagged", 260f, y, paintBold)
            y += 30f

            // Table Header
            canvas.drawText("3. DETAILED PRODUCT CATALOG STATUS", 40f, y, paintHeading)
            y += 20f

            canvas.drawText("Product SKU / ID", 45f, y, paintBold)
            canvas.drawText("Product Label", 150f, y, paintBold)
            canvas.drawText("Price", 350f, y, paintBold)
            canvas.drawText("Stock", 440f, y, paintBold)
            canvas.drawText("Barcode", 510f, y, paintBold)
            y += 8f
            canvas.drawLine(40f, y, 555f, y, paintText)
            y += 15f

            val listToDraw = productInventory.take(15)
            for (product in listToDraw) {
                val priceStr = "${storeConfig.currencySymbol}${String.format(Locale.US, "%.2f", product.price)}"
                val stockStr = "${product.stockCount} ${product.baseUom}"

                canvas.drawText("#${product.id}", 45f, y, paintText)
                val pName = if (product.name.length > 25) product.name.substring(0, 23) + ".." else product.name
                canvas.drawText(pName, 150f, y, paintText)
                canvas.drawText(priceStr, 350f, y, paintText)
                
                if (product.stockCount == 0) {
                    paintBold.color = Color.RED
                    canvas.drawText("OUT OF STOCK", 440f, y, paintBold)
                    paintBold.color = Color.BLACK
                } else {
                    canvas.drawText(stockStr, 440f, y, paintText)
                }
                
                canvas.drawText(product.barcode.ifEmpty { "N/A" }, 510f, y, paintText)
                y += 18f
            }

            if (productInventory.size > 15) {
                canvas.drawText("(and ${productInventory.size - 15} more catalog entries in database)", 45f, y, paintSubtitle)
            }

            // Footer
            y = 800f
            canvas.drawLine(40f, y, 555f, y, paintText)
            y += 15f
            paintSubtitle.textAlign = Paint.Align.CENTER
            canvas.drawText("StorePoint POS Management System - Confidential Store Report", (width / 2).toFloat(), y, paintSubtitle)

            document.finishPage(page)

            val file = File(context.cacheDir, "storepoint_sales_report.pdf")
            FileOutputStream(file).use { fos ->
                document.writeTo(fos)
            }
            shareFile(context, file)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to generate Sales Report PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } finally {
            try {
                document.close()
            } catch (e: Exception) {
                android.util.Log.e("PdfExportUtil", "Error closing PdfDocument", e)
            }
        }
    }

    /**
     * Exports Daily Register Journal Audit Report to PDF.
     */
    fun exportDailyRegisterJournalPdf(
        context: Context,
        storeConfig: StoreConfig,
        sessions: List<CashierSession>
    ) {
        val document = PdfDocument()
        try {
            val width = 595
            val height = 842
            val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paintTitle = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val paintSubtitle = Paint().apply {
                color = Color.GRAY
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val paintHeading = Paint().apply {
                color = Color.rgb(51, 65, 85)
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val paintText = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val paintBold = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            var y = 50f
            canvas.drawText("${storeConfig.storeName.uppercase()} - DAILY REGISTER JOURNAL AUDIT", 40f, y, paintTitle)
            y += 18f
            canvas.drawText("Generated at: ${formatDate(Date())} | Currency: ${storeConfig.currencySymbol}", 40f, y, paintSubtitle)
            y += 28f

            canvas.drawText("1. REGISTER SESSION SUMMARY", 40f, y, paintHeading)
            y += 18f

            val totalSessions = sessions.size
            val activeSessions = sessions.count { it.status == "ACTIVE" }
            val closedSessions = sessions.count { it.status != "ACTIVE" }
            val totalStartFloat = sessions.sumOf { it.startingCash }
            val totalEndCash = sessions.sumOf { it.endingCash ?: 0.0 }

            canvas.drawText("Total Cashier Sessions:", 50f, y, paintText)
            canvas.drawText("$totalSessions ($closedSessions closed, $activeSessions active)", 250f, y, paintBold)
            y += 16f

            canvas.drawText("Total Starting Floats Allocated:", 50f, y, paintText)
            canvas.drawText("${storeConfig.currencySymbol}${String.format(Locale.US, "%.2f", totalStartFloat)}", 250f, y, paintBold)
            y += 16f

            canvas.drawText("Total Ending Drawer Cash Counted:", 50f, y, paintText)
            canvas.drawText("${storeConfig.currencySymbol}${String.format(Locale.US, "%.2f", totalEndCash)}", 250f, y, paintBold)
            y += 28f

            canvas.drawText("2. CASHIER SHIFT AUDIT TRAIL", 40f, y, paintHeading)
            y += 18f

            canvas.drawText("Session", 45f, y, paintBold)
            canvas.drawText("Cashier", 110f, y, paintBold)
            canvas.drawText("Start Float", 210f, y, paintBold)
            canvas.drawText("End Cash", 310f, y, paintBold)
            canvas.drawText("Time Window", 410f, y, paintBold)
            canvas.drawText("Status", 510f, y, paintBold)
            y += 8f
            canvas.drawLine(40f, y, 555f, y, paintText)
            y += 16f

            val displaySessions = sessions.take(18)
            for (s in displaySessions) {
                val startStr = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(s.startTime))
                val endStr = if (s.endTime != null) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(s.endTime)) else "Active"

                canvas.drawText("#${s.id}", 45f, y, paintText)
                canvas.drawText(s.cashierUsername, 110f, y, paintText)
                canvas.drawText("${storeConfig.currencySymbol}${String.format(Locale.US, "%.2f", s.startingCash)}", 210f, y, paintText)
                canvas.drawText(if (s.endingCash != null) "${storeConfig.currencySymbol}${String.format(Locale.US, "%.2f", s.endingCash)}" else "Pending", 310f, y, paintText)
                canvas.drawText("$startStr-$endStr", 410f, y, paintText)
                canvas.drawText(s.status, 510f, y, paintBold)
                y += 18f
            }

            y = 800f
            canvas.drawLine(40f, y, 555f, y, paintText)
            y += 15f
            paintSubtitle.textAlign = Paint.Align.CENTER
            canvas.drawText("Daily Register Journal Audit - StorePoint POS", (width / 2).toFloat(), y, paintSubtitle)

            document.finishPage(page)

            val file = File(context.cacheDir, "storepoint_register_journal.pdf")
            FileOutputStream(file).use { fos ->
                document.writeTo(fos)
            }
            shareFile(context, file)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to generate Register Journal PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } finally {
            try {
                document.close()
            } catch (e: Exception) {
                android.util.Log.e("PdfExportUtil", "Error closing PdfDocument", e)
            }
        }
    }

    /**
     * Exports Inventory Valuation Audit Report to PDF.
     */
    fun exportInventoryValuationPdf(
        context: Context,
        storeConfig: StoreConfig,
        products: List<Product>
    ) {
        val document = PdfDocument()
        try {
            val width = 595
            val height = 842
            val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paintTitle = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val paintSubtitle = Paint().apply {
                color = Color.GRAY
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val paintHeading = Paint().apply {
                color = Color.rgb(51, 65, 85)
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val paintText = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val paintBold = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            var y = 50f
            canvas.drawText("${storeConfig.storeName.uppercase()} - INVENTORY VALUATION REPORT", 40f, y, paintTitle)
            y += 18f
            canvas.drawText("Generated at: ${formatDate(Date())} | Valuation Basis: WAC / COGS", 40f, y, paintSubtitle)
            y += 28f

            canvas.drawText("1. INVENTORY ASSET VALUATION SUMMARY", 40f, y, paintHeading)
            y += 18f

            val totalProducts = products.size
            val totalUnits = products.sumOf { it.stockCount }
            val totalCostValuation = StorePointRepository.roundMoney(products.sumOf { it.cost * it.stockCount })
            val totalRetailValuation = StorePointRepository.roundMoney(products.sumOf { it.price * it.stockCount })
            val potentialGrossProfit = StorePointRepository.roundMoney(totalRetailValuation - totalCostValuation)
            val projectedMarginPct = if (totalRetailValuation > 0) (potentialGrossProfit / totalRetailValuation) * 100.0 else 0.0

            canvas.drawText("Total Catalog SKUs:", 50f, y, paintText)
            canvas.drawText("$totalProducts items ($totalUnits total units on hand)", 260f, y, paintBold)
            y += 16f

            canvas.drawText("Total Cost Valuation (COGS Asset):", 50f, y, paintText)
            canvas.drawText("${storeConfig.currencySymbol}${String.format(Locale.US, "%.2f", totalCostValuation)}", 260f, y, paintBold)
            y += 16f

            canvas.drawText("Total Retail Valuation (Sales Potential):", 50f, y, paintText)
            canvas.drawText("${storeConfig.currencySymbol}${String.format(Locale.US, "%.2f", totalRetailValuation)}", 260f, y, paintBold)
            y += 16f

            canvas.drawText("Projected Gross Profit / Margin:", 50f, y, paintText)
            canvas.drawText("${storeConfig.currencySymbol}${String.format(Locale.US, "%.2f", potentialGrossProfit)} (${String.format(Locale.US, "%.1f", projectedMarginPct)}%)", 260f, y, paintBold)
            y += 28f

            canvas.drawText("2. DETAILED INVENTORY VALUATION REGISTER", 40f, y, paintHeading)
            y += 18f

            canvas.drawText("SKU", 45f, y, paintBold)
            canvas.drawText("Product Label", 110f, y, paintBold)
            canvas.drawText("Stock", 260f, y, paintBold)
            canvas.drawText("Unit Cost", 330f, y, paintBold)
            canvas.drawText("Retail", 410f, y, paintBold)
            canvas.drawText("Total Asset Value", 480f, y, paintBold)
            y += 8f
            canvas.drawLine(40f, y, 555f, y, paintText)
            y += 16f

            val displayList = products.take(18)
            for (p in displayList) {
                val assetVal = StorePointRepository.roundMoney(p.cost * p.stockCount)
                val pName = if (p.name.length > 22) p.name.substring(0, 20) + ".." else p.name

                canvas.drawText("#${p.id}", 45f, y, paintText)
                canvas.drawText(pName, 110f, y, paintText)
                canvas.drawText("${p.stockCount} ${p.baseUom}", 260f, y, paintText)
                canvas.drawText("${storeConfig.currencySymbol}${String.format(Locale.US, "%.2f", p.cost)}", 330f, y, paintText)
                canvas.drawText("${storeConfig.currencySymbol}${String.format(Locale.US, "%.2f", p.price)}", 410f, y, paintText)
                canvas.drawText("${storeConfig.currencySymbol}${String.format(Locale.US, "%.2f", assetVal)}", 480f, y, paintBold)
                y += 18f
            }

            if (products.size > 18) {
                canvas.drawText("(and ${products.size - 18} more items in full database)", 45f, y, paintSubtitle)
            }

            y = 800f
            canvas.drawLine(40f, y, 555f, y, paintText)
            y += 15f
            paintSubtitle.textAlign = Paint.Align.CENTER
            canvas.drawText("Inventory Valuation Audit Report - StorePoint POS", (width / 2).toFloat(), y, paintSubtitle)

            document.finishPage(page)

            val file = File(context.cacheDir, "storepoint_inventory_valuation.pdf")
            FileOutputStream(file).use { fos ->
                document.writeTo(fos)
            }
            shareFile(context, file)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to generate Inventory Valuation PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } finally {
            try {
                document.close()
            } catch (e: Exception) {
                android.util.Log.e("PdfExportUtil", "Error closing PdfDocument", e)
            }
        }
    }

    /**
     * Draws a very simple barcode-like series of lines representing the barcodeId string.
     */
    fun drawSimpleBarcode(canvas: Canvas, text: String, xStart: Float, yStart: Float, width: Float, height: Float) {
        val paintBar = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        
        val cleaned = text.uppercase().filter { it.isLetterOrDigit() }.ifEmpty { "CASHIER" }
        val binaryStringBuilder = java.lang.StringBuilder()
        binaryStringBuilder.append("10110110") // Start guard
        for (char in cleaned) {
            val code = char.code
            for (i in 0..6) {
                if (((code ushr i) and 1) == 1) {
                    binaryStringBuilder.append("11")
                } else {
                    binaryStringBuilder.append("1")
                }
                binaryStringBuilder.append("0")
            }
        }
        binaryStringBuilder.append("10110110") // End guard
        
        val binaryStr = binaryStringBuilder.toString()
        val totalBars = binaryStr.length
        val barWidth = width / totalBars
        
        for (i in binaryStr.indices) {
            if (binaryStr[i] == '1') {
                val xCurrent = xStart + i * barWidth
                canvas.drawRect(xCurrent, yStart, xCurrent + barWidth, yStart + height, paintBar)
            }
        }
    }

    /**
     * Generates a printable Staff ID Badge PDF with custom pseudo-rendered 1D scan code
     */
    fun exportCashierBadgePdf(
        context: Context,
        storeConfig: StoreConfig,
        username: String,
        role: String,
        barcodeId: String
    ) {
        val document = PdfDocument()
        try {
            val width = 250 // Custom card width in points
            val height = 350 // Custom card height in points
            val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paintBorder = Paint().apply {
                color = Color.rgb(30, 41, 59)
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }

            val paintPrimaryBg = Paint().apply {
                color = Color.rgb(30, 41, 59)
                style = Paint.Style.FILL
            }

            val paintRoleBadge = Paint().apply {
                color = if (role.equals("ADMIN", ignoreCase = true)) Color.rgb(220, 38, 38) else Color.rgb(37, 99, 235)
                style = Paint.Style.FILL
            }

            val paintTextLight = Paint().apply {
                color = Color.WHITE
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            val paintHeader = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            val paintLabel = Paint().apply {
                color = Color.GRAY
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
            }

            val paintTextDark = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            val centerX = (width / 2).toFloat()

            // Outer Frame
            val rectFrame = android.graphics.RectF(10f, 10f, (width - 10).toFloat(), (height - 10).toFloat())
            canvas.drawRoundRect(rectFrame, 16f, 16f, paintBorder)

            var y = 35f
            canvas.drawText(storeConfig.storeName.uppercase(), centerX, y, paintHeader)
            y += 14f
            canvas.drawText("OFFICIAL EMPLOYEE ID", centerX, y, paintLabel)
            y += 20f

            // Avatar placeholder box
            val avatarSize = 65f
            val avatarX = (width - avatarSize) / 2f
            val avatarPaint = Paint().apply {
                color = Color.rgb(241, 245, 249)
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(android.graphics.RectF(avatarX, y, avatarX + avatarSize, y + avatarSize), 10f, 10f, avatarPaint)
            
            val iconPaint = Paint().apply {
                color = Color.rgb(148, 163, 184)
                style = Paint.Style.FILL
            }
            canvas.drawCircle(centerX, y + 25f, 12f, iconPaint)
            canvas.drawArc(android.graphics.RectF(centerX - 20f, y + 40f, centerX + 20f, y + 70f), 180f, 180f, true, iconPaint)
            y += avatarSize + 18f

            // Username
            canvas.drawRect(25f, y, (width - 25).toFloat(), y + 35f, paintPrimaryBg)
            paintTextLight.textSize = 14f
            paintTextLight.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(username.uppercase(), centerX, y + 22f, paintTextLight)
            y += 45f

            // Role
            canvas.drawRoundRect(50f, y, (width - 50).toFloat(), y + 20f, 6f, 6f, paintRoleBadge)
            paintTextLight.textSize = 9f
            paintTextLight.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(role.uppercase(), centerX, y + 13f, paintTextLight)
            y += 35f

            // Barcode
            val targetBarcodeCode = barcodeId.ifEmpty { "CASHIER-${username.hashCode().coerceAtLeast(0) % 9999}" }
            val barcodeWidth = 180f
            val barcodeHeight = 40f
            val barcodeX = (width - barcodeWidth) / 2f
            drawSimpleBarcode(canvas, targetBarcodeCode, barcodeX, y, barcodeWidth, barcodeHeight)
            y += 52f

            canvas.drawText("* $targetBarcodeCode *", centerX, y, paintTextDark)
            y += 15f
            canvas.drawText("Secure scanning badge - StorePoint System", centerX, y, paintLabel)

            document.finishPage(page)

            val cleanName = username.replace("[^a-zA-Z0-9]".toRegex(), "_")
            val file = File(context.cacheDir, "cashier_badge_${cleanName}.pdf")
            FileOutputStream(file).use { fos ->
                document.writeTo(fos)
            }
            shareFile(context, file)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to write Badge PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } finally {
            try {
                document.close()
            } catch (e: Exception) {
                android.util.Log.e("PdfExportUtil", "Error closing PdfDocument", e)
            }
        }
    }
}
