package com.fieldcrm.android.ui.screens.application

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.FieldCard
import com.fieldcrm.android.ui.components.FieldTopAppBar
import com.fieldcrm.android.ui.components.PrimaryButton
import com.fieldcrm.android.ui.components.SecondaryButton
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.viewmodel.SyncItem
import com.fieldcrm.android.ui.viewmodel.SyncItemStatus
import com.fieldcrm.android.ui.viewmodel.SyncViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun OfflineQueueScreen(onBackClick: () -> Unit) {
    val viewModel: SyncViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sync_rotation"
    )

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "Offline Sync Queue",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = FieldIcons.ArrowBackOutlined,
                            contentDescription = "Back",
                            tint = FieldTheme.colors.gray400
                        )
                    }
                }
            )
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status header card
            FieldCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rotating sync icon badge
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(FieldTheme.colors.purple900.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.isSyncing) FieldIcons.SyncFilled else FieldIcons.SyncOutlined,
                            contentDescription = if (uiState.isSyncing) "Syncing" else "Sync",
                            tint = if (uiState.isSyncing) FieldTheme.colors.purple400 else FieldTheme.colors.gray500,
                            modifier = Modifier
                                .size(28.dp)
                                .graphicsLayer(rotationZ = if (uiState.isSyncing) rotation else 0f)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        val headlineText = when {
                            uiState.isSyncing -> "Synchronising…"
                            uiState.lastResult == true && uiState.items.isEmpty() -> "All records synced"
                            uiState.lastResult == false -> "Sync failed — will retry"
                            uiState.items.isEmpty() -> "Queue is empty"
                            else -> "${uiState.items.size} item${if (uiState.items.size == 1) "" else "s"} pending"
                        }
                        val subText = when {
                            uiState.isSyncing -> "Uploading queued records to server…"
                            uiState.lastResult == true && uiState.items.isEmpty() -> "Local data matches the server."
                            uiState.lastResult == false -> "Check your connection and try again."
                            uiState.items.isEmpty() -> "No offline changes are waiting to sync."
                            uiState.items.any { it.status == SyncItemStatus.FAILED } ->
                                "${uiState.items.count { it.status == SyncItemStatus.FAILED }} item(s) failed — tap Sync to retry."
                            else -> "These records will be uploaded when you sync."
                        }
                        Text(
                            text = headlineText,
                            style = FieldTheme.typography.title,
                            color = FieldTheme.colors.gray100
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subText,
                            style = FieldTheme.typography.body,
                            color = FieldTheme.colors.gray400
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = FieldTheme.colors.brandPrimary,
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.5.dp
                    )
                }
            } else if (uiState.items.isEmpty()) {
                // Empty queue state
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = FieldIcons.CheckCircleOutlined,
                            contentDescription = null,
                            tint = FieldTheme.colors.statusSuccess,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = if (uiState.lastResult == true) "Sync complete" else "Nothing to sync",
                            style = FieldTheme.typography.title,
                            color = FieldTheme.colors.gray300
                        )
                        Text(
                            text = "All offline records have been uploaded to the server.",
                            style = FieldTheme.typography.body,
                            color = FieldTheme.colors.gray500,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Text(
                    text = "PENDING ITEMS",
                    style = FieldTheme.typography.label,
                    color = FieldTheme.colors.gray500
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        SyncQueueCard(
                            item = item,
                            isSyncing = uiState.isSyncing,
                            onRetry = { viewModel.syncNow() }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }

            PrimaryButton(
                text = when {
                    uiState.isSyncing -> "Synchronising…"
                    uiState.items.isEmpty() && uiState.lastResult == true -> "Sync Again"
                    else -> "Sync Now"
                },
                onClick = { viewModel.syncNow() },
                enabled = !uiState.isSyncing,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SyncQueueCard(
    item: SyncItem,
    isSyncing: Boolean,
    onRetry: () -> Unit
) {
    FieldCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (item.status) {
                    SyncItemStatus.PENDING -> FieldIcons.ClockOutlined
                    SyncItemStatus.FAILED  -> FieldIcons.AlertOutlined
                },
                contentDescription = item.status.name,
                tint = when (item.status) {
                    SyncItemStatus.PENDING -> FieldTheme.colors.statusWarning
                    SyncItemStatus.FAILED  -> FieldTheme.colors.statusDanger
                },
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.label,
                    style = FieldTheme.typography.bodyStrong,
                    color = FieldTheme.colors.gray100
                )
                Text(
                    text = item.status.name.lowercase(),
                    style = FieldTheme.typography.mono.copy(fontSize = 11.sp),
                    color = when (item.status) {
                        SyncItemStatus.PENDING -> FieldTheme.colors.statusWarning
                        SyncItemStatus.FAILED  -> FieldTheme.colors.statusDanger
                    }
                )
                if (item.errorMsg != null) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.errorMsg,
                        style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                        color = FieldTheme.colors.gray500
                    )
                }
            }
        }

        if (item.status == SyncItemStatus.FAILED) {
            Spacer(modifier = Modifier.height(10.dp))
            SecondaryButton(
                text = "Retry",
                leadingIcon = {
                    Icon(
                        imageVector = FieldIcons.SyncOutlined,
                        contentDescription = null,
                        tint = FieldTheme.colors.purple600,
                        modifier = Modifier.size(16.dp)
                    )
                },
                onClick = onRetry,
                enabled = !isSyncing,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(name = "Compact", widthDp = 411, heightDp = 850)
@Composable
fun PreviewOfflineQueueCompact() {
    FieldCRMTheme {
        // Preview with static data — SyncViewModel is not available in preview
        OfflineQueuePreviewContent()
    }
}

@Composable
private fun OfflineQueuePreviewContent() {
    val items = listOf(
        SyncItem("1", "Loan application - A1B2C3D4", "CREATE_APPLICATION", SyncItemStatus.PENDING, 0),
        SyncItem("2", "Loan application — E5F6G7H8", "CREATE_APPLICATION", SyncItemStatus.PENDING, 1),
        SyncItem("3", "Repayment entry — I9J0K1L2", "RECORD_REPAYMENT", SyncItemStatus.FAILED, 3, "Failed after 3 attempts — will retry automatically")
    )
    Scaffold(
        topBar = { FieldTopAppBar(title = "Offline Sync Queue", navigationIcon = {
            IconButton(onClick = {}) {
                Icon(imageVector = FieldIcons.ArrowBackOutlined, contentDescription = "Back", tint = FieldTheme.colors.gray400)
            }
        }) },
        containerColor = FieldTheme.colors.gray950
    ) { pv ->
        Column(modifier = Modifier.fillMaxSize().padding(pv).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FieldCard {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).background(FieldTheme.colors.purple900.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(FieldIcons.SyncOutlined, contentDescription = null, tint = FieldTheme.colors.gray500, modifier = Modifier.size(28.dp))
                    }
                    Column {
                        Text("3 items pending", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                        Text("1 item failed — tap Sync to retry.", style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)
                    }
                }
            }
            Text("PENDING ITEMS", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items, key = { it.id }) { item -> SyncQueueCard(item = item, isSyncing = false, onRetry = {}) }
            }
            PrimaryButton(text = "Sync Now", onClick = {}, modifier = Modifier.fillMaxWidth())
        }
    }
}
