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



private enum class KioskPendingAction { RELEASE_LOCKDOWN }

@Composable
fun AdminKioskLockdownSection(viewModel: StorePointViewModel) {
    val context = LocalContext.current
    val dpm = remember { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    val adminName = remember { ComponentName(context, com.munzo.storepoint.StorePointDeviceAdminReceiver::class.java) }
    var isAdminActive by remember { mutableStateOf(dpm.isAdminActive(adminName)) }
    var lockdownStrength by remember {
        mutableStateOf(KioskLockdownCapability.current(context))
    }

    fun refreshLockdownStatus() {
        isAdminActive = dpm.isAdminActive(adminName)
        lockdownStrength = KioskLockdownCapability.current(context)
    }

    val deviceAdminLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        refreshLockdownStatus()
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                refreshLockdownStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isKioskActive by viewModel.isKioskModeActive.collectAsState()
    val savedKioskPin by viewModel.kioskPin.collectAsState()

    var showKioskPinSetupDialog by remember { mutableStateOf(false) }
    var showKioskPinVerifyDialog by remember { mutableStateOf(false) }
    var pinSetupInput by remember { mutableStateOf("") }
    var pinSetupConfirmInput by remember { mutableStateOf("") }
    var pinSetupError by remember { mutableStateOf("") }
    var pinSetupStage by remember { mutableStateOf(KioskPinSetupFlow.Stage.ENTER) }
    
    var pinVerifyInput by remember { mutableStateOf("") }
    var pinVerifyError by remember { mutableStateOf("") }

    var showAdminFallbackAuthDialog by remember { mutableStateOf(false) }
    var pendingKioskAction by remember { mutableStateOf(KioskPendingAction.RELEASE_LOCKDOWN) }
    var adminAuthError by remember { mutableStateOf("") }
    var adminAuthPin by remember { mutableStateOf("") }

    var kioskPinLockoutActive by remember { mutableStateOf(false) }

        // --- LAUNCHER KIOSK LOCKDOWN CONTROL ---
        HeroCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isKioskActive) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                }
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
                        imageVector = if (isKioskActive) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Kiosk",
                        tint = if (isKioskActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = if (isKioskActive) "TERMINAL LOCKDOWN ACTIVE" else "LAUNCHER LOCKDOWN STANDBY",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isKioskActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Kiosk mode restricts tablet use purely to StorePointPOS with PIN lockouts.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Text(
                    text = "When active, the device enters Kiosk Pin-Screen Lockdown. This disables the home button, recent apps, and pulldown quick settings so cashiers cannot escape the app, launch other applications, or access unauthorized device menus.",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (savedKioskPin.orEmpty().isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Kiosk security PIN code is set.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "No Kiosk PIN configured yet. You will be prompted to set one before locking.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                HeroButton(
                    onClick = {
                        if (isKioskActive) {
                            // SECURITY (issue #2): fail closed. This branch used to call
                            // toggleKioskMode(false) directly when no kiosk PIN was set,
                            // releasing lockdown with no credential at all — directly
                            // contradicting the comment above it and the fail-closed
                            // posture of verifyAdminPin(). If the kiosk PIN is missing we
                            // now require an ADMIN account PIN via the rate-limited
                            // verifyKioskUnlock() path, which is also the only recovery
                            // for a terminal left in lockdown with no PIN configured.
                            if (savedKioskPin.orEmpty().isEmpty()) {
                                adminAuthError = "No Kiosk PIN is configured on this terminal."
                                adminAuthPin = ""
                                pendingKioskAction = KioskPendingAction.RELEASE_LOCKDOWN
                                showAdminFallbackAuthDialog = true
                            } else {
                                pinVerifyInput = ""
                                pinVerifyError = ""
                                showKioskPinVerifyDialog = true
                            }
                        } else {
                            // Turn ON requires PIN to exist
                            if (savedKioskPin.orEmpty().isEmpty()) {
                                pinSetupInput = ""
                                pinSetupConfirmInput = ""
                                pinSetupError = ""
                                pinSetupStage = KioskPinSetupFlow.Stage.ENTER
                                showKioskPinSetupDialog = true
                            } else {
                                viewModel.toggleKioskMode(true)
                                Toast.makeText(context, "System entered secure Kiosk lockdown.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("toggle_kiosk_button"),
                    color = if (isKioskActive) HeroColor.Danger else HeroColor.Primary,
                    text = if (isKioskActive) "Disable Kiosk Mode (Enter PIN to Unlock)" else "Activate Kiosk Mode (Lock Screen)",
                    icon = if (isKioskActive) Icons.Default.LockOpen else Icons.Default.Lock
                )
            }
        }


        // --- DEVICE ADMINISTRATOR CARD ---
        HeroCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isAdminActive) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                }
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
                        // SECURITY (issue #4): the icon and colour now follow real
                        // enforcement, not merely whether device admin is switched on.
                        imageVector = when (lockdownStrength) {
                            LockdownStrength.DeviceOwnerEnforced -> Icons.Default.Security
                            LockdownStrength.ScreenPinningOnly -> Icons.Default.Warning
                            LockdownStrength.Unprotected -> Icons.Default.Warning
                        },
                        contentDescription = "Status",
                        tint = when (lockdownStrength) {
                            LockdownStrength.DeviceOwnerEnforced -> MaterialTheme.colorScheme.primary
                            LockdownStrength.ScreenPinningOnly -> MaterialTheme.colorScheme.tertiary
                            LockdownStrength.Unprotected -> MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            // Previously derived from isAdminActive alone, which reported
                            // "DEVICE PROTECTION ON" for a terminal that could not enforce
                            // anything. Now states what is actually enforced.
                            text = KioskLockdownCapability.label(lockdownStrength),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = when (lockdownStrength) {
                                LockdownStrength.DeviceOwnerEnforced -> MaterialTheme.colorScheme.primary
                                LockdownStrength.ScreenPinningOnly -> MaterialTheme.colorScheme.tertiary
                                LockdownStrength.Unprotected -> MaterialTheme.colorScheme.error
                            }
                        )
                        Text(
                            text = "Terminal enforcement status",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Warn specifically about the admin-but-not-owner case, which is the
                // one that previously looked healthy on screen.
                if (lockdownStrength == LockdownStrength.ScreenPinningOnly) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Device Administrator is ON but this terminal is NOT provisioned as " +
                                "device owner. Kiosk lockdown is screen pinning only.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Text(
                    text = KioskLockdownCapability.explanation(lockdownStrength),
                    style = MaterialTheme.typography.bodyMedium
                )

                if (!isAdminActive) {
                    HeroButton(
                        onClick = {
                            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminName)
                                putExtra(
                                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                    "Lets StorePoint lock or erase this device if the terminal is lost or stolen."
                                )
                            }
                            deviceAdminLauncher.launch(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("activate_device_admin_btn"),
                        icon = Icons.Default.AdminPanelSettings,
                        text = "Enable Device Administrator"
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Protection is active.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HeroTextButton(
                            onClick = {
                                refreshLockdownStatus()
                            },
                            icon = Icons.Default.Refresh,
                            size = HeroSize.Sm,
                            text = "Refresh Status"
                        )
                    }
                }
            }
        }


    // --- SETUP SECURE KIOSK MODE PIN DIALOG (stage 1: enter, stage 2: verify) ---
    // Only one keypad is on screen per stage: PinPadEntry IS a full keypad, and two
    // stacked in this dialog made it taller than the display, pushing "Confirm &
    // Enter Lockdown" off-screen so lockdown could never be completed. Stage rules
    // live in KioskPinSetupFlow; setKioskPin() remains the write-time enforcement.
    if (showKioskPinSetupDialog) {
        val isEnterStage = pinSetupStage == KioskPinSetupFlow.Stage.ENTER
        AlertDialog(
            onDismissRequest = { showKioskPinSetupDialog = false },
            title = {
                Text(
                    text = if (isEnterStage) "Set Secure Kiosk PIN - Step 1 of 2"
                           else "Confirm Kiosk PIN - Step 2 of 2",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                // Scrollable so the keypad, hint and the action buttons all stay
                // reachable on short or dense displays.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isEnterStage) {
                        Text("Create a numeric PIN to lock/unlock Kiosk Mode. Cashiers will not be able to disable Kiosk Mode without this code.")
                        PinPadEntry(
                            pin = pinSetupInput,
                            onPinChange = { newPin ->
                                // Exactly PIN_LENGTH digits, now enforced centrally by
                                // setKioskPin() rather than a local "<= 8" filter.
                                pinSetupInput = newPin.take(SecurityHelper.PIN_LENGTH)
                                pinSetupError = ""
                            },
                            isMasked = false,
                            isError = pinSetupError.isNotEmpty(),
                            errorMessage = pinSetupError.ifEmpty { null },
                            modifier = Modifier.fillMaxWidth()
                        )
                        // The kiosk PIN is held to the same policy as every user PIN
                        // (issue #9) - it is the app's highest-value credential.
                        Text(
                            text = SecurityHelper.pinPolicyHint(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("Re-enter the same PIN to verify it. Lockdown starts as soon as both entries match.")
                        PinPadEntry(
                            pin = pinSetupConfirmInput,
                            onPinChange = { newPin ->
                                pinSetupConfirmInput = newPin.take(SecurityHelper.PIN_LENGTH)
                                pinSetupError = ""
                            },
                            isMasked = false,
                            isError = pinSetupError.isNotEmpty(),
                            errorMessage = pinSetupError.ifEmpty { null },
                            modifier = Modifier.fillMaxWidth()
                        )
                        val matches = pinSetupConfirmInput == pinSetupInput
                        Text(
                            text = when {
                                matches && KioskPinSetupFlow.isComplete(pinSetupConfirmInput) ->
                                    "PINs match. Tap Confirm to enter lockdown."
                                else -> "${pinSetupConfirmInput.length}/${SecurityHelper.PIN_LENGTH} digits entered"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (matches) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                if (isEnterStage) {
                    HeroButton(
                        enabled = KioskPinSetupFlow.isComplete(pinSetupInput),
                        onClick = {
                            val rejection = KioskPinSetupFlow.enterRejection(pinSetupInput)
                            if (rejection != null) {
                                pinSetupError = rejection
                            } else {
                                pinSetupError = ""
                                pinSetupConfirmInput = ""
                                pinSetupStage = KioskPinSetupFlow.Stage.VERIFY
                            }
                        },
                        text = "Continue"
                    )
                } else {
                    HeroButton(
                        enabled = KioskPinSetupFlow.isComplete(pinSetupConfirmInput),
                        onClick = {
                            val rejection = KioskPinSetupFlow.verifyRejection(pinSetupInput, pinSetupConfirmInput)
                            if (rejection != null) {
                                pinSetupError = rejection
                                pinSetupConfirmInput = ""
                            } else {
                                val writeRejection = viewModel.setKioskPin(pinSetupInput)
                                if (writeRejection != null) {
                                    pinSetupError = writeRejection
                                } else {
                                    showKioskPinSetupDialog = false
                                    pinSetupInput = ""
                                    pinSetupConfirmInput = ""
                                    pinSetupStage = KioskPinSetupFlow.Stage.ENTER
                                    viewModel.toggleKioskMode(true)
                                    Toast.makeText(context, "Kiosk PIN configured. Entering lockdown.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        text = "Confirm & Enter Lockdown"
                    )
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isEnterStage) {
                        HeroTextButton(
                            onClick = {
                                pinSetupConfirmInput = ""
                                pinSetupError = ""
                                pinSetupStage = KioskPinSetupFlow.Stage.ENTER
                            },
                            text = "Back"
                        )
                    }
                    HeroTextButton(
                        onClick = { showKioskPinSetupDialog = false },
                        text = "Cancel"
                    )
                }
            }
        )
    }

    // --- VERIFY SECURE KIOSK MODE PIN TO UNLOCK ---
    if (showKioskPinVerifyDialog) {
        AlertDialog(
            onDismissRequest = { showKioskPinVerifyDialog = false },
            title = { Text("Disable Kiosk Mode", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter the Kiosk secure PIN code to release lockdown:")
                    // Keypad, not the IME: this gate must be usable without the system
                    // keyboard covering the dialog, and it keeps the PIN off a keyboard
                    // another customer could read over a shoulder.
                    PinPadEntry(
                        pin = pinVerifyInput,
                        onPinChange = { newPin ->
                            // Clear on every change so a wrong PIN is not left on screen
                            // for the next guess to read.
                            pinVerifyInput = newPin.take(SecurityHelper.PIN_LENGTH)
                            pinVerifyError = ""
                        },
                        isMasked = true,
                        isError = pinVerifyError.isNotEmpty(),
                        errorMessage = pinVerifyError.ifEmpty { null },
                        isEnabled = !kioskPinLockoutActive,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinVerifyError.isNotEmpty()) {
                        Text(pinVerifyError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                HeroButton(
                    enabled = pinVerifyInput.isNotEmpty() && !kioskPinLockoutActive,
                    onClick = {
                        viewModel.verifyKioskPinUnlock(
                            pin = pinVerifyInput,
                            onSuccess = {
                                viewModel.toggleKioskMode(false)
                                showKioskPinVerifyDialog = false
                                pinVerifyInput = ""
                                kioskPinLockoutActive = false
                                Toast.makeText(context, "Kiosk Mode released.", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { err ->
                                pinVerifyError = err
                                pinVerifyInput = ""
                                kioskPinLockoutActive = err.startsWith("Too many")
                            }
                        )
                    },
                    text = "Unlock"
                )
            },
            dismissButton = {
                HeroTextButton(
                    onClick = {
                        showKioskPinVerifyDialog = false
                        pinVerifyInput = ""
                        pinVerifyError = ""
                        kioskPinLockoutActive = false
                    },
                    text = "Cancel"
                )
            }
        )
    }

    // --- FAIL-CLOSED ADMIN FALLBACK: RELEASE LOCKDOWN WITH NO KIOSK PIN SET ---
    // SECURITY (issue #2): a terminal stuck in lockdown with no kiosk PIN must still
    // require a verified ADMIN credential. This is also the documented recovery path
    // for a forgotten kiosk PIN.
    if (showAdminFallbackAuthDialog) {
        AlertDialog(
            onDismissRequest = {
                showAdminFallbackAuthDialog = false
                adminAuthError = ""
            },
            title = { Text("Admin Authorization Required", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "No Kiosk security PIN is configured on this terminal, so lockdown " +
                            "cannot be released without an Admin account PIN."
                    )
                    if (adminAuthError.isNotEmpty()) {
                        Text(adminAuthError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    PinPadEntry(
                        pin = adminAuthPin,
                        onPinChange = {
                            adminAuthPin = it
                            adminAuthError = ""
                        },
                        isMasked = true,
                        isError = adminAuthError.isNotEmpty(),
                        errorMessage = adminAuthError.ifEmpty { null },
                        isEnabled = !adminAuthError.startsWith("Too many"),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                HeroButton(
                    enabled = adminAuthPin.isNotEmpty(),
                    onClick = {
                        viewModel.verifyKioskUnlock(
                            pin = adminAuthPin,
                            onSuccess = {
                                showAdminFallbackAuthDialog = false
                                adminAuthPin = ""
                                adminAuthError = ""
                                if (pendingKioskAction == KioskPendingAction.RELEASE_LOCKDOWN) {
                                    viewModel.toggleKioskMode(false)
                                    Toast.makeText(context, "Kiosk Mode released by Admin.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onFailure = { err ->
                                adminAuthError = err
                                adminAuthPin = ""
                            }
                        )
                    },
                    text = "Authorize & Release"
                )
            },
            dismissButton = {
                HeroTextButton(
                    onClick = {
                        showAdminFallbackAuthDialog = false
                        adminAuthError = ""
                        adminAuthPin = ""
                    },
                    text = "Cancel"
                )
            }
        )
    }

}
