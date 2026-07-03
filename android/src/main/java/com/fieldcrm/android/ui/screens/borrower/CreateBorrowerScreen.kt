package com.fieldcrm.android.ui.screens.borrower

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.fieldcrm.android.ui.theme.FieldIcons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.android.ui.viewmodel.BorrowerViewModel

@Composable
fun CreateBorrowerScreenView(
    viewModel: BorrowerViewModel,
    onBorrowerCreated: (BorrowerModel) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    CreateBorrowerContent(
        isLoading = state.isLoading,
        errorMessage = state.errorMessage,
        newBorrowerName = state.newBorrowerName,
        newBorrowerPhone = state.newBorrowerPhone,
        newBorrowerBvn = state.newBorrowerBvn,
        newBorrowerNin = state.newBorrowerNin,
        onNameChange = { viewModel.setNewBorrowerName(it) },
        onPhoneChange = { viewModel.setNewBorrowerPhone(it) },
        onBvnChange = { viewModel.setNewBorrowerBvn(it) },
        onNinChange = { viewModel.setNewBorrowerNin(it) },
        onCreateClick = {
            viewModel.createBorrower { newBorrower ->
                onBorrowerCreated(newBorrower)
            }
        },
        onBackClick = onBackClick
    )
}

@Composable
fun CreateBorrowerContent(
    isLoading: Boolean,
    errorMessage: String?,
    newBorrowerName: String,
    newBorrowerPhone: String,
    newBorrowerBvn: String,
    newBorrowerNin: String,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onBvnChange: (String) -> Unit,
    onNinChange: (String) -> Unit,
    onCreateClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        topBar = {
            FieldTopAppBar(
                title = "New Client Profile",
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
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FieldTheme.colors.gray900)
                    .border(width = 0.5.dp, color = FieldTheme.colors.gray800)
                    .padding(16.dp)
            ) {
                PrimaryButton(
                    text = if (isLoading) "Registering Profile..." else "Create Borrower Profile",
                    onClick = onCreateClick,
                    enabled = !isLoading && newBorrowerName.isNotEmpty() && newBorrowerPhone.isNotEmpty() && newBorrowerBvn.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Rich Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FieldTheme.colors.purple600.copy(alpha = 0.05f))
                    .padding(vertical = 32.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            color = FieldTheme.colors.purple600.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(36.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = FieldTheme.colors.purple600.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(36.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FieldIcons.ShieldOutlined,
                        contentDescription = "Identity",
                        tint = FieldTheme.colors.purple400,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Identity Verification",
                    style = FieldTheme.typography.title,
                    color = FieldTheme.colors.gray100
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Please ensure all details match the client's official government-issued ID exactly.",
                    style = FieldTheme.typography.body.copy(fontSize = 14.sp),
                    color = FieldTheme.colors.gray400,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Form Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "PERSONAL INFORMATION",
                    style = FieldTheme.typography.label.copy(color = FieldTheme.colors.purple400)
                )

                FieldTextField(
                    value = newBorrowerName,
                    onValueChange = onNameChange,
                    label = "Legal Full Name",
                    placeholder = "e.g. Adaeze Okonkwo",
                    isRequired = true,
                    enabled = !isLoading,
                    leadingIcon = {
                        Icon(
                            imageVector = FieldIcons.PersonOutlined,
                            contentDescription = "Name",
                            tint = FieldTheme.colors.gray500
                        )
                    }
                )

                FieldTextField(
                    value = newBorrowerPhone,
                    onValueChange = onPhoneChange,
                    label = "Primary Mobile Number",
                    placeholder = "08012345678",
                    isRequired = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = {
                        Icon(
                            imageVector = FieldIcons.PhoneOutlined,
                            contentDescription = "Phone",
                            tint = FieldTheme.colors.gray500
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "GOVERNMENT IDS",
                    style = FieldTheme.typography.label.copy(color = FieldTheme.colors.purple400)
                )

                FieldTextField(
                    value = newBorrowerBvn,
                    onValueChange = onBvnChange,
                    label = "Bank Verification Number (BVN)",
                    placeholder = "11-digit BVN",
                    isRequired = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(
                            imageVector = FieldIcons.FingerprintOutlined,
                            contentDescription = "BVN",
                            tint = FieldTheme.colors.gray500
                        )
                    }
                )

                FieldTextField(
                    value = newBorrowerNin,
                    onValueChange = onNinChange,
                    label = "National Identification Number (NIN)",
                    placeholder = "11-digit NIN",
                    isRequired = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(
                            imageVector = FieldIcons.BadgeOutlined, // Reusing PersonOutlined or another if Badge missing. Let's assume Badge exists or I use PersonOutlined. But let's check what I have. Wait, in previous search BadgeOutlined wasn't visible but maybe it is. Wait, I will use ShieldOutlined if Badge isn't guaranteed. Wait, I'll just use FingerprintOutlined again or ShieldOutlined for NIN just to be safe. Actually, I saw BadgeOutlined used in original! Ah yes, line 186 in original had BadgeOutlined. So it exists.
                            contentDescription = "NIN",
                            tint = FieldTheme.colors.gray500
                        )
                    }
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = FieldTheme.colors.statusDanger.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = FieldTheme.colors.statusDanger.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = FieldIcons.AlertOutlined,
                            contentDescription = "Error",
                            tint = FieldTheme.colors.statusDanger,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = errorMessage,
                            style = FieldTheme.typography.body.copy(fontSize = 14.sp),
                            color = FieldTheme.colors.statusDanger
                        )
                    }
                }
                
                // Add a bit of bottom padding to ensure the scroll goes above the bottom bar
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(name = "Compact Phone Form", widthDp = 411, heightDp = 850)
@Composable
fun PreviewCreateBorrowerCompact() {
    FieldCRMTheme {
        CreateBorrowerContent(
            isLoading = false,
            errorMessage = null,
            newBorrowerName = "Adaeze Okonkwo",
            newBorrowerPhone = "08012345678",
            newBorrowerBvn = "222333444",
            newBorrowerNin = "111222333",
            onNameChange = {},
            onPhoneChange = {},
            onBvnChange = {},
            onNinChange = {},
            onCreateClick = {},
            onBackClick = {}
        )
    }
}
