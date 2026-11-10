package com.munzo.storepoint.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.munzo.storepoint.ui.StorePointViewModel
import com.munzo.storepoint.ui.components.*
import com.munzo.storepoint.ui.theme.PinPadEntry
import com.munzo.storepoint.util.SecurityHelper

@Composable
fun AdminDataWipeSection(viewModel: StorePointViewModel) {
    val context = LocalContext.current
    var showWipeDataDialog by remember { mutableStateOf(false) }
    var adminWipePasswordInput by remember { mutableStateOf("") }
    var wipeDataError by remember { mutableStateOf("") }

        // --- ACCIDENTAL DATA CLEAR PREVENTATIVE WIPE ---
        HeroCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
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
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Data Clear",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "Accidental Data Wipe Protection",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Admin PIN certification required to clear records.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Text(
                    text = "To clear all local products, transactions, and session history records, click below. This requires validating active admin credentials beforehand.",
                    style = MaterialTheme.typography.bodyMedium
                )

                HeroButton(
                    onClick = {
                        adminWipePasswordInput = ""
                        wipeDataError = ""
                        showWipeDataDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = HeroColor.Danger,
                    icon = Icons.Default.DeleteForever,
                    text = "Deconstruct & Clear Entire Local System"
                )
            }
        }

        // Additional register lock system or tips
        HeroCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "How to Block Uninstallation & Clear-Data:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    text = "1. Toggle 'Enable Device Administrator' above. When prompted by Android, grant 'Activate' permission.\n" +
                            "2. Under Android rules, once activated, the OS restricts instant uninstall command from the Launcher Home.\n" +
                            "3. Verify: Go to device settings / drag the icon. You will see uninstall is restricted while active.\n" +
                            "4. To deliberate remove/wipe the register yourself later, simply deactivate this Device Administrator policy first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }


    if (showWipeDataDialog) {
        AlertDialog(
            onDismissRequest = { showWipeDataDialog = false },
            title = { Text("Validate Administrative Authority", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Warning! This deletes all transaction items, users, and catalog sets. Retriggering the system setup sequence. Enter your Admin PIN (${SecurityHelper.PIN_LENGTH} digits) to proceed:")
                    Spacer(Modifier.height(4.dp))
                    // Keypad, not the IME: this authorises a destructive, irreversible
                    // wipe of all sales and inventory.
                    PinPadEntry(
                        pin = adminWipePasswordInput,
                        onPinChange = {
                            adminWipePasswordInput = it
                            wipeDataError = ""
                        },
                        isMasked = true,
                        isError = wipeDataError.isNotEmpty(),
                        errorMessage = wipeDataError.ifEmpty { null },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                HeroButton(
                    color = HeroColor.Danger,
                    enabled = SecurityHelper.isValidPinLenient(adminWipePasswordInput),
                    onClick = {
                        viewModel.clearAllDatabaseData(
                            adminPass = adminWipePasswordInput,
                            onSuccess = {
                                showWipeDataDialog = false
                                Toast.makeText(context, "System successfully cleared and reset.", Toast.LENGTH_LONG).show()
                            },
                            onFailure = { err ->
                                wipeDataError = err
                            }
                        )
                    },
                    text = "Authorize Pure Clearance"
                )
            },
            dismissButton = {
                HeroTextButton(onClick = { showWipeDataDialog = false }, text = "Cancel")
            }
        )
    }



}
