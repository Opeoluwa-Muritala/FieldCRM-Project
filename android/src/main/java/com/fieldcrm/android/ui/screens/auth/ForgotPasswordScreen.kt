package com.fieldcrm.android.ui.screens.auth

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import com.fieldcrm.android.ui.theme.FieldIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.AnimatedSuccessCheckmark
import com.fieldcrm.android.ui.components.FieldCard
import com.fieldcrm.android.ui.components.FieldTextField
import com.fieldcrm.android.ui.components.PrimaryButton
import com.fieldcrm.android.ui.components.SecondaryButton
import com.fieldcrm.android.ui.theme.FieldTheme
import kotlinx.coroutines.delay

@Composable
fun ForgotPasswordScreen(
    onBackClick: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isSubmitted by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        contentAlignment = Alignment.Center
    ) {
        if (isTablet) {
            // Tablet Layout: Centered Card, max 420dp wide
            Box(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Crossfade(targetState = isSubmitted, animationSpec = tween(250), label = "forgot_password_tablet_crossfade") { submitted ->
                    if (!submitted) {
                        FieldCard {
                            ForgotPasswordForm(
                                email = email,
                                onEmailChange = { email = it; emailError = null },
                                emailError = emailError,
                                isLoading = isLoading,
                                onBackClick = onBackClick,
                                onSubmit = {
                                    if (email.isBlank()) {
                                        emailError = "Email is required"
                                    } else if (!email.contains("@")) {
                                        emailError = "Invalid email format"
                                    } else {
                                        isLoading = true
                                    }
                                }
                            )
                        }
                    } else {
                        FieldCard {
                            ForgotPasswordSuccess(
                                email = email,
                                onNavigateToLogin = onNavigateToLogin
                            )
                        }
                    }
                }
            }
        } else {
            // Phone Layout: Full Screen, scrollable, actions pushed to the bottom thumb zone
            Crossfade(
                targetState = isSubmitted,
                animationSpec = tween(250),
                label = "forgot_password_phone_crossfade",
                modifier = Modifier.fillMaxSize()
            ) { submitted ->
                if (!submitted) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp)
                    ) {
                        // Header Back Button with at least 48dp tap target height
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable { onBackClick() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = FieldIcons.ArrowBackOutlined,
                                contentDescription = "Back",
                                tint = FieldTheme.colors.purple600
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Back to Sign In",
                                style = FieldTheme.typography.bodyStrong,
                                color = FieldTheme.colors.purple600
                            )
                        }
                        Spacer(modifier = Modifier.height(32.dp))

                        // Circular lock reset icon
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(FieldTheme.colors.purple900.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FieldIcons.LockOutlined,
                                contentDescription = null,
                                tint = FieldTheme.colors.purple600,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Reset your password",
                            style = FieldTheme.typography.display,
                            color = FieldTheme.colors.gray100
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Enter the email associated with your staff account. We'll send a reset link.",
                            style = FieldTheme.typography.body,
                            color = FieldTheme.colors.gray400
                        )
                        Spacer(modifier = Modifier.height(32.dp))

                        FieldTextField(
                            value = email,
                            onValueChange = { email = it; emailError = null },
                            label = "Email address",
                            placeholder = "e.g. staff@mainstreetmfb.com",
                            errorText = emailError
                        )

                        Spacer(modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.height(32.dp))

                        PrimaryButton(
                            text = if (isLoading) "Sending..." else "Send Reset Link",
                            onClick = {
                                if (email.isBlank()) {
                                    emailError = "Email is required"
                                } else if (!email.contains("@")) {
                                    emailError = "Invalid email format"
                                } else {
                                    isLoading = true
                                }
                            },
                            enabled = !isLoading && email.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(64.dp))
                        AnimatedSuccessCheckmark()
                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = "Check your email",
                            style = FieldTheme.typography.display,
                            color = FieldTheme.colors.gray100,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val masked = maskEmail(email)
                        Text(
                            text = "We've sent a reset link to $masked",
                            style = FieldTheme.typography.body,
                            color = FieldTheme.colors.gray400,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.height(32.dp))

                        SecondaryButton(
                            text = "Back to Sign In",
                            onClick = onNavigateToLogin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        var timerSeconds by remember { mutableStateOf(47) }
                        LaunchedEffect(Unit) {
                            while (timerSeconds > 0) {
                                delay(1000)
                                timerSeconds--
                            }
                        }

                        if (timerSeconds > 0) {
                            Text(
                                text = "Didn't get it? Resend in 0:${timerSeconds.toString().padStart(2, '0')}",
                                style = FieldTheme.typography.body,
                                color = FieldTheme.colors.gray500,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable { timerSeconds = 47 },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Resend Link",
                                    style = FieldTheme.typography.bodyStrong,
                                    color = FieldTheme.colors.purple600,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(1500)
            isLoading = false
            isSubmitted = true
        }
    }
}

@Composable
fun ForgotPasswordForm(
    email: String,
    onEmailChange: (String) -> Unit,
    emailError: String?,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable { onBackClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = FieldIcons.ArrowBackOutlined,
                contentDescription = "Back",
                tint = FieldTheme.colors.purple600
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Back to Sign In",
                style = FieldTheme.typography.bodyStrong,
                color = FieldTheme.colors.purple600
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Circular lock reset icon
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(FieldTheme.colors.purple900.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = FieldIcons.LockOutlined,
                contentDescription = null,
                tint = FieldTheme.colors.purple600,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Reset your password",
            style = FieldTheme.typography.display,
            color = FieldTheme.colors.gray100
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter the email associated with your staff account. We'll send a reset link.",
            style = FieldTheme.typography.body,
            color = FieldTheme.colors.gray400
        )
        Spacer(modifier = Modifier.height(24.dp))

        FieldTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Email address",
            placeholder = "e.g. staff@mainstreetmfb.com",
            errorText = emailError
        )
        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = if (isLoading) "Sending..." else "Send Reset Link",
            onClick = onSubmit,
            enabled = !isLoading && email.isNotEmpty()
        )
    }
}

@Composable
fun ForgotPasswordSuccess(
    email: String,
    onNavigateToLogin: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        AnimatedSuccessCheckmark()
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Check your email",
            style = FieldTheme.typography.display,
            color = FieldTheme.colors.gray100,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))

        val masked = maskEmail(email)
        Text(
            text = "We've sent a reset link to $masked",
            style = FieldTheme.typography.body,
            color = FieldTheme.colors.gray400,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        SecondaryButton(
            text = "Back to Sign In",
            onClick = onNavigateToLogin
        )
        Spacer(modifier = Modifier.height(16.dp))

        var timerSeconds by remember { mutableStateOf(47) }
        LaunchedEffect(Unit) {
            while (timerSeconds > 0) {
                delay(1000)
                timerSeconds--
            }
        }
        if (timerSeconds > 0) {
            Text(
                text = "Didn't get it? Resend in 0:${timerSeconds.toString().padStart(2, '0')}",
                style = FieldTheme.typography.body,
                color = FieldTheme.colors.gray500,
                textAlign = TextAlign.Center
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { timerSeconds = 47 },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Resend Link",
                    style = FieldTheme.typography.bodyStrong,
                    color = FieldTheme.colors.purple600,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

fun maskEmail(email: String): String {
    val parts = email.split("@")
    if (parts.size != 2) return email
    val name = parts[0]
    val domain = parts[1]
    if (name.isEmpty()) return email
    return "${name[0]}***@$domain"
}
