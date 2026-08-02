package com.fieldcrm.android.ui.screens.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.theme.FieldTheme
import java.util.Locale

@Composable
fun DetailField(label: String, value: String) {
    DetailItem(label = label, value = value)
}

@Composable
fun DetailItem(
    label: String,
    value: String,
    isMono: Boolean = false
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = FieldTheme.typography.label.copy(fontSize = 10.sp),
            color = FieldTheme.colors.gray500
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = if (isMono) FieldTheme.typography.mono else FieldTheme.typography.bodyStrong,
            color = FieldTheme.colors.gray300
        )
    }
}

/**
 * Reusable label/value row component matching Part G3 requirements.
 * Muted small-caps DM Sans label, DM Sans regular value, optional trailing icon/action.
 */
@Composable
fun DetailFieldRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    isMono: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.uppercase(Locale.getDefault()),
                style = FieldTheme.typography.label.copy(fontSize = 10.sp, letterSpacing = 0.5.sp),
                color = FieldTheme.colors.gray500
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = if (isMono) FieldTheme.typography.mono else FieldTheme.typography.body,
                color = FieldTheme.colors.gray300
            )
        }
        if (trailingIcon != null && onTrailingIconClick != null) {
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(
                onClick = onTrailingIconClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = FieldTheme.colors.purple400,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
