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
                                            TextButton(
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
                                                modifier = Modifier.testTag("schedule_bill_btn_${po.id}")
                                            ) {
                                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Add to Calendar", style = MaterialTheme.typography.labelSmall)
                                            }
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
        var nameInput by remember { mutableStateOf(editingSupplier?.name ?: "") }
        var contactPersonInput by remember { mutableStateOf(editingSupplier?.contactPerson ?: "") }
        var phoneInput by remember { mutableStateOf(editingSupplier?.phone ?: "") }
        var deliveryScheduleInput by remember { mutableStateOf(editingSupplier?.deliverySchedule ?: "") }
        var paymentTermsInput by remember { mutableStateOf(editingSupplier?.paymentTerms ?: "COD") }
        var notesInput by remember { mutableStateOf(editingSupplier?.notes ?: "") }

        AlertDialog(
            onDismissRequest = { showSupplierDialog = false },
            title = { Text(if (editingSupplier == null) "Add Supplier / Distributor" else "Edit Supplier Details") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroInput(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Supplier / Distributor Name *") },
                        placeholder = { Text("e.g. San Miguel Dist., Coca-Cola Bottlers, Puregold Wholesale") },
                        modifier = Modifier.fillMaxWidth().testTag("supplier_name_field"),
                        singleLine = true
                    )

                    // Vendor Type Helper Chips
                    Text("Vendor / Distributor Classification:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Direct Distributor", "Delivery Agent", "Wholesale Vendor", "Local Producer").forEach { vType ->
                            AssistChip(
                                onClick = {
                                    if (!notesInput.contains(vType)) {
                                        notesInput = if (notesInput.isBlank()) vType else "$vType • $notesInput"
                                    }
                                },
                                label = { Text(vType, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    HeroInput(
                        value = contactPersonInput,
                        onValueChange = { contactPersonInput = it },
                        label = { Text("Contact Person / Delivery Agent") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    HeroInput(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Mobile / Contact Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Configurable Credit Payment Terms (COD, 7-day, 15-day, 30-day)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Configurable Credit Payment Terms:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("COD", "7-Day", "15-Day", "30-Day").forEach { term ->
                                val isSelected = paymentTermsInput.equals(term, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { paymentTermsInput = term },
                                    label = { Text(term) },
                                    modifier = Modifier.testTag("term_chip_$term")
                                )
                            }
                        }
                        HeroInput(
                            value = paymentTermsInput,
                            onValueChange = { paymentTermsInput = it },
                            label = { Text("Payment Terms (COD, 7-Day, 15-Day, 30-Day)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // Delivery Schedule with presets
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Delivery Schedule:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Daily", "Mondays & Thursdays", "Tuesdays & Fridays", "Wednesdays & Saturdays", "Weekly", "On-Call").forEach { sched ->
                                AssistChip(
                                    onClick = { deliveryScheduleInput = sched },
                                    label = { Text(sched, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        HeroInput(
                            value = deliveryScheduleInput,
                            onValueChange = { deliveryScheduleInput = it },
                            label = { Text("Delivery Schedule (e.g. Tuesdays & Fridays)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    HeroInput(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Agent Notes / Minimum Orders") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                HeroButton(
                    onClick = {
                        val sName = nameInput.trim()
                        if (sName.isBlank()) {
                            Toast.makeText(context, "Supplier name cannot be empty.", Toast.LENGTH_SHORT).show()
                            return@HeroButton
                        }
                        val supplier = Supplier(
                            id = editingSupplier?.id ?: 0,
                            name = sName,
                            contactPerson = contactPersonInput.trim(),
                            phone = phoneInput.trim(),
                            deliverySchedule = deliveryScheduleInput.trim(),
                            paymentTerms = paymentTermsInput.trim().ifBlank { "COD" },
                            notes = notesInput.trim()
                        )
                        viewModel.saveSupplier(supplier) {
                            Toast.makeText(context, "Supplier saved successfully.", Toast.LENGTH_SHORT).show()
                            showSupplierDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_supplier_submit_btn"),
                    text = "Save Supplier",
                    color = HeroColor.Primary,
                    variant = HeroButtonVariant.Solid,
                    size = HeroSize.Md
                )
            },
            dismissButton = {
                TextButton(onClick = { showSupplierDialog = false }) {
                    Text("Cancel")
                }
            }
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
                TextButton(onClick = { poToCancel = null }) { Text("Dismiss") }
            }
        )
    }

    // Create Purchase Order Dialog
    if (showCreatePoDialog) {
        var selectedSupplier by remember { mutableStateOf(suppliers.firstOrNull()) }
        var poNotes by remember { mutableStateOf("") }
        var expectedDeliveryDaysOffset by remember { mutableStateOf(3) }
        // Order items: productId -> Pair(quantity, unitCost)
        val orderItems = remember { mutableStateMapOf<Int, Pair<Int, Double>>() }

        Dialog(
            onDismissRequest = { showCreatePoDialog = false },
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
                                IconButton(onClick = { showCreatePoDialog = false }) {
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
                            OutlinedButton(
                                onClick = { supplierDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Supplier: ${selectedSupplier?.name ?: "Select Supplier"}", fontWeight = FontWeight.Bold)
                            }
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
                                    FilterChip(
                                        selected = expectedDeliveryDaysOffset == days,
                                        onClick = { expectedDeliveryDaysOffset = days },
                                        label = { Text(label) },
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
                                                            orderItems[prod.id] = Pair(10, if (prod.cost > 0) prod.cost else prod.price * 0.8)
                                                        } else {
                                                            orderItems.remove(prod.id)
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
                                                HeroInput(
                                                    value = currentQty.toString(),
                                                    onValueChange = {
                                                        val q = it.toIntOrNull() ?: 1
                                                        orderItems[prod.id] = Pair(q, currentCost)
                                                    },
                                                    label = { Text("Qty") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f).height(54.dp),
                                                    singleLine = true
                                                )
                                                HeroInput(
                                                    value = String.format(Locale.getDefault(), "%.2f", currentCost),
                                                    onValueChange = {
                                                        val c = it.toDoubleOrNull() ?: 0.0
                                                        orderItems[prod.id] = Pair(currentQty, c)
                                                    },
                                                    label = { Text("Unit Cost") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    modifier = Modifier.weight(1.2f).height(54.dp),
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
                                        showCreatePoDialog = false
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

    // Receive Shipment Dialog
    if (receivingPo != null) {
        val po = receivingPo!!
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
            onDismissRequest = { receivingPo = null },
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
                            FilterChip(
                                selected = paymentStatusInput == "PAID",
                                onClick = {
                                    paymentStatusInput = "PAID"
                                    scheduleBillInCalendar = false
                                },
                                label = { Text("PAID") }
                            )
                            FilterChip(
                                selected = paymentStatusInput == "UNPAID",
                                onClick = {
                                    paymentStatusInput = "UNPAID"
                                    scheduleBillInCalendar = true
                                },
                                label = { Text("UNPAID (Credit)") }
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
                                receivingPo = null
                            }
                        )
                    },
                    modifier = Modifier.testTag("confirm_receive_shipment_btn"),
                    text = "Confirm & Restock Stock",
                    color = HeroColor.Success,
                    variant = HeroButtonVariant.Solid,
                    size = HeroSize.Md
                )
            },
            dismissButton = {
                TextButton(onClick = { receivingPo = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
