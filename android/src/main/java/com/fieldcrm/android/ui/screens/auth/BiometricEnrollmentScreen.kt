package com.fieldcrm.android.ui.screens.auth

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.FieldCard
import com.fieldcrm.android.ui.components.PrimaryButton
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme

@Composable
fun BiometricEnrollmentScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isAuthenticating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Pulsing circle animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    fun handleEnableBiometric() {
        val activity = context as? androidx.fragment.app.FragmentActivity
        if (activity == null) {
            errorMessage = "System error: Host activity unavailable"
            return
        }

        isAuthenticating = true
        errorMessage = null

        val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
        val callback = object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                isAuthenticating = false
                // Save biometric enabled preference securely
                val biometricPrefs = context.getSharedPreferences("fieldcrm_biometric_prefs", Context.MODE_PRIVATE)
                biometricPrefs.edit().putBoolean("biometric_enabled", true).apply()
                onComplete()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                isAuthenticating = false
                if (errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED) {
                    errorMessage = errString.toString()
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                isAuthenticating = false
                errorMessage = "Authentication failed. Try again."
            }
        }

        val biometricPrompt = androidx.biometric.BiometricPrompt(activity, executor, callback)
        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Enable Biometric Login")
            .setSubtitle("Authenticate using your biometric credentials to confirm setup")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
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

                    // Pulse-animated Biometric Illustration
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                            .background(FieldTheme.colors.purple900.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(FieldTheme.colors.purple900.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FieldIcons.FingerprintOutlined,
                                contentDescription = "Biometrics icon",
                                tint = FieldTheme.colors.purple400,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "Biometric Sign-In",
                        style = FieldTheme.typography.title,
                        color = FieldTheme.colors.gray100,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Access your staff account instantly using secure biometric validation on this device.",
                        style = FieldTheme.typography.body,
                        color = FieldTheme.colors.gray400,
                        textAlign = TextAlign.Center
                    )

                    errorMessage?.let {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = it,
                            color = FieldTheme.colors.statusDanger,
                            style = FieldTheme.typography.body.copy(fontSize = 13.sp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    PrimaryButton(
                        text = if (isAuthenticating) "Enrolling..." else "Enable Biometric Login",
                        onClick = { handleEnableBiometric() },
                        enabled = !isAuthenticating,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = onComplete,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Skip for now",
                            style = FieldTheme.typography.bodyStrong,
                            color = FieldTheme.colors.gray500,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
