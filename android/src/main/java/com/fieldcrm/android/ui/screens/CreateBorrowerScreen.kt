package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material3.*
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
                title = "New Borrower Intake",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = FieldTheme.colors.gray400
                        )
                    }
                }
            )
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            val isWide = maxWidth >= 600.dp
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                ) {
                    FieldCard {
                        Text(
                            text = "Identity Details",
                            style = FieldTheme.typography.title,
                            color = FieldTheme.colors.gray100
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "REGISTER APPLICANT PROFILE IN OFFLINE SYNC DATABASE",
                            style = FieldTheme.typography.label.copy(color = FieldTheme.colors.purple400)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        FieldTextField(
                            value = newBorrowerName,
                            onValueChange = onNameChange,
                            label = "Full Name",
                            placeholder = "Adaeze Okonkwo",
                            isRequired = true,
                            enabled = !isLoading,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = "Name",
                                    tint = FieldTheme.colors.gray500
                                )
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        FieldTextField(
                            value = newBorrowerPhone,
                            onValueChange = onPhoneChange,
                            label = "Primary Phone",
                            placeholder = "e.g. +234 80...",
                            isRequired = true,
                            enabled = !isLoading,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Phone,
                                    contentDescription = "Phone",
                                    tint = FieldTheme.colors.gray500
                                )
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        FieldTextField(
                            value = newBorrowerBvn,
                            onValueChange = onBvnChange,
                            label = "Bank Verification Number (BVN)",
                            placeholder = "11-digit numeric code",
                            isRequired = true,
                            enabled = !isLoading,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Fingerprint,
                                    contentDescription = "BVN",
                                    tint = FieldTheme.colors.gray500
                                )
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        FieldTextField(
                            value = newBorrowerNin,
                            onValueChange = onNinChange,
                            label = "National Identification Number (NIN)",
                            placeholder = "11-digit numeric code",
                            isRequired = true,
                            enabled = !isLoading,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Badge,
                                    contentDescription = "NIN",
                                    tint = FieldTheme.colors.gray500
                                )
                            }
                        )
                        
                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = errorMessage,
                                style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                color = FieldTheme.colors.statusDanger
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        PrimaryButton(
                            text = if (isLoading) "Registering Profile..." else "Create Borrower Profile",
                            onClick = onCreateClick,
                            enabled = !isLoading && newBorrowerName.isNotEmpty() && newBorrowerPhone.isNotEmpty() && newBorrowerBvn.isNotEmpty()
                        )
                    }
                }
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
