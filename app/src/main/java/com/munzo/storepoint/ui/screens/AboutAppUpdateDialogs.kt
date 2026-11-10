package com.munzo.storepoint.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.munzo.storepoint.ui.StorePointViewModel
import com.munzo.storepoint.util.AppUpdateInfo
import com.munzo.storepoint.util.UpdateDownloadState
import com.munzo.storepoint.ui.components.*

/**
 * Dialogs for in-app OTA software updates via GitHub Releases.
 * Modularized from AboutScreen to adhere to the maintainability ceiling.
 */
@Composable
fun AboutAppUpdateDialogs(
    context: Context,
    viewModel: StorePointViewModel,
    currentVersion: String,
    updateInfo: AppUpdateInfo?,
    updateError: String?,
    downloadState: UpdateDownloadState,
    onDismiss: () -> Unit
) {
    if (updateError != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = { Text("Update Check Error", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(updateError, style = MaterialTheme.typography.bodyMedium)
                    val helpText = when {
                        updateError.contains("404") ->
                            "The release or repository was not found on GitHub. If the repository is private, ensure your access token is configured, or publish an initial release via GitHub Actions."
                        updateError.contains("401") || updateError.contains("403") ->
                            "GitHub authentication failed or rate limit exceeded. Verify your GitHub access token permissions."
                        updateError.contains("network", ignoreCase = true) || updateError.contains("connection", ignoreCase = true) ->
                            "Unable to communicate with GitHub. Verify your device's Wi-Fi or mobile data connection."
                        else ->
                            "Verify your internet connection and that GitHub is reachable."
                    }
                    Text(
                        helpText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                HeroButton(
                    onClick = onDismiss,
                    text = "Dismiss"
                )
            }
        )
    } else if (updateInfo != null) {
        if (!updateInfo.isNewer) {
            // App is up to date
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = { Text("StorePoint is Up to Date", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "You are currently running the latest version (v$currentVersion).",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (updateInfo.latestVersionTag.isNotEmpty()) {
                            Text(
                                "Latest GitHub tag: ${updateInfo.latestVersionTag}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                confirmButton = {
                    HeroButton(
                        onClick = onDismiss,
                        text = "Great!"
                    )
                }
            )
        } else {
            // Newer version available!
            when (downloadState) {
                is UpdateDownloadState.Idle -> {
                    AlertDialog(
                        onDismissRequest = onDismiss,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        },
                        title = {
                            Text("New Update Available", fontWeight = FontWeight.Bold)
                        },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                "Current: v$currentVersion",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                            Text(
                                                "Available: ${updateInfo.latestVersionTag}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (updateInfo.assetSize > 0) {
                                            val mb = updateInfo.assetSize / (1024.0 * 1024.0)
                                            Text(
                                                String.format("%.1f MB", mb),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                if (updateInfo.releaseNotes.isNotEmpty()) {
                                    Text(
                                        "Release Notes:",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(
                                        updateInfo.releaseNotes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            HeroButton(
                                onClick = {
                                    viewModel.startUpdateDownload(
                                        context = context,
                                        downloadUrl = updateInfo.downloadUrl,
                                        fileName = "StorePoint-${updateInfo.latestVersionTag}.apk"
                                    )
                                },
                                shape = CircleShape,
                                modifier = Modifier.testTag("download_update_button"),
                                icon = Icons.Default.Download,
                                text = "Download & Install"
                            )
                        },
                        dismissButton = {
                            HeroTextButton(
                                onClick = onDismiss,
                                text = "Later"
                            )
                        }
                    )
                }

                is UpdateDownloadState.Downloading -> {
                    val progress = downloadState.progress
                    val downloadedMb = downloadState.downloadedBytes / (1024.0 * 1024.0)
                    val totalMb = if (downloadState.totalBytes > 0) downloadState.totalBytes / (1024.0 * 1024.0) else 0.0

                    AlertDialog(
                        onDismissRequest = { /* Non dismissable while downloading */ },
                        icon = {
                            HeroSpinner(size = 36.dp, strokeWidth = 3.dp)
                        },
                        title = { Text("Downloading Update...", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (progress >= 0f) {
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            String.format("%.1f MB / %.1f MB", downloadedMb, totalMb),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            String.format("%.0f%%", progress * 100),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    Text(
                                        String.format("%.1f MB downloaded...", downloadedMb),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        },
                        confirmButton = {}
                    )
                }

                is UpdateDownloadState.ReadyToInstall -> {
                    AlertDialog(
                        onDismissRequest = onDismiss,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        },
                        title = { Text("Download Complete!", fontWeight = FontWeight.Bold) },
                        text = {
                            Text(
                                "The updated APK is ready to install. Click below to launch the Android package installer.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        confirmButton = {
                            HeroButton(
                                onClick = {
                                    viewModel.installDownloadedApk(context, downloadState.apkFile)
                                },
                                modifier = Modifier.testTag("install_apk_button"),
                                icon = Icons.Default.SystemUpdate,
                                text = "Install Now"
                            )
                        },
                        dismissButton = {
                            HeroTextButton(
                                onClick = onDismiss,
                                text = "Close"
                            )
                        }
                    )
                }

                is UpdateDownloadState.Error -> {
                    AlertDialog(
                        onDismissRequest = onDismiss,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(36.dp)
                            )
                        },
                        title = { Text("Download Failed", fontWeight = FontWeight.Bold) },
                        text = {
                            Text(
                                "Error downloading APK: ${downloadState.message}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        confirmButton = {
                            HeroButton(
                                onClick = onDismiss,
                                text = "OK"
                            )
                        }
                    )
                }
            }
        }
    }
}
