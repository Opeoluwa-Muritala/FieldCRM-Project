package com.fieldcrm.android.ui.screens.application

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
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
    onApplicationCreated: (LoanApplicationModel, BorrowerModel) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    CreateApplicationContent(
        isLoading = state.isLoading,
        errorMessage = state.errorMessage,
        customerType = state.customerType,
        loanCategory = state.loanCategory,
        newCustomerName = state.newCustomerName,
        newCustomerPhone = state.newCustomerPhone,
        newCustomerBvn = state.newCustomerBvn,
        newCustomerNin = state.newCustomerNin,
        selectedBorrower = state.selectedBorrowerForApp,
        borrowers = borrowers,
        onCustomerTypeChange = { viewModel.setCustomerType(it) },
        onLoanCategoryChange = { viewModel.setLoanCategory(it) },
        onNewCustomerNameChange = { viewModel.setNewCustomerName(it) },
        onNewCustomerPhoneChange = { viewModel.setNewCustomerPhone(it) },
        onNewCustomerBvnChange = { viewModel.setNewCustomerBvn(it) },
        onNewCustomerNinChange = { viewModel.setNewCustomerNin(it) },
        onBorrowerSelected = { viewModel.setSelectedBorrowerForApp(it) },
        onCreateClick = {
            viewModel.createApplication { newApp, borrower ->
                onApplicationCreated(newApp, borrower)
            }
        },
        onBackClick = onBackClick
    )
}

@Composable
fun CreateApplicationContent(
    isLoading: Boolean,
    errorMessage: String?,
    customerType: String,
    loanCategory: String,
    newCustomerName: String,
    newCustomerPhone: String,
    newCustomerBvn: String,
    newCustomerNin: String,
    selectedBorrower: BorrowerModel?,
    borrowers: List<BorrowerModel>,
    onCustomerTypeChange: (String) -> Unit,
    onLoanCategoryChange: (String) -> Unit,
    onNewCustomerNameChange: (String) -> Unit,
    onNewCustomerPhoneChange: (String) -> Unit,
    onNewCustomerBvnChange: (String) -> Unit,
    onNewCustomerNinChange: (String) -> Unit,
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
                title = "New Lending Intake",
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
        BoxWithConstraints(
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
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                ) {
                    FieldCard {
                        Text(
                            text = "New Application",
                            style = FieldTheme.typography.title,
                            color = FieldTheme.colors.gray100
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "START NEW LOAN VERIFICATION SEQUENCE",
                            style = FieldTheme.typography.label.copy(color = FieldTheme.colors.purple400)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Select Customer Type TabRow
                        val activeTab = if (customerType == "New Customer") 0 else 1
                        TabRow(
                            selectedTabIndex = activeTab,
                            containerColor = FieldTheme.colors.gray900,
                            contentColor = FieldTheme.colors.purple400,
                            modifier = Modifier.clip(RoundedCornerShape(4.dp))
                        ) {
                            listOf("New Customer", "Existing Customer").forEachIndexed { i, label ->
                                Tab(
                                    selected = activeTab == i,
                                    onClick = {
                                        onCustomerTypeChange(label)
                                    },
                                    text = {
                                        Text(
                                            text = label,
                                            color = if (activeTab == i) FieldTheme.colors.purple400 else FieldTheme.colors.gray400,
                                            style = FieldTheme.typography.bodyStrong.copy(fontSize = 14.sp)
                                        )
                                    }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Select Loan Category Dropdown
                        FieldDropdown(
                            value = loanCategory,
                            options = listOf("Enterprise Loan", "MSEF", "PAYEE", "Other Option"),
                            onOptionSelected = onLoanCategoryChange,
                            label = "Select Loan Category",
                            isRequired = true
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (customerType == "Existing Customer") {
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
                        } else {
                            // New Customer Fields
                            Text(
                                text = "NEW BORROWER REGISTRATION",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray500
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            FieldTextField(
                                value = newCustomerName,
                                onValueChange = onNewCustomerNameChange,
                                label = "Full Name",
                                placeholder = "Adaeze Okonkwo",
                                isRequired = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = FieldIcons.PersonOutlined,
                                        contentDescription = "Name",
                                        tint = FieldTheme.colors.gray500
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FieldTextField(
                                value = newCustomerPhone,
                                onValueChange = onNewCustomerPhoneChange,
                                label = "Primary Phone",
                                placeholder = "e.g. +234 80...",
                                isRequired = true,
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                                leadingIcon = {
                                    Icon(
                                        imageVector = FieldIcons.PhoneOutlined,
                                        contentDescription = "Phone",
                                        tint = FieldTheme.colors.gray500
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FieldTextField(
                                value = newCustomerBvn,
                                onValueChange = onNewCustomerBvnChange,
                                label = "Bank Verification Number (BVN)",
                                placeholder = "11-digit numeric code",
                                isRequired = true,
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                leadingIcon = {
                                    Icon(
                                        imageVector = FieldIcons.FingerprintOutlined,
                                        contentDescription = "BVN",
                                        tint = FieldTheme.colors.gray500
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FieldTextField(
                                value = newCustomerNin,
                                onValueChange = onNewCustomerNinChange,
                                label = "National Identification Number (NIN)",
                                placeholder = "11-digit numeric code",
                                isRequired = true,
                                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                leadingIcon = {
                                    Icon(
                                        imageVector = FieldIcons.BadgeOutlined,
                                        contentDescription = "NIN",
                                        tint = FieldTheme.colors.gray500
                                    )
                                }
                            )
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
                        
                        val isFormValid = if (customerType == "New Customer") {
                            newCustomerName.isNotEmpty() && newCustomerPhone.isNotEmpty() && newCustomerBvn.isNotEmpty() && newCustomerNin.isNotEmpty()
                        } else {
                            selectedBorrower != null
                        }
                        
                        PrimaryButton(
                            text = if (isLoading) "Creating Draft..." else "Begin Application",
                            onClick = onCreateClick,
                            enabled = !isLoading && isFormValid
                        )
                    }
                }
            }
        }
    }
}

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
            customerType = "Existing Customer",
            loanCategory = "Enterprise Loan",
            newCustomerName = "",
            newCustomerPhone = "",
            newCustomerBvn = "",
            newCustomerNin = "",
            selectedBorrower = demoBorrowers[0],
            borrowers = demoBorrowers,
            onCustomerTypeChange = {},
            onLoanCategoryChange = {},
            onNewCustomerNameChange = {},
            onNewCustomerPhoneChange = {},
            onNewCustomerBvnChange = {},
            onNewCustomerNinChange = {},
            onBorrowerSelected = {},
            onCreateClick = {},
            onBackClick = {}
        )
    }
}
