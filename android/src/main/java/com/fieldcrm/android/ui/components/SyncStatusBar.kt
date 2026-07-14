package com.fieldcrm.android.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme

@Composable
fun SyncStatusBar(
    message: String,
    tone: SyncStatusTone,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val color = when (tone) {
        SyncStatusTone.Synced -> FieldTheme.colors.statusSuccess
        SyncStatusTone.Pending -> FieldTheme.colors.statusWarning
        SyncStatusTone.Failed -> FieldTheme.colors.statusDanger
    }
    val icon = when (tone) {
        SyncStatusTone.Synced -> FieldIcons.CheckOutlined
        SyncStatusTone.Pending -> FieldIcons.SyncOutlined
        SyncStatusTone.Failed -> FieldIcons.AlertOutlined
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .clickable(enabled = onActionClick != null) { onActionClick?.invoke() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = message, tint = color)
            Spacer(Modifier.width(8.dp))
            AnimatedContent(
                targetState = message,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(90)) },
                label = "syncStatus"
            ) { label ->
                Text(label, style = FieldTheme.typography.label, color = FieldTheme.colors.gray100)
            }
        }
        if (actionLabel != null) {
            Spacer(Modifier.width(12.dp))
            Text(actionLabel, style = FieldTheme.typography.label, color = color)
        }
    }
}

@Preview(name = "Sync pending", widthDp = 411)
@Composable
private fun SyncStatusBarPreview() {
    FieldCRMTheme {
        SyncStatusBar(message = "3 changes saved on this device", tone = SyncStatusTone.Pending)
    }
}

@Preview(name = "Sync failed — dark", widthDp = 411, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SyncStatusBarDarkPreview() {
    FieldCRMTheme(darkTheme = true) {
        SyncStatusBar(
            message = "Server unavailable — retrying",
            tone = SyncStatusTone.Failed,
            actionLabel = "Retry now",
            onActionClick = {}
        )
    }
}

@Preview(name = "Sync expanded", widthDp = 1280)
@Composable
private fun SyncStatusBarExpandedPreview() {
    FieldCRMTheme {
        SyncStatusBar(message = "All changes synced", tone = SyncStatusTone.Synced)
    }
}
