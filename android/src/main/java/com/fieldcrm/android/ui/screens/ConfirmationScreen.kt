package com.fieldcrm.android.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.AnimatedSuccessCheckmark
import com.fieldcrm.android.ui.components.FieldCard
import com.fieldcrm.android.ui.components.PrimaryButton
import com.fieldcrm.android.ui.theme.FieldTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    
    // Start auto-dismiss countdown for 4 seconds
    LaunchedEffect(Unit) {
        val job = launch {
            countdownProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 4000, easing = LinearEasing)
            )
            // Auto dismiss action when countdown ends
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
            ) { cancelTimer() }, // Tapping anywhere cancels the auto-dismiss timer
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
                    ConfirmationContent(
                        title = title,
                        subtitle = subtitle,
                        primaryButtonText = primaryButtonText,
                        secondaryButtonText = secondaryButtonText,
                        onPrimaryClick = onPrimaryClick,
                        onSecondaryClick = onSecondaryClick,
                        countdownProgress = countdownProgress.value,
                        isTimerCancelled = isTimerCancelled,
                        isTablet = true,
                        cancelTimer = cancelTimer
                    )
                }
            }
        } else {
            // Phone Layout: Full Screen, actions pushed to the bottom thumb zone
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(64.dp))
                ConfirmationContent(
                    title = title,
                    subtitle = subtitle,
                    primaryButtonText = primaryButtonText,
                    secondaryButtonText = secondaryButtonText,
                    onPrimaryClick = onPrimaryClick,
                    onSecondaryClick = onSecondaryClick,
                    countdownProgress = countdownProgress.value,
                    isTimerCancelled = isTimerCancelled,
                    isTablet = false,
                    cancelTimer = cancelTimer
                )
            }
        }
    }
}

@Composable
fun ConfirmationContent(
    title: String,
    subtitle: String,
    primaryButtonText: String,
    secondaryButtonText: String,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit,
    countdownProgress: Float,
    isTimerCancelled: Boolean,
    isTablet: Boolean,
    cancelTimer: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        // Celebratory animated checkmark draws in
        AnimatedSuccessCheckmark()
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

        if (!isTablet) {
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(32.dp))
        } else {
            Spacer(modifier = Modifier.height(32.dp))
        }

        PrimaryButton(
            text = primaryButtonText,
            onClick = {
                cancelTimer()
                onPrimaryClick()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Ghost button for secondary action with at least 48dp tap target
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable {
                    cancelTimer()
                    onSecondaryClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = secondaryButtonText,
                style = FieldTheme.typography.bodyStrong,
                color = FieldTheme.colors.gray400
            )
        }

        // Countdown progress bar depleting over 4 seconds
        if (!isTimerCancelled) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(FieldTheme.colors.gray800)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(countdownProgress)
                        .fillMaxHeight()
                        .background(FieldTheme.colors.purple600)
                )
            }
        }
    }
}
