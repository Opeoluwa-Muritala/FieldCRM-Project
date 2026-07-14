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
import androidx.compose.ui.tooling.preview.Preview
import com.fieldcrm.android.ui.components.FieldCard
import com.fieldcrm.android.ui.components.PrimaryButton
import com.fieldcrm.android.ui.components.SecondaryButton
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.theme.FieldCRMTheme
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
                text = "Passkeys are not set up yet",
                style = FieldTheme.typography.title,
                color = FieldTheme.colors.gray100,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Your organisation must enable secure passkey verification before you can create or manage a passkey on this device.",
                style = FieldTheme.typography.body,
                color = FieldTheme.colors.gray400,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            SecondaryButton(
                text = "Create a passkey",
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            PrimaryButton(text = "Back", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview(name = "Passkeys — compact", widthDp = 411)
@Composable
private fun PasskeyUnavailableCardPreview() {
    FieldCRMTheme { PasskeyUnavailableCard(onDismiss = {}) }
}

@Preview(name = "Passkeys — dark", widthDp = 411, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PasskeyUnavailableCardDarkPreview() {
    FieldCRMTheme(darkTheme = true) { PasskeyUnavailableCard(onDismiss = {}) }
}

@Preview(name = "Passkeys — expanded", widthDp = 840)
@Composable
private fun PasskeyUnavailableCardExpandedPreview() {
    FieldCRMTheme { PasskeyUnavailableCard(onDismiss = {}) }
}
