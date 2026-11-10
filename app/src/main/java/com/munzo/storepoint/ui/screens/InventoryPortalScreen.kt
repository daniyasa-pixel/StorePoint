package com.munzo.storepoint.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.munzo.storepoint.data.Category
import com.munzo.storepoint.data.Product
import com.munzo.storepoint.ui.BarcodeScannerDialog
import com.munzo.storepoint.ui.StorePointViewModel
import com.munzo.storepoint.ui.components.ExpressiveButton
import com.munzo.storepoint.ui.components.ExpressiveButtonSize
import com.munzo.storepoint.ui.components.ExpressiveButtonVariant
import com.munzo.storepoint.ui.components.ExpressiveWidthClass
import com.munzo.storepoint.ui.components.rememberExpressiveWidthClass
import com.munzo.storepoint.ui.layout.ScanSearchField
import com.munzo.storepoint.ui.theme.*
import com.munzo.storepoint.ui.theme.HeroUITheme
import com.munzo.storepoint.ui.components.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryPortalScreen(
    viewModel: StorePointViewModel,
    onLogout: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val storeConfig by viewModel.storeConfig.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val products by viewModel.allProducts.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    val curr = storeConfig?.currencySymbol ?: "₱"
    var selectedPortalTab by remember { mutableStateOf(0) } // 0: Price Checker, 1: Stocktake Audit, 2: Quick Share
    var showBulkProductionDialog by remember { mutableStateOf(false) }
    var showReceiveDialogForProduct by remember { mutableStateOf<Product?>(null) }
    var showProductPickerForReceive by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onBack != null && activeUser?.role == "ADMIN") {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back to Admin")
                        }
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = storeConfig?.storeName ?: "StorePoint",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "Inventory & Audit Console",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    val widthClass = rememberExpressiveWidthClass()
                    val showLabels = widthClass != ExpressiveWidthClass.COMPACT

                    if (showLabels) {
                        // Receive Stock Button
                        HeroButton(
                            onClick = { showProductPickerForReceive = true },
                            modifier = Modifier.testTag("portal_receive_stock_btn"),
                            text = "Receive Stock",
                            icon = Icons.Default.AddBusiness,
                            color = HeroColor.Primary,
                            variant = HeroButtonVariant.Flat,
                            size = HeroSize.Sm
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Bulk Repackaging / BOM Production Dialog
                        HeroButton(
                            onClick = { showBulkProductionDialog = true },
                            modifier = Modifier.testTag("portal_bulk_production_btn"),
                            text = "Repackage / BOM",
                            icon = Icons.Default.PrecisionManufacturing,
                            color = HeroColor.Primary,
                            variant = HeroButtonVariant.Flat,
                            size = HeroSize.Sm
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Quick Share Action Button in TopBar (wide layouts)
                        HeroButton(
                            onClick = { viewModel.shareInventoryViaQuickShare(context) },
                            modifier = Modifier.testTag("portal_quick_share_btn"),
                            text = "Quick Share",
                            icon = Icons.Default.Share,
                            color = HeroColor.Default,
                            variant = HeroButtonVariant.Bordered,
                            size = HeroSize.Sm
                        )
                    } else {
                        // Compact layouts: icon-only to avoid a cramped/clipped app bar
                        FilledTonalIconButton(
                            onClick = { showProductPickerForReceive = true },
                            modifier = Modifier.size(38.dp).clip(HeroUITheme.radius.md).testTag("portal_receive_stock_btn"),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = HeroUITheme.colors.accent.copy(alpha = 0.14f),
                                contentColor = HeroUITheme.colors.accent
                            )
                        ) {
                            Icon(Icons.Default.AddBusiness, contentDescription = "Receive Stock", modifier = Modifier.size(18.dp))
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        FilledTonalIconButton(
                            onClick = { showBulkProductionDialog = true },
                            modifier = Modifier.size(38.dp).clip(HeroUITheme.radius.md).testTag("portal_bulk_production_btn"),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = HeroUITheme.colors.surfaceSecondary,
                                contentColor = HeroUITheme.colors.foreground
                            )
                        ) {
                            Icon(Icons.Default.PrecisionManufacturing, contentDescription = "Repackage / BOM", modifier = Modifier.size(18.dp))
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        FilledTonalIconButton(
                            onClick = { viewModel.shareInventoryViaQuickShare(context) },
                            modifier = Modifier.size(38.dp).clip(HeroUITheme.radius.md).testTag("portal_quick_share_btn"),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = HeroUITheme.colors.surfaceSecondary,
                                contentColor = HeroUITheme.colors.foreground
                            )
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Quick Share", modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Logged in Staff Badge & Logout
                    IconButton(
                        onClick = {
                            viewModel.logout()
                            onLogout()
                        },
                        modifier = Modifier.testTag("portal_logout_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
        ) { padding ->
        val widthClass = rememberExpressiveWidthClass()
        val isCompact = widthClass == ExpressiveWidthClass.COMPACT

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = if (isCompact) Dp.Unspecified else 1100.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // User Status Pill & Expressive Tabs (stacks on compact windows so the
            // segmented tabs are never squeezed or clipped)
            if (isCompact) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroChip(
                        text = "Auditor: ${activeUser?.username?.uppercase() ?: "STAFF"}",
                        variant = HeroChipVariant.Flat,
                        color = HeroColor.Primary
                    )

                    HeroSegmentedTabs(
                        items = listOf("Price Check", "Stocktake", "Quick Share"),
                        selectedItem = when (selectedPortalTab) {
                            0 -> "Price Check"
                            1 -> "Stocktake"
                            else -> "Quick Share"
                        },
                        onItemSelected = { item ->
                            selectedPortalTab = when (item) {
                                "Price Check" -> 0
                                "Stocktake" -> 1
                                else -> 2
                            }
                        },
                        labelProvider = { it },
                        iconProvider = { item ->
                            when (item) {
                                "Price Check" -> Icons.Default.QrCodeScanner
                                "Stocktake" -> Icons.Default.FactCheck
                                else -> Icons.Default.Share
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("portal_segmented_tabs")
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeroChip(
                        text = "Auditor: ${activeUser?.username?.uppercase() ?: "STAFF"}",
                        variant = HeroChipVariant.Flat,
                        color = HeroColor.Primary
                    )

                    HeroSegmentedTabs(
                        items = listOf("Price Check", "Stocktake Audit", "Quick Share"),
                        selectedItem = when (selectedPortalTab) {
                            0 -> "Price Check"
                            1 -> "Stocktake Audit"
                            else -> "Quick Share"
                        },
                        onItemSelected = { item ->
                            selectedPortalTab = when (item) {
                                "Price Check" -> 0
                                "Stocktake Audit" -> 1
                                else -> 2
                            }
                        },
                        labelProvider = { it },
                        iconProvider = { item ->
                            when (item) {
                                "Price Check" -> Icons.Default.QrCodeScanner
                                "Stocktake Audit" -> Icons.Default.FactCheck
                                else -> Icons.Default.Share
                            }
                        },
                        modifier = Modifier.testTag("portal_segmented_tabs")
                    )
                }
            }

            AnimatedContent(
                targetState = selectedPortalTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "portal_content"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> InventoryPriceCheckerTab(
                        products = products,
                        categories = categories,
                        curr = curr,
                        viewModel = viewModel,
                        onReceiveProduct = { showReceiveDialogForProduct = it }
                    )
                    1 -> InventoryStocktakeAuditTab(
                        products = products,
                        categories = categories,
                        curr = curr,
                        viewModel = viewModel,
                        onReceiveProduct = { showReceiveDialogForProduct = it }
                    )
                    2 -> InventoryQuickShareHubTab(viewModel = viewModel, totalProducts = products.size, totalCategories = categories.size)
                }
            }
        }
        }
    }

    if (showReceiveDialogForProduct != null) {
        val prod = showReceiveDialogForProduct!!
        val drawerCash = viewModel.getCurrentDrawerCash()
        ReceiveInventoryDialog(
            product = prod,
            currencySymbol = curr,
            drawerCash = drawerCash,
            onDismiss = { showReceiveDialogForProduct = null },
            onConfirm = { quantity, multiplier, totalCost, payFromDrawer ->
                viewModel.restockProductWithUom(
                    product = prod,
                    quantity = quantity,
                    multiplier = multiplier,
                    totalCost = totalCost,
                    payFromDrawer = payFromDrawer,
                    onPayoutError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
                showReceiveDialogForProduct = null
            }
        )
    }

    if (showProductPickerForReceive) {
        var pickerQuery by remember { mutableStateOf("") }
        val pickerFiltered = remember(pickerQuery, products) {
            if (pickerQuery.isBlank()) products.take(30)
            else products.filter {
                it.name.contains(pickerQuery, ignoreCase = true) ||
                it.barcode.contains(pickerQuery, ignoreCase = true)
            }
        }
        AlertDialog(
            onDismissRequest = { showProductPickerForReceive = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AddBusiness, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Select Product to Receive Stock", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroInput(
                        value = pickerQuery,
                        onValueChange = { pickerQuery = it },
                        label = { Text("Search product name or barcode") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("receive_picker_search_input")
                    )
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(pickerFiltered) { item ->
                            HeroCard(
                                onClick = {
                                    showProductPickerForReceive = false
                                    showReceiveDialogForProduct = item
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("Stock: ${item.stockCount} ${item.baseUom} • Barcode: ${item.barcode.ifEmpty { "N/A" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                HeroTextButton(onClick = { showProductPickerForReceive = false }) { Text("Close") }
            }
        )
    }

    if (showBulkProductionDialog) {
        BulkProductionDialog(
            viewModel = viewModel,
            onDismiss = { showBulkProductionDialog = false }
        )
    }
}
