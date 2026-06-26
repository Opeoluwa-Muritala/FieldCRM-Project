package com.fieldcrm.android.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.AnimatedSuccessCheckmark
import com.fieldcrm.android.ui.components.FieldCard
import com.fieldcrm.android.ui.components.FieldTextField
import com.fieldcrm.android.ui.components.PrimaryButton
import com.fieldcrm.android.ui.theme.FieldTheme
import kotlinx.coroutines.delay

@Composable
fun ResetPasswordScreen(
    onNavigateToLogin: (prefilledEmail: String, successMessage: String) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    
    var isLoading by remember { mutableStateOf(false) }
    var isSubmitted by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    // Validation rules
    val lengthMet = newPassword.length >= 8
    val numberMet = newPassword.any { it.isDigit() }
    val specialMet = newPassword.any { !it.isLetterOrDigit() }
    
    val strengthScore = (if (lengthMet) 1 else 0) + (if (numberMet) 1 else 0) + (if (specialMet) 1 else 0) + (if (newPassword.length >= 12) 1 else 0)
    
    val strengthLabel = when (strengthScore) {
        0 -> "Very Weak"
        1 -> "Weak"
        2 -> "Moderate"
        3 -> "Strong"
        else -> "Very Strong"
    }
    
    val strengthColor = when (strengthScore) {
        0, 1 -> FieldTheme.colors.statusDanger
        2 -> FieldTheme.colors.statusWarning
        else -> FieldTheme.colors.statusSuccess
    }

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
                Crossfade(targetState = isSubmitted, animationSpec = tween(250), label = "reset_password_tablet_crossfade") { submitted ->
                    if (!submitted) {
                        FieldCard {
                            ResetPasswordFormContent(
                                newPassword = newPassword,
                                onNewPasswordChange = { newPassword = it },
                                confirmPassword = confirmPassword,
                                onConfirmPasswordChange = { confirmPassword = it; confirmPasswordError = null },
                                showNewPassword = showNewPassword,
                                onToggleShowNewPassword = { showNewPassword = !showNewPassword },
                                showConfirmPassword = showConfirmPassword,
                                onToggleShowConfirmPassword = { showConfirmPassword = !showConfirmPassword },
                                lengthMet = lengthMet,
                                numberMet = numberMet,
                                specialMet = specialMet,
                                strengthLabel = strengthLabel,
                                strengthColor = strengthColor,
                                confirmPasswordError = confirmPasswordError,
                                isLoading = isLoading,
                                onSubmit = {
                                    if (newPassword != confirmPassword) {
                                        confirmPasswordError = "Passwords do not match"
                                    } else if (!lengthMet || !numberMet) {
                                        confirmPasswordError = "Password does not meet requirements"
                                    } else {
                                        isLoading = true
                                    }
                                }
                            )
                        }
                    } else {
                        FieldCard {
                            ResetPasswordSuccessContent()
                        }
                    }
                }
            }
        } else {
            // Phone Layout: Full Screen, Scrollable, CTA pushed to bottom
            Crossfade(
                targetState = isSubmitted,
                animationSpec = tween(250),
                label = "reset_password_phone_crossfade",
                modifier = Modifier.fillMaxSize()
            ) { submitted ->
                if (!submitted) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Create a new password",
                            style = FieldTheme.typography.display,
                            color = FieldTheme.colors.gray100
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        FieldTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = "New Password",
                            placeholder = "••••••••",
                            visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                Text(
                                    text = if (showNewPassword) "HIDE" else "SHOW",
                                    style = FieldTheme.typography.bodyStrong.copy(fontSize = 12.sp),
                                    color = FieldTheme.colors.purple600,
                                    modifier = Modifier
                                        .clickable { showNewPassword = !showNewPassword }
                                        .padding(end = 12.dp)
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        FieldTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; confirmPasswordError = null },
                            label = "Confirm password",
                            placeholder = "••••••••",
                            visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            errorText = confirmPasswordError,
                            trailingIcon = {
                                Text(
                                    text = if (showConfirmPassword) "HIDE" else "SHOW",
                                    style = FieldTheme.typography.bodyStrong.copy(fontSize = 12.sp),
                                    color = FieldTheme.colors.purple600,
                                    modifier = Modifier
                                        .clickable { showConfirmPassword = !showConfirmPassword }
                                        .padding(end = 12.dp)
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // Strength Indicator
                        Text(
                            text = "Password strength: $strengthLabel",
                            style = FieldTheme.typography.body.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                            color = strengthColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (i in 0 until 4) {
                                val active = strengthScore > i
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .background(
                                            if (active) strengthColor else FieldTheme.colors.gray800,
                                            shape = MaterialTheme.shapes.small
                                        )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        // Requirements Check List
                        Text(
                            text = "Must contain:",
                            style = FieldTheme.typography.bodyStrong.copy(fontSize = 12.sp),
                            color = FieldTheme.colors.gray300
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        RequirementRow(satisfied = lengthMet, text = "8+ characters")
                        Spacer(modifier = Modifier.height(8.dp))
                        RequirementRow(satisfied = numberMet, text = "One number")
                        Spacer(modifier = Modifier.height(8.dp))
                        RequirementRow(satisfied = specialMet, text = "One special character")

                        Spacer(modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.height(32.dp))

                        PrimaryButton(
                            text = if (isLoading) "Updating..." else "Update Password",
                            onClick = {
                                if (newPassword != confirmPassword) {
                                    confirmPasswordError = "Passwords do not match"
                                } else if (!lengthMet || !numberMet) {
                                    confirmPasswordError = "Password does not meet requirements"
                                } else {
                                    isLoading = true
                                }
                            },
                            enabled = !isLoading && newPassword.isNotEmpty() && confirmPassword.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.height(48.dp))
                        AnimatedSuccessCheckmark()
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Password Reset Successful",
                            style = FieldTheme.typography.display,
                            color = FieldTheme.colors.gray100,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Your password has been reset successfully. Redirecting you to sign in...",
                            style = FieldTheme.typography.body,
                            color = FieldTheme.colors.gray400,
                            textAlign = TextAlign.Center
                        )
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

    LaunchedEffect(isSubmitted) {
        if (isSubmitted) {
            delay(2000)
            onNavigateToLogin("staff@mainstreetmfb.com", "Password updated. Please sign in.")
        }
    }
}

@Composable
fun ResetPasswordFormContent(
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    showNewPassword: Boolean,
    onToggleShowNewPassword: () -> Unit,
    showConfirmPassword: Boolean,
    onToggleShowConfirmPassword: () -> Unit,
    lengthMet: Boolean,
    numberMet: Boolean,
    specialMet: Boolean,
    strengthLabel: String,
    strengthColor: Color,
    confirmPasswordError: String?,
    isLoading: Boolean,
    onSubmit: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Create a new password",
            style = FieldTheme.typography.display,
            color = FieldTheme.colors.gray100
        )
        Spacer(modifier = Modifier.height(16.dp))

        FieldTextField(
            value = newPassword,
            onValueChange = onNewPasswordChange,
            label = "New Password",
            placeholder = "••••••••",
            visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Text(
                    text = if (showNewPassword) "HIDE" else "SHOW",
                    style = FieldTheme.typography.bodyStrong.copy(fontSize = 12.sp),
                    color = FieldTheme.colors.purple600,
                    modifier = Modifier
                        .clickable { onToggleShowNewPassword() }
                        .padding(end = 12.dp)
                )
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        FieldTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = "Confirm password",
            placeholder = "••••••••",
            visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
            errorText = confirmPasswordError,
            trailingIcon = {
                Text(
                    text = if (showConfirmPassword) "HIDE" else "SHOW",
                    style = FieldTheme.typography.bodyStrong.copy(fontSize = 12.sp),
                    color = FieldTheme.colors.purple600,
                    modifier = Modifier
                        .clickable { onToggleShowConfirmPassword() }
                        .padding(end = 12.dp)
                )
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Strength
        Text(
            text = "Password strength: $strengthLabel",
            style = FieldTheme.typography.body.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
            color = strengthColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val score = (if (lengthMet) 1 else 0) + (if (numberMet) 1 else 0) + (if (specialMet) 1 else 0) + (if (newPassword.length >= 12) 1 else 0)
            for (i in 0 until 4) {
                val active = score > i
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            if (active) strengthColor else FieldTheme.colors.gray800,
                            shape = MaterialTheme.shapes.small
                        )
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Checklist
        Text(
            text = "Must contain:",
            style = FieldTheme.typography.bodyStrong.copy(fontSize = 12.sp),
            color = FieldTheme.colors.gray300
        )
        Spacer(modifier = Modifier.height(8.dp))
        RequirementRow(satisfied = lengthMet, text = "8+ characters")
        Spacer(modifier = Modifier.height(4.dp))
        RequirementRow(satisfied = numberMet, text = "One number")
        Spacer(modifier = Modifier.height(4.dp))
        RequirementRow(satisfied = specialMet, text = "One special character")
        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = if (isLoading) "Updating..." else "Update Password",
            onClick = onSubmit,
            enabled = !isLoading && newPassword.isNotEmpty() && confirmPassword.isNotEmpty()
        )
    }
}

@Composable
fun ResetPasswordSuccessContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        AnimatedSuccessCheckmark()
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Password Reset Successful",
            style = FieldTheme.typography.display,
            color = FieldTheme.colors.gray100,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your password has been reset successfully. Redirecting you to sign in...",
            style = FieldTheme.typography.body,
            color = FieldTheme.colors.gray400,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RequirementRow(satisfied: Boolean, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        val color by animateColorAsState(
            targetValue = if (satisfied) FieldTheme.colors.statusSuccess else FieldTheme.colors.gray500,
            animationSpec = tween(100),
            label = "requirement_color"
        )
        Text(
            text = if (satisfied) "✓" else "○",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.width(20.dp)
        )
        Text(
            text = text,
            style = FieldTheme.typography.body.copy(fontSize = 13.sp),
            color = color
        )
    }
}
