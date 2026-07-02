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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext

data class WizardTab(val index: Int, val name: String, val icon: String)

@Composable
fun LoanApplicationFormScreen(
    application: LoanApplicationModel,
    borrower: BorrowerModel?,
    applicationViewModel: ApplicationViewModel,
    borrowerViewModel: BorrowerViewModel,
    appViewModel: AppViewModel,
    onBackClick: () -> Unit,
    onNavigateToGuarantorsForm: () -> Unit = {}
) {
    LoanApplicationFormContent(
        application = application,
        borrower = borrower,
        onBackClick = onBackClick,
        onNavigateToGuarantorsForm = onNavigateToGuarantorsForm,
        onSubmit = { name, phone, bvn, address, _, _, employment, employer, income, amount, tenure, product, collateralDesc, collateralVal, gName, gPhone, bank, acc ->
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
                applicationViewModel.submitIntakeForm(updatedApp, updatedBorrower) {
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
    onNavigateToGuarantorsForm: () -> Unit = {},
    onSubmit: (
        name: String, phone: String, bvn: String, address: String, dob: String, marital: String,
        employment: String, employer: String, income: String, amount: String, tenure: String,
        product: String, collateralDesc: String, collateralVal: String, gName: String, gPhone: String,
        bank: String, acc: String
    ) -> Unit
) {
    var currentTab by remember { mutableIntStateOf(0) }
    var isDirty by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

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

    // Step 1 additional fields
    var idTypeInput by remember { mutableStateOf("National ID") }
    var idNumberInput by remember { mutableStateOf("") }
    var idExpiryInput by remember { mutableStateOf("") }
    var stateOfOriginInput by remember { mutableStateOf("") }
    var lgaInput by remember { mutableStateOf("") }
    var nearestLandmarkInput by remember { mutableStateOf("") }

    var spouseNameInput by remember { mutableStateOf("") }
    var spousePhoneInput by remember { mutableStateOf("") }
    var spouseChildrenInput by remember { mutableStateOf("2") }
    var spouseDependantsInput by remember { mutableStateOf("1") }
    var spouseBusinessAddressInput by remember { mutableStateOf("") }

    // Step 2 spouse signature
    var spouseSignatureData by remember { mutableStateOf<String?>(null) }

    var gNameInput by remember { mutableStateOf(borrower?.guarantor_name ?: "") }
    var gPhoneInput by remember { mutableStateOf(borrower?.guarantor_phone ?: "") }

    var employmentInput by remember { mutableStateOf(borrower?.employment_status ?: "Self-employed") }
    var employerInput by remember { mutableStateOf(borrower?.employer_name ?: "") }
    var incomeInput by remember { mutableStateOf(borrower?.monthly_income?.toInt()?.toString() ?: "350000") }

    var industryInput by remember { mutableStateOf("") }
    var yearsEmployedInput by remember { mutableStateOf("") }
    var employerAddressInput by remember { mutableStateOf("") }
    var businessTypeInput by remember { mutableStateOf("") }
    var businessDetailsInput by remember { mutableStateOf("") }
    var supportingProofInput by remember { mutableStateOf("") }

    var facilityBankInput by remember { mutableStateOf("") }
    var facilityAmountInput by remember { mutableStateOf("") }
    var facilityTenureInput by remember { mutableStateOf("") }

    var amountInput by remember { mutableStateOf(application.amount.toInt().toString()) }
    var tenureInput by remember { mutableStateOf(application.tenure.toString()) }
    var productInput by remember { mutableStateOf(application.product_type) }

    // Step 6 mode of repayment
    var modeOfRepayment by remember { mutableStateOf("Direct Debit") }

    // Step 7 additional fields
    var accountNameInput by remember { mutableStateOf("") }
    var sortCodeInput by remember { mutableStateOf("") }

    var collateralDescInput by remember { mutableStateOf(application.collateral_desc ?: "") }
    var collateralValInput by remember { mutableStateOf(application.collateral_value?.toInt()?.toString() ?: "") }

    var bankInput by remember { mutableStateOf(borrower?.bank_name ?: "") }
    var accInput by remember { mutableStateOf(borrower?.account_number ?: "") }

    // Step 9 consent state
    var educationLevelInput by remember { mutableStateOf("Graduate") }
    var loanPurposeInput by remember { mutableStateOf("Working Capital") }
    var collateralSecurityInput by remember { mutableStateOf(setOf<String>()) }

    var pledgeBorrowerInput by remember { mutableStateOf(borrower?.name ?: "") }
    var pledgeObligorInput by remember { mutableStateOf("") }
    var pledgeLocationInput by remember { mutableStateOf("") }
    var pledgeDateInput by remember { mutableStateOf("") }
    var pledgeAmountWordsInput by remember { mutableStateOf("") }
    var pledgeWitnessNameInput by remember { mutableStateOf("") }
    var pledgeWitnessAddressInput by remember { mutableStateOf("") }
    var pledgeLegalAckInput by remember { mutableStateOf(false) }

    var consentBureauDisclosure by remember { mutableStateOf(false) }
    var consentCreditCheck by remember { mutableStateOf(false) }
    var consentChequeRecovery by remember { mutableStateOf(false) }
    var consentGsi by remember { mutableStateOf(false) }
    var step9SignatureData by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950)
    ) {
        val isWide = maxWidth >= 840.dp

        if (showUnsavedDialog) {
            AlertDialog(
                onDismissRequest = { showUnsavedDialog = false },
                title = { Text("Unsaved Changes") },
                text = { Text("You have unsaved form data. Leave without saving?") },
                confirmButton = {
                    TextButton(onClick = { showUnsavedDialog = false; onBackClick() }) { Text("Leave") }
                },
                dismissButton = {
                    TextButton(onClick = { showUnsavedDialog = false }) { Text("Stay") }
                }
            )
        }

        Scaffold(
            topBar = {
                FieldTopAppBar(
                    title = "Lending Wizard: Step ${currentTab + 1} of 9",
                    navigationIcon = {
                        IconButton(onClick = {
                            if (isDirty && currentTab > 0) showUnsavedDialog = true else onBackClick()
                        }) {
                            Icon(
                                imageVector = FieldIcons.ArrowBackOutlined,
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
                                            imageVector = FieldIcons.CheckOutlined,
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
                            name = nameInput, onNameChange = { nameInput = it; isDirty = true },
                            phone = phoneInput, onPhoneChange = { phoneInput = it },
                            bvn = bvnInput, onBvnChange = { bvnInput = it },
                            address = addressInput, onAddressChange = { addressInput = it },
                            dob = dobInput, onDobChange = { dobInput = it },
                            marital = maritalInput, onMaritalChange = { maritalInput = it },
                            idType = idTypeInput, onIdTypeChange = { idTypeInput = it },
                            idNumber = idNumberInput, onIdNumberChange = { idNumberInput = it },
                            idExpiry = idExpiryInput, onIdExpiryChange = { idExpiryInput = it },
                            stateOfOrigin = stateOfOriginInput, onStateOfOriginChange = { stateOfOriginInput = it },
                            lga = lgaInput, onLgaChange = { lgaInput = it },
                            nearestLandmark = nearestLandmarkInput, onNearestLandmarkChange = { nearestLandmarkInput = it },
                            spouseName = spouseNameInput, onSpouseNameChange = { spouseNameInput = it },
                            spousePhone = spousePhoneInput, onSpousePhoneChange = { spousePhoneInput = it },
                            spouseChildren = spouseChildrenInput, onSpouseChildrenChange = { spouseChildrenInput = it },
                            spouseDependants = spouseDependantsInput, onSpouseDependantsChange = { spouseDependantsInput = it },
                            spouseBusinessAddress = spouseBusinessAddressInput, onSpouseBusinessAddressChange = { spouseBusinessAddressInput = it },
                            spouseSignatureData = spouseSignatureData, onSpouseSignatureConfirm = { spouseSignatureData = it }, onSpouseSignatureClear = { spouseSignatureData = null },
                            gName = gNameInput, onGNameChange = { gNameInput = it },
                            gPhone = gPhoneInput, onGPhoneChange = { gPhoneInput = it },
                            onNavigateToGuarantorsForm = onNavigateToGuarantorsForm,
                            employment = employmentInput, onEmploymentChange = { employmentInput = it },
                            employer = employerInput, onEmployerChange = { employerInput = it },
                            income = incomeInput, onIncomeChange = { incomeInput = it },
                            industry = industryInput, onIndustryChange = { industryInput = it },
                            yearsEmployed = yearsEmployedInput, onYearsEmployedChange = { yearsEmployedInput = it },
                            employerAddress = employerAddressInput, onEmployerAddressChange = { employerAddressInput = it },
                            businessType = businessTypeInput, onBusinessTypeChange = { businessTypeInput = it },
                            businessDetails = businessDetailsInput, onBusinessDetailsChange = { businessDetailsInput = it },
                            supportingProof = supportingProofInput, onSupportingProofChange = { supportingProofInput = it },
                            facilityBank = facilityBankInput, onFacilityBankChange = { facilityBankInput = it },
                            facilityAmount = facilityAmountInput, onFacilityAmountChange = { facilityAmountInput = it },
                            facilityTenure = facilityTenureInput, onFacilityTenureChange = { facilityTenureInput = it },
                            amount = amountInput, onAmountChange = { amountInput = it; isDirty = true },
                            tenure = tenureInput, onTenureChange = { tenureInput = it },
                            product = productInput, onProductChange = { productInput = it },
                            modeOfRepayment = modeOfRepayment, onModeOfRepaymentChange = { modeOfRepayment = it },
                            accountName = accountNameInput, onAccountNameChange = { accountNameInput = it },
                            sortCode = sortCodeInput, onSortCodeChange = { sortCodeInput = it },
                            collateralDesc = collateralDescInput, onCollateralDescChange = { collateralDescInput = it },
                            collateralVal = collateralValInput, onCollateralValChange = { collateralValInput = it },
                            bank = bankInput, onBankChange = { bankInput = it },
                            acc = accInput, onAccChange = { accInput = it },
                            educationLevel = educationLevelInput, onEducationLevelChange = { educationLevelInput = it },
                            loanPurpose = loanPurposeInput, onLoanPurposeChange = { loanPurposeInput = it },
                            collateralSecurity = collateralSecurityInput, onCollateralSecurityChange = { collateralSecurityInput = it },
                            pledgeBorrower = pledgeBorrowerInput, onPledgeBorrowerChange = { pledgeBorrowerInput = it },
                            pledgeObligor = pledgeObligorInput, onPledgeObligorChange = { pledgeObligorInput = it },
                            pledgeLocation = pledgeLocationInput, onPledgeLocationChange = { pledgeLocationInput = it },
                            pledgeDate = pledgeDateInput, onPledgeDateChange = { pledgeDateInput = it },
                            pledgeAmountWords = pledgeAmountWordsInput, onPledgeAmountWordsChange = { pledgeAmountWordsInput = it },
                            pledgeWitnessName = pledgeWitnessNameInput, onPledgeWitnessNameChange = { pledgeWitnessNameInput = it },
                            pledgeWitnessAddress = pledgeWitnessAddressInput, onPledgeWitnessAddressChange = { pledgeWitnessAddressInput = it },
                            pledgeLegalAck = pledgeLegalAckInput, onPledgeLegalAckChange = { pledgeLegalAckInput = it },
                            consentBureauDisclosure = consentBureauDisclosure, onConsentBureauDisclosureChange = { consentBureauDisclosure = it },
                            consentCreditCheck = consentCreditCheck, onConsentCreditCheckChange = { consentCreditCheck = it },
                            consentChequeRecovery = consentChequeRecovery, onConsentChequeRecoveryChange = { consentChequeRecovery = it },
                            consentGsi = consentGsi, onConsentGsiChange = { consentGsi = it },
                            step9SignatureData = step9SignatureData, onStep9SignatureConfirm = { step9SignatureData = it }, onStep9SignatureClear = { step9SignatureData = null }
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
                        if (currentTab == 8) {
                            SecondaryButton(
                                text = "Save Draft",
                                onClick = {
                                    onSubmit(
                                        nameInput, phoneInput, bvnInput, addressInput, dobInput, maritalInput,
                                        employmentInput, employerInput, incomeInput, amountInput, tenureInput,
                                        productInput, collateralDescInput, collateralValInput, gNameInput, gPhoneInput,
                                        bankInput, accInput
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            PrimaryButton(
                                text = "Submit for Credit Review",
                                onClick = {
                                    onSubmit(
                                        nameInput, phoneInput, bvnInput, addressInput, dobInput, maritalInput,
                                        employmentInput, employerInput, incomeInput, amountInput, tenureInput,
                                        productInput, collateralDescInput, collateralValInput, gNameInput, gPhoneInput,
                                        bankInput, accInput
                                    )
                                },
                                enabled = consentBureauDisclosure && consentCreditCheck && consentChequeRecovery && consentGsi && step9SignatureData != null,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            PrimaryButton(
                                text = "Next Step",
                                onClick = { currentTab++ },
                                modifier = Modifier.weight(1f)
                            )
                        }
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
    idType: String, onIdTypeChange: (String) -> Unit,
    idNumber: String, onIdNumberChange: (String) -> Unit,
    idExpiry: String, onIdExpiryChange: (String) -> Unit,
    stateOfOrigin: String, onStateOfOriginChange: (String) -> Unit,
    lga: String, onLgaChange: (String) -> Unit,
    nearestLandmark: String, onNearestLandmarkChange: (String) -> Unit,
    spouseName: String, onSpouseNameChange: (String) -> Unit,
    spousePhone: String, onSpousePhoneChange: (String) -> Unit,
    spouseChildren: String, onSpouseChildrenChange: (String) -> Unit,
    spouseDependants: String, onSpouseDependantsChange: (String) -> Unit,
    spouseBusinessAddress: String, onSpouseBusinessAddressChange: (String) -> Unit,
    spouseSignatureData: String?, onSpouseSignatureConfirm: (String) -> Unit, onSpouseSignatureClear: () -> Unit,
    gName: String, onGNameChange: (String) -> Unit,
    gPhone: String, onGPhoneChange: (String) -> Unit,
    onNavigateToGuarantorsForm: () -> Unit,
    employment: String, onEmploymentChange: (String) -> Unit,
    employer: String, onEmployerChange: (String) -> Unit,
    income: String, onIncomeChange: (String) -> Unit,
    industry: String, onIndustryChange: (String) -> Unit,
    yearsEmployed: String, onYearsEmployedChange: (String) -> Unit,
    employerAddress: String, onEmployerAddressChange: (String) -> Unit,
    businessType: String, onBusinessTypeChange: (String) -> Unit,
    businessDetails: String, onBusinessDetailsChange: (String) -> Unit,
    supportingProof: String, onSupportingProofChange: (String) -> Unit,
    facilityBank: String, onFacilityBankChange: (String) -> Unit,
    facilityAmount: String, onFacilityAmountChange: (String) -> Unit,
    facilityTenure: String, onFacilityTenureChange: (String) -> Unit,
    amount: String, onAmountChange: (String) -> Unit,
    tenure: String, onTenureChange: (String) -> Unit,
    product: String, onProductChange: (String) -> Unit,
    modeOfRepayment: String, onModeOfRepaymentChange: (String) -> Unit,
    accountName: String, onAccountNameChange: (String) -> Unit,
    sortCode: String, onSortCodeChange: (String) -> Unit,
    collateralDesc: String, onCollateralDescChange: (String) -> Unit,
    collateralVal: String, onCollateralValChange: (String) -> Unit,
    bank: String, onBankChange: (String) -> Unit,
    acc: String, onAccChange: (String) -> Unit,
    educationLevel: String, onEducationLevelChange: (String) -> Unit,
    loanPurpose: String, onLoanPurposeChange: (String) -> Unit,
    collateralSecurity: Set<String>, onCollateralSecurityChange: (Set<String>) -> Unit,
    pledgeBorrower: String, onPledgeBorrowerChange: (String) -> Unit,
    pledgeObligor: String, onPledgeObligorChange: (String) -> Unit,
    pledgeLocation: String, onPledgeLocationChange: (String) -> Unit,
    pledgeDate: String, onPledgeDateChange: (String) -> Unit,
    pledgeAmountWords: String, onPledgeAmountWordsChange: (String) -> Unit,
    pledgeWitnessName: String, onPledgeWitnessNameChange: (String) -> Unit,
    pledgeWitnessAddress: String, onPledgeWitnessAddressChange: (String) -> Unit,
    pledgeLegalAck: Boolean, onPledgeLegalAckChange: (Boolean) -> Unit,
    consentBureauDisclosure: Boolean, onConsentBureauDisclosureChange: (Boolean) -> Unit,
    consentCreditCheck: Boolean, onConsentCreditCheckChange: (Boolean) -> Unit,
    consentChequeRecovery: Boolean, onConsentChequeRecoveryChange: (Boolean) -> Unit,
    consentGsi: Boolean, onConsentGsiChange: (Boolean) -> Unit,
    step9SignatureData: String?, onStep9SignatureConfirm: (String) -> Unit, onStep9SignatureClear: () -> Unit
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
                Spacer(modifier = Modifier.height(12.dp))
                FieldDropdown(
                    value = idType,
                    options = listOf("National ID", "Voters Card", "Drivers License", "Passport"),
                    onOptionSelected = onIdTypeChange,
                    label = "Means of Identification",
                    isRequired = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                FieldTextField(
                    value = idNumber,
                    onValueChange = onIdNumberChange,
                    label = "ID Number",
                    isRequired = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                FieldTextField(
                    value = idExpiry,
                    onValueChange = onIdExpiryChange,
                    label = "ID Expiry Date",
                    isRequired = true,
                    placeholder = "YYYY-MM-DD"
                )
                Spacer(modifier = Modifier.height(12.dp))
                FieldTextField(
                    value = stateOfOrigin,
                    onValueChange = onStateOfOriginChange,
                    label = "State of Origin"
                )
                Spacer(modifier = Modifier.height(12.dp))
                FieldTextField(
                    value = lga,
                    onValueChange = onLgaChange,
                    label = "LGA"
                )
                Spacer(modifier = Modifier.height(12.dp))
                FieldTextField(
                    value = nearestLandmark,
                    onValueChange = onNearestLandmarkChange,
                    label = "Nearest Landmark / Bus Stop"
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("PASSPORT PHOTO", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                Spacer(modifier = Modifier.height(8.dp))
                var hasPassportPhoto by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(4.dp))
                        .background(FieldTheme.colors.gray900, RoundedCornerShape(4.dp))
                        .clickable { hasPassportPhoto = !hasPassportPhoto }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (hasPassportPhoto) FieldIcons.CheckOutlined else FieldIcons.CameraOutlined,
                        contentDescription = "Passport Photo",
                        tint = if (hasPassportPhoto) FieldTheme.colors.statusSuccess else FieldTheme.colors.purple400,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (hasPassportPhoto) "Passport Photo uploaded / captured" else "No passport photo - tap to upload/capture",
                        style = FieldTheme.typography.body,
                        color = if (hasPassportPhoto) FieldTheme.colors.gray100 else FieldTheme.colors.gray500
                    )
                }
            }
        }
        1 -> {
            FieldCard {
                Text("Spousal Consent & Dependants", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                Spacer(modifier = Modifier.height(16.dp))
                if (marital == "Married") {
                    FieldTextField(
                        value = spouseName,
                        onValueChange = onSpouseNameChange,
                        label = "Name of Spouse",
                        isRequired = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldTextField(
                        value = spousePhone,
                        onValueChange = onSpousePhoneChange,
                        label = "Spouse Telephone Number",
                        isRequired = true
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
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("SPOUSE ATTESTATION SIGNATURE", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                    Spacer(modifier = Modifier.height(8.dp))
                    FieldSignaturePad(
                        onConfirm = { onSpouseSignatureConfirm("signed") },
                        onClear = onSpouseSignatureClear,
                        modifier = Modifier.fillMaxWidth().height(150.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FieldTheme.colors.gray900, RoundedCornerShape(8.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Not Applicable",
                                style = FieldTheme.typography.bodyStrong,
                                color = FieldTheme.colors.gray400,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Spousal consent is not required for $marital applicants. You may proceed to the next step.",
                                style = FieldTheme.typography.body.copy(fontSize = 13.sp),
                                color = FieldTheme.colors.gray500,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
        2 -> {
            var g1Relationship by remember { mutableStateOf("Sibling") }
            var g2Name by remember { mutableStateOf("") }
            var g2Relationship by remember { mutableStateOf("Friend") }
            var g2Phone by remember { mutableStateOf("") }
            
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FieldCard {
                    Text("Guarantor 1 Profile", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                    Spacer(modifier = Modifier.height(16.dp))
                    FieldTextField(
                        value = gName,
                        onValueChange = onGNameChange,
                        label = "Guarantor 1 Full Name",
                        isRequired = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldDropdown(
                        value = g1Relationship,
                        options = listOf("Sibling", "Spouse", "Parent", "Business Partner", "Friend", "Other"),
                        onOptionSelected = { g1Relationship = it },
                        label = "Relationship"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldTextField(
                        value = gPhone,
                        onValueChange = onGPhoneChange,
                        label = "Guarantor 1 Phone",
                        isRequired = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PrimaryButton(
                        text = "Complete Guarantor 1 Form",
                        onClick = onNavigateToGuarantorsForm
                    )
                }

                FieldCard {
                    Text("Guarantor 2 Profile", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                    Spacer(modifier = Modifier.height(16.dp))
                    FieldTextField(
                        value = g2Name,
                        onValueChange = { g2Name = it },
                        label = "Guarantor 2 Full Name",
                        isRequired = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldDropdown(
                        value = g2Relationship,
                        options = listOf("Sibling", "Spouse", "Parent", "Business Partner", "Friend", "Other"),
                        onOptionSelected = { g2Relationship = it },
                        label = "Relationship"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldTextField(
                        value = g2Phone,
                        onValueChange = { g2Phone = it },
                        label = "Guarantor 2 Phone",
                        isRequired = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PrimaryButton(
                        text = "Complete Guarantor 2 Form",
                        onClick = onNavigateToGuarantorsForm
                    )
                }
            }
        }
        3 -> {
            FieldCard {
                Text("Employment & Business Details", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                Spacer(modifier = Modifier.height(16.dp))
                FieldDropdown(
                    value = employment,
                    options = listOf("Full-time", "Part-time", "Contract Staff", "Public Servant", "Self-employed", "Unemployed"),
                    onOptionSelected = onEmploymentChange,
                    label = "Employment Status"
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (employment in listOf("Full-time", "Part-time", "Contract Staff", "Public Servant")) {
                    FieldTextField(
                        value = industry,
                        onValueChange = onIndustryChange,
                        label = "Industry / Sector",
                        isRequired = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldTextField(
                        value = yearsEmployed,
                        onValueChange = onYearsEmployedChange,
                        label = "Years Employed",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isRequired = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldTextField(
                        value = employer,
                        onValueChange = onEmployerChange,
                        label = "Employer Name",
                        isRequired = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldAmountField(
                        value = income,
                        onValueChange = onIncomeChange,
                        label = "Monthly Salary",
                        isRequired = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldTextField(
                        value = employerAddress,
                        onValueChange = onEmployerAddressChange,
                        label = "Employer Address",
                        isRequired = true
                    )
                } else if (employment == "Self-employed") {
                    FieldTextField(
                        value = businessType,
                        onValueChange = onBusinessTypeChange,
                        label = "Business Type",
                        isRequired = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldTextField(
                        value = businessDetails,
                        onValueChange = onBusinessDetailsChange,
                        label = "Business Details / Activity",
                        isRequired = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldAmountField(
                        value = income,
                        onValueChange = onIncomeChange,
                        label = "Monthly Average Sales / Turnover",
                        isRequired = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldTextField(
                        value = supportingProof,
                        onValueChange = onSupportingProofChange,
                        label = "Supporting Proof / References",
                        placeholder = "e.g. CAC Reg No, Rent Receipt, Invoice Ref",
                        isRequired = true
                    )
                } else {
                    Text(
                        text = "Unemployed applicant. No employment details required. You may proceed to the next step.",
                        style = FieldTheme.typography.body,
                        color = FieldTheme.colors.gray500,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
        }
        4 -> {
            var facilityRows by remember { mutableStateOf(listOf(Triple(facilityBank, facilityAmount, facilityTenure))) }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FieldCard {
                    Text("Educational Background", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                    Spacer(modifier = Modifier.height(16.dp))
                    FieldDropdown(
                        value = educationLevel,
                        options = listOf("Primary", "Secondary", "Graduate", "Postgraduate"),
                        onOptionSelected = onEducationLevelChange,
                        label = "Highest Level of Education"
                    )
                }
                FieldCard {
                    Text("Existing Loan Facilities", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                    Spacer(modifier = Modifier.height(16.dp))
                    facilityRows.forEachIndexed { idx, row ->
                        if (idx > 0) Spacer(modifier = Modifier.height(12.dp))
                        FieldTextField(
                            value = row.first,
                            onValueChange = { newVal ->
                                val updated = facilityRows.toMutableList()
                                updated[idx] = Triple(newVal, row.second, row.third)
                                facilityRows = updated
                                if (idx == 0) onFacilityBankChange(newVal)
                            },
                            label = if (idx == 0) "Bank Name" else "Bank Name ${idx + 1}"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                FieldAmountField(
                                    value = row.second,
                                    onValueChange = { newVal ->
                                        val updated = facilityRows.toMutableList()
                                        updated[idx] = Triple(row.first, newVal, row.third)
                                        facilityRows = updated
                                        if (idx == 0) onFacilityAmountChange(newVal)
                                    },
                                    label = "Amount"
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                FieldTextField(
                                    value = row.third,
                                    onValueChange = { newVal ->
                                        val updated = facilityRows.toMutableList()
                                        updated[idx] = Triple(row.first, row.second, newVal)
                                        facilityRows = updated
                                        if (idx == 0) onFacilityTenureChange(newVal)
                                    },
                                    label = "Tenor (months)",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                            if (facilityRows.size > 1) {
                                IconButton(
                                    onClick = {
                                        val updated = facilityRows.toMutableList()
                                        updated.removeAt(idx)
                                        facilityRows = updated
                                    },
                                    modifier = Modifier.padding(top = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = FieldIcons.CloseOutlined,
                                        contentDescription = "Remove",
                                        tint = FieldTheme.colors.statusDanger
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    SecondaryButton(
                        text = "Add Another Facility",
                        onClick = { facilityRows = facilityRows + Triple("", "", "") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        5 -> {
            val collateralOptions = listOf("Shop Stock", "Household Appliances", "Business Proceeds", "Property Documents", "Vehicles")
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                        value = loanPurpose,
                        options = listOf("Working Capital", "Business Asset Acquisition", "Emergency Personal Expense", "Other"),
                        onOptionSelected = { onLoanPurposeChange(it); onProductChange(it) },
                        label = "Loan Purpose",
                        isRequired = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldDropdown(
                        value = modeOfRepayment,
                        options = listOf("Cheque", "Standing Order", "Direct Debit", "Cash Deposit"),
                        onOptionSelected = onModeOfRepaymentChange,
                        label = "Mode of Repayment",
                        isRequired = true
                    )
                }
                FieldCard {
                    Text("Collateral Security", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Select all that apply", style = FieldTheme.typography.body, color = FieldTheme.colors.gray500)
                    Spacer(modifier = Modifier.height(12.dp))
                    collateralOptions.forEach { option ->
                        val checked = option in collateralSecurity
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCollateralSecurityChange(
                                        if (checked) collateralSecurity - option else collateralSecurity + option
                                    )
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    onCollateralSecurityChange(
                                        if (it) collateralSecurity + option else collateralSecurity - option
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(option, style = FieldTheme.typography.body, color = FieldTheme.colors.gray300)
                        }
                    }
                }
            }
        }
        6 -> {
            FieldCard {
                Text("Disbursement Account Details", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                Spacer(modifier = Modifier.height(16.dp))
                FieldTextField(
                    value = accountName,
                    onValueChange = onAccountNameChange,
                    label = "Account Name",
                    isRequired = true
                )
                Spacer(modifier = Modifier.height(12.dp))
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
                Spacer(modifier = Modifier.height(12.dp))
                FieldTextField(
                    value = sortCode,
                    onValueChange = onSortCodeChange,
                    label = "Sort Code"
                )
            }
        }
        7 -> {
            var pledgeAmountInput by remember { mutableStateOf(amount) }
            var pledgeDescription by remember { mutableStateOf(collateralDesc) }
            var pledgeItems by remember { mutableStateOf(listOf(Pair("", ""))) }
            var borrowerSignature by remember { mutableStateOf<String?>(null) }
            var witnessSignature by remember { mutableStateOf<String?>(null) }
            val context = LocalContext.current
            var uploadedDocName by remember { mutableStateOf<String?>(null) }
            val pledgePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                uri?.let {
                    val name = context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else "pledge_document"
                    } ?: "pledge_document"
                    uploadedDocName = name
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FieldCard {
                    Text("Pledge & Trust Receipt Details", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                    Spacer(modifier = Modifier.height(16.dp))
                    FieldTextField(
                        value = pledgeBorrower,
                        onValueChange = onPledgeBorrowerChange,
                        label = "Borrower / Pledgor Name",
                        isRequired = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldTextField(
                        value = pledgeObligor,
                        onValueChange = onPledgeObligorChange,
                        label = "Obligor / Surety Name",
                        isRequired = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldTextField(
                        value = pledgeLocation,
                        onValueChange = onPledgeLocationChange,
                        label = "Pledge Location / Address",
                        isRequired = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldTextField(
                        value = pledgeDate,
                        onValueChange = onPledgeDateChange,
                        label = "Pledge Date",
                        placeholder = "YYYY-MM-DD",
                        isRequired = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldAmountField(
                        value = pledgeAmountInput,
                        onValueChange = {
                            pledgeAmountInput = it
                            onCollateralValChange(it)
                        },
                        label = "Pledge Amount (Figures)",
                        isRequired = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldTextField(
                        value = pledgeAmountWords,
                        onValueChange = onPledgeAmountWordsChange,
                        label = "Pledge Amount (Words)",
                        placeholder = "e.g. Five Hundred Thousand Naira Only",
                        isRequired = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldTextField(
                        value = pledgeDescription,
                        onValueChange = {
                            pledgeDescription = it
                            onCollateralDescChange(it)
                        },
                        label = "General Pledge Description",
                        isRequired = true
                    )
                }

                FieldCard {
                    Text("Pledged Items Schedule", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                    Spacer(modifier = Modifier.height(16.dp))

                    pledgeItems.forEachIndexed { idx, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.5f)) {
                                FieldTextField(
                                    value = item.first,
                                    onValueChange = { newVal ->
                                        val newList = pledgeItems.toMutableList()
                                        newList[idx] = Pair(newVal, item.second)
                                        pledgeItems = newList
                                    },
                                    label = "Item Description ${idx + 1}"
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                FieldTextField(
                                    value = item.second,
                                    onValueChange = { newVal ->
                                        val newList = pledgeItems.toMutableList()
                                        newList[idx] = Pair(item.first, newVal)
                                        pledgeItems = newList
                                    },
                                    label = "Qty / Value"
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (pledgeItems.size > 1) {
                                        val newList = pledgeItems.toMutableList()
                                        newList.removeAt(idx)
                                        pledgeItems = newList
                                    }
                                },
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Icon(
                                    imageVector = FieldIcons.CloseOutlined,
                                    contentDescription = "Remove Item",
                                    tint = FieldTheme.colors.statusDanger
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    SecondaryButton(
                        text = "Add Pledged Item",
                        onClick = { pledgeItems = pledgeItems + Pair("", "") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                FieldCard {
                    Text("Witness Details", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                    Spacer(modifier = Modifier.height(16.dp))
                    FieldTextField(
                        value = pledgeWitnessName,
                        onValueChange = onPledgeWitnessNameChange,
                        label = "Witness Full Name",
                        isRequired = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldTextField(
                        value = pledgeWitnessAddress,
                        onValueChange = onPledgeWitnessAddressChange,
                        label = "Witness Address",
                        isRequired = true
                    )
                }

                FieldCard {
                    Text("Signatures & Verification", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("BORROWER SIGNATURE", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                    Spacer(modifier = Modifier.height(8.dp))
                    FieldSignaturePad(
                        onConfirm = { borrowerSignature = "signed" },
                        onClear = { borrowerSignature = null },
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("WITNESS SIGNATURE", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                    Spacer(modifier = Modifier.height(8.dp))
                    FieldSignaturePad(
                        onConfirm = { witnessSignature = "signed" },
                        onClear = { witnessSignature = null },
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPledgeLegalAckChange(!pledgeLegalAck) }
                    ) {
                        Checkbox(checked = pledgeLegalAck, onCheckedChange = onPledgeLegalAckChange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "I acknowledge that the pledged items are free of encumbrance and I have legal authority to pledge them.",
                            style = FieldTheme.typography.body,
                            color = FieldTheme.colors.gray300,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("ALTERNATIVE METHOD", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(6.dp))
                            .background(FieldTheme.colors.gray900, RoundedCornerShape(6.dp))
                            .clickable { pledgePickerLauncher.launch("*/*") }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = FieldIcons.DocumentOutlined,
                            contentDescription = "Upload Document",
                            tint = FieldTheme.colors.purple400,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = uploadedDocName?.let { "Uploaded: $it" } ?: "Upload alternative signed document",
                            style = FieldTheme.typography.body,
                            color = if (uploadedDocName != null) FieldTheme.colors.gray100 else FieldTheme.colors.gray500
                        )
                    }
                }
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
            Spacer(modifier = Modifier.height(16.dp))
            FieldCard {
                Text("DECLARATIONS & LEGAL CONSENTS", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = consentBureauDisclosure, onCheckedChange = onConsentBureauDisclosureChange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("I consent to Credit Bureau Disclosure and sharing of my credit information.", style = FieldTheme.typography.body, color = FieldTheme.colors.gray300, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = consentCreditCheck, onCheckedChange = onConsentCreditCheckChange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("I authorise a credit check to be conducted on my behalf.", style = FieldTheme.typography.body, color = FieldTheme.colors.gray300, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().background(FieldTheme.colors.statusWarning.copy(alpha = 0.1f), RoundedCornerShape(6.dp)).padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = consentChequeRecovery, onCheckedChange = onConsentChequeRecoveryChange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("I authorise Cheque Recovery as a repayment mechanism. I understand this is irrevocable once signed.", style = FieldTheme.typography.body, color = FieldTheme.colors.gray300, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().background(FieldTheme.colors.statusWarning.copy(alpha = 0.1f), RoundedCornerShape(6.dp)).padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = consentGsi, onCheckedChange = onConsentGsiChange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("I consent to a GSI Mandate being placed on all my accounts.", style = FieldTheme.typography.body, color = FieldTheme.colors.gray300, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("APPLICANT FINAL SIGNATURE", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                Spacer(modifier = Modifier.height(8.dp))
                FieldSignaturePad(
                    onConfirm = { onStep9SignatureConfirm("signed") },
                    onClear = onStep9SignatureClear,
                    modifier = Modifier.fillMaxWidth().height(150.dp)
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
