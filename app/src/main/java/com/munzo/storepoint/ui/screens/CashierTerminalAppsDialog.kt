package com.munzo.storepoint.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.munzo.storepoint.ui.components.HeroCard
import com.munzo.storepoint.ui.components.HeroTextButton
import com.munzo.storepoint.util.DigitalServicesHelper

@Composable
fun CashierTerminalAppsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val adminPrefs = remember { context.getSharedPreferences(DigitalServicesHelper.PREFS_NAME, Context.MODE_PRIVATE) }
    val allowedPackages = remember(adminPrefs) { DigitalServicesHelper.getAllowedApps(adminPrefs) }
    val pm = context.packageManager
    val launchableApps = remember(allowedPackages) {
        allowedPackages.mapNotNull { pkg ->
            try {
                val info = pm.getApplicationInfo(pkg, 0)
                val label = pm.getApplicationLabel(info).toString()
                pkg to label
            } catch (e: Exception) {
                null
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Apps, null, tint = MaterialTheme.colorScheme.primary)
                Text("Authorized Terminal Apps", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Admin-whitelisted applications authorized for launch on this register terminal:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (launchableApps.isEmpty()) {
                    HeroCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("No external apps currently authorized by admin.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for ((pkg, label) in launchableApps) {
                            HeroCard(
                                onClick = {
                                    DigitalServicesHelper.launchAppByPackage(context, pkg)
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(pkg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Icon(Icons.Default.Launch, contentDescription = "Launch", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            HeroTextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
