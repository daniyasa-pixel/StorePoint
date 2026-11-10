package com.munzo.storepoint.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.munzo.storepoint.data.Product
import com.munzo.storepoint.ui.StorePointViewModel
import com.munzo.storepoint.ui.components.HeroCard
import com.munzo.storepoint.ui.components.HeroTextButton
import java.util.Locale

@Composable
fun CashierVariantPickerDialog(
    product: Product,
    curr: String,
    viewModel: StorePointViewModel,
    onDismiss: () -> Unit
) {
    val uomOptions = viewModel.getActiveUomsForProduct(product)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Select Size / Variant", fontWeight = FontWeight.Bold)
                Text(product.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Available Stock: ${product.stockCount} ${product.baseUom.ifBlank { "pcs" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                for (opt in uomOptions) {
                    HeroCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectCartUom(product.id, opt)
                                viewModel.addToCart(product, 1)
                                viewModel.playBeep()
                                onDismiss()
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
            HeroTextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
