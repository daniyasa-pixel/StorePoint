package com.munzo.storepoint.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.munzo.storepoint.data.PurchaseOrder
import com.munzo.storepoint.data.PurchaseOrderItem
import com.munzo.storepoint.ui.StorePointViewModel
import com.munzo.storepoint.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReceivePurchaseOrderDialog(
    po: PurchaseOrder,
    curr: String,
    viewModel: StorePointViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var paymentStatusInput by remember { mutableStateOf("PAID") }
    var scheduleBillInCalendar by remember { mutableStateOf(false) }
    var poItems by remember { mutableStateOf<List<PurchaseOrderItem>>(emptyList()) }
    val receivedQuantities = remember { mutableStateMapOf<Int, Int>() }

    LaunchedEffect(po.id) {
        viewModel.repository.getPurchaseOrderItems(po.id).collect { items ->
            poItems = items
            items.forEach {
                if (!receivedQuantities.containsKey(it.productId)) {
                    receivedQuantities[it.productId] = it.quantityOrdered
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receive Shipment - ${po.poNumber}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Supplier: ${po.supplierName}", fontWeight = FontWeight.Bold)
                Text("Confirm items received to automatically update product inventory and cost prices:")

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(poItems) { item ->
                        val currentRec = receivedQuantities[item.productId] ?: item.quantityOrdered
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.productName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text("Ordered: ${item.quantityOrdered} ${item.uomName} • Unit: $curr${String.format(Locale.getDefault(), "%.2f", item.unitCost)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                            HeroInput(
                                value = currentRec.toString(),
                                onValueChange = {
                                    val q = it.toIntOrNull() ?: 0
                                    receivedQuantities[item.productId] = q
                                },
                                label = { Text("Rec'd") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(80.dp),
                                singleLine = true
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Payment Settlement:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HeroFilterChip(
                            selected = paymentStatusInput == "PAID",
                            onClick = {
                                paymentStatusInput = "PAID"
                                scheduleBillInCalendar = false
                            },
                            label = "PAID",
                            color = HeroColor.Success
                        )
                        HeroFilterChip(
                            selected = paymentStatusInput == "UNPAID",
                            onClick = {
                                paymentStatusInput = "UNPAID"
                                scheduleBillInCalendar = true
                            },
                            label = "UNPAID (Credit)",
                            color = HeroColor.Warning
                        )
                    }
                }

                if (paymentStatusInput == "UNPAID") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HeroCheckbox(
                            checked = scheduleBillInCalendar,
                            onCheckedChange = { scheduleBillInCalendar = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Schedule credit payment in Store Bill Calendar", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            HeroButton(
                onClick = {
                    val receivedList = receivedQuantities.map { (pId, q) -> Pair(pId, q) }
                    viewModel.receivePurchaseOrder(
                        poId = po.id,
                        receivedItems = receivedList,
                        paymentStatus = paymentStatusInput,
                        onSuccess = {
                            if (paymentStatusInput == "UNPAID" && scheduleBillInCalendar) {
                                val dueDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis() + 86400000L * 7))
                                viewModel.addPaymentSchedule(
                                    title = "${po.supplierName} (${po.poNumber})",
                                    amount = po.totalCost,
                                    dueDate = dueDateStr,
                                    category = "Distributor Credit",
                                    priority = "Medium",
                                    paymentMethod = "CASH"
                                )
                            }
                            Toast.makeText(context, "Shipment received! Inventory stock and costs updated.", Toast.LENGTH_LONG).show()
                            onDismiss()
                        }
                    )
                },
                enabled = poItems.isNotEmpty() && receivedQuantities.isNotEmpty(),
                modifier = Modifier.testTag("confirm_receive_shipment_btn"),
                text = "Confirm & Restock Stock",
                color = HeroColor.Success,
                variant = HeroButtonVariant.Solid,
                size = HeroSize.Md
            )
        },
        dismissButton = {
            HeroTextButton(onClick = onDismiss, text = "Cancel")
        }
    )
}
