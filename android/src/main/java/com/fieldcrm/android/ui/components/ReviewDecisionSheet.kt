package com.fieldcrm.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme

@Composable
fun ReviewDecisionSheet(
    title: String,
    message: String,
    confirmLabel: String,
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (destructive) FieldIcons.AlertOutlined else FieldIcons.ShieldOutlined,
                contentDescription = if (destructive) "Review action needs attention" else "Secure approval action",
                tint = if (destructive) FieldTheme.colors.statusDanger else FieldTheme.colors.brandPrimary
            )
            Text(text = title, style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
            Text(text = message, style = FieldTheme.typography.body.copy(fontSize = 15.sp), color = FieldTheme.colors.gray400)
            Spacer(modifier = Modifier.height(4.dp))
            PrimaryButton(
                text = confirmLabel,
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth()
            )
            SecondaryButton(
                text = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
