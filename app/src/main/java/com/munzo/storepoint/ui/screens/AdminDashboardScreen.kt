package com.munzo.storepoint.ui.screens

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.items
import com.munzo.storepoint.data.*
import com.munzo.storepoint.ui.theme.*
import com.munzo.storepoint.ui.theme.HeroUITheme
import com.munzo.storepoint.ui.components.*
import androidx.compose.ui.platform.LocalConfiguration
import com.munzo.storepoint.ui.StorePointViewModel
import com.munzo.storepoint.ui.KioskStatusBar
import com.munzo.storepoint.ui.layout.WindowLayout
import com.munzo.storepoint.ui.layout.rememberWindowLayout
import com.munzo.storepoint.ui.components.ExpressiveButton
import com.munzo.storepoint.ui.components.ExpressiveButtonSize
import com.munzo.storepoint.ui.components.ExpressiveButtonVariant
import com.munzo.storepoint.util.PdfExportUtil
import com.munzo.storepoint.util.BiometricAuthHelper
import com.munzo.storepoint.util.CrashDiagnosticsManager
import com.munzo.storepoint.util.DatabaseBackupManager
import com.munzo.storepoint.util.EscPosHelper
import com.munzo.storepoint.util.SecurityHelper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: StorePointViewModel,
    onBackToPOS: () -> Unit,
    onNavigateToInventoryPortal: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val storeConfig by viewModel.storeConfig.collectAsState()
    val isKioskActive by viewModel.isKioskModeActive.collectAsState()

    val categories by viewModel.allCategories.collectAsState()
    val products by viewModel.allProducts.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val transactionItems by viewModel.allTransactionItems.collectAsState()
    val sessions by viewModel.allCashierSessions.collectAsState()
    val users by viewModel.allUsers.collectAsState()

    var activeTab by remember { mutableStateOf("products") } // tabs: "products"

    var deleteConfirmType by remember { mutableStateOf<String?>(null) }
    var deleteConfirmId by remember { mutableStateOf<Any?>(null) }
    var deleteConfirmName by remember { mutableStateOf("") }

    // Dialog flags
    var showProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var productToRestock by remember { mutableStateOf<Product?>(null) }
    var productForVariants by remember { mutableStateOf<Product?>(null) }

    var showCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var showUserDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<User?>(null) }
    var showBulkProductionDialog by remember { mutableStateOf(false) }

    val curr = storeConfig?.currencySymbol ?: "$"

    val tabs = listOf(
        Triple("analytics", "Analytics", Icons.Default.Assessment),
        Triple("products", "Catalog Inventory", Icons.Default.Inventory),
        Triple("suppliers", "Suppliers & POs", Icons.Default.LocalShipping),
        Triple("categories", "Categories", Icons.Default.Category),
        Triple("users", "Staff Accounts", Icons.Default.People),
        Triple("calendar", "Payment Calendar", Icons.Default.CalendarMonth),
        Triple("security", "Security", Icons.Default.Security),
        Triple("about", "About Information", Icons.Default.Info)
    )

    val windowLayout = rememberWindowLayout()
    var showMoreNavSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                if (isKioskActive) {
                    KioskStatusBar(viewModel = viewModel)
                }
                TopAppBar(
                    windowInsets = if (isKioskActive) WindowInsets(0, 0, 0, 0) else TopAppBarDefaults.windowInsets,
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Mini beautiful "SP" logo badge matching launcher/app design
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "SP",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
                                    ),
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                                )
                            }
                            Text("Admin Center", fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackToPOS) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "POS Register")
                        }
                    },
                    actions = {
                        if (onNavigateToInventoryPortal != null) {
                            IconButton(
                                onClick = onNavigateToInventoryPortal,
                                modifier = Modifier.testTag("admin_open_inventory_portal_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = "Inventory Portal",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(
                            onClick = { showBulkProductionDialog = true },
                            modifier = Modifier.testTag("admin_open_production_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PrecisionManufacturing,
                                contentDescription = "Bulk Repackaging & Production",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = {
                                storeConfig?.let { config ->
                                    PdfExportUtil.exportSalesAndInventoryReportPdf(context, config, transactions, products)
                                }
                            },
                            modifier = Modifier.testTag("export_report_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Export Audit PDF",
                                tint = MaterialTheme.colorScheme.primary
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
        },
        bottomBar = {
            // Adaptive bottom navigation for compact widths:
            // Sized in Scaffold bottomBar for dynamic auto-adjusting height when navigation/status bars are toggled!
            if (windowLayout == WindowLayout.Compact) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HeroDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    val primaryTabs = tabs.take(4)
                    val moreTabs = tabs.drop(4)
                    NavigationBar(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        primaryTabs.forEach { (id, label, icon) ->
                            NavigationBarItem(
                                selected = activeTab == id,
                                onClick = { activeTab = id },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                        NavigationBarItem(
                            selected = moreTabs.any { it.first == activeTab },
                            onClick = { showMoreNavSheet = true },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (moreTabs.any { it.first == activeTab }) {
                                            Badge { Text("•") }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.MoreHoriz, contentDescription = "More sections")
                                }
                            },
                            label = { Text("More", maxLines = 1) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            if (windowLayout != WindowLayout.Compact) {
                // Tablet / expanded widths: HeroUI Segmented Navigation Tabs
                HeroSegmentedTabs(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    items = tabs.map { it.first },
                    selectedItem = activeTab,
                    onItemSelected = { activeTab = it },
                    labelProvider = { id -> tabs.find { it.first == id }?.second ?: id },
                    iconProvider = { id -> tabs.find { it.first == id }?.third ?: Icons.Default.Circle }
                )
            }

            // Tab Screen views
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                when (activeTab) {
                    "analytics" -> AnalyticsTab(
                        transactions,
                        transactionItems,
                        sessions,
                        products,
                        curr,
                        storeConfig?.gcashBalance ?: 0.0,
                        storeConfig?.smartLoadBalance ?: 0.0,
                        storeConfig?.globeLoadBalance ?: 0.0,
                        storeConfig = storeConfig,
                        viewModel = viewModel
                    )
                    "products" -> ProductsTab(products, categories, curr, onEdit = {
                        editingProduct = it
                        showProductDialog = true
                    }, onDelete = { id ->
                        val item = products.find { it.id == id }
                        deleteConfirmName = item?.name ?: "Product #$id"
                        deleteConfirmId = id
                        deleteConfirmType = "product"
                    }, onAddClick = {
                        editingProduct = null
                        showProductDialog = true
                    }, onRestockClick = {
                        productToRestock = it
                    }, onVariantsClick = {
                        productForVariants = it
                    }, viewModel = viewModel)
                    "suppliers" -> SuppliersAndPurchasesTab(viewModel = viewModel)
                    "categories" -> CategoriesTab(
                        categories = categories,
                        onDelete = { id ->
                            val item = categories.find { it.id == id }
                            deleteConfirmName = item?.name ?: "Category #$id"
                            deleteConfirmId = id
                            deleteConfirmType = "category"
                        },
                        onAddClick = {
                            editingCategory = null
                            showCategoryDialog = true
                        },
                        onEditClick = {
                            editingCategory = it
                            showCategoryDialog = true
                        }
                    )
                    "users" -> UsersTab(
                        users = users,
                        onDelete = { name ->
                            deleteConfirmName = name
                            deleteConfirmId = name
                            deleteConfirmType = "user"
                        },
                        onAddClick = {
                            editingUser = null
                            showUserDialog = true
                        },
                        onEdit = { usr ->
                            editingUser = usr
                            showUserDialog = true
                        },
                        onPrintBadge = { usr ->
                            val config = storeConfig ?: StoreConfig(id = 1, storeName = "StorePoint POS", currencySymbol = "₱", taxPercentage = 12.0)
                            com.munzo.storepoint.util.PdfExportUtil.exportCashierBadgePdf(
                                context = context,
                                storeConfig = config,
                                username = usr.username,
                                role = usr.role,
                                barcodeId = usr.barcodeId
                            )
                        },
                        viewModel = viewModel
                    )
                    "calendar" -> CalendarTab(viewModel = viewModel)
                    "security" -> SecurityTab(viewModel)
                    "about" -> AboutScreenContent(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                }
            }

        }

        if (showMoreNavSheet) {
            ModalBottomSheet(onDismissRequest = { showMoreNavSheet = false }) {
                val moreTabs = tabs.drop(4)
                moreTabs.forEach { (id, label, icon) ->
                    ListItem(
                        headlineContent = {
                            Text(
                                label,
                                fontWeight = if (activeTab == id) FontWeight.Bold else FontWeight.Normal,
                                color = if (activeTab == id) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingContent = {
                            Icon(
                                icon,
                                contentDescription = label,
                                tint = if (activeTab == id) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier
                            .clickable {
                                activeTab = id
                                showMoreNavSheet = false
                            }
                            .testTag("admin_more_tab_$id")
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        // --- CRUD Dialog Modals ---

        // Deletion Confirmation Dialog
        if (deleteConfirmType != null) {
            AlertDialog(
                onDismissRequest = { deleteConfirmType = null },
                title = { Text(text = "Confirm Deletion", fontWeight = FontWeight.Bold) },
                text = { Text(text = "Are you absolutely sure you want to delete '$deleteConfirmName'? This choice is permanent and cannot be undone.") },
                confirmButton = {
                    ExpressiveButton(
                        onClick = {
                            val id = deleteConfirmId
                            when (deleteConfirmType) {
                                "product" -> if (id is Int) viewModel.deleteProduct(id)
                                "category" -> if (id is Int) viewModel.deleteCategory(id)
                                "user" -> if (id is String) viewModel.deleteUser(id)
                            }
                            deleteConfirmType = null
                        },
                        label = "Delete",
                        variant = ExpressiveButtonVariant.FILLED,
                        size = ExpressiveButtonSize.M,
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    )
                },
                dismissButton = {
                    HeroTextButton(onClick = { deleteConfirmType = null }, text = "Cancel")
                }
            )
        }

        // 1) PRODUCT CRUD DIALOG
        if (showProductDialog) {
            ProductCrudDialog(
                editingProduct = editingProduct,
                categories = categories,
                onDismiss = { showProductDialog = false },
                onSubmit = { name, categoryId, price, stock, barcode, expirationDate,
                             hasStick10s, priceStick10s, hasStick20s, priceStick20s, hasReam, priceReam, hasMasterCase, priceMasterCase,
                             hasCustomUom, customUomName, customUomMultiplier, customUomPrice,
                             barcode10s, barcode20s, barcodeReam, barcodeMasterCase, barcodeCustomUom, cost, baseUom, trackInventory ->
                    val prod = (editingProduct ?: Product(
                        name = name,
                        categoryId = categoryId,
                        price = price,
                        stockCount = stock,
                        barcode = barcode,
                        expirationDate = expirationDate,
                        cost = cost,
                        baseUom = baseUom,
                        trackInventory = trackInventory
                    )).copy(
                        name = name,
                        categoryId = categoryId,
                        price = price,
                        stockCount = stock,
                        barcode = barcode,
                        expirationDate = expirationDate,
                        hasStick10s = hasStick10s,
                        priceStick10s = priceStick10s,
                        hasStick20s = hasStick20s,
                        priceStick20s = priceStick20s,
                        hasReam = hasReam,
                        priceReam = priceReam,
                        hasMasterCase = hasMasterCase,
                        priceMasterCase = priceMasterCase,
                        hasCustomUom = hasCustomUom,
                        customUomName = customUomName,
                        customUomMultiplier = customUomMultiplier,
                        customUomPrice = customUomPrice,
                        barcode10s = barcode10s,
                        barcode20s = barcode20s,
                        barcodeReam = barcodeReam,
                        barcodeMasterCase = barcodeMasterCase,
                        barcodeCustomUom = barcodeCustomUom,
                        cost = cost,
                        baseUom = baseUom,
                        trackInventory = trackInventory
                    )
                    viewModel.saveProduct(prod)
                    showProductDialog = false
                }
            )
        }

        // RECEIVE INVENTORY (MULTI-UOM RESTOCK) DIALOG
        val prodToRestockLocal = productToRestock
        if (prodToRestockLocal != null) {
            ReceiveInventoryDialog(
                product = prodToRestockLocal,
                currencySymbol = curr,
                drawerCash = viewModel.getCurrentDrawerCash(),
                onDismiss = { productToRestock = null },
                onConfirm = { qty, multiplier, totalCost, payFromDrawer ->
                    viewModel.restockProductWithUom(
                        product = prodToRestockLocal,
                        quantity = qty,
                        multiplier = multiplier,
                        totalCost = totalCost,
                        payFromDrawer = payFromDrawer
                    )
                    productToRestock = null
                }
            )
        }

        // 2) CATEGORY CREATE & EDIT DIALOG
        if (showCategoryDialog) {
            var categoryNameInput by remember { mutableStateOf(editingCategory?.name ?: "") }
            var selectedColor by remember { mutableStateOf(editingCategory?.colorAccent?.ifBlank { "#2563EB" } ?: "#2563EB") }
            var selectedIcon by remember { mutableStateOf(editingCategory?.iconBadge?.ifBlank { "folder" } ?: "folder") }

            AlertDialog(
                onDismissRequest = { showCategoryDialog = false },
                confirmButton = {
                    HeroButton(
                        onClick = {
                            if (categoryNameInput.isNotBlank()) {
                                viewModel.saveCategory(
                                    Category(
                                        id = editingCategory?.id ?: 0,
                                        name = categoryNameInput.trim(),
                                        colorAccent = selectedColor,
                                        iconBadge = selectedIcon
                                    )
                                )
                                showCategoryDialog = false
                            }
                        },
                        modifier = Modifier.testTag("category_submit"),
                        text = "Save Category",
                        color = HeroColor.Primary,
                        variant = HeroButtonVariant.Solid,
                        size = HeroSize.Md
                    )
                },
                dismissButton = {
                    HeroTextButton(onClick = { showCategoryDialog = false }, text = "Cancel")
                },
                title = { Text(if (editingCategory == null) "Add Category Folder" else "Edit Category Details") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        HeroInput(
                            value = categoryNameInput,
                            onValueChange = { categoryNameInput = it },
                            label = { Text("Category Name") },
                            modifier = Modifier.fillMaxWidth().testTag("category_name_input"),
                            singleLine = true
                        )

                        // Color Accent Picker
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Color Accent:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .testTag("category_color_picker"),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CATEGORY_COLOR_PRESETS.forEach { (hex, label) ->
                                    val isSelected = selectedColor.equals(hex, ignoreCase = true)
                                    val chipColor = parseCategoryColor(hex)
                                    Surface(
                                        shape = CircleShape,
                                        color = chipColor,
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface) else null,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("category_color_chip_$hex")
                                            .clickable { selectedColor = hex }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = label,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Icon Badge Picker
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Icon Badge:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .testTag("category_icon_picker"),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CATEGORY_ICON_PRESETS.forEach { (iconKey, label) ->
                                    val isSelected = selectedIcon.equals(iconKey, ignoreCase = true)
                                    HeroFilterChip(
                                        selected = isSelected,
                                        onClick = { selectedIcon = iconKey },
                                        icon = getCategoryIconVector(iconKey),
                                        label = label,
                                        modifier = Modifier.testTag("category_icon_chip_$iconKey")
                                    )
                                }
                            }
                        }

                        // Live Preview Card
                        val previewColor = parseCategoryColor(selectedColor, MaterialTheme.colorScheme.primary)
                        val previewIcon = getCategoryIconVector(selectedIcon)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = previewColor.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, previewColor.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = previewColor,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = previewIcon,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = if (categoryNameInput.isBlank()) "Category Preview" else categoryNameInput,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Color: $selectedColor • Icon: $selectedIcon",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }        // 3) USER REGISTER DIALOG
        if (showUserDialog) {
            var inputUsername by remember { mutableStateOf(editingUser?.username ?: "") }
            var inputPin by remember { mutableStateOf("") }
            var inputBarcodeId by remember { mutableStateOf(editingUser?.barcodeId ?: "") }
            var inputRole by remember { mutableStateOf(editingUser?.role ?: "CASHIER") }
            var showStaffCameraScanner by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showUserDialog = false },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HeroButton(
                            onClick = {
                                val pinToSave = if (inputPin.isNotEmpty()) inputPin else (editingUser?.pinHash ?: "")
                                if (inputUsername.isBlank()) {
                                    Toast.makeText(context, "Username cannot be empty.", Toast.LENGTH_SHORT).show()
                                    return@HeroButton
                                }
                                if (editingUser == null && !SecurityHelper.isValidPin(inputPin)) {
                                    Toast.makeText(context, "A full ${SecurityHelper.PIN_LENGTH}-digit security PIN is required.", Toast.LENGTH_SHORT).show()
                                    return@HeroButton
                                }
                                if (inputPin.isNotEmpty() && !SecurityHelper.isValidPin(inputPin)) {
                                    Toast.makeText(context, "PIN must be exactly ${SecurityHelper.PIN_LENGTH} digits.", Toast.LENGTH_SHORT).show()
                                    return@HeroButton
                                }
                                // Enforce the same weak-PIN policy the self-service
                                // reset wizard applies, so an admin cannot create a
                                // credential the user would be refused elsewhere.
                                if (inputPin.isNotEmpty() && SecurityHelper.isWeakPin(inputPin)) {
                                    Toast.makeText(context, "That PIN is too easy to guess. ${SecurityHelper.pinPolicyHint()}", Toast.LENGTH_LONG).show()
                                    return@HeroButton
                                }

                                // Hash here rather than storing a plaintext PIN, and
                                // clear the migration flag so a newly created account
                                // is immediately usable without the reset wizard.
                                val toSave = User(
                                    username = inputUsername.trim(),
                                    pinHash = if (inputPin.isNotEmpty()) SecurityHelper.hashPin(inputPin) else pinToSave,
                                    role = inputRole,
                                    barcodeId = inputBarcodeId.trim(),
                                    biometricEnrolled = editingUser?.biometricEnrolled ?: false,
                                    pinResetRequired = false
                                )
                                viewModel.saveUser(toSave)
                                showUserDialog = false
                                if (inputRole == "INVENTORY") {
                                    Toast.makeText(context, "Inventory audit account successfully registered & certified!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("user_submit"),
                            text = if (editingUser != null) "Save Changes" else "Add Staff User",
                            icon = if (editingUser != null) Icons.Default.Save else Icons.Default.PersonAdd,
                            color = HeroColor.Primary,
                            variant = HeroButtonVariant.Solid,
                            size = HeroSize.Md
                        )

                        HeroButton(
                            onClick = { showUserDialog = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("user_cancel"),
                            text = "Cancel",
                            icon = Icons.Default.Close,
                            color = HeroColor.Default,
                            variant = HeroButtonVariant.Light,
                            size = HeroSize.Md
                        )
                    }
                },
                dismissButton = null,
                title = {
                    Text(
                        text = if (editingUser != null) "Edit Staff Account" else "Add Local Staff Account",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Start
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HeroInput(
                            value = inputUsername,
                            onValueChange = { inputUsername = it },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth().testTag("staff_username_input"),
                            singleLine = true,
                            enabled = editingUser == null
                        )

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (editingUser != null)
                                    "New ${SecurityHelper.PIN_LENGTH}-Digit PIN (Leave blank to keep)"
                                else
                                    "${SecurityHelper.PIN_LENGTH}-Digit Security PIN",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val pinIsWeak = inputPin.isNotEmpty() && SecurityHelper.isWeakPin(inputPin)
                            ExpressiveOtpPinInput(
                                pin = inputPin,
                                onPinChange = { inputPin = it },
                                isMasked = true,
                                isError = pinIsWeak,
                                modifier = Modifier.fillMaxWidth().testTag("staff_password_input")
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Live feedback: tell the staff manager the PIN is weak while
                            // they can still change it, rather than rejecting on submit.
                            Text(
                                text = when {
                                    pinIsWeak -> "Too easy to guess — ${SecurityHelper.pinPolicyHint()}"
                                    inputPin.length == SecurityHelper.PIN_LENGTH -> "✓ PIN ready"
                                    else -> "${inputPin.length}/${SecurityHelper.PIN_LENGTH} digits"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    pinIsWeak -> MaterialTheme.colorScheme.error
                                    inputPin.length == SecurityHelper.PIN_LENGTH -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outline
                                }
                            )
                        }

                        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                        val isTablet = configuration.screenWidthDp >= 600

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HeroInput(
                                value = inputBarcodeId,
                                onValueChange = { inputBarcodeId = it },
                                label = { Text("Staff Badge Barcode ID") },
                                placeholder = { Text("e.g. EMP_ID_789") },
                                modifier = Modifier.weight(1f).testTag("staff_barcode_input"),
                                singleLine = true,
                                trailingIcon = {
                                    if (inputBarcodeId.isNotEmpty()) {
                                        IconButton(onClick = { inputBarcodeId = "" }) {
                                            Icon(Icons.Default.Clear, "Clear")
                                        }
                                    }
                                }
                            )

                            if (isTablet) {
                                FilledIconButton(
                                    onClick = {
                                        showStaffCameraScanner = true
                                    },
                                    modifier = Modifier.size(56.dp).testTag("staff_barcode_scan_button")
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan with Camera")
                                }
                            }
                        }

                        if (showStaffCameraScanner && isTablet) {
                            com.munzo.storepoint.ui.BarcodeScannerDialog(
                                title = "Scan Staff Badge Barcode",
                                instruction = "Scan the staff numeric badge serial or QR code to register their account badge ID.",
                                onBarcodeScanned = { code ->
                                    inputBarcodeId = code
                                    showStaffCameraScanner = false
                                    try {
                                        val toneG = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
                                        toneG.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 120)
                                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                            try { toneG.release() } catch (ignored: Exception) {}
                                        }, 250)
                                    } catch (e: Exception) {
                                        android.util.Log.w("AdminDashboard", "Tone generation failed: ${e.message}")
                                    }
                                },
                                onDismiss = { showStaffCameraScanner = false }
                            )
                        }

                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Privilege Role:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                HeroChip(
                                    text = "Admin",
                                    variant = if (inputRole == "ADMIN") HeroChipVariant.Solid else HeroChipVariant.Flat,
                                    color = if (inputRole == "ADMIN") HeroColor.Primary else HeroColor.Default,
                                    onClick = { inputRole = "ADMIN" },
                                    modifier = Modifier.testTag("role_chip_admin")
                                )
                                HeroChip(
                                    text = "Cashier",
                                    variant = if (inputRole == "CASHIER") HeroChipVariant.Solid else HeroChipVariant.Flat,
                                    color = if (inputRole == "CASHIER") HeroColor.Primary else HeroColor.Default,
                                    onClick = { inputRole = "CASHIER" },
                                    modifier = Modifier.testTag("role_chip_cashier")
                                )
                                HeroChip(
                                    text = "Inventory",
                                    variant = if (inputRole == "INVENTORY") HeroChipVariant.Solid else HeroChipVariant.Flat,
                                    color = if (inputRole == "INVENTORY") HeroColor.Primary else HeroColor.Default,
                                    onClick = { inputRole = "INVENTORY" },
                                    modifier = Modifier.testTag("role_chip_inventory")
                                )
                            }
                        }

                        if (inputRole == "INVENTORY") {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Admin Certified Inventory Role",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Text(
                                            text = "Authorized by Administrator. Grants inventory auditing, price checks, and Quick Share sync.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

        if (productForVariants != null) {
            ProductVariantsDialog(
                product = productForVariants!!,
                viewModel = viewModel,
                onDismiss = { productForVariants = null }
            )
        }

        if (showBulkProductionDialog) {
            BulkProductionDialog(
                viewModel = viewModel,
                onDismiss = { showBulkProductionDialog = false }
            )
        }
    }
}
