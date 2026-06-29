package com.fieldcrm.android.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GppMaybe
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fieldcrm.android.ui.components.FieldCard
import com.fieldcrm.android.ui.components.PrimaryButton
import com.fieldcrm.android.ui.components.SecondaryButton
import com.fieldcrm.android.ui.theme.FieldTheme

@Composable
fun SessionExpiredScreen(
    userEmail: String,
    onReauthSuccess: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            FieldCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Warning icon circle badge
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(FieldTheme.colors.statusDanger.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GppMaybe,
                            contentDescription = "Session Expired",
                            tint = FieldTheme.colors.statusDanger,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Session Expired",
                        style = FieldTheme.typography.display,
                        color = FieldTheme.colors.gray100,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "For your security, you have been automatically logged out due to inactivity. Please sign in again to access FieldCRM.",
                        style = FieldTheme.typography.body,
                        color = FieldTheme.colors.gray400,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))

                    PrimaryButton(
                        text = "Login Again",
                        onClick = onReauthSuccess,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    SecondaryButton(
                        text = "Return to Home",
                        onClick = onReauthSuccess,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
