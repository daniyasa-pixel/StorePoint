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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.munzo.storepoint.data.CashierSession
import com.munzo.storepoint.data.ShiftReconciliation
import com.munzo.storepoint.data.StoreConfig
import com.munzo.storepoint.data.StorePointRepository
import com.munzo.storepoint.ui.*
import com.munzo.storepoint.ui.components.ExpressiveButton
import com.munzo.storepoint.ui.components.ExpressiveButtonSize
import com.munzo.storepoint.ui.components.ExpressiveButtonVariant
import com.munzo.storepoint.util.SecurityHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftReconciliationDialog(
    session: CashierSession,
    viewModel: StorePointViewModel,
    expectedCash: Double,
    expectedGCash: Double,
    expectedSmartLoad: Double,
    expectedGlobeLoad: Double,
    onDismiss: () -> Unit,
    onShiftClosed: (ShiftReconciliation) -> Unit
) {
    val context = LocalContext.current
    val storeConfig by viewModel.storeConfig.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val curr = storeConfig?.currencySymbol ?: "$"

    // Cash Drawer Physical Denominations
    var count1000Str by remember { mutableStateOf("") }
    var count500Str by remember { mutableStateOf("") }
    var count200Str by remember { mutableStateOf("") }
    var count100Str by remember { mutableStateOf("") }
    var count50Str by remember { mutableStateOf("") }
    var count20Str by remember { mutableStateOf("") }
    var countCoinsStr by remember { mutableStateOf("") }

    val c1000 = count1000Str.toIntOrNull() ?: 0
    val c500 = count500Str.toIntOrNull() ?: 0
    val c200 = count200Str.toIntOrNull() ?: 0
    val c100 = count100Str.toIntOrNull() ?: 0
    val c50 = count50Str.toIntOrNull() ?: 0
    val c20 = count20Str.toIntOrNull() ?: 0
    val cCoins = countCoinsStr.toDoubleOrNull() ?: 0.0

    val totalCountedCash = remember(c1000, c500, c200, c100, c50, c20, cCoins) {
        StorePointRepository.roundMoney(
            (c1000 * 1000.0) +
            (c500 * 500.0) +
            (c200 * 200.0) +
            (c100 * 100.0) +
            (c50 * 50.0) +
            (c20 * 20.0) +
            cCoins
        )
    }

    // GCash & Prepaid Load Counts
    var gcashCountStr by remember { mutableStateOf(String.format(Locale.US, "%.2f", expectedGCash)) }
    var smartLoadCountStr by remember { mutableStateOf(String.format(Locale.US, "%.2f", expectedSmartLoad)) }
    var globeLoadCountStr by remember { mutableStateOf(String.format(Locale.US, "%.2f", expectedGlobeLoad)) }

    val countedGCash = gcashCountStr.toDoubleOrNull() ?: 0.0
    val countedSmart = smartLoadCountStr.toDoubleOrNull() ?: 0.0
    val countedGlobe = globeLoadCountStr.toDoubleOrNull() ?: 0.0

    // Variances: Variance = Counted - Expected
    val cashVariance = remember(totalCountedCash, expectedCash) {
        StorePointRepository.roundMoney(totalCountedCash - expectedCash)
    }
    val gcashVariance = remember(countedGCash, expectedGCash) {
        StorePointRepository.roundMoney(countedGCash - expectedGCash)
    }
    val smartVariance = remember(countedSmart, expectedSmartLoad) {
        StorePointRepository.roundMoney(countedSmart - expectedSmartLoad)
    }
    val globeVariance = remember(countedGlobe, expectedGlobeLoad) {
        StorePointRepository.roundMoney(countedGlobe - expectedGlobeLoad)
    }

    val hasCashDiscrepancy = Math.abs(cashVariance) >= 0.01
    val hasGcashDiscrepancy = storeConfig?.hasGCash == true && Math.abs(gcashVariance) >= 0.01
    val hasSmartDiscrepancy = storeConfig?.hasLoad == true && Math.abs(smartVariance) >= 0.01
    val hasGlobeDiscrepancy = storeConfig?.hasLoad == true && Math.abs(globeVariance) >= 0.01

    val hasAnyDiscrepancy = hasCashDiscrepancy || hasGcashDiscrepancy || hasSmartDiscrepancy || hasGlobeDiscrepancy

    // Manager Override & Audit Controls
    var managerUsername by remember { mutableStateOf("") }
    var managerPin by remember { mutableStateOf("") }
    var discrepancyNote by remember { mutableStateOf("") }
    var managerOverrideVerified by remember { mutableStateOf(false) }
    var overrideError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Completed Reconciliation for Receipt Printing
    var savedReconciliation by remember { mutableStateOf<ShiftReconciliation?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
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
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Dual Drawer & Shift Balancing Guard", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                IconButton(onClick = onDismiss, enabled = !isSubmitting) {
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
                    text = "Shift close is blocked until physical cash denominations and digital wallet ending balances are verified.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1) Cash Drawer Denominations Input
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("1. Cash Drawer Physical Count", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            Surface(
                                shape = CircleShape,
                                color = if (hasCashDiscrepancy) MaterialTheme.colorScheme.errorContainer else Color(0xFF10B981).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (hasCashDiscrepancy) "VARIANCE: $curr${String.format(Locale.US, "%.2f", cashVariance)}" else "BALANCED",
                                    color = if (hasCashDiscrepancy) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF10B981),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Grid of Denominations
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DenomField(label = "₱1,000", value = count1000Str, onValueChange = { count1000Str = it }, modifier = Modifier.weight(1f))
                            DenomField(label = "₱500", value = count500Str, onValueChange = { count500Str = it }, modifier = Modifier.weight(1f))
                            DenomField(label = "₱200", value = count200Str, onValueChange = { count200Str = it }, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DenomField(label = "₱100", value = count100Str, onValueChange = { count100Str = it }, modifier = Modifier.weight(1f))
                            DenomField(label = "₱50", value = count50Str, onValueChange = { count50Str = it }, modifier = Modifier.weight(1f))
                            DenomField(label = "₱20", value = count20Str, onValueChange = { count20Str = it }, modifier = Modifier.weight(1f))
                        }
                        OutlinedTextField(
                            value = countCoinsStr,
                            onValueChange = { countCoinsStr = it },
                            label = { Text("Coins & Loose Change (₱)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().testTag("count_coins_input"),
                            singleLine = true
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Counted Cash:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("$curr${String.format(Locale.US, "%.2f", totalCountedCash)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Expected Cash Ledger:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Text("$curr${String.format(Locale.US, "%.2f", expectedCash)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                // 2) GCash Digital Wallet Reconciliation
                if (storeConfig?.hasGCash == true) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("2. GCash App Ending Balance", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                Surface(
                                    shape = CircleShape,
                                    color = if (hasGcashDiscrepancy) MaterialTheme.colorScheme.errorContainer else Color(0xFF10B981).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (hasGcashDiscrepancy) "VAR: $curr${String.format(Locale.US, "%.2f", gcashVariance)}" else "MATCH",
                                        color = if (hasGcashDiscrepancy) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF10B981),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = gcashCountStr,
                                onValueChange = { gcashCountStr = it },
                                label = { Text("Physical GCash App Ending Balance ($curr)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth().testTag("count_gcash_input"),
                                singleLine = true
                            )
                            Text("Expected Ledger: $curr${String.format(Locale.US, "%.2f", expectedGCash)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                // 3) Mobile Load Reconciliation
                if (storeConfig?.hasLoad == true) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("3. Prepaid Load SIM Ending Balances", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = smartLoadCountStr,
                                        onValueChange = { smartLoadCountStr = it },
                                        label = { Text("Smart/TNT SIM") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.fillMaxWidth().testTag("count_smart_input"),
                                        singleLine = true
                                    )
                                    Text("Expected: ${String.format(Locale.US, "%.0f", expectedSmartLoad)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = globeLoadCountStr,
                                        onValueChange = { globeLoadCountStr = it },
                                        label = { Text("Globe/TM SIM") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.fillMaxWidth().testTag("count_globe_input"),
                                        singleLine = true
                                    )
                                    Text("Expected: ${String.format(Locale.US, "%.0f", expectedGlobeLoad)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }

                // 4) Discrepancy & Manager Override Guard
                if (hasAnyDiscrepancy) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Text("Reconciliation Variance Detected!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                            Text(
                                text = "A variance exists between counted totals and POS ledgers. A supervisor/manager PIN and mandatory explanatory notes are required to close this shift.",
                                style = MaterialTheme.typography.bodySmall
                            )

                            OutlinedTextField(
                                value = discrepancyNote,
                                onValueChange = { discrepancyNote = it },
                                label = { Text("Mandatory Discrepancy Reason *") },
                                modifier = Modifier.fillMaxWidth().testTag("discrepancy_reason_input"),
                                maxLines = 3
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = managerUsername,
                                    onValueChange = { managerUsername = it; overrideError = null },
                                    label = { Text("Manager User") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = managerPin,
                                    onValueChange = { managerPin = it; overrideError = null },
                                    label = { Text("PIN") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            overrideError?.let {
                                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                            }

                            if (!managerOverrideVerified) {
                                OutlinedButton(
                                    onClick = {
                                        val mgr = allUsers.firstOrNull {
                                            it.username.equals(managerUsername.trim(), ignoreCase = true) &&
                                                    it.role.equals("ADMIN", ignoreCase = true)
                                        }
                                        if (mgr != null && SecurityHelper.verifyPin(managerPin, mgr.pinHash).isMatch) {
                                            if (discrepancyNote.trim().length >= 4) {
                                                managerOverrideVerified = true
                                                overrideError = null
                                            } else {
                                                overrideError = "Please explain the reason for this variance (min 4 chars)."
                                            }
                                        } else {
                                            overrideError = "Invalid manager credentials. Only ADMIN accounts can override."
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("manager_override_btn")
                                ) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Verify Manager Override")
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                        Text("Manager Override Approved by @${managerUsername.trim()}", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                // Receipt Printing Button if already reconciled or available
                savedReconciliation?.let { recon ->
                    ExpressiveButton(
                        onClick = {
                            viewModel.printShiftReconciliationReceipt(recon, session.startTime) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        label = "Print Reconciliation Receipt",
                        icon = Icons.Default.Print,
                        variant = ExpressiveButtonVariant.OUTLINED,
                        size = ExpressiveButtonSize.M,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            val canClose = !hasAnyDiscrepancy || managerOverrideVerified
            ExpressiveButton(
                onClick = {
                    isSubmitting = true
                    viewModel.reconcileShiftAndClose(
                        count1000 = c1000,
                        count500 = c500,
                        count200 = c200,
                        count100 = c100,
                        count50 = c50,
                        count20 = c20,
                        countCoins = cCoins,
                        totalCountedCash = totalCountedCash,
                        expectedCash = expectedCash,
                        countedGCash = countedGCash,
                        expectedGCash = expectedGCash,
                        countedSmartLoad = countedSmart,
                        expectedSmartLoad = expectedSmartLoad,
                        countedGlobeLoad = countedGlobe,
                        expectedGlobeLoad = expectedGlobeLoad,
                        managerOverrideApproved = managerOverrideVerified,
                        managerUsername = if (managerOverrideVerified) managerUsername.trim() else "",
                        discrepancyReason = discrepancyNote.trim()
                    ) { success, msg, recon ->
                        isSubmitting = false
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        if (success && recon != null) {
                            savedReconciliation = recon
                            // Prompt to print receipt
                            viewModel.printShiftReconciliationReceipt(recon, session.startTime) { _, _ -> }
                            onShiftClosed(recon)
                        }
                    }
                },
                label = if (isSubmitting) "Reconciling..." else "Confirm & Close Shift",
                icon = Icons.Default.Lock,
                enabled = canClose && !isSubmitting,
                variant = ExpressiveButtonVariant.FILLED,
                size = ExpressiveButtonSize.M,
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                modifier = Modifier.testTag("reconciliation_confirm_btn")
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DenomField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { str ->
            if (str.all { it.isDigit() }) onValueChange(str)
        },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        singleLine = true
    )
}
