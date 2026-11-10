package com.munzo.storepoint.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.munzo.storepoint.data.User
import com.munzo.storepoint.ui.StorePointViewModel
import com.munzo.storepoint.ui.theme.glassPanel
import com.munzo.storepoint.ui.theme.HeroUITheme
import com.munzo.storepoint.ui.components.*
import com.munzo.storepoint.util.SecurityHelper

// Staff user lists tab
@Composable
fun UsersTab(
    users: List<User>,
    onDelete: (String) -> Unit,
    onAddClick: () -> Unit,
    onEdit: (User) -> Unit,
    onPrintBadge: (User) -> Unit,
    viewModel: StorePointViewModel? = null,
    onResetPin: ((User, String) -> Unit)? = null
) {
    val context = LocalContext.current
    var userToResetPin by remember { mutableStateOf<User?>(null) }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Staff Roles & Access",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            HeroButton(
                onClick = onAddClick,
                text = "Register Staff",
                icon = Icons.Default.PersonAdd,
                modifier = Modifier.testTag("add_user_button")
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(users) { usr ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassPanel(cornerRadius = 16.dp)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        val containerCol = when (usr.role) {
                            "ADMIN" -> MaterialTheme.colorScheme.primaryContainer
                            "INVENTORY" -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        }
                        val contentCol = when (usr.role) {
                            "ADMIN" -> MaterialTheme.colorScheme.onPrimaryContainer
                            "INVENTORY" -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        }
                        val iconVec = when (usr.role) {
                            "ADMIN" -> Icons.Default.AdminPanelSettings
                            "INVENTORY" -> Icons.Default.Inventory2
                            else -> Icons.Default.Badge
                        }
                        Surface(
                            shape = CircleShape,
                            color = containerCol,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = iconVec,
                                    contentDescription = null,
                                    tint = contentCol,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(usr.username.uppercase(), fontWeight = FontWeight.Bold)
                            Text("Assigned Role: ${usr.role}", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = if (usr.barcodeId.isNotEmpty()) "ID Barcode: ${usr.barcodeId}" else "No Barcode Badge",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (usr.barcodeId.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { onPrintBadge(usr) },
                            modifier = Modifier.size(36.dp).testTag("print_user_${usr.username}"),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode, 
                                contentDescription = "Export & Print Cashier ID Badge", 
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        FilledTonalIconButton(
                            onClick = { onEdit(usr) },
                            modifier = Modifier.size(36.dp).testTag("edit_user_${usr.username}"),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                contentColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Staff Barcode & Credentials", modifier = Modifier.size(18.dp))
                        }

                        FilledTonalIconButton(
                            onClick = {
                                userToResetPin = usr
                                newPinInput = ""
                                confirmPinInput = ""
                                pinError = null
                            },
                            modifier = Modifier.size(36.dp).testTag("reset_pin_btn_${usr.username}"),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                contentColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Icon(Icons.Default.LockReset, contentDescription = "Reset PIN", modifier = Modifier.size(18.dp))
                        }

                        // Protect standard default admin and last remaining admin profile from deletion
                        if (usr.username != "admin" && (usr.role != "ADMIN" || users.count { it.role == "ADMIN" } > 1)) {
                            FilledTonalIconButton(
                                onClick = { onDelete(usr.username) },
                                modifier = Modifier.size(36.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Staff", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Admin PIN Reset Dialog
    userToResetPin?.let { targetUser ->
        AlertDialog(
            onDismissRequest = { userToResetPin = null },
            icon = {
                Icon(Icons.Default.LockReset, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            title = {
                Text("Reset Staff PIN: @${targetUser.username}")
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Assign a new ${SecurityHelper.PIN_LENGTH}-digit numeric PIN for ${targetUser.username} (${targetUser.role}):",
                        style = MaterialTheme.typography.bodySmall
                    )

                    HeroInput(
                        value = newPinInput,
                        onValueChange = {
                            if (it.length <= SecurityHelper.PIN_LENGTH && it.all { c -> c.isDigit() }) {
                                newPinInput = it
                                pinError = null
                            }
                        },
                        label = { Text("New ${SecurityHelper.PIN_LENGTH}-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_pin_input")
                    )

                    HeroInput(
                        value = confirmPinInput,
                        onValueChange = {
                            if (it.length <= SecurityHelper.PIN_LENGTH && it.all { c -> c.isDigit() }) {
                                confirmPinInput = it
                                pinError = null
                            }
                        },
                        label = { Text("Confirm New PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("confirm_pin_input")
                    )

                    pinError?.let { err ->
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                HeroButton(
                    onClick = {
                        if (!SecurityHelper.isValidPin(newPinInput)) {
                            pinError = "PIN must be exactly ${SecurityHelper.PIN_LENGTH} digits."
                            return@HeroButton
                        }
                        if (newPinInput != confirmPinInput) {
                            pinError = "PIN entries do not match."
                            return@HeroButton
                        }
                        if (onResetPin != null) {
                            onResetPin(targetUser, newPinInput)
                        } else if (viewModel != null) {
                            viewModel.resetUserPin(targetUser.username, newPinInput) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "PIN successfully updated for @${targetUser.username}", Toast.LENGTH_SHORT).show()
                        }
                        userToResetPin = null
                    },
                    text = "Save New PIN",
                    modifier = Modifier.testTag("save_reset_pin_btn")
                )
            },
            dismissButton = {
                HeroTextButton(onClick = { userToResetPin = null }, text = "Cancel")
            }
        )
    }
}
