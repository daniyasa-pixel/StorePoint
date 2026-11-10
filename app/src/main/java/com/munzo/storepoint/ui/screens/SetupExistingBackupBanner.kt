package com.munzo.storepoint.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.munzo.storepoint.ui.StorePointViewModel
import com.munzo.storepoint.ui.components.*
import com.munzo.storepoint.ui.theme.HeroUITheme
import com.munzo.storepoint.util.DatabaseBackupManager
import java.io.File

/**
 * Banner shown on SetupScreen when an existing persistent backup file is detected.
 * Extracted from SetupScreen to enforce <800 lines maintainability ceiling.
 */
@Composable
fun SetupExistingBackupBanner(
    context: Context,
    viewModel: StorePointViewModel,
    existingBackupMeta: DatabaseBackupManager.BackupMetadata?,
    existingBackupFile: File?,
    onDismiss: () -> Unit
) {
    var isRestoringBackup by remember { mutableStateOf(false) }

    HeroCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        backgroundColor = HeroUITheme.colors.accent.copy(alpha = 0.08f),
        borderColor = HeroUITheme.colors.accent.copy(alpha = 0.4f),
        shape = HeroUITheme.radius.xl
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(HeroUITheme.radius.md)
                        .background(HeroUITheme.colors.accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SettingsBackupRestore,
                        contentDescription = null,
                        tint = HeroUITheme.colors.accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Previous Installation Backup Found!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HeroUITheme.colors.foreground
                    )
                    Text(
                        text = "Store: ${existingBackupMeta?.storeName ?: "StorePoint"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = HeroUITheme.colors.muted
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = HeroUITheme.radius.md,
                    color = HeroUITheme.colors.surfaceSecondary,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Products", style = MaterialTheme.typography.labelSmall, color = HeroUITheme.colors.muted)
                        Text("${existingBackupMeta?.productsCount ?: 0}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(
                    shape = HeroUITheme.radius.md,
                    color = HeroUITheme.colors.surfaceSecondary,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Transactions", style = MaterialTheme.typography.labelSmall, color = HeroUITheme.colors.muted)
                        Text("${existingBackupMeta?.transactionsCount ?: 0}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(
                    shape = HeroUITheme.radius.md,
                    color = HeroUITheme.colors.surfaceSecondary,
                    modifier = Modifier.weight(1.4f)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Backup Date", style = MaterialTheme.typography.labelSmall, color = HeroUITheme.colors.muted)
                        Text(existingBackupMeta?.dateTime?.take(10) ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            if (isRestoringBackup) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeroSpinner(size = 20.dp, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Restoring database from internal backup...", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeroButton(
                        onClick = onDismiss,
                        color = HeroColor.Default,
                        variant = HeroButtonVariant.Light,
                        size = HeroSize.Sm,
                        text = "Start Fresh Setup"
                    )
                    HeroButton(
                        onClick = {
                            val file = existingBackupFile
                            if (file != null) {
                                isRestoringBackup = true
                                viewModel.restoreFromPersistentBackup(file) { summary ->
                                    isRestoringBackup = false
                                    if (summary.success) {
                                        Toast.makeText(context, "Store restored successfully!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Restore failed: ${summary.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        color = HeroColor.Primary,
                        variant = HeroButtonVariant.Solid,
                        size = HeroSize.Md,
                        icon = Icons.Default.CloudDownload,
                        text = "Restore Store Now"
                    )
                }
            }
        }
    }
}
