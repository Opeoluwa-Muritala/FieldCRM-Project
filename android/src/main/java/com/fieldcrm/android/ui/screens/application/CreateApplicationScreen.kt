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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
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
import com.fieldcrm.android.data.api.ExistingCustomerSearchItem
import com.fieldcrm.android.data.api.PersonalProfileSnapshot
import kotlinx.coroutines.delay

@Composable
fun CreateApplicationScreenView(
    viewModel: ApplicationViewModel,
    borrowers: List<BorrowerModel>,
    onApplicationCreated: (LoanApplicationModel, BorrowerModel) -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val configViewModel: com.fieldcrm.android.ui.viewmodel.ConfigViewModel = org.koin.androidx.compose.koinViewModel()
    val configState by configViewModel.uiState.collectAsState()
    
    val products = configState.config?.dropdowns?.loan_products ?: emptyList()
    val loanCategoryOptions = if (products.isNotEmpty()) products.map { it.name } else listOf("Enterprise Loan", "MSEF", "PAYEE", "Other Option")
    
    val loanCategoryDisplayName = if (products.isNotEmpty()) {
        products.find { it.id.equals(state.loanCategory, ignoreCase = true) }?.name ?: state.loanCategory
    } else {
        when (state.loanCategory) {
            "enterprise" -> "Enterprise Loan"
            "msef" -> "MSEF"
            "payee" -> "PAYEE"
            "other" -> "Other Option"
            else -> state.loanCategory
        }
    }
    
    LaunchedEffect(state.customerSearchQuery, state.customerType) {
        if (state.customerType == "existing" && state.customerSearchQuery.trim().length >= 3) {
            delay(350)
            viewModel.searchExistingCustomers()
        }
    }
 
    CreateApplicationContent(
        isLoading = state.isLoading,
        errorMessage = state.errorMessage,
        customerType = state.customerType,
        loanCategory = loanCategoryDisplayName,
        loanCategoryOptions = loanCategoryOptions,
        newCustomerName = state.newCustomerName,
        newCustomerPhone = state.newCustomerPhone,
        newCustomerBvn = state.newCustomerBvn,
        newCustomerNin = state.newCustomerNin,
        selectedBorrower = state.selectedBorrowerForApp,
        borrowers = borrowers,
        shareUrl = state.shareUrl,
        isGeneratingLink = state.isGeneratingLink,
        onCustomerTypeChange = { viewModel.setCustomerType(it) },
        onLoanCategoryChange = { name ->
            val code = products.find { it.name.equals(name, ignoreCase = true) }?.id ?: when (name) {
                "Enterprise Loan" -> "enterprise"
                "Other Option" -> "other"
                else -> name.lowercase()
            }
            viewModel.setLoanCategory(code)
        },
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
        onBackClick = onBackClick,
        customerSearchQuery = state.customerSearchQuery,
        customerSearchResults = state.customerSearchResults,
        isSearchingCustomers = state.isSearchingCustomers,
        selectedCustomerProfile = state.selectedCustomerProfile,
        onCustomerSearchQueryChange = viewModel::setCustomerSearchQuery,
        onSearchCustomer = viewModel::searchExistingCustomers,
        onExistingCustomerSelected = viewModel::selectExistingCustomer
    )
}

