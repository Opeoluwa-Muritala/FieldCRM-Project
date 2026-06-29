package com.fieldcrm.android.ui.screens.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.core.session.UserSession
import com.fieldcrm.android.ui.components.FieldCard
import com.fieldcrm.android.ui.components.FieldTextField
import com.fieldcrm.android.ui.components.PrimaryButton
import com.fieldcrm.android.ui.components.SecondaryButton
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.viewmodel.LoginUiState
import com.fieldcrm.android.ui.viewmodel.LoginViewModel
import kotlinx.coroutines.delay

@Composable
fun LoginScreenView(
    viewModel: LoginViewModel,
    hasEnrolledBiometrics: Boolean,
    onLoginSuccess: (UserSession) -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LoginScreenContent(
        state = state,
        hasEnrolledBiometrics = hasEnrolledBiometrics,
        onEmailChange = { viewModel.setEmail(it) },
        onPasswordChange = { viewModel.setPassword(it) },
        onLoginClick = { viewModel.login(onSuccess = onLoginSuccess) },
        onForgotPasswordClick = onForgotPasswordClick
    )
}

@Composable
fun LoginScreenContent(
    state: LoginUiState,
    hasEnrolledBiometrics: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    val shakeOffset = remember { Animatable(0f) }
    var triggerShake by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    // Error shake animation sequence
    LaunchedEffect(triggerShake) {
        if (triggerShake) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            repeat(3) {
                shakeOffset.animateTo(6f, animationSpec = tween(50, easing = LinearEasing))
                shakeOffset.animateTo(-6f, animationSpec = tween(50, easing = LinearEasing))
            }
            shakeOffset.animateTo(0f, animationSpec = tween(50, easing = LinearEasing))
            triggerShake = false
        }
    }

    // Capture when validation error changes
    LaunchedEffect(state.error) {
        if (state.error != null) {
            passwordError = state.error
            triggerShake = true
        } else {
            passwordError = null
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
            ) {
                FieldCard(
                    modifier = Modifier.offset(x = shakeOffset.value.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Centered Shield + M Logo Mark, 64dp, Gray900 (White surface in Light Theme)
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(FieldTheme.colors.gray900, RoundedCornerShape(FieldTheme.shapes.cardRadius)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = "Shield Logo",
                                tint = FieldTheme.colors.purple600,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Mainstreet",
                            style = FieldTheme.typography.display,
                            color = FieldTheme.colors.gray100,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "MICROFINANCE BANK",
                            style = FieldTheme.typography.label.copy(
                                color = FieldTheme.colors.gray500
                            ),
                            textAlign = TextAlign.Center
                        )
 
                        Spacer(modifier = Modifier.height(32.dp))
 
                        // Email or Staff ID Field with Blur Validation
                        FieldTextField(
                            value = state.email,
                            onValueChange = {
                                onEmailChange(it)
                                emailError = null
                            },
                            label = "Email or Staff ID",
                            placeholder = "e.g. staff@mainstreetmfb.com",
                            enabled = !state.isLoading,
                            errorText = emailError,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = FieldTheme.colors.gray400
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    if (!focusState.isFocused && state.email.isNotEmpty()) {
                                        if (!state.email.contains("@") && state.email.length < 4) {
                                            emailError = "Invalid email format or staff ID"
                                        }
                                    }
                                }
                        )
 
                        Spacer(modifier = Modifier.height(16.dp))
 
                        // Password Field with Show/Hide toggle
                        FieldTextField(
                            value = state.password,
                            onValueChange = {
                                onPasswordChange(it)
                                passwordError = null
                            },
                            label = "Password",
                            placeholder = "••••••••",
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            enabled = !state.isLoading,
                            errorText = passwordError,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = FieldTheme.colors.gray400
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                        contentDescription = if (showPassword) "Toggle password visibility" else "Toggle password visibility",
                                        tint = FieldTheme.colors.gray400
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
 
                        Spacer(modifier = Modifier.height(12.dp))
 
                        // Forgot password? Link
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "Forgot password?",
                                style = FieldTheme.typography.bodyStrong,
                                color = FieldTheme.colors.purple600,
                                modifier = Modifier
                                    .clickable { onForgotPasswordClick() }
                                    .padding(vertical = 4.dp)
                            )
                        }
 
                        Spacer(modifier = Modifier.height(24.dp))
 
                        // Sign In Button (opacity 40% when disabled)
                        val inputsFilled = state.email.isNotEmpty() && state.password.isNotEmpty()
                        PrimaryButton(
                            text = if (state.isLoading) "Signing In..." else "Sign In",
                            onClick = {
                                focusManager.clearFocus()
                                onLoginClick()
                            },
                            enabled = !state.isLoading && inputsFilled,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .alpha(if (inputsFilled) 1f else 0.4f)
                        )
 
                        // Biometric option (shown on second+ login only)
                        if (hasEnrolledBiometrics) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val context = LocalContext.current
                            SecondaryButton(
                                text = "Use Face ID / Touch ID",
                                onClick = {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                        val executor = ContextCompat.getMainExecutor(context)
                                        val biometricPrompt = BiometricPrompt.Builder(context)
                                            .setTitle("Biometric Login")
                                            .setSubtitle("Sign in to your FieldCRM staff account")
                                            .setNegativeButton("Cancel", executor) { _, _ -> }
                                            .build()

                                        biometricPrompt.authenticate(
                                            CancellationSignal(),
                                            executor,
                                            object : BiometricPrompt.AuthenticationCallback() {
                                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                                                    onEmailChange("chidi@mmfb.com")
                                                    onPasswordChange("password123")
                                                    onLoginClick()
                                                }
                                            }
                                        )
                                    } else {
                                        onEmailChange("chidi@mmfb.com")
                                        onPasswordChange("password123")
                                        onLoginClick()
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Fingerprint,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
 
                        Spacer(modifier = Modifier.height(32.dp))
 
                        // Bottom Anchored Helper Label
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clickable { /* IT Support Navigation */ }
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.HelpOutline,
                                contentDescription = null,
                                tint = FieldTheme.colors.gray500,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Need help? Contact IT",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray500
                            )
                        }
                    }
                }
            }
        }
    }
}
