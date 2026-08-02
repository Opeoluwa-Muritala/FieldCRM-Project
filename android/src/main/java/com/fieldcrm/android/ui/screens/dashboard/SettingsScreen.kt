package com.fieldcrm.android.ui.screens.dashboard

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.core.network.ApiResult
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.data.api.MobileApiService
import com.fieldcrm.android.ui.components.ConfirmationDialog
import com.fieldcrm.android.ui.components.FieldCard
import com.fieldcrm.android.ui.components.FieldDivider
import com.fieldcrm.android.ui.components.FieldPassword
import com.fieldcrm.android.ui.components.FieldTextField
import com.fieldcrm.android.ui.components.FieldTopAppBar
import com.fieldcrm.android.ui.components.PrimaryButton
import com.fieldcrm.android.ui.components.SecondaryButton
import com.fieldcrm.android.ui.screens.common.DetailFieldRow
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.viewmodel.ConfigViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userName: String = "",
    userEmail: String = "chidi@mmfb.com",
    role: UserRole? = UserRole.LOAN_OFFICER,
    onBackClick: (() -> Unit)? = null,
    onNavigateToOfflineQueue: () -> Unit = {},
    onSignOutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val api: MobileApiService = koinInject()
    val scope = rememberCoroutineScope()
    val configViewModel: ConfigViewModel = koinViewModel()
    val configState by configViewModel.uiState.collectAsState()
    val config = configState.config

    // ── SharedPreferences storage for themes and biometrics ─────────────────────
    val themePrefs = remember { context.getSharedPreferences("fieldcrm_theme_prefs", Context.MODE_PRIVATE) }
    var selectedTheme by remember { mutableStateOf(themePrefs.getString("theme", "System") ?: "System") }

    val biometricPrefs = remember { context.getSharedPreferences("fieldcrm_biometric_prefs", Context.MODE_PRIVATE) }
    var biometricEnabled by remember { mutableStateOf(biometricPrefs.getBoolean("biometric_enabled", false)) }

    // ── Biometric capability check ──────────────────────────────────────────────
    val hasBiometricSupport = remember {
        val bm = BiometricManager.from(context)
        val canAuth = bm.canAuthenticate(Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK)
        canAuth == BiometricManager.BIOMETRIC_SUCCESS
    }

    // ── State for modals & active overlays ──────────────────────────────────────
    var activeOverlay by rememberSaveable { mutableStateOf<String?>(null) } // "PASSWORD" or "NOTIFICATIONS"
    var showLogoutConfirm by remember { mutableStateOf(false) }

    // ── Notification Preferences states ─────────────────────────────────────────
    var notifyApprovals by rememberSaveable { mutableStateOf(biometricPrefs.getBoolean("pref_notify_approvals", true)) }
    var notifyDocs by rememberSaveable { mutableStateOf(biometricPrefs.getBoolean("pref_notify_docs", true)) }
    var notifySystem by rememberSaveable { mutableStateOf(biometricPrefs.getBoolean("pref_notify_system", false)) }

    // ── Active Overlay Rendering ────────────────────────────────────────────────
    if (activeOverlay != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FieldTheme.colors.gray950),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                ) {
                    when (activeOverlay) {
                        "PASSWORD" -> ChangePasswordOverlay(
                            api = api,
                            onDismiss = { activeOverlay = null }
                        )
                        "NOTIFICATIONS" -> NotificationPrefsOverlay(
                            initialApprovals = notifyApprovals,
                            initialDocs = notifyDocs,
                            initialSystem = notifySystem,
                            onSave = { app, doc, sys ->
                                notifyApprovals = app
                                notifyDocs = doc
                                notifySystem = sys
                                biometricPrefs.edit()
                                    .putBoolean("pref_notify_approvals", app)
                                    .putBoolean("pref_notify_docs", doc)
                                    .putBoolean("pref_notify_system", sys)
                                    .apply()
                                activeOverlay = null
                            },
                            onDismiss = { activeOverlay = null }
                        )
                    }
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                FieldTopAppBar(
                    title = "Settings",
                    navigationIcon = if (onBackClick != null) {
                        {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = FieldIcons.ArrowBackOutlined,
                                    contentDescription = "Back",
                                    tint = FieldTheme.colors.gray400
                                )
                            }
                        }
                    } else null
                )
            },
            containerColor = FieldTheme.colors.gray950
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Main Settings Form Grouped
                    Column(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Account Section
                        FieldCard {
                            Text(
                                text = "ACCOUNT",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.purple400
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            DetailFieldRow(label = "Full Name", value = userName)
                            FieldDivider()
                            DetailFieldRow(label = "Email Address", value = userEmail)
                            FieldDivider()
                            DetailFieldRow(label = "User Role", value = role?.displayName ?: "Relationship Officer")
                            FieldDivider()
                            DetailFieldRow(
                                label = "Branch / Organization",
                                value = config?.org_name ?: "Mainstreet MFB"
                            )
                        }

                        // 2. Security Section
                        FieldCard {
                            Text(
                                text = "SECURITY",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.purple400
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeOverlay = "PASSWORD" }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = FieldIcons.LockOutlined,
                                    contentDescription = null,
                                    tint = FieldTheme.colors.purple400,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Change Password",
                                        style = FieldTheme.typography.bodyStrong,
                                        color = FieldTheme.colors.gray100
                                    )
                                    Text(
                                        text = "Update login credentials securely",
                                        style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                        color = FieldTheme.colors.gray500
                                    )
                                }
                                Icon(
                                    imageVector = FieldIcons.ChevronRightOutlined,
                                    contentDescription = "Go",
                                    tint = FieldTheme.colors.gray500,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (hasBiometricSupport) {
                                FieldDivider()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = FieldIcons.FingerprintOutlined,
                                        contentDescription = null,
                                        tint = FieldTheme.colors.purple400,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Biometric Authentication",
                                            style = FieldTheme.typography.bodyStrong,
                                            color = FieldTheme.colors.gray100
                                        )
                                        Text(
                                            text = "Enable face or fingerprint login",
                                            style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                            color = FieldTheme.colors.gray500
                                        )
                                    }
                                    Switch(
                                        checked = biometricEnabled,
                                        onCheckedChange = { checked ->
                                            biometricEnabled = checked
                                            biometricPrefs.edit().putBoolean("biometric_enabled", checked).apply()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = FieldTheme.colors.purple600,
                                            uncheckedThumbColor = FieldTheme.colors.gray400,
                                            uncheckedTrackColor = FieldTheme.colors.gray800
                                        )
                                    )
                                }
                            }
                        }

                        // 3. Preferences Section
                        FieldCard {
                            Text(
                                text = "PREFERENCES",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.purple400
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Theme Selector Row
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Text(
                                    text = "Application Theme",
                                    style = FieldTheme.typography.bodyStrong,
                                    color = FieldTheme.colors.gray100
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val themes = listOf("Light", "Dark", "System")
                                    themes.forEach { theme ->
                                        val isSelected = selectedTheme == theme
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp)
                                                .background(
                                                    color = if (isSelected) FieldTheme.colors.purple900.copy(alpha = 0.2f) else FieldTheme.colors.gray900,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) FieldTheme.colors.purple600 else FieldTheme.colors.gray800,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    selectedTheme = theme
                                                    themePrefs.edit().putString("theme", theme).apply()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = theme,
                                                style = FieldTheme.typography.body.copy(
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = if (isSelected) FieldTheme.colors.purple400 else FieldTheme.colors.gray400
                                            )
                                        }
                                    }
                                }
                            }

                            FieldDivider()

                            // Notification Preferences Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeOverlay = "NOTIFICATIONS" }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = FieldIcons.HomeOutlined, // Bell or home as fallback
                                    contentDescription = null,
                                    tint = FieldTheme.colors.purple400,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Notification Preferences",
                                        style = FieldTheme.typography.bodyStrong,
                                        color = FieldTheme.colors.gray100
                                    )
                                    Text(
                                        text = "Choose which alerts to receive",
                                        style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                        color = FieldTheme.colors.gray500
                                    )
                                }
                                Icon(
                                    imageVector = FieldIcons.ChevronRightOutlined,
                                    contentDescription = "Go",
                                    tint = FieldTheme.colors.gray500,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // 4. About Section
                        FieldCard {
                            Text(
                                text = "ABOUT",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.purple400
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            DetailFieldRow(label = "Application Version", value = "1.0.0 (Build 42)")
                            FieldDivider()
                            DetailFieldRow(label = "Node Endpoint ID", value = config?.node_id ?: "ledger_ng_lagos_01")
                        }
                    }

                    // 5. Log Out Section (visually isolated at the very bottom)
                    Column(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 32.dp, bottom = 16.dp)
                    ) {
                        Button(
                            onClick = { showLogoutConfirm = true },
                            shape = RoundedCornerShape(FieldTheme.shapes.inputRadius),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = FieldTheme.colors.statusDanger
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                FieldTheme.colors.statusDanger.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = "Log Out",
                                style = FieldTheme.typography.bodyStrong,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Log Out Confirmation Dialog ───────────────────────────────
    if (showLogoutConfirm) {
        ConfirmationDialog(
            title = "Log Out of FieldCRM?",
            description = "You will be signed out of your staff profile. Offline cache pending synchronisation remains stored locally on this secure device.",
            confirmButtonText = "Log Out",
            cancelButtonText = "Cancel",
            isDestructive = false, // Routine confirmation
            onConfirm = {
                showLogoutConfirm = false
                onSignOutClick()
            },
            onCancel = {
                showLogoutConfirm = false
            }
        )
    }
}

// ── Change Password Subscreen Overlay ───────────────────────────────────────
@Composable
fun ChangePasswordOverlay(
    api: MobileApiService,
    onDismiss: () -> Unit
) {
    var oldPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var showPasswords by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Validation Requirements
    val lengthMet = newPassword.length >= 8
    val hasUpper = newPassword.any { it.isUpperCase() }
    val hasDigit = newPassword.any { it.isDigit() }
    val hasSpecial = newPassword.any { !it.isLetterOrDigit() }

    val meetsRequirements = lengthMet && hasUpper && hasDigit && hasSpecial
    val matchesConfirm = newPassword == confirmPassword && confirmPassword.isNotEmpty()

    val score = (if (lengthMet) 1 else 0) + (if (hasUpper) 1 else 0) + (if (hasDigit) 1 else 0) + (if (hasSpecial) 1 else 0)
    val strengthText = when (score) {
        0, 1 -> "Weak"
        2 -> "Medium"
        3 -> "Good"
        else -> "Strong"
    }
    val strengthColor = when (score) {
        0, 1 -> FieldTheme.colors.statusDanger
        2 -> FieldTheme.colors.statusWarning
        3 -> FieldTheme.colors.statusSuccess
        else -> FieldTheme.colors.statusSuccess
    }

    val scope = rememberCoroutineScope()

    FieldCard {
        if (isSuccess) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(FieldTheme.colors.purple900.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FieldIcons.CheckOutlined,
                        contentDescription = "Success",
                        tint = FieldTheme.colors.statusSuccess,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Password Updated",
                    style = FieldTheme.typography.title,
                    color = FieldTheme.colors.gray100
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your staff account password credentials have been changed successfully.",
                    style = FieldTheme.typography.body,
                    color = FieldTheme.colors.gray400,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                PrimaryButton(
                    text = "Done",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismiss() }
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = FieldIcons.ArrowBackOutlined,
                    contentDescription = "Back",
                    tint = FieldTheme.colors.purple400
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Back to Settings",
                    style = FieldTheme.typography.bodyStrong,
                    color = FieldTheme.colors.purple400
                )
            }

            Text(
                text = "Change Password",
                style = FieldTheme.typography.title,
                color = FieldTheme.colors.gray100
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Current Password field (never logged, cached, or retained on exit)
            FieldPassword(
                value = oldPassword,
                onValueChange = { oldPassword = it },
                label = "Current Password",
                placeholder = "••••••••",
                enabled = !isSaving
            )
            Spacer(modifier = Modifier.height(12.dp))

            // New Password
            FieldPassword(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = "New Password",
                placeholder = "••••••••",
                enabled = !isSaving
            )

            // Password strength feedback
            if (newPassword.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Password Strength: $strengthText",
                        style = FieldTheme.typography.label,
                        color = strengthColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { score / 4.0f },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = strengthColor,
                    trackColor = FieldTheme.colors.gray800,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Confirm password
            FieldPassword(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirm New Password",
                placeholder = "••••••••",
                enabled = !isSaving
            )

            // Inline requirements checkmarks
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RequirementRow(text = "At least 8 characters", isMet = lengthMet)
                RequirementRow(text = "At least one uppercase letter", isMet = hasUpper)
                RequirementRow(text = "At least one digit", isMet = hasDigit)
                RequirementRow(text = "At least one special character", isMet = hasSpecial)
                RequirementRow(text = "Passwords match", isMet = matchesConfirm)
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = it,
                    color = FieldTheme.colors.statusDanger,
                    style = FieldTheme.typography.body.copy(fontSize = 13.sp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(
                    text = "Cancel",
                    onClick = {
                        // Securely clear raw state variables on exit
                        oldPassword = ""
                        newPassword = ""
                        confirmPassword = ""
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                )
                PrimaryButton(
                    text = if (isSaving) "Saving..." else "Save",
                    onClick = {
                        scope.launch {
                            isSaving = true
                            errorMessage = null
                            // Save current locally, make sure it is not retained
                            val currentToSubmit = oldPassword
                            val newToSubmit = newPassword
                            val confirmToSubmit = confirmPassword

                            // Wipe password variables instantly in memory to comply with main prompt §8
                            oldPassword = ""
                            newPassword = ""
                            confirmPassword = ""

                            when (val result = api.changePassword(currentToSubmit, newToSubmit, confirmToSubmit)) {
                                is ApiResult.Success -> {
                                    isSuccess = result.data.changed
                                    if (!isSuccess) {
                                        errorMessage = "Failed to update password. Verify current credentials."
                                    }
                                }
                                is ApiResult.Error -> {
                                    errorMessage = result.detail ?: "Internal server error"
                                }
                                is ApiResult.NetworkError -> {
                                    errorMessage = result.message ?: "Network unavailable"
                                }
                                ApiResult.Loading -> Unit
                            }
                            isSaving = false
                        }
                    },
                    enabled = meetsRequirements && matchesConfirm && !isSaving,
                    modifier = Modifier.weight(1.2f)
                )
            }
        }
    }
}

@Composable
fun RequirementRow(text: String, isMet: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isMet) FieldIcons.CheckOutlined else FieldIcons.CloseOutlined,
            contentDescription = null,
            tint = if (isMet) FieldTheme.colors.statusSuccess else FieldTheme.colors.gray600,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = FieldTheme.typography.body.copy(fontSize = 12.sp),
            color = if (isMet) FieldTheme.colors.gray300 else FieldTheme.colors.gray500
        )
    }
}

