package com.fieldcrm.android.ui.screens.application

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.viewmodel.ApplicationViewModel
import com.fieldcrm.android.ui.viewmodel.BorrowerViewModel
import com.fieldcrm.android.ui.viewmodel.AppViewModel
import com.fieldcrm.android.ui.viewmodel.Screen
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel
import java.util.Locale

data class WizardTab(val index: Int, val name: String, val icon: String)

@Composable
fun LoanApplicationFormScreen(
    application: LoanApplicationModel,
    borrower: BorrowerModel?,
    applicationViewModel: ApplicationViewModel,
    borrowerViewModel: BorrowerViewModel,
    appViewModel: AppViewModel,
    onBackClick: () -> Unit
) {
    LoanApplicationFormContent(
        application = application,
        borrower = borrower,
        onBackClick = onBackClick,
        onSubmit = { name, phone, bvn, address, dob, marital, employment, employer, income, amount, tenure, product, collateralDesc, collateralVal, gName, gPhone, bank, acc ->
            val updatedBorrower = borrower?.copy(
                name = name,
                phone = phone,
                bvn = bvn,
                physical_address = address,
                employment_status = employment,
                employer_name = employer,
                monthly_income = income.toDoubleOrNull() ?: 0.0,
                bank_name = bank,
                account_number = acc,
                guarantor_name = gName,
                guarantor_phone = gPhone
            ) ?: BorrowerModel(
                id = java.util.UUID.randomUUID().toString(),
                org_id = "org_1",
                loan_officer_id = "lo_1",
                name = name,
                phone = phone,
                bvn = bvn,
                nin = "12345678901",
                status = "ACTIVE",
                physical_address = address,
                employment_status = employment,
                employer_name = employer,
                monthly_income = income.toDoubleOrNull() ?: 0.0,
                bank_name = bank,
                account_number = acc,
                guarantor_name = gName,
                guarantor_phone = gPhone,
                created_at = System.currentTimeMillis().toString()
            )

            val updatedApp = application.copy(
                amount = amount.toDoubleOrNull() ?: 0.0,
                tenure = tenure.toIntOrNull() ?: 0,
                product_type = product,
                collateral_desc = collateralDesc,
                collateral_value = collateralVal.toDoubleOrNull() ?: 0.0,
                current_stage = 2,
                status = "OCR Review"
            )

            borrowerViewModel.updateBorrowerLocal(updatedBorrower) {
                applicationViewModel.updateApplicationLocal(updatedApp) {
                    appViewModel.setSelectedApplication(updatedApp)
                    appViewModel.setSelectedBorrower(updatedBorrower)
                    appViewModel.triggerSuccessScreen(
                        title = "Intake Complete",
                        subtitle = "Application dossier successfully advanced to OCR Review pipeline stage.",
                        destination = Screen.ApplicationDetail
                    )
                }
            }
        }
    )
}

