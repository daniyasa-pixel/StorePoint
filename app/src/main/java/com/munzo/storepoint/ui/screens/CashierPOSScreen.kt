package com.munzo.storepoint.ui.screens

import android.widget.Toast
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.munzo.storepoint.ui.theme.ExpressiveButtonShape
import com.munzo.storepoint.ui.theme.ExpressiveSectionHeader
import com.munzo.storepoint.ui.components.staggeredEnter
import com.munzo.storepoint.ui.theme.ExpressiveChipShape
import com.munzo.storepoint.ui.theme.ExpressiveSmallCardShape
import com.munzo.storepoint.ui.theme.expressiveGlassCard
import com.munzo.storepoint.ui.theme.ExpressiveSplitButton
import com.munzo.storepoint.ui.theme.ExpressiveButtonGroup
import com.munzo.storepoint.ui.theme.glassPanel
import com.munzo.storepoint.ui.theme.tactileBounce
import com.munzo.storepoint.ui.theme.GCashColor
import com.munzo.storepoint.ui.theme.MayaColor
import com.munzo.storepoint.ui.theme.LoadColor
import com.munzo.storepoint.ui.theme.HeroUITheme
import com.munzo.storepoint.ui.components.*
import com.munzo.storepoint.ui.components.ExpressiveButton
import com.munzo.storepoint.ui.components.ExpressiveButtonSize
import com.munzo.storepoint.ui.components.ExpressiveButtonVariant
import com.munzo.storepoint.ui.layout.ScanSearchField
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import com.munzo.storepoint.data.*
import com.munzo.storepoint.ui.BarcodeScannerDialog
import com.munzo.storepoint.ui.KioskStatusBar
import com.munzo.storepoint.ui.StorePointViewModel
import com.munzo.storepoint.util.PdfExportUtil
import com.munzo.storepoint.util.DigitalServicesHelper
import com.munzo.storepoint.util.PendingDigitalService
import com.munzo.storepoint.util.APP_VERSION
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierPOSScreen(
    viewModel: StorePointViewModel,
    onAdminDashboardNavigate: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val storeConfig by viewModel.storeConfig.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()

    val categories by viewModel.allCategories.collectAsState()
    val products by viewModel.allProducts.collectAsState()
    val cartItems by viewModel.cart.collectAsState()
    val parkedList by viewModel.parkedTransactions.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    // LAYA fast-response: debounce keystrokes so heavy catalog filtering recomposes ~7x/sec, not per keypress.
    var debouncedSearchQuery by remember { mutableStateOf("") }
    LaunchedEffect(searchQuery) { kotlinx.coroutines.delay(140); debouncedSearchQuery = searchQuery }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    val categoryNameById = remember(categories) { categories.associate { it.id to it.name } }
    var activePortraitTab by remember { mutableStateOf(0) }
    var activeCatalogSection by remember { mutableStateOf(0) } // 0 = Standard Products, 1 = Digital Services

    // --- Restored-basket notice -------------------------------------------------
    // Silently repopulating the cart would leave the cashier wondering where the
    // items came from (Recognition over Recall). Acknowledge the restore explicitly,
    // then clear the flag so it cannot re-fire on recomposition.
    val restoredLineCount = viewModel.restoredCartLineCount
    var showRestoredNotice by remember { mutableStateOf(false) }
    LaunchedEffect(restoredLineCount) {
        if (restoredLineCount > 0) {
            showRestoredNotice = true
            viewModel.acknowledgeCartRestore()
        }
    }
    // Auto-dismiss after a few seconds; the information is confirmatory, not blocking.
    LaunchedEffect(showRestoredNotice) {
        if (showRestoredNotice) {
            kotlinx.coroutines.delay(4000)
            showRestoredNotice = false
        }
    }

    val isProductsLoading = products.isEmpty() && categories.isEmpty()

    // Dialog flags
    var showShiftStartDialog by remember { mutableStateOf(false) }
    var showShiftCloseDialog by remember { mutableStateOf(false) }
    var showActiveShiftLogoutWarning by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showParkDialog by remember { mutableStateOf(false) }
    var showParkedTxDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var showReturnRefundDialog by remember { mutableStateOf(false) }
    var showBulkProductionDialog by remember { mutableStateOf(false) }
    var showDrawerCashInOutDialog by remember { mutableStateOf(false) }
    var showFastCashierSwitchDialog by remember { mutableStateOf(false) }
    var productForVariantPicker by remember { mutableStateOf<com.munzo.storepoint.data.Product?>(null) }
    var productForDiscountDialog by remember { mutableStateOf<com.munzo.storepoint.data.Product?>(null) }
    var lastScannedBarcodeFeedback by remember { mutableStateOf<String?>(null) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showCallAdminDialog by remember { mutableStateOf(false) }
    var showTerminalAppsDialog by remember { mutableStateOf(false) }
    var showPostCheckoutDigitalDialog by remember { mutableStateOf(false) }
    var pendingDigitalServices by remember { mutableStateOf<List<PendingDigitalService>>(emptyList()) }
    val isScannerOpen by viewModel.isScannerOpen.collectAsState()
    val isKioskActive by viewModel.isKioskModeActive.collectAsState()

    // Last completed transaction for receipt viewing
    var lastCompletedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var lastCompletedItems by remember { mutableStateOf<List<TransactionItem>>(emptyList()) }

    // Shift drawer local states
    var initialCashInput by remember { mutableStateOf("100.00") }
    var initialGCashInput by remember { mutableStateOf("0.00") }
    var initialSmartLoadInput by remember { mutableStateOf("0.00") }
    var initialGlobeLoadInput by remember { mutableStateOf("0.00") }
    var closeCashInput by remember { mutableStateOf("250.00") }
    var closeGCashInput by remember { mutableStateOf("0.00") }
    var closeSmartLoadInput by remember { mutableStateOf("0.00") }
    var closeGlobeLoadInput by remember { mutableStateOf("0.00") }

    // Scanner inline controllers state
    var scannerQuantityInput by remember { mutableStateOf("1") }
    var manualBarcodeInput by remember { mutableStateOf("") }

    // Park/Hold note input
    var parkNoteInput by remember { mutableStateOf("") }

    // Active configuration currencies
    val curr = storeConfig?.currencySymbol ?: "$"
    var isSensitiveDataVisible by remember { mutableStateOf(true) }

    // Drawer scope & State Navigation
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val checkedCartItems = remember { mutableStateMapOf<Int, Boolean>() }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    var longPressedCartItem by remember { mutableStateOf<com.munzo.storepoint.data.Product?>(null) }
    var itemPendingDelete by remember { mutableStateOf<com.munzo.storepoint.data.Product?>(null) }

    val selectedCartUoms by viewModel.selectedCartUoms.collectAsState()
    val cartItemDiscounts by viewModel.cartItemDiscounts.collectAsState()

    // Calculations based exclusively on CHECKED shopping cart items.
    // LAYA fast-response fix: plain derivedStateOf (not nested in remember) so cart
    // + checkbox changes always trigger exactly one recompute, never a stale snapshot.
    val activeCartItems by remember(cartItems, checkedCartItems) {
        derivedStateOf {
            cartItems.filter { checkedCartItems[it.first.id] != false }
        }
    }
    val doubleTaxRate = storeConfig?.taxPercentage ?: 12.0
    val grossCartTotal = activeCartItems.sumOf { 
        val uom = selectedCartUoms[it.first.id] ?: com.munzo.storepoint.data.UomOption("Base Unit", 1, it.first.price)
        val discount = cartItemDiscounts[it.first.id] ?: 0.0
        val effectivePrice = (uom.price - discount).coerceAtLeast(0.0)
        effectivePrice * it.second
    }
    // Philippine VAT: retail prices displayed on product tags already include the 12% Value-Added Tax.
    // Formula: amount / 1.12 x .12
    val taxAmount = StorePointRepository.calculateVatFromInclusive(grossCartTotal, doubleTaxRate)
    val subtotal = StorePointRepository.calculateVatableSalesFromInclusive(grossCartTotal, doubleTaxRate)
    val totalAmount = StorePointRepository.roundMoney(grossCartTotal)

    // Session-check: Trigger drawer open state if activeSession is null
    LaunchedEffect(activeSession, storeConfig) {
        if (activeSession == null) {
            showShiftStartDialog = true
            storeConfig?.let {
                initialGCashInput = String.format(Locale.getDefault(), "%.2f", it.gcashBalance)
                initialSmartLoadInput = String.format(Locale.getDefault(), "%.2f", it.smartLoadBalance)
                initialGlobeLoadInput = String.format(Locale.getDefault(), "%.2f", it.globeLoadBalance)
            }
        }
    }

    // Global Key Events / Hotkeys listener modifier
    val hotkeyModifier = Modifier.onKeyEvent { keyEvent ->
        if (keyEvent.type == KeyEventType.KeyDown) {
            when (keyEvent.key) {
                Key.F4 -> { // Park transaction hotkey
                    if (cartItems.isNotEmpty()) {
                        parkNoteInput = "Parked at ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}"
                        showParkDialog = true
                    } else {
                        Toast.makeText(context, "Nothing to park. Cart is empty.", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                Key.F9 -> { // Payment process hotkey
                    if (activeCartItems.isNotEmpty() && activeSession != null) {
                        showPaymentDialog = true
                    } else if (activeSession == null) {
                        Toast.makeText(context, "Shift session must be active to pay.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "No checked items in cart to checkout.", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        } else {
            false
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                // Adaptive width: full 85% on narrow phones, capped at 320dp on wider screens
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .widthIn(max = 320.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Drawer Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(52.dp),
                            shape = ExpressiveButtonShape,
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 3.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "SP",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = storeConfig?.storeName ?: "StorePoint POS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.2).sp
                            )
                            Text(
                                text = "Secure Cashier Drawer",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    HeroDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = "ACTIVE CASHIER PROFILE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .expressiveGlassCard(cornerRadius = 16.dp, elevation = 1.dp, accentGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(14.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(42.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeUser?.username ?: "Cashier",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = activeUser?.role ?: "CASHIER",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        FilledTonalIconButton(
                            onClick = { showFastCashierSwitchDialog = true },
                            modifier = Modifier.size(36.dp).testTag("switch_cashier_btn")
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Fast Cashier Switch", modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "SECURE CASH & DIGITAL BALANCES",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline,
                        letterSpacing = 0.5.sp
                    )

                    activeSession?.let { session ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Shift Started", style = MaterialTheme.typography.bodySmall)
                                val startTimeFormatted = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(session.startTime))
                                Text(
                                    text = if (isSensitiveDataVisible) startTimeFormatted else "••:••",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Starting cash drawer", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = if (isSensitiveDataVisible) "$curr${String.format(Locale.getDefault(), "%.2f", session.startingCash)}" else "$curr••••",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                TextButton(
                                    onClick = { showDrawerCashInOutDialog = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp).testTag("quick_cash_in_out_btn")
                                ) {
                                    Icon(Icons.Default.PointOfSale, contentDescription = "Cash In/Out", modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cash In/Out", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            if (storeConfig?.hasGCash == true) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("GCash Balance", style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            text = if (isSensitiveDataVisible) "$curr${String.format(Locale.getDefault(), "%.2f", storeConfig?.gcashBalance ?: 0.0)}" else "$curr••••",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }

                                    var showAdjustGCashDialogDrawer by remember { mutableStateOf(false) }
                                    TextButton(
                                        onClick = { showAdjustGCashDialogDrawer = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp).testTag("drawer_adjust_gcash_btn")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit GCash Balance", modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Adjust", style = MaterialTheme.typography.labelSmall)
                                    }

                                    if (showAdjustGCashDialogDrawer) {
                                        var tempBalStr by remember { mutableStateOf(storeConfig?.gcashBalance?.toString() ?: "0.00") }
                                        AlertDialog(
                                            onDismissRequest = { showAdjustGCashDialogDrawer = false },
                                            title = { Text("Update GCash Balance") },
                                            text = {
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text("Enter the accurate secondary GCash digital balance of this outlet's retailing account:", style = MaterialTheme.typography.bodyMedium)
                                                    HeroInput(
                                                        value = tempBalStr,
                                                        onValueChange = { tempBalStr = it },
                                                        label = { Text("GCash Balance ($curr)") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier.fillMaxWidth().testTag("temp_gcash_balance_field")
                                                    )
                                                }
                                            },
                                            confirmButton = {
                                                Button(
                                                    onClick = {
                                                        val parsedBal = tempBalStr.toDoubleOrNull() ?: storeConfig?.gcashBalance ?: 0.0
                                                        viewModel.updateGCashBalance(parsedBal)
                                                        showAdjustGCashDialogDrawer = false
                                                    }
                                                ) {
                                                    Text("Save")
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showAdjustGCashDialogDrawer = false }) {
                                                    Text("Cancel")
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            if (storeConfig?.hasLoad == true) {
                                // Smart / TNT Retailer Load Balance
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Smart/TNT Load Wallet", style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            text = if (isSensitiveDataVisible) "$curr${String.format(Locale.getDefault(), "%.2f", storeConfig?.smartLoadBalance ?: 0.0)}" else "$curr••••",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    var showAdjustSmartDialogDrawer by remember { mutableStateOf(false) }
                                    TextButton(
                                        onClick = { showAdjustSmartDialogDrawer = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp).testTag("drawer_adjust_smart_btn")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Smart Load Balance", modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Adjust", style = MaterialTheme.typography.labelSmall)
                                    }

                                    if (showAdjustSmartDialogDrawer) {
                                        var tempBalStr by remember { mutableStateOf(storeConfig?.smartLoadBalance?.toString() ?: "0.00") }
                                        AlertDialog(
                                            onDismissRequest = { showAdjustSmartDialogDrawer = false },
                                            title = { Text("Update Smart/TNT Load Wallet") },
                                            text = {
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text("Enter current retailer load wallet balance for Smart/TNT (2% rebate active):", style = MaterialTheme.typography.bodyMedium)
                                                    HeroInput(
                                                        value = tempBalStr,
                                                        onValueChange = { tempBalStr = it },
                                                        label = { Text("Smart Load Balance ($curr)") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier.fillMaxWidth().testTag("temp_smart_balance_field")
                                                    )
                                                }
                                            },
                                            confirmButton = {
                                                Button(
                                                    onClick = {
                                                        val parsedBal = tempBalStr.toDoubleOrNull() ?: storeConfig?.smartLoadBalance ?: 0.0
                                                        viewModel.updateSmartLoadBalance(parsedBal)
                                                        showAdjustSmartDialogDrawer = false
                                                    }
                                                ) {
                                                    Text("Save")
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showAdjustSmartDialogDrawer = false }) {
                                                    Text("Cancel")
                                                }
                                            }
                                        )
                                    }
                                }

                                // Globe / TM Retailer Load Balance
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Globe/TM Load Wallet", style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            text = if (isSensitiveDataVisible) "$curr${String.format(Locale.getDefault(), "%.2f", storeConfig?.globeLoadBalance ?: 0.0)}" else "$curr••••",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }

                                    var showAdjustGlobeDialogDrawer by remember { mutableStateOf(false) }
                                    TextButton(
                                        onClick = { showAdjustGlobeDialogDrawer = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp).testTag("drawer_adjust_globe_btn")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Globe Load Balance", modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Adjust", style = MaterialTheme.typography.labelSmall)
                                    }

                                    if (showAdjustGlobeDialogDrawer) {
                                        var tempBalStr by remember { mutableStateOf(storeConfig?.globeLoadBalance?.toString() ?: "0.00") }
                                        AlertDialog(
                                            onDismissRequest = { showAdjustGlobeDialogDrawer = false },
                                            title = { Text("Update Globe/TM Load Wallet") },
                                            text = {
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text("Enter current retailer load wallet balance for Globe/TM (2% rebate active):", style = MaterialTheme.typography.bodyMedium)
                                                    HeroInput(
                                                        value = tempBalStr,
                                                        onValueChange = { tempBalStr = it },
                                                        label = { Text("Globe Load Balance ($curr)") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier.fillMaxWidth().testTag("temp_globe_balance_field")
                                                    )
                                                }
                                            },
                                            confirmButton = {
                                                Button(
                                                    onClick = {
                                                        val parsedBal = tempBalStr.toDoubleOrNull() ?: storeConfig?.globeLoadBalance ?: 0.0
                                                        viewModel.updateGlobeLoadBalance(parsedBal)
                                                        showAdjustGlobeDialogDrawer = false
                                                    }
                                                ) {
                                                    Text("Save")
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showAdjustGlobeDialogDrawer = false }) {
                                                    Text("Cancel")
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HeroDivider()

                    // Returns, Refunds & Exchanges Launcher
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.AssignmentReturn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        label = { Text("Returns & Refunds", fontWeight = FontWeight.SemiBold) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showReturnRefundDialog = true
                        },
                        modifier = Modifier.testTag("drawer_returns_refunds")
                    )

                    // Bulk-to-Retail Repackaging & Production (BOM Engine)
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Transform, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        label = { Text("Bulk Repackaging (BOM)", fontWeight = FontWeight.SemiBold) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showBulkProductionDialog = true
                        },
                        modifier = Modifier.testTag("drawer_bulk_production")
                    )

                    // Drawer Cash Management (Cash In / Out & Petty Cash)
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.PointOfSale, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        label = { Text("Cash In / Cash Out", fontWeight = FontWeight.SemiBold) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showDrawerCashInOutDialog = true
                        },
                        modifier = Modifier.testTag("drawer_cash_in_out")
                    )

                    // Fast Cashier PIN Switching
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        label = { Text("Switch Staff / Cashier", fontWeight = FontWeight.SemiBold) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showFastCashierSwitchDialog = true
                        },
                        modifier = Modifier.testTag("drawer_switch_cashier")
                    )

                    // Call Admin / Supervisor Assistance Request (Cashiers only, strictly hidden on admin accounts)
                    if (activeUser?.role?.equals("ADMIN", ignoreCase = true) != true) {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Call Admin", fontWeight = FontWeight.Bold)
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        modifier = Modifier.padding(start = 4.dp)
                                    ) {
                                        Text(
                                            text = "HELP",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 0.5.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                showCallAdminDialog = true
                            },
                            modifier = Modifier.testTag("drawer_call_admin")
                        )
                    }

                    // Admin Dashboard navigation in Drawer if they are admin
                    if (activeUser?.role == "ADMIN") {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                            label = { Text("Admin Dashboard") },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onAdminDashboardNavigate()
                            },
                            modifier = Modifier.testTag("drawer_admin_dashboard")
                        )
                    }

                    // Shift session actions — single, non-duplicated source for shift + terminal actions.
                    HeroDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Shift Session",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = CircleShape,
                            color = if (activeSession != null) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = if (activeSession != null) "SHIFT ACTIVE" else "NO ACTIVE SHIFT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = if (activeSession != null) MaterialTheme.colorScheme.onErrorContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    HeroButton(
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            if (activeSession != null) {
                                showShiftCloseDialog = true
                            } else {
                                Toast.makeText(context, "No active shift running. Start a shift first.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drawer_end_shift_btn"),
                        text = "End of Day Close Shift",
                        icon = Icons.Default.Assessment,
                        color = HeroColor.Danger,
                        variant = HeroButtonVariant.Solid,
                        size = HeroSize.Md,
                        enabled = activeSession != null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HeroButton(
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            if (activeSession != null) {
                                showActiveShiftLogoutWarning = true
                            } else {
                                viewModel.logout()
                                onLogout()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drawer_logout_btn"),
                        text = "Log Out Terminal",
                        icon = Icons.Default.ExitToApp,
                        color = HeroColor.Default,
                        variant = HeroButtonVariant.Bordered,
                        size = HeroSize.Md
                    )


                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "StorePoint POS Terminal v$APP_VERSION",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = "Developed by uznom (GitHub: @uznom)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 8.dp)
                            .clickable {
                                coroutineScope.launch { drawerState.close() }
                                showAboutDialog = true
                            }
                    )
                }
            }
        },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(hotkeyModifier)
            ) {
                Scaffold(
                    topBar = {
                        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                            if (isKioskActive) {
                                KioskStatusBar(viewModel = viewModel)
                            }
                            TopAppBar(
                                windowInsets = if (isKioskActive) WindowInsets(0, 0, 0, 0) else TopAppBarDefaults.windowInsets,
                                navigationIcon = {
                                    IconButton(onClick = {
                                        coroutineScope.launch { drawerState.open() }
                                    }, modifier = Modifier.testTag("hamburger_menu_button")) {
                                        Icon(Icons.Default.Menu, contentDescription = "Open Sidebar Navigation")
                                    }
                                },
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // SP logo
                                        Surface(
                                            modifier = Modifier.size(40.dp),
                                            shape = ExpressiveButtonShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            shadowElevation = 2.dp
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "SP",
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f, fill = false)) {
                                            Text(
                                                text = storeConfig?.storeName ?: "StorePoint POS",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Transacting: ${activeUser?.username ?: "Cashier"}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    if (activeUser?.role?.equals("ADMIN", ignoreCase = true) != true) {
                                        IconButton(
                                            onClick = { showCallAdminDialog = true },
                                            modifier = Modifier.testTag("top_bar_call_admin_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AdminPanelSettings,
                                                contentDescription = "Call Admin / Supervisor Assistance",
                                                tint = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { showReturnRefundDialog = true },
                                        modifier = Modifier.testTag("top_bar_return_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AssignmentReturn,
                                            contentDescription = "Returns & Refunds"
                                        )
                                    }
                                    IconButton(
                                        onClick = { showParkedTxDialog = true },
                                        modifier = Modifier.testTag("parked_sidebar_button")
                                    ) {
                                        BadgedBox(
                                            badge = {
                                                if (parkedList.isNotEmpty()) {
                                                    Badge { Text("${parkedList.size}") }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.HourglassEmpty,
                                                contentDescription = "Held Transactions (${parkedList.size})"
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { showTerminalAppsDialog = true },
                                        modifier = Modifier.testTag("top_bar_terminal_apps_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Apps,
                                            contentDescription = "Terminal Apps"
                                        )
                                    }
                                    IconButton(
                                        onClick = { showAboutDialog = true },
                                        modifier = Modifier.testTag("top_bar_about_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "About Creator"
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    // Main Workspace Layout with BoxWithConstraints for superb responsive design on mobile portrait
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .imePadding()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        val screenMaxWidth = maxWidth
                        val isPortrait = screenMaxWidth < 650.dp

                    // Left Workspace Content: Catalog Search, Categories & Grid
                    @Composable
                    fun SariSariCatalogWorkspace() {
                        val showDigitalCategory = (storeConfig?.hasGCash == true) || (storeConfig?.hasMaya == true) || (storeConfig?.hasLoad == true)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            if (showDigitalCategory && !isPortrait) {
                                HeroSegmentedTabs(
                                    items = listOf("Standard Products", "Digital Services"),
                                    selectedItem = if (activeCatalogSection == 0) "Standard Products" else "Digital Services",
                                    onItemSelected = { selected ->
                                        val idx = if (selected == "Standard Products") 0 else 1
                                        activeCatalogSection = idx
                                        if (isPortrait) activePortraitTab = idx + 1
                                    },
                                    labelProvider = { it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                )
                            }

                            if (activeCatalogSection == 1 && showDigitalCategory) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    DigitalServicesForm(
                                        curr = curr,
                                        initialService = "GCash",
                                        initialNetwork = "Smart",
                                        onAddProductToCart = { dynamicProduct ->
                                            viewModel.addToCart(dynamicProduct)
                                        },
                                        initialGCashBalance = storeConfig?.gcashBalance ?: 0.0,
                                        smartLoadBalance = storeConfig?.smartLoadBalance ?: 0.0,
                                        globeLoadBalance = storeConfig?.globeLoadBalance ?: 0.0,
                                        isSensitiveDataVisible = isSensitiveDataVisible,
                                        hasGCash = storeConfig?.hasGCash ?: true,
                                        hasMaya = storeConfig?.hasMaya ?: true,
                                        hasLoad = storeConfig?.hasLoad ?: true,
                                        defaultLoadFee = storeConfig?.loadServiceFee ?: 2.0,
                                        defaultGcashFee = storeConfig?.gcashServiceFee ?: 10.0,
                                        defaultMayaFee = storeConfig?.mayaServiceFee ?: 10.0,
                                        defaultMayaBankFee = storeConfig?.mayaBankFee ?: 15.0,
                                        digitalCategoryId = categories.find { it.name.contains("GCash", ignoreCase = true) || it.name.contains("Digital", ignoreCase = true) }?.id ?: categories.firstOrNull()?.id ?: 1
                                    )
                                }
                            } else {
                            // Google M3 Expressive Pill Search Bar with integrated Scan button.
                            // The label/placeholder resolve from the width this field actually
                            // gets, so a narrow phone or split-screen window degrades the copy
                            // instead of ellipsing it. See ScanSearchField.
                            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                val searchCopy = ScanSearchField.copyFor(maxWidth)
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(searchCopy.placeholder, style = MaterialTheme.typography.bodyMedium, color = HeroUITheme.colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = HeroUITheme.colors.muted
                                    )
                                },
                                trailingIcon = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Clear search",
                                                    tint = HeroUITheme.colors.muted
                                                )
                                            }
                                        }
                                        FilledTonalIconButton(
                                            onClick = { viewModel.isScannerOpen.value = true },
                                            modifier = Modifier.testTag("barcode_cam_trigger"),
                                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                containerColor = HeroUITheme.colors.accent.copy(alpha = 0.14f),
                                                contentColor = HeroUITheme.colors.accent
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.QrCodeScanner,
                                                contentDescription = "Scan",
                                                tint = HeroUITheme.colors.accent
                                            )
                                        }
                                    }
                                },
                                shape = HeroUITheme.radius.full,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = HeroUITheme.colors.surfaceSecondary,
                                    unfocusedContainerColor = HeroUITheme.colors.surfaceSecondary,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, HeroUITheme.colors.border, HeroUITheme.radius.full)
                                    .padding(bottom = 8.dp)
                                    .testTag("catalog_search_bar"),
                                singleLine = true
                            )
                            } // end BoxWithConstraints for adaptive search copy

                             // HeroUI v3 Category Filter Row
                             Row(
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .horizontalScroll(rememberScrollState())
                                     .padding(vertical = 6.dp),
                                 verticalAlignment = Alignment.CenterVertically,
                                 horizontalArrangement = Arrangement.spacedBy(6.dp)
                             ) {
                                 Text(
                                     "Filter: ",
                                     style = MaterialTheme.typography.labelMedium,
                                     color = HeroUITheme.colors.muted
                                 )

                                 // "All" chip
                                 HeroChip(
                                     text = "All",
                                     variant = if (selectedCategory == null) HeroChipVariant.Solid else HeroChipVariant.Flat,
                                     color = if (selectedCategory == null) HeroColor.Primary else HeroColor.Default,
                                     onClick = { selectedCategory = null },
                                     modifier = Modifier.testTag("cat_chip_all")
                                 )

                                 val showDigitalCategory = (storeConfig?.hasGCash == true) || (storeConfig?.hasMaya == true) || (storeConfig?.hasLoad == true)
                                 categories.filterNot { cat ->
                                     cat.name.contains("Digital", ignoreCase = true) || cat.name.contains("Service", ignoreCase = true)
                                 }.forEach { cat ->
                                     val isSelected = selectedCategory?.id == cat.id
                                     HeroChip(
                                         text = cat.name,
                                         icon = if (cat.iconBadge.isNotBlank()) getCategoryIconVector(cat.iconBadge) else null,
                                         variant = if (isSelected) HeroChipVariant.Solid else HeroChipVariant.Flat,
                                         color = if (isSelected) HeroColor.Primary else HeroColor.Default,
                                         onClick = { selectedCategory = cat },
                                         modifier = Modifier.testTag("cat_chip_${cat.id}")
                                     )
                                 }
                             }

                            // Product Grid Catalog — LAYA fast-response: memoized, debounced, O(1) category map.
                            val categoryNameById = remember(categories) { categories.associate { it.id to it.name } }
                            val effectiveQuery = debouncedSearchQuery.trim()
                            val filteredProducts = remember(products, categoryNameById, selectedCategory, effectiveQuery) {
                                products.filter { p ->
                                val categoryName = categoryNameById[p.categoryId] ?: ""
                                val isOldDigital = p.name.contains("GCash Cash-In", ignoreCase = true) || 
                                                   p.name.contains("GCash Cash-Out", ignoreCase = true) || 
                                                   p.name.contains("Smart Load", ignoreCase = true) ||
                                                   categoryName.contains("Digital", ignoreCase = true)
                                
                                !isOldDigital && (selectedCategory == null || p.categoryId == selectedCategory?.id) &&
                                        (effectiveQuery.isBlank() || p.name.contains(effectiveQuery, ignoreCase = true) || p.barcode.contains(effectiveQuery, ignoreCase = true) || categoryName.contains(effectiveQuery, ignoreCase = true))
                            }
                        }

                            val searchQueryMatchedDigital = effectiveQuery.isNotEmpty() && (
                                effectiveQuery.contains("gcash", ignoreCase = true) || 
                                effectiveQuery.contains("g-cash", ignoreCase = true) || 
                                effectiveQuery.contains("g cash", ignoreCase = true) || 
                                effectiveQuery.contains("maya", ignoreCase = true) || 
                                effectiveQuery.contains("load", ignoreCase = true) || 
                                effectiveQuery.contains("smart", ignoreCase = true) || 
                                effectiveQuery.contains("tnt", ignoreCase = true) || 
                                effectiveQuery.contains("globe", ignoreCase = true) || 
                                effectiveQuery.contains("tm", ignoreCase = true)
                            )
                            val isDigitalActive = searchQueryMatchedDigital

                            if (isDigitalActive) {
                                val initialService = when {
                                    effectiveQuery.contains("gcash", ignoreCase = true) || effectiveQuery.contains("g-cash", ignoreCase = true) || effectiveQuery.contains("g cash", ignoreCase = true) -> "GCash"
                                    effectiveQuery.contains("maya", ignoreCase = true) -> "Maya"
                                    effectiveQuery.contains("load", ignoreCase = true) || effectiveQuery.contains("smart", ignoreCase = true) || effectiveQuery.contains("tnt", ignoreCase = true) || effectiveQuery.contains("globe", ignoreCase = true) || effectiveQuery.contains("tm", ignoreCase = true) -> "Load"
                                    else -> "GCash"
                                }
                                val initialNetwork = when {
                                    effectiveQuery.contains("smart", ignoreCase = true) -> "Smart"
                                    effectiveQuery.contains("tnt", ignoreCase = true) -> "TNT"
                                    effectiveQuery.contains("globe", ignoreCase = true) -> "Globe"
                                    effectiveQuery.contains("tm", ignoreCase = true) -> "TM"
                                    else -> "Smart"
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    DigitalServicesForm(
                                        curr = curr,
                                        initialService = initialService,
                                        initialNetwork = initialNetwork,
                                        onAddProductToCart = { dynamicProduct ->
                                            viewModel.addToCart(dynamicProduct)
                                            searchQuery = "" // Clear search so catalog resets
                                        },
                                        initialGCashBalance = storeConfig?.gcashBalance ?: 0.0,
                                        smartLoadBalance = storeConfig?.smartLoadBalance ?: 0.0,
                                        globeLoadBalance = storeConfig?.globeLoadBalance ?: 0.0,
                                        isSensitiveDataVisible = isSensitiveDataVisible,
                                        hasGCash = storeConfig?.hasGCash ?: true,
                                        hasMaya = storeConfig?.hasMaya ?: true,
                                        hasLoad = storeConfig?.hasLoad ?: true,
                                        defaultLoadFee = storeConfig?.loadServiceFee ?: 2.0,
                                        defaultGcashFee = storeConfig?.gcashServiceFee ?: 10.0,
                                        defaultMayaFee = storeConfig?.mayaServiceFee ?: 10.0,
                                        defaultMayaBankFee = storeConfig?.mayaBankFee ?: 15.0,
                                        digitalCategoryId = categories.find { it.name.contains("GCash", ignoreCase = true) || it.name.contains("Digital", ignoreCase = true) }?.id ?: categories.firstOrNull()?.id ?: 1
                                    )
                                }
                            } else if (isProductsLoading) {
                                // Same cell size and spacing as the real grid below, so the
                                // skeleton does not reflow when products arrive.
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 150.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(8) { index ->
                                        ShimmerProductCard()
                                    }
                                }
                            } else if (filteredProducts.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            "No products found to match filters.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Text(
                                            "Ensure items are setup in the Admin dashboard inventory catalog.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 150.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(items = filteredProducts, key = { it.id }, contentType = { "product" }) { item ->
                                        val isLowStock = item.stockCount in 1..5
                                        val isOutOfStock = item.stockCount <= 0
                                        val availableUoms = remember(item.id, item.price, item.hasCustomUom, item.hasStick10s, item.hasStick20s, item.hasReam, item.hasMasterCase) { viewModel.getActiveUomsForProduct(item) }
                                        val isGCash = item.name.contains("GCash", ignoreCase = true)
                                        val isSmart = item.name.contains("Smart", ignoreCase = true) || item.name.contains("TNT", ignoreCase = true)
                                        val isGlobe = item.name.contains("Globe", ignoreCase = true) || item.name.contains("TM", ignoreCase = true)

                                        HeroCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .tactileBounce {
                                                    if (!isOutOfStock) {
                                                        if (availableUoms.size > 1) {
                                                            productForVariantPicker = item
                                                        } else {
                                                            viewModel.addToCart(item)
                                                            viewModel.playBeep()
                                                        }
                                                    }
                                                }
                                                .staggeredEnter((item.id and 0x3F) % 8)
                                                .testTag("product_card_${item.id}"),
                                            // Fully rounded on all four corners. This was
                                            // AsymmetricCardShape, which only rounds top-left
                                            // and bottom-right and draws straight edges for
                                            // the other two - so every catalog tile had two
                                            // hard 90-degree corners.
                                            shape = HeroUITheme.radius.lg,
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isOutOfStock) {
                                                    HeroUITheme.colors.surfaceSecondary.copy(alpha = 0.5f)
                                                } else {
                                                    HeroUITheme.colors.surface
                                                }
                                            ),
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = when {
                                                    isOutOfStock -> HeroUITheme.colors.danger.copy(alpha = 0.4f)
                                                    isLowStock -> HeroUITheme.colors.warning.copy(alpha = 0.5f)
                                                    else -> HeroUITheme.colors.border
                                                }
                                            ),
                                            elevation = CardDefaults.cardElevation(
                                                defaultElevation = if (isOutOfStock) 0.dp else 1.dp
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Category Tag and Stock Pill
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val categoryName = categoryNameById[item.categoryId] ?: "General"
                                                    HeroChip(
                                                        text = categoryName,
                                                        variant = HeroChipVariant.Flat,
                                                        color = HeroColor.Primary,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )

                                                    HeroChip(
                                                        text = when {
                                                            isOutOfStock -> "OUT"
                                                            isLowStock -> "LOW: ${item.stockCount}"
                                                            else -> "${item.stockCount} left"
                                                        },
                                                        variant = HeroChipVariant.Flat,
                                                        color = when {
                                                            isOutOfStock -> HeroColor.Danger
                                                            isLowStock -> HeroColor.Warning
                                                            else -> HeroColor.Default
                                                        },
                                                        modifier = Modifier.padding(start = 6.dp)
                                                    )
                                                }

                                                // Fixed 2-Line Product Title for Perfect Alignment
                                                Text(
                                                    text = item.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isOutOfStock) HeroUITheme.colors.muted else HeroUITheme.colors.foreground,
                                                    maxLines = 2,
                                                    minLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                // Bottom Price and Action Row
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "$curr${String.format(Locale.getDefault(), "%.2f", item.price)}",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Black,
                                                        color = HeroUITheme.colors.accent,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )

                                                    if (availableUoms.size > 1) {
                                                        HeroChip(
                                                            text = "${availableUoms.size} UOM",
                                                            variant = HeroChipVariant.Flat,
                                                            color = HeroColor.Primary
                                                        )
                                                    } else if (isGCash || isSmart || isGlobe) {
                                                        val context = LocalContext.current
                                                        IconButton(
                                                            onClick = {
                                                                try {
                                                                    if (isGCash) {
                                                                        val pkg = "com.globe.gcash.android"
                                                                        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                                                                        if (intent != null) {
                                                                            context.startActivity(intent)
                                                                        } else {
                                                                            val playIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$pkg"))
                                                                            playIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                            context.startActivity(playIntent)
                                                                        }
                                                                    } else {
                                                                        val ussd = if (isSmart) "*343#" else "*100#"
                                                                        val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                                                            data = android.net.Uri.parse("tel:" + android.net.Uri.encode(ussd))
                                                                        }
                                                                        context.startActivity(dialIntent)
                                                                    }
                                                                } catch (_: Exception) {}
                                                            },
                                                            modifier = Modifier.size(26.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Launch,
                                                                contentDescription = "Dial/App",
                                                                tint = HeroUITheme.colors.accent,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            } // Closes else
                        } // Closes Column
                    } // Closes SariSariCatalogWorkspace
                    val catalogContent = @Composable { SariSariCatalogWorkspace() }

                    // Right Workspace Content: Checkout Register Card
                    val registerContent = @Composable {
                        HeroCard(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ExpressiveSectionHeader(
                                    title = "Checkout Register",
                                    subtitle = "Scan items and take payment",
                                    icon = Icons.Default.PointOfSale
                                )

                                // 1) INLINE CUSTOMER QUANTITY + BARCODE SCANNER
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HeroInput(
                                        value = scannerQuantityInput,
                                        onValueChange = { scannerQuantityInput = it },
                                        label = { Text("Qty") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(76.dp).testTag("scanner_qty_field"),
                                        singleLine = true
                                    )

                                    Box(modifier = Modifier.weight(1f)) {
                                        var isDropdownExpanded by remember { mutableStateOf(false) }
                                        val matchedSuggestions = remember(manualBarcodeInput, products, categoryNameById) {
                                            if (manualBarcodeInput.isBlank()) emptyList()
                                            else products.filter {
                                                val catName = categoryNameById[it.categoryId] ?: ""
                                                it.name.contains(manualBarcodeInput, ignoreCase = true) ||
                                                it.barcode.contains(manualBarcodeInput, ignoreCase = true) ||
                                                catName.contains(manualBarcodeInput, ignoreCase = true)
                                            }.take(5)
                                        }

                                        // This field shares the row with the fixed-width Qty field,
                                        // so the sibling space is reserved when resolving the copy.
                                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                        val searchCopy = ScanSearchField.copyFor(maxWidth, hasSiblingField = true)

                                        HeroInput(
                                            value = manualBarcodeInput,
                                            onValueChange = { 
                                                manualBarcodeInput = it 
                                                isDropdownExpanded = true
                                            },
                                            label = { Text(searchCopy.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                            placeholder = { Text(searchCopy.placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                            trailingIcon = {
                                                IconButton(onClick = { viewModel.isScannerOpen.value = true }) {
                                                    Icon(Icons.Default.QrCodeScanner, "Launch camera")
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().testTag("scanner_barcode_field"),
                                            singleLine = true,
                                            keyboardActions = KeyboardActions(onDone = {
                                                if (manualBarcodeInput.isNotBlank()) {
                                                    val scanQty = scannerQuantityInput.toIntOrNull() ?: 1
                                                    viewModel.handleBarcodeScan(manualBarcodeInput, scanQty)
                                                    manualBarcodeInput = ""
                                                    isDropdownExpanded = false
                                                }
                                            })
                                        )
                                        } // end BoxWithConstraints for adaptive search copy

                                        if (isDropdownExpanded && matchedSuggestions.isNotEmpty()) {
                                            DropdownMenu(
                                                expanded = isDropdownExpanded,
                                                onDismissRequest = { isDropdownExpanded = false },
                                                modifier = Modifier.fillMaxWidth(0.95f).heightIn(max = 280.dp)
                                            ) {
                                                matchedSuggestions.forEach { prod ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(prod.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                                    Text(
                                                                        text = "Code: ${prod.barcode.ifEmpty { "None" }} • Stock: ${prod.stockCount}", 
                                                                        style = MaterialTheme.typography.labelSmall, 
                                                                        color = MaterialTheme.colorScheme.outline
                                                                    )
                                                                }
                                                                Text(
                                                                    text = "$curr${String.format(java.util.Locale.getDefault(), "%.2f", prod.price)}", 
                                                                    style = MaterialTheme.typography.labelMedium, 
                                                                    fontWeight = FontWeight.Bold, 
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.padding(start = 8.dp)
                                                                )
                                                            }
                                                        },
                                                        onClick = {
                                                            val scanQty = scannerQuantityInput.toIntOrNull() ?: 1
                                                            viewModel.addToCart(prod, scanQty)
                                                            viewModel.playBeep()
                                                            Toast.makeText(context, "Added: $scanQty x ${prod.name}", Toast.LENGTH_SHORT).show()
                                                            manualBarcodeInput = ""
                                                            isDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            if (manualBarcodeInput.isNotBlank()) {
                                                val scanQty = scannerQuantityInput.toIntOrNull() ?: 1
                                                viewModel.handleBarcodeScan(manualBarcodeInput, scanQty)
                                                manualBarcodeInput = ""
                                            } else {
                                                Toast.makeText(context, "Please scan or enter a barcode first", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
                                    ) {
                                        Icon(Icons.Default.Add, "Add barcode item")
                                    }
                                }

                                // 2) TRANSACTION LIST WITH CHECKBOX + LINE TOTAL PRICE
                                if (cartItems.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.ShoppingCart,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                                modifier = Modifier.size(56.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Cart Register is empty",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                            Text(
                                                "Scan items or tap on left catalog folders.",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        item {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                val allChecked = cartItems.all { checkedCartItems[it.first.id] != false }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    HeroCheckbox(
                                                        checked = allChecked && cartItems.isNotEmpty(),
                                                        onCheckedChange = { allCheckedValue ->
                                                            cartItems.forEach {
                                                                checkedCartItems[it.first.id] = allCheckedValue
                                                            }
                                                        },
                                                        modifier = Modifier.testTag("bulk_select_checkbox")
                                                    )
                                                    Text("Select All", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                                }
                                                
                                                val checkedCount = cartItems.count { checkedCartItems[it.first.id] != false }
                                                if (checkedCount > 0) {
                                                    TextButton(
                                                        onClick = { showBulkDeleteConfirm = true },
                                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                        modifier = Modifier.testTag("bulk_delete_button")
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete Checked", modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Delete Selected ($checkedCount)", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                            }
                                        }

                                        items(cartItems) { lineItem ->
                                            val product = lineItem.first
                                            val qty = lineItem.second
                                            val isChecked = checkedCartItems[product.id] ?: true
                                            val activeUom = selectedCartUoms[product.id] ?: com.munzo.storepoint.data.UomOption("Base Unit", 1, product.price)
                                            val itemDiscount = cartItemDiscounts[product.id] ?: 0.0
                                            val effectivePrice = (activeUom.price - itemDiscount).coerceAtLeast(0.0)
                                            val linePriceTotal = effectivePrice * qty

                                            HeroCard(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .pointerInput(product.id) {
                                                        detectTapGestures(
                                                            onLongPress = {
                                                                longPressedCartItem = product
                                                            }
                                                        )
                                                    },
                                                shape = RoundedCornerShape(16.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isChecked) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainerLowest
                                                ),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                                )
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    // Top Row: Checkbox, Name, and Delete
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        HeroCheckbox(
                                                            checked = isChecked,
                                                            onCheckedChange = { checkedCartItems[product.id] = it },
                                                            modifier = Modifier.testTag("item_check_${product.id}")
                                                        )

                                                        Spacer(modifier = Modifier.width(6.dp))

                                                        Text(
                                                            text = product.name,
                                                            fontWeight = FontWeight.Bold,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = if (isChecked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f)
                                                        )

                                                        IconButton(
                                                            onClick = { itemPendingDelete = product },
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Delete,
                                                                contentDescription = "Delete item",
                                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }

                                                    // Bottom Row: Price/UOM and Quantity Stepper + Line Total
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // Price and UOM selector & Discount
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            if (itemDiscount > 0.0) {
                                                                Column {
                                                                    Text(
                                                                        text = "$curr${String.format(Locale.getDefault(), "%.2f", activeUom.price)}",
                                                                        style = MaterialTheme.typography.labelSmall.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                                                                        color = MaterialTheme.colorScheme.outline
                                                                    )
                                                                    Text(
                                                                        text = "$curr${String.format(Locale.getDefault(), "%.2f", effectivePrice)}",
                                                                        style = MaterialTheme.typography.labelMedium,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = MaterialTheme.colorScheme.primary
                                                                    )
                                                                }
                                                                Surface(
                                                                    onClick = { productForDiscountDialog = product },
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                                                    modifier = Modifier.testTag("cart_discount_chip_${product.id}")
                                                                ) {
                                                                    Row(
                                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                                        verticalAlignment = Alignment.CenterVertically
                                                                    ) {
                                                                        Icon(Icons.Default.Percent, null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                                                        Spacer(modifier = Modifier.width(2.dp))
                                                                        Text("-$curr${String.format(Locale.getDefault(), "%.2f", itemDiscount)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                                                    }
                                                                }
                                                            } else {
                                                                Text(
                                                                    text = "$curr${String.format(Locale.getDefault(), "%.2f", activeUom.price)}",
                                                                    style = MaterialTheme.typography.labelMedium,
                                                                    color = MaterialTheme.colorScheme.outline
                                                                )
                                                                IconButton(
                                                                    onClick = { productForDiscountDialog = product },
                                                                    modifier = Modifier.size(24.dp).testTag("cart_discount_btn_${product.id}")
                                                                ) {
                                                                    Icon(Icons.Default.Percent, contentDescription = "Add Discount", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(12.dp))
                                                                }
                                                            }

                                                            val uomsList = viewModel.getActiveUomsForProduct(product)
                                                            if (uomsList.size > 1) {
                                                                var showUomMenu by remember { mutableStateOf(false) }
                                                                Box {
                                                                    Surface(
                                                                        onClick = { showUomMenu = true },
                                                                        shape = RoundedCornerShape(6.dp),
                                                                        color = MaterialTheme.colorScheme.secondaryContainer
                                                                    ) {
                                                                        Row(
                                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                            verticalAlignment = Alignment.CenterVertically
                                                                        ) {
                                                                            Text(activeUom.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                                                            Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                                                        }
                                                                    }
                                                                    DropdownMenu(
                                                                        expanded = showUomMenu,
                                                                        onDismissRequest = { showUomMenu = false }
                                                                    ) {
                                                                        uomsList.forEach { option ->
                                                                            DropdownMenuItem(
                                                                                text = { Text("${option.name} ($curr${String.format(Locale.getDefault(), "%.2f", option.price)})") },
                                                                                onClick = {
                                                                                    viewModel.selectCartUom(product.id, option)
                                                                                    showUomMenu = false
                                                                                }
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        // Stepper + Line Total
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Surface(
                                                                shape = RoundedCornerShape(8.dp),
                                                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
                                                                ) {
                                                                    IconButton(
                                                                        onClick = { viewModel.updateCartQuantity(product, qty - 1) },
                                                                        modifier = Modifier.size(26.dp)
                                                                    ) {
                                                                        Icon(Icons.Default.Remove, "Less", modifier = Modifier.size(12.dp))
                                                                    }
                                                                    Text(
                                                                        text = "$qty",
                                                                        style = MaterialTheme.typography.labelMedium,
                                                                        fontWeight = FontWeight.Bold,
                                                                        modifier = Modifier.padding(horizontal = 4.dp)
                                                                    )
                                                                    IconButton(
                                                                        onClick = { viewModel.updateCartQuantity(product, qty + 1) },
                                                                        modifier = Modifier.size(26.dp)
                                                                    ) {
                                                                        Icon(Icons.Default.Add, "Add", modifier = Modifier.size(12.dp))
                                                                    }
                                                                }
                                                            }

                                                            Text(
                                                                text = "$curr${String.format(Locale.getDefault(), "%.2f", linePriceTotal)}",
                                                                fontWeight = FontWeight.Black,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Math Subtotals
                                HeroDivider()

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("VATable Sales (Net)", style = MaterialTheme.typography.bodySmall)
                                        Text("$curr${String.format(Locale.getDefault(), "%.2f", subtotal)}")
                                    }
                                    if (doubleTaxRate > 0.0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("VAT (${String.format(Locale.US, "%.1f", doubleTaxRate)}% Included)", style = MaterialTheme.typography.bodySmall)
                                            Text("$curr${String.format(Locale.getDefault(), "%.2f", taxAmount)}")
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("TOTAL AMOUNT (VAT Inc.)", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                        Text("$curr${String.format(Locale.getDefault(), "%.2f", totalAmount)}", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                // Action hotkeys guides / Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.clearCart() },
                                        enabled = cartItems.isNotEmpty(),
                                        modifier = Modifier.size(44.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteSweep,
                                            contentDescription = "Clear Cart",
                                            tint = if (cartItems.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            parkNoteInput = "Parked Note #${parkedList.size + 1}"
                                            showParkDialog = true
                                        },
                                        enabled = cartItems.isNotEmpty(),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Pause, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Park")
                                    }
                                }

                                HeroButton(
                                    onClick = { showPaymentDialog = true },
                                    enabled = activeCartItems.isNotEmpty() && activeSession != null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("checkout_charge_button"),
                                    text = "Charge $curr${String.format(Locale.getDefault(), "%.2f", totalAmount)} [F9]",
                                    icon = Icons.Default.ShoppingBag,
                                    color = HeroColor.Primary,
                                    variant = HeroButtonVariant.Solid,
                                    size = HeroSize.Lg
                                )
                            }
                        }
                    }

                    if (isPortrait) {
                        // Portrait layout: Smoothly switch between Checkout Cart, Products, and Digital Services
                        Column(modifier = Modifier.fillMaxSize()) {
                            val showDigital = (storeConfig?.hasGCash == true) || (storeConfig?.hasMaya == true) || (storeConfig?.hasLoad == true)
                            // Clean bounds safety check: if selectedTabIndex is invalid because digital was disabled
                            val selectedIndex = if (!showDigital && activePortraitTab == 2) 1 else activePortraitTab

                            TabRow(
                                selectedTabIndex = selectedIndex,
                                modifier = Modifier.fillMaxWidth().testTag("pos_portrait_tab_row"),
                                containerColor = HeroUITheme.colors.surface,
                                contentColor = HeroUITheme.colors.accent,
                                indicator = { tabPositions ->
                                    if (selectedIndex < tabPositions.size) {
                                        TabRowDefaults.SecondaryIndicator(
                                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                                            color = HeroUITheme.colors.accent
                                        )
                                    }
                                }
                            ) {
                                Tab(
                                    selected = selectedIndex == 0,
                                    onClick = { activePortraitTab = 0 },
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            BadgedBox(badge = {
                                                if (cartItems.isNotEmpty()) {
                                                    Badge { Text("${cartItems.sumOf { it.second }}") }
                                                }
                                            }) {
                                                Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(18.dp))
                                            }
                                            Text(
                                                text = "Cart (${cartItems.size})",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    },
                                    modifier = Modifier.testTag("tab_checkout_cart")
                                )
                                Tab(
                                    selected = selectedIndex == 1,
                                    onClick = { 
                                        activePortraitTab = 1
                                        activeCatalogSection = 0
                                    },
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.Storefront, null, modifier = Modifier.size(18.dp))
                                            Text(
                                                text = "Catalog",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    },
                                    modifier = Modifier.testTag("tab_standard_products")
                                )
                                if (showDigital) {
                                    Tab(
                                        selected = selectedIndex == 2,
                                        onClick = { 
                                            activePortraitTab = 2
                                            activeCatalogSection = 1
                                        },
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.PhoneAndroid, null, modifier = Modifier.size(18.dp))
                                                Text(
                                                    text = "Digital",
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        },
                                        modifier = Modifier.testTag("tab_digital_services")
                                    )
                                }
                            }
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                if (selectedIndex == 0) {
                                    registerContent()
                                } else {
                                    catalogContent()
                                }
                            }
                        }
                    } else {
                        // Landscape/Wide layout: Side-by-side Sari-Sari Digital Retailing & Checkout Register
                        Row(modifier = Modifier.fillMaxSize()) {
                            val catalogWeight = if (screenMaxWidth >= 900.dp) 0.58f else 0.50f
                            val registerWeight = 1f - catalogWeight
                            Box(modifier = Modifier.fillMaxHeight().weight(catalogWeight)) {
                                catalogContent()
                            }
                            Box(modifier = Modifier.fillMaxHeight().weight(registerWeight)) {
                                registerContent()
                            }
                        }
                    }
                }
            }
        }
    }
)

        // --- Dialogs & Panels ---

        // 1) Shift start dialog
        if (showShiftStartDialog) {
            AlertDialog(
                onDismissRequest = {}, // Force compliance: must enter shift to use POS
                confirmButton = {
                    HeroButton(
                        onClick = {
                            val amount = initialCashInput.toDoubleOrNull() ?: 0.0
                            val gcashBal = initialGCashInput.toDoubleOrNull() ?: 0.0
                            val smartBal = initialSmartLoadInput.toDoubleOrNull() ?: 0.0
                            val globeBal = initialGlobeLoadInput.toDoubleOrNull() ?: 0.0
                            viewModel.startCashierShift(amount)
                            if (storeConfig?.hasGCash == true) {
                                viewModel.updateGCashBalance(gcashBal)
                            }
                            if (storeConfig?.hasLoad == true) {
                                viewModel.updateSmartLoadBalance(smartBal)
                                viewModel.updateGlobeLoadBalance(globeBal)
                            }
                            showShiftStartDialog = false
                        },
                        modifier = Modifier.testTag("shift_start_confirm"),
                        text = "Register Drawer Start",
                        color = HeroColor.Primary,
                        variant = HeroButtonVariant.Solid,
                        size = HeroSize.Md
                    )
                },
                title = { Text("Open Shift Drawer Session") },
                text = {
                    Column(
                        modifier = Modifier.imePadding().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Welcome cashier! Register the physical starting cash float and the digital wallet balance to start your shift session.")
                        
                        HeroInput(
                            value = initialCashInput,
                            onValueChange = { initialCashInput = it },
                            label = { Text("Starting Cash float ($curr)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("starting_cash_input")
                        )

                        if (storeConfig?.hasGCash == true) {
                            HeroInput(
                                value = initialGCashInput,
                                onValueChange = { initialGCashInput = it },
                                label = { Text("Starting GCash/Digital Balance ($curr)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("starting_gcash_input")
                            )
                        }

                        if (storeConfig?.hasLoad == true) {
                            HeroInput(
                                value = initialSmartLoadInput,
                                onValueChange = { initialSmartLoadInput = it },
                                label = { Text("Starting Smart/TNT Load Wallet ($curr)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("starting_smart_load_input")
                            )

                            HeroInput(
                                value = initialGlobeLoadInput,
                                onValueChange = { initialGlobeLoadInput = it },
                                label = { Text("Starting Globe/TM Load Wallet ($curr)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("starting_globe_load_input")
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "This automatically pre-fills with the ending GCash balance from your previous shift.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        }

        // 2) Shift closure & reconciliation dialog (Mandatory End-of-Shift Guard)
        if (showShiftCloseDialog) {
            val transactionsList by viewModel.allTransactions.collectAsState()
            val sessionTransactions = remember(transactionsList, activeSession) {
                transactionsList.filter { it.timestamp >= (activeSession?.startTime ?: 0L) }
            }
            val startingFloat = activeSession?.startingCash ?: 0.0
            val cashSalesInSession = remember(sessionTransactions) {
                sessionTransactions.sumOf {
                    if (it.paymentMethod == "CASH") it.totalAmount
                    else if (it.paymentMethod.startsWith("SPLIT")) it.cashPaid
                    else 0.0
                }
            }
            val drawerPayoutsList by viewModel.allDrawerTransactions.collectAsState()
            val sessionDrawerTx = remember(drawerPayoutsList, activeSession) {
                drawerPayoutsList.filter {
                    it.sessionId == (activeSession?.id ?: 0)
                }
            }
            val cashIn = remember(sessionDrawerTx) {
                sessionDrawerTx.filter { it.amount > 0 }.sumOf { it.amount }
            }
            val cashOut = remember(sessionDrawerTx) {
                sessionDrawerTx.filter { it.amount < 0 && !it.reason.startsWith("Refund") && it.type != "REFUND" }.sumOf { -it.amount }
            }
            val cashRefunds = remember(sessionDrawerTx) {
                sessionDrawerTx.filter { it.amount < 0 && (it.reason.startsWith("Refund") || it.type == "REFUND") }.sumOf { -it.amount }
            }
            val expectedCash = StorePointRepository.roundMoney(startingFloat + cashSalesInSession + cashIn - cashOut - cashRefunds)

            // GCash Digital Wallet Ledger
            val allWalletEntries by viewModel.allWalletLedger.collectAsState()
            val sessionGcashEntries = remember(allWalletEntries, activeSession) {
                allWalletEntries.filter { it.walletType == "GCASH" && it.timestamp >= (activeSession?.startTime ?: 0L) }
            }
            val gcashInflow = remember(sessionGcashEntries) {
                sessionGcashEntries.filter { it.delta > 0 }.sumOf { it.delta }
            }
            val gcashDisbursed = remember(sessionGcashEntries) {
                sessionGcashEntries.filter { it.delta < 0 }.sumOf { -it.delta }
            }
            val expectedGCash = storeConfig?.gcashBalance ?: 0.0
            val gcashStartingBalance = StorePointRepository.roundMoney(expectedGCash - gcashInflow + gcashDisbursed)

            // Prepaid Load (Smart & Globe)
            val sessionSmartEntries = remember(allWalletEntries, activeSession) {
                allWalletEntries.filter { it.walletType == "SMART" && it.timestamp >= (activeSession?.startTime ?: 0L) }
            }
            val smartReloadSales = remember(sessionSmartEntries) {
                sessionSmartEntries.filter { it.delta < 0 }.sumOf { -it.delta }
            }
            val expectedSmartLoad = storeConfig?.smartLoadBalance ?: 0.0
            val smartStartingBalance = StorePointRepository.roundMoney(expectedSmartLoad + smartReloadSales)

            val sessionGlobeEntries = remember(allWalletEntries, activeSession) {
                allWalletEntries.filter { it.walletType == "GLOBE" && it.timestamp >= (activeSession?.startTime ?: 0L) }
            }
            val globeReloadSales = remember(sessionGlobeEntries) {
                sessionGlobeEntries.filter { it.delta < 0 }.sumOf { -it.delta }
            }
            val expectedGlobeLoad = storeConfig?.globeLoadBalance ?: 0.0
            val globeStartingBalance = StorePointRepository.roundMoney(expectedGlobeLoad + globeReloadSales)

            if (activeSession != null) {
                ShiftReconciliationDialog(
                    session = activeSession!!,
                    viewModel = viewModel,
                    expectedCash = expectedCash,
                    startingFloat = startingFloat,
                    cashSales = cashSalesInSession,
                    cashIn = cashIn,
                    cashOut = cashOut,
                    cashRefunds = cashRefunds,
                    expectedGCash = expectedGCash,
                    gcashStartingBalance = gcashStartingBalance,
                    gcashInflow = gcashInflow,
                    gcashDisbursed = gcashDisbursed,
                    expectedSmartLoad = expectedSmartLoad,
                    smartStartingBalance = smartStartingBalance,
                    smartReloadSales = smartReloadSales,
                    expectedGlobeLoad = expectedGlobeLoad,
                    globeStartingBalance = globeStartingBalance,
                    globeReloadSales = globeReloadSales,
                    onDismiss = { showShiftCloseDialog = false },
                    onShiftClosed = {
                        showShiftCloseDialog = false
                        Toast.makeText(context, "Shift closed and reconciled successfully.", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                showShiftCloseDialog = false
            }
        }

        // Active Shift Logout Warning (Guard against abandoning open shifts)
        if (showActiveShiftLogoutWarning) {
            AlertDialog(
                onDismissRequest = { showActiveShiftLogoutWarning = false },
                icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Active Shift Handover Required", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "You have an active open cashier shift (#SES-${activeSession?.id}). Store policy requires physical drawer balancing and reconciliation before logging out.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Please reconcile your shift now or cancel to return to POS operations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showActiveShiftLogoutWarning = false
                            showShiftCloseDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("logout_warning_reconcile_btn")
                    ) {
                        Text("Reconcile & Close Shift")
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (activeUser?.role.equals("ADMIN", ignoreCase = true)) {
                            TextButton(
                                onClick = {
                                    showActiveShiftLogoutWarning = false
                                    viewModel.logout()
                                    onLogout()
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Admin Force Logout")
                            }
                        }
                        TextButton(onClick = { showActiveShiftLogoutWarning = false }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }

        // 3) Barcode scanner camera view (Continuous Fast Item Addition)
        if (isScannerOpen) {
            BarcodeScannerDialog(
                products = products,
                title = "Continuous Barcode Scanner",
                instruction = "Point camera at barcodes. Items are scanned and added continuously.",
                continuous = true,
                lastScannedBarcode = lastScannedBarcodeFeedback,
                onBarcodeScanned = { barcode ->
                    val match = products.find {
                        it.barcode.equals(barcode.trim(), ignoreCase = true) ||
                        (it.hasStick10s && it.barcode10s.equals(barcode.trim(), ignoreCase = true)) ||
                        (it.hasStick20s && it.barcode20s.equals(barcode.trim(), ignoreCase = true))
                    }
                    lastScannedBarcodeFeedback = if (match != null) "${match.name} (+1)" else barcode
                    viewModel.handleBarcodeScan(barcode)
                },
                onDismiss = {
                    viewModel.isScannerOpen.value = false
                    lastScannedBarcodeFeedback = null
                }
            )
        }

        // 4) Park Transaction Dialog
        if (showParkDialog) {
            AlertDialog(
                onDismissRequest = { showParkDialog = false },
                confirmButton = {
                    ExpressiveButton(
                        onClick = {
                            if (parkNoteInput.isNotBlank()) {
                                viewModel.parkActiveTransaction(parkNoteInput.trim())
                                showParkDialog = false
                            }
                        },
                        modifier = Modifier.testTag("park_submit_button"),
                        label = "Park Sale",
                        variant = ExpressiveButtonVariant.FILLED,
                        size = ExpressiveButtonSize.M,
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showParkDialog = false }) {
                        Text("Browse")
                    }
                },
                title = { Text("Park Transaction (Hold)") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Hold catalog cart temporarily. You can retrieve it instantly later using the held sidebar panels.")
                        HeroInput(
                            value = parkNoteInput,
                            onValueChange = { parkNoteInput = it },
                            label = { Text("Reference text / Customer name") },
                            modifier = Modifier.fillMaxWidth().testTag("park_note_input")
                        )
                    }
                }
            )
        }

        // 5) Resuming Parked Dialog list
        if (showParkedTxDialog) {
            AlertDialog(
                onDismissRequest = { showParkedTxDialog = false },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showParkedTxDialog = false }) {
                        Text("Close")
                    }
                },
                title = { Text("Parked Held Transactions Archive (${parkedList.size})") },
                text = {
                    if (parkedList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No pending sales on hold.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(parkedList) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.note, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(
                                            "Held: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(item.timestamp))}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Button(
                                            onClick = {
                                                viewModel.resumeParkedTransaction(item.id) {
                                                    showParkedTxDialog = false
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(32.dp).testTag("resume_park_btn_${item.id}")
                                        ) {
                                            Text("Resume", style = MaterialTheme.typography.bodySmall)
                                        }

                                        IconButton(onClick = { viewModel.deleteParkedTransaction(item.id) }) {
                                            Icon(Icons.Default.Delete, "Delete parked transaction", tint = Color.Red.copy(alpha = 0.6f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

        // --- SINGLE ITEM DELETE SAFETY CONFIRMATION ---
        if (itemPendingDelete != null) {
            val productToDelete = itemPendingDelete!!
            AlertDialog(
                onDismissRequest = { itemPendingDelete = null },
                title = { Text("Confirm Item Removal", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to remove '${productToDelete.name}' from your checkout cart?") },
                confirmButton = {
                    ExpressiveButton(
                        onClick = {
                            viewModel.removeFromCart(productToDelete)
                            checkedCartItems[productToDelete.id] = false
                            itemPendingDelete = null
                            Toast.makeText(context, "${productToDelete.name} removed from cart.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("single_delete_confirm_btn"),
                        label = "Confirm Remove",
                        variant = ExpressiveButtonVariant.FILLED,
                        size = ExpressiveButtonSize.M,
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    )
                },
                dismissButton = {
                    TextButton(onClick = { itemPendingDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- BULK DELETE CONFIRMATION DIALOG ---
        if (showBulkDeleteConfirm) {
            val checkedQty = cartItems.count { checkedCartItems[it.first.id] != false }
            AlertDialog(
                onDismissRequest = { showBulkDeleteConfirm = false },
                title = { Text("Confirm Bulk Deletion", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete all $checkedQty checked item(s) from the checkout cart? This action cannot be undone.") },
                confirmButton = {
                    ExpressiveButton(
                        onClick = {
                            val toDelete = cartItems.filter { checkedCartItems[it.first.id] != false }
                            toDelete.forEach { (p, _) ->
                                viewModel.removeFromCart(p)
                                checkedCartItems[p.id] = false
                            }
                            showBulkDeleteConfirm = false
                            Toast.makeText(context, "Deleted $checkedQty item(s) successfully.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("bulk_delete_confirm_button"),
                        label = "Confirm Bulk Delete",
                        variant = ExpressiveButtonVariant.FILLED,
                        size = ExpressiveButtonSize.M,
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showBulkDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- EDIT PRODUCT UOM VIA TOUCH LONG-PRESS ---
        if (longPressedCartItem != null) {
            val product = longPressedCartItem!!
            val qty = cartItems.find { it.first.id == product.id }?.second ?: 0
            val activeUom = selectedCartUoms[product.id] ?: com.munzo.storepoint.data.UomOption("Base Unit", 1, product.price)
            val subtotalValue = activeUom.price * qty

            AlertDialog(
                onDismissRequest = { longPressedCartItem = null },
                confirmButton = {
                    Button(
                        onClick = { longPressedCartItem = null },
                        modifier = Modifier.testTag("close_uom_edit_dialog")
                    ) {
                        Text("Close")
                    }
                },
                title = {
                    Text(
                        text = "Edit Item Measurement & Pricing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Product Name: ${product.name}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        
                        Text("Current Unit of Measurement: ${activeUom.name}", style = MaterialTheme.typography.bodyMedium)
                        
                        Text("Base Retail Price: $curr${String.format(Locale.US, "%.2f", product.price)}", style = MaterialTheme.typography.bodySmall)

                        if (product.cost > 0.0) {
                            Text("Dealer's Cost Price: $curr${String.format(Locale.US, "%.2f", product.cost * activeUom.multiplier)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }

                        Text("Selected Qty: $qty unit(s)", style = MaterialTheme.typography.bodySmall)
                        
                        Text("Line Subtotal: $curr${String.format(Locale.US, "%.2f", subtotalValue)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        HeroDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("Switch Unit of Measurement:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)

                        val uomsList = viewModel.getActiveUomsForProduct(product)
                        uomsList.forEach { option ->
                            val isOptionSelected = activeUom.name == option.name
                            HeroCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassPanel(cornerRadius = 16.dp)
                                    .tactileBounce()
                                    .clickable {
                                        viewModel.selectCartUom(product.id, option)
                                    }
                                    .border(
                                        width = if (isOptionSelected) 2.dp else 0.dp,
                                        color = if (isOptionSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isOptionSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(option.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text("Multiplier: ×${option.multiplier}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text("$curr${String.format(Locale.US, "%.2f", option.price)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            )
        }

        // 5.5) CALL ADMIN / SUPERVISOR ASSISTANCE DIALOG (With Direct Admin Phone Dialing)
        if (showCallAdminDialog) {
            var selectedReason by remember { mutableStateOf("Manager Override / Void") }
            var assistanceNote by remember { mutableStateOf("") }
            val adminPrefs = remember { context.getSharedPreferences("storepoint_admin_prefs", Context.MODE_PRIVATE) }
            var adminPhoneInput by remember { mutableStateOf(adminPrefs.getString("admin_phone_number", "") ?: "") }
            val reasons = listOf(
                "Manager Override / Void",
                "Price Check / Discount",
                "Cash Drawer Float Refill",
                "Hardware / Scanner Issue",
                "Customer Dispute / Inquiries"
            )

            val triggerDialAdmin = {
                val clean = adminPhoneInput.filter { it.isDigit() || it == '+' }
                adminPrefs.edit().putString("admin_phone_number", adminPhoneInput).apply()
                val dialIntent = if (clean.isNotBlank()) {
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean"))
                } else {
                    Intent(Intent.ACTION_DIAL)
                }.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(dialIntent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Cannot open phone dialer: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }

            AlertDialog(
                onDismissRequest = { showCallAdminDialog = false },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                title = {
                    Text("Call Admin / Supervisor", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Request immediate manager or admin assistance. Confirming will dial the administrator's phone number directly.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (adminPhoneInput.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                    Column {
                                        Text(
                                            text = "Admin Contact Number:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = adminPhoneInput,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                    Text(
                                        "No admin phone number is configured yet. Admin can set it under Admin Center > Security, or you may enter it below:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }

                            HeroInput(
                                value = adminPhoneInput,
                                onValueChange = {
                                    adminPhoneInput = it
                                    adminPrefs.edit().putString("admin_phone_number", it).apply()
                                },
                                label = { Text("Admin / Supervisor Phone Number") },
                                placeholder = { Text("e.g. 09171234567 or +639171234567") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                leadingIcon = { Icon(Icons.Default.Phone, null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("admin_phone_input")
                            )
                        }

                        Text(
                            "Reason for Assistance:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            reasons.forEach { reason ->
                                FilterChip(
                                    selected = selectedReason == reason,
                                    onClick = { selectedReason = reason },
                                    label = { Text(reason, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        HeroInput(
                            value = assistanceNote,
                            onValueChange = { assistanceNote = it },
                            label = { Text("Note / Description (Optional)") },
                            placeholder = { Text("e.g. Needs manager PIN for void") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showCallAdminDialog = false
                            // Dial the admin
                            triggerDialAdmin()
                            try {
                                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    vibrator?.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator?.vibrate(500)
                                }
                            } catch (_: Exception) {}
                            try {
                                val tone = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 100)
                                tone.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 600)
                            } catch (_: Exception) {}
                            val cashierName = activeUser?.username ?: "Register"
                            val noteText = if (assistanceNote.isNotBlank()) " ($assistanceNote)" else ""
                            Toast.makeText(
                                context,
                                "Calling Admin for Cashier $cashierName... [$selectedReason]$noteText",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.testTag("submit_call_admin_btn")
                    ) {
                        Icon(Icons.Default.Call, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dial Admin Now")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCallAdminDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 6) PAYMENT PROCESSING DIALOG
        // 6) PAYMENT PROCESSING DIALOG (Multi-Tender Settlement: Cash, GCash, Maya, Split)
        if (showPaymentDialog) {
            var selectedPaymentMethod by remember { mutableStateOf("CASH") } // "CASH", "GCASH", "MAYA", "SPLIT"
            var selectedSplitWallet by remember { mutableStateOf("GCash") }
            var amountPaidInput by remember { mutableStateOf("") }
            var splitCashPaidInput by remember { mutableStateOf("") }
            var ewalletRefNumber by remember { mutableStateOf("") }
            var customerNameInput by remember { mutableStateOf("") }

            val parsedPayAmount = amountPaidInput.replace(',', '.').toDoubleOrNull() ?: 0.0
            val calculatedChange = if (totalAmount < 0.0) -totalAmount else (parsedPayAmount - totalAmount).coerceAtLeast(0.0)

            val parsedSplitCash = splitCashPaidInput.replace(',', '.').toDoubleOrNull() ?: 0.0
            val splitWalletBalance = (totalAmount - parsedSplitCash).coerceAtLeast(0.0)
            val splitCashChange = (parsedSplitCash - totalAmount).coerceAtLeast(0.0)

            Dialog(onDismissRequest = { showPaymentDialog = false }) {
                HeroCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .glassPanel(cornerRadius = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Multi-Tender Payment Settlement",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Payment Methods selectable row (Cash, GCash, Maya, Split Tender)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedPaymentMethod == "CASH",
                                onClick = { selectedPaymentMethod = "CASH" },
                                label = { Text("Cash", fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.AttachMoney, null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.testTag("pay_cash_method")
                            )
                            FilterChip(
                                selected = selectedPaymentMethod == "GCASH",
                                onClick = { selectedPaymentMethod = "GCASH" },
                                label = { Text("GCash", fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.PhoneAndroid, null, tint = GCashColor, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.testTag("pay_gcash_method")
                            )
                            FilterChip(
                                selected = selectedPaymentMethod == "MAYA",
                                onClick = { selectedPaymentMethod = "MAYA" },
                                label = { Text("Maya", fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, null, tint = MayaColor, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.testTag("pay_maya_method")
                            )
                            FilterChip(
                                selected = selectedPaymentMethod == "SPLIT",
                                onClick = { selectedPaymentMethod = "SPLIT" },
                                label = { Text("Split Tender", fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.TrendingFlat, null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.testTag("pay_split_method")
                            )
                        }

                        // Total Due Banner
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total Amount Due:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "$curr${String.format(Locale.getDefault(), "%.2f", totalAmount)}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Mode-specific Payment Content
                        when (selectedPaymentMethod) {
                            "CASH" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    // Common Denomination Quick-Pay Buttons
                                    Text("Quick-Pay Cash Denominations:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        SuggestionChip(
                                            onClick = {
                                                amountPaidInput = String.format(Locale.US, "%.2f", if (totalAmount < 0.0) 0.0 else totalAmount)
                                            },
                                            label = { Text("Exact", fontWeight = FontWeight.Bold) },
                                            modifier = Modifier.testTag("denom_exact_btn")
                                        )
                                        listOf(20, 50, 100, 200, 500, 1000).forEach { denom ->
                                            SuggestionChip(
                                                onClick = {
                                                    val current = amountPaidInput.replace(',', '.').toDoubleOrNull() ?: 0.0
                                                    amountPaidInput = if (current == 0.0) {
                                                        String.format(Locale.US, "%.2f", denom.toDouble())
                                                    } else {
                                                        String.format(Locale.US, "%.2f", current + denom)
                                                    }
                                                },
                                                label = { Text("+$curr$denom") },
                                                modifier = Modifier.testTag("denom_${denom}_btn")
                                            )
                                        }
                                    }

                                    HeroInput(
                                        value = amountPaidInput,
                                        onValueChange = { amountPaidInput = it },
                                        label = { Text("Tendered Cash Paid Amount ($curr)") },
                                        placeholder = { Text(String.format(Locale.getDefault(), "%.2f", if (totalAmount < 0.0) 0.0 else totalAmount)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth().testTag("tendered_cash_input"),
                                        trailingIcon = {
                                            if (amountPaidInput.isNotEmpty()) {
                                                IconButton(onClick = { amountPaidInput = "" }) {
                                                    Icon(Icons.Default.Clear, "Clear")
                                                }
                                            }
                                        }
                                    )

                                    // Instant Change Calculation
                                    if (totalAmount < 0.0 || (amountPaidInput.isNotBlank() && parsedPayAmount >= totalAmount)) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val changeLabel = if (totalAmount < 0.0) "CASH TO PAY OUT:" else "CHANGE TO RETURN:"
                                                Text(changeLabel, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                                Text("$curr${String.format(Locale.getDefault(), "%.2f", calculatedChange)}", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                            }
                                        }
                                    } else if (amountPaidInput.isNotBlank() && parsedPayAmount < totalAmount) {
                                        Text(
                                            "Insufficient tender cash paid (Short by $curr${String.format(Locale.getDefault(), "%.2f", totalAmount - parsedPayAmount)}).",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                            "GCASH" -> {
                                HeroCard(
                                    colors = CardDefaults.cardColors(containerColor = GCashColor.copy(alpha = 0.12f)),
                                    border = BorderStroke(1.5.dp, GCashColor.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.PhoneAndroid, null, tint = GCashColor)
                                            Text("GCash Digital Payment Settlement", fontWeight = FontWeight.Bold, color = GCashColor)
                                        }
                                        Text(
                                            "Collect exact amount $curr${String.format(Locale.getDefault(), "%.2f", totalAmount)} via customer GCash transfer or QR code.",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                HeroInput(
                                    value = ewalletRefNumber,
                                    onValueChange = { ewalletRefNumber = it },
                                    label = { Text("GCash Reference Number (Optional)") },
                                    placeholder = { Text("e.g. 100234567890") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("ewallet_ref_input")
                                )
                            }
                            "MAYA" -> {
                                HeroCard(
                                    colors = CardDefaults.cardColors(containerColor = MayaColor.copy(alpha = 0.12f)),
                                    border = BorderStroke(1.5.dp, MayaColor.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.AccountBalanceWallet, null, tint = MayaColor)
                                            Text("Maya Digital Payment Settlement", fontWeight = FontWeight.Bold, color = MayaColor)
                                        }
                                        Text(
                                            "Collect exact amount $curr${String.format(Locale.getDefault(), "%.2f", totalAmount)} via customer Maya transfer or QR code.",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                HeroInput(
                                    value = ewalletRefNumber,
                                    onValueChange = { ewalletRefNumber = it },
                                    label = { Text("Maya Reference Number (Optional)") },
                                    placeholder = { Text("e.g. 987654321") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("ewallet_ref_input")
                                )
                            }
                            "SPLIT" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    Text("Split Bill: Part Cash + Part E-Wallet", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                                    // Quick presets for cash portion
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(20, 50, 100, 200, 500).forEach { denom ->
                                            if (denom < totalAmount) {
                                                SuggestionChip(
                                                    onClick = { splitCashPaidInput = String.format(Locale.US, "%.2f", denom.toDouble()) },
                                                    label = { Text("Cash $curr$denom") }
                                                )
                                            }
                                        }
                                    }

                                    HeroInput(
                                        value = splitCashPaidInput,
                                        onValueChange = { splitCashPaidInput = it },
                                        label = { Text("Cash Tendered Portion ($curr)") },
                                        placeholder = { Text("e.g. 100.00") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth().testTag("split_cash_input")
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Remaining E-Wallet:", style = MaterialTheme.typography.bodySmall)
                                        FilterChip(
                                            selected = selectedSplitWallet == "GCash",
                                            onClick = { selectedSplitWallet = "GCash" },
                                            label = { Text("GCash") }
                                        )
                                        FilterChip(
                                            selected = selectedSplitWallet == "Maya",
                                            onClick = { selectedSplitWallet = "Maya" },
                                            label = { Text("Maya") }
                                        )
                                    }

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Cash Paid:", style = MaterialTheme.typography.bodySmall)
                                                Text("$curr${String.format(Locale.getDefault(), "%.2f", parsedSplitCash)}", fontWeight = FontWeight.Bold)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Balance via $selectedSplitWallet:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                                Text("$curr${String.format(Locale.getDefault(), "%.2f", splitWalletBalance)}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                            }
                                            if (splitCashChange > 0.0) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("Cash Change:", style = MaterialTheme.typography.bodySmall)
                                                    Text("$curr${String.format(Locale.getDefault(), "%.2f", splitCashChange)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        }
                                    }

                                    HeroInput(
                                        value = ewalletRefNumber,
                                        onValueChange = { ewalletRefNumber = it },
                                        label = { Text("$selectedSplitWallet Ref # (Optional)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        HeroInput(
                            value = customerNameInput,
                            onValueChange = { customerNameInput = it },
                            label = { Text("Customer Name (Optional)") },
                            placeholder = { Text("e.g. Walk-in or Juan Dela Cruz") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("customer_name_input")
                        )

                        // Submit Checkout Actions
                        val isSubmitEnabled = when (selectedPaymentMethod) {
                            "CASH" -> totalAmount <= 0.0 || (amountPaidInput.isNotBlank() && parsedPayAmount >= totalAmount)
                            "SPLIT" -> parsedSplitCash > 0.0 || splitWalletBalance > 0.0
                            else -> true // GCASH or MAYA exact digital payment
                        }

                        HeroButton(
                            onClick = {
                                val finalMethod = when (selectedPaymentMethod) {
                                    "SPLIT" -> "SPLIT ($selectedSplitWallet)"
                                    else -> selectedPaymentMethod
                                }
                                val finalCash = when (selectedPaymentMethod) {
                                    "CASH" -> parsedPayAmount
                                    "SPLIT" -> parsedSplitCash
                                    else -> totalAmount
                                }
                                val finalCustomer = when {
                                    selectedPaymentMethod == "SPLIT" -> {
                                        val base = customerNameInput.ifBlank { "Walk-in" }
                                        val refStr = if (ewalletRefNumber.isNotBlank()) " Ref:$ewalletRefNumber" else ""
                                        "$base [Cash: $curr${String.format(Locale.US, "%.2f", parsedSplitCash)}, $selectedSplitWallet: $curr${String.format(Locale.US, "%.2f", splitWalletBalance)}$refStr]"
                                    }
                                    selectedPaymentMethod != "CASH" && ewalletRefNumber.isNotBlank() -> {
                                        val base = customerNameInput.ifBlank { "Walk-in" }
                                        "$base [Ref: $ewalletRefNumber]"
                                    }
                                    else -> customerNameInput
                                }

                                executePaymentAction(
                                    viewModel = viewModel,
                                    paymentMethod = finalMethod,
                                    cashPaid = finalCash,
                                    subtotal = subtotal,
                                    taxAmount = taxAmount,
                                    totalAmount = totalAmount,
                                    cartLineItems = activeCartItems,
                                    customerName = finalCustomer,
                                    onComplete = { tx, items ->
                                        // Save completed tx state to open printable Receipt Dialog
                                        val pendingServices = DigitalServicesHelper.parseDigitalServiceItems(items)
                                        pendingDigitalServices = pendingServices
                                        lastCompletedTransaction = tx
                                        lastCompletedItems = items
                                        showPaymentDialog = false
                                        showReceiptDialog = true
                                        if ((selectedPaymentMethod == "CASH" || selectedPaymentMethod == "SPLIT") && viewModel.isAutoKickDrawerEnabled.value) {
                                            viewModel.kickCashDrawer { _, _ -> }
                                        }
                                    }
                                )
                            },
                            enabled = isSubmitEnabled,
                            modifier = Modifier.fillMaxWidth().testTag("payment_submit_button"),
                            text = "Complete Checkout",
                            icon = Icons.Filled.CheckCircle,
                            color = HeroColor.Primary,
                            variant = HeroButtonVariant.Solid,
                            size = HeroSize.Lg
                        )
                    }
                }
            }
        }

        // 7) RECEIPT DIALOG (Prints, exports receipt PDF files natively)
        if (showReceiptDialog && lastCompletedTransaction != null) {
            val transaction = lastCompletedTransaction!!
            val items = lastCompletedItems

            AlertDialog(
                onDismissRequest = {
                    showReceiptDialog = false
                    if (pendingDigitalServices.isNotEmpty()) {
                        showPostCheckoutDigitalDialog = true
                    } else {
                        lastCompletedTransaction = null
                        lastCompletedItems = emptyList()
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val currentPrinterType by viewModel.printerType.collectAsState()
                        if (currentPrinterType != com.munzo.storepoint.util.EscPosHelper.PrinterType.SYSTEM_SPOOLER.name) {
                            FilledTonalButton(
                                onClick = {
                                    viewModel.printReceiptHardware(transaction, items) { _, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.testTag("receipt_thermal_print")
                            ) {
                                Icon(Icons.Default.Print, null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ESC/POS")
                            }
                        }

                        Button(
                            onClick = {
                                storeConfig?.let { config ->
                                    PdfExportUtil.exportReceiptPdf(context, config, transaction, items)
                                }
                            },
                            modifier = Modifier.testTag("receipt_share_pdf")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showReceiptDialog = false
                            if (pendingDigitalServices.isNotEmpty()) {
                                showPostCheckoutDigitalDialog = true
                            } else {
                                lastCompletedTransaction = null
                                lastCompletedItems = emptyList()
                            }
                        },
                        modifier = Modifier.testTag("receipt_done")
                    ) {
                        Text(if (pendingDigitalServices.isNotEmpty()) "Next: Fulfill Digital Service" else "Done")
                    }
                },
                title = { Text("Checkout Transaction Success") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green, modifier = Modifier.size(56.dp))
                        Text("Transaction #STP-${transaction.id} registered offline.", fontWeight = FontWeight.Bold)
                        if (transaction.customerName.isNotBlank()) {
                            Text("Customer: ${transaction.customerName}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("Total Amount: $curr${String.format(Locale.getDefault(), "%.2f", transaction.totalAmount)}")
                        Text("Cash Paid: $curr${String.format(Locale.getDefault(), "%.2f", transaction.cashPaid)}")
                        Text("Change: $curr${String.format(Locale.getDefault(), "%.2f", transaction.changeAmount)}")

                        // Prominent banner if pay-first digital service items need fulfillment
                        if (pendingDigitalServices.isNotEmpty()) {
                            HeroCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ElectricBolt,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "DIGITAL SERVICE READY TO SEND",
                                            fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Text(
                                        text = "Customer has paid cash. Launch external app now to complete ${pendingDigitalServices.size} service(s).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Button(
                                        onClick = {
                                            showReceiptDialog = false
                                            showPostCheckoutDigitalDialog = true
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Icon(Icons.Default.Launch, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open Service App Now")
                                    }
                                }
                            }
                        }

                        HeroDivider()
                        Text("You can export the printable invoice receipt immediately in PDF format above for standard thermal printer sizes.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            )
        }

        // 7b) POST-CHECKOUT DIGITAL SERVICE ACTION DIALOG (Pay-First GCash, Maya, Load Fulfillment)
        if (showPostCheckoutDigitalDialog && pendingDigitalServices.isNotEmpty()) {
            val adminPrefs = remember { context.getSharedPreferences(DigitalServicesHelper.PREFS_NAME, Context.MODE_PRIVATE) }
            val preferredLoadApp = remember(adminPrefs) { DigitalServicesHelper.getPreferredLoadApp(adminPrefs) }

            AlertDialog(
                onDismissRequest = {
                    showPostCheckoutDigitalDialog = false
                    pendingDigitalServices = emptyList()
                    lastCompletedTransaction = null
                    lastCompletedItems = emptyList()
                },
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

                        pendingDigitalServices.forEach { item ->
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
                                            Button(
                                                onClick = {
                                                    DigitalServicesHelper.launchGCash(context)
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = GCashColor)
                                            ) {
                                                Icon(Icons.Default.Launch, null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    if (item.action == "Cash Out") "Open GCash (Verify Transfer)"
                                                    else "Open GCash (Send ₱${String.format(Locale.getDefault(), "%.2f", item.amount)})"
                                                )
                                            }
                                            if (item.mobileNumber.isNotBlank()) {
                                                OutlinedButton(
                                                    onClick = {
                                                        val smsMsg = "GCash ${item.action}: ₱${String.format(Locale.getDefault(), "%.2f", item.amount)} processed at store."
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
                                            Button(
                                                onClick = {
                                                    DigitalServicesHelper.launchMaya(context)
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = MayaColor)
                                            ) {
                                                Icon(Icons.Default.Launch, null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    if (item.action == "Cash Out") "Open Maya (Verify Transfer)"
                                                    else "Open Maya (Send ₱${String.format(Locale.getDefault(), "%.2f", item.amount)})"
                                                )
                                            }
                                            if (item.mobileNumber.isNotBlank()) {
                                                OutlinedButton(
                                                    onClick = {
                                                        val smsMsg = "Maya ${item.action}: ₱${String.format(Locale.getDefault(), "%.2f", item.amount)} processed at store."
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
                                                Button(
                                                    onClick = {
                                                        DigitalServicesHelper.launchDialer(context, ussdCode)
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(containerColor = LoadColor)
                                                ) {
                                                    Icon(Icons.Default.Call, null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Dial ($ussdCode)", style = MaterialTheme.typography.labelSmall)
                                                }
                                                OutlinedButton(
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
                                                OutlinedButton(
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
                                                FilledTonalButton(
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
                    Button(
                        onClick = {
                            showPostCheckoutDigitalDialog = false
                            pendingDigitalServices = emptyList()
                            lastCompletedTransaction = null
                            lastCompletedItems = emptyList()
                        }
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("All Services Completed")
                    }
                }
            )
        }

        // 7c) AUTHORIZED TERMINAL APPS QUICK LAUNCH DIALOG
        if (showTerminalAppsDialog) {
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
                onDismissRequest = { showTerminalAppsDialog = false },
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
                                launchableApps.forEach { (pkg, label) ->
                                    HeroCard(
                                        onClick = {
                                            DigitalServicesHelper.launchAppByPackage(context, pkg)
                                            showTerminalAppsDialog = false
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
                    TextButton(onClick = { showTerminalAppsDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Quick Variant / UOM Selection Dialog for Sari-Sari Selling
        if (productForVariantPicker != null) {
            val prod = productForVariantPicker!!
            val uomOptions = viewModel.getActiveUomsForProduct(prod)
            AlertDialog(
                onDismissRequest = { productForVariantPicker = null },
                title = {
                    Column {
                        Text("Select Size / Variant", fontWeight = FontWeight.Bold)
                        Text(prod.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Available Stock: ${prod.stockCount} ${prod.baseUom.ifBlank { "pcs" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        uomOptions.forEach { opt ->
                            HeroCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addToCart(prod, 1)
                                        viewModel.selectCartUom(prod.id, opt)
                                        viewModel.playBeep()
                                        productForVariantPicker = null
                                    }
                                    .testTag("variant_pick_${opt.name.replace(" ", "_")}"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(opt.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        if (opt.multiplier > 1) {
                                            Text("Deducts ${opt.multiplier} base units", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                    Text(
                                        "$curr${String.format(Locale.getDefault(), "%.2f", opt.price)}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { productForVariantPicker = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Custom Item Discount Dialog
        if (productForDiscountDialog != null) {
            val prod = productForDiscountDialog!!
            val activeUom = selectedCartUoms[prod.id] ?: com.munzo.storepoint.data.UomOption("Base Unit", 1, prod.price)
            val basePrice = activeUom.price
            val currentDiscount = cartItemDiscounts[prod.id] ?: 0.0

            var discountMode by remember { mutableIntStateOf(0) } // 0 = Percent, 1 = Fixed Amount
            var percentInput by remember { mutableStateOf(if (currentDiscount > 0.0 && basePrice > 0) String.format(Locale.US, "%.0f", (currentDiscount / basePrice) * 100) else "10") }
            var amountInput by remember { mutableStateOf(if (currentDiscount > 0.0) String.format(Locale.US, "%.2f", currentDiscount) else "5.00") }

            val parsedPercent = percentInput.replace(',', '.').toDoubleOrNull() ?: 0.0
            val parsedAmount = amountInput.replace(',', '.').toDoubleOrNull() ?: 0.0

            val calculatedDiscount = if (discountMode == 0) {
                StorePointRepository.roundMoney((basePrice * (parsedPercent / 100.0)).coerceIn(0.0, basePrice))
            } else {
                StorePointRepository.roundMoney(parsedAmount.coerceIn(0.0, basePrice))
            }
            val discountedPrice = (basePrice - calculatedDiscount).coerceAtLeast(0.0)

            AlertDialog(
                onDismissRequest = { productForDiscountDialog = null },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Percent, null, tint = MaterialTheme.colorScheme.primary)
                        Text("Custom Item Discount", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = prod.name,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Original Unit Price: $curr${String.format(Locale.getDefault(), "%.2f", basePrice)} (${activeUom.name})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        // Mode Selector: Percent vs Fixed
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = discountMode == 0,
                                onClick = { discountMode = 0 },
                                label = { Text("Percentage (%)") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = discountMode == 1,
                                onClick = { discountMode = 1 },
                                label = { Text("Fixed Amount ($curr)") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (discountMode == 0) {
                            // Quick Percentage Presets
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("5", "10", "15", "20", "50").forEach { pct ->
                                    SuggestionChip(
                                        onClick = { percentInput = pct },
                                        label = { Text("$pct%") }
                                    )
                                }
                            }
                            HeroInput(
                                value = percentInput,
                                onValueChange = { percentInput = it },
                                label = { Text("Discount Percentage") },
                                trailingIcon = { Text("%", modifier = Modifier.padding(end = 12.dp), fontWeight = FontWeight.Bold) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("discount_percent_input")
                            )
                        } else {
                            // Quick Fixed Amount Presets
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(5.0, 10.0, 20.0, 50.0).forEach { amt ->
                                    if (amt < basePrice) {
                                        SuggestionChip(
                                            onClick = { amountInput = String.format(Locale.US, "%.2f", amt) },
                                            label = { Text("$curr$amt") }
                                        )
                                    }
                                }
                            }
                            HeroInput(
                                value = amountInput,
                                onValueChange = { amountInput = it },
                                label = { Text("Discount Amount ($curr)") },
                                leadingIcon = { Text(curr, modifier = Modifier.padding(start = 12.dp), fontWeight = FontWeight.Bold) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("discount_amount_input")
                            )
                        }

                        // Summary preview
                        HeroCard(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Discount per unit:", style = MaterialTheme.typography.bodySmall)
                                    Text("-$curr${String.format(Locale.getDefault(), "%.2f", calculatedDiscount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Discounted Price:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text("$curr${String.format(Locale.getDefault(), "%.2f", discountedPrice)}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.setItemDiscount(prod.id, calculatedDiscount)
                            productForDiscountDialog = null
                            Toast.makeText(context, "Discount applied: -$curr${String.format(Locale.getDefault(), "%.2f", calculatedDiscount)}/unit", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("apply_discount_btn")
                    ) {
                        Text("Apply Discount")
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (currentDiscount > 0.0) {
                            TextButton(
                                onClick = {
                                    viewModel.setItemDiscount(prod.id, 0.0)
                                    productForDiscountDialog = null
                                    Toast.makeText(context, "Discount removed", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.testTag("remove_discount_btn")
                            ) {
                                Text("Remove")
                            }
                        }
                        TextButton(onClick = { productForDiscountDialog = null }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }

        // Returns, Refunds and Item Exchanges Dialog
        if (showReturnRefundDialog) {
            ReturnRefundDialog(
                viewModel = viewModel,
                onDismiss = { showReturnRefundDialog = false }
            )
        }

        // Bulk-to-Retail Repackaging & Production (BOM Engine)
        if (showBulkProductionDialog) {
            BulkProductionDialog(
                viewModel = viewModel,
                onDismiss = { showBulkProductionDialog = false }
            )
        }

        // Drawer Cash Management (Cash In / Cash Out Dialog)
        if (showDrawerCashInOutDialog) {
            DrawerCashInOutDialog(
                viewModel = viewModel,
                onDismiss = { showDrawerCashInOutDialog = false }
            )
        }

        // Fast Cashier PIN Switch Dialog
        if (showFastCashierSwitchDialog) {
            FastCashierSwitchDialog(
                viewModel = viewModel,
                onDismiss = { showFastCashierSwitchDialog = false }
            )
        }

        // 8) ABOUT CREATOR & DEVELOPER DIALOG
        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                confirmButton = {
                    Button(
                        onClick = { showAboutDialog = false },
                        modifier = Modifier.testTag("about_dialog_close")
                    ) {
                        Text("Awesome")
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("About App & Creator", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Dev Avatar Block
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "GitHub Developer",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "uznom",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "GitHub: @uznom",
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            TextButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/uznom"))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "https://github.com/uznom",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        HeroDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // App Technical Highlights
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "SYSTEM INFO & DETAILS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            HeroCard(
                                modifier = Modifier.fillMaxWidth().glassPanel(cornerRadius = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "• StorePoint POS Terminal v$APP_VERSION",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "• Powered by Jetpack Compose & Material 3 design system",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "• Advanced Room Local Cache database offline-first stability",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "• Live camera barcode engine powered by Google ML Kit scanner SDK",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "• PDF Exports of receipts, cashier badges, and full shop audits instantly",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Crafted with dedication for robust offline retail administration.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            )

            // Restored-basket notice. Overlaid rather than inserted into the layout so
            // it never shifts the POS grid — the cashier's muscle memory for product
            // positions must stay valid (Fitts's Law: don't move the targets).
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                AnimatedVisibility(
                    visible = showRestoredNotice,
                    enter = fadeIn() + slideInVertically { -it },
                    exit = fadeOut() + slideOutVertically { -it }
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("cart_restored_notice")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Restored $restoredLineCount item${if (restoredLineCount == 1) "" else "s"} from your last session",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }


    }

// Private helper to wrap ViewModel actions securely
private fun executePaymentAction(
    viewModel: StorePointViewModel,
    paymentMethod: String,
    cashPaid: Double,
    subtotal: Double,
    taxAmount: Double,
    totalAmount: Double,
    cartLineItems: List<Pair<Product, Int>>,
    customerName: String = "",
    onComplete: (Transaction, List<TransactionItem>) -> Unit
) {
    val selectedUoms = viewModel.selectedCartUoms.value
    val currentDiscounts = viewModel.cartItemDiscounts.value
    val itemsToSave = cartLineItems.map { lineItem ->
        val prod = lineItem.first
        val uom = selectedUoms[prod.id] ?: com.munzo.storepoint.data.UomOption("Base Unit", 1, prod.price)
        val discount = currentDiscounts[prod.id] ?: 0.0
        val effectivePrice = (uom.price - discount).coerceAtLeast(0.0)
        val baseName = if (uom.name.equals("Base Unit", ignoreCase = true) || uom.name.equals("Piece", ignoreCase = true) || uom.name.equals("Stick", ignoreCase = true)) {
            prod.name
        } else {
            "${prod.name} (${uom.name})"
        }
        val nameWithUom = if (discount > 0.0) {
            val currSym = viewModel.storeConfig.value?.currencySymbol ?: "$"
            "$baseName (Disc -$currSym${String.format(Locale.US, "%.2f", discount)})"
        } else {
            baseName
        }
        TransactionItem(
            transactionId = 0, // Stub placeholder during checkout compilation
            productId = prod.id,
            productName = nameWithUom,
            price = StorePointRepository.roundMoney(effectivePrice),
            quantity = lineItem.second,
            cost = prod.cost * uom.multiplier,
            uomName = uom.name
        )
    }

    viewModel.executeCheckout(
        paymentMethod = paymentMethod,
        cashPaid = if (paymentMethod == "CARD" || paymentMethod == "GCASH" || paymentMethod == "MAYA") totalAmount else cashPaid,
        subtotal = subtotal,
        taxAmount = taxAmount,
        totalAmount = totalAmount,
        checkoutItems = cartLineItems,
        customerName = customerName,
        onSuccess = { createdTransaction ->
            val finalItems = itemsToSave.map { it.copy(transactionId = createdTransaction.id) }
            onComplete(createdTransaction, finalItems)
        }
    )
}

