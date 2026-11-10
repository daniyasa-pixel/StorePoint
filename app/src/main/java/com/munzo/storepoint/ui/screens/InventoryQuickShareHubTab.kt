package com.munzo.storepoint.ui.screens

import android.content.Intent
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
import com.munzo.storepoint.ui.components.*
import com.munzo.storepoint.ui.layout.ScanSearchField
import com.munzo.storepoint.ui.theme.*
import java.util.Locale

/**
 * Tab 3: Quick Share Hub for StorePoint App-Only Transfer
 */
@Composable
fun InventoryQuickShareHubTab(
    viewModel: StorePointViewModel,
    totalProducts: Int,
    totalCategories: Int
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeroCard(
            modifier = Modifier
                .fillMaxWidth()
                .expressiveCard(cornerRadius = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Text(
                    text = "App-Only Quick Share",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Export and beam store inventory data directly between Android devices running StorePoint. The file is packaged as an app-only .spinventory container for secure terminal-to-terminal sync.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                HeroDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Products", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text("$totalProducts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Categories", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text("$totalCategories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Format", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(".spinventory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                HeroButton(
                    onClick = { viewModel.shareInventoryViaQuickShare(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hub_quick_share_button"),
                    text = "Share Inventory via Quick Share",
                    icon = Icons.Default.Share,
                    color = HeroColor.Primary,
                    variant = HeroButtonVariant.Solid,
                    size = HeroSize.Lg
                )

                Spacer(modifier = Modifier.height(8.dp))

                HeroButton(
                    onClick = { viewModel.shareStockSheetCsv(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hub_share_stock_sheet_csv_button"),
                    text = "Export Stock Sheet (CSV / Excel)",
                    icon = Icons.Default.Description,
                    color = HeroColor.Default,
                    variant = HeroButtonVariant.Bordered,
                    size = HeroSize.Lg
                )
            }
        }

        // Informational Security Notice
        HeroCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Column {
                    Text("Admin Import Control", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    Text(
                        "On the receiving terminal, this data is imported under Admin Center → Security → Import Inventory Data, certified by Admin 4-digit PIN.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