@Composable
fun CreateApplicationContent(
    isLoading: Boolean,
    errorMessage: String?,
    customerType: String,
    loanCategory: String,
    loanCategoryOptions: List<String> = emptyList(),
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
    onBackClick: () -> Unit,
    customerSearchQuery: String = "",
    customerSearchResults: List<ExistingCustomerSearchItem> = emptyList(),
    isSearchingCustomers: Boolean = false,
    selectedCustomerProfile: PersonalProfileSnapshot? = null,
    onCustomerSearchQueryChange: (String) -> Unit = {},
    onSearchCustomer: () -> Unit = {},
    onExistingCustomerSelected: (ExistingCustomerSearchItem) -> Unit = {}
) {
    val isFormValid = if (customerType == "new") {
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
        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
        val nameFocusRequester = remember { FocusRequester() }
        val phoneFocusRequester = remember { FocusRequester() }
        val bvnFocusRequester = remember { FocusRequester() }
        val ninFocusRequester = remember { FocusRequester() }

        KeyboardAwareForm(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                    options = loanCategoryOptions,
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
                val activeTab = if (customerType == "new") 0 else 1
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

                if (customerType == "existing") {
                    FieldTextField(
                        value = customerSearchQuery,
                        onValueChange = onCustomerSearchQueryChange,
                        label = "Search Organization Customers",
                        placeholder = "Name, BVN, NIN, phone, or reference",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearchCustomer() }),
                        trailingIcon = {
                            if (isSearchingCustomers) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Icon(FieldIcons.SearchOutlined, "Search customers", tint = FieldTheme.colors.gray500)
                        }
                    )

                    customerSearchResults.forEach { customer ->
                        ExistingCustomerRow(customer = customer, onClick = { onExistingCustomerSelected(customer) })
                    }

                    if (selectedBorrower != null) {
                        SelectedCustomerCard(selectedBorrower, selectedCustomerProfile)
                    }
                } else {
                    // New Customer Embedded Fields
                    FieldFormText(
                        value = newCustomerName,
                        onValueChange = onNewCustomerNameChange,
                        label = "Legal Full Name",
                        placeholder = "Adaeze Okonkwo",
                        isRequired = true,
                        focusRequester = nameFocusRequester,
                        imeAction = ImeAction.Next,
                        keyboardActions = KeyboardActions(onNext = { phoneFocusRequester.requestFocus() })
                    )
                    FieldPhone(
                        value = newCustomerPhone,
                        onValueChange = onNewCustomerPhoneChange,
                        label = "Primary Phone",
                        placeholder = "08012345678",
                        isRequired = true,
                        focusRequester = phoneFocusRequester,
                        imeAction = ImeAction.Next,
                        keyboardActions = KeyboardActions(onNext = { bvnFocusRequester.requestFocus() })
                    )
                    FieldInteger(
                        value = newCustomerBvn,
                        onValueChange = { if (it.length <= 11) onNewCustomerBvnChange(it) },
                        label = "Bank Verification Number (BVN)",
                        placeholder = "11-digit BVN",
                        isRequired = true,
                        focusRequester = bvnFocusRequester,
                        imeAction = ImeAction.Next,
                        keyboardActions = KeyboardActions(onNext = { ninFocusRequester.requestFocus() })
                    )
                    FieldInteger(
                        value = newCustomerNin,
                        onValueChange = { if (it.length <= 11) onNewCustomerNinChange(it) },
                        label = "National Identification Number (NIN)",
                        placeholder = "11-digit NIN",
                        isRequired = true,
                        focusRequester = ninFocusRequester,
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                        })
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

@Composable
private fun ExistingCustomerRow(customer: ExistingCustomerSearchItem, onClick: () -> Unit) {
    FieldCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(customer.legal_name, style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray100)
                Text(if (customer.active) "ACTIVE" else "INACTIVE", style = FieldTheme.typography.label, color = if (customer.active) FieldTheme.colors.statusSuccess else FieldTheme.colors.gray500)
            }
            Text(customer.customer_reference ?: "No customer reference", style = FieldTheme.typography.mono, color = FieldTheme.colors.gray400)
            Text(
                listOfNotNull(customer.masked_phone, customer.masked_bvn ?: customer.masked_nin).joinToString(" | ").ifBlank { "Identifiers not available" },
                style = FieldTheme.typography.body,
                color = FieldTheme.colors.gray500
            )
            val ownership = listOfNotNull(customer.branch, customer.relationship_owner).joinToString(" | ")
            if (ownership.isNotBlank()) Text(ownership, style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
        }
    }
}

@Composable
private fun SelectedCustomerCard(customer: BorrowerModel, profile: PersonalProfileSnapshot?) {
    FieldCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("Selected Customer", style = FieldTheme.typography.label, color = FieldTheme.colors.purple400)
            Text(customer.name, style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray100)
            listOfNotNull(profile?.phone, profile?.email, profile?.residential_address).forEach {
                Text(it, style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)
            }
            Text(
                "Personal information is copied into this application. Changes affect this application only.",
                style = FieldTheme.typography.body,
                color = FieldTheme.colors.statusWarning
            )
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
