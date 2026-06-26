package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    ErrorFormContent(
                        isLoading = isLoading,
                        isTablet = true,
                        onTryAgain = { isLoading = true }
                    )
                }
            }
        } else {
            // Phone Layout: Full Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(64.dp))
                ErrorFormContent(
                    isLoading = isLoading,
                    isTablet = false,
                    onTryAgain = { isLoading = true }
                )
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

@Composable
fun ErrorFormContent(
    isLoading: Boolean,
    isTablet: Boolean,
    onTryAgain: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Warning Error",
            tint = FieldTheme.colors.gray500,
            modifier = Modifier.size(64.dp)
        )
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

        if (!isTablet) {
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(32.dp))
        } else {
            Spacer(modifier = Modifier.height(32.dp))
        }

        PrimaryButton(
            text = if (isLoading) "Checking connection..." else "Try Again",
            onClick = onTryAgain,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )
    }
}
