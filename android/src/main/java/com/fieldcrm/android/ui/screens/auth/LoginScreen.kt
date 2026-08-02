package com.fieldcrm.android.ui.screens.auth

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.core.session.SessionStore
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.core.session.UserSession
import com.fieldcrm.android.ui.components.FieldCard
import com.fieldcrm.android.ui.components.FieldTextField
import com.fieldcrm.android.ui.components.PrimaryButton
import com.fieldcrm.android.ui.components.SecondaryButton
import com.fieldcrm.android.ui.components.FieldEmail
import com.fieldcrm.android.ui.components.FieldPassword
import com.fieldcrm.android.ui.components.KeyboardAwareForm
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.viewmodel.LoginUiState
import com.fieldcrm.android.ui.viewmodel.LoginViewModel
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

@Composable
fun LoginScreenView(
    viewModel: LoginViewModel,
    hasPasscode: Boolean,
    onLoginSuccess: (UserSession) -> Unit,
    onForgotPasswordClick: () -> Unit,
    onPasscodeClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LoginScreenContent(
        state = state,
        viewModel = viewModel,
        hasPasscode = hasPasscode,
        onEmailChange = { viewModel.setEmail(it) },
        onPasswordChange = { viewModel.setPassword(it) },
        onLoginClick = { viewModel.login(onSuccess = onLoginSuccess) },
        onForgotPasswordClick = onForgotPasswordClick,
        onPasscodeClick = onPasscodeClick,
        onLoginSuccess = onLoginSuccess
    )
}

