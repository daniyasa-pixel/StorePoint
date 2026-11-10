package com.munzo.storepoint.ui.screens

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.munzo.storepoint.data.Product
import com.munzo.storepoint.data.StorePointRepository
import com.munzo.storepoint.data.UomOption
import com.munzo.storepoint.ui.StorePointViewModel
import com.munzo.storepoint.ui.components.*
import java.util.Locale

@Composable
fun CashierItemDiscountDialog(
    product: Product,
    activeUom: UomOption,
    currentDiscount: Double,
    curr: String,
    viewModel: StorePointViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val basePrice = activeUom.price

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
        onDismissRequest = onDismiss,
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
                    text = product.name,
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
                    HeroFilterChip(
                        selected = discountMode == 0,
                        onClick = { discountMode = 0 },
                        label = { Text("Percentage (%)") },
                        modifier = Modifier.weight(1f)
                    )
                    HeroFilterChip(
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
                        for (pct in listOf("5", "10", "15", "20", "50")) {
                            HeroChip(
                                text = "$pct%",
                                variant = HeroChipVariant.Flat,
                                onClick = { percentInput = pct }
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
                        for (amt in listOf(5.0, 10.0, 20.0, 50.0)) {
                            if (amt < basePrice) {
                                HeroChip(
                                    text = "$curr$amt",
                                    variant = HeroChipVariant.Flat,
                                    onClick = { amountInput = String.format(Locale.US, "%.2f", amt) }
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
            HeroButton(
                onClick = {
                    viewModel.setItemDiscount(product.id, calculatedDiscount)
                    onDismiss()
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
                    HeroTextButton(
                        onClick = {
                            viewModel.setItemDiscount(product.id, 0.0)
                            onDismiss()
                            Toast.makeText(context, "Discount removed", Toast.LENGTH_SHORT).show()
                        },
                        color = HeroColor.Danger,
                        modifier = Modifier.testTag("remove_discount_btn")
                    ) {
                        Text("Remove")
                    }
                }
                HeroTextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
