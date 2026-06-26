package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fieldcrm.android.ui.components.FieldCard
import com.fieldcrm.android.ui.components.PrimaryButton
import com.fieldcrm.android.ui.theme.FieldTheme
import kotlinx.coroutines.delay

@Composable
fun FullScreenErrorScreen(
    onRetryClick: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.background),
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
                    
                    // CloudOff Icon Circle Badge
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(FieldTheme.colors.gray800, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudOff,
                            contentDescription = "No Connection",
                            tint = FieldTheme.colors.gray400,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Something went wrong",
                        style = FieldTheme.typography.display,
                        color = FieldTheme.colors.gray100,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "We couldn't load this. Check your connection and try again.",
                        style = FieldTheme.typography.body,
                        color = FieldTheme.colors.gray400,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))

                    PrimaryButton(
                        text = if (isLoading) "CHECKING CONNECTION..." else "TRY AGAIN",
                        onClick = { isLoading = true },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(1500)
            isLoading = false
            onRetryClick()
        }
    }
}

