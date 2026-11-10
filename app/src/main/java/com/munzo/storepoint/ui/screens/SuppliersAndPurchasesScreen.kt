package com.munzo.storepoint.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.munzo.storepoint.data.*
import com.munzo.storepoint.ui.StorePointViewModel
import com.munzo.storepoint.ui.components.ExpressiveButton
import com.munzo.storepoint.ui.components.ExpressiveButtonSize
import com.munzo.storepoint.ui.components.ExpressiveButtonVariant
import com.munzo.storepoint.ui.theme.*
import com.munzo.storepoint.ui.theme.HeroUITheme
import com.munzo.storepoint.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersAndPurchasesTab(
    viewModel: StorePointViewModel
) {
    val context = LocalContext.current
    val storeConfig by viewModel.storeConfig.collectAsState()
    val suppliers by viewModel.allSuppliers.collectAsState()
    val purchaseOrders by viewModel.allPurchaseOrders.collectAsState()
    val products by viewModel.allProducts.collectAsState()
    val curr = storeConfig?.currencySymbol ?: "₱"

    var subTab by remember { mutableStateOf(0) } // 0 = Suppliers, 1 = Purchase Orders
    var supplierSearchQuery by remember { mutableStateOf("") }
    var poStatusFilter by remember { mutableStateOf("All") } // "All", "Pending", "Received", "Canceled"

    // Dialog flags
    var showSupplierDialog by remember { mutableStateOf(false) }
    var editingSupplier by remember { mutableStateOf<Supplier?>(null) }

    var showCreatePoDialog by remember { mutableStateOf(false) }
    var receivingPo by remember { mutableStateOf<PurchaseOrder?>(null) }
    var poToCancel by remember { mutableStateOf<PurchaseOrder?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Sub-tabs & Action Row
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompact = maxWidth < 600.dp
            if (isCompact) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroSegmentedTabs(
                        modifier = Modifier.fillMaxWidth(),
                        items = listOf(
                            "Suppliers (${suppliers.size})",
                            "Purchase Orders (${purchaseOrders.size})"
                        ),
                        selectedItem = if (subTab == 0) "Suppliers (${suppliers.size})" else "Purchase Orders (${purchaseOrders.size})",
                        onItemSelected = { selected ->
                            subTab = if (selected.startsWith("Suppliers")) 0 else 1
                        },
                        labelProvider = { it },
                        iconProvider = { if (it.startsWith("Suppliers")) Icons.Default.Business else Icons.Default.ShoppingCart }
                    )

                    if (subTab == 0) {
                        HeroButton(
                            onClick = {
                                editingSupplier = null
                                showSupplierDialog = true
                            },
                            modifier = Modifier.fillMaxWidth().testTag("add_supplier_btn"),
                            text = "Add Supplier / Distributor",
                            icon = Icons.Default.Add,
                            color = HeroColor.Primary,
                            variant = HeroButtonVariant.Solid,
                            size = HeroSize.Md
                        )
                    } else {
                        HeroButton(
                            onClick = {
                                if (suppliers.isEmpty()) {
                                    Toast.makeText(context, "Please add at least one supplier first.", Toast.LENGTH_SHORT).show()
                                } else {
                                    showCreatePoDialog = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("create_po_btn"),
                            text = "Create Purchase Order",
                            icon = Icons.Default.NoteAdd,
                            color = HeroColor.Primary,
                            variant = HeroButtonVariant.Solid,
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
                    HeroSegmentedTabs(
                        modifier = Modifier.weight(1f, fill = false),
                        items = listOf(
                            "Suppliers (${suppliers.size})",
                            "Purchase Orders (${purchaseOrders.size})"
                        ),
                        selectedItem = if (subTab == 0) "Suppliers (${suppliers.size})" else "Purchase Orders (${purchaseOrders.size})",
                        onItemSelected = { selected ->
                            subTab = if (selected.startsWith("Suppliers")) 0 else 1
                        },
                        labelProvider = { it },
                        iconProvider = { if (it.startsWith("Suppliers")) Icons.Default.Business else Icons.Default.ShoppingCart }
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    if (subTab == 0) {
                        HeroButton(
                            onClick = {
                                editingSupplier = null
                                showSupplierDialog = true
                            },
                            modifier = Modifier.testTag("add_supplier_btn"),
                            text = "Add Supplier / Distributor",
                            icon = Icons.Default.Add,
                            color = HeroColor.Primary,
                            variant = HeroButtonVariant.Solid,
                            size = HeroSize.Md
                        )
                    } else {
                        HeroButton(
                            onClick = {
                                if (suppliers.isEmpty()) {
                                    Toast.makeText(context, "Please add at least one supplier first.", Toast.LENGTH_SHORT).show()
                                } else {
                                    showCreatePoDialog = true
                                }
                            },
                            modifier = Modifier.testTag("create_po_btn"),
                            text = "Create Purchase Order",
                            icon = Icons.Default.NoteAdd,
                            color = HeroColor.Primary,
                            variant = HeroButtonVariant.Solid,
                            size = HeroSize.Md
                        )
                    }
                }
            }
        }

        if (subTab == 0) {
            // Suppliers Directory Search & List
            HeroInput(
                value = supplierSearchQuery,
                onValueChange = { supplierSearchQuery = it },
                label = { Text("Search Vendors, Distributors & Agents") },
                placeholder = { Text("Filter by supplier name, phone, delivery day, terms...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (supplierSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { supplierSearchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("supplier_search_input"),
                singleLine = true
            )

            val q = supplierSearchQuery.trim().lowercase()
            val filteredSuppliers = suppliers.filter { s ->
                q.isEmpty() ||
                s.name.lowercase().contains(q) ||
                s.contactPerson.lowercase().contains(q) ||
                s.phone.lowercase().contains(q) ||
                s.deliverySchedule.lowercase().contains(q) ||
                s.paymentTerms.lowercase().contains(q) ||
                s.notes.lowercase().contains(q)
            }

            if (filteredSuppliers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(64.dp))
                        Text(if (suppliers.isEmpty()) "No suppliers registered yet." else "No matching suppliers found.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                        Text("Add your direct distributors, market stalls, and beverage delivery agents.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredSuppliers) { sup ->
                        HeroCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .expressiveCard(cornerRadius = 24.dp)
                                .tactileBounce(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            sup.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (sup.deliverySchedule.isNotBlank()) {
                                            ExpressivePillBadge(
                                                text = sup.deliverySchedule,
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (sup.contactPerson.isNotBlank()) {
                                            Text(
                                                "Agent: ${sup.contactPerson}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        if (sup.phone.isNotBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clickable {
                                                        try {
                                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${sup.phone}"))
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "Call: ${sup.phone}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), CircleShape)
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.Phone, contentDescription = "Dial", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(sup.phone, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                            }
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (sup.paymentTerms.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.tertiaryContainer
                                            ) {
                                                Text(
                                                    "Terms: ${sup.paymentTerms}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        if (sup.notes.isNotBlank()) {
                                            Text(
                                                sup.notes,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            editingSupplier = sup
                                            showSupplierDialog = true
                                        },
                                        modifier = Modifier.testTag("edit_supplier_${sup.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Supplier")
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteSupplier(sup.id) {
                                                Toast.makeText(context, "Supplier removed.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.testTag("delete_supplier_${sup.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Supplier", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Purchase Orders List with Status Filters (Pending, Received, Canceled)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("All", "Pending", "Received", "Canceled").forEach { filter ->
                    val isSelected = poStatusFilter.equals(filter, ignoreCase = true)
                    HeroChip(
                        text = filter,
                        variant = if (isSelected) HeroChipVariant.Solid else HeroChipVariant.Flat,
                        color = if (isSelected) HeroColor.Primary else HeroColor.Default,
                        onClick = { poStatusFilter = filter },
                        modifier = Modifier.testTag("po_filter_$filter")
                    )
                }
            }

            val filteredPOs = purchaseOrders.filter { po ->
                when (poStatusFilter) {
                    "Pending" -> po.status.equals("PENDING", ignoreCase = true) || po.status.equals("ORDERED", ignoreCase = true)
                    "Received" -> po.status.equals("RECEIVED", ignoreCase = true)
                    "Canceled" -> po.status.equals("CANCELED", ignoreCase = true) || po.status.equals("CANCELLED", ignoreCase = true)
                    else -> true
                }
            }

            if (filteredPOs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(64.dp))
                        Text(if (purchaseOrders.isEmpty()) "No purchase orders created yet." else "No POs match '$poStatusFilter'.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                        Text("Create purchase orders to track expected deliveries and lock in accurate COGS.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredPOs) { po ->
                        val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(po.orderDate))
                        val isPending = po.status.equals("PENDING", ignoreCase = true) || po.status.equals("ORDERED", ignoreCase = true)
                        val isReceived = po.status.equals("RECEIVED", ignoreCase = true)
                        val isCanceled = po.status.equals("CANCELED", ignoreCase = true) || po.status.equals("CANCELLED", ignoreCase = true)

                        HeroCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .expressiveCard(cornerRadius = 24.dp)
                                .tactileBounce(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPending) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(po.poNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1, softWrap = false)
                                        HeroChip(
                                            text = if (isPending) "PENDING" else if (isReceived) "RECEIVED" else "CANCELED",
                                            variant = HeroChipVariant.Flat,
                                            color = when {
                                                isReceived -> HeroColor.Primary
                                                isPending -> HeroColor.Warning
                                                else -> HeroColor.Danger
                                            }
                                        )
                                    }

                                    Text(
                                        "$curr${String.format(Locale.getDefault(), "%.2f", po.totalCost)}",
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Supplier: ${po.supplierName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                                    Text("Ordered: $dateStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, softWrap = false)
                                }

                                if (po.expectedDeliveryDate != null) {
                                    val expStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(po.expectedDeliveryDate))
                                    Text("Expected Delivery: $expStr", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, maxLines = 1, softWrap = false)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HeroChip(
                                        text = "Payment: ${po.paymentStatus}",
                                        variant = HeroChipVariant.Flat,
                                        color = if (po.paymentStatus == "PAID") HeroColor.Success else HeroColor.Danger
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        if (isPending) {
                                            HeroButton(
                                                onClick = { receivingPo = po },
                                                modifier = Modifier.testTag("receive_po_btn_${po.id}"),
                                                text = "Receive Shipment",
                                                icon = Icons.Default.Check,
                                                color = HeroColor.Primary,
                                                variant = HeroButtonVariant.Solid,
                                                size = HeroSize.Sm
                                            )

                                            HeroButton(
                                                onClick = { poToCancel = po },
                                                modifier = Modifier.testTag("cancel_po_btn_${po.id}"),
                                                text = "Cancel PO",
                                                color = HeroColor.Danger,
                                                variant = HeroButtonVariant.Bordered,
                                                size = HeroSize.Sm
                                            )
                                        }

                                        if (isReceived && po.paymentStatus == "UNPAID") {
                                            HeroTextButton(
                                                onClick = {
                                                    // Quick schedule bill for this PO in calendar
                                                    val dueDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis() + 86400000L * 7))
                                                    viewModel.addPaymentSchedule(
                                                        title = "${po.supplierName} (${po.poNumber})",
                                                        amount = po.totalCost,
                                                        dueDate = dueDateStr,
                                                        category = "Distributor Credit",
                                                        priority = "Medium",
                                                        paymentMethod = "CASH"
                                                    )
                                                    Toast.makeText(context, "Added to Payment Calendar (Due in 7 days).", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.testTag("schedule_bill_btn_${po.id}"),
                                                icon = Icons.Default.CalendarMonth,
                                                text = "Add to Calendar",
                                                size = HeroSize.Sm
                                            )
                                        }

                                        IconButton(onClick = {
                                            viewModel.deletePurchaseOrder(po.id) {
                                                Toast.makeText(context, "Purchase order deleted.", Toast.LENGTH_SHORT).show()
                                            }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete PO", tint = MaterialTheme.colorScheme.outline)
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

    // Add / Edit Supplier Dialog
    if (showSupplierDialog) {
        SupplierCrudDialog(
            editingSupplier = editingSupplier,
            viewModel = viewModel,
            onDismiss = { showSupplierDialog = false }
        )
    }

    // Cancel PO Confirmation Dialog
    if (poToCancel != null) {
        val po = poToCancel!!
        AlertDialog(
            onDismissRequest = { poToCancel = null },
            title = { Text("Cancel Purchase Order") },
            text = { Text("Are you sure you want to cancel purchase order ${po.poNumber} for ${po.supplierName}? This will mark it as CANCELED.") },
            confirmButton = {
                HeroButton(
                    onClick = {
                        viewModel.cancelPurchaseOrder(po.id) {
                            Toast.makeText(context, "PO ${po.poNumber} marked as Canceled.", Toast.LENGTH_SHORT).show()
                            poToCancel = null
                        }
                    },
                    modifier = Modifier.testTag("confirm_cancel_po_btn"),
                    text = "Cancel PO",
                    color = HeroColor.Danger,
                    variant = HeroButtonVariant.Solid,
                    size = HeroSize.Md
                )
            },
            dismissButton = {
                HeroTextButton(onClick = { poToCancel = null }, text = "Dismiss")
            }
        )
    }

    // Create Purchase Order Dialog
    if (showCreatePoDialog) {
        CreatePurchaseOrderDialog(
            suppliers = suppliers,
            products = products,
            curr = curr,
            viewModel = viewModel,
            onDismiss = { showCreatePoDialog = false }
        )
    }

    // Receive Shipment Dialog
    if (receivingPo != null) {
        ReceivePurchaseOrderDialog(
            po = receivingPo!!,
            curr = curr,
            viewModel = viewModel,
            onDismiss = { receivingPo = null }
        )
    }
}
