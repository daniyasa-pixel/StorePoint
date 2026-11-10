package com.munzo.storepoint.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.munzo.storepoint.ui.StorePointViewModel
import com.munzo.storepoint.data.User
import com.munzo.storepoint.ui.layout.rememberWindowLayout
import com.munzo.storepoint.ui.layout.WindowLayout
import com.munzo.storepoint.ui.theme.ExpressiveButtonShape
import com.munzo.storepoint.ui.theme.ExpressiveCardShape
import com.munzo.storepoint.ui.theme.tactileBounce
import com.munzo.storepoint.ui.theme.glassPanel
import com.munzo.storepoint.ui.theme.ExpressiveOtpPinInput
import com.munzo.storepoint.ui.theme.PinPadEntry
import com.munzo.storepoint.ui.theme.shakeOnTrigger
import com.munzo.storepoint.util.SecurityHelper
import com.munzo.storepoint.ui.components.ExpressiveAction
import com.munzo.storepoint.ui.components.ExpressiveActionRow
import com.munzo.storepoint.ui.components.ExpressiveButton
import com.munzo.storepoint.ui.components.ExpressiveButtonSize
import com.munzo.storepoint.ui.components.ExpressiveButtonVariant

import com.munzo.storepoint.ui.components.*
import com.munzo.storepoint.ui.theme.HeroUITheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    viewModel: StorePointViewModel,
    onAboutNavigate: () -> Unit,
    onPinResetNavigate: () -> Unit,
    onLoginSuccess: (String) -> Unit // returns role: "ADMIN" or "CASHIER"
) {
    val context = LocalContext.current
    val storeConfig by viewModel.storeConfig.collectAsState()
    val users by viewModel.allUsers.collectAsState(initial = emptyList())

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isAuthenticating by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }

    // Tap counter on the SP logo
    var logoTapCount by remember { mutableStateOf(0) }
    var showLogoUnlockDialog by remember { mutableStateOf(false) }
    var logoUnlockInput by remember { mutableStateOf("") }
    var logoUnlockError by remember { mutableStateOf("") }

    // Support barcode scan simulation automatically
    if (showScanner) {
        com.munzo.storepoint.ui.BarcodeScannerDialog(
            products = emptyList(),
            onBarcodeScanned = { scannedBarcode ->
                showScanner = false
                isAuthenticating = true
                viewModel.loginWithBarcode(
                    barcodeId = scannedBarcode,
                    onSuccess = { user ->
                        isAuthenticating = false
                        Toast.makeText(context, "Logged in via Badge: Welcome ${user.username}!", Toast.LENGTH_SHORT).show()
                        onLoginSuccess(user.role)
                    },
                    onFailure = { errorMsg ->
                        isAuthenticating = false
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    }
                )
            },
            onDismiss = { showScanner = false }
        )
    }

    if (showLogoUnlockDialog) {
        AlertDialog(
            onDismissRequest = {
                showLogoUnlockDialog = false
                logoUnlockInput = ""
                logoUnlockError = ""
            },
            title = { Text("Kiosk Emergency Unlock", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.imePadding(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Enter the Admin 4-Digit PIN to immediately stop Lock Task Mode and return to normal operation.")
                    Spacer(Modifier.height(4.dp))
                    PinPadEntry(
                        pin = logoUnlockInput,
                        onPinChange = { logoUnlockInput = it },
                        isMasked = true,
                        isError = logoUnlockError.isNotEmpty(),
                        errorMessage = logoUnlockError.ifEmpty { null },
                        modifier = Modifier.fillMaxWidth().testTag("kiosk_unlock_pin_input")
                    )
                }
            },
            confirmButton = {
                HeroButton(
                    onClick = {
                        viewModel.verifyKioskUnlock(
                            pin = logoUnlockInput,
                            onSuccess = {
                                viewModel.toggleKioskMode(false)
                                showLogoUnlockDialog = false
                                
                                var actContext = context
                                var activity: android.app.Activity? = null
                                while (actContext is android.content.ContextWrapper) {
                                    if (actContext is android.app.Activity) {
                                        activity = actContext
                                        break
                                    }
                                    actContext = actContext.baseContext
                                }

                                if (activity != null) {
                                    try {
                                        activity.stopLockTask()
                                        Toast.makeText(context, "Kiosk Lockdown Mode successfully disabled by Admin.", Toast.LENGTH_LONG).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Kiosk Mode disabled (stopLockTask failed: ${e.localizedMessage})", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Kiosk Mode disabled in ViewModel.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onFailure = { err ->
                                logoUnlockError = err
                            }
                        )
                    },
                    text = "Unlock"
                )
            },
            dismissButton = {
                HeroTextButton(
                    onClick = {
                        showLogoUnlockDialog = false
                        logoUnlockInput = ""
                        logoUnlockError = ""
                    },
                    text = "Cancel"
                )
            }
        )
    }

    val heroWindowSize = rememberHeroWindowSize()
    val isExpanded = heroWindowSize == HeroWindowSize.Expanded
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val lastUser by viewModel.lastLoggedInUser.collectAsState()
    val pendingResetUsers by viewModel.usersPendingPinReset.collectAsState()

    val fragmentActivity = LocalContext.current as? androidx.fragment.app.FragmentActivity
    val biometricAvailability = remember(context) {
        com.munzo.storepoint.util.BiometricAuthHelper.checkAvailability(context)
    }
    val isBiometricAvailable = biometricAvailability.isAvailable

    val suppressedBiometric = remember { mutableStateOf(false) }
    val effectiveLastUser = if (suppressedBiometric.value) null else lastUser
    val biometricTargetUser = remember(effectiveLastUser, pendingResetUsers) {
        effectiveLastUser?.takeIf { it !in pendingResetUsers.map(User::username) }
    }
    val canOfferBiometric = isBiometricEnabled && isBiometricAvailable &&
        fragmentActivity != null && biometricTargetUser != null

    var loginError by remember { mutableStateOf("") }
    var shakeTrigger by remember { mutableIntStateOf(0) }

    fun attemptLogin(pin: String) {
        if (username.isBlank()) {
            loginError = "Enter your username first."
            return
        }
        isAuthenticating = true
        loginError = ""
        viewModel.login(
            username = username.trim(),
            pass = pin,
            onSuccess = { user ->
                isAuthenticating = false
                onLoginSuccess(user.role)
            },
            onFailure = { errorMsg ->
                isAuthenticating = false
                loginError = errorMsg
                password = ""
                shakeTrigger++
            }
        )
    }

    fun launchBiometric(target: String) {
        val host = fragmentActivity ?: return
        com.munzo.storepoint.util.BiometricAuthHelper.authenticate(
            activity = host,
            title = "Fingerprint Sign-In",
            subtitle = "Verify to open the register as $target",
            onOutcome = { outcome, message ->
                when (outcome) {
                    com.munzo.storepoint.util.BiometricAuthHelper.BiometricOutcome.SUCCESS -> Unit
                    com.munzo.storepoint.util.BiometricAuthHelper.BiometricOutcome.FALLBACK -> Unit
                    com.munzo.storepoint.util.BiometricAuthHelper.BiometricOutcome.UNAVAILABLE -> loginError = message
                    else -> loginError = message
                }
            },
            onSuccess = {
                isAuthenticating = true
                viewModel.loginWithBiometric(
                    targetUsername = target,
                    onSuccess = { user ->
                        isAuthenticating = false
                        onLoginSuccess(user.role)
                    },
                    onFailure = { err ->
                        isAuthenticating = false
                        loginError = err
                    }
                )
            }
        )
    }

    LaunchedEffect(canOfferBiometric, biometricTargetUser) {
        if (canOfferBiometric && !isAuthenticating) {
            biometricTargetUser?.let { launchBiometric(it) }
        }
    }

    LaunchedEffect(logoTapCount) {
        if (logoTapCount > 0) {
            kotlinx.coroutines.delay(3000L)
            logoTapCount = 0
        }
    }

    Scaffold(
        modifier = Modifier.imePadding()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(HeroUITheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            if (isExpanded) {
                // =========================================================================
                // HeroUI v3 Dual-Pane Responsive Layout for Tablets & Landscape POS Terminals
                // =========================================================================
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Hero Brand Panel
                    HeroCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        backgroundColor = HeroUITheme.colors.surface,
                        borderColor = HeroUITheme.colors.border
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(36.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .tactileBounce()
                                        .clickable {
                                            logoTapCount++
                                            if (logoTapCount >= 5) {
                                                logoTapCount = 0
                                                showLogoUnlockDialog = true
                                            }
                                        },
                                    shape = HeroUITheme.radius.xl,
                                    color = HeroUITheme.colors.accent,
                                    shadowElevation = 8.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "SP",
                                            color = HeroUITheme.colors.accentForeground,
                                            style = MaterialTheme.typography.displaySmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = (-0.5).sp
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = storeConfig?.storeName ?: "StorePoint POS",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = HeroUITheme.colors.foreground,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                HeroChip(
                                    text = "SECURE COMMERCIAL POS TERMINAL",
                                    color = HeroColor.Primary,
                                    variant = HeroChipVariant.Flat
                                )
                            }

                            // Quick Scan Badge Hero Card
                            HeroCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = HeroUITheme.colors.surfaceSecondary,
                                borderColor = HeroUITheme.colors.border
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = HeroUITheme.colors.accent,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        "Instant Cashier Badge Sign-In",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = HeroUITheme.colors.foreground
                                    )
                                    Text(
                                        "Hold your employee ID card to the barcode camera to unlock register access.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = HeroUITheme.colors.muted,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    HeroButton(
                                        onClick = { showScanner = true },
                                        modifier = Modifier.fillMaxWidth().testTag("scan_badge_login_button"),
                                        color = HeroColor.Primary,
                                        variant = HeroButtonVariant.Solid,
                                        size = HeroSize.Lg,
                                        icon = Icons.Default.QrCodeScanner,
                                        text = "Scan Staff Badge"
                                    )
                                }
                            }

                            HeroButton(
                                onClick = onAboutNavigate,
                                color = HeroColor.Default,
                                variant = HeroButtonVariant.Light,
                                size = HeroSize.Sm,
                                icon = Icons.Default.Info,
                                text = "About StorePoint & Compliance",
                                modifier = Modifier.testTag("about_storepoint_button")
                            )
                        }
                    }

                    // Right Credentials PIN Pad Panel
                    HeroCard(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                            .shakeOnTrigger(trigger = shakeTrigger),
                        backgroundColor = HeroUITheme.colors.surface,
                        borderColor = HeroUITheme.colors.border
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            HeroCardHeader(
                                title = "Sign In to Terminal",
                                subtitle = "Enter your username and 4-digit PIN",
                                icon = Icons.Default.Lock,
                                iconColor = HeroUITheme.colors.accent
                            )

                            if (pendingResetUsers.any { it.username.equals(username.trim(), ignoreCase = true) }) {
                                HeroCard(
                                    modifier = Modifier.fillMaxWidth().testTag("pin_reset_banner"),
                                    backgroundColor = HeroUITheme.colors.warning.copy(alpha = 0.12f),
                                    borderColor = HeroUITheme.colors.warning.copy(alpha = 0.3f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "This account still uses an old 6-digit PIN.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = HeroUITheme.colors.foreground,
                                            fontWeight = FontWeight.Medium
                                        )
                                        HeroButton(
                                            onClick = onPinResetNavigate,
                                            modifier = Modifier.fillMaxWidth().testTag("pin_reset_start_button"),
                                            color = HeroColor.Warning,
                                            variant = HeroButtonVariant.Flat,
                                            size = HeroSize.Md,
                                            icon = Icons.Default.LockReset,
                                            text = "Set up new 4-digit PIN"
                                        )
                                    }
                                }
                            }

                            HeroInput(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Account Username") },
                                leadingIcon = { Icon(Icons.Default.AccountCircle, null, tint = HeroUITheme.colors.accent) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("username_input"),
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
                                        "${SecurityHelper.PIN_LENGTH}-Digit Security PIN",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = HeroUITheme.colors.foreground
                                    )
                                    if (password.length == SecurityHelper.PIN_LENGTH) {
                                        HeroChip(
                                            text = "PIN Ready",
                                            color = HeroColor.Success,
                                            variant = HeroChipVariant.Flat
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                PinPadEntry(
                                    pin = password,
                                    onPinChange = { password = it },
                                    onPinComplete = { completed -> attemptLogin(completed) },
                                    isMasked = true,
                                    isError = loginError.isNotEmpty(),
                                    errorMessage = loginError.ifEmpty { null },
                                    isEnabled = !isAuthenticating,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("password_input")
                                )
                            }

                            HeroButton(
                                onClick = { attemptLogin(password) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("submit_login_button"),
                                color = HeroColor.Primary,
                                variant = HeroButtonVariant.Solid,
                                size = HeroSize.Lg,
                                icon = Icons.Default.Login,
                                text = if (isAuthenticating) "Authorizing…" else "Sign In",
                                enabled = !isAuthenticating
                            )

                            if (canOfferBiometric) {
                                HeroButton(
                                    onClick = { launchBiometric(biometricTargetUser!!) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("biometric_login_button"),
                                    color = HeroColor.Success,
                                    variant = HeroButtonVariant.Flat,
                                    size = HeroSize.Lg,
                                    icon = Icons.Default.Fingerprint,
                                    text = "Sign In with Fingerprint"
                                )
                            }

                            if (isBiometricAvailable && lastUser != null) {
                                HeroButton(
                                    onClick = {
                                        suppressedBiometric.value = true
                                        username = ""
                                        password = ""
                                        loginError = ""
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("switch_user_button"),
                                    color = HeroColor.Default,
                                    variant = HeroButtonVariant.Light,
                                    size = HeroSize.Sm,
                                    icon = Icons.Default.PersonOutline,
                                    text = "Sign in as someone else"
                                )
                            }
                        }
                    }
                }
            } else {
                // =========================================================================
                // HeroUI v3 Streamlined Single-Column Responsive Layout for Phones & Registers
                // =========================================================================
                Column(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Header Brand
                    Surface(
                        modifier = Modifier
                            .size(80.dp)
                            .tactileBounce()
                            .clickable {
                                logoTapCount++
                                if (logoTapCount >= 5) {
                                    logoTapCount = 0
                                    showLogoUnlockDialog = true
                                }
                            },
                        shape = HeroUITheme.radius.xl,
                        color = HeroUITheme.colors.accent,
                        shadowElevation = 6.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "SP",
                                color = HeroUITheme.colors.accentForeground,
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                )
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = storeConfig?.storeName ?: "StorePoint POS",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = HeroUITheme.colors.foreground,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        HeroChip(
                            text = "SECURE TERMINAL ACCESS",
                            color = HeroColor.Primary,
                            variant = HeroChipVariant.Flat
                        )
                    }

                    // Scan Badge Hero Card
                    HeroCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = HeroUITheme.colors.surface,
                        borderColor = HeroUITheme.colors.border
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Scan ID Badge for Instant Login",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = HeroUITheme.colors.accent
                            )
                            Text(
                                "Tap below to scan employee barcode ID badge via camera scanner.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = HeroUITheme.colors.muted
                            )
                            HeroButton(
                                onClick = { showScanner = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("scan_badge_login_button"),
                                color = HeroColor.Primary,
                                variant = HeroButtonVariant.Solid,
                                size = HeroSize.Md,
                                icon = Icons.Default.QrCodeScanner,
                                text = "Tap to Scan Badge"
                            )
                        }
                    }

                    // Divider
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeroDivider(modifier = Modifier.weight(1f), color = HeroUITheme.colors.border)
                        Text(
                            "OR ENTER PIN",
                            modifier = Modifier.padding(horizontal = 12.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = HeroUITheme.colors.muted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        HeroDivider(modifier = Modifier.weight(1f), color = HeroUITheme.colors.border)
                    }

                    // Credentials HeroCard
                    HeroCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shakeOnTrigger(trigger = shakeTrigger),
                        backgroundColor = HeroUITheme.colors.surface,
                        borderColor = HeroUITheme.colors.border
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (pendingResetUsers.any { it.username.equals(username.trim(), ignoreCase = true) }) {
                                HeroCard(
                                    modifier = Modifier.fillMaxWidth().testTag("pin_reset_banner"),
                                    backgroundColor = HeroUITheme.colors.warning.copy(alpha = 0.12f),
                                    borderColor = HeroUITheme.colors.warning.copy(alpha = 0.3f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "This account still uses an old 6-digit PIN.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = HeroUITheme.colors.foreground
                                        )
                                        HeroButton(
                                            onClick = onPinResetNavigate,
                                            modifier = Modifier.fillMaxWidth().testTag("pin_reset_start_button"),
                                            color = HeroColor.Warning,
                                            variant = HeroButtonVariant.Flat,
                                            size = HeroSize.Md,
                                            icon = Icons.Default.LockReset,
                                            text = "Set up new 4-digit PIN"
                                        )
                                    }
                                }
                            }

                            HeroInput(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Account Username") },
                                leadingIcon = { Icon(Icons.Default.AccountCircle, null, tint = HeroUITheme.colors.accent) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("username_input"),
                                singleLine = true,
                                shape = HeroUITheme.radius.md
                            )

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "${SecurityHelper.PIN_LENGTH}-Digit Security PIN",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HeroUITheme.colors.foreground
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                PinPadEntry(
                                    pin = password,
                                    onPinChange = { password = it },
                                    onPinComplete = { completed -> attemptLogin(completed) },
                                    isMasked = true,
                                    isError = loginError.isNotEmpty(),
                                    errorMessage = loginError.ifEmpty { null },
                                    isEnabled = !isAuthenticating,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("password_input")
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when {
                                        isAuthenticating -> "Verifying…"
                                        password.length == SecurityHelper.PIN_LENGTH -> "✓ PIN ready — verifying automatically"
                                        else -> "${password.length}/${SecurityHelper.PIN_LENGTH} digits"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        isAuthenticating -> HeroUITheme.colors.accent
                                        password.length == SecurityHelper.PIN_LENGTH -> HeroUITheme.colors.success
                                        else -> HeroUITheme.colors.muted
                                    }
                                )
                            }

                            HeroButton(
                                onClick = { attemptLogin(password) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("submit_login_button"),
                                color = HeroColor.Primary,
                                variant = HeroButtonVariant.Solid,
                                size = HeroSize.Lg,
                                icon = Icons.Default.Login,
                                text = if (isAuthenticating) "Authorizing…" else "Sign In",
                                enabled = !isAuthenticating
                            )
                        }
                    }

                    // Secondary action buttons
                    if (canOfferBiometric) {
                        HeroButton(
                            onClick = { launchBiometric(biometricTargetUser!!) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("biometric_login_button"),
                            color = HeroColor.Success,
                            variant = HeroButtonVariant.Flat,
                            size = HeroSize.Md,
                            icon = Icons.Default.Fingerprint,
                            text = "Sign in with fingerprint"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isBiometricAvailable && lastUser != null) {
                            HeroButton(
                                onClick = {
                                    suppressedBiometric.value = true
                                    username = ""
                                    password = ""
                                    loginError = ""
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("switch_user_button"),
                                color = HeroColor.Default,
                                variant = HeroButtonVariant.Bordered,
                                size = HeroSize.Sm,
                                icon = Icons.Default.PersonOutline,
                                text = "Switch User"
                            )
                        }
                        HeroButton(
                            onClick = onAboutNavigate,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("about_storepoint_button"),
                            color = HeroColor.Default,
                            variant = HeroButtonVariant.Bordered,
                            size = HeroSize.Sm,
                            icon = Icons.Default.Info,
                            text = "About"
                        )
                    }
                }
            }
        }
    }
}
