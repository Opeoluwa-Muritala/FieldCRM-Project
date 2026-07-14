package com.fieldcrm.android.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.screens.auth.PasskeyUnavailableCard
import com.fieldcrm.android.ui.screens.common.DetailItem
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.viewmodel.AppViewModel
import com.fieldcrm.android.ui.viewmodel.ConfigViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userName: String = "Chidi Okafor",
    userEmail: String = "chidi@mmfb.com",
    role: UserRole? = UserRole.LOAN_OFFICER,
    onBackClick: () -> Unit,
    onNavigateToOfflineQueue: () -> Unit = {},
    onSignOutClick: () -> Unit = {}
) {
    val configViewModel: ConfigViewModel = koinViewModel()
    val configState by configViewModel.uiState.collectAsState()
    val config = configState.config

    val appViewModel: AppViewModel = koinViewModel()
    val appUiState by appViewModel.uiState.collectAsState()

    var faceIdEnabled by remember { mutableStateOf(true) }
    var pushEnabled by remember { mutableStateOf(true) }
    val darkModeEnabled = appUiState.isDarkMode
    var showSignOutConfirmation by remember { mutableStateOf(false) }

    // Active modal overlay: "PASSWORD", "PHONE", "HELP", "IT", "REPORT"
    var activeModal by remember { mutableStateOf<String?>(null) }

    var currentPhoneNumber by remember { mutableStateOf("+234 801 234 5678") }

    if (showSignOutConfirmation) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FieldTheme.colors.gray950),
            contentAlignment = Alignment.Center
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
                    FieldCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Sign Out",
                                style = FieldTheme.typography.display.copy(fontSize = 20.sp),
                                color = FieldTheme.colors.gray100,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Are you sure you want to sign out? Any unsynced data will be saved and sent when you sign back in.",
                                style = FieldTheme.typography.body,
                                color = FieldTheme.colors.gray400,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(32.dp))

                            PrimaryButton(
                                text = "Sign Out",
                                onClick = onSignOutClick,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { showSignOutConfirmation = false },
                                shape = RoundedCornerShape(FieldTheme.shapes.inputRadius),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = FieldTheme.colors.gray400
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Text(
                                    text = "Cancel",
                                    style = FieldTheme.typography.bodyStrong,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    } else if (activeModal != null) {
        // Modal Overlays
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
                    when (activeModal) {
                        "PASSWORD" -> ChangePasswordModal(
                            onDismiss = { activeModal = null }
                        )
                        "PHONE" -> UpdatePhoneModal(
                            currentPhone = currentPhoneNumber,
                            onSave = {
                                currentPhoneNumber = it
                                activeModal = null
                            },
                            onDismiss = { activeModal = null }
                        )
                        "PASSKEYS" -> PasskeyUnavailableCard(
                            onDismiss = { activeModal = null }
                        )
                        "HELP" -> HelpCenterModal(
                            onDismiss = { activeModal = null },
                            supportEmail = config?.support_email
                        )
                        "IT" -> ITSupportModal(
                            onDismiss = { activeModal = null },
                            supportPhone = config?.support_phone,
                            supportEmail = config?.support_email,
                            nodeId = config?.node_id
                        )
                        "REPORT" -> ReportProblemModal(
                            onDismiss = { activeModal = null },
                            prefillName = userName,
                            prefillEmail = userEmail,
                            orgName = config?.org_name ?: "FieldCRM MFB"
                        )
                    }
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                FieldTopAppBar(
                    title = "Profile & Settings",
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = FieldIcons.ArrowBackOutlined,
                                contentDescription = "Back",
                                tint = FieldTheme.colors.gray400
                            )
                        }
                    }
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // User Info Card
                            FieldCard {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val initials = userName.split(" ")
                                        .mapNotNull { it.firstOrNull()?.toString() }
                                        .joinToString("")
                                        .uppercase()

                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .background(FieldTheme.colors.purple900.copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initials,
                                            style = FieldTheme.typography.title.copy(fontSize = 18.sp),
                                            color = FieldTheme.colors.purple400,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = userName,
                                            style = FieldTheme.typography.title,
                                            color = FieldTheme.colors.gray100
                                        )
                                        Text(
                                            text = if (userEmail.isNotBlank()) userEmail else role?.displayName ?: "Loan Officer",
                                            style = FieldTheme.typography.body.copy(fontSize = 13.sp),
                                            color = FieldTheme.colors.gray400
                                        )
                                    }
                                }
                            }
                            
                            // Account Section
                            FieldCard {
                                Text(
                                    text = "ACCOUNT",
                                    style = FieldTheme.typography.label,
                                    color = FieldTheme.colors.gray500
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                SettingsRow(label = "Change Password", leadingIcon = FieldIcons.LockOutlined) {
                                    activeModal = "PASSWORD"
                                }
                                FieldDivider()
                                SettingsRow(label = "Update Phone Number", leadingIcon = FieldIcons.PhoneOutlined) {
                                    activeModal = "PHONE"
                                }
                                FieldDivider()
                                SettingsRow(label = "Manage Passkeys", leadingIcon = FieldIcons.LockOutlined) {
                                    activeModal = "PASSKEYS"
                                }
                                FieldDivider()
                                SettingsRow(label = "Offline Sync Queue", leadingIcon = FieldIcons.PaymentsOutlined) {
                                    onNavigateToOfflineQueue()
                                }
                            }

                            // Preferences Section
                            FieldCard {
                                Text(
                                    text = "PREFERENCES",
                                    style = FieldTheme.typography.label,
                                    color = FieldTheme.colors.gray500
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                SettingsToggleRow(
                                    label = "Enable Face ID",
                                    leadingIcon = FieldIcons.FingerprintOutlined,
                                    checked = faceIdEnabled,
                                    onCheckedChange = { faceIdEnabled = it }
                                )
                                FieldDivider()
                                SettingsToggleRow(
                                    label = "Push Notifications",
                                    leadingIcon = FieldIcons.BellFilled,
                                    checked = pushEnabled,
                                    onCheckedChange = { pushEnabled = it }
                                )
                                FieldDivider()
                                SettingsToggleRow(
                                    label = "Dark Mode",
                                    leadingIcon = FieldIcons.SettingsOutlined,
                                    checked = darkModeEnabled,
                                    onCheckedChange = { appViewModel.setDarkMode(it) }
                                )
                            }

                            // Support Section
                            FieldCard {
                                Text(
                                    text = "SUPPORT",
                                    style = FieldTheme.typography.label,
                                    color = FieldTheme.colors.gray500
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                SettingsRow(label = "Help Center FAQs", leadingIcon = FieldIcons.InfoOutlined) {
                                    activeModal = "HELP"
                                }
                                FieldDivider()
                                SettingsRow(label = "Contact IT Support Helpline", leadingIcon = FieldIcons.PersonOutlined) {
                                    activeModal = "IT"
                                }
                                FieldDivider()
                                SettingsRow(label = "Report a Technical Problem", leadingIcon = FieldIcons.AlertOutlined) {
                                    activeModal = "REPORT"
                                }
                            }

                            // Version Label
                            Text(
                                text = "App Version 2.4.1",
                                style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                color = FieldTheme.colors.gray500,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                textAlign = TextAlign.Center
                            )

                            // Sign Out Button
                            Button(
                                onClick = { showSignOutConfirmation = true },
                                shape = RoundedCornerShape(FieldTheme.shapes.inputRadius),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = FieldTheme.colors.statusDanger
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, FieldTheme.colors.statusDanger.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "Sign Out",
                                    style = FieldTheme.typography.bodyStrong,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Change Password Modal
@Composable
fun ChangePasswordModal(onDismiss: () -> Unit) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }

    FieldCard {
        if (isSuccess) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(56.dp).background(FieldTheme.colors.purple900.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FieldIcons.CheckOutlined,
                        contentDescription = "Success",
                        tint = FieldTheme.colors.purple400,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Password Updated", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Your staff profile security configuration has been updated successfully.", style = FieldTheme.typography.body, color = FieldTheme.colors.gray400, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                PrimaryButton(text = "Done", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        } else {
            Text("Change Password", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
            Spacer(modifier = Modifier.height(24.dp))
            FieldTextField(value = oldPassword, onValueChange = { oldPassword = it }, label = "Current Password", isRequired = true)
            Spacer(modifier = Modifier.height(12.dp))
            FieldTextField(value = newPassword, onValueChange = { newPassword = it }, label = "New Password", isRequired = true)
            Spacer(modifier = Modifier.height(12.dp))
            FieldTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Confirm New Password", isRequired = true)
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(text = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                PrimaryButton(
                    text = "Update Password",
                    onClick = { isSuccess = true },
                    enabled = oldPassword.isNotEmpty() && newPassword.isNotEmpty() && newPassword == confirmPassword,
                    modifier = Modifier.weight(1.5f)
                )
            }
        }
    }
}

// Update Phone Modal
@Composable
fun UpdatePhoneModal(currentPhone: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var newPhone by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(1) } // 1: Input, 2: Verification

    FieldCard {
        if (step == 1) {
            Text("Update Phone Number", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Current: $currentPhone", style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)
            Spacer(modifier = Modifier.height(24.dp))
            FieldTextField(value = newPhone, onValueChange = { newPhone = it }, label = "New Phone Number", isRequired = true)
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(text = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                PrimaryButton(
                    text = "Request Verification Code",
                    onClick = { step = 2 },
                    enabled = newPhone.isNotEmpty(),
                    modifier = Modifier.weight(1.5f)
                )
            }
        } else {
            Text("Verify OTP Code", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
            Spacer(modifier = Modifier.height(8.dp))
            Text("A 4-digit verification token was sent to $newPhone.", style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)
            Spacer(modifier = Modifier.height(24.dp))
            FieldTextField(value = otpCode, onValueChange = { otpCode = it }, label = "Verification Token", isRequired = true)
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(text = "Back", onClick = { step = 1 }, modifier = Modifier.weight(1f))
                PrimaryButton(
                    text = "Verify & Save",
                    onClick = { onSave(newPhone) },
                    enabled = otpCode.length >= 4,
                    modifier = Modifier.weight(1.5f)
                )
            }
        }
    }
}

// Help Center Modal
@Composable
fun HelpCenterModal(onDismiss: () -> Unit, supportEmail: String? = null) {
    var expandedIndex by remember { mutableIntStateOf(-1) }
    val faqs = listOf(
        Pair("How does the camera OCR parser work?", "Align the NIN/BVN identity document inside the viewfinder scanner box. Click Scan & Extract; standard ML Kit extracts and matches text values locally without remote delays."),
        Pair("GPS Coordinates lock timeout?", "Ensure location sensors are enabled on your device. Click the refresh location button to trigger an active ACCESS_FINE_LOCATION provider query."),
        Pair("Managing the Offline Sync Queue?", "When working offline, completed dossiers are queued locally. Tap the Sync button on the main tab once network coverage is restored to upload cached records."),
        Pair("What is the DTI limit?", "The Debt-to-Income ratio limit is 40%. Applications above this threshold require additional review before disbursement."),
        Pair("How do I escalate a compliance flag?", "Navigate to the application audit trail, review workflow events, and use the Report Problem option to escalate to the compliance officer.")
    )

    FieldCard {
        Text("Help Center FAQs", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            faqs.forEachIndexed { index, item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, FieldTheme.colors.gray800, RoundedCornerShape(8.dp))
                        .clickable { expandedIndex = if (expandedIndex == index) -1 else index }
                        .padding(12.dp)
                ) {
                    Text(item.first, style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.purple400)
                    if (expandedIndex == index) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(item.second, style = FieldTheme.typography.body.copy(fontSize = 13.sp), color = FieldTheme.colors.gray300)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        PrimaryButton(text = "Close FAQ Portal", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
    }
}

// IT Support Modal
@Composable
fun ITSupportModal(
    onDismiss: () -> Unit,
    supportPhone: String? = null,
    supportEmail: String? = null,
    nodeId: String? = null
) {
    FieldCard {
        Text("Contact IT Support Helpdesk", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
        Spacer(modifier = Modifier.height(8.dp))
        Text("If you encounter hardware errors or authentication lockouts, contact the microfinance systems support desk.", style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)

        Spacer(modifier = Modifier.height(24.dp))
        DetailItem(label = "Direct Call Support Desk", value = supportPhone ?: "+234 1 234 5678")
        DetailItem(label = "Email System Administrator", value = supportEmail ?: "helpdesk@mainstreetmfb.com")
        if (nodeId != null) {
            DetailItem(label = "System Node", value = nodeId)
        }

        Spacer(modifier = Modifier.height(24.dp))
        PrimaryButton(text = "Close", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
    }
}

// Report Problem Modal
@Composable
fun ReportProblemModal(
    onDismiss: () -> Unit,
    prefillName: String = "",
    prefillEmail: String = "",
    orgName: String = "FieldCRM"
) {
    var name by remember { mutableStateOf(prefillName) }
    var email by remember { mutableStateOf(prefillEmail) }
    var accountNum by remember { mutableStateOf("") }
    var errorType by remember { mutableStateOf("bank_one_loading") }
    var description by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }
    var isSubmitted by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isFormValid = name.isNotEmpty() && email.contains("@") && accountNum.length == 10 && description.isNotEmpty()

    FieldCard {
        if (isSubmitted) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(56.dp).background(FieldTheme.colors.purple900.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FieldIcons.CheckOutlined,
                        contentDescription = "Success",
                        tint = FieldTheme.colors.purple400,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Dossier Submitted", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Your support ticket has been sent to $orgName IT queue successfully.", style = FieldTheme.typography.body, color = FieldTheme.colors.gray400, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                PrimaryButton(text = "Done", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        } else {
            Text("Report a Technical Issue", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Submits native support ticket directly to MFB API", style = FieldTheme.typography.body.copy(fontSize = 11.sp), color = FieldTheme.colors.gray500)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = FieldTheme.colors.statusDanger,
                    style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            FieldTextField(
                value = name,
                onValueChange = { name = it },
                label = "Full Name",
                isRequired = true,
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(12.dp))
            FieldTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email Address",
                isRequired = true,
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(12.dp))
            FieldTextField(
                value = accountNum,
                onValueChange = { accountNum = it.take(10).replace(Regex("[^0-9]"), "") },
                label = "10-Digit Account Number",
                isRequired = true,
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Error Type Dropdown Selection
            Text(
                text = "ERROR CATEGORY",
                style = FieldTheme.typography.label,
                color = FieldTheme.colors.gray500,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            var expandedDropdown by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                val displayLabel = when(errorType) {
                    "payment_failed" -> "Payment Failed"
                    "wrong_deduction" -> "Wrong Deduction"
                    "not_credited" -> "Not Credited"
                    "bank_one_loading" -> "BankOne Issue"
                    else -> "Other"
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(FieldTheme.colors.gray900, RoundedCornerShape(FieldTheme.shapes.inputRadius))
                        .border(1.dp, FieldTheme.colors.gray800, RoundedCornerShape(FieldTheme.shapes.inputRadius))
                        .clickable(enabled = !isLoading) { expandedDropdown = true }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(displayLabel, style = FieldTheme.typography.body, color = FieldTheme.colors.gray300)
                }
                DropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false },
                    modifier = Modifier.background(FieldTheme.colors.gray900).border(1.dp, FieldTheme.colors.gray800)
                ) {
                    DropdownMenuItem(
                        text = { Text("Payment Failed", color = Color.White) },
                        onClick = { errorType = "payment_failed"; expandedDropdown = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Wrong Deduction", color = Color.White) },
                        onClick = { errorType = "wrong_deduction"; expandedDropdown = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Not Credited", color = Color.White) },
                        onClick = { errorType = "not_credited"; expandedDropdown = false }
                    )
                    DropdownMenuItem(
                        text = { Text("BankOne Issue", color = Color.White) },
                        onClick = { errorType = "bank_one_loading"; expandedDropdown = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Other", color = Color.White) },
                        onClick = { errorType = "other"; expandedDropdown = false }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            FieldTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description of Problem",
                isRequired = true,
                enabled = !isLoading
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(text = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f), enabled = !isLoading)
                PrimaryButton(
                    text = if (isLoading) "Submitting..." else "Submit Ticket",
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        submitSupportTicket(
                            name = name,
                            email = email,
                            account = accountNum,
                            errorType = errorType,
                            description = description,
                            onResult = { success, msg ->
                                isLoading = false
                                if (success) {
                                    isSubmitted = true
                                } else {
                                    errorMessage = msg
                                }
                            }
                        )
                    },
                    enabled = isFormValid && !isLoading,
                    modifier = Modifier.weight(1.5f)
                )
            }
        }
    }
}

private fun submitSupportTicket(
    name: String,
    email: String,
    account: String,
    errorType: String,
    description: String,
    onResult: (Boolean, String) -> Unit
) {
    Thread {
        try {
            // Step 1: GET to retrieve session cookie and CSRF token
            val getUrl = java.net.URL("https://ticket-api-rho.vercel.app/")
            val getConn = getUrl.openConnection() as java.net.HttpURLConnection
            getConn.requestMethod = "GET"
            getConn.connect()

            val cookies = getConn.headerFields["Set-Cookie"] ?: getConn.headerFields["set-cookie"]
            val cookieString = cookies?.joinToString("; ") { it.substringBefore(";") }

            val reader = java.io.BufferedReader(java.io.InputStreamReader(getConn.inputStream))
            val html = reader.readText()
            reader.close()

            val csrfTokenPattern = """id="csrf_token"\s+name="csrf_token"\s+type="hidden"\s+value="([^"]+)"""".toRegex()
            val match = csrfTokenPattern.find(html)
            val csrfToken = match?.groupValues?.get(1) ?: ""

            // Step 2: POST Multipart Form Data
            val postUrl = java.net.URL("https://ticket-api-rho.vercel.app/")
            val postConn = postUrl.openConnection() as java.net.HttpURLConnection
            postConn.requestMethod = "POST"
            postConn.doOutput = true
            postConn.doInput = true

            val boundary = "===Boundary" + System.currentTimeMillis() + "==="
            postConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            if (!cookieString.isNullOrEmpty()) {
                postConn.setRequestProperty("Cookie", cookieString)
            }

            val outputStream = postConn.outputStream
            val writer = java.io.PrintWriter(outputStream.writer(), true)

            fun writeFormField(fieldName: String, value: String) {
                writer.append("--$boundary").append("\r\n")
                writer.append("Content-Disposition: form-data; name=\"$fieldName\"").append("\r\n\r\n")
                writer.append(value).append("\r\n")
            }

            writeFormField("csrf_token", csrfToken)
            writeFormField("name", name)
            writeFormField("email", email)
            writeFormField("account", account)
            writeFormField("reference", "FieldCRM App Android Ticket")
            writeFormField("error_type", errorType)
            writeFormField("description", description)

            // Optional empty screenshot file
            writer.append("--$boundary").append("\r\n")
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"\"").append("\r\n")
            writer.append("Content-Type: application/octet-stream").append("\r\n\r\n")
            writer.append("\r\n")

            writer.append("--$boundary--").append("\r\n")
            writer.flush()
            writer.close()

            val responseCode = postConn.responseCode
            if (responseCode in 200..399) {
                onResult(true, "Ticket submitted successfully")
            } else {
                val errorStream = postConn.errorStream ?: postConn.inputStream
                val errText = errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                onResult(false, "Failed: $responseCode - $errText")
            }
        } catch (e: Exception) {
            onResult(false, "Network error: ${e.localizedMessage}")
        }
    }.start()
}

@Composable
fun SettingsRow(
    label: String,
    leadingIcon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = label,
            tint = FieldTheme.colors.purple400,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = FieldTheme.typography.body,
            color = FieldTheme.colors.gray100,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = FieldIcons.ChevronRightOutlined,
            contentDescription = "Go",
            tint = FieldTheme.colors.gray500,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SettingsToggleRow(
    label: String,
    leadingIcon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = label,
            tint = FieldTheme.colors.purple400,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = FieldTheme.typography.body,
            color = FieldTheme.colors.gray100,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = FieldTheme.colors.purple600,
                uncheckedThumbColor = FieldTheme.colors.gray400,
                uncheckedTrackColor = FieldTheme.colors.gray800
            )
        )
    }
}
