package com.fieldcrm.android.ui.screens.application

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.FieldCard
import com.fieldcrm.android.ui.components.FieldTopAppBar
import com.fieldcrm.android.ui.components.PrimaryButton
import com.fieldcrm.android.ui.components.SecondaryButton
import androidx.compose.ui.tooling.preview.Preview
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import kotlinx.coroutines.delay

enum class SyncItemStatus {
    SYNCED,
    PENDING,
    FAILED
}

data class SyncItem(
    val id: String,
    val name: String,
    val status: SyncItemStatus,
    val errorMsg: String? = null
)

@Composable
fun SyncingBadge(
    isSyncing: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    Box(
        modifier = modifier
            .size(64.dp)
            .background(FieldTheme.colors.purple900.copy(alpha = 0.1f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Sync,
            contentDescription = "Syncing",
            tint = if (isSyncing) FieldTheme.colors.purple600 else FieldTheme.colors.gray500,
            modifier = Modifier
                .size(32.dp)
                .graphicsLayer(rotationZ = if (isSyncing) rotation else 0f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineQueueScreen(
    onBackClick: () -> Unit
) {
    var isSyncing by remember { mutableStateOf(false) }
    var syncItems by remember {
        mutableStateOf(
            listOf(
                SyncItem("1", "Loan MMFB-052 Intake Data", SyncItemStatus.SYNCED),
                SyncItem("2", "NIN Card Image Attachment", SyncItemStatus.PENDING),
                SyncItem("3", "GPS Visitation Log Report", SyncItemStatus.FAILED, "Server handshake failure — will auto retry")
            )
        )
    }

    // Interactive Sync Simulation logic
    LaunchedEffect(isSyncing) {
        if (isSyncing) {
            // First set all non-synced items to PENDING
            syncItems = syncItems.map {
                if (it.status != SyncItemStatus.SYNCED) it.copy(status = SyncItemStatus.PENDING) else it
            }
            delay(2000) // handshaking delay
            syncItems = syncItems.map { it.copy(status = SyncItemStatus.SYNCED, errorMsg = null) }
            isSyncing = false
        }
    }

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "Sync Status",
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = FieldTheme.colors.gray400
                        )
                    }
                }
            )
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sync status header card matching Stitch mock gap and border style
                FieldCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SyncingBadge(isSyncing = isSyncing)
                        Column {
                            Text(
                                text = if (isSyncing) "Syncing Ledger Queues..." else "All Cache Harmonised",
                                style = FieldTheme.typography.title,
                                color = FieldTheme.colors.gray100
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isSyncing) "Connecting to Lagos MMFB Server node..." else "Local database matches primary bank server.",
                                style = FieldTheme.typography.body,
                                color = FieldTheme.colors.gray400
                            )
                        }
                    }
                }

                // Sync Queue Header
                Text(
                    text = "SYNC QUEUE",
                    style = FieldTheme.typography.label,
                    color = FieldTheme.colors.gray500
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(syncItems) { item ->
                        FieldCard {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (item.status) {
                                            SyncItemStatus.SYNCED -> Icons.Outlined.CheckCircle
                                            SyncItemStatus.PENDING -> Icons.Outlined.AccessTime
                                            SyncItemStatus.FAILED -> Icons.Outlined.Error
                                        },
                                        contentDescription = item.status.name,
                                        tint = when (item.status) {
                                            SyncItemStatus.SYNCED -> FieldTheme.colors.statusSuccess
                                            SyncItemStatus.PENDING -> FieldTheme.colors.statusWarning
                                            SyncItemStatus.FAILED -> FieldTheme.colors.statusDanger
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        val statusSuffix = when (item.status) {
                                            SyncItemStatus.SYNCED -> "synced"
                                            SyncItemStatus.PENDING -> "pending"
                                            SyncItemStatus.FAILED -> "failed"
                                        }
                                        Text(
                                            text = item.name,
                                            style = FieldTheme.typography.bodyStrong,
                                            color = FieldTheme.colors.gray100
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = statusSuffix.uppercase(),
                                            style = FieldTheme.typography.mono.copy(fontSize = 11.sp),
                                            color = when (item.status) {
                                                SyncItemStatus.SYNCED -> FieldTheme.colors.statusSuccess
                                                SyncItemStatus.PENDING -> FieldTheme.colors.statusWarning
                                                SyncItemStatus.FAILED -> FieldTheme.colors.statusDanger
                                            }
                                        )
                                        if (item.status == SyncItemStatus.FAILED && item.errorMsg != null) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = item.errorMsg,
                                                style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                                color = FieldTheme.colors.gray500
                                            )
                                        }
                                    }
                                }

                                if (item.status == SyncItemStatus.FAILED) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    SecondaryButton(
                                        text = "Retry Now",
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.Refresh,
                                                contentDescription = "Retry",
                                                tint = FieldTheme.colors.purple600,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            isSyncing = true
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                PrimaryButton(
                    text = if (isSyncing) "Synchronising Ledgers..." else "Force Synchronisation Check",
                    onClick = {
                        isSyncing = true
                    },
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    LaunchedEffect(isSyncing) {
        if (isSyncing) {
            delay(2000)
            syncItems = syncItems.map {
                if (it.status == SyncItemStatus.PENDING) it.copy(status = SyncItemStatus.SYNCED) else it
            }
            isSyncing = false
        }
    }
}

@Preview(name = "Compact Phone Offline Queue", widthDp = 411, heightDp = 850)
@Composable
fun PreviewOfflineQueueCompact() {
    FieldCRMTheme {
        OfflineQueueScreen(onBackClick = {})
    }
}

@Preview(name = "Tablet Offline Queue Layout", widthDp = 1280, heightDp = 800)
@Composable
fun PreviewOfflineQueueTablet() {
    FieldCRMTheme {
        OfflineQueueScreen(onBackClick = {})
    }
}