@Composable
fun LoanApplicationFormContent(
    application: LoanApplicationModel,
    borrower: BorrowerModel?,
    onBackClick: () -> Unit,
    onSubmit: (
        name: String, phone: String, bvn: String, address: String, dob: String, marital: String,
        employment: String, employer: String, income: String, amount: String, tenure: String,
        product: String, collateralDesc: String, collateralVal: String, gName: String, gPhone: String,
        bank: String, acc: String
    ) -> Unit
) {
    var currentTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        WizardTab(0, "Applicant Details", "01"),
        WizardTab(1, "Spousal Consent", "02"),
        WizardTab(2, "Guarantors List", "03"),
        WizardTab(3, "Employment & Business", "04"),
        WizardTab(4, "Existing Facilities", "05"),
        WizardTab(5, "Loan Request", "06"),
        WizardTab(6, "Disbursement Account", "07"),
        WizardTab(7, "Pledge Agreement", "08"),
        WizardTab(8, "Final Review", "09")
    )

    // Form inputs state
    var nameInput by remember { mutableStateOf(borrower?.name ?: "") }
    var phoneInput by remember { mutableStateOf(borrower?.phone ?: "") }
    var bvnInput by remember { mutableStateOf(borrower?.bvn ?: "") }
    var addressInput by remember { mutableStateOf(borrower?.physical_address ?: "") }
    var dobInput by remember { mutableStateOf("1992-04-12") }
    var maritalInput by remember { mutableStateOf("Married") }

    var spouseNameInput by remember { mutableStateOf("") }
    var spousePhoneInput by remember { mutableStateOf("") }
    var spouseChildrenInput by remember { mutableStateOf("2") }
    var spouseDependantsInput by remember { mutableStateOf("1") }
    var spouseBusinessAddressInput by remember { mutableStateOf("") }

    var gNameInput by remember { mutableStateOf(borrower?.guarantor_name ?: "") }
    var gPhoneInput by remember { mutableStateOf(borrower?.guarantor_phone ?: "") }

    var employmentInput by remember { mutableStateOf(borrower?.employment_status ?: "Self Employed") }
    var employerInput by remember { mutableStateOf(borrower?.employer_name ?: "") }
    var incomeInput by remember { mutableStateOf(borrower?.monthly_income?.toInt()?.toString() ?: "350000") }

    var facilityBankInput by remember { mutableStateOf("") }
    var facilityAmountInput by remember { mutableStateOf("") }
    var facilityTenureInput by remember { mutableStateOf("") }

    var amountInput by remember { mutableStateOf(application.amount.toInt().toString()) }
    var tenureInput by remember { mutableStateOf(application.tenure.toString()) }
    var productInput by remember { mutableStateOf(application.product_type) }

    var collateralDescInput by remember { mutableStateOf(application.collateral_desc ?: "") }
    var collateralValInput by remember { mutableStateOf(application.collateral_value?.toInt()?.toString() ?: "") }

    var bankInput by remember { mutableStateOf(borrower?.bank_name ?: "") }
    var accInput by remember { mutableStateOf(borrower?.account_number ?: "") }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950)
    ) {
        val isWide = maxWidth >= 840.dp

        Scaffold(
            topBar = {
                FieldTopAppBar(
                    title = "Lending Wizard: Step ${currentTab + 1} of 9",
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = FieldTheme.colors.gray400
                            )
                        }
                    },
                    actions = {
                        Text(
                            text = tabs[currentTab].name.uppercase(),
                            style = FieldTheme.typography.label.copy(color = FieldTheme.colors.purple400),
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                )
            },
            containerColor = FieldTheme.colors.gray950
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isWide) {
                    Column(
                        modifier = Modifier
                            .width(260.dp)
                            .fillMaxHeight()
                            .background(FieldTheme.colors.gray900)
                            .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(0.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "APPLICATION PROGRESS",
                            style = FieldTheme.typography.label,
                            color = FieldTheme.colors.gray500,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        tabs.forEach { tab ->
                            val isSelected = tab.index == currentTab
                            val isCompleted = tab.index < currentTab
                            val rowColor = if (isSelected) FieldTheme.colors.purple950 else Color.Transparent
                            val textColor = if (isSelected) FieldTheme.colors.purple200 else if (isCompleted) FieldTheme.colors.gray300 else FieldTheme.colors.gray500

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .background(rowColor, RoundedCornerShape(6.dp))
                                    .clickable { currentTab = tab.index }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(
                                            if (isCompleted) FieldTheme.colors.purple600 else if (isSelected) FieldTheme.colors.purple900 else FieldTheme.colors.gray800,
                                            RoundedCornerShape(11.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCompleted) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = "Done",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    } else {
                                        Text(
                                            text = tab.icon,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) FieldTheme.colors.purple400 else FieldTheme.colors.gray400
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = tab.name,
                                    style = FieldTheme.typography.bodyStrong.copy(fontSize = 13.sp),
                                    color = textColor
                                )
                            }
                        }
                    }
                }

                // Main Content Block
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    if (!isWide) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tabs.forEach { tab ->
                                val isSelected = tab.index == currentTab
                                val isCompleted = tab.index < currentTab
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .background(
                                            if (isSelected) FieldTheme.colors.purple600
                                            else if (isCompleted) FieldTheme.colors.purple900.copy(alpha = 0.5f)
                                            else FieldTheme.colors.gray800,
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        WizardTabContent(
                            tabIndex = currentTab,
                            name = nameInput, onNameChange = { nameInput = it },
                            phone = phoneInput, onPhoneChange = { phoneInput = it },
                            bvn = bvnInput, onBvnChange = { bvnInput = it },
                            address = addressInput, onAddressChange = { addressInput = it },
                            dob = dobInput, onDobChange = { dobInput = it },
                            marital = maritalInput, onMaritalChange = { maritalInput = it },
                            spouseName = spouseNameInput, onSpouseNameChange = { spouseNameInput = it },
                            spousePhone = spousePhoneInput, onSpousePhoneChange = { spousePhoneInput = it },
                            spouseChildren = spouseChildrenInput, onSpouseChildrenChange = { spouseChildrenInput = it },
                            spouseDependants = spouseDependantsInput, onSpouseDependantsChange = { spouseDependantsInput = it },
                            spouseBusinessAddress = spouseBusinessAddressInput, onSpouseBusinessAddressChange = { spouseBusinessAddressInput = it },
                            gName = gNameInput, onGNameChange = { gNameInput = it },
                            gPhone = gPhoneInput, onGPhoneChange = { gPhoneInput = it },
                            employment = employmentInput, onEmploymentChange = { employmentInput = it },
                            employer = employerInput, onEmployerChange = { employerInput = it },
                            income = incomeInput, onIncomeChange = { incomeInput = it },
                            facilityBank = facilityBankInput, onFacilityBankChange = { facilityBankInput = it },
                            facilityAmount = facilityAmountInput, onFacilityAmountChange = { facilityAmountInput = it },
                            facilityTenure = facilityTenureInput, onFacilityTenureChange = { facilityTenureInput = it },
                            amount = amountInput, onAmountChange = { amountInput = it },
                            tenure = tenureInput, onTenureChange = { tenureInput = it },
                            product = productInput, onProductChange = { productInput = it },
                            collateralDesc = collateralDescInput, onCollateralDescChange = { collateralDescInput = it },
                            collateralVal = collateralValInput, onCollateralValChange = { collateralValInput = it },
                            bank = bankInput, onBankChange = { bankInput = it },
                            acc = accInput, onAccChange = { accInput = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SecondaryButton(
                            text = "Previous",
                            onClick = { if (currentTab > 0) currentTab-- },
                            enabled = currentTab > 0,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        PrimaryButton(
                            text = if (currentTab == 8) "Submit Profile" else "Next Step",
                            onClick = {
                                if (currentTab < 8) {
                                    currentTab++
                                } else {
                                    onSubmit(
                                        nameInput, phoneInput, bvnInput, addressInput, dobInput, maritalInput,
                                        employmentInput, employerInput, incomeInput, amountInput, tenureInput,
                                        productInput, collateralDescInput, collateralValInput, gNameInput, gPhoneInput,
                                        bankInput, accInput
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WizardTabContent(
    tabIndex: Int,
    name: String, onNameChange: (String) -> Unit,
    phone: String, onPhoneChange: (String) -> Unit,
    bvn: String, onBvnChange: (String) -> Unit,
    address: String, onAddressChange: (String) -> Unit,
    dob: String, onDobChange: (String) -> Unit,
    marital: String, onMaritalChange: (String) -> Unit,
    spouseName: String, onSpouseNameChange: (String) -> Unit,
    spousePhone: String, onSpousePhoneChange: (String) -> Unit,
    spouseChildren: String, onSpouseChildrenChange: (String) -> Unit,
    spouseDependants: String, onSpouseDependantsChange: (String) -> Unit,
    spouseBusinessAddress: String, onSpouseBusinessAddressChange: (String) -> Unit,
    gName: String, onGNameChange: (String) -> Unit,
    gPhone: String, onGPhoneChange: (String) -> Unit,
    employment: String, onEmploymentChange: (String) -> Unit,
    employer: String, onEmployerChange: (String) -> Unit,
    income: String, onIncomeChange: (String) -> Unit,
    facilityBank: String, onFacilityBankChange: (String) -> Unit,
    facilityAmount: String, onFacilityAmountChange: (String) -> Unit,
    facilityTenure: String, onFacilityTenureChange: (String) -> Unit,
    amount: String, onAmountChange: (String) -> Unit,
    tenure: String, onTenureChange: (String) -> Unit,
    product: String, onProductChange: (String) -> Unit,
    collateralDesc: String, onCollateralDescChange: (String) -> Unit,
    collateralVal: String, onCollateralValChange: (String) -> Unit,
    bank: String, onBankChange: (String) -> Unit,
    acc: String, onAccChange: (String) -> Unit
) {
    when (tabIndex) {
        0 -> {
            FieldCard {
                Text("Applicant Details", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                Spacer(modifier = Modifier.height(16.dp))
                FieldTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = "Applicant Full Name",
                    isRequired = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                FieldTextField(
                    value = dob,
                    onValueChange = onDobChange,
                    label = "Date of Birth",
                    isRequired = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                FieldDropdown(
                    value = marital,
                    options = listOf("Single", "Married", "Widowed", "Divorced"),
                    onOptionSelected = onMaritalChange,
                    label = "Marital Status"
                )
                Spacer(modifier = Modifier.height(12.dp))
                FieldTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = "Phone Number",
                    isRequired = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                FieldTextField(
                    value = bvn,
                    onValueChange = onBvnChange,
                    label = "BVN Identifier (11 digits)",
                    isRequired = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                FieldTextField(
                    value = address,
                    onValueChange = onAddressChange,
                    label = "Residential Address",
                    isRequired = true
                )
            }
        }
        1 -> {
            FieldCard {
                Text("Spousal Consent & Dependants", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                Spacer(modifier = Modifier.height(16.dp))
                FieldTextField(
                    value = spouseName,
                    onValueChange = onSpouseNameChange,
                    label = "Name of Spouse",
                    isRequired = marital == "Married"
                )
                Spacer(modifier = Modifier.height(12.dp))
                FieldTextField(
                    value = spousePhone,
                    onValueChange = onSpousePhoneChange,
                    label = "Spouse Telephone Number",
                    isRequired = marital == "Married"
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        FieldTextField(
                            value = spouseChildren,
                            onValueChange = onSpouseChildrenChange,
                            label = "Number of Children",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        FieldTextField(
                            value = spouseDependants,
                            onValueChange = onSpouseDependantsChange,
                            label = "Number of Dependants",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                FieldTextField(
                    value = spouseBusinessAddress,
                    onValueChange = onSpouseBusinessAddressChange,
                    label = "Spouse Business Address"
                )
            }
        }
        2 -> {
            FieldCard {
                Text("Guarantors List Overview", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                Spacer(modifier = Modifier.height(16.dp))
                FieldTextField(
                    value = gName,
                    onValueChange = onGNameChange,
                    label = "Guarantor 1 Full Name",
                    isRequired = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                FieldTextField(
                    value = gPhone,
                    onValueChange = onGPhoneChange,
                    label = "Guarantor 1 Phone",
                    isRequired = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Guarantor profiles can be thoroughly completed and signed from the main dossier screen.",
                    style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                    color = FieldTheme.colors.gray500
                )
            }
        }
        3 -> {
            FieldCard {
                Text("Employment & Business details", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                Spacer(modifier = Modifier.height(16.dp))
                FieldDropdown(
                    value = employment,
                    options = listOf("Public Service", "Private Sector", "Self Employed", "Unemployed"),
                    onOptionSelected = onEmploymentChange,
                    label = "Employment Status"
                )
                Spacer(modifier = Modifier.height(12.dp))
                FieldTextField(
                    value = employer,
                    onValueChange = onEmployerChange,
                    label = "Employer / Business Name",
                    isRequired = employment != "Unemployed"
                )
                Spacer(modifier = Modifier.height(12.dp))
                FieldAmountField(
                    value = income,
                    onValueChange = onIncomeChange,
                    label = "Monthly Average Income / Sales",
                    isRequired = true
                )
            }
        }
        4 -> {
            FieldCard {
                Text("Existing Loan Facilities", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                Spacer(modifier = Modifier.height(16.dp))
                FieldTextField(
                    value = facilityBank,
                    onValueChange = onFacilityBankChange,
                    label = "Bank Name"
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        FieldAmountField(
                            value = facilityAmount,
                            onValueChange = onFacilityAmountChange,
                            label = "Amount"
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        FieldTextField(
                            value = facilityTenure,
                            onValueChange = onFacilityTenureChange,
                            label = "Tenor (months)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }
        }
        5 -> {
            FieldCard {
                Text("Principal Loan Request", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                Spacer(modifier = Modifier.height(16.dp))
                FieldAmountField(
                    value = amount,
                    onValueChange = onAmountChange,
                    label = "Requested Loan Principal",
                    isRequired = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                FieldTextField(
                    value = tenure,
                    onValueChange = onTenureChange,
                    label = "Requested Tenure (Months)",
                    isRequired = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(12.dp))
                FieldDropdown(
                    value = product,
                    options = listOf("Working Capital", "Asset Purchase", "Inventory Expansion", "Education"),
                    onOptionSelected = onProductChange,
                    label = "Loan Product Segment"
                )
            }
        }
        6 -> {
            FieldCard {
                Text("Disbursement Account Details", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                Spacer(modifier = Modifier.height(16.dp))
                FieldTextField(
                    value = bank,
                    onValueChange = onBankChange,
                    label = "Bank Name",
                    isRequired = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                FieldTextField(
                    value = acc,
                    onValueChange = onAccChange,
                    label = "Account Number",
                    isRequired = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
        7 -> {
            FieldCard {
                Text("Collateral Pledge Schedule", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                Spacer(modifier = Modifier.height(16.dp))
                FieldTextField(
                    value = collateralDesc,
                    onValueChange = onCollateralDescChange,
                    label = "Pledged Asset Description",
                    isRequired = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                FieldAmountField(
                    value = collateralVal,
                    onValueChange = onCollateralValChange,
                    label = "Estimated Market Value",
                    isRequired = true
                )
            }
        }
        else -> {
            FieldCard {
                Text("Dossier Summary Review", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Please confirm that all details are accurate. Submitting will advance the application to the OCR Review pipeline stage.", style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)
                Spacer(modifier = Modifier.height(16.dp))
                ReadinessChecklist(
                    gates = listOf(
                        ChecklistGate("Applicant Name Provided", name.isNotEmpty(), if (name.isNotEmpty()) StatusChipVariant.Verified else StatusChipVariant.Missing),
                        ChecklistGate("Requested Loan Principal Captured", amount.isNotEmpty(), if (amount.isNotEmpty()) StatusChipVariant.Verified else StatusChipVariant.Missing),
                        ChecklistGate("Pledge Collateral Specified", collateralDesc.isNotEmpty(), if (collateralDesc.isNotEmpty()) StatusChipVariant.Verified else StatusChipVariant.Missing)
                    )
                )
            }
        }
    }
}

@Preview(name = "Compact Phone Form Wizard", widthDp = 411, heightDp = 850)
@Composable
fun PreviewWizardCompact() {
    val demoApp = LoanApplicationModel(
        id = "app_1", org_id = "org_1", borrower_id = "1",
        current_stage = 1, current_owner_id = "LO_1", status = "intake",
        amount = 450000.0, tenure = 12, product_type = "Working Capital",
        interest_rate = 18.5, repayment_frequency = "MONTHLY", created_at = ""
    )
    val demoBorrower = BorrowerModel(
        id = "1", org_id = "org_1", loan_officer_id = "LO_1",
        name = "Emeka Chukwu", phone = "08012345678", bvn = "222333444", nin = "111222333",
        status = "Active", created_at = "1992-04-12"
    )
    FieldCRMTheme {
        LoanApplicationFormContent(
            application = demoApp,
            borrower = demoBorrower,
            onBackClick = {},
            onSubmit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
        )
    }
}
