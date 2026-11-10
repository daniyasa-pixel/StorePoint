package com.munzo.storepoint.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.munzo.storepoint.data.Category
import com.munzo.storepoint.data.Product
import com.munzo.storepoint.ui.BarcodeScannerDialog
import com.munzo.storepoint.ui.StorePointViewModel
import com.munzo.storepoint.ui.components.*
import com.munzo.storepoint.ui.layout.ScanSearchField
import com.munzo.storepoint.ui.theme.*
import java.util.Locale

/**
 * Tab 2: Interactive Stocktake Audit Console
 */
@Composable
fun InventoryStocktakeAuditTab(
    products: List<Product>,
    categories: List<Category>,
    curr: String,
    viewModel: StorePointViewModel,
    onReceiveProduct: (Product) -> Unit
) {
    val context = LocalContext.current
    val auditedItems = remember { mutableStateMapOf<Int, Int>() }
    var auditQuery by remember { mutableStateOf("") }
    var showAuditScanner by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Audit Metrics Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val totalAudited = auditedItems.size
            val matches = auditedItems.count { (id, count) -> products.find { it.id == id }?.stockCount == count }
            val discrepancies = totalAudited - matches

            ExpressiveMetricCard(
                title = "Audited",
                value = "$totalAudited",
                subtitle = "of ${products.size} items",
                icon = Icons.Default.Checklist,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            ExpressiveMetricCard(
                title = "In Sync",
                value = "$matches",
                subtitle = "DB matched",
                icon = Icons.Default.CheckCircle,
                accentColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )

            ExpressiveMetricCard(
                title = "Discrepancies",
                value = "$discrepancies",
                subtitle = if (discrepancies > 0) "Needs sync" else "All balanced",
                icon = Icons.Default.Warning,
                accentColor = if (discrepancies > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                modifier = Modifier.weight(1f)
            )
        }

        // Action Toolbar. The audit field shares this row with a fixed-width button, so
        // the sibling space is reserved when resolving the search copy.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val searchCopy = ScanSearchField.copyFor(maxWidth, hasSiblingField = true)
            HeroInput(
                value = auditQuery,
                onValueChange = { auditQuery = it },
                label = { Text(searchCopy.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                placeholder = { Text(searchCopy.placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.fillMaxWidth().testTag("audit_input_field"),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    IconButton(onClick = { showAuditScanner = true }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Camera Scanner", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
            }

            HeroButton(
                onClick = {
                    if (auditQuery.isNotBlank()) {
                        val match = products.find {
                            it.barcode.equals(auditQuery.trim(), ignoreCase = true) ||
                            it.name.contains(auditQuery.trim(), ignoreCase = true)
                        }
                        if (match != null) {
                            val cur = auditedItems[match.id] ?: 0
                            auditedItems[match.id] = cur + 1
                            viewModel.playBeep()
                            auditQuery = ""
                            Toast.makeText(context, "Audited: ${match.name} (+1)", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Item not found in catalog.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.height(56.dp).testTag("audit_add_btn"),
                text = "Count",
                icon = Icons.Default.Add,
                color = HeroColor.Primary,
                variant = HeroButtonVariant.Solid,
                size = HeroSize.Md
            )
        }

        // Scanner Dialog
        if (showAuditScanner) {
            BarcodeScannerDialog(
                products = products,
                title = "Stocktake Camera Scanner",
                instruction = "Scan barcodes continuously to increment audited counts.",
                onBarcodeScanned = { scanned ->
                    val match = products.find { it.barcode.equals(scanned.trim(), ignoreCase = true) }
                    if (match != null) {
                        val cur = auditedItems[match.id] ?: 0
                        auditedItems[match.id] = cur + 1
                        viewModel.playBeep()
                        Toast.makeText(context, "${match.name}: Scanned count ${cur + 1}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Unknown barcode: $scanned", Toast.LENGTH_LONG).show()
                    }
                    showAuditScanner = false
                },
                onDismiss = { showAuditScanner = false }
            )
        }

        // Audit Worksheet Header & Bulk Actions (stacks on compact windows so the
        // action buttons keep full labels and 48dp touch targets)
        val worksheetCompact = rememberExpressiveWidthClass() == ExpressiveWidthClass.COMPACT
        if (auditedItems.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Audit Worksheet (${auditedItems.size} items monitored)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (worksheetCompact) Arrangement.spacedBy(8.dp)
                    else Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeroButton(
                        onClick = {
                            var synced = 0
                            auditedItems.forEach { (id, physicalCount) ->
                                val p = products.find { it.id == id }
                                if (p != null && p.stockCount != physicalCount) {
                                    viewModel.saveProduct(p.copy(stockCount = physicalCount))
                                    synced++
                                }
                            }
                            if (synced > 0) {
                                Toast.makeText(context, "Corrected $synced product counts in database!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "All audited items match system counts.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("audit_sync_btn"),
                        text = "Sync All",
                        icon = Icons.Default.Sync,
                        color = HeroColor.Success,
                        variant = HeroButtonVariant.Solid,
                        size = HeroSize.Md
                    )

                    HeroButton(
                        onClick = { auditedItems.clear() },
                        modifier = Modifier.testTag("audit_clear_btn"),
                        text = "Clear",
                        icon = Icons.Default.Clear,
                        color = HeroColor.Danger,
                        variant = HeroButtonVariant.Bordered,
                        size = HeroSize.Md
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Audit Worksheet (0 items monitored)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Worksheet List
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (auditedItems.isEmpty()) {
                item {
                    HeroCard(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("Worksheet Empty", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Scan or search items above to begin counting physical inventory stacks on the store floor.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(auditedItems.keys.toList()) { productId ->
                    val item = products.find { it.id == productId }
                    val physicalCount = auditedItems[productId] ?: 0
                    if (item != null) {
                        val systemCount = item.stockCount
                        val variance = physicalCount - systemCount
                        val isMatch = variance == 0

                        HeroCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassPanel(cornerRadius = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMatch) Color.Transparent else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("System Stock: $systemCount • Barcode: ${item.barcode.ifEmpty { "N/A" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    
                                    // Status pill
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = when {
                                            isMatch -> MaterialTheme.colorScheme.primaryContainer
                                            variance > 0 -> MaterialTheme.colorScheme.secondaryContainer
                                            else -> MaterialTheme.colorScheme.errorContainer
                                        },
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                isMatch -> "Matched"
                                                variance > 0 -> "Surplus (+$variance)"
                                                else -> "Shortage ($variance)"
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Physical quantity controls
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (physicalCount > 0) auditedItems[productId] = physicalCount - 1
                                        },
                                        modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                                    }

                                    Text(
                                        text = "$physicalCount",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.widthIn(min = 28.dp),
                                        textAlign = TextAlign.Center
                                    )

                                    IconButton(
                                        onClick = { auditedItems[productId] = physicalCount + 1 },
                                        modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    if (!isMatch) {
                                        IconButton(
                                            onClick = {
                                                viewModel.saveProduct(item.copy(stockCount = physicalCount))
                                                Toast.makeText(context, "${item.name} stock updated to $physicalCount!", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = "Sync", tint = MaterialTheme.colorScheme.primary)
                                        }

                                        IconButton(
                                            onClick = { onReceiveProduct(item) },
                                            modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(8.dp))
                                        ) {
                                            Icon(Icons.Default.AddBusiness, contentDescription = "Restock", tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(16.dp))
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