// ── Notification Preferences Overlay ────────────────────────────────────────
@Composable
fun NotificationPrefsOverlay(
    initialApprovals: Boolean,
    initialDocs: Boolean,
    initialSystem: Boolean,
    onSave: (Boolean, Boolean, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var appVal by rememberSaveable { mutableStateOf(initialApprovals) }
    var docVal by rememberSaveable { mutableStateOf(initialDocs) }
    var sysVal by rememberSaveable { mutableStateOf(initialSystem) }

    FieldCard {
        Text(
            text = "Notification Preferences",
            style = FieldTheme.typography.title,
            color = FieldTheme.colors.gray100
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Select which system-level triggers generate push notifications on this staff device.",
            style = FieldTheme.typography.body.copy(fontSize = 12.sp),
            color = FieldTheme.colors.gray500
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Checkbox 1
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { appVal = !appVal }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = appVal,
                onCheckedChange = { appVal = it },
                colors = CheckboxDefaults.colors(checkedColor = FieldTheme.colors.purple600)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Decision & Concurrence Approvals",
                    style = FieldTheme.typography.bodyStrong,
                    color = FieldTheme.colors.gray100
                )
                Text(
                    text = "When dossiers are escalated or signed off",
                    style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                    color = FieldTheme.colors.gray500
                )
            }
        }

        FieldDivider()

        // Checkbox 2
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { docVal = !docVal }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = docVal,
                onCheckedChange = { docVal = it },
                colors = CheckboxDefaults.colors(checkedColor = FieldTheme.colors.purple600)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Intake & Document Sync Alerts",
                    style = FieldTheme.typography.bodyStrong,
                    color = FieldTheme.colors.gray100
                )
                Text(
                    text = "Updates from offline sync queue transfers",
                    style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                    color = FieldTheme.colors.gray500
                )
            }
        }

        FieldDivider()

        // Checkbox 3
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { sysVal = !sysVal }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = sysVal,
                onCheckedChange = { sysVal = it },
                colors = CheckboxDefaults.colors(checkedColor = FieldTheme.colors.purple600)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "System Integrity Alerts",
                    style = FieldTheme.typography.bodyStrong,
                    color = FieldTheme.colors.gray100
                )
                Text(
                    text = "Integrity violations or server health flags",
                    style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                    color = FieldTheme.colors.gray500
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton(
                text = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            PrimaryButton(
                text = "Save Preferences",
                onClick = { onSave(appVal, docVal, sysVal) },
                modifier = Modifier.weight(1.5f)
            )
        }
    }
}
