package com.fieldcrm.android.ui.screens.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme
import java.security.MessageDigest

enum class PasscodeMode { SETUP, LOGIN }

private fun hashPin(pin: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

@Composable
fun PasscodeScreen(
    mode: PasscodeMode,
    storedHash: String? = null,
    onSetupComplete: (hash: String) -> Unit = {},
    onLoginSuccess: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirming by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val shakeOffset = remember { Animatable(0f) }

    fun triggerShake() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            repeat(3) {
                shakeOffset.animateTo(8f, tween(50, easing = LinearEasing))
                shakeOffset.animateTo(-8f, tween(50, easing = LinearEasing))
            }
            shakeOffset.animateTo(0f, tween(50, easing = LinearEasing))
        }
    }

    fun onDigit(d: String) {
        errorMessage = null
        val current = if (isConfirming) confirmPin else pin
        if (current.length >= 6) return
        val updated = current + d
        if (isConfirming) confirmPin = updated else pin = updated

        if (updated.length == 6) {
            when (mode) {
                PasscodeMode.SETUP -> {
                    if (!isConfirming) {
                        isConfirming = true
                    } else {
                        if (pin == confirmPin) {
                            onSetupComplete(hashPin(pin))
                        } else {
                            triggerShake()
                            errorMessage = "PINs don't match. Try again."
                            confirmPin = ""
                            pin = ""
                            isConfirming = false
                        }
                    }
                }
                PasscodeMode.LOGIN -> {
                    if (storedHash != null && hashPin(updated) == storedHash) {
                        onLoginSuccess()
                    } else {
                        triggerShake()
                        errorMessage = "Incorrect passcode"
                        pin = ""
                    }
                }
            }
        }
    }

    fun onBackspace() {
        errorMessage = null
        if (isConfirming) {
            confirmPin = confirmPin.dropLast(1)
        } else {
            pin = pin.dropLast(1)
        }
    }

    val activePin = if (isConfirming) confirmPin else pin
    val title = when {
        mode == PasscodeMode.SETUP && !isConfirming -> "Create Passcode"
        mode == PasscodeMode.SETUP && isConfirming -> "Confirm Passcode"
        else -> "Enter Passcode"
    }
    val subtitle = when {
        mode == PasscodeMode.SETUP && !isConfirming -> "Set a 6-digit PIN for quick access"
        mode == PasscodeMode.SETUP && isConfirming -> "Re-enter your PIN to confirm"
        else -> "Enter your 6-digit PIN"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .padding(24.dp)
                .offset(x = shakeOffset.value.dp)
        ) {
            // Back button
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = FieldIcons.ArrowBackOutlined,
                        contentDescription = "Back",
                        tint = FieldTheme.colors.gray400
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Lock icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(FieldTheme.colors.purple900.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FieldIcons.LockOutlined,
                    contentDescription = null,
                    tint = FieldTheme.colors.purple600,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = title,
                style = FieldTheme.typography.display,
                color = FieldTheme.colors.gray100
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = FieldTheme.typography.body,
                color = FieldTheme.colors.gray400,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 6 dot indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(6) { index ->
                    val filled = index < activePin.length
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(
                                color = if (filled) FieldTheme.colors.purple600 else FieldTheme.colors.gray700,
                                shape = CircleShape
                            )
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    style = FieldTheme.typography.label,
                    color = FieldTheme.colors.statusDanger,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Number pad
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "⌫")
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.6f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (key.isEmpty()) FieldTheme.colors.gray950
                                        else FieldTheme.colors.gray800
                                    )
                                    .clickable(enabled = key.isNotEmpty()) {
                                        if (key == "⌫") onBackspace() else onDigit(key)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "⌫") {
                                    Icon(
                                        imageVector = FieldIcons.DeleteOutlined,
                                        contentDescription = "Backspace",
                                        tint = FieldTheme.colors.gray300,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else if (key.isNotEmpty()) {
                                    Text(
                                        text = key,
                                        style = FieldTheme.typography.display.copy(fontSize = 24.sp),
                                        color = FieldTheme.colors.gray100
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
