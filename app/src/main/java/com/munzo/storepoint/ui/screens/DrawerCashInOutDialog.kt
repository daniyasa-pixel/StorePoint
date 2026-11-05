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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.munzo.storepoint.data.DrawerTransaction
import com.munzo.storepoint.data.StorePointRepository
import com.munzo.storepoint.ui.StorePointViewModel
import com.munzo.storepoint.ui.theme.HeroUITheme
import com.munzo.storepoint.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerCashInOutDialog(
    viewModel: StorePointViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val storeConfig by viewModel.storeConfig.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val allDrawerTransactions by viewModel.allDrawerTransactions.collectAsState()

    val curr = storeConfig?.currencySymbol ?: "₱"
    val currentDrawerCash = viewModel.getCurrentDrawerCash()

    // Action mode: "CASH_IN" or "CASH_OUT"
    var selectedAction by remember { mutableStateOf("CASH_IN") }
    var amountText by remember { mutableStateOf("") }
    var reasonText by remember { mutableStateOf("") }
    var kickDrawer by remember { mutableStateOf(true) }
    var printSlip by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    val sessionDrawerTx = remember(allDrawerTransactions, activeSession) {
        val sId = activeSession?.id ?: 0
        allDrawerTransactions.filter { it.sessionId == sId }.sortedByDescending { it.timestamp }
    }

    val cashInPresets = listOf("Float Top-Up", "Change Fund Add", "Petty Cash Injection", "Owner Capital")
    val cashOutPresets = listOf("Store Supplies / Packaging", "Ice / Water Purchase", "Jeepney / Delivery Fare", "Store Utilities", "Staff Meals / Snacks", "Supplier COD Payout")

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val isCashOut = selectedAction == "CASH_OUT"
    val signedAmount = if (isCashOut) -parsedAmount else parsedAmount
    val projectedBalance = StorePointRepository.roundMoney(currentDrawerCash + signedAmount)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PointOfSale,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Cash Drawer Management",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Petty Cash Inflow & Operational Cash Disbursements",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HeroDivider(modifier = Modifier.padding(vertical = 12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Balance & Shift Session Info Banner
                    HeroCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Current Drawer Cash Balance",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$curr${String.format(Locale.US, "%.2f", currentDrawerCash)}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.testTag("current_drawer_cash_display")
                                )
                                val sessionInfo = activeSession?.let {
                                    val sdf = SimpleDateFormat("HH:mm", Locale.US)
                                    "Shift #${it.id} • Cashier: ${it.cashierUsername} • Started: ${sdf.format(Date(it.startTime))}"
                                } ?: "No Active Session"
                                Text(
                                    text = sessionInfo,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            HeroButton(
                                onClick = {
                                    viewModel.kickCashDrawer { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                variant = HeroButtonVariant.Bordered,
                                color = HeroColor.Default,
                                text = "Kick Drawer",
                                icon = Icons.Default.LockOpen,
                                modifier = Modifier.testTag("test_drawer_kick_btn")
                            )
                        }
                    }

                    // Action Type Selector (Cash In vs Cash Out)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = selectedAction == "CASH_IN",
                            onClick = { selectedAction = "CASH_IN" },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDownward, null, modifier = Modifier.size(16.dp), tint = Color(0xFF10B981))
                                    Text("Cash In (Float Top-Up / Insertion)", fontWeight = FontWeight.Bold)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_cash_in_chip"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFF047857)
                            )
                        )

                        FilterChip(
                            selected = selectedAction == "CASH_OUT",
                            onClick = { selectedAction = "CASH_OUT" },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                    Text("Cash Out (Disbursement / Expense)", fontWeight = FontWeight.Bold)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_cash_out_chip"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                selectedLabelColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }

                    // Amount Input & Quick Increment Buttons
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (selectedAction == "CASH_IN") "Amount to Insert into Drawer" else "Amount to Disburse from Drawer",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        HeroInput(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("Amount ($curr)") },
                            placeholder = { Text("0.00") },
                            leadingIcon = { Text(curr, fontWeight = FontWeight.Bold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("drawer_amount_input"),
                            singleLine = true
                        )

                        // Quick Increment Buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(50.0, 100.0, 200.0, 500.0, 1000.0).forEach { inc ->
                                AssistChip(
                                    onClick = {
                                        val cur = amountText.toDoubleOrNull() ?: 0.0
                                        amountText = String.format(Locale.US, "%.2f", cur + inc)
                                    },
                                    label = { Text("+$curr${inc.toInt()}") },
                                    modifier = Modifier.testTag("quick_add_${inc.toInt()}")
                                )
                            }
                            if (amountText.isNotBlank()) {
                                AssistChip(
                                    onClick = { amountText = "" },
                                    label = { Text("Clear") }
                                )
                            }
                        }
                    }

                    // Reason / Purpose Preset Chips & Custom Input
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Purpose / Category Preset",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        val currentPresets = if (selectedAction == "CASH_IN") cashInPresets else cashOutPresets
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currentPresets.forEach { preset ->
                                val isSelected = reasonText.equals(preset, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { reasonText = preset },
                                    label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        HeroInput(
                            value = reasonText,
                            onValueChange = { reasonText = it },
                            label = { Text("Detailed Reason / Explanation") },
                            placeholder = { Text(if (selectedAction == "CASH_IN") "e.g. Additional ₱20 coins from bank" else "e.g. 5x Bags of Tube Ice for beverage cooler") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("drawer_reason_input"),
                            maxLines = 2
                        )
                    }

                    // Projected Drawer Balance Preview
                    HeroCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Drawer Impact Summary", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Current Balance:", style = MaterialTheme.typography.bodySmall)
                                Text("$curr${String.format(Locale.US, "%.2f", currentDrawerCash)}", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val deltaSign = if (selectedAction == "CASH_IN") "+" else "-"
                                val deltaColor = if (selectedAction == "CASH_IN") Color(0xFF10B981) else MaterialTheme.colorScheme.error
                                Text(if (selectedAction == "CASH_IN") "(+) Cash Inflow:" else "(-) Cash Outflow:", style = MaterialTheme.typography.bodySmall)
                                Text("$deltaSign$curr${String.format(Locale.US, "%.2f", parsedAmount)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = deltaColor)
                            }
                            HeroDivider(thickness = 0.5.dp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Projected Drawer Total:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "$curr${String.format(Locale.US, "%.2f", projectedBalance)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (projectedBalance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.testTag("projected_drawer_balance_display")
                                )
                            }
                        }
                    }

                    // Execution Checkbox Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { kickDrawer = !kickDrawer }
                                .padding(4.dp)
                        ) {
                            HeroCheckbox(
                                checked = kickDrawer,
                                onCheckedChange = { kickDrawer = it },
                                modifier = Modifier.testTag("kick_drawer_checkbox")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kick Cash Drawer", style = MaterialTheme.typography.bodySmall)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { printSlip = !printSlip }
                                .padding(4.dp)
                        ) {
                            HeroCheckbox(
                                checked = printSlip,
                                onCheckedChange = { printSlip = it },
                                modifier = Modifier.testTag("print_slip_checkbox")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Print Thermal Audit Slip", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // Recent Shift Drawer Movements Table
                    if (sessionDrawerTx.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Recent Shift Drawer Movements (${sessionDrawerTx.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )

                            sessionDrawerTx.take(5).forEach { tx ->
                                HeroCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                val isTxIn = tx.amount > 0
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (isTxIn) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                                ) {
                                                    Text(
                                                        text = if (isTxIn) "CASH IN" else "CASH OUT",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isTxIn) Color(0xFF047857) else MaterialTheme.colorScheme.error,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Text(
                                                    text = sdf.format(Date(tx.timestamp)),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                            Text(
                                                text = tx.reason,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1
                                            )
                                        }

                                        Text(
                                            text = "${if (tx.amount >= 0) "+" else ""}$curr${String.format(Locale.US, "%.2f", tx.amount)}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (tx.amount >= 0) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Buttons
                HeroDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    HeroButton(
                        onClick = {
                            if (parsedAmount <= 0.0) {
                                Toast.makeText(context, "Please enter a valid amount greater than 0.", Toast.LENGTH_SHORT).show()
                                return@HeroButton
                            }
                            if (isCashOut && currentDrawerCash < parsedAmount) {
                                Toast.makeText(context, "Insufficient cash in drawer for this cash-out.", Toast.LENGTH_SHORT).show()
                                return@HeroButton
                            }
                            isSubmitting = true
                            viewModel.logCashInOutDrawerTransaction(
                                type = selectedAction,
                                amount = parsedAmount,
                                reason = reasonText,
                                kickDrawer = kickDrawer,
                                printSlip = printSlip
                            ) { success, msg ->
                                isSubmitting = false
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                if (success) {
                                    onDismiss()
                                }
                            }
                        },
                        enabled = !isSubmitting && parsedAmount > 0.0 && (!isCashOut || currentDrawerCash >= parsedAmount),
                        color = if (selectedAction == "CASH_IN") HeroColor.Success else HeroColor.Danger,
                        text = if (selectedAction == "CASH_IN") "Record Cash In" else "Disburse Cash Out",
                        icon = if (selectedAction == "CASH_IN") Icons.Default.AddCircle else Icons.Default.RemoveCircle,
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("confirm_drawer_tx_btn")
                    )
                }
            }
        }
    }
}
