package com.munzo.storepoint.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.munzo.storepoint.StorePointDeviceAdminReceiver
import com.munzo.storepoint.ui.components.*
import com.munzo.storepoint.ui.theme.ExpressiveOtpPinInput
import com.munzo.storepoint.ui.theme.HeroUITheme
import com.munzo.storepoint.util.SecurityHelper

/**
 * Step 2 of StorePoint onboarding setup: Admin credentials and device policy lockdown.
 * Extracted from SetupScreen to enforce modular code structure and <800 line maintainability ceiling.
 */
@Composable
fun SetupStepTwoSecurity(
    context: Context,
    isExpanded: Boolean,
    isDeviceAdminActive: Boolean,
    adminName: String,
    onAdminNameChange: (String) -> Unit,
    adminPhoneNumber: String,
    onAdminPhoneNumberChange: (String) -> Unit,
    adminUsername: String,
    onAdminUsernameChange: (String) -> Unit,
    adminPassword: String,
    onAdminPasswordChange: (String) -> Unit
) {
    HeroCardHeader(
        title = "Step 2: Admin Accounts & Terminal Security",
        subtitle = "Create master credentials and lock down kiosk mode",
        icon = Icons.Default.Shield,
        iconColor = HeroUITheme.colors.accent
    )

    // Device Administrator Security Card
    HeroCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = HeroUITheme.radius.lg,
        backgroundColor = if (isDeviceAdminActive)
            HeroUITheme.colors.success.copy(alpha = 0.1f)
        else
            HeroUITheme.colors.danger.copy(alpha = 0.1f),
        borderColor = if (isDeviceAdminActive)
            HeroUITheme.colors.success.copy(alpha = 0.4f)
        else
            HeroUITheme.colors.danger.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isDeviceAdminActive) Icons.Default.Shield else Icons.Default.GppMaybe,
                        contentDescription = null,
                        tint = if (isDeviceAdminActive) HeroUITheme.colors.success else HeroUITheme.colors.danger,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Device Administrator",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                HeroChip(
                    text = if (isDeviceAdminActive) "ACTIVE" else "REQUIRED",
                    color = if (isDeviceAdminActive) HeroColor.Success else HeroColor.Danger,
                    variant = HeroChipVariant.Solid
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isDeviceAdminActive)
                    "Device Administrator is active. Anti-uninstall defense and Kiosk unpin lock are enforced."
                else
                    "Device Administrator privilege is required to lock kiosk mode and prevent unauthorized app uninstallation or clearing data.",
                style = MaterialTheme.typography.bodySmall,
                color = HeroUITheme.colors.muted
            )
            if (!isDeviceAdminActive) {
                Spacer(modifier = Modifier.height(10.dp))
                HeroButton(
                    onClick = {
                        try {
                            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, StorePointDeviceAdminReceiver.getComponentName(context))
                                putExtra(
                                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                    "StorePoint requires Device Administrator privileges to lock kiosk mode and prevent unauthorized app uninstallation or clearing data."
                                )
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open Device Admin screen: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = HeroColor.Danger,
                    variant = HeroButtonVariant.Solid,
                    size = HeroSize.Md,
                    icon = Icons.Default.AdminPanelSettings,
                    text = "Activate Device Administrator"
                )
            }
        }
    }

    if (isExpanded) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeroInput(
                value = adminName,
                onValueChange = onAdminNameChange,
                label = { Text("Admin / Owner Full Name") },
                placeholder = { Text("Juan Dela Cruz") },
                leadingIcon = { Icon(Icons.Default.Badge, null, tint = HeroUITheme.colors.accent) },
                modifier = Modifier.weight(1f).testTag("admin_name_input"),
                singleLine = true,
                shape = HeroUITheme.radius.md
            )
            HeroInput(
                value = adminPhoneNumber,
                onValueChange = { input ->
                    if (input.length <= 15 && input.all { it.isDigit() || it == '+' || it == ' ' || it == '-' }) {
                        onAdminPhoneNumberChange(input)
                    }
                },
                label = { Text("Admin Phone Number") },
                placeholder = { Text("09171234567") },
                leadingIcon = { Icon(Icons.Default.Phone, null, tint = HeroUITheme.colors.accent) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.weight(1f).testTag("admin_phone_input"),
                singleLine = true,
                shape = HeroUITheme.radius.md
            )
        }
    } else {
        HeroInput(
            value = adminName,
            onValueChange = onAdminNameChange,
            label = { Text("Admin / Owner Full Name") },
            placeholder = { Text("Juan Dela Cruz") },
            leadingIcon = { Icon(Icons.Default.Badge, null, tint = HeroUITheme.colors.accent) },
            modifier = Modifier.fillMaxWidth().testTag("admin_name_input"),
            singleLine = true,
            shape = HeroUITheme.radius.md
        )
        HeroInput(
            value = adminPhoneNumber,
            onValueChange = { input ->
                if (input.length <= 15 && input.all { it.isDigit() || it == '+' || it == ' ' || it == '-' }) {
                    onAdminPhoneNumberChange(input)
                }
            },
            label = { Text("Admin Phone Number") },
            placeholder = { Text("09171234567") },
            supportingText = { Text("Dialed automatically when cashiers trigger 'Call Admin'") },
            leadingIcon = { Icon(Icons.Default.Phone, null, tint = HeroUITheme.colors.accent) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth().testTag("admin_phone_input"),
            singleLine = true,
            shape = HeroUITheme.radius.md
        )
    }

    HeroInput(
        value = adminUsername,
        onValueChange = onAdminUsernameChange,
        label = { Text("Admin Username") },
        placeholder = { Text("admin") },
        leadingIcon = { Icon(Icons.Default.AccountCircle, null, tint = HeroUITheme.colors.accent) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_user_input"),
        singleLine = true,
        shape = HeroUITheme.radius.md
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Admin ${SecurityHelper.PIN_LENGTH}-Digit PIN",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = HeroUITheme.colors.foreground
            )
            val adminPinWeak = adminPassword.isNotEmpty() && SecurityHelper.isWeakPin(adminPassword)
            if (adminPinWeak) {
                HeroChip(text = "Too Weak", color = HeroColor.Danger, variant = HeroChipVariant.Flat)
            } else if (adminPassword.length == SecurityHelper.PIN_LENGTH) {
                HeroChip(text = "PIN Ready", color = HeroColor.Success, variant = HeroChipVariant.Flat)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        val adminPinWeak = adminPassword.isNotEmpty() && SecurityHelper.isWeakPin(adminPassword)
        ExpressiveOtpPinInput(
            pin = adminPassword,
            onPinChange = onAdminPasswordChange,
            isMasked = true,
            isError = adminPinWeak,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_password_input")
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = when {
                adminPinWeak -> "Too easy to guess — ${SecurityHelper.pinPolicyHint()}"
                adminPassword.length == SecurityHelper.PIN_LENGTH -> "✓ Master PIN ready"
                else -> "${adminPassword.length}/${SecurityHelper.PIN_LENGTH} digits (numeric only)"
            },
            style = MaterialTheme.typography.labelSmall,
            color = when {
                adminPinWeak -> HeroUITheme.colors.danger
                adminPassword.length == SecurityHelper.PIN_LENGTH -> HeroUITheme.colors.success
                else -> HeroUITheme.colors.muted
            }
        )
    }
}
