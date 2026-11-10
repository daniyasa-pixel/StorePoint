package com.munzo.storepoint.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.munzo.storepoint.KioskLockdownCapability
import com.munzo.storepoint.LockdownStrength
import com.munzo.storepoint.StorePointDeviceAdminReceiver
import com.munzo.storepoint.data.*
import com.munzo.storepoint.ui.StorePointViewModel
import com.munzo.storepoint.ui.theme.*
import com.munzo.storepoint.ui.components.*
import com.munzo.storepoint.util.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch



@Composable
fun AdminDatabaseMaintenanceSection(viewModel: StorePointViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dbHealth by viewModel.databaseHealth.collectAsState()
    var isCheckingDbHealth by remember { mutableStateOf(false) }

    // Admin Verification for Inventory Import (.spinventory & JSON)
    var showImportInventoryPinDialog by remember { mutableStateOf(false) }
    var pendingImportInventoryContent by remember { mutableStateOf("") }
    var importInventoryPinInput by remember { mutableStateOf("") }
    var importInventoryPinError by remember { mutableStateOf("") }

    // Backup & Restore states
    val backupSnapshots by viewModel.backupSnapshots.collectAsState()
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestoreJson by remember { mutableStateOf("") }
    var restoreAdminPinInput by remember { mutableStateOf("") }
    var restoreAdminPinError by remember { mutableStateOf("") }
    var restoreSummaryResult by remember { mutableStateOf<DatabaseBackupManager.RestoreSummary?>(null) }
    var showRestoreSummaryDialog by remember { mutableStateOf(false) }

    // M2: optional backup passphrase protection
    var showExportEncryptDialog by remember { mutableStateOf(false) }
    var backupPassphrase by remember { mutableStateOf("") }
    var backupPassphraseConfirm by remember { mutableStateOf("") }
    var backupPassphraseError by remember { mutableStateOf("") }
    var restorePassphrase by remember { mutableStateOf("") }

    // Wipe data states
    // Full DB Restore Launcher
    val fullRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val json = stream.bufferedReader().use { it.readText() }
                    pendingRestoreJson = json
                    showRestoreConfirmDialog = true
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Read error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Backup & Inventory Imports launcher (.spinventory, .json, */*)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val fileContents = stream.bufferedReader().use { it.readText() }
                    pendingImportInventoryContent = fileContents
                    importInventoryPinInput = ""
                    importInventoryPinError = ""
                    showImportInventoryPinDialog = true
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot read file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

        // --- DATABASE CONNECTION & PEAK PERFORMANCE ENGINE HEALTH ---
        HeroCard(
            modifier = Modifier.fillMaxWidth().testTag("db_connection_health_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (dbHealth?.isConnected == true) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (dbHealth?.isConnected == true) Icons.Default.Storage else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (dbHealth?.isConnected == true) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Database Engine & Connection Health",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "SQLite WAL Engine • Turbo Memory (128MB Cache, 256MB MMAP)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val badgeText = if (dbHealth == null) "CHECKING..." else if (dbHealth?.isConnected == true) "ACTIVE & PEAK" else "NO CONNECTIONS"
                    val badgeColor = if (dbHealth?.isConnected == true) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                    val contentBadgeColor = if (dbHealth?.isConnected == true) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    ExpressiveMorphingBadge(
                        text = badgeText,
                        containerColor = badgeColor,
                        contentColor = contentBadgeColor,
                        icon = if (dbHealth?.isConnected == true) Icons.Default.CheckCircle else Icons.Default.Error
                    )
                }

                if (isCheckingDbHealth) {
                    ExpressiveSquiggleLinearProgress(
                        color = MaterialTheme.colorScheme.primary,
                        amplitude = 3.dp,
                        wavelength = 20.dp
                    )
                }

                dbHealth?.let { health ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Latency", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text("${health.latencyMs} ms", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Write Mode", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(if (health.isWritable) "Read / Write" else "Read Only", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Integrity", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(if (health.integrityOk) "Passed (OK)" else "Corrupted", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = if (health.integrityOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Text(
                        text = health.message,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                HeroFilledTonalButton(
                    onClick = {
                        isCheckingDbHealth = true
                        viewModel.checkDatabaseHealth()
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            isCheckingDbHealth = false
                            Toast.makeText(context, "Database connection verified: Peak throughput active", Toast.LENGTH_SHORT).show()
                        }, 500)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("audit_db_connections_btn"),
                    icon = Icons.Default.Speed,
                    text = "Verify Database Connections & Performance"
                )
            }
        }


        // --- FULL DATABASE BACKUP & DISASTER RECOVERY ---
        HeroCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = "Full Disaster Recovery",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "Database Disaster Recovery & Backup",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Complete database snapshot with SHA-256 integrity verification.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Text(
                    text = "Export the entire operational state: Store configuration, user credentials, categories, product catalog, sales receipts, drawer shifts, cash payouts, and payment schedules.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Automatic Transaction Safeguard: A full database backup is silently saved to device internal storage (Documents/StorePoint/Backups/) after every transaction. These backups survive app uninstallation and data clearing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                if (backupSnapshots.isNotEmpty()) {
                    val latest = backupSnapshots.first()
                    Text(
                        text = "Latest persistent vault: ${latest.name} (${latest.length() / 1024} KB)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroButton(
                        modifier = Modifier.weight(1f).testTag("export_full_db_btn"),
                        onClick = {
                            backupPassphrase = ""
                            backupPassphraseConfirm = ""
                            backupPassphraseError = ""
                            showExportEncryptDialog = true
                        },
                        text = "Export Full DB",
                        icon = Icons.Default.Upload
                    )

                    HeroButton(
                        modifier = Modifier.weight(1f).testTag("restore_full_db_btn"),
                        onClick = {
                            fullRestoreLauncher.launch("application/json")
                        },
                        color = HeroColor.Default,
                        text = "Restore Full DB",
                        icon = Icons.Default.Download
                    )
                }

                // Dedicated Section: Store Inventory Data Transfer & Quick Share (.spinventory / .json)
                HeroDivider(modifier = Modifier.padding(vertical = 4.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Import Inventory Data:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text("Receive Quick Share packages (.spinventory) or catalog backups.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            HeroTextButton(
                                onClick = { viewModel.shareInventoryViaQuickShare(context) },
                                size = HeroSize.Sm,
                                icon = Icons.Default.Share,
                                text = "Quick Share"
                            )
                            HeroButton(
                                onClick = { importLauncher.launch("*/*") },
                                size = HeroSize.Sm,
                                text = "Import Inventory",
                                icon = Icons.Default.Download,
                                modifier = Modifier.testTag("import_inventory_data_btn")
                            )
                        }
                    }
                }
            }
        }


        AdminDataWipeSection(viewModel = viewModel)
    // --- SECURE INVENTORY IMPORT ADMIN PIN VERIFICATION DIALOG ---
    if (showImportInventoryPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showImportInventoryPinDialog = false
                importInventoryPinInput = ""
                importInventoryPinError = ""
                pendingImportInventoryContent = ""
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Admin Import Certification", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "You are importing inventory data (.spinventory package / catalog). Enter your Admin PIN (${SecurityHelper.PIN_LENGTH} digits) to certify and merge this into the active store catalog:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    // Keypad, not the IME: this certifies an import that overwrites the
                    // live catalogue.
                    PinPadEntry(
                        pin = importInventoryPinInput,
                        onPinChange = {
                            importInventoryPinInput = it
                            importInventoryPinError = ""
                        },
                        isMasked = true,
                        isError = importInventoryPinError.isNotEmpty(),
                        errorMessage = importInventoryPinError.ifEmpty { null },
                        modifier = Modifier.fillMaxWidth().testTag("import_inventory_admin_pin_field")
                    )
                }
            },
            confirmButton = {
                HeroButton(
                    onClick = {
                        if (SecurityHelper.isValidPinLenient(importInventoryPinInput)) {
                            coroutineScope.launch {
                                val isAuth = viewModel.verifyAdminPin(importInventoryPinInput)
                                if (isAuth) {
                                    viewModel.importInventoryPackageDetailed(
                                        rawContent = pendingImportInventoryContent,
                                        onSuccess = { catCount, prodCount ->
                                            showImportInventoryPinDialog = false
                                            importInventoryPinInput = ""
                                            pendingImportInventoryContent = ""
                                            Toast.makeText(
                                                context,
                                                "Inventory import successful! Merged $catCount categories and $prodCount products.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        },
                                        onFailure = { err ->
                                            importInventoryPinError = "Import failed: $err"
                                        }
                                    )
                                } else {
                                    importInventoryPinError = "Incorrect Admin PIN. Certification failed."
                                }
                            }
                        }
                    },
                    enabled = SecurityHelper.isValidPinLenient(importInventoryPinInput),
                    modifier = Modifier.testTag("confirm_import_inventory_btn"),
                    text = "Certify & Import"
                )
            },
            dismissButton = {
                HeroTextButton(
                    onClick = {
                        showImportInventoryPinDialog = false
                        importInventoryPinInput = ""
                        importInventoryPinError = ""
                        pendingImportInventoryContent = ""
                    },
                    text = "Cancel"
                )
            }
        )
    }


    // --- BACKUP ENCRYPTION EXPORT DIALOG (M2) ---
    if (showExportEncryptDialog) {
        AlertDialog(
            onDismissRequest = { showExportEncryptDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Database Backup", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Optionally protect this backup with a password (AES-256 + HMAC-SHA256). Leave the fields empty to export an unprotected file. Do not forget the password — it cannot be recovered.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    HeroInput(
                        value = backupPassphrase,
                        onValueChange = { backupPassphrase = it },
                        label = { Text("Backup password (optional)") },
                        modifier = Modifier.fillMaxWidth().testTag("backup_passphrase_input"),
                        singleLine = true
                    )
                    HeroInput(
                        value = backupPassphraseConfirm,
                        onValueChange = { backupPassphraseConfirm = it },
                        label = { Text("Confirm backup password") },
                        modifier = Modifier.fillMaxWidth().testTag("backup_passphrase_confirm_input"),
                        singleLine = true
                    )
                    if (backupPassphraseError.isNotEmpty()) {
                        Text(backupPassphraseError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                HeroButton(
                    modifier = Modifier.testTag("confirm_export_backup_btn"),
                    onClick = {
                        if (backupPassphrase != backupPassphraseConfirm) {
                            backupPassphraseError = "Passwords do not match."
                            return@HeroButton
                        }
                        showExportEncryptDialog = false
                        viewModel.exportDatabaseBackup(backupPassphrase) { file ->
                            if (file != null) {
                                try {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Database Backup"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "Failed to create database snapshot.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    text = "Export"
                )
            },
            dismissButton = {
                HeroTextButton(
                    onClick = {
                        showExportEncryptDialog = false
                    },
                    text = "Cancel"
                )
            }
        )
    }

    // --- FULL DATABASE RESTORE CONFIRMATION DIALOG ---
    if (showRestoreConfirmDialog) {
        AlertDialog(
                                                onDismissRequest = {
                showRestoreConfirmDialog = false
                restoreAdminPinInput = ""
                restoreAdminPinError = ""
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm Full Database Restore", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Restoring a database snapshot will overwrite existing local data with the contents of the backup file. This operation updates store configurations, user accounts, product catalogs, transactions, and shifts.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "An automated safety snapshot of the current state will be created before applying this restore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                                        HeroInput(
                        value = restorePassphrase,
                        onValueChange = { restorePassphrase = it },
                        label = { Text("Backup password (required only for password-protected backups)") },
                        modifier = Modifier.fillMaxWidth().testTag("restore_passphrase_input"),
                        singleLine = true
                    )
                    Text(
                        "Full restore overwrites store data. Enter your Admin PIN (${SecurityHelper.PIN_LENGTH} digits) to authorize:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    HeroInput(
                        value = restoreAdminPinInput,
                        onValueChange = {
                            if (it.length <= SecurityHelper.PIN_LENGTH && it.all { c -> c.isDigit() }) {
                                restoreAdminPinInput = it
                                restoreAdminPinError = ""
                            }
                        },
                        label = { Text("Admin PIN (${SecurityHelper.PIN_LENGTH} digits)") },
                        isError = restoreAdminPinError.isNotEmpty(),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("restore_admin_pin_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
                        confirmButton = {
                HeroButton(
                    enabled = SecurityHelper.isValidPinLenient(restoreAdminPinInput),
                    modifier = Modifier.testTag("confirm_full_restore_btn"),
                    onClick = {
                        coroutineScope.launch {
                            if (viewModel.verifyAdminPin(restoreAdminPinInput)) {
                                showRestoreConfirmDialog = false
                                restoreAdminPinInput = ""
                                restoreAdminPinError = ""
                                viewModel.restoreDatabaseBackup(pendingRestoreJson, restorePassphrase) { summary ->
                                    restoreSummaryResult = summary
                                    showRestoreSummaryDialog = true
                                }
                            } else {
                                restoreAdminPinError = "Incorrect Admin PIN. Restore not authorized."
                            }
                        }
                    },
                    color = HeroColor.Danger,
                    text = "Proceed with Full Restore"
                )
            },
            dismissButton = {
                HeroTextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        restoreAdminPinInput = ""
                        restoreAdminPinError = ""
                    },
                    text = "Cancel"
                )
            }
        )
    }

    // --- FULL DATABASE RESTORE SUMMARY MODAL ---
    if (showRestoreSummaryDialog && restoreSummaryResult != null) {
        val summary = restoreSummaryResult!!
        AlertDialog(
            onDismissRequest = { showRestoreSummaryDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (summary.success) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (summary.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (summary.success) "Restore Completed" else "Restore Failed", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(summary.message, fontWeight = FontWeight.Medium)
                    if (summary.success) {
                        HeroDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("• Staff Users Restored: ${summary.usersCount}", style = MaterialTheme.typography.bodySmall)
                        Text("• Products Restored: ${summary.productsCount}", style = MaterialTheme.typography.bodySmall)
                        Text("• Transactions Restored: ${summary.transactionsCount}", style = MaterialTheme.typography.bodySmall)
                        if (summary.backupDate.isNotEmpty()) {
                            Text("• Backup Date: ${summary.backupDate}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                HeroButton(onClick = { showRestoreSummaryDialog = false }, text = "OK")
            }
        )
    }


}
