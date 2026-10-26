package com.munzo.storepoint.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.munzo.storepoint.data.BillOfMaterials
import com.munzo.storepoint.data.Product
import com.munzo.storepoint.data.ProductionLogEntity
import com.munzo.storepoint.data.StorePointRepository
import com.munzo.storepoint.ui.StorePointViewModel
import com.munzo.storepoint.ui.components.ExpressiveButton
import com.munzo.storepoint.ui.components.ExpressiveButtonSize
import com.munzo.storepoint.ui.components.ExpressiveButtonVariant
import com.munzo.storepoint.ui.executeRepackagingProduction
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkProductionDialog(
    viewModel: StorePointViewModel,
    onDismiss: () -> Unit,
    preselectedRawProduct: Product? = null
) {
    val context = LocalContext.current
    val allProducts by viewModel.allProducts.collectAsState()
    val allBoms by viewModel.allBOMs.collectAsState()
    val storeConfig by viewModel.storeConfig.collectAsState()
    val curr = storeConfig?.currencySymbol ?: "$"

    var selectedRawProduct by remember { mutableStateOf(preselectedRawProduct ?: allProducts.firstOrNull()) }
    var selectedTargetProduct by remember { mutableStateOf<Product?>(null) }

    var rawUsageQtyStr by remember { mutableStateOf("1") }
    var targetYieldQtyStr by remember { mutableStateOf("10") }
    var wasteLossPctStr by remember { mutableStateOf("0") }
    var retailPriceStr by remember { mutableStateOf("") }
    var productionNotes by remember { mutableStateOf("") }
    var saveAsBomRecipe by remember { mutableStateOf(true) }

    var isExecuting by remember { mutableStateOf(false) }

    // Dropdown expansion states
    var rawDropdownExpanded by remember { mutableStateOf(false) }
    var targetDropdownExpanded by remember { mutableStateOf(false) }
    var bomDropdownExpanded by remember { mutableStateOf(false) }

    // When a BOM recipe is selected, autofill
    fun applyBom(bom: BillOfMaterials) {
        val raw = allProducts.firstOrNull { it.id == bom.rawMaterialProductId }
        val target = allProducts.firstOrNull { it.id == bom.targetRetailProductId }
        if (raw != null) selectedRawProduct = raw
        if (target != null) {
            selectedTargetProduct = target
            retailPriceStr = String.format(Locale.US, "%.2f", target.price)
        }
        rawUsageQtyStr = bom.rawQuantityNeeded.toInt().toString()
        targetYieldQtyStr = bom.producedRetailYield.toInt().toString()
        wasteLossPctStr = String.format(Locale.US, "%.1f", bom.wasteLossPercentage)
    }

    // When target product changes, prefill selling price
    LaunchedEffect(selectedTargetProduct) {
        val target = selectedTargetProduct
        if (target != null && retailPriceStr.isBlank()) {
            retailPriceStr = String.format(Locale.US, "%.2f", target.price)
        }
    }

    val rawUsage = rawUsageQtyStr.toDoubleOrNull() ?: 0.0
    val targetYield = targetYieldQtyStr.toDoubleOrNull() ?: 0.0
    val wastePct = wasteLossPctStr.toDoubleOrNull() ?: 0.0
    val retailPrice = retailPriceStr.toDoubleOrNull() ?: (selectedTargetProduct?.price ?: 0.0)

    val rawUnitCost = selectedRawProduct?.cost ?: 0.0
    val totalBatchRawCost = StorePointRepository.roundMoney(rawUsage * rawUnitCost)
    val wasteLossQty = if (targetYield > 0.0) (targetYield * (wastePct / 100.0)) else 0.0
    val netSellableYield = (targetYield - wasteLossQty).coerceAtLeast(1.0)
    val derivedUnitCost = if (netSellableYield > 0.0) {
        StorePointRepository.roundMoney(totalBatchRawCost / netSellableYield)
    } else 0.0

    val profitMarginPct = if (retailPrice > 0.0) {
        StorePointRepository.roundMoney(((retailPrice - derivedUnitCost) / retailPrice) * 100.0)
    } else 0.0

    val isRawStockSufficient = selectedRawProduct?.let { it.stockCount >= rawUsage.toInt() } ?: false
    val canExecute = selectedRawProduct != null &&
            selectedTargetProduct != null &&
            selectedRawProduct?.id != selectedTargetProduct?.id &&
            rawUsage > 0.0 &&
            targetYield > 0.0 &&
            isRawStockSufficient &&
            !isExecuting

    AlertDialog(
        onDismissRequest = { if (!isExecuting) onDismiss() },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Transform, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Bulk-to-Retail Production & Repackaging", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                IconButton(onClick = onDismiss, enabled = !isExecuting) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Transform bulk bulk stock (e.g. 20L Water/Oil, 50kg Sacks) into sellable retail packaged SKUs with atomic inventory updates and cost derivation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick load from existing BOM recipes if available
                if (allBoms.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { bomDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Load Preset Recipe (${allBoms.size} saved BOMs)")
                        }
                        DropdownMenu(
                            expanded = bomDropdownExpanded,
                            onDismissRequest = { bomDropdownExpanded = false }
                        ) {
                            allBoms.forEach { bom ->
                                val rawName = allProducts.firstOrNull { it.id == bom.rawMaterialProductId }?.name ?: "Raw #${bom.rawMaterialProductId}"
                                val targetName = allProducts.firstOrNull { it.id == bom.targetRetailProductId }?.name ?: "Retail #${bom.targetRetailProductId}"
                                DropdownMenuItem(
                                    text = { Text("$rawName ➔ $targetName (${bom.producedRetailYield.toInt()} yield)") },
                                    onClick = {
                                        applyBom(bom)
                                        bomDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Step 1: Source Raw Material Product
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("1. Source Raw Bulk Stock", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedRawProduct?.name ?: "Select Raw Product",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Raw Bulk Stock SKU") },
                                trailingIcon = {
                                    IconButton(onClick = { rawDropdownExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = rawDropdownExpanded,
                                onDismissRequest = { rawDropdownExpanded = false },
                                modifier = Modifier.heightIn(max = 280.dp)
                            ) {
                                allProducts.forEach { prod ->
                                    DropdownMenuItem(
                                        text = { Text("${prod.name} (Stock: ${prod.stockCount} ${prod.baseUom} • Cost: $curr${String.format(Locale.US, "%.2f", prod.cost)})") },
                                        onClick = {
                                            selectedRawProduct = prod
                                            rawDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        selectedRawProduct?.let { raw ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Available: ${raw.stockCount} ${raw.baseUom} • Unit Cost: $curr${String.format(Locale.US, "%.2f", raw.cost)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (raw.stockCount < rawUsage.toInt()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        OutlinedTextField(
                            value = rawUsageQtyStr,
                            onValueChange = { str -> if (str.all { it.isDigit() }) rawUsageQtyStr = str },
                            label = { Text("Raw Units Consumed (${selectedRawProduct?.baseUom ?: "units"})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("raw_usage_input"),
                            singleLine = true,
                            isError = !isRawStockSufficient
                        )
                        if (!isRawStockSufficient) {
                            Text("Insufficient stock in inventory for this batch size.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Step 2: Target Retail Output SKU
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("2. Target Packaged Retail SKU", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedTargetProduct?.name ?: "Select Target Retail Product",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Target Retail Product") },
                                trailingIcon = {
                                    IconButton(onClick = { targetDropdownExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = targetDropdownExpanded,
                                onDismissRequest = { targetDropdownExpanded = false },
                                modifier = Modifier.heightIn(max = 280.dp)
                            ) {
                                allProducts.filter { it.id != selectedRawProduct?.id }.forEach { prod ->
                                    DropdownMenuItem(
                                        text = { Text("${prod.name} (Price: $curr${String.format(Locale.US, "%.2f", prod.price)} • Stock: ${prod.stockCount})") },
                                        onClick = {
                                            selectedTargetProduct = prod
                                            retailPriceStr = String.format(Locale.US, "%.2f", prod.price)
                                            targetDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = targetYieldQtyStr,
                                onValueChange = { str -> if (str.all { it.isDigit() }) targetYieldQtyStr = str },
                                label = { Text("Yield (${selectedTargetProduct?.baseUom ?: "pcs"})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("target_yield_input"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = wasteLossPctStr,
                                onValueChange = { wasteLossPctStr = it },
                                label = { Text("Waste / Scrap %") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).testTag("waste_pct_input"),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = retailPriceStr,
                            onValueChange = { retailPriceStr = it },
                            label = { Text("Retail Selling Price ($curr)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().testTag("retail_price_input"),
                            singleLine = true
                        )
                    }
                }

                // Step 3: Financial & Derived Cost Breakdown
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("3. Derived Economics & Profit Margin", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Batch Raw Material Cost:", style = MaterialTheme.typography.bodySmall)
                            Text("$curr${String.format(Locale.US, "%.2f", totalBatchRawCost)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Derived Unit Cost (incl. waste loss):", style = MaterialTheme.typography.bodySmall)
                            Text("$curr${String.format(Locale.US, "%.2f", derivedUnitCost)} / unit", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Projected Profit Margin:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Surface(
                                shape = CircleShape,
                                color = if (profitMarginPct >= 20.0) Color(0xFF10B981) else MaterialTheme.colorScheme.tertiary
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", profitMarginPct)}% ($curr${String.format(Locale.US, "%.2f", retailPrice - derivedUnitCost)}/unit)",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Recipe Save & Notes
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = saveAsBomRecipe,
                        onCheckedChange = { saveAsBomRecipe = it }
                    )
                    Text("Save as standard Bill of Materials (BOM) recipe for 1-click repetition", style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = productionNotes,
                    onValueChange = { productionNotes = it },
                    label = { Text("Batch Notes / Operator Instructions") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            ExpressiveButton(
                onClick = {
                    val raw = selectedRawProduct ?: return@ExpressiveButton
                    val target = selectedTargetProduct ?: return@ExpressiveButton

                    isExecuting = true
                    viewModel.executeRepackagingProduction(
                        rawMaterialProductId = raw.id,
                        rawMaterialQtyUsed = rawUsage,
                        targetRetailProductId = target.id,
                        targetRetailQtyProduced = targetYield,
                        wasteLossQty = wasteLossQty,
                        retailSellingPrice = retailPrice,
                        saveAsBom = saveAsBomRecipe,
                        notes = productionNotes.trim()
                    ) { success, msg, _ ->
                        isExecuting = false
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        if (success) {
                            onDismiss()
                        }
                    }
                },
                label = if (isExecuting) "Processing Batch..." else "Execute Production Run",
                icon = Icons.Default.PrecisionManufacturing,
                enabled = canExecute,
                variant = ExpressiveButtonVariant.FILLED,
                size = ExpressiveButtonSize.M,
                modifier = Modifier.testTag("execute_production_btn")
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isExecuting) {
                Text("Cancel")
            }
        }
    )
}

