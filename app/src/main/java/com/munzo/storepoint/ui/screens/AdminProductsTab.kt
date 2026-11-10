package com.munzo.storepoint.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.munzo.storepoint.data.*
import com.munzo.storepoint.ui.BarcodeScannerDialog
import com.munzo.storepoint.ui.StorePointViewModel
import com.munzo.storepoint.ui.components.*
import com.munzo.storepoint.ui.theme.*
import java.util.Date
import java.util.Locale

// Products Manager View
@Composable
fun ProductsTab(
    products: List<Product>,
    categories: List<Category>,
    curr: String,
    onEdit: (Product) -> Unit,
    onDelete: (Int) -> Unit,
    onAddClick: () -> Unit,
    onRestockClick: (Product) -> Unit,
    onVariantsClick: (Product) -> Unit = {},
    viewModel: StorePointViewModel
) {
    val context = LocalContext.current
    var isStockCheckMode by remember { mutableStateOf(false) }
    val auditedItems = remember { mutableStateMapOf<Int, Int>() }
    var showScannerDialog by remember { mutableStateOf(false) }
    var manualAuditInput by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // TOP CONTROLS & TOGGLE BLOCK
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompact = maxWidth < 560.dp
            if (isCompact) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (isStockCheckMode) "Inventory Audit & Stocktake" else "Store Inventory Register",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isStockCheckMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isStockCheckMode) "Verify matching digital registers with physical stocks safely" else "Manage items list and products database",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HeroButton(
                            onClick = {
                                isStockCheckMode = !isStockCheckMode
                                if (!isStockCheckMode) {
                                    auditedItems.clear()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("toggle_stock_check_btn"),
                            text = if (isStockCheckMode) "Exit Audit" else "Stock Audit",
                            icon = if (isStockCheckMode) Icons.Default.Close else Icons.Default.Inventory,
                            color = if (isStockCheckMode) HeroColor.Danger else HeroColor.Default,
                            variant = if (isStockCheckMode) HeroButtonVariant.Solid else HeroButtonVariant.Flat,
                            size = HeroSize.Md
                        )

                        if (!isStockCheckMode) {
                            FilledTonalIconButton(
                                onClick = { viewModel.shareInventoryViaQuickShare(context) },
                                modifier = Modifier.size(48.dp).clip(HeroUITheme.radius.md).testTag("admin_catalog_quick_share_btn_compact")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Quick Share")
                            }
                            HeroButton(
                                onClick = onAddClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("add_product_button"),
                                text = "New Product",
                                icon = Icons.Default.Add,
                                color = HeroColor.Primary,
                                variant = HeroButtonVariant.Solid,
                                size = HeroSize.Md
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isStockCheckMode) "Inventory Audit & Stocktake" else "Store Inventory Register",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isStockCheckMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isStockCheckMode) "Verify matching digital registers with physical stocks safely" else "Manage items list and products database",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        HeroButton(
                            onClick = {
                                isStockCheckMode = !isStockCheckMode
                                if (!isStockCheckMode) {
                                    auditedItems.clear()
                                }
                            },
                            modifier = Modifier.testTag("toggle_stock_check_btn"),
                            text = if (isStockCheckMode) "Exit Audit" else "Stock Check Mode",
                            icon = if (isStockCheckMode) Icons.Default.Close else Icons.Default.Inventory,
                            color = if (isStockCheckMode) HeroColor.Danger else HeroColor.Default,
                            variant = if (isStockCheckMode) HeroButtonVariant.Solid else HeroButtonVariant.Flat,
                            size = HeroSize.Md
                        )

                        if (!isStockCheckMode) {
                            HeroFilledTonalButton(
                                onClick = { viewModel.shareInventoryViaQuickShare(context) },
                                modifier = Modifier.testTag("admin_catalog_quick_share_btn_wide")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Quick Share", style = MaterialTheme.typography.labelMedium)
                            }
                            HeroButton(
                                onClick = onAddClick,
                                modifier = Modifier.testTag("add_product_button"),
                                text = "New Product",
                                icon = Icons.Default.Add,
                                color = HeroColor.Primary,
                                variant = HeroButtonVariant.Solid,
                                size = HeroSize.Md
                            )
                        }
                    }
                }
            }
        }

        // DYNAMIC VIEW CHANGER
        if (isStockCheckMode) {
            // STOCKCHECK INTEGRATED COCKPIT
            HeroCard(
                modifier = Modifier.fillMaxWidth().glassPanel(cornerRadius = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title and Description
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isCompactConsole = maxWidth < 540.dp
                        if (isCompactConsole) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Inventory, null, tint = MaterialTheme.colorScheme.primary)
                                    Text("Local Audit Console", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                                if (auditedItems.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        HeroFilledTonalButton(
                                            onClick = {
                                                val report = StringBuilder()
                                                report.append("--- STOREPOINT STOCKTAKE AUDIT ---\n")
                                                report.append("Date Conducted: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}\n\n")
                                                
                                                var totalChecked = 0
                                                var discrepancies = 0
                                                
                                                auditedItems.forEach { (id, checkedCount) ->
                                                    val p = products.find { it.id == id }
                                                    if (p != null) {
                                                        totalChecked++
                                                        val variance = checkedCount - p.stockCount
                                                        if (variance != 0) {
                                                            discrepancies++
                                                            report.append("• ${p.name}\n")
                                                            report.append("  System: ${p.stockCount} | Scanned: $checkedCount\n")
                                                            report.append("  Variance: ${if (variance > 0) "+$variance" else "$variance"}\n\n")
                                                        }
                                                    }
                                                }
                                                
                                                report.append("Summary metrics:\n")
                                                report.append("- Items Checked: $totalChecked\n")
                                                report.append("- Discrepancy Alerts: $discrepancies\n")
                                                report.append("-----------------------------\n")

                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_SUBJECT, "Inventory Audit Log")
                                                    putExtra(Intent.EXTRA_TEXT, report.toString())
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Share Inventory Audit Report"))
                                            },
                                            modifier = Modifier.weight(1f).testTag("audit_share_report_btn"),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Share, null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Share Report", style = MaterialTheme.typography.labelSmall)
                                        }

                                        HeroButton(
                                            onClick = {
                                                var syncedCount = 0
                                                auditedItems.forEach { (id, checkedCount) ->
                                                    val p = products.find { it.id == id }
                                                    if (p != null && p.stockCount != checkedCount) {
                                                        viewModel.saveProduct(p.copy(stockCount = checkedCount))
                                                        syncedCount++
                                                    }
                                                }
                                                if (syncedCount > 0) {
                                                    Toast.makeText(context, "Bulk corrected $syncedCount discrepancy records in database!", Toast.LENGTH_LONG).show()
                                                } else {
                                                    Toast.makeText(context, "All audited items are already synchronized!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.weight(1f).testTag("audit_sync_all_btn"),
                                            text = "Correct All",
                                            icon = Icons.Default.Sync,
                                            color = HeroColor.Success,
                                            variant = HeroButtonVariant.Solid,
                                            size = HeroSize.Sm
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Inventory, null, tint = MaterialTheme.colorScheme.primary)
                                    Text("Local Audit Console", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                                
                                // Bulk Actions Bar
                                if (auditedItems.isNotEmpty()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        HeroFilledTonalButton(
                                            onClick = {
                                                val report = StringBuilder()
                                                report.append("--- STOREPOINT STOCKTAKE AUDIT ---\n")
                                                report.append("Date Conducted: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}\n\n")
                                                
                                                var totalChecked = 0
                                                var discrepancies = 0
                                                
                                                auditedItems.forEach { (id, checkedCount) ->
                                                    val p = products.find { it.id == id }
                                                    if (p != null) {
                                                        totalChecked++
                                                        val variance = checkedCount - p.stockCount
                                                        if (variance != 0) {
                                                            discrepancies++
                                                            report.append("• ${p.name}\n")
                                                            report.append("  System: ${p.stockCount} | Scanned: $checkedCount\n")
                                                            report.append("  Variance: ${if (variance > 0) "+$variance" else "$variance"}\n\n")
                                                        }
                                                    }
                                                }
                                                
                                                report.append("Summary metrics:\n")
                                                report.append("- Items Checked: $totalChecked\n")
                                                report.append("- Discrepancy Alerts: $discrepancies\n")
                                                report.append("-----------------------------\n")

                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_SUBJECT, "Inventory Audit Log")
                                                    putExtra(Intent.EXTRA_TEXT, report.toString())
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Share Inventory Audit Report"))
                                            },
                                            modifier = Modifier.testTag("audit_share_report_btn"),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Share, null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Share Report", style = MaterialTheme.typography.labelSmall)
                                        }

                                        ExpressiveButton(
                                            onClick = {
                                                var syncedCount = 0
                                                auditedItems.forEach { (id, checkedCount) ->
                                                    val p = products.find { it.id == id }
                                                    if (p != null && p.stockCount != checkedCount) {
                                                        viewModel.saveProduct(p.copy(stockCount = checkedCount))
                                                        syncedCount++
                                                    }
                                                }
                                                if (syncedCount > 0) {
                                                    Toast.makeText(context, "Bulk corrected $syncedCount discrepancy records in database!", Toast.LENGTH_LONG).show()
                                                } else {
                                                    Toast.makeText(context, "All audited items are already synchronized!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.testTag("audit_sync_all_btn"),
                                            label = "Sync All",
                                            icon = Icons.Default.Sync,
                                            variant = ExpressiveButtonVariant.FILLED,
                                            size = ExpressiveButtonSize.XS,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SCANNER ENGINE TRIGGERS ROW
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            val matchedSuggestions = remember(manualAuditInput, products) {
                                if (manualAuditInput.isBlank()) emptyList()
                                else products.filter { p ->
                                    p.name.contains(manualAuditInput, ignoreCase = true) ||
                                    p.barcode.contains(manualAuditInput, ignoreCase = true)
                                }.take(5)
                            }

                            HeroInput(
                                value = manualAuditInput,
                                onValueChange = {
                                    manualAuditInput = it
                                    isDropdownExpanded = true
                                },
                                label = { Text("Search Item / Barcode") },
                                placeholder = { Text("Enter SKU, barcode, or item name") },
                                modifier = Modifier.fillMaxWidth().testTag("audit_barcode_field"),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { showScannerDialog = true }) {
                                        Icon(Icons.Default.QrCodeScanner, "Launch Audit Scanner", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )

                            if (isDropdownExpanded && matchedSuggestions.isNotEmpty()) {
                                DropdownMenu(
                                    expanded = isDropdownExpanded,
                                    onDismissRequest = { isDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    matchedSuggestions.forEach { prod ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(prod.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                    Text("Barcode: ${prod.barcode.ifEmpty { "None" }} • Stock: ${prod.stockCount}", style = MaterialTheme.typography.labelSmall)
                                                }
                                            },
                                            onClick = {
                                                val current = auditedItems[prod.id] ?: 0
                                                auditedItems[prod.id] = current + 1
                                                viewModel.playBeep()
                                                manualAuditInput = ""
                                                isDropdownExpanded = false
                                                Toast.makeText(context, "${prod.name} registered (+1)", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Add manually button
                        IconButton(
                            onClick = {
                                if (manualAuditInput.isNotBlank()) {
                                    val match = products.find { it.barcode.equals(manualAuditInput.trim(), ignoreCase = true) || it.name.contains(manualAuditInput, ignoreCase = true) }
                                    if (match != null) {
                                        val current = auditedItems[match.id] ?: 0
                                        auditedItems[match.id] = current + 1
                                        viewModel.playBeep()
                                        manualAuditInput = ""
                                        Toast.makeText(context, "Added ${match.name} to audit sheet (+1)", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "No matching product SKU or name found.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
                        ) {
                            Icon(Icons.Default.Add, "Search & Add to Worksheet")
                        }
                    }

                    // SUMMARY COMPACT COUNTER CHIPS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val totalCheckedSize = auditedItems.size
                        val perfectMatchSize = auditedItems.count { (id, checkedQty) ->
                            products.find { it.id == id }?.stockCount == checkedQty
                        }
                        val discrepancySize = totalCheckedSize - perfectMatchSize

                        // Stats Card 1: Audited
                        HeroCard(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Audited Items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text("$totalCheckedSize / ${products.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Stats Card 2: Perfect Matches
                        HeroCard(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("In Sync", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text("$perfectMatchSize", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        // Stats Card 3: Discrepancies
                        HeroCard(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Discrepancies", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text("$discrepancySize", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (discrepancySize > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            // WORKSHEET VIEW
            Text(
                text = "Audit Worksheet Progress (${auditedItems.size} items monitored)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (auditedItems.isEmpty()) {
                    item {
                        HeroCard(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("Empty Audit Worksheet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Scan a product's barcode with the camera or search using the text-field above to begin matching physical inventory stocks.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                } else {
                    items(auditedItems.keys.toList()) { productId ->
                        val item = products.find { it.id == productId }
                        val physicalQty = auditedItems[productId] ?: 0
                        
                        if (item != null) {
                            val systemQty = item.stockCount
                            val variance = physicalQty - systemQty
                            val isMatch = variance == 0

                            HeroCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassPanel(cornerRadius = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMatch) Color.Transparent else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("System Registered Stock: $systemQty", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        
                                        // Status Pill Badge
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    when {
                                                        isMatch -> MaterialTheme.colorScheme.primaryContainer
                                                        variance > 0 -> MaterialTheme.colorScheme.secondaryContainer
                                                        else -> MaterialTheme.colorScheme.errorContainer
                                                    }
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = when {
                                                        isMatch -> Icons.Default.CheckCircle
                                                        variance > 0 -> Icons.Default.ArrowUpward
                                                        else -> Icons.Default.Warning
                                                    },
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = when {
                                                        isMatch -> MaterialTheme.colorScheme.onPrimaryContainer
                                                        variance > 0 -> MaterialTheme.colorScheme.onSecondaryContainer
                                                        else -> MaterialTheme.colorScheme.onError
                                                    }
                                                )
                                                Text(
                                                    text = when {
                                                        isMatch -> "DB Match"
                                                        variance > 0 -> "Surplus (+$variance)"
                                                        else -> "Shortage ($variance)"
                                                    },
                                                    color = when {
                                                        isMatch -> MaterialTheme.colorScheme.onPrimaryContainer
                                                        variance > 0 -> MaterialTheme.colorScheme.onSecondaryContainer
                                                        else -> MaterialTheme.colorScheme.onError
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    // PHYSICAL QUANTITY INCREMENTER
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (physicalQty > 0) {
                                                    auditedItems[productId] = physicalQty - 1
                                                }
                                            },
                                            modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                                        ) {
                                            Icon(Icons.Default.Remove, "Decrease audited quantity", modifier = Modifier.size(16.dp))
                                        }

                                        Text(
                                            text = "$physicalQty",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.widthIn(min = 28.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )

                                        IconButton(
                                            onClick = {
                                                auditedItems[productId] = physicalQty + 1
                                            },
                                            modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                                        ) {
                                            Icon(Icons.Default.Add, "Increase audited quantity", modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // CORRECT STOCK IN DB DIRECT BUTTON
                                    if (!isMatch) {
                                        ExpressiveButton(
                                            onClick = {
                                                viewModel.saveProduct(item.copy(stockCount = physicalQty))
                                                Toast.makeText(context, "${item.name} stocks corrected in DB!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.testTag("correct_stock_btn_${item.id}"),
                                            label = "Correct",
                                            icon = Icons.Default.Sync,
                                            variant = ExpressiveButtonVariant.TONAL,
                                            size = ExpressiveButtonSize.XS,
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                        )
                                    } else {
                                        IconButton(onClick = { auditedItems.remove(productId) }) {
                                            Icon(Icons.Default.Delete, "Remove from monitor list", tint = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating Camera Dialog for Audit Scan
            if (showScannerDialog) {
                com.munzo.storepoint.ui.BarcodeScannerDialog(
                    products = products,
                    title = "Inventory Audit Scanner",
                    instruction = "Scan product barcodes to check if physical stacks match digital store stock counts.",
                    onBarcodeScanned = { barcode ->
                        val matched = products.find { it.barcode.trim().equals(barcode.trim(), ignoreCase = true) }
                        if (matched != null) {
                            val currentQty = auditedItems[matched.id] ?: 0
                            auditedItems[matched.id] = currentQty + 1
                            viewModel.playBeep()
                            Toast.makeText(context, "Matched: ${matched.name}. Prepped in Worksheet.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Opps! No catalog item found with barcode '$barcode'", Toast.LENGTH_LONG).show()
                        }
                        showScannerDialog = false
                    },
                    onDismiss = { showScannerDialog = false }
                )
            }

        } else {
            // STANDARD PRODUCT CATALOG REGISTRY
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (products.isEmpty()) {
                    item {
                        Text("No products exist in store. Tap 'New Product' above to build items catalog.", color = Color.Gray, modifier = Modifier.padding(16.dp))
                    }
                } else {
                    items(products) { item ->
                        val isLowStock = item.stockCount in 1..5
                        val isOutOfStock = item.stockCount <= 0

                        HeroCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 3.dp)
                                .expressiveCard(cornerRadius = 24.dp)
                                .tactileBounce(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Top Row: Product Name, Category Badge, and Stock Badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val categoryName = categories.find { it.id == item.categoryId }?.name ?: "No Category"
                                        Text(
                                            text = categoryName,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                    
                                    // Stock Badge
                                    ExpressivePillBadge(
                                        text = if (isOutOfStock) "STOCKED OUT" else "${item.stockCount} IN STOCK",
                                        containerColor = when {
                                            isOutOfStock -> MaterialTheme.colorScheme.errorContainer
                                            isLowStock -> MaterialTheme.colorScheme.tertiaryContainer
                                            else -> MaterialTheme.colorScheme.primaryContainer
                                        },
                                        contentColor = when {
                                            isOutOfStock -> MaterialTheme.colorScheme.error
                                            isLowStock -> MaterialTheme.colorScheme.onTertiaryContainer
                                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                                        }
                                    )
                                }

                                // Middle Info Row: ID and Barcode, Expiration, Selling Price
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "ID: #${item.id}  •  Barcode: ${item.barcode.ifEmpty { "None" }}  •  Cost/Unit: $curr${String.format(Locale.getDefault(), "%.2f", item.cost)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (!item.expirationDate.isNullOrBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Event,
                                                    contentDescription = "Expiration Date",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Expires: ${item.expirationDate}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "$curr${String.format(Locale.getDefault(), "%.2f", item.price)}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                HeroDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )

                                // Action Buttons Row (Edit, Delete, Quick Restock)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left actions: Quick Restock
                                    HeroFilledTonalButton(
                                        onClick = { onRestockClick(item) },
                                        modifier = Modifier.weight(1f, fill = false).testTag("revised_restock_btn_${item.id}"),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddCircle,
                                            contentDescription = "Receive / Restock Unit",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Restock",
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Right actions: Variants, Edit & Delete
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedIconButton(
                                            onClick = { onVariantsClick(item) },
                                            modifier = Modifier.size(38.dp).testTag("product_variants_btn_${item.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Layers,
                                                contentDescription = "Variants & UOM",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        OutlinedIconButton(
                                            onClick = { onEdit(item) },
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit product",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        OutlinedIconButton(
                                            onClick = { onDelete(item.id) },
                                            modifier = Modifier.size(38.dp),
                                            colors = IconButtonDefaults.outlinedIconButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete product",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