@Composable
fun LoginScreenContent(
    state: LoginUiState,
    viewModel: LoginViewModel,
    hasPasscode: Boolean = false,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onPasscodeClick: () -> Unit = {},
    onLoginSuccess: (UserSession) -> Unit
) {
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }
    var showNewDeviceNotice by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    val shakeOffset = remember { Animatable(0f) }
    var triggerShake by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }

    val context = LocalContext.current
    val sessionStore: SessionStore = koinInject()

    // Biometric Preferences & prior session verification
    val biometricPrefs = remember { context.getSharedPreferences("fieldcrm_biometric_prefs", Context.MODE_PRIVATE) }
    val biometricEnabled = remember { biometricPrefs.getBoolean("biometric_enabled", false) }
    val hasPriorSession = remember { sessionStore.isStored() }

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

    val errorMessage = when (state.error) {
        "network_error" -> "No internet connection. Check your network and try again."
        "invalid_credentials" -> "Incorrect email or password. Please try again."
        null -> null
        else -> state.error
    }
    val isNetworkError = state.error == "network_error"

    LaunchedEffect(state.error) {
        when {
            state.error == null -> passwordError = null
            isNetworkError -> { passwordError = null; triggerShake = false }
            else -> { passwordError = errorMessage; triggerShake = true }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        contentAlignment = Alignment.Center
    ) {
        KeyboardAwareForm(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                            
                            // Centered Brand Mark (Shield Purple)
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(FieldTheme.colors.purple900.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
                                    .border(0.5.dp, FieldTheme.colors.purple600.copy(alpha = 0.3f), RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = FieldIcons.ShieldOutlined,
                                    contentDescription = "Shield Logo",
                                    tint = FieldTheme.colors.purple400,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Text(
                                text = "Institutional Portal",
                                style = FieldTheme.typography.title.copy(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
                                color = FieldTheme.colors.gray100,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "SECURE STAFF AUTHENTICATION",
                                style = FieldTheme.typography.label.copy(
                                    color = FieldTheme.colors.purple400,
                                    letterSpacing = 1.sp
                                ),
                                textAlign = TextAlign.Center
                            )
      
                            Spacer(modifier = Modifier.height(if (isNetworkError) 16.dp else 32.dp))

                            // Network / server error banner
                            if (isNetworkError) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            FieldTheme.colors.statusWarning.copy(alpha = 0.1f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(0.5.dp, FieldTheme.colors.statusWarning.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = FieldIcons.InfoOutlined,
                                        contentDescription = null,
                                        tint = FieldTheme.colors.statusWarning,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "No internet connection. Check your network and try again.",
                                        style = FieldTheme.typography.body.copy(fontSize = 13.sp),
                                        color = FieldTheme.colors.statusWarning
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Email or Staff ID Field
                            FieldEmail(
                                value = state.email,
                                onValueChange = {
                                    onEmailChange(it)
                                    emailError = null
                                },
                                label = "Email or Staff ID",
                                placeholder = "e.g. staff@mainstreetmfb.com",
                                enabled = !state.isLoading,
                                errorText = emailError,
                                focusRequester = emailFocusRequester,
                                imeAction = ImeAction.Next,
                                keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
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
                            FieldPassword(
                                value = state.password,
                                onValueChange = {
                                    onPasswordChange(it)
                                    passwordError = null
                                },
                                label = "Password",
                                placeholder = "••••••••",
                                enabled = !state.isLoading,
                                errorText = passwordError,
                                focusRequester = passwordFocusRequester,
                                imeAction = ImeAction.Done,
                                keyboardActions = KeyboardActions(onDone = {
                                    focusManager.clearFocus()
                                    onLoginClick()
                                }),
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
      
                            // Sign In Button
                            val inputsFilled = state.email.isNotEmpty() && state.password.isNotEmpty()
                            PrimaryButton(
                                text = if (state.isLoading) "Logging In..." else "Log In",
                                onClick = {
                                    focusManager.clearFocus()
                                    onLoginClick()
                                },
                                enabled = !state.isLoading && inputsFilled,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .alpha(if (inputsFilled) 1f else 0.4f)
                            )
      
                            // Passcode option
                            if (hasPasscode) {
                                Spacer(modifier = Modifier.height(8.dp))
                                SecondaryButton(
                                    text = "Enter Passcode",
                                    onClick = onPasscodeClick,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = FieldIcons.LockOutlined,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Biometric quick-login affordance (fingerprint icon) beneath the form
                            if (biometricEnabled && hasPriorSession) {
                                Spacer(modifier = Modifier.height(16.dp))
                                val activity = context as? androidx.fragment.app.FragmentActivity
                                if (activity != null) {
                                    IconButton(
                                        onClick = {
                                            showBiometricPrompt(activity) {
                                                viewModel.restoreStoredSession(onLoginSuccess, {
                                                    // Fallback if session restore fails
                                                })
                                            }
                                        },
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Icon(
                                            imageVector = FieldIcons.FingerprintOutlined,
                                            contentDescription = "Biometric Quick Login",
                                            tint = FieldTheme.colors.purple400,
                                            modifier = Modifier.size(44.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            SecondaryButton(
                                text = "Sign in on a new device",
                                onClick = { showNewDeviceNotice = !showNewDeviceNotice },
                                leadingIcon = {
                                    Icon(
                                        imageVector = FieldIcons.LockOutlined,
                                        contentDescription = "New device sign-in",
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
      
                            if (showNewDeviceNotice) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Passkey sign-in will appear here once your organisation enables secure passkey verification. Use your staff password for this device today.",
                                    style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                    color = FieldTheme.colors.gray500,
                                    textAlign = TextAlign.Center,
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
                                    imageVector = FieldIcons.InfoOutlined,
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
}

private fun showBiometricPrompt(
    activity: androidx.fragment.app.FragmentActivity,
    onSuccess: () -> Unit
) {
    val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
    val callback = object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            onSuccess()
        }
    }
    val biometricPrompt = androidx.biometric.BiometricPrompt(activity, executor, callback)
    val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
        .setTitle("Biometric Quick Login")
        .setSubtitle("Scan fingerprint or face to sign in")
        .setNegativeButtonText("Use Password")
        .build()
    biometricPrompt.authenticate(promptInfo)
}
