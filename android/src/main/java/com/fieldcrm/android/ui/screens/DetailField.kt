package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.theme.FieldTheme
import java.util.Locale

@Composable
fun DetailField(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = FieldTheme.typography.label.copy(fontSize = 11.sp),
            color = FieldTheme.colors.gray500
        )
        Text(
            text = value,
            style = FieldTheme.typography.bodyStrong,
            color = FieldTheme.colors.gray300
        )
    }
}
