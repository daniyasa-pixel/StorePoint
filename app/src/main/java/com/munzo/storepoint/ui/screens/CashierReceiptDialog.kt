package com.munzo.storepoint.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.munzo.storepoint.data.StoreConfig
import com.munzo.storepoint.data.Transaction
import com.munzo.storepoint.data.TransactionItem
import com.munzo.storepoint.ui.StorePointViewModel
import com.munzo.storepoint.ui.components.*
import com.munzo.storepoint.ui.theme.HeroUITheme
import com.munzo.storepoint.ui.theme.celebratePulse
import com.munzo.storepoint.util.EscPosHelper
import com.munzo.storepoint.util.PdfExportUtil
import com.munzo.storepoint.util.PendingDigitalService
import java.util.Locale

@Composable
fun CashierReceiptDialog(
    transaction: Transaction,
    items: List<TransactionItem>,
    pendingDigitalServices: List<PendingDigitalService>,
    curr: String,
    viewModel: StorePointViewModel,
    storeConfig: StoreConfig?,
    onDismiss: () -> Unit,
    onOpenDigitalServices: () -> Unit
) {
    val context = LocalContext.current
    val celebrateTrigger by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = {
            if (pendingDigitalServices.isNotEmpty()) {
                onOpenDigitalServices()
            } else {
                onDismiss()
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val currentPrinterType by viewModel.printerType.collectAsState()
                if (currentPrinterType != EscPosHelper.PrinterType.SYSTEM_SPOOLER.name) {
                    HeroFilledTonalButton(
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

                HeroButton(
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
            HeroTextButton(
                onClick = {
                    if (pendingDigitalServices.isNotEmpty()) {
                        onOpenDigitalServices()
                    } else {
                        onDismiss()
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
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = HeroUITheme.colors.success,
                    modifier = Modifier
                        .size(56.dp)
                        .celebratePulse(celebrateTrigger)
                )
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
                            HeroButton(
                                onClick = onOpenDigitalServices,
                                modifier = Modifier.fillMaxWidth()
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
