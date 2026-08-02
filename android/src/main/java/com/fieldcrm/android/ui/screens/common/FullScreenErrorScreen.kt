package com.fieldcrm.android.ui.screens.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.FieldCard
import com.fieldcrm.android.ui.components.FieldTopAppBar
import com.fieldcrm.android.ui.components.PrimaryButton
import com.fieldcrm.android.ui.components.SecondaryButton
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenErrorScreen(
    title: String = "Connection Offline",
    description: String = "Unable to reach the Mainstreet ledger network. Please confirm mobile data or WiFi status and try again.",
    onRetryClick: () -> Unit,
    onGoHomeClick: (() -> Unit)? = null,
    onLogOutClick: (() -> Unit)? = null
) {
    var isLoading by remember { mutableStateOf(false) }

    // Pulsing circle animation for error state badge
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Scaffold(
        topBar = {
            // Z1: Top bar stripped down (no navigation, only branding)
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(end = 16.dp)
                    ) {
                        Icon(
                            imageVector = FieldIcons.ShieldOutlined,
                            contentDescription = null,
                            tint = FieldTheme.colors.purple400,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FieldCRM Ledger Link",
                            style = FieldTheme.typography.label.copy(
                                color = FieldTheme.colors.gray100,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FieldTheme.colors.gray950
                )
            )
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 420.dp)
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
                        
                        // CloudOff / Alert Icon Circle Badge with pulse animation
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                                .background(FieldTheme.colors.statusDanger.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FieldIcons.CloudOffOutlined,
                                contentDescription = "Critical Warning",
                                tint = FieldTheme.colors.statusDanger,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = title,
                            style = FieldTheme.typography.title,
                            color = FieldTheme.colors.gray100,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = description,
                            style = FieldTheme.typography.body,
                            color = FieldTheme.colors.gray400,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))

                        // Dominant Retry Action Button
                        PrimaryButton(
                            text = if (isLoading) "VERIFYING LINK..." else "RETRY QUEUE",
                            onClick = {
                                isLoading = true
                            },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Secondary action buttons if provided
                        if (onGoHomeClick != null || onLogOutClick != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                onGoHomeClick?.let { goHome ->
                                    SecondaryButton(
                                        text = "Go to Home",
                                        onClick = goHome,
                                        enabled = !isLoading,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                onLogOutClick?.let { logOut ->
                                    TextButton(
                                        onClick = logOut,
                                        enabled = !isLoading,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Log Out Credentials",
                                            style = FieldTheme.typography.bodyStrong,
                                            color = FieldTheme.colors.statusDanger,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            kotlinx.coroutines.delay(1000)
            isLoading = false
            onRetryClick()
        }
    }
}
