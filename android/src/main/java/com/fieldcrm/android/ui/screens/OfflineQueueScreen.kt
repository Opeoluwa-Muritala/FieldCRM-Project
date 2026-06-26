package com.fieldcrm.android.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.FieldCard
import com.fieldcrm.android.ui.components.FieldTopAppBar
import com.fieldcrm.android.ui.components.PrimaryButton
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
fun ShieldPulseSpinner(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val opacity by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "opacity"
    )
    Icon(
        imageVector = Icons.Default.Lock, // Shield mark substitute
        contentDescription = "Pulsing Shield Logo",
        tint = FieldTheme.colors.purple600,
        modifier = modifier
            .size(16.dp)
            .graphicsLayer(alpha = opacity)
    )
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
                SyncItem("1", "Loan MMFB-052", SyncItemStatus.SYNCED),
                SyncItem("2", "Document upload", SyncItemStatus.PENDING),
                SyncItem("3", "Visit report", SyncItemStatus.FAILED, "Server unavailable — will retry automatically")
            )
        )
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                    .padding(24.dp), // 8-point grid layout padding
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sync status header card
                FieldCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isSyncing) "Syncing items..." else "All Synced & Cached",
                                style = FieldTheme.typography.title,
                                color = FieldTheme.colors.gray100
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isSyncing) "Syncing 2 items..." else "Local SQLite database matches Mainstreet server.",
                                style = FieldTheme.typography.body,
                                color = if (isSyncing) FieldTheme.colors.purple600 else FieldTheme.colors.gray400
                            )
                        }
                        if (isSyncing) {
                            ShieldPulseSpinner() // Custom shield-pulse spinner
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = item.status == SyncItemStatus.FAILED) {
                                        // Tap to retry action
                                        isSyncing = true
                                        syncItems = syncItems.map {
                                            if (it.id == item.id) it.copy(status = SyncItemStatus.PENDING) else it
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Status Indicator Icon matching the B6 wireframe details
                                Icon(
                                    imageVector = when (item.status) {
                                        SyncItemStatus.SYNCED -> Icons.Default.CheckCircle
                                        SyncItemStatus.PENDING -> Icons.Default.Refresh
                                        SyncItemStatus.FAILED -> Icons.Default.Close // Red X for failure
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
                                        text = "${item.name} — $statusSuffix",
                                        style = FieldTheme.typography.bodyStrong,
                                        color = FieldTheme.colors.gray100
                                    )
                                    if (item.status == SyncItemStatus.FAILED && item.errorMsg != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.errorMsg,
                                            style = FieldTheme.typography.body.copy(fontSize = 13.sp),
                                            color = FieldTheme.colors.gray500 // Slate helper text
                                        )
                                    }
                                }

                                if (item.status == SyncItemStatus.FAILED) {
                                    TextButton(
                                        onClick = {
                                            isSyncing = true
                                            syncItems = syncItems.map {
                                                if (it.id == item.id) it.copy(status = SyncItemStatus.PENDING) else it
                                            }
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Text(
                                            text = "Retry Now",
                                            style = FieldTheme.typography.bodyStrong,
                                            color = FieldTheme.colors.purple600
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                PrimaryButton(
                    text = if (isSyncing) "Syncing ledgers..." else "Force Synchronisation Check",
                    onClick = {
                        isSyncing = true
                    },
                    enabled = !isSyncing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
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
