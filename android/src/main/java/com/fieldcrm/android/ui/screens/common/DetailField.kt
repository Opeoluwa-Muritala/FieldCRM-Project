package com.fieldcrm.android.ui.screens.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
