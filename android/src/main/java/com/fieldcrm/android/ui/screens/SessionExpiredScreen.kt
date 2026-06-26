package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.FieldCard
import com.fieldcrm.android.ui.components.FieldTextField
import com.fieldcrm.android.ui.components.PrimaryButton
import com.fieldcrm.android.ui.theme.FieldTheme
import kotlinx.coroutines.delay

@Composable
fun SessionExpiredScreen(
    userEmail: String,
    onReauthSuccess: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

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
                FieldCard {
                    SessionExpiredForm(
                        userEmail = userEmail,
                        password = password,
                        onPasswordChange = { password = it; passwordError = null },
                        showPassword = showPassword,
                        onToggleShowPassword = { showPassword = !showPassword },
                        passwordError = passwordError,
                        isLoading = isLoading,
                        isTablet = true,
                        onSubmit = {
                            if (password.isBlank()) {
                                passwordError = "Password is required"
                            } else if (password != "password123") {
                                passwordError = "Incorrect password"
                            } else {
                                isLoading = true
                            }
                        }
                    )
                }
            }
        } else {
            // Phone Layout: Full Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                SessionExpiredForm(
                    userEmail = userEmail,
                    password = password,
                    onPasswordChange = { password = it; passwordError = null },
                    showPassword = showPassword,
                    onToggleShowPassword = { showPassword = !showPassword },
                    passwordError = passwordError,
                    isLoading = isLoading,
                    isTablet = false,
                    onSubmit = {
                        if (password.isBlank()) {
                            passwordError = "Password is required"
                        } else if (password != "password123") {
                            passwordError = "Incorrect password"
                        } else {
                            isLoading = true
                        }
                    }
                )
            }
        }
    }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(1500)
            isLoading = false
            onReauthSuccess()
        }
    }
}

@Composable
fun SessionExpiredForm(
    userEmail: String,
    password: String,
    onPasswordChange: (String) -> Unit,
    showPassword: Boolean,
    onToggleShowPassword: () -> Unit,
    passwordError: String?,
    isLoading: Boolean,
    isTablet: Boolean,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Session Expired Lock",
            tint = FieldTheme.colors.purple600,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Your session has expired",
            style = FieldTheme.typography.display,
            color = FieldTheme.colors.gray100,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "For your security, please sign in again to continue. Any local changes will be preserved.",
            style = FieldTheme.typography.body,
            color = FieldTheme.colors.gray400,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Email: $userEmail",
            style = FieldTheme.typography.bodyStrong.copy(fontSize = 13.sp),
            color = FieldTheme.colors.gray300
        )
        Spacer(modifier = Modifier.height(16.dp))

        FieldTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Password",
            placeholder = "••••••••",
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            errorText = passwordError,
            trailingIcon = {
                Text(
                    text = if (showPassword) "HIDE" else "SHOW",
                    style = FieldTheme.typography.bodyStrong.copy(fontSize = 12.sp),
                    color = FieldTheme.colors.purple600,
                    modifier = Modifier
                        .clickable { onToggleShowPassword() }
                        .padding(end = 12.dp)
                )
            }
        )

        if (!isTablet) {
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(32.dp))
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }

        PrimaryButton(
            text = if (isLoading) "Re-authenticating..." else "Sign In Again",
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )
    }
}
