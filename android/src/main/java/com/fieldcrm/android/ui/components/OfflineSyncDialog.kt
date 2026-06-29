package com.fieldcrm.android.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.fieldcrm.android.ui.theme.FieldTheme
import kotlinx.coroutines.launch

@Composable
fun OfflineSyncDialog(
    onDismiss: () -> Unit,
    onTriggerSync: (onComplete: (Boolean) -> Unit) -> Unit
) {
    var isSyncing by remember { mutableStateOf(false) }
    var syncResult by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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

    AlertDialog(
        onDismissRequest = { if (!isSyncing) onDismiss() },
        containerColor = FieldTheme.colors.gray900,
        modifier = Modifier.border(1.dp, FieldTheme.colors.gray800, RoundedCornerShape(24.dp)),
        title = {
            Text(
                text = "Offline Ledger Sync",
                style = FieldTheme.typography.title,
                color = FieldTheme.colors.gray100
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(FieldTheme.colors.purple900.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = "Sync",
                        tint = if (isSyncing) FieldTheme.colors.purple400 else FieldTheme.colors.gray400,
                        modifier = Modifier
                            .size(32.dp)
                            .graphicsLayer(rotationZ = if (isSyncing) rotation else 0f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                if (syncResult != null) {
                    Text(
                        text = syncResult ?: "",
                        style = FieldTheme.typography.body,
                        color = FieldTheme.colors.gray300
                    )
                } else if (isSyncing) {
                    Text(
                        text = "Synchronizing cached dossiers to Render host...",
                        style = FieldTheme.typography.body,
                        color = FieldTheme.colors.gray300
                    )
                } else {
                    Text(
                        text = "Dossiers registered offline are stored locally in the Sync Queue database table.",
                        style = FieldTheme.typography.body,
                        color = FieldTheme.colors.gray400
                    )
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = if (isSyncing) "Syncing..." else "Sync Now",
                onClick = {
                    isSyncing = true
                    syncResult = null
                    scope.launch {
                        onTriggerSync { success ->
                            isSyncing = false
                            syncResult = if (success) "Sync Complete!" else "Sync Failed. Will retry automatically."
                        }
                    }
                },
                enabled = !isSyncing
            )
        },
        dismissButton = {
            if (!isSyncing) {
                SecondaryButton(
                    text = "Close",
                    onClick = onDismiss
                )
            }
        }
    )
}
