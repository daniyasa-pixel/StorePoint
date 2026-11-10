package com.munzo.storepoint.ui.screens

import android.content.Context
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.munzo.storepoint.data.*
import com.munzo.storepoint.ui.StorePointViewModel
import com.munzo.storepoint.ui.theme.*
import com.munzo.storepoint.ui.components.*
import com.munzo.storepoint.util.*

@Composable
fun AdminHardwareSection(viewModel: StorePointViewModel) {
    val context = LocalContext.current
    val printerType by viewModel.printerType.collectAsState()
    val printerIp by viewModel.printerIpAddress.collectAsState()
    val printerPort by viewModel.printerPort.collectAsState()
    val printerMac by viewModel.printerBtMac.collectAsState()
    val printerDeviceName by viewModel.printerDeviceName.collectAsState()
    val printerUsbIdentifier by viewModel.printerUsbIdentifier.collectAsState()
    val is80mm by viewModel.is80mmThermal.collectAsState()
    val isAutoKick by viewModel.isAutoKickDrawerEnabled.collectAsState()
    val printerHasCutter by viewModel.printerHasAutoCutter.collectAsState()
    val storeTin by viewModel.storeTin.collectAsState()
    val storeAddress by viewModel.storeAddress.collectAsState()
    val receiptHeader by viewModel.receiptHeader.collectAsState()
    val receiptFooter by viewModel.receiptFooter.collectAsState()
    val printReceiptBarcode by viewModel.printReceiptBarcode.collectAsState()

    var editPrinterIp by remember(printerIp) { mutableStateOf(printerIp) }
    var editPrinterPort by remember(printerPort) { mutableStateOf(printerPort.toString()) }
    var editPrinterMac by remember(printerMac) { mutableStateOf(printerMac) }
    var editStoreTin by remember(storeTin) { mutableStateOf(storeTin) }
    var editStoreAddress by remember(storeAddress) { mutableStateOf(storeAddress) }
    var editReceiptHeader by remember(receiptHeader) { mutableStateOf(receiptHeader) }
    var editReceiptFooter by remember(receiptFooter) { mutableStateOf(receiptFooter) }
    var editPrintBarcode by remember(printReceiptBarcode) { mutableStateOf(printReceiptBarcode) }
    var showBtDevicesDialog by remember { mutableStateOf(false) }
    var showUsbDevicesDialog by remember { mutableStateOf(false) }

    // Kiosk App Whitelisting & Admin PIN Reset states

        // --- HARDWARE PRINTER & ESC/POS PERIPHERAL CARD ---
        HeroCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
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
                        imageVector = Icons.Default.Print,
                        contentDescription = "Hardware Printer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "Thermal Receipt Printer & Cash Drawer",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Raw ESC/POS protocol communication for POS peripherals.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Text(
                    text = "Connect Bluetooth (XP-58 Plus / POS-58), USB OTG, or Network thermal receipt printers. Direct ESC/POS byte commands enable ultra-fast receipt printing and automatic solenoid cash drawer release like in Loyverse.",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Printer Type selection
                Text("Connection Mode:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val types = listOf(
                        EscPosHelper.PrinterType.SYSTEM_SPOOLER to "System Print",
                        EscPosHelper.PrinterType.BLUETOOTH_ESCPOS to "Bluetooth",
                        EscPosHelper.PrinterType.USB_ESCPOS to "USB OTG",
                        EscPosHelper.PrinterType.NETWORK_ESCPOS to "Network LAN"
                    )
                    types.forEach { (type, label) ->
                        val isSelected = printerType == type.name
                        HeroFilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setPrinterType(type.name) },
                            label = label,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Bluetooth Configuration (XP-58 Plus / POS-58)
                if (printerType == EscPosHelper.PrinterType.BLUETOOTH_ESCPOS.name) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (printerDeviceName.isNotBlank()) "Selected: $printerDeviceName" else if (printerMac.isNotBlank()) "MAC: $printerMac" else "No Bluetooth Printer Selected",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (printerMac.isNotBlank()) {
                                    Text("Address: $printerMac", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            HeroButton(
                                onClick = { showBtDevicesDialog = true },
                                icon = Icons.Default.Search,
                                text = "Search",
                                size = HeroSize.Sm
                            )
                        }

                        Text(
                            text = "Loyverse-style one-tap pairing: tap 'Search' to pick your XP-58 Plus from paired devices (default PIN: 0000 or 1234).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        var showManualMac by remember { mutableStateOf(false) }
                        HeroTextButton(
                            onClick = { showManualMac = !showManualMac },
                            size = HeroSize.Sm,
                            text = if (showManualMac) "Hide Manual MAC Entry" else "Advanced: Enter MAC Manually"
                        )

                        if (showManualMac) {
                            HeroInput(
                                value = editPrinterMac,
                                onValueChange = {
                                    editPrinterMac = it
                                    viewModel.setPrinterBluetoothMac(it)
                                },
                                label = { Text("Bluetooth MAC Address") },
                                placeholder = { Text("00:11:22:33:44:55") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }

                // USB OTG Configuration (XP-58 Plus USB)
                if (printerType == EscPosHelper.PrinterType.USB_ESCPOS.name) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (printerDeviceName.isNotBlank()) "Selected: $printerDeviceName" else if (printerUsbIdentifier.isNotBlank()) "USB: $printerUsbIdentifier" else "No USB Printer Selected",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (printerUsbIdentifier.isNotBlank()) {
                                    Text("Identifier: $printerUsbIdentifier", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            HeroButton(
                                onClick = { showUsbDevicesDialog = true },
                                icon = Icons.Default.Refresh,
                                text = "Scan USB",
                                size = HeroSize.Sm
                            )
                        }

                        Text(
                            text = "Connect XP-58 Plus using a standard USB printer cable + USB-OTG adapter to your Android device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Network LAN Configuration
                if (printerType == EscPosHelper.PrinterType.NETWORK_ESCPOS.name) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HeroInput(
                            value = editPrinterIp,
                            onValueChange = {
                                editPrinterIp = it
                                viewModel.setPrinterNetworkConfig(it, editPrinterPort.toIntOrNull() ?: 9100)
                            },
                            label = { Text("Printer IP Address") },
                            placeholder = { Text("192.168.1.100") },
                            modifier = Modifier.weight(2f),
                            singleLine = true
                        )
                        HeroInput(
                            value = editPrinterPort,
                            onValueChange = {
                                editPrinterPort = it
                                viewModel.setPrinterNetworkConfig(editPrinterIp, it.toIntOrNull() ?: 9100)
                            },
                            label = { Text("Port") },
                            placeholder = { Text("9100") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }

                // Paper width, cutter and auto drawer switches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("80mm Paper Width", fontWeight = FontWeight.SemiBold)
                        Text(if (is80mm) "80mm Wide (48 columns)" else "58mm Standard / XP-58 Plus (32 columns)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HeroSwitch(checked = is80mm, onCheckedChange = { viewModel.setPaperWidth80mm(it) })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Automatic Paper Cutter", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (printerHasCutter) "Enabled (Sends GS V cut command)" else "Disabled (Feed lines for XP-58 Plus manual tear bar)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HeroSwitch(checked = printerHasCutter, onCheckedChange = { viewModel.setPrinterHasAutoCutter(it) })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Kick Drawer on Cash Sale", fontWeight = FontWeight.SemiBold)
                        Text("Automatically pop drawer open upon cash checkout completion (RJ11)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HeroSwitch(checked = isAutoKick, onCheckedChange = { viewModel.setAutoKickDrawerEnabled(it) })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroOutlinedButton(
                        onClick = {
                            viewModel.kickCashDrawer { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.LockOpen,
                        text = "Kick Drawer"
                    )

                    HeroButton(
                        onClick = {
                            viewModel.testHardwarePrinter { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Print,
                        text = "Test Print & Kick"
                    )
                }
            }
        }

        // --- RECEIPT BRANDING, STORE TIN & BARCODE CONFIGURATION ---
        HeroCard(
            modifier = Modifier.fillMaxWidth().testTag("receipt_branding_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
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
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = "Receipt Branding & TIN",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "Receipt Branding & Store TIN / VAT",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Custom header, footer, BIR TIN/VAT registration, and barcode printing.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                HeroInput(
                    value = editStoreTin,
                    onValueChange = { editStoreTin = it },
                    label = { Text("Store TIN / VAT Reg. Number") },
                    placeholder = { Text("VAT Reg. TIN: 000-123-456-000") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_store_tin_input"),
                    singleLine = true
                )

                HeroInput(
                    value = editStoreAddress,
                    onValueChange = { editStoreAddress = it },
                    label = { Text("Store Address / Branch") },
                    placeholder = { Text("Unit 102 Rizal Avenue, Poblacion, Metro Manila") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_store_address_input"),
                    singleLine = true
                )

                HeroInput(
                    value = editReceiptHeader,
                    onValueChange = { editReceiptHeader = it },
                    label = { Text("Custom Receipt Header Message") },
                    placeholder = { Text("Welcome to our Sari-Sari Store!") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_receipt_header_input"),
                    singleLine = true
                )

                HeroInput(
                    value = editReceiptFooter,
                    onValueChange = { editReceiptFooter = it },
                    label = { Text("Custom Receipt Footer Notice") },
                    placeholder = { Text("Items can be returned/exchanged within 7 days.") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_receipt_footer_input"),
                    maxLines = 2
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Print 1D Barcode on Receipts", fontWeight = FontWeight.SemiBold)
                        Text("Prints Code 128 / Code 39 barcode for quick scanner returns & shift audits", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HeroSwitch(
                        checked = editPrintBarcode,
                        onCheckedChange = { editPrintBarcode = it },
                        modifier = Modifier.testTag("toggle_receipt_barcode_switch")
                    )
                }

                HeroButton(
                    onClick = {
                        viewModel.updateReceiptBranding(
                            tin = editStoreTin.trim(),
                            address = editStoreAddress.trim(),
                            header = editReceiptHeader.trim(),
                            footer = editReceiptFooter.trim(),
                            printBarcode = editPrintBarcode
                        )
                        Toast.makeText(context, "Receipt branding & TIN settings saved.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_receipt_branding_btn"),
                    text = "Save Receipt Branding & TIN",
                    icon = Icons.Default.Save
                )
            }
        }


    // --- BLUETOOTH PRINTER SELECTION DIALOG (XP-58 Plus) ---
    if (showBtDevicesDialog) {
        var refreshTrigger by remember { mutableStateOf(0) }
        var hasBtPermission by remember {
            mutableStateOf(
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.BLUETOOTH_CONNECT
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                } else true
            )
        }

        val btPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasBtPermission = isGranted
            if (isGranted) {
                refreshTrigger++
            }
        }

        val pairedPrinters = remember(hasBtPermission, refreshTrigger) {
            if (hasBtPermission) EscPosHelper.getPairedBluetoothPrinters(context) else emptyList()
        }

        AlertDialog(
            onDismissRequest = { showBtDevicesDialog = false },
            shape = HeroUITheme.radius.xl,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Bluetooth Printer", fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { refreshTrigger++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh paired list", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            text = {
                if (!hasBtPermission && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HeroCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Bluetooth Permission Required",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "Android 12+ requires the Nearby Devices permission to connect to Bluetooth receipt printers.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        HeroButton(
                            text = "Grant Bluetooth Permission",
                            onClick = { btPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT) },
                            icon = Icons.Default.Bluetooth,
                            variant = HeroButtonVariant.Solid,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else if (pairedPrinters.isEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "No paired Bluetooth devices found.",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "To connect your XP-58 Plus or thermal printer:\n1. Turn on the printer.\n2. Open Android Settings > Bluetooth.\n3. Pair the printer (standard PIN: 0000 or 1234).\n4. Return here and tap the Refresh button above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pairedPrinters, key = { it.macAddress }) { device ->
                            HeroCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setPrinterBluetoothConfig(device.macAddress, device.name)
                                        editPrinterMac = device.macAddress
                                        showBtDevicesDialog = false
                                        Toast.makeText(context, "Selected ${device.name}", Toast.LENGTH_SHORT).show()
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (printerMac == device.macAddress)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(device.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(device.macAddress, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (device.isLikelyPosPrinter) {
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                            Text("POS / XP-58", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                HeroTextButton(onClick = { showBtDevicesDialog = false }, text = "Close")
            }
        )
    }

    // --- USB OTG THERMAL PRINTER SELECTION DIALOG ---
    if (showUsbDevicesDialog) {
        var usbRefreshTrigger by remember { mutableStateOf(0) }
        val usbPrinters = remember(usbRefreshTrigger) { UsbPrinterHelper.getConnectedUsbPrinters(context) }
        AlertDialog(
            onDismissRequest = { showUsbDevicesDialog = false },
            shape = HeroUITheme.radius.xl,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Usb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select USB Thermal Printer", fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { usbRefreshTrigger++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan USB Peripherals", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            text = {
                if (usbPrinters.isEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "No USB thermal printers detected.",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Connect your XP-58 Plus using a USB cable and USB-OTG adapter, verify the printer is turned on, and tap Scan USB again.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(usbPrinters, key = { it.identifier }) { device ->
                            val rawDevice = remember(device.identifier) { UsbPrinterHelper.findDeviceByIdentifier(context, device.identifier) }
                            val hasPerm = rawDevice?.let { UsbPrinterHelper.hasPermission(context, it) } ?: false
                            HeroCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (rawDevice != null && !hasPerm) {
                                            UsbPrinterHelper.requestPermission(context, rawDevice) { granted ->
                                                if (granted) {
                                                    viewModel.setPrinterUsbConfig(device.identifier, device.displayName)
                                                    showUsbDevicesDialog = false
                                                    Toast.makeText(context, "USB Printer Connected: ${device.displayName}", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "USB permission denied", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } else {
                                            viewModel.setPrinterUsbConfig(device.identifier, device.displayName)
                                            showUsbDevicesDialog = false
                                            Toast.makeText(context, "Selected ${device.displayName}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (printerUsbIdentifier == device.identifier)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(device.displayName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("ID: ${device.identifier}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (!hasPerm) {
                                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                                            Text("Needs Permission", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                HeroTextButton(onClick = { showUsbDevicesDialog = false }, text = "Close")
            }
        )
    }
}
