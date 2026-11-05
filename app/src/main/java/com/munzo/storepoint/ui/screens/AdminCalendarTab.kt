package com.munzo.storepoint.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.munzo.storepoint.ui.StorePointViewModel
import com.munzo.storepoint.ui.theme.HeroUITheme
import com.munzo.storepoint.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarTab(viewModel: StorePointViewModel) {
    val items by viewModel.allPaymentSchedules.collectAsState()
    val config by viewModel.storeConfig.collectAsState()
    val curr = config?.currencySymbol ?: "₱"

    val todayCalendar = java.util.Calendar.getInstance()
    val currentYear = todayCalendar.get(java.util.Calendar.YEAR)
    val currentMonth = todayCalendar.get(java.util.Calendar.MONTH) + 1
    val currentDay = todayCalendar.get(java.util.Calendar.DAY_OF_MONTH)
    val todayDateStr = String.format("%04d-%02d-%02d", currentYear, currentMonth, currentDay)

    // Calendar state
    var selectedYear by remember { mutableStateOf(currentYear) }
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var selectedDay by remember { mutableStateOf<Int?>(currentDay) }
    var statusFilter by remember { mutableStateOf("All") } // "All", "Unpaid", "Paid"
    var searchQuery by remember { mutableStateOf("") }

    // Dialog state
    var showAddDialog by remember { mutableStateOf(false) }

    // Forms
    var newTitle by remember { mutableStateOf("") }
    var newAmount by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("Distributor Credit") }
    var newPriority by remember { mutableStateOf("Medium") } // "Low", "Medium", "Critical"
    var newPayMethod by remember { mutableStateOf("CASH") }

    // Date computation
    val cal = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.YEAR, selectedYear)
        set(java.util.Calendar.MONTH, selectedMonth - 1)
        set(java.util.Calendar.DAY_OF_MONTH, 1)
    }
    val firstDayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday...
    val maxDays = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

    val monthName = when(selectedMonth) {
        1 -> "January"; 2 -> "February"; 3 -> "March"; 4 -> "April"; 5 -> "May"; 6 -> "June"
        7 -> "July"; 8 -> "August"; 9 -> "September"; 10 -> "October"; 11 -> "November"; else -> "December"
    }

    Column(modifier = Modifier.fillMaxSize().padding(2.dp)) {
        // Top action row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Store Payables & Bill Calendar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Schedule Rent, Meralco/Water Overheads & Distributor Credit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            HeroButton(
                onClick = { showAddDialog = true },
                text = "Add Bill",
                icon = Icons.Default.Add,
                modifier = Modifier.testTag("add_schedule_btn")
            )
        }

        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isTablet = configuration.screenWidthDp >= 600

        @Composable
        fun CalendarGridCard(modifier: Modifier) {
            HeroCard(
                modifier = modifier,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Month controller
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (selectedMonth == 1) {
                                selectedMonth = 12
                                selectedYear--
                            } else {
                                selectedMonth--
                            }
                        }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Prev")
                        }
                        
                        Text(
                            text = "$monthName $selectedYear",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(onClick = {
                            if (selectedMonth == 12) {
                                selectedMonth = 1
                                selectedYear++
                            } else {
                                selectedMonth++
                            }
                        }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Days label (Sun, Mon, Tue...)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
                        dayLabels.forEach { label ->
                            Text(
                                text = label,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Calendar Grid Layout
                    val days = mutableListOf<Int?>()
                    for (i in 1 until firstDayOfWeek) {
                        days.add(null)
                    }
                    for (i in 1..maxDays) {
                        days.add(i)
                    }
                    while (days.size % 7 != 0) {
                        days.add(null)
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(days.size) { index ->
                            val day = days[index]
                            if (day != null) {
                                val dateStr = String.format("%04d-%02d-%02d", selectedYear, selectedMonth, day)
                                val hasPaymentDue = items.any { it.dueDate == dateStr && !it.isPaid }
                                val hasCriticalDue = items.any { it.dueDate == dateStr && !it.isPaid && (it.priority.equals("Critical", ignoreCase = true) || it.priority.equals("High", ignoreCase = true)) }
                                val hasPaidPayment = items.any { it.dueDate == dateStr && it.isPaid }
                                val isSelected = selectedDay == day

                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.primaryContainer
                                                hasCriticalDue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                                                hasPaymentDue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                                                hasPaidPayment -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else if (hasPaymentDue) 1.5.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else if (hasCriticalDue) MaterialTheme.colorScheme.error else if (hasPaymentDue) MaterialTheme.colorScheme.error.copy(alpha = 0.7f) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedDay = day },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = day.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected || hasPaymentDue) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                                hasCriticalDue -> MaterialTheme.colorScheme.error
                                                hasPaymentDue -> MaterialTheme.colorScheme.error
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        if (hasPaymentDue) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(if (hasCriticalDue) Color.Red else Color(0xFFF59E0B))
                                            )
                                        }
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.aspectRatio(1f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Indicator Legends", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.Red))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Unpaid Due", style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF10B981)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Settled/Paid", style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Active Focus", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        @Composable
        fun ScheduleListCard(modifier: Modifier) {
            val filterDateStr = selectedDay?.let { String.format("%04d-%02d-%02d", selectedYear, selectedMonth, it) }
            val q = searchQuery.trim().lowercase()

            val filteredItems = items.filter { item ->
                val dateMatch = filterDateStr == null || item.dueDate == filterDateStr
                val statusMatch = when (statusFilter) {
                    "Unpaid" -> !item.isPaid
                    "Paid" -> item.isPaid
                    else -> true
                }
                val queryMatch = q.isEmpty() || item.title.lowercase().contains(q) || item.category.lowercase().contains(q)
                dateMatch && statusMatch && queryMatch
            }

            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedDay != null) "Schedules for $monthName $selectedDay, $selectedYear" else "All Scheduled Store Overheads",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (selectedDay != null) {
                        TextButton(onClick = { selectedDay = null }) {
                            Text("Show All Dates", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Filter chips: All, Unpaid, Paid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("All", "Unpaid", "Paid").forEach { statusOption ->
                        FilterChip(
                            selected = statusFilter == statusOption,
                            onClick = { statusFilter = statusOption },
                            label = { Text(statusOption) }
                        )
                    }

                    val unpaidCount = items.count { !it.isPaid }
                    if (unpaidCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                text = "$unpaidCount Unpaid",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.EventNote, "Empty", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No Scheduled Payments Registered", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            Text("Tap 'Add Bill' at top right to log upcoming rent, utilities, or credit.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredItems) { item ->
                            val isPastDue = !item.isPaid && item.dueDate < todayDateStr
                            val isCritical = item.priority.equals("Critical", ignoreCase = true) || item.priority.equals("High", ignoreCase = true)
                            
                            HeroCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (item.isPaid) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    width = if (isCritical && !item.isPaid) 1.5.dp else 1.dp,
                                    color = when {
                                        isPastDue -> MaterialTheme.colorScheme.error
                                        isCritical && !item.isPaid -> Color(0xFFEF4444)
                                        else -> MaterialTheme.colorScheme.outlineVariant
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (item.isPaid) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                                            )
                                            
                                            val (badgeBg, badgeText) = when {
                                                item.priority.equals("Critical", ignoreCase = true) || item.priority.equals("High", ignoreCase = true) ->
                                                    Color(0xFFFEE2E2) to Color(0xFFDC2626)
                                                item.priority.equals("Medium", ignoreCase = true) ->
                                                    Color(0xFFFEF3C7) to Color(0xFFD97706)
                                                else ->
                                                    Color(0xFFF1F5F9) to Color(0xFF475569)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(badgeBg)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    item.priority.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = badgeText,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            if (isPastDue) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(MaterialTheme.colorScheme.errorContainer)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        "OVERDUE",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.error,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        Text(
                                            text = "Category: ${item.category} • Method: ${item.paymentMethod}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )

                                        Text(
                                            text = "Due Date: ${item.dueDate}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isPastDue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "$curr${String.format(Locale.getDefault(), "%.2f", item.amount)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (item.isPaid) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = { viewModel.togglePaymentScheduleStatus(item) },
                                                modifier = Modifier.size(36.dp).testTag("toggle_bill_paid_${item.id}")
                                            ) {
                                                Icon(
                                                    imageVector = if (item.isPaid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                    contentDescription = "Toggle Paid",
                                                    tint = if (item.isPaid) Color(0xFF10B981) else MaterialTheme.colorScheme.outline
                                                )
                                            }

                                            IconButton(
                                                onClick = { viewModel.deletePaymentSchedule(item) },
                                                modifier = Modifier.size(36.dp).testTag("delete_bill_${item.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Schedule",
                                                    tint = MaterialTheme.colorScheme.error
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

        if (isTablet) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CalendarGridCard(modifier = Modifier.weight(1.2f).fillMaxHeight())
                ScheduleListCard(modifier = Modifier.weight(1.5f).fillMaxHeight())
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CalendarGridCard(modifier = Modifier.fillMaxWidth().wrapContentHeight())
                ScheduleListCard(modifier = Modifier.weight(1f).fillMaxWidth())
            }
        }
    }

    // Add Payment Schedule Dialog Modal
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Schedule Store Bill / Overhead", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroInput(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Bill Description / Vendor Name *") },
                        placeholder = { Text("e.g. Meralco Electric, Water Bill, Store Rent") },
                        modifier = Modifier.fillMaxWidth().testTag("bill_title_input"),
                        singleLine = true
                    )

                    HeroInput(
                        value = newAmount,
                        onValueChange = { newAmount = it },
                        label = { Text("Amount Due ($curr) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("bill_amount_input"),
                        singleLine = true
                    )

                    Text("Urgency Level / Priority:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Low", "Medium", "Critical").forEach { item ->
                            val isSelected = newPriority == item
                            FilterChip(
                                selected = isSelected,
                                onClick = { newPriority = item },
                                label = { Text(item) },
                                modifier = Modifier.testTag("priority_chip_$item")
                            )
                        }
                    }

                    Text("Overhead Category:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "Rent",
                                "Electricity / Meralco",
                                "Water",
                                "Distributor Credit",
                                "Salaries",
                                "Taxes / Licenses",
                                "Internet / Telco",
                                "Other Overheads"
                            ).forEach { item ->
                                val isSelected = newCategory == item
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { newCategory = item },
                                    label = { Text(item) },
                                    modifier = Modifier.testTag("category_chip_$item")
                                )
                            }
                        }
                    }

                    Text("Fund Source (Outflow Deduct):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("CASH" to "Cash Register Drawer", "GCASH" to "Store GCash Account").forEach { (code, label) ->
                            val isSelected = newPayMethod == code
                            FilterChip(
                                selected = isSelected,
                                onClick = { newPayMethod = code },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                HeroButton(
                    onClick = {
                        val amt = newAmount.toDoubleOrNull() ?: 0.0
                        if (newTitle.isNotBlank() && amt > 0) {
                            val activeDay = selectedDay ?: 1
                            val dateStr = String.format("%04d-%02d-%02d", selectedYear, selectedMonth, activeDay)
                            viewModel.addPaymentSchedule(
                                title = newTitle.trim(),
                                amount = amt,
                                dueDate = dateStr,
                                category = newCategory,
                                priority = newPriority,
                                paymentMethod = newPayMethod
                            )
                            showAddDialog = false
                            newTitle = ""
                            newAmount = ""
                        }
                    },
                    text = "Schedule Bill",
                    modifier = Modifier.testTag("submit_bill_schedule_btn")
                )
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
