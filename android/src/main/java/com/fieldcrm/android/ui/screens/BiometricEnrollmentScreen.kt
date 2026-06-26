package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.FieldCard
import com.fieldcrm.android.ui.components.PrimaryButton
import com.fieldcrm.android.ui.theme.FieldTheme

@Composable
fun BiometricEnrollmentScreen(
    onEnableClick: () -> Unit,
    onNotNowClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        contentAlignment = Alignment.Center
    ) {
        if (isTablet) {
            // Tablet Layout: Centered Card
            Box(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                FieldCard {
                    BiometricFormContent(
                        onEnableClick = onEnableClick,
                        onNotNowClick = onNotNowClick
                    )
                }
            }
        } else {
            // Phone Layout: Full Screen, scrollable, actions pushed to the bottom thumb zone
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(64.dp))
                // Platform native fingerprint / Face ID representation
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Biometric Icon",
                    tint = FieldTheme.colors.purple600,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Sign in faster next time",
                    style = FieldTheme.typography.display,
                    color = FieldTheme.colors.gray100,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Use biometric lock to access FieldCRM quickly and securely without typing your password.",
                    style = FieldTheme.typography.body,
                    color = FieldTheme.colors.gray400,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(32.dp))

                PrimaryButton(
                    text = "Enable Biometric Login",
                    onClick = onEnableClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Ghost Button for "Not Now" with at least 48dp tap target height
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { onNotNowClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Not Now",
                        style = FieldTheme.typography.bodyStrong,
                        color = FieldTheme.colors.gray400
                    )
                }
            }
        }
    }
}

@Composable
fun BiometricFormContent(
    onEnableClick: () -> Unit,
    onNotNowClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Biometric Icon",
            tint = FieldTheme.colors.purple600,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Sign in faster next time",
            style = FieldTheme.typography.display,
            color = FieldTheme.colors.gray100,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Use biometric lock to access FieldCRM quickly and securely without typing your password.",
            style = FieldTheme.typography.body,
            color = FieldTheme.colors.gray400,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        PrimaryButton(
            text = "Enable Biometric Login",
            onClick = onEnableClick
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Ghost button for "Not Now" with at least 48dp tap target
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable { onNotNowClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Not Now",
                style = FieldTheme.typography.bodyStrong,
                color = FieldTheme.colors.gray400
            )
        }
    }
}
