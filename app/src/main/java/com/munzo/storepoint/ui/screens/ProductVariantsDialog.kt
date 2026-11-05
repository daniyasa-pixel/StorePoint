package com.munzo.storepoint.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.munzo.storepoint.data.Product
import com.munzo.storepoint.data.ProductVariant
import com.munzo.storepoint.ui.BarcodeScannerDialog
import com.munzo.storepoint.ui.StorePointViewModel
import com.munzo.storepoint.ui.theme.HeroUITheme
import com.munzo.storepoint.ui.components.*
import com.munzo.storepoint.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductVariantsDialog(
    product: Product,
    viewModel: StorePointViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val storeConfig by viewModel.storeConfig.collectAsState()
    val allVariants by viewModel.allProductVariants.collectAsState()
    val curr = storeConfig?.currencySymbol ?: "₱"

    val productVariants = remember(allVariants, product.id) {
        allVariants.filter { it.productId == product.id }
    }

    var isAddingOrEditing by remember { mutableStateOf(false) }
    var editingVariantId by remember { mutableStateOf(0) }
    var variantNameInput by remember { mutableStateOf("") }
    var uomNameInput by remember { mutableStateOf("pc") }
    var multiplierInput by remember { mutableStateOf("1") }
    var priceInput by remember { mutableStateOf("") }
    var costInput by remember { mutableStateOf("") }
    var barcodeInput by remember { mutableStateOf("") }
    var stockCountInput by remember { mutableStateOf("0") }
    var showBarcodeScanner by remember { mutableStateOf(false) }

    fun resetForm() {
        editingVariantId = 0
        variantNameInput = ""
        uomNameInput = "pc"
        multiplierInput = "1"
        priceInput = ""
        costInput = ""
        barcodeInput = ""
        stockCountInput = "0"
        isAddingOrEditing = false
        showBarcodeScanner = false
    }

    fun startEdit(variant: ProductVariant) {
        editingVariantId = variant.id
        variantNameInput = variant.variantName
        uomNameInput = variant.uomName
        multiplierInput = variant.multiplier.toString()
        priceInput = String.format(Locale.getDefault(), "%.2f", variant.price)
        costInput = String.format(Locale.getDefault(), "%.2f", variant.cost)
        barcodeInput = variant.barcode
        stockCountInput = variant.stockCount.toString()
        isAddingOrEditing = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .imePadding()
                .expressiveGlassCard(cornerRadius = 28.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    "Variants & UOM",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${product.name} (Base: $curr${String.format(Locale.getDefault(), "%.2f", product.price)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        },
                        actions = {
                            if (!isAddingOrEditing) {
                                HeroButton(
                                    onClick = {
                                        resetForm()
                                        isAddingOrEditing = true
                                    },
                                    text = "Add Variant",
                                    icon = Icons.Default.Add,
                                    modifier = Modifier.padding(end = 8.dp).testTag("add_variant_btn")
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    if (isAddingOrEditing) {
                        // Edit/Add Form
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                if (editingVariantId == 0) "Create New Product Variant" else "Edit Product Variant",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // 1. Packaging Size Presets
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Packaging Size Presets:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AssistChip(
                                        onClick = {
                                            variantNameInput = "Tingi (1s)"
                                            uomNameInput = "tingi"
                                            multiplierInput = "1"
                                        },
                                        label = { Text("Tingi (1s)", style = MaterialTheme.typography.labelSmall) }
                                    )
                                    AssistChip(
                                        onClick = {
                                            variantNameInput = "6-Pack"
                                            uomNameInput = "pack"
                                            multiplierInput = "6"
                                        },
                                        label = { Text("6-Pack", style = MaterialTheme.typography.labelSmall) }
                                    )
                                    AssistChip(
                                        onClick = {
                                            variantNameInput = "12-Pack (Dozen)"
                                            uomNameInput = "pack"
                                            multiplierInput = "12"
                                        },
                                        label = { Text("12-Pack (Dozen)", style = MaterialTheme.typography.labelSmall) }
                                    )
                                    AssistChip(
                                        onClick = {
                                            variantNameInput = "Case / Box (24s)"
                                            uomNameInput = "box"
                                            multiplierInput = "24"
                                        },
                                        label = { Text("Box (24s)", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }

                            // 2. Sizing Presets
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Volume & Portion Sizing Presets:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        Triple("Small", "pc", 1),
                                        Triple("Medium", "pc", 1),
                                        Triple("Large", "pc", 1),
                                        Triple("250ml", "bottle", 1),
                                        Triple("500ml", "bottle", 1),
                                        Triple("1 Liter", "bottle", 1),
                                        Triple("1.5L", "bottle", 1)
                                    ).forEach { (sizeLabel, uom, mult) ->
                                        AssistChip(
                                            onClick = {
                                                variantNameInput = sizeLabel
                                                uomNameInput = uom
                                                multiplierInput = mult.toString()
                                            },
                                            label = { Text(sizeLabel, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            }

                            // 3. Flavor Presets
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Flavor Presets:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("Original", "Spicy", "Cheese", "BBQ", "Sour Cream", "Sweet Chili").forEach { flavor ->
                                        AssistChip(
                                            onClick = {
                                                variantNameInput = flavor
                                                uomNameInput = "pc"
                                                multiplierInput = "1"
                                            },
                                            label = { Text(flavor, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                HeroInput(
                                    value = variantNameInput,
                                    onValueChange = { variantNameInput = it },
                                    label = { Text("Variant Name (e.g. 1L Bottle, Spicy, 6-Pack)") },
                                    modifier = Modifier.weight(1.5f).testTag("variant_name_input"),
                                    singleLine = true
                                )
                                HeroInput(
                                    value = uomNameInput,
                                    onValueChange = { uomNameInput = it },
                                    label = { Text("UOM (pc, pack, kg, bottle)") },
                                    modifier = Modifier.weight(1f).testTag("variant_uom_input"),
                                    singleLine = true
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                HeroInput(
                                    value = multiplierInput,
                                    onValueChange = { multiplierInput = it },
                                    label = { Text("Stock Multiplier") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f).testTag("variant_multiplier_input"),
                                    singleLine = true
                                )

                                HeroInput(
                                    value = stockCountInput,
                                    onValueChange = { stockCountInput = it },
                                    label = { Text("Variant Stock Count") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f).testTag("variant_stock_input"),
                                    singleLine = true
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                HeroInput(
                                    value = priceInput,
                                    onValueChange = { priceInput = it },
                                    label = { Text("Selling Price ($curr)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f).testTag("variant_price_input"),
                                    singleLine = true
                                )
                                HeroInput(
                                    value = costInput,
                                    onValueChange = { costInput = it },
                                    label = { Text("Cost Price ($curr)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f).testTag("variant_cost_input"),
                                    singleLine = true
                                )
                            }

                            // Individual Barcode Field with CameraX Scanner Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HeroInput(
                                    value = barcodeInput,
                                    onValueChange = { barcodeInput = it },
                                    label = { Text("Variant Barcode (Optional - scan directly on POS)") },
                                    modifier = Modifier.weight(1f).testTag("variant_barcode_input"),
                                    singleLine = true
                                )
                                FilledIconButton(
                                    onClick = { showBarcodeScanner = true },
                                    modifier = Modifier.size(56.dp).testTag("variant_barcode_scan_btn")
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Variant Barcode")
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { resetForm() }) {
                                    Text("Cancel")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                HeroButton(
                                    onClick = {
                                        val vName = variantNameInput.trim()
                                        val mult = multiplierInput.toIntOrNull() ?: 1
                                        val prc = priceInput.toDoubleOrNull() ?: 0.0
                                        val cst = costInput.toDoubleOrNull() ?: 0.0
                                        val stk = stockCountInput.toIntOrNull() ?: 0
                                        if (vName.isBlank() || prc <= 0.0) {
                                            Toast.makeText(context, "Please enter a variant name and valid price.", Toast.LENGTH_SHORT).show()
                                            return@HeroButton
                                        }

                                        val variant = ProductVariant(
                                            id = editingVariantId,
                                            productId = product.id,
                                            variantName = vName,
                                            uomName = uomNameInput.trim().ifBlank { "pc" },
                                            multiplier = mult.coerceAtLeast(1),
                                            price = prc,
                                            cost = cst,
                                            barcode = barcodeInput.trim(),
                                            stockCount = stk
                                        )

                                        viewModel.saveProductVariant(variant) {
                                            Toast.makeText(context, "Variant saved successfully!", Toast.LENGTH_SHORT).show()
                                            resetForm()
                                        }
                                    },
                                    text = "Save Variant",
                                    modifier = Modifier.testTag("save_variant_submit_btn")
                                )
                            }
                        }
                    } else {
                        // Variants List
                        if (productVariants.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(56.dp))
                                    Text("No custom variants configured for this product.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                    Text("Tap '+ Add Variant' to configure tingi, flavors, multi-packs, or sizing.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(productVariants) { variant ->
                                    HeroCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .expressiveCard(cornerRadius = 20.dp)
                                            .tactileBounce(),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(
                                                        variant.variantName,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                                        modifier = Modifier.padding(horizontal = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = "Stock: ${variant.stockCount}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    "Deducts ${variant.multiplier} base units • UOM: ${variant.uomName}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.outline,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (variant.barcode.isNotBlank()) {
                                                    Text(
                                                        "Barcode: ${variant.barcode}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                // Quick stock stepper buttons
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.updateVariantStock(variant.id, (variant.stockCount - 1).coerceAtLeast(0))
                                                        },
                                                        modifier = Modifier.size(28.dp).testTag("variant_stock_dec_${variant.id}")
                                                    ) {
                                                        Icon(Icons.Default.Remove, contentDescription = "Decrease stock", modifier = Modifier.size(16.dp))
                                                    }
                                                    Text(
                                                        "${variant.stockCount}",
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        modifier = Modifier.padding(horizontal = 4.dp)
                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.updateVariantStock(variant.id, variant.stockCount + 1)
                                                        },
                                                        modifier = Modifier.size(28.dp).testTag("variant_stock_inc_${variant.id}")
                                                    ) {
                                                        Icon(Icons.Default.Add, contentDescription = "Increase stock", modifier = Modifier.size(16.dp))
                                                    }
                                                }

                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        "$curr${String.format(Locale.getDefault(), "%.2f", variant.price)}",
                                                        fontWeight = FontWeight.Black,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                    if (variant.cost > 0.0) {
                                                        Text(
                                                            "Cost: $curr${String.format(Locale.getDefault(), "%.2f", variant.cost)}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.outline
                                                        )
                                                    }
                                                }

                                                IconButton(onClick = { startEdit(variant) }) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit Variant", tint = MaterialTheme.colorScheme.outline)
                                                }

                                                IconButton(onClick = {
                                                    viewModel.deleteProductVariant(variant.id) {
                                                        Toast.makeText(context, "Variant deleted.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete Variant", tint = MaterialTheme.colorScheme.error)
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
    }

    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            title = "Scan Variant Barcode",
            instruction = "Align the variant product barcode in the scanner window.",
            onBarcodeScanned = { code ->
                barcodeInput = code
                showBarcodeScanner = false
                try {
                    val toneG = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
                    toneG.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 120)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try { toneG.release() } catch (ignored: Exception) {}
                    }, 250)
                } catch (e: Exception) {
                    android.util.Log.w("VariantsDialog", "Tone generation failed: ${e.message}")
                }
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }
}
