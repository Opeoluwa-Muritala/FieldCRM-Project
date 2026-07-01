package com.fieldcrm.android.ui.screens.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.fieldcrm.android.ui.theme.FieldIcons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.FieldCard
import com.fieldcrm.android.ui.components.FieldDivider
import com.fieldcrm.android.ui.components.PrimaryButton
import com.fieldcrm.android.ui.components.SecondaryButton
import com.fieldcrm.android.ui.theme.FieldTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun ConfirmationScreen(
    title: String,
    subtitle: String,
    primaryButtonText: String = "View Application",
    secondaryButtonText: String = "Back to Queue",
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit
) {
    val countdownProgress = remember { Animatable(1f) }
    val timerJob = remember { mutableStateOf<Job?>(null) }
    var isTimerCancelled by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    // Pulsing circle animation for success state badge
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        val job = launch {
            countdownProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 4000, easing = LinearEasing)
            )
            if (!isTimerCancelled) {
                onSecondaryClick()
            }
        }
        timerJob.value = job
    }

    val cancelTimer = {
        if (!isTimerCancelled) {
            isTimerCancelled = true
            timerJob.value?.cancel()
            scope.launch {
                countdownProgress.snapTo(0f)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { cancelTimer() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            FieldCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Success Check Circle Badge with pulse animation
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                            .background(FieldTheme.colors.purple900.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(FieldTheme.colors.statusSuccess, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FieldIcons.CheckOutlined,
                                contentDescription = "Success",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = title,
                        style = FieldTheme.typography.title,
                        color = FieldTheme.colors.gray100,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = subtitle,
                        style = FieldTheme.typography.body,
                        color = FieldTheme.colors.gray400,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    FieldDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Transaction Summary Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FieldTheme.colors.gray900, RoundedCornerShape(4.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "LEDGER METADATA",
                            style = FieldTheme.typography.label.copy(fontSize = 10.sp),
                            color = FieldTheme.colors.gray500
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Dossier Status", style = FieldTheme.typography.body.copy(fontSize = 12.sp), color = FieldTheme.colors.gray400)
                            Text("SIGNED & QUEUED", style = FieldTheme.typography.mono.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = FieldTheme.colors.statusSuccess)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Local Integrity Hash", style = FieldTheme.typography.body.copy(fontSize = 12.sp), color = FieldTheme.colors.gray400)
                            Text("SHA-256 Validated", style = FieldTheme.typography.mono.copy(fontSize = 12.sp), color = FieldTheme.colors.gray300)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    PrimaryButton(
                        text = primaryButtonText,
                        onClick = {
                            cancelTimer()
                            onPrimaryClick()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    SecondaryButton(
                        text = secondaryButtonText,
                        onClick = {
                            cancelTimer()
                            onSecondaryClick()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Bottom progress bar (4 seconds timer)
        if (!isTimerCancelled) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(FieldTheme.colors.gray800)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(countdownProgress.value)
                        .fillMaxHeight()
                        .background(FieldTheme.colors.purple600)
                )
            }
        }
    }
}

