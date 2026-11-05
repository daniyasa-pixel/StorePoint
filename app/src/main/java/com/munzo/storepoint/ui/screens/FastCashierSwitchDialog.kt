package com.munzo.storepoint.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import com.munzo.storepoint.data.User
import com.munzo.storepoint.ui.StorePointViewModel
import com.munzo.storepoint.ui.theme.PinPadEntry
import com.munzo.storepoint.ui.theme.shakeOnTrigger
import com.munzo.storepoint.util.BiometricAuthHelper
import com.munzo.storepoint.util.SecurityHelper

import com.munzo.storepoint.ui.components.*
import com.munzo.storepoint.ui.theme.HeroUITheme

private fun Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastCashierSwitchDialog(
    viewModel: StorePointViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allUsers by viewModel.allUsers.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()

    // Filter to cashiers and staff
    val availableStaff = remember(allUsers) {
        allUsers.filter { it.username.isNotBlank() }
    }

    var selectedUser by remember {
        mutableStateOf(availableStaff.firstOrNull { it.username != activeUser?.username } ?: availableStaff.firstOrNull())
    }
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var shakeTrigger by remember { mutableIntStateOf(0) }
    var isVerifying by remember { mutableStateOf(false) }

    val isBiometricAvailable = remember(context) {
        BiometricAuthHelper.checkAvailability(context).isAvailable
    }

    fun verifyAndSwitch(pin: String) {
        val target = selectedUser
        if (target == null) {
            errorMessage = "Please select a staff profile."
            shakeTrigger++
            return
        }
        isVerifying = true
        val result = SecurityHelper.verifyPin(pin, target.pinHash)
        if (result.isMatch) {
            viewModel.activeUser.value = target
            Toast.makeText(context, "Switched active cashier to @${target.username}", Toast.LENGTH_SHORT).show()
            onDismiss()
        } else {
            errorMessage = "Incorrect ${SecurityHelper.PIN_LENGTH}-digit PIN for @${target.username}."
            shakeTrigger++
            pinInput = ""
            isVerifying = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        HeroCard(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 440.dp)
                .testTag("fast_cashier_switch_dialog"),
            backgroundColor = HeroUITheme.colors.surface,
            borderColor = HeroUITheme.colors.border,
            shape = HeroUITheme.radius.xxl
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with HeroUI Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(HeroUITheme.radius.md)
                                .background(HeroUITheme.colors.accent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = HeroUITheme.colors.accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Fast Cashier Switch",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = HeroUITheme.colors.foreground
                            )
                            Text(
                                text = "Current: @${activeUser?.username ?: "None"} (${activeUser?.role ?: ""})",
                                style = MaterialTheme.typography.labelSmall,
                                color = HeroUITheme.colors.muted
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = HeroUITheme.colors.muted)
                    }
                }

                HeroDivider(color = HeroUITheme.colors.separator)

                // Staff Picker Chips
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Incoming Staff:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = HeroUITheme.colors.foreground
                        )
                        selectedUser?.let { usr ->
                            HeroChip(
                                text = usr.role,
                                color = if (usr.role == "ADMIN") HeroColor.Warning else HeroColor.Primary,
                                variant = HeroChipVariant.Flat
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableStaff.forEach { usr ->
                            val isSelected = selectedUser?.username == usr.username
                            Surface(
                                onClick = {
                                    selectedUser = usr
                                    errorMessage = ""
                                    pinInput = ""
                                },
                                shape = HeroUITheme.radius.full,
                                color = if (isSelected) HeroUITheme.colors.accent else HeroUITheme.colors.surfaceSecondary,
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, HeroUITheme.colors.border),
                                modifier = Modifier.testTag("switch_user_chip_${usr.username}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        if (usr.role == "ADMIN") Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isSelected) HeroUITheme.colors.accentForeground else HeroUITheme.colors.muted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        usr.username,
                                        color = if (isSelected) HeroUITheme.colors.accentForeground else HeroUITheme.colors.foreground,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Biometric Fingerprint Switch Option
                if (isBiometricAvailable) {
                    HeroButton(
                        onClick = {
                            val fragmentActivity = context.findActivity() as? FragmentActivity
                            if (fragmentActivity != null) {
                                BiometricAuthHelper.authenticate(
                                    activity = fragmentActivity,
                                    title = "Fast Cashier Fingerprint Switch",
                                    subtitle = "Scan your fingerprint to switch cashier instantly",
                                    onOutcome = { outcome, msg ->
                                        if (outcome != BiometricAuthHelper.BiometricOutcome.SUCCESS) {
                                            errorMessage = msg
                                        }
                                    },
                                    onSuccess = {
                                        val target = selectedUser ?: activeUser
                                        if (target != null) {
                                            viewModel.activeUser.value = target
                                            Toast.makeText(context, "Fingerprint verified: @${target.username} signed in", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        }
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("biometric_fast_switch_btn"),
                        color = HeroColor.Success,
                        variant = HeroButtonVariant.Flat,
                        size = HeroSize.Md,
                        icon = Icons.Default.Fingerprint,
                        text = "Switch with Fingerprint"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeroDivider(modifier = Modifier.weight(1f), color = HeroUITheme.colors.separator)
                        Text(
                            "OR ENTER PIN",
                            style = MaterialTheme.typography.labelSmall,
                            color = HeroUITheme.colors.muted,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            fontWeight = FontWeight.Bold
                        )
                        HeroDivider(modifier = Modifier.weight(1f), color = HeroUITheme.colors.separator)
                    }
                }

                // Numeric PIN Pad
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shakeOnTrigger(shakeTrigger),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Enter ${SecurityHelper.PIN_LENGTH}-Digit PIN for @${selectedUser?.username ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = HeroUITheme.colors.muted
                    )

                    PinPadEntry(
                        pin = pinInput,
                        onPinChange = {
                            pinInput = it
                            errorMessage = ""
                        },
                        onPinComplete = { completedPin ->
                            verifyAndSwitch(completedPin)
                        },
                        isMasked = true,
                        isError = errorMessage.isNotEmpty(),
                        errorMessage = errorMessage.ifEmpty { null },
                        isEnabled = !isVerifying,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("fast_switch_pin_pad")
                    )
                }

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        color = HeroColor.Default,
                        variant = HeroButtonVariant.Light,
                        size = HeroSize.Md,
                        text = "Cancel"
                    )

                    HeroButton(
                        onClick = { verifyAndSwitch(pinInput) },
                        enabled = pinInput.length == SecurityHelper.PIN_LENGTH && !isVerifying,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_fast_switch_btn"),
                        color = HeroColor.Primary,
                        variant = HeroButtonVariant.Solid,
                        size = HeroSize.Md,
                        text = "Switch Staff"
                    )
                }
            }
        }
    }
}
