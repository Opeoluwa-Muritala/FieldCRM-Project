package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.core.session.UserSession
import com.fieldcrm.android.ui.components.FieldCard
import com.fieldcrm.android.ui.components.FieldTextField
import com.fieldcrm.android.ui.components.PrimaryButton
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.viewmodel.LoginUiState
import com.fieldcrm.android.ui.viewmodel.LoginViewModel

@Composable
fun LoginScreenView(
    viewModel: LoginViewModel,
    onLoginSuccess: (UserSession) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var biometricEnabled by remember { mutableStateOf(false) }

    LoginScreenContent(
        state = state,
        biometricEnabled = biometricEnabled,
        onEmailChange = { viewModel.setEmail(it) },
        onPasswordChange = { viewModel.setPassword(it) },
        onBiometricToggle = { biometricEnabled = it },
        onLoginClick = { viewModel.login(onSuccess = onLoginSuccess) }
    )
}

@Composable
fun LoginScreenContent(
    state: LoginUiState,
    biometricEnabled: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onLoginClick: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        contentAlignment = Alignment.Center
    ) {
        val isWideScreen = maxWidth >= 600.dp
        
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
                FieldCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "FieldCRM",
                            style = FieldTheme.typography.display.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = FieldTheme.colors.gray100
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "LENDING OPERATIONS GATEWAY",
                            style = FieldTheme.typography.label.copy(
                                letterSpacing = 1.sp,
                                color = FieldTheme.colors.purple400
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        FieldTextField(
                            value = state.email,
                            onValueChange = onEmailChange,
                            label = "Phone Number or BVN",
                            isRequired = true,
                            placeholder = "e.g. 22233344455",
                            enabled = !state.isLoading
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        FieldTextField(
                            value = state.password,
                            onValueChange = onPasswordChange,
                            label = "Password",
                            isRequired = true,
                            placeholder = "••••••••",
                            visualTransformation = PasswordVisualTransformation(),
                            enabled = !state.isLoading
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Biometric Toggle Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "BIOMETRIC LOCK",
                                    style = FieldTheme.typography.label,
                                    color = FieldTheme.colors.gray400
                                )
                                Text(
                                    text = "Enable fingerprint sign-in",
                                    style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                    color = FieldTheme.colors.gray500
                                )
                            }
                            Switch(
                                checked = biometricEnabled,
                                onCheckedChange = onBiometricToggle,
                                enabled = !state.isLoading,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = FieldTheme.colors.purple600,
                                    uncheckedThumbColor = FieldTheme.colors.gray500,
                                    uncheckedTrackColor = FieldTheme.colors.gray800
                                )
                            )
                        }
                        
                        val error = state.error
                        if (error != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = error,
                                style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                color = FieldTheme.colors.statusDanger
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        PrimaryButton(
                            text = if (state.isLoading) "Authenticating..." else "Login Securely",
                            onClick = onLoginClick,
                            enabled = !state.isLoading && state.email.isNotEmpty() && state.password.isNotEmpty()
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// PREVIEWS FOR RESPONSIVE BREAKPOINTS
// ==========================================

@Preview(name = "Compact Phone - 411dp", widthDp = 411, heightDp = 850)
@Composable
fun PreviewLoginCompact() {
    FieldCRMTheme {
        LoginScreenContent(
            state = LoginUiState(email = "chidi@mmfb.com"),
            biometricEnabled = true,
            onEmailChange = {},
            onPasswordChange = {},
            onBiometricToggle = {},
            onLoginClick = {}
        )
    }
}

@Preview(name = "Small Phone - 320dp", widthDp = 320, heightDp = 640)
@Composable
fun PreviewLoginSmall() {
    FieldCRMTheme {
        LoginScreenContent(
            state = LoginUiState(email = "fatima@mmfb.com"),
            biometricEnabled = false,
            onEmailChange = {},
            onPasswordChange = {},
            onBiometricToggle = {},
            onLoginClick = {}
        )
    }
}

@Preview(name = "Foldable Unfolded - 673dp", widthDp = 673, heightDp = 800)
@Composable
fun PreviewLoginFoldable() {
    FieldCRMTheme {
        LoginScreenContent(
            state = LoginUiState(email = "adebayo@mmfb.com"),
            biometricEnabled = true,
            onEmailChange = {},
            onPasswordChange = {},
            onBiometricToggle = {},
            onLoginClick = {}
        )
    }
}

@Preview(name = "Tablet - 1280dp", widthDp = 1280, heightDp = 800)
@Composable
fun PreviewLoginTablet() {
    FieldCRMTheme {
        LoginScreenContent(
            state = LoginUiState(email = "admin@mmfb.com"),
            biometricEnabled = true,
            onEmailChange = {},
            onPasswordChange = {},
            onBiometricToggle = {},
            onLoginClick = {}
        )
    }
}
