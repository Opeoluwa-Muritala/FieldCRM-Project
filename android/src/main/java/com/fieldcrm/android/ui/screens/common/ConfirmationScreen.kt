package com.fieldcrm.android.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fieldcrm.android.ui.components.FieldCard
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
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    
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
            .background(FieldTheme.colors.background)
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
                    
                    // Success Check Circle Badge
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .background(FieldTheme.colors.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Success",
                            tint = FieldTheme.colors.onPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = title,
                        style = FieldTheme.typography.display,
                        color = FieldTheme.colors.gray100,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = subtitle,
                        style = FieldTheme.typography.body,
                        color = FieldTheme.colors.gray400,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

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
                        .background(FieldTheme.colors.primary)
                )
            }
        }
    }
}

