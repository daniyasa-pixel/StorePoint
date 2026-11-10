package com.munzo.storepoint.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.munzo.storepoint.ui.components.HeroButton
import com.munzo.storepoint.ui.components.HeroCard
import com.munzo.storepoint.ui.components.HeroFilledTonalButton
import com.munzo.storepoint.ui.components.HeroOutlinedButton
import com.munzo.storepoint.ui.theme.GCashColor
import com.munzo.storepoint.ui.theme.LoadColor
import com.munzo.storepoint.ui.theme.MayaColor
import com.munzo.storepoint.util.DigitalServicesHelper
import com.munzo.storepoint.util.PendingDigitalService
import java.util.Locale

@Composable
fun CashierPostCheckoutDigitalDialog(
    pendingDigitalServices: List<PendingDigitalService>,
    curr: String,
    onDismiss: () -> Unit,
    onAllCompleted: () -> Unit
) {
    val context = LocalContext.current
    val adminPrefs = remember { context.getSharedPreferences(DigitalServicesHelper.PREFS_NAME, Context.MODE_PRIVATE) }
    val preferredLoadApp = remember(adminPrefs) { DigitalServicesHelper.getPreferredLoadApp(adminPrefs) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Fulfill Digital Services (Paid)", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Customer payment has been recorded. Fulfill external transfer or load top-up below:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                for (item in pendingDigitalServices) {
                    HeroCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.service} ${item.action}" + if (item.network.isNotBlank()) " (${item.network})" else "",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = when (item.service) {
                                        "GCash" -> GCashColor
                                        "Maya" -> MayaColor
                                        else -> LoadColor
                                    }
                                )
                                Text(
                                    text = "$curr${String.format(Locale.getDefault(), "%.2f", item.amount)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            if (item.action == "Cash Out") {
                                Text(
                                    text = "Verify customer transferred $curr${String.format(Locale.getDefault(), "%.2f", item.amount)} to store wallet before releasing cash.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Text(
                                    text = "Customer paid fee. Send $curr${String.format(Locale.getDefault(), "%.2f", item.amount)} to recipient.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (item.mobileNumber.isNotBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Phone, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                                        Text(item.mobileNumber, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    IconButton(
                                        onClick = {
                                            DigitalServicesHelper.copyToClipboard(context, "Mobile Number", item.mobileNumber)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy number", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            when (item.service) {
                                "GCash" -> {
                                    HeroButton(
                                        onClick = {
                                            DigitalServicesHelper.launchGCash(context)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Launch, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            if (item.action == "Cash Out") "Open GCash (Verify Transfer)"
                                            else "Open GCash (Send $curr${String.format(Locale.getDefault(), "%.2f", item.amount)})"
                                        )
                                    }
                                    if (item.mobileNumber.isNotBlank()) {
                                        HeroOutlinedButton(
                                            onClick = {
                                                val smsMsg = "GCash ${item.action}: $curr${String.format(Locale.getDefault(), "%.2f", item.amount)} processed at store."
                                                DigitalServicesHelper.launchSms(context, item.mobileNumber, smsMsg)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Send, null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Direct SMS Confirmation", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                                "Maya" -> {
                                    HeroButton(
                                        onClick = {
                                            DigitalServicesHelper.launchMaya(context)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Launch, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            if (item.action == "Cash Out") "Open Maya (Verify Transfer)"
                                            else "Open Maya (Send $curr${String.format(Locale.getDefault(), "%.2f", item.amount)})"
                                        )
                                    }
                                    if (item.mobileNumber.isNotBlank()) {
                                        HeroOutlinedButton(
                                            onClick = {
                                                val smsMsg = "Maya ${item.action}: $curr${String.format(Locale.getDefault(), "%.2f", item.amount)} processed at store."
                                                DigitalServicesHelper.launchSms(context, item.mobileNumber, smsMsg)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Send, null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Direct SMS Confirmation", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                                "Load" -> {
                                    val ussdCode = DigitalServicesHelper.getUssdCodeForNetwork(item.network)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        HeroButton(
                                            onClick = {
                                                DigitalServicesHelper.launchDialer(context, ussdCode)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Call, null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Dial ($ussdCode)", style = MaterialTheme.typography.labelSmall)
                                        }
                                        HeroOutlinedButton(
                                            onClick = {
                                                DigitalServicesHelper.launchSimToolkit(context)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.SimCard, null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("SIM Toolkit", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }

                                    if (item.mobileNumber.isNotBlank()) {
                                        val smsCmd = DigitalServicesHelper.getSmsCommand(item.service, item.network, item.mobileNumber, item.amount)
                                        HeroOutlinedButton(
                                            onClick = {
                                                DigitalServicesHelper.launchSms(context, item.mobileNumber, smsCmd)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Send, null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Direct SMS Trigger (${item.network})", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }

                                    if (preferredLoadApp.isNotBlank()) {
                                        val pm = context.packageManager
                                        val appName = try {
                                            val info = pm.getApplicationInfo(preferredLoadApp, 0)
                                            pm.getApplicationLabel(info).toString()
                                        } catch (e: Exception) {
                                            preferredLoadApp
                                        }
                                        HeroFilledTonalButton(
                                            onClick = {
                                                DigitalServicesHelper.launchAppByPackage(context, preferredLoadApp)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.AppShortcut, null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Open $appName (Admin Load App)")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            HeroButton(
                onClick = onAllCompleted
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("All Services Completed")
            }
        }
    )
}
