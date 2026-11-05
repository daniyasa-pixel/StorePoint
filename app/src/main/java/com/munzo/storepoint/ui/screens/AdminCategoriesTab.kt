package com.munzo.storepoint.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.munzo.storepoint.data.Category
import com.munzo.storepoint.ui.theme.glassPanel
import com.munzo.storepoint.ui.theme.HeroUITheme
import com.munzo.storepoint.ui.components.*

val CATEGORY_COLOR_PRESETS = listOf(
    "#10B981" to "Emerald",
    "#2563EB" to "Royal Blue",
    "#F59E0B" to "Amber",
    "#8B5CF6" to "Purple",
    "#EF4444" to "Crimson",
    "#14B8A6" to "Teal",
    "#EC4899" to "Pink",
    "#64748B" to "Slate"
)

val CATEGORY_ICON_PRESETS = listOf(
    "folder" to "Folder",
    "restaurant" to "Food",
    "local_drink" to "Drinks",
    "devices" to "Gadgets",
    "checkroom" to "Apparel",
    "shopping_bag" to "Goods",
    "medication" to "Pharma",
    "cleaning_services" to "Cleaning"
)

fun getCategoryIconVector(iconBadge: String): ImageVector {
    return when (iconBadge.lowercase()) {
        "restaurant", "food" -> Icons.Default.Restaurant
        "local_drink", "drink", "beverage" -> Icons.Default.LocalDrink
        "devices", "electronic", "gadget" -> Icons.Default.Devices
        "checkroom", "apparel", "clothing" -> Icons.Default.Checkroom
        "shopping_bag", "bag", "goods" -> Icons.Default.ShoppingBag
        "medication", "medicine", "pharma" -> Icons.Default.Medication
        "cleaning_services", "cleaning" -> Icons.Default.CleaningServices
        "inventory", "stock" -> Icons.Default.Inventory
        else -> Icons.Default.FolderOpen
    }
}

fun parseCategoryColor(hex: String, fallback: Color = Color(0xFF3B82F6)): Color {
    if (hex.isBlank()) return fallback
    return try {
        Color(android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex"))
    } catch (_: Exception) {
        fallback
    }
}

@Composable
fun CategoriesTab(
    categories: List<Category>,
    onDelete: (Int) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (Category) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Product Categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            HeroButton(
                onClick = onAddClick,
                modifier = Modifier.testTag("add_category_button"),
                text = "New Category",
                icon = Icons.Default.Add,
                color = HeroColor.Primary,
                variant = HeroButtonVariant.Solid,
                size = HeroSize.Md
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (categories.isEmpty()) {
                item {
                    Text(
                        "No classification folders exist.",
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(categories) { item ->
                    val accentColor = parseCategoryColor(item.colorAccent, MaterialTheme.colorScheme.primary)
                    val iconVector = getCategoryIconVector(item.iconBadge)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassPanel(cornerRadius = 16.dp)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = accentColor.copy(alpha = 0.18f),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = item.name,
                                        tint = accentColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    item.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (item.colorAccent.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(accentColor, CircleShape)
                                        )
                                        Text(
                                            item.colorAccent,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    if (item.iconBadge.isNotBlank()) {
                                        Text(
                                            "• ${item.iconBadge}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = { onEditClick(item) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("edit_category_${item.id}")
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit category",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            FilledTonalIconButton(
                                onClick = { onDelete(item.id) },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("delete_category_${item.id}")
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete category",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
