package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.viewmodel.ApplicationUiState
import com.fieldcrm.android.ui.viewmodel.ApplicationViewModel
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel

@Composable
fun CreateApplicationScreenView(
    viewModel: ApplicationViewModel,
    borrowers: List<BorrowerModel>,
    onApplicationCreated: (LoanApplicationModel) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    CreateApplicationContent(
        isLoading = state.isLoading,
        errorMessage = state.errorMessage,
        newAppAmount = state.newAppAmount,
        newAppTenure = state.newAppTenure,
        newAppInterestRate = state.newAppInterestRate,
        selectedBorrower = state.selectedBorrowerForApp,
        borrowers = borrowers,
        onAmountChange = { viewModel.setNewAppAmount(it) },
        onTenureChange = { viewModel.setNewAppTenure(it) },
        onInterestChange = { viewModel.setNewAppInterestRate(it) },
        onBorrowerSelected = { viewModel.setSelectedBorrowerForApp(it) },
        onCreateClick = {
            viewModel.createApplication { newApp ->
                onApplicationCreated(newApp)
            }
        },
        onBackClick = onBackClick
    )
}

@Composable
fun CreateApplicationContent(
    isLoading: Boolean,
    errorMessage: String?,
    newAppAmount: String,
    newAppTenure: String,
    newAppInterestRate: String,
    selectedBorrower: BorrowerModel?,
    borrowers: List<BorrowerModel>,
    onAmountChange: (String) -> Unit,
    onTenureChange: (String) -> Unit,
    onInterestChange: (String) -> Unit,
    onBorrowerSelected: (BorrowerModel) -> Unit,
    onCreateClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        topBar = {
            FieldTopAppBar(
                title = "New Lending intake",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                            text = "Intake Form",
                            style = FieldTheme.typography.title,
                            color = FieldTheme.colors.gray100
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "START NEW LOAN VERIFICATION SEQUENCE",
                            style = FieldTheme.typography.label.copy(color = FieldTheme.colors.purple400)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Select Borrower Dropdown
                        val borrowerNames = borrowers.map { it.name }
                        val selectedName = selectedBorrower?.name ?: ""
                        
                        FieldDropdown(
                            value = selectedName,
                            options = borrowerNames,
                            onOptionSelected = { name ->
                                borrowers.find { it.name == name }?.let { onBorrowerSelected(it) }
                            },
                            label = "Select Borrower Profile",
                            isRequired = true
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Loan Amount (₦ currency specific field)
                        FieldAmountField(
                            value = newAppAmount,
                            onValueChange = onAmountChange,
                            label = "Principal Amount",
                            isRequired = true
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                FieldTextField(
                                    value = newAppTenure,
                                    onValueChange = onTenureChange,
                                    label = "Tenure",
                                    placeholder = "Months",
                                    isRequired = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                FieldTextField(
                                    value = newAppInterestRate,
                                    onValueChange = onInterestChange,
                                    label = "Interest Rate",
                                    placeholder = "% / year",
                                    isRequired = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                )
                            }
                        }
                        
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
                            text = if (isLoading) "Saving Intake..." else "Create Application",
                            onClick = onCreateClick,
                            enabled = !isLoading && selectedBorrower != null && newAppAmount.isNotEmpty() && newAppTenure.isNotEmpty()
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
fun PreviewCreateAppCompact() {
    val demoBorrowers = listOf(
        BorrowerModel(
            id = "1", org_id = "org_1", loan_officer_id = "LO_1",
            name = "Adaeze Okonkwo", phone = "08012345678", bvn = "222333444", nin = "111222333",
            status = "Active", created_at = "2026-06-18"
        ),
        BorrowerModel(
            id = "2", org_id = "org_1", loan_officer_id = "LO_1",
            name = "Emeka Chukwu", phone = "08087654321", bvn = "555666777", nin = "999888777",
            status = "Active", created_at = "2026-06-18"
        )
    )

    FieldCRMTheme {
        CreateApplicationContent(
            isLoading = false,
            errorMessage = null,
            newAppAmount = "250000",
            newAppTenure = "6",
            newAppInterestRate = "15.0",
            selectedBorrower = demoBorrowers[0],
            borrowers = demoBorrowers,
            onAmountChange = {},
            onTenureChange = {},
            onInterestChange = {},
            onBorrowerSelected = {},
            onCreateClick = {},
            onBackClick = {}
        )
    }
}
