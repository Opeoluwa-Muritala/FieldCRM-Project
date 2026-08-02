package com.fieldcrm.android.ui.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fieldcrm.android.ui.components.FieldCard
import com.fieldcrm.android.ui.components.PrimaryButton
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text

@Composable
fun PasskeyUnavailableCard(onDismiss: () -> Unit) {
    FieldCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = FieldIcons.LockOutlined,
                contentDescription = "Passkey security",
                tint = FieldTheme.colors.purple400
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Passkey Login Unavailable",
                style = FieldTheme.typography.title,
                color = FieldTheme.colors.gray100,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Your organisation has not enabled passkey verification yet. Fallback to standard email and password authentication to sign in.",
                style = FieldTheme.typography.body,
                color = FieldTheme.colors.gray400,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                text = "Back to Password Login",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
