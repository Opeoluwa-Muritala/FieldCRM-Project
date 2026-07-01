package com.fieldcrm.android.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.viewmodel.SyncViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun OfflineSyncDialog(
    onDismiss: () -> Unit,
    onTriggerSync: (onComplete: (Boolean) -> Unit) -> Unit
) {
    val syncViewModel: SyncViewModel = koinViewModel()
    val syncUiState by syncViewModel.uiState.collectAsState()
    val pendingCount = syncUiState.items.size

    var isSyncing by remember { mutableStateOf(false) }
    var syncResult by remember { mutableStateOf<Boolean?>(null) }

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

    AlertDialog(
        onDismissRequest = { if (!isSyncing) onDismiss() },
        containerColor = FieldTheme.colors.gray900,
        modifier = Modifier.border(1.dp, FieldTheme.colors.gray800, RoundedCornerShape(24.dp)),
        title = {
            Text(
                text = "Offline Sync",
                style = FieldTheme.typography.title,
                color = FieldTheme.colors.gray100
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(FieldTheme.colors.purple900.copy(alpha = 0.10f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSyncing) FieldIcons.SyncFilled else FieldIcons.SyncOutlined,
                        contentDescription = if (isSyncing) "Syncing" else "Sync",
                        tint = if (isSyncing) FieldTheme.colors.purple400 else FieldTheme.colors.gray400,
                        modifier = Modifier
                            .size(32.dp)
                            .graphicsLayer(rotationZ = if (isSyncing) rotation else 0f)
                    )
                }

                Text(
                    text = when {
                        isSyncing -> "Uploading queued records to the server…"
                        syncResult == true -> "Sync complete. Local data is up to date."
                        syncResult == false -> "Sync failed. Check your connection and try again."
                        pendingCount > 0 -> "$pendingCount offline record${if (pendingCount == 1) "" else "s"} waiting to be uploaded."
                        else -> "No pending records. Tap Sync to pull the latest data from the server."
                    },
                    style = FieldTheme.typography.body,
                    color = when {
                        syncResult == true -> FieldTheme.colors.statusSuccess
                        syncResult == false -> FieldTheme.colors.statusDanger
                        else -> FieldTheme.colors.gray400
                    }
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = if (isSyncing) "Syncing…" else "Sync Now",
                onClick = {
                    isSyncing = true
                    syncResult = null
                    onTriggerSync { success ->
                        isSyncing = false
                        syncResult = success
                        syncViewModel.load()
                    }
                },
                enabled = !isSyncing
            )
        },
        dismissButton = {
            if (!isSyncing) {
                SecondaryButton(text = "Close", onClick = onDismiss)
            }
        }
    )
}
