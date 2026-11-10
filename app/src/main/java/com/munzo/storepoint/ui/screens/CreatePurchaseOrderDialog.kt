package com.munzo.storepoint.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.munzo.storepoint.data.Product
import com.munzo.storepoint.data.Supplier
import com.munzo.storepoint.ui.StorePointViewModel
import com.munzo.storepoint.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePurchaseOrderDialog(
    suppliers: List<Supplier>,
    products: List<Product>,
    curr: String,
    viewModel: StorePointViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedSupplier by remember { mutableStateOf(suppliers.firstOrNull()) }
    var poNotes by remember { mutableStateOf("") }
    var expectedDeliveryDaysOffset by remember { mutableStateOf(3) }
    // Order items: productId -> Pair(quantity, unitCost)
    val orderItems = remember { mutableStateMapOf<Int, Pair<Int, Double>>() }
    val qtyInputs = remember { mutableStateMapOf<Int, String>() }
    val costInputs = remember { mutableStateMapOf<Int, String>() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Create Purchase Order", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                    )
                }
            ) { pValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(pValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Supplier Selector
                    var supplierDropdownExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        HeroOutlinedButton(
                            onClick = { supplierDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            text = "Supplier: ${selectedSupplier?.name ?: "Select Supplier"}"
                        )
                        DropdownMenu(expanded = supplierDropdownExpanded, onDismissRequest = { supplierDropdownExpanded = false }) {
                            suppliers.forEach { sup ->
                                DropdownMenuItem(
                                    text = { Text(sup.name) },
                                    onClick = {
                                        selectedSupplier = sup
                                        supplierDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Expected Delivery Date Presets
                    val targetExpectedDeliveryDate = System.currentTimeMillis() + 86400000L * expectedDeliveryDaysOffset
                    val expectedDateFormatted = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).format(Date(targetExpectedDeliveryDate))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Expected Delivery Date:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Text(expectedDateFormatted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                1 to "Tomorrow (+1d)",
                                3 to "3 Days",
                                7 to "7 Days",
                                15 to "15 Days",
                                30 to "30 Days"
                            ).forEach { (days, label) ->
                                HeroFilterChip(
                                    selected = expectedDeliveryDaysOffset == days,
                                    onClick = { expectedDeliveryDaysOffset = days },
                                    label = label,
                                    modifier = Modifier.testTag("delivery_preset_${days}d")
                                )
                            }
                        }
                    }

                    Text("Select Products to Order:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(products) { prod ->
                            val selection = orderItems[prod.id]
                            val isSelected = selection != null
                            val currentQty = selection?.first ?: 1
                            val currentCost = selection?.second ?: prod.cost

                            HeroCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            HeroCheckbox(
                                                checked = isSelected,
                                                onCheckedChange = { checked ->
                                                    if (checked) {
                                                        val initialQty = 10
                                                        val initialCost = if (prod.cost > 0) prod.cost else prod.price * 0.8
                                                        orderItems[prod.id] = Pair(initialQty, initialCost)
                                                        qtyInputs[prod.id] = initialQty.toString()
                                                        costInputs[prod.id] = String.format(Locale.US, "%.2f", initialCost)
                                                    } else {
                                                        orderItems.remove(prod.id)
                                                        qtyInputs.remove(prod.id)
                                                        costInputs.remove(prod.id)
                                                    }
                                                }
                                            )
                                            Column {
                                                Text(prod.name, fontWeight = FontWeight.Bold)
                                                Text("Current Stock: ${prod.stockCount} ${prod.baseUom}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                            }
                                        }

                                        if (isSelected) {
                                            Text(
                                                "$curr${String.format(Locale.getDefault(), "%.2f", currentQty * currentCost)}",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 40.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val currentQtyStr = qtyInputs[prod.id] ?: currentQty.toString()
                                            HeroInput(
                                                value = currentQtyStr,
                                                onValueChange = { text ->
                                                    qtyInputs[prod.id] = text
                                                    val q = text.toIntOrNull() ?: 1
                                                    orderItems[prod.id] = Pair(q, currentCost)
                                                },
                                                label = { Text("Qty") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                            val currentCostStr = costInputs[prod.id] ?: String.format(Locale.US, "%.2f", currentCost)
                                            HeroInput(
                                                value = currentCostStr,
                                                onValueChange = { text ->
                                                    costInputs[prod.id] = text
                                                    val c = text.replace(',', '.').toDoubleOrNull() ?: 0.0
                                                    orderItems[prod.id] = Pair(currentQty, c)
                                                },
                                                label = { Text("Unit Cost") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.weight(1.2f),
                                                singleLine = true
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val totalPoCost = orderItems.entries.sumOf { it.value.first * it.value.second }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total PO Cost:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "$curr${String.format(Locale.getDefault(), "%.2f", totalPoCost)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HeroButton(
                        onClick = {
                            if (selectedSupplier == null || orderItems.isEmpty()) {
                                Toast.makeText(context, "Select at least 1 product to order.", Toast.LENGTH_SHORT).show()
                                return@HeroButton
                            }
                            val itemsList = orderItems.mapNotNull { (pId, details) ->
                                val p = products.find { it.id == pId }
                                if (p != null) Pair(p, details) else null
                            }

                            viewModel.createPurchaseOrder(
                                supplier = selectedSupplier!!,
                                expectedDeliveryDate = targetExpectedDeliveryDate,
                                items = itemsList,
                                notes = poNotes,
                                onSuccess = {
                                    Toast.makeText(context, "PO ${it.poNumber} created successfully!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("submit_po_btn"),
                        enabled = orderItems.isNotEmpty(),
                        text = "Place Purchase Order (Pending)",
                        icon = Icons.Default.Send,
                        color = HeroColor.Primary,
                        variant = HeroButtonVariant.Solid,
                        size = HeroSize.Lg
                    )
                }
            }
        }
    }
}
