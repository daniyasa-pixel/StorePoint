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
fun AdminCrashDiagnosticsSection(viewModel: StorePointViewModel) {
    val context = LocalContext.current
    val crashLogs by viewModel.crashLogs.collectAsState()
    var showCrashLogsDialog by remember { mutableStateOf(false) }

        // --- OBSERVABILITY & CRASH DIAGNOSTICS CARD ---
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
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Software Updates",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Terminal Software Updates",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Automated GitHub Releases distribution & OTA updates. (Installed: v$APP_VERSION)",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Text(
                    text = "StorePoint can check the official GitHub Releases pipeline for compiled APK assets, download new release binaries over Wi-Fi, and initiate system package updates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val isCheckingUpdates by viewModel.updateCheckInProgress.collectAsState()
                HeroButton(
                    onClick = {
                        viewModel.checkForAppUpdates(APP_VERSION) { info ->
                            if (info != null && info.isNewer) {
                                Toast.makeText(context, "New update available: ${info.latestVersionTag}! Open 'About StorePoint' to install.", Toast.LENGTH_LONG).show()
                            } else if (info != null) {
                                Toast.makeText(context, "StorePoint is already running the latest version.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Unable to check updates: check internet connection.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("security_check_updates_btn"),
                    enabled = !isCheckingUpdates
                ) {
                    if (isCheckingUpdates) {
                        HeroSpinner(size = 16.dp, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Checking GitHub Releases...")
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Check for OTA Releases")
                    }
                }
            }
        }

        // --- OBSERVABILITY & CRASH DIAGNOSTICS CARD ---
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
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Diagnostics",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "System Observability & Diagnostics",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Crash logging, device telemetry, and operational observability.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Telemetry summary badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val rt = Runtime.getRuntime()
                    val usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
                    val maxMb = rt.maxMemory() / (1024 * 1024)
                    HeroCard(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("App RAM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("${usedMb}MB / ${maxMb}MB", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    HeroCard(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Crash Incidents", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("${crashLogs.size} Logged", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = if (crashLogs.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroButton(
                        onClick = { showCrashLogsDialog = true },
                        modifier = Modifier.weight(1f).testTag("view_crash_logs_btn"),
                        variant = HeroButtonVariant.Bordered,
                        color = HeroColor.Default,
                        text = "Crash Logs (${crashLogs.size})",
                        icon = Icons.Default.Visibility
                    )

                    HeroButton(
                        onClick = {
                            val report = viewModel.getDiagnosticReport()
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "StorePoint Diagnostics")
                                putExtra(Intent.EXTRA_TEXT, report)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Diagnostics"))
                        },
                        modifier = Modifier.weight(1f).testTag("share_diagnostics_report_btn"),
                        text = "Share Report",
                        icon = Icons.Default.Share
                    )
                }
            }
        }


    // --- CRASH DIAGNOSTICS & INCIDENT VIEWER MODAL ---
    if (showCrashLogsDialog) {
        AlertDialog(
            onDismissRequest = { showCrashLogsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BugReport, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crash Diagnostics Logs (${crashLogs.size})")
                }
            },
            text = {
                if (crashLogs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text("No crashes recorded. System is operating stably.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(crashLogs) { entry ->
                            HeroCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(entry.dateFormatted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        Text("Cashier: ${entry.activeCashier}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Text(
                                        text = "${entry.exceptionName}: ${entry.message}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = entry.stackTrace.take(300) + if (entry.stackTrace.length > 300) "..." else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (crashLogs.isNotEmpty()) {
                    HeroTextButton(
                        onClick = {
                            viewModel.clearCrashLogs()
                            Toast.makeText(context, "Crash diagnostics cleared.", Toast.LENGTH_SHORT).show()
                        },
                        color = HeroColor.Danger,
                        text = "Clear Logs"
                    )
                }
            },
            dismissButton = {
                HeroButton(
                    onClick = { showCrashLogsDialog = false },
                    text = "Done"
                )
            }
        )
    }


}
