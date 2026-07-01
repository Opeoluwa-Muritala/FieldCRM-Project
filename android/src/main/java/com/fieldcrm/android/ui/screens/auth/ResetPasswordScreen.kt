package com.fieldcrm.android.ui.screens.auth

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.fieldcrm.android.ui.theme.FieldIcons
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
import com.fieldcrm.android.ui.components.SecondaryButton
import com.fieldcrm.android.data.api.MobileApiService
import com.fieldcrm.android.ui.theme.FieldTheme
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

@Composable
fun ResetPasswordScreen(
    onNavigateToLogin: (prefilledEmail: String, successMessage: String) -> Unit
) {
    val apiService: MobileApiService = koinInject()

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var resetToken by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var isSubmitted by remember { mutableStateOf(false) }
    var resetError by remember { mutableStateOf<String?>(null) }
 
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
 
    // Validation rules
    val lengthMet = newPassword.length >= 8
    val uppercaseMet = newPassword.any { it.isUpperCase() }
    val numberMet = newPassword.any { it.isDigit() }
    val specialMet = newPassword.any { !it.isLetterOrDigit() }
    
    val strengthScore = (if (lengthMet) 1 else 0) + (if (uppercaseMet) 1 else 0) + (if (numberMet) 1 else 0) + (if (specialMet) 1 else 0)
    
    val strengthLabel = when (strengthScore) {
        0, 1 -> "Weak"
        2 -> "Fair"
        3 -> "Good"
        else -> "Strong"
    }
    
    val strengthColor = when (strengthScore) {
        0, 1 -> FieldTheme.colors.statusDanger
        2 -> FieldTheme.colors.statusWarning
        3 -> FieldTheme.colors.statusSuccess
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
                                uppercaseMet = uppercaseMet,
                                numberMet = numberMet,
                                specialMet = specialMet,
                                strengthLabel = strengthLabel,
                                strengthColor = strengthColor,
                                confirmPasswordError = confirmPasswordError,
                                isLoading = isLoading,
                                onSubmit = {
                                    if (newPassword != confirmPassword) {
                                        confirmPasswordError = "Passwords do not match"
                                    } else if (!lengthMet || !uppercaseMet || !numberMet || !specialMet) {
                                        confirmPasswordError = "Password does not meet requirements"
                                    } else {
                                        isLoading = true
                                    }
                                },
                                onCancel = { onNavigateToLogin("", "") }
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
                        
                        // Shield brand mark circle
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(FieldTheme.colors.gray850, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = FieldIcons.ShieldOutlined,
                                    contentDescription = null,
                                    tint = FieldTheme.colors.purple600,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Reset Password",
                                style = FieldTheme.typography.display.copy(fontSize = 24.sp),
                                color = FieldTheme.colors.purple600,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Enter a new password for your FieldCRM account to regain access securely.",
                                style = FieldTheme.typography.body.copy(fontSize = 14.sp),
                                color = FieldTheme.colors.gray500,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.widthIn(max = 300.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
 
                        FieldTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = "New Password",
                            placeholder = "Enter new password",
                            visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showNewPassword = !showNewPassword }) {
                                    Icon(
                                        imageVector = if (showNewPassword) FieldIcons.EyeOutlined else FieldIcons.EyeOffOutlined,
                                        contentDescription = "Toggle password visibility",
                                        tint = FieldTheme.colors.gray400
                                    )
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
 
                        // Strength Indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Password Strength",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray400
                            )
                            Text(
                                text = strengthLabel,
                                style = FieldTheme.typography.label,
                                color = strengthColor
                            )
                        }
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
                                        .height(6.dp)
                                        .background(
                                            if (active) strengthColor else FieldTheme.colors.gray850,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
 
                        // Requirements Check List
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FieldTheme.colors.gray900, RoundedCornerShape(FieldTheme.shapes.cardRadius))
                                .border(0.5.dp, FieldTheme.colors.gray800, RoundedCornerShape(FieldTheme.shapes.cardRadius))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Your password must contain:",
                                    style = FieldTheme.typography.bodyStrong.copy(fontSize = 12.sp),
                                    color = FieldTheme.colors.gray100
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                RequirementRow(satisfied = lengthMet, text = "At least 8 characters")
                                RequirementRow(satisfied = uppercaseMet, text = "One uppercase letter")
                                RequirementRow(satisfied = numberMet, text = "One number")
                                RequirementRow(satisfied = specialMet, text = "One special character")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        FieldTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; confirmPasswordError = null },
                            label = "Confirm password",
                            placeholder = "Re-enter new password",
                            visualTransformation = PasswordVisualTransformation(),
                            errorText = confirmPasswordError
                        )
 
                        Spacer(modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.height(32.dp))
 
                        PrimaryButton(
                            text = if (isLoading) "Updating..." else "Reset Password",
                            onClick = {
                                if (newPassword != confirmPassword) {
                                    confirmPasswordError = "Passwords do not match"
                                } else if (!lengthMet || !uppercaseMet || !numberMet || !specialMet) {
                                    confirmPasswordError = "Password does not meet requirements"
                                } else {
                                    isLoading = true
                                }
                            },
                            enabled = !isLoading && newPassword.isNotEmpty() && confirmPassword.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        SecondaryButton(
                            text = "Cancel",
                            onClick = { onNavigateToLogin("", "") },
                            modifier = Modifier.fillMaxWidth()
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
            val success = apiService.resetPassword(resetToken, newPassword)
            isLoading = false
            if (success) {
                isSubmitted = true
            } else {
                resetError = "Reset link is invalid or expired. Please request a new one."
            }
        }
    }

    LaunchedEffect(isSubmitted) {
        if (isSubmitted) {
            delay(2000)
            onNavigateToLogin("", "Password updated. Please sign in.")
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
    @Suppress("UNUSED_PARAMETER") showConfirmPassword: Boolean,
    @Suppress("UNUSED_PARAMETER") onToggleShowConfirmPassword: () -> Unit,
    lengthMet: Boolean,
    uppercaseMet: Boolean,
    numberMet: Boolean,
    specialMet: Boolean,
    strengthLabel: String,
    strengthColor: Color,
    confirmPasswordError: String?,
    isLoading: Boolean,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Shield brand mark circle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(FieldTheme.colors.gray850, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FieldIcons.ShieldOutlined,
                    contentDescription = null,
                    tint = FieldTheme.colors.purple600,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Reset Password",
                style = FieldTheme.typography.display.copy(fontSize = 24.sp),
                color = FieldTheme.colors.purple600,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enter a new password for your FieldCRM account to regain access securely.",
                style = FieldTheme.typography.body.copy(fontSize = 14.sp),
                color = FieldTheme.colors.gray500,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 300.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        FieldTextField(
            value = newPassword,
            onValueChange = onNewPasswordChange,
            label = "New Password",
            placeholder = "Enter new password",
            visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { onToggleShowNewPassword() }) {
                    Icon(
                        imageVector = if (showNewPassword) FieldIcons.EyeOutlined else FieldIcons.EyeOffOutlined,
                        contentDescription = "Toggle password visibility",
                        tint = FieldTheme.colors.gray400
                    )
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Strength
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Password Strength",
                style = FieldTheme.typography.label,
                color = FieldTheme.colors.gray400
            )
            Text(
                text = strengthLabel,
                style = FieldTheme.typography.label,
                color = strengthColor
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val score = (if (lengthMet) 1 else 0) + (if (uppercaseMet) 1 else 0) + (if (numberMet) 1 else 0) + (if (specialMet) 1 else 0)
            for (i in 0 until 4) {
                val active = score > i
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .background(
                            if (active) strengthColor else FieldTheme.colors.gray850,
                            shape = CircleShape
                        )
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Requirements Check List
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(FieldTheme.colors.gray900, RoundedCornerShape(FieldTheme.shapes.cardRadius))
                .border(0.5.dp, FieldTheme.colors.gray800, RoundedCornerShape(FieldTheme.shapes.cardRadius))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Your password must contain:",
                    style = FieldTheme.typography.bodyStrong.copy(fontSize = 12.sp),
                    color = FieldTheme.colors.gray100
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                RequirementRow(satisfied = lengthMet, text = "At least 8 characters")
                RequirementRow(satisfied = uppercaseMet, text = "One uppercase letter")
                RequirementRow(satisfied = numberMet, text = "One number")
                RequirementRow(satisfied = specialMet, text = "One special character")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        FieldTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = "Confirm password",
            placeholder = "Re-enter new password",
            visualTransformation = PasswordVisualTransformation(),
            errorText = confirmPasswordError
        )
        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = if (isLoading) "Updating..." else "Reset Password",
            onClick = onSubmit,
            enabled = !isLoading && newPassword.isNotEmpty() && confirmPassword.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        SecondaryButton(
            text = "Cancel",
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        val color by animateColorAsState(
            targetValue = if (satisfied) FieldTheme.colors.statusSuccess else FieldTheme.colors.gray500,
            animationSpec = tween(100),
            label = "requirement_color"
        )
        Icon(
            imageVector = if (satisfied) FieldIcons.CheckCircleOutlined else FieldIcons.RadioUncheckedOutlined,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = FieldTheme.typography.body.copy(fontSize = 13.sp),
            color = color
        )
    }
}
