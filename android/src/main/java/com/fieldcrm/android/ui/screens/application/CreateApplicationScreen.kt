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
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

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
        shareUrl = state.shareUrl,
        isGeneratingLink = state.isGeneratingLink,
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
        onGenerateLinkClick = { viewModel.generateClientIntakeLink() },
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
    shareUrl: String?,
    isGeneratingLink: Boolean,
    onCustomerTypeChange: (String) -> Unit,
    onLoanCategoryChange: (String) -> Unit,
    onNewCustomerNameChange: (String) -> Unit,
    onNewCustomerPhoneChange: (String) -> Unit,
    onNewCustomerBvnChange: (String) -> Unit,
    onNewCustomerNinChange: (String) -> Unit,
    onBorrowerSelected: (BorrowerModel) -> Unit,
    onCreateClick: () -> Unit,
    onGenerateLinkClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val isFormValid = if (customerType == "New Customer") {
        newCustomerName.isNotEmpty() && newCustomerPhone.isNotEmpty() && newCustomerBvn.isNotEmpty() && newCustomerNin.isNotEmpty()
    } else {
        selectedBorrower != null
    }

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
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FieldTheme.colors.gray900)
                    .border(width = 0.5.dp, color = FieldTheme.colors.gray800)
                    .padding(16.dp)
            ) {
                PrimaryButton(
                    text = if (isLoading) "Creating Draft..." else "Begin Application",
                    onClick = onCreateClick,
                    enabled = !isLoading && isFormValid,
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
            // Rich Header
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
                        imageVector = FieldIcons.ShieldOutlined, // Using Shield as primary trusted action icon
                        contentDescription = "Intake",
                        tint = FieldTheme.colors.purple400,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Loan Origination",
                    style = FieldTheme.typography.title,
                    color = FieldTheme.colors.gray100
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select product category and client profile to begin application draft.",
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
                    text = "APPLICATION SETUP",
                    style = FieldTheme.typography.label.copy(color = FieldTheme.colors.purple400)
                )

                // Select Loan Category Dropdown
                FieldDropdown(
                    value = loanCategory,
                    options = listOf("Enterprise Loan", "MSEF", "PAYEE", "Other Option"),
                    onOptionSelected = onLoanCategoryChange,
                    label = "Select Loan Category",
                    isRequired = true
                )

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "CLIENT SELECTION",
                    style = FieldTheme.typography.label.copy(color = FieldTheme.colors.purple400)
                )

                // Tabs for Customer Type
                val activeTab = if (customerType == "New Customer") 0 else 1
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = FieldTheme.colors.gray900,
                    contentColor = FieldTheme.colors.purple400,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    listOf("New Customer", "Existing Customer").forEachIndexed { i, label ->
                        Tab(
                            selected = activeTab == i,
                            onClick = { onCustomerTypeChange(label) },
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

                // Share Client Intake Link Card — shown before detail fields so it's immediately visible
                val context = LocalContext.current
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = FieldTheme.colors.purple600.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = FieldTheme.colors.purple600.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Send Link to Customer",
                        style = FieldTheme.typography.bodyStrong.copy(fontSize = 16.sp),
                        color = FieldTheme.colors.gray100
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Generate a secure link and send it to the customer so they can fill in their own details and upload documents directly — without you having to enter them manually.",
                        style = FieldTheme.typography.body.copy(fontSize = 13.sp),
                        color = FieldTheme.colors.gray400
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (shareUrl != null) {
                        FieldTextField(
                            value = shareUrl,
                            onValueChange = {},
                            label = "Customer Intake Link",
                            readOnly = true,
                            trailingIcon = {
                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("FieldCRM Client Intake Link", shareUrl)
                                        clipboard.setPrimaryClip(clip)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = FieldTheme.colors.purple600),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text("Copy", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        )
                    } else {
                        Button(
                            onClick = onGenerateLinkClick,
                            enabled = !isGeneratingLink,
                            colors = ButtonDefaults.buttonColors(containerColor = FieldTheme.colors.purple600),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isGeneratingLink) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Generate & Send Link to Customer", color = Color.White)
                            }
                        }
                    }
                }

                // Divider with "or fill manually" label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = FieldTheme.colors.gray800)
                    Text(
                        text = "OR FILL DETAILS BELOW",
                        style = FieldTheme.typography.label,
                        color = FieldTheme.colors.gray600
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = FieldTheme.colors.gray800)
                }

                if (customerType == "Existing Customer") {
                    val borrowerNames = borrowers.map { it.name }
                    val selectedName = selectedBorrower?.name ?: ""

                    FieldDropdown(
                        value = selectedName,
                        options = borrowerNames,
                        onOptionSelected = { name ->
                            borrowers.find { it.name == name }?.let { onBorrowerSelected(it) }
                        },
                        label = "Select Registered Profile",
                        isRequired = true
                    )
                } else {
                    // New Customer Embedded Fields
                    FieldTextField(
                        value = newCustomerName,
                        onValueChange = onNewCustomerNameChange,
                        label = "Legal Full Name",
                        placeholder = "Adaeze Okonkwo",
                        isRequired = true,
                        leadingIcon = {
                            Icon(imageVector = FieldIcons.PersonOutlined, contentDescription = null, tint = FieldTheme.colors.gray500)
                        }
                    )
                    FieldTextField(
                        value = newCustomerPhone,
                        onValueChange = onNewCustomerPhoneChange,
                        label = "Primary Phone",
                        placeholder = "e.g. +234 80...",
                        isRequired = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                        leadingIcon = {
                            Icon(imageVector = FieldIcons.PhoneOutlined, contentDescription = null, tint = FieldTheme.colors.gray500)
                        }
                    )
                    FieldTextField(
                        value = newCustomerBvn,
                        onValueChange = onNewCustomerBvnChange,
                        label = "Bank Verification Number (BVN)",
                        placeholder = "11-digit BVN",
                        isRequired = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        leadingIcon = {
                            Icon(imageVector = FieldIcons.FingerprintOutlined, contentDescription = null, tint = FieldTheme.colors.gray500)
                        }
                    )
                    FieldTextField(
                        value = newCustomerNin,
                        onValueChange = onNewCustomerNinChange,
                        label = "National Identification Number (NIN)",
                        placeholder = "11-digit NIN",
                        isRequired = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        leadingIcon = {
                            Icon(imageVector = FieldIcons.BadgeOutlined, contentDescription = null, tint = FieldTheme.colors.gray500)
                        }
                    )
                }

                if (errorMessage != null) {
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

                Spacer(modifier = Modifier.height(32.dp))
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
            shareUrl = null,
            isGeneratingLink = false,
            onCustomerTypeChange = {},
            onLoanCategoryChange = {},
            onNewCustomerNameChange = {},
            onNewCustomerPhoneChange = {},
            onNewCustomerBvnChange = {},
            onNewCustomerNinChange = {},
            onBorrowerSelected = {},
            onCreateClick = {},
            onGenerateLinkClick = {},
            onBackClick = {}
        )
    }
}
