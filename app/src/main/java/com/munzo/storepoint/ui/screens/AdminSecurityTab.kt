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
fun SecurityTab(viewModel: StorePointViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isAlwaysOn by viewModel.isAlwaysOnEnabled.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val storeConfig by viewModel.storeConfig.collectAsState()

    // Kiosk App Whitelisting & Admin PIN Reset states
    val allowedTerminalApps by viewModel.allowedTerminalApps.collectAsState()
    var newAllowedPackage by remember { mutableStateOf("") }
    // Admin Contact & Assistance Phone
    val adminPrefs = remember { context.getSharedPreferences("storepoint_admin_prefs", Context.MODE_PRIVATE) }
    var adminContactName by remember { mutableStateOf(adminPrefs.getString("admin_name", "") ?: "") }
    var adminPhoneNumber by remember { mutableStateOf(adminPrefs.getString("admin_phone_number", "") ?: "") }
    var adminPhoneSavedFeedback by remember { mutableStateOf(false) }

    // Allowed Apps & Terminal Whitelist
    var appSearchQuery by remember { mutableStateOf("") }
    var allowedAppsSet by remember {
        mutableStateOf(DigitalServicesHelper.getAllowedApps(adminPrefs))
    }
    var preferredLoadApp by remember {
        mutableStateOf(DigitalServicesHelper.getPreferredLoadApp(adminPrefs))
    }
    var appsSavedFeedback by remember { mutableStateOf(false) }

    val installedApps = remember {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        try {
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            resolveInfos.map {
                it.activityInfo.packageName to it.loadLabel(pm).toString()
            }.filter { it.first != context.packageName }
             .distinctBy { it.first }
             .sortedBy { it.second.lowercase(Locale.getDefault()) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Device Security Management",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // 1. Database Health, Backup & Maintenance
        AdminDatabaseMaintenanceSection(viewModel = viewModel)

        // 2. Kiosk Mode & Device Admin Protection
        AdminKioskLockdownSection(viewModel = viewModel)

        // 3. Hardware Thermal Printer & Cash Drawer Peripherals
        AdminHardwareSection(viewModel = viewModel)

        // 4. Incident Observability & Diagnostics
        AdminCrashDiagnosticsSection(viewModel = viewModel)

        // --- ADMIN ASSISTANCE & PHONE DIALING CONFIGURATION ---
        HeroCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Admin Assistance & Cashier Phone Dialing",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Configured contact number dialed when cashiers trigger 'Call Admin' from the register.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HeroInput(
                    value = adminContactName,
                    onValueChange = {
                        adminContactName = it
                        adminPhoneSavedFeedback = false
                    },
                    label = { Text("Admin / Owner Full Name") },
                    placeholder = { Text("e.g. Juan Dela Cruz") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Badge, null) },
                    modifier = Modifier.fillMaxWidth()
                )

                HeroInput(
                    value = adminPhoneNumber,
                    onValueChange = {
                        adminPhoneNumber = it
                        adminPhoneSavedFeedback = false
                    },
                    label = { Text("Admin / Supervisor Mobile Number") },
                    placeholder = { Text("e.g. 09171234567 or +639171234567") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Call, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_security_phone_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroButton(
                        onClick = {
                            adminPrefs.edit()
                                .putString("admin_phone_number", adminPhoneNumber.trim())
                                .putString("admin_name", adminContactName.trim())
                                .apply()
                            adminPhoneSavedFeedback = true
                            Toast.makeText(context, "Admin contact details saved successfully!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Save,
                        text = if (adminPhoneSavedFeedback) "Saved!" else "Save Contact Details"
                    )

                    HeroOutlinedButton(
                        onClick = {
                            val clean = adminPhoneNumber.filter { it.isDigit() || it == '+' }
                            if (clean.isNotBlank()) {
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(dialIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot launch dialer: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Please enter a phone number to test dialing.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        icon = Icons.Default.PhoneForwarded,
                        text = "Test Dial"
                    )
                }
            }
        }


        // --- ALLOWED TERMINAL APPS (WHITELIST FOR CASHIER TERMINAL) ---
        HeroCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Allowed Terminal Apps (Launcher Whitelist)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Control which external applications the POS terminal is authorized to launch. Other apps are restricted from the cashier.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${allowedAppsSet.size} of ${installedApps.size} apps authorized",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HeroTextButton(
                            onClick = {
                                allowedAppsSet = installedApps.map { it.first }.toSet()
                                appsSavedFeedback = false
                            },
                            size = HeroSize.Sm,
                            text = "Allow All"
                        )
                        HeroTextButton(
                            onClick = {
                                allowedAppsSet = DigitalServicesHelper.DEFAULT_ALLOWED_APPS
                                appsSavedFeedback = false
                            },
                            size = HeroSize.Sm,
                            text = "Reset Defaults"
                        )
                    }
                }

                HeroInput(
                    value = appSearchQuery,
                    onValueChange = { appSearchQuery = it },
                    label = { Text("Search installed applications") },
                    placeholder = { Text("Filter by app name or package...") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (appSearchQuery.isNotEmpty()) {
                                                        IconButton(onClick = { appSearchQuery = "" }) {
                                Icon(Icons.Default.Close, "Clear search", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // List of Apps with Toggles
                val filteredApps = if (appSearchQuery.isBlank()) installedApps else installedApps.filter {
                    it.second.contains(appSearchQuery, ignoreCase = true) || it.first.contains(appSearchQuery, ignoreCase = true)
                }

                HeroCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    if (filteredApps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No applications found.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            filteredApps.forEach { (pkg, label) ->
                                val isAllowed = allowedAppsSet.contains(pkg)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isAllowed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                             else Color.Transparent,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(pkg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    HeroSwitch(
                                        checked = isAllowed,
                                        onCheckedChange = { checked ->
                                            allowedAppsSet = if (checked) {
                                                allowedAppsSet + pkg
                                            } else {
                                                allowedAppsSet - pkg
                                            }
                                            appsSavedFeedback = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Preferred Load App Selection
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Preferred Load Application:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Default is USSD Dialer & SIM Toolkit. You can designate an installed app (e.g. Smart Padala, GlobeOne) as the dedicated load launcher.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    var loadPickerExpanded by remember { mutableStateOf(false) }
                    val currentLoadAppLabel = if (preferredLoadApp.isBlank()) {
                        "Default: Dialer (*123# / *143#) & SIM Toolkit"
                    } else {
                        installedApps.find { it.first == preferredLoadApp }?.second ?: preferredLoadApp
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        HeroOutlinedButton(
                            onClick = { loadPickerExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.SimCard, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(currentLoadAppLabel, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(
                            expanded = loadPickerExpanded,
                            onDismissRequest = { loadPickerExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Default: Dialer & SIM Toolkit") },
                                onClick = {
                                    preferredLoadApp = ""
                                    appsSavedFeedback = false
                                    loadPickerExpanded = false
                                }
                            )
                            val allowedInstalledApps = installedApps.filter { allowedAppsSet.contains(it.first) }
                            allowedInstalledApps.forEach { (pkg, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        preferredLoadApp = pkg
                                        appsSavedFeedback = false
                                        loadPickerExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Save Whitelist Button
                HeroButton(
                    onClick = {
                        DigitalServicesHelper.saveAllowedApps(adminPrefs, allowedAppsSet)
                        DigitalServicesHelper.savePreferredLoadApp(adminPrefs, preferredLoadApp)
                        appsSavedFeedback = true
                        Toast.makeText(context, "Allowed terminal apps configuration saved!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Save,
                    text = if (appsSavedFeedback) "Saved Successfully!" else "Save Allowed Apps Configuration"
                )
            }
        }


        // --- ALWAYS ON DISPLAY TOGGLE ---
        HeroCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isAlwaysOn) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.LightMode,
                        contentDescription = "Always On Awake",
                        tint = if (isAlwaysOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                    Column {
                        Text(
                            text = "Always-On Awake Mode",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Keeps the checkout register screen lit continuously without dimming or screen-off timer shutdowns.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HeroSwitch(
                    checked = isAlwaysOn,
                    onCheckedChange = { viewModel.setAlwaysOnEnabled(it) }
                )
            }
        }


        // --- OWNER WALLET FLOAT RELOAD ---
        // Admin-only, and recorded in the wallet_ledger table so every peso in/out of
        // the owner's GCash / Smart / Globe floats is explainable. See WalletReloadCard.
        WalletReloadCard(
            viewModel = viewModel,
            isAdmin = activeUser?.role?.equals("ADMIN", ignoreCase = true) == true,
            curr = storeConfig?.currencySymbol ?: "P"
        )


        // --- AUTHORIZED TERMINAL APPLICATIONS WHITELIST ---
        HeroCard(
            modifier = Modifier.fillMaxWidth().testTag("kiosk_app_whitelisting_card"),
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
                        imageVector = Icons.Default.Apps,
                        contentDescription = "Authorized Terminal Apps",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "Kiosk Whitelisted Terminal Applications",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Authorized banking, e-wallet, and utility apps cashiers may launch in kiosk mode.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Text(
                    text = "Whitelisted applications can be launched directly from the register without exiting Kiosk Mode or allowing unauthorized device access:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Whitelisted app items
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    allowedTerminalApps.forEach { pkg ->
                        HeroCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val friendlyName = when {
                                    pkg.contains("gcash", ignoreCase = true) -> "GCash (E-Wallet)"
                                    pkg.contains("paymaya", ignoreCase = true) || pkg.contains("maya", ignoreCase = true) -> "Maya (E-Wallet / Bank)"
                                    pkg.contains("stk", ignoreCase = true) -> "SIM Toolkit (Telco Airtime)"
                                    pkg.contains("bdo", ignoreCase = true) -> "BDO Digital Banking"
                                    pkg.contains("bpi", ignoreCase = true) -> "BPI Mobile Banking"
                                    pkg.contains("unionbank", ignoreCase = true) -> "UnionBank Online"
                                    else -> "Authorized App"
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(friendlyName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(pkg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                                IconButton(
                                    onClick = { viewModel.removeAllowedTerminalApp(pkg) },
                                    modifier = Modifier.size(32.dp).testTag("remove_whitelist_${pkg}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Quick presets
                Text("Quick Preset Whitelist Packages:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "GCash" to "com.globe.gcash.android",
                        "Maya" to "com.paymaya",
                        "SIM Toolkit" to "com.android.stk",
                        "BDO" to "ph.com.bdo.digital",
                        "BPI" to "com.bpi.ng.app",
                        "UnionBank" to "com.unionbankph.online"
                    ).forEach { (label, pkg) ->
                        AssistChip(
                            onClick = {
                                viewModel.addAllowedTerminalApp(pkg)
                                Toast.makeText(context, "Added $label to Kiosk whitelist.", Toast.LENGTH_SHORT).show()
                            },
                            label = { Text("+$label") }
                        )
                    }
                }

                // Add custom package row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeroInput(
                        value = newAllowedPackage,
                        onValueChange = { newAllowedPackage = it },
                        label = { Text("Add Android Package Name") },
                        placeholder = { Text("e.g. com.mybank.pos") },
                        modifier = Modifier.weight(1f).testTag("whitelist_package_input"),
                        singleLine = true
                    )
                    HeroButton(
                        onClick = {
                            if (newAllowedPackage.isNotBlank()) {
                                viewModel.addAllowedTerminalApp(newAllowedPackage.trim())
                                newAllowedPackage = ""
                                Toast.makeText(context, "Package added to whitelist.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        text = "Add",
                        icon = Icons.Default.Add,
                        modifier = Modifier.testTag("add_whitelist_app_btn")
                    )
                }
            }
        }


        AdminStaffSecuritySection(viewModel = viewModel)
    }
}
