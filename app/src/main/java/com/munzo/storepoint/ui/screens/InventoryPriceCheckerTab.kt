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
 * Tab 1: Live Price Checker and Product Lookup for Aisle Audits
 */
@Composable
fun InventoryPriceCheckerTab(
    products: List<Product>,
    categories: List<Category>,
    curr: String,
    viewModel: StorePointViewModel,
    onReceiveProduct: (Product) -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var selectedStockFilter by remember { mutableStateOf("ALL") }

    val outOfStockCount = remember(products) { products.count { it.stockCount <= 0 } }
    val lowStockCount = remember(products) { products.count { it.stockCount in 1..5 } }
    val inStockCount = remember(products) { products.count { it.stockCount > 5 } }

    val filteredProducts = remember(query, selectedStockFilter, products) {
        val searched = if (query.isBlank()) products else products.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.barcode.contains(query, ignoreCase = true) ||
            it.barcode10s.contains(query, ignoreCase = true) ||
            it.barcode20s.contains(query, ignoreCase = true) ||
            it.barcodeReam.contains(query, ignoreCase = true) ||
            it.barcodeMasterCase.contains(query, ignoreCase = true) ||
            it.barcodeCustomUom.contains(query, ignoreCase = true)
        }
        val baseList = when (selectedStockFilter) {
            "OUT_OF_STOCK" -> searched.filter { it.stockCount <= 0 }
            "LOW_STOCK" -> searched.filter { it.stockCount in 1..5 }
            "IN_STOCK" -> searched.filter { it.stockCount > 5 }
            else -> searched
        }
        if (query.isBlank() && selectedStockFilter == "ALL") baseList.take(25) else baseList
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Search & Scanner Bar. The copy degrades with the width actually available to
        // the field, so a narrow window does not ellipsize a long fixed label.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val searchCopy = ScanSearchField.copyFor(maxWidth)
            HeroInput(
                value = query,
                onValueChange = { query = it },
                label = { Text(searchCopy.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                placeholder = { Text(searchCopy.placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("price_check_search_field"),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            }

            FilledIconButton(
                onClick = { showScanner = true },
                modifier = Modifier
                    .size(56.dp)
                    .testTag("price_check_scanner_btn"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode")
            }
        }

        // Camera Scanner Dialog
        if (showScanner) {
            BarcodeScannerDialog(
                products = products,
                title = "Price Checker Scanner",
                instruction = "Scan any product barcode or shelf label to verify current pricing.",
                onBarcodeScanned = { scannedCode ->
                    val matched = products.find { p ->
                        p.barcode.equals(scannedCode.trim(), ignoreCase = true) ||
                        p.barcode10s.equals(scannedCode.trim(), ignoreCase = true) ||
                        p.barcode20s.equals(scannedCode.trim(), ignoreCase = true) ||
                        p.barcodeReam.equals(scannedCode.trim(), ignoreCase = true) ||
                        p.barcodeMasterCase.equals(scannedCode.trim(), ignoreCase = true) ||
                        p.barcodeCustomUom.equals(scannedCode.trim(), ignoreCase = true)
                    }
                    if (matched != null) {
                        selectedProduct = matched
                        query = matched.name
                        viewModel.playBeep()
                        Toast.makeText(context, "Matched: ${matched.name}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "No item found for barcode '$scannedCode'", Toast.LENGTH_LONG).show()
                    }
                    showScanner = false
                },
                onDismiss = { showScanner = false }
            )
        }

        // Active Product Price Card Highlight
        if (selectedProduct != null) {
            val prod = selectedProduct!!
            val catName = categories.find { it.id == prod.categoryId }?.name ?: "Uncategorized"

            HeroCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .expressiveCard(cornerRadius = 24.dp)
                    .tactileBounce(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = prod.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Category: $catName • Barcode: ${prod.barcode.ifEmpty { "None" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        IconButton(onClick = { selectedProduct = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close details")
                        }
                    }

                    // Large Price Display
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Retail Price (${prod.baseUom}):", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    text = "$curr${String.format(Locale.US, "%.2f", prod.price)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Available Stock:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                Surface(
                                    shape = CircleShape,
                                    color = when {
                                        prod.stockCount <= 0 -> MaterialTheme.colorScheme.errorContainer
                                        prod.stockCount in 1..5 -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.primaryContainer
                                    }
                                ) {
                                    Text(
                                        text = "${prod.stockCount} ${prod.baseUom}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }

                    // Multi-UOM Price Breakdown
                    val uomOptions = viewModel.getActiveUomsForProduct(prod)
                    if (uomOptions.size > 1) {
                        Text("Multi-Unit Pricing Breakdown:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uomOptions.filter { it.multiplier > 1 }.forEach { uom ->
                                HeroCard(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(uom.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        Text("$curr${String.format(Locale.US, "%.2f", uom.price)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        Text("${uom.multiplier} ${prod.baseUom}/unit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }
                    }

                    // Receive / Restock Quick Action Button
                    HeroButton(
                        onClick = { onReceiveProduct(prod) },
                        modifier = Modifier.fillMaxWidth().testTag("price_check_receive_stock_btn"),
                        text = "Receive / Restock Item",
                        icon = Icons.Default.AddBusiness,
                        color = HeroColor.Primary,
                        variant = HeroButtonVariant.Solid,
                        size = HeroSize.Md
                    )
                }
            }
        }

        // Real-Time Stock Auditing Status Chips & Safety Threshold Indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HeroFilterChip(
                selected = selectedStockFilter == "ALL",
                onClick = { selectedStockFilter = "ALL" },
                label = "All (${products.size})",
                icon = Icons.Default.List
            )
            HeroFilterChip(
                selected = selectedStockFilter == "LOW_STOCK",
                onClick = { selectedStockFilter = "LOW_STOCK" },
                label = "Low Stock Alert ($lowStockCount)",
                color = HeroColor.Warning,
                icon = Icons.Default.Warning
            )
            HeroFilterChip(
                selected = selectedStockFilter == "OUT_OF_STOCK",
                onClick = { selectedStockFilter = "OUT_OF_STOCK" },
                label = "Out of Stock ($outOfStockCount)",
                color = HeroColor.Danger,
                icon = Icons.Default.ErrorOutline
            )
            HeroFilterChip(
                selected = selectedStockFilter == "IN_STOCK",
                onClick = { selectedStockFilter = "IN_STOCK" },
                label = "In Stock ($inStockCount)",
                color = HeroColor.Success,
                icon = Icons.Default.CheckCircle
            )
        }

        // Product Catalog List for Quick Price Inspection
        Text(
            text = "Store Catalog (${filteredProducts.size} items)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredProducts) { item ->
                HeroCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .expressiveCard(cornerRadius = 18.dp)
                        .tactileBounce(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    onClick = {
                        selectedProduct = item
                        viewModel.playBeep()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                val badgeColor = when {
                                    item.stockCount <= 0 -> MaterialTheme.colorScheme.errorContainer
                                    item.stockCount in 1..5 -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.primaryContainer
                                }
                                val badgeText = when {
                                    item.stockCount <= 0 -> "OUT OF STOCK"
                                    item.stockCount in 1..5 -> "LOW STOCK"
                                    else -> "IN STOCK"
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = badgeColor
                                ) {
                                    Text(
                                        text = badgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Barcode: ${item.barcode.ifEmpty { "N/A" }} • Stock: ${item.stockCount} ${item.baseUom}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$curr${String.format(Locale.US, "%.2f", item.price)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("Tap to inspect", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}
