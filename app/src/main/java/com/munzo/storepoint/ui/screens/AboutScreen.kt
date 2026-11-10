package com.munzo.storepoint.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.munzo.storepoint.ui.theme.ExpressiveButtonShape
import com.munzo.storepoint.ui.theme.ExpressiveSectionHeader
import com.munzo.storepoint.ui.theme.glassPanel
import com.munzo.storepoint.ui.theme.tactileBounce
import com.munzo.storepoint.util.APP_VERSION
import com.munzo.storepoint.ui.components.*

import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.platform.LocalContext
import com.munzo.storepoint.ui.StorePointViewModel
import com.munzo.storepoint.util.AppUpdateInfo
import com.munzo.storepoint.util.UpdateDownloadState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    viewModel: StorePointViewModel? = null,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About StorePoint", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("about_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        AboutScreenContent(
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

@Composable
fun AboutScreenContent(
    viewModel: StorePointViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var privacyExpanded by remember { mutableStateOf(false) }
    var termsExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Updater states
    val isCheckingUpdate = viewModel?.updateCheckInProgress?.collectAsState()?.value ?: false
    val updateInfo = viewModel?.updateInfo?.collectAsState()?.value
    val updateError = viewModel?.updateErrorMessage?.collectAsState()?.value
    val downloadState = viewModel?.updateDownloadState?.collectAsState()?.value ?: UpdateDownloadState.Idle

    var showUpdateDialog by remember { mutableStateOf(false) }
    var currentVersion = APP_VERSION

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- HEADER BRAND LOGO CARD ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(84.dp)
                        .tactileBounce(),
                    shape = ExpressiveButtonShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Store",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Point",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
                Text(
                    text = "StorePoint POS",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    // Removed the "Turbo Edition (Max Uncapped RAM)" suffix. The manifest
                    // sets android:largeHeap="true", which only *requests* a larger heap
                    // from Android - the OS still caps the app. Describing it as
                    // "Max Uncapped RAM" overstated what the build actually does.
                    text = "Version $currentVersion",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // In-App Updater Button
                if (viewModel != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    HeroFilledTonalButton(
                        onClick = {
                            viewModel.checkForAppUpdates(currentVersion) { info ->
                                showUpdateDialog = true
                            }
                        },
                        enabled = !isCheckingUpdate,
                        shape = CircleShape,
                        modifier = Modifier.testTag("check_for_updates_button")
                    ) {
                        if (isCheckingUpdate) {
                            HeroSpinner(size = 16.dp, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Checking GitHub Releases...")
                        } else {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Check for Updates")
                        }
                    }
                }
            }

        HeroDivider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        // --- DEVELOPER GITHUB PROFILE CARD ---
        ExpressiveSectionHeader(
            title = "GitHub Profile",
            subtitle = "Open Source Creator & Maintainer",
            icon = Icons.Default.Code,
            modifier = Modifier.align(Alignment.Start)
        )

        HeroCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("developer_profile_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // GitHub Avatar Box
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "GitHub Icon",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "uznom",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "GitHub: @uznom",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                HeroDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                // Bio info
                Text(
                    text = "Builds reliable, offline-first open-source software for small business point-of-sale terminals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                // Quick links to GitHub
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroFilledTonalButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/uznom"))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        shape = CircleShape,
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        text = "Profile"
                    )

                    HeroOutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/uznom/StorePoint"))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        shape = CircleShape,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.FolderZip,
                        text = "Repository"
                    )
                }
            }
        }

        // --- INTERACTIVE / EXPANDABLE PRIVACY POLICY CARD ---
        HeroCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { privacyExpanded = !privacyExpanded }
                .testTag("privacy_policy_expandable_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PrivacyTip,
                            contentDescription = "Privacy Policy Icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Google Play User-Data Compliance",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(
                        onClick = { privacyExpanded = !privacyExpanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (privacyExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (privacyExpanded) "Collapse" else "Expand"
                        )
                    }
                }

                Text(
                    text = "StorePoint keeps all of your data on this device. Tap to see how your information is handled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AnimatedVisibility(
                    visible = privacyExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        HeroDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

                        // Policy 1: Offline Processing
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "100% Local Offline Processing",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                "Your PINs, product catalog, sales records, and staff accounts never leave this device. Everything is stored in a local database protected by Android.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }

                        // Policy 2: Scoped Media Storage
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderZip,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Scoped Secure Media Access",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                "Backups and PDF reports are saved only to this app's own storage folders, and are shared only when you choose to share them.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }

                        // Policy 3: Zero Telemetry
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Zero Data Telemetry & Analytics Tracker",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                "StorePoint does not collect crash reports, usage statistics, advertising identifiers, or any other telemetry. The app has no analytics and no tracking of any kind.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "No user data is collected, shared, or sold.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // --- INTERACTIVE / EXPANDABLE TERMS AND CONDITIONS CARD ---
        HeroCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { termsExpanded = !termsExpanded }
                .testTag("terms_conditions_expandable_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = "Terms and Conditions Icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Terms & Conditions of System Use",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(
                        onClick = { termsExpanded = !termsExpanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (termsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (termsExpanded) "Collapse" else "Expand"
                        )
                    }
                }

                Text(
                    text = "By using the system, you acknowledge and agree to operational, data custody, and liability policies. Tap to read.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AnimatedVisibility(
                    visible = termsExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        HeroDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

                        Text(
                            text = "By using the system, you acknowledge and agree that:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Term 1: Local Sovereignty
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "1. Local Data Sovereignty & Backup Custody",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "StorePoint runs 100% locally on your hardware. You maintain exclusive custody and control of all sales transactions, customer records, and inventory data. You are solely responsible for exporting periodic database backups and safeguarding terminal PINs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }

                        // Term 2: As-Is Provision
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "2. 'As-Is' Software Disclaimer",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "StorePoint POS is provided 'as is' and 'as available' without warranties of any kind, whether express or implied, including merchantability, fitness for a particular business purpose, or uninterrupted error-free operation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }

                        // Term 3: Limitation of Liability
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "3. Operational Limitation of Liability",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "In no event shall the developer (@uznom) or open-source contributors be liable for any loss of revenue, financial discrepancies, inventory imbalances, hardware failures, or business interruptions arising from terminal use or downtime.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }

                        // Term 4: Regulatory & Tax Compliance
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "4. Tax & Commercial Compliance",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Terminal operators are solely responsible for ensuring that all issued receipts, applied tax percentages (e.g., VAT or sales tax exemptions), and business reports conform to applicable regional commercial and tax authority requirements.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Usage constitutes full acceptance of these terms.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Footer branding
        Text(
            text = "StorePoint POS is free and open source, licensed under the Apache License 2.0.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // --- SOFTWARE UPDATE MODAL DIALOGS ---
    if (showUpdateDialog && viewModel != null) {
        AboutAppUpdateDialogs(
            context = context,
            viewModel = viewModel,
            currentVersion = currentVersion,
            updateInfo = updateInfo,
            updateError = updateError,
            downloadState = downloadState,
            onDismiss = {
                showUpdateDialog = false
                viewModel.clearUpdateState()
            }
        )
    }
}
}

