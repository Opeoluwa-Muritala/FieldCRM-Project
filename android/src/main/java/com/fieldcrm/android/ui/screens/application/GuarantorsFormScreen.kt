package com.fieldcrm.android.ui.screens.application

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.fieldcrm.android.ui.theme.FieldIcons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.viewmodel.BorrowerViewModel
import com.fieldcrm.shared.model.BorrowerModel

@Composable
fun GuarantorsFormScreen(
    borrower: BorrowerModel,
    borrowerViewModel: BorrowerViewModel,
    onBackClick: () -> Unit,
    onSave: () -> Unit
) {
    GuarantorsFormContent(
        borrower = borrower,
        onBackClick = onBackClick,
        onSaveComplete = { name, phone ->
            val updatedBorrower = borrower.copy(
                guarantor_name = name,
                guarantor_phone = phone
            )
            borrowerViewModel.updateBorrowerLocal(updatedBorrower) {
                onSave()
            }
        }
    )
}

@Composable
fun GuarantorsFormContent(
    borrower: BorrowerModel,
    onBackClick: () -> Unit,
    onSaveComplete: (name: String, phone: String) -> Unit
) {
    // Wizard navigation
    var currentSlot by remember { mutableIntStateOf(1) }   // 1 or 2
    var currentStep by remember { mutableIntStateOf(1) }   // 1–8

    // ── Slot 1 fields ──────────────────────────────────────────────────────────
    var g1FullName       by remember { mutableStateOf(borrower.guarantor_name ?: "") }
    var g1Relationship   by remember { mutableStateOf("") }
    var g1Phone          by remember { mutableStateOf(borrower.guarantor_phone ?: "") }
    var g1Bvn            by remember { mutableStateOf("") }
    var g1DateOfBirth    by remember { mutableStateOf("") }
    var g1StateOfOrigin  by remember { mutableStateOf("") }
    var g1HomeAddress    by remember { mutableStateOf("") }
    var g1ExistingLoans  by remember { mutableStateOf("") }
    var g1MaritalStatus  by remember { mutableStateOf("Single") }
    var g1NumDependants  by remember { mutableStateOf("") }
    var g1SpouseName     by remember { mutableStateOf("") }
    var g1SpouseAddress  by remember { mutableStateOf("") }
    var g1EmployerName   by remember { mutableStateOf("") }
    var g1MonthlySalary  by remember { mutableStateOf("") }
    var g1WorkplaceAddress  by remember { mutableStateOf("") }
    var g1BusinessSector    by remember { mutableStateOf("") }
    var g1MonthlyTurnover   by remember { mutableStateOf("") }
    var g1DeclarationAccepted  by remember { mutableStateOf(false) }
    var g1MaxGuaranteeLimit by remember { mutableStateOf("") }
    var g1ChequeNumber   by remember { mutableStateOf("") }
    var g1BankName       by remember { mutableStateOf("") }
    var g1AccountNumber  by remember { mutableStateOf("") }
    var g1GuarantorSig   by remember { mutableStateOf<String?>(null) }
    var g1WitnessSig     by remember { mutableStateOf<String?>(null) }
    var g1WitnessName    by remember { mutableStateOf("") }
    var g1WitnessDate    by remember { mutableStateOf("") }

    // ── Slot 2 fields ──────────────────────────────────────────────────────────
    var g2FullName       by remember { mutableStateOf("") }
    var g2Relationship   by remember { mutableStateOf("") }
    var g2Phone          by remember { mutableStateOf("") }
    var g2Bvn            by remember { mutableStateOf("") }
    var g2DateOfBirth    by remember { mutableStateOf("") }
    var g2StateOfOrigin  by remember { mutableStateOf("") }
    var g2HomeAddress    by remember { mutableStateOf("") }
    var g2ExistingLoans  by remember { mutableStateOf("") }
    var g2MaritalStatus  by remember { mutableStateOf("Single") }
    var g2NumDependants  by remember { mutableStateOf("") }
    var g2SpouseName     by remember { mutableStateOf("") }
    var g2SpouseAddress  by remember { mutableStateOf("") }
    var g2EmployerName   by remember { mutableStateOf("") }
    var g2MonthlySalary  by remember { mutableStateOf("") }
    var g2WorkplaceAddress  by remember { mutableStateOf("") }
    var g2BusinessSector    by remember { mutableStateOf("") }
    var g2MonthlyTurnover   by remember { mutableStateOf("") }
    var g2DeclarationAccepted  by remember { mutableStateOf(false) }
    var g2MaxGuaranteeLimit by remember { mutableStateOf("") }
    var g2ChequeNumber   by remember { mutableStateOf("") }
    var g2BankName       by remember { mutableStateOf("") }
    var g2AccountNumber  by remember { mutableStateOf("") }
    var g2GuarantorSig   by remember { mutableStateOf<String?>(null) }
    var g2WitnessSig     by remember { mutableStateOf<String?>(null) }
    var g2WitnessName    by remember { mutableStateOf("") }
    var g2WitnessDate    by remember { mutableStateOf("") }

    // ── Active-slot aliases (read/write via lambdas) ───────────────────────────
    val fullName        = if (currentSlot == 1) g1FullName        else g2FullName
    val relationship    = if (currentSlot == 1) g1Relationship    else g2Relationship
    val phone           = if (currentSlot == 1) g1Phone           else g2Phone
    val bvn             = if (currentSlot == 1) g1Bvn             else g2Bvn
    val dateOfBirth     = if (currentSlot == 1) g1DateOfBirth     else g2DateOfBirth
    val stateOfOrigin   = if (currentSlot == 1) g1StateOfOrigin   else g2StateOfOrigin
    val homeAddress     = if (currentSlot == 1) g1HomeAddress     else g2HomeAddress
    val existingLoans   = if (currentSlot == 1) g1ExistingLoans   else g2ExistingLoans
    val maritalStatus   = if (currentSlot == 1) g1MaritalStatus   else g2MaritalStatus
    val numDependants   = if (currentSlot == 1) g1NumDependants   else g2NumDependants
    val spouseName      = if (currentSlot == 1) g1SpouseName      else g2SpouseName
    val spouseAddress   = if (currentSlot == 1) g1SpouseAddress   else g2SpouseAddress
    val employerName    = if (currentSlot == 1) g1EmployerName    else g2EmployerName
    val monthlySalary   = if (currentSlot == 1) g1MonthlySalary   else g2MonthlySalary
    val workplaceAddress= if (currentSlot == 1) g1WorkplaceAddress else g2WorkplaceAddress
    val businessSector  = if (currentSlot == 1) g1BusinessSector  else g2BusinessSector
    val monthlyTurnover = if (currentSlot == 1) g1MonthlyTurnover else g2MonthlyTurnover
    val declarationAccepted = if (currentSlot == 1) g1DeclarationAccepted else g2DeclarationAccepted
    val maxGuaranteeLimit   = if (currentSlot == 1) g1MaxGuaranteeLimit   else g2MaxGuaranteeLimit
    val chequeNumber    = if (currentSlot == 1) g1ChequeNumber    else g2ChequeNumber
    val bankName        = if (currentSlot == 1) g1BankName        else g2BankName
    val accountNumber   = if (currentSlot == 1) g1AccountNumber   else g2AccountNumber
    val guarantorSig    = if (currentSlot == 1) g1GuarantorSig    else g2GuarantorSig
    val witnessSig      = if (currentSlot == 1) g1WitnessSig      else g2WitnessSig
    val witnessName     = if (currentSlot == 1) g1WitnessName     else g2WitnessName
    val witnessDate     = if (currentSlot == 1) g1WitnessDate     else g2WitnessDate

    val onFullNameChange        : (String) -> Unit = { if (currentSlot == 1) g1FullName        = it else g2FullName        = it }
    val onRelationshipChange    : (String) -> Unit = { if (currentSlot == 1) g1Relationship    = it else g2Relationship    = it }
    val onPhoneChange           : (String) -> Unit = { if (currentSlot == 1) g1Phone           = it else g2Phone           = it }
    val onBvnChange             : (String) -> Unit = { if (currentSlot == 1) g1Bvn             = it else g2Bvn             = it }
    val onDateOfBirthChange     : (String) -> Unit = { if (currentSlot == 1) g1DateOfBirth     = it else g2DateOfBirth     = it }
    val onStateOfOriginChange   : (String) -> Unit = { if (currentSlot == 1) g1StateOfOrigin   = it else g2StateOfOrigin   = it }
    val onHomeAddressChange     : (String) -> Unit = { if (currentSlot == 1) g1HomeAddress     = it else g2HomeAddress     = it }
    val onExistingLoansChange   : (String) -> Unit = { if (currentSlot == 1) g1ExistingLoans   = it else g2ExistingLoans   = it }
    val onMaritalStatusChange   : (String) -> Unit = { if (currentSlot == 1) g1MaritalStatus   = it else g2MaritalStatus   = it }
    val onNumDependantsChange   : (String) -> Unit = { if (currentSlot == 1) g1NumDependants   = it else g2NumDependants   = it }
    val onSpouseNameChange      : (String) -> Unit = { if (currentSlot == 1) g1SpouseName      = it else g2SpouseName      = it }
    val onSpouseAddressChange   : (String) -> Unit = { if (currentSlot == 1) g1SpouseAddress   = it else g2SpouseAddress   = it }
    val onEmployerNameChange    : (String) -> Unit = { if (currentSlot == 1) g1EmployerName    = it else g2EmployerName    = it }
    val onMonthlySalaryChange   : (String) -> Unit = { if (currentSlot == 1) g1MonthlySalary   = it else g2MonthlySalary   = it }
    val onWorkplaceAddressChange: (String) -> Unit = { if (currentSlot == 1) g1WorkplaceAddress= it else g2WorkplaceAddress= it }
    val onBusinessSectorChange  : (String) -> Unit = { if (currentSlot == 1) g1BusinessSector  = it else g2BusinessSector  = it }
    val onMonthlyTurnoverChange : (String) -> Unit = { if (currentSlot == 1) g1MonthlyTurnover = it else g2MonthlyTurnover = it }
    val onDeclarationChange     : (Boolean) -> Unit= { if (currentSlot == 1) g1DeclarationAccepted = it else g2DeclarationAccepted = it }
    val onMaxGuaranteeLimitChange:(String) -> Unit = { if (currentSlot == 1) g1MaxGuaranteeLimit= it else g2MaxGuaranteeLimit= it }
    val onChequeNumberChange    : (String) -> Unit = { if (currentSlot == 1) g1ChequeNumber    = it else g2ChequeNumber    = it }
    val onBankNameChange        : (String) -> Unit = { if (currentSlot == 1) g1BankName        = it else g2BankName        = it }
    val onAccountNumberChange   : (String) -> Unit = { if (currentSlot == 1) g1AccountNumber   = it else g2AccountNumber   = it }
    val onGuarantorSigConfirm   : (ImageBitmap) -> Unit = { if (currentSlot == 1) g1GuarantorSig = "signed" else g2GuarantorSig = "signed" }
    val onGuarantorSigClear     : () -> Unit = { if (currentSlot == 1) g1GuarantorSig = null else g2GuarantorSig = null }
    val onWitnessSigConfirm     : (ImageBitmap) -> Unit = { if (currentSlot == 1) g1WitnessSig = "signed" else g2WitnessSig = "signed" }
    val onWitnessSigClear       : () -> Unit = { if (currentSlot == 1) g1WitnessSig = null else g2WitnessSig = null }
    val onWitnessNameChange     : (String) -> Unit = { if (currentSlot == 1) g1WitnessName     = it else g2WitnessName     = it }
    val onWitnessDateChange     : (String) -> Unit = { if (currentSlot == 1) g1WitnessDate     = it else g2WitnessDate     = it }

    // BVN error
    val bvnError = if (bvn.isNotEmpty() && bvn.length != 11) "BVN must be exactly 11 digits" else null

    // Account number error
    val accountNumberError = if (accountNumber.isNotEmpty() && accountNumber.length != 10) "Account number must be exactly 10 digits" else null

    // Step-6 Next gate
    val step6NextEnabled = declarationAccepted

    // Step-8 complete gate
    val step8CompleteEnabled = guarantorSig != null && witnessSig != null &&
            witnessName.isNotEmpty() && witnessDate.isNotEmpty()

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "Guarantor Profile",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = FieldIcons.ArrowBackOutlined,
                            contentDescription = "Back",
                            tint = FieldTheme.colors.gray400
                        )
                    }
                },
                actions = {
                    Text(
                        text = "STEP $currentStep OF 8",
                        style = FieldTheme.typography.label.copy(color = FieldTheme.colors.purple400),
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FieldTheme.colors.gray950)
                    .border(width = 0.5.dp, color = FieldTheme.colors.gray800)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SecondaryButton(
                        text = "Back",
                        onClick = {
                            if (currentStep > 1) {
                                currentStep--
                            } else {
                                onBackClick()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    when {
                        currentStep < 8 -> {
                            PrimaryButton(
                                text = "Next",
                                onClick = { currentStep++ },
                                enabled = if (currentStep == 6) step6NextEnabled else true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        currentStep == 8 && currentSlot == 1 -> {
                            PrimaryButton(
                                text = "Proceed to Guarantor 2",
                                onClick = {
                                    currentSlot = 2
                                    currentStep = 1
                                },
                                enabled = step8CompleteEnabled,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        else -> {
                            PrimaryButton(
                                text = "Complete",
                                onClick = { onSaveComplete(g1FullName, g1Phone) },
                                enabled = step8CompleteEnabled,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(bottom = 100.dp) // Leave space for bottom bar
        ) {
            // High-End Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FieldTheme.colors.purple600.copy(alpha = 0.05f))
                    .border(width = 0.5.dp, color = FieldTheme.colors.purple600.copy(alpha = 0.1f))
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "Guarantor Profile Intake",
                    style = FieldTheme.typography.title.copy(fontSize = 28.sp),
                    color = FieldTheme.colors.gray100
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Detailed compliance screening and documentation for legal credit guarantees.",
                    style = FieldTheme.typography.body.copy(fontSize = 14.sp),
                    color = FieldTheme.colors.gray400
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
            // ── Step progress bar ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..8) {
                    val isCompleted = i < currentStep
                    val isActive    = i == currentStep
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(
                                color = when {
                                    isActive    -> FieldTheme.colors.purple600
                                    isCompleted -> FieldTheme.colors.purple900.copy(alpha = 0.5f)
                                    else        -> FieldTheme.colors.gray800
                                },
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }

            // ── Slot selector ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(1, 2).forEach { slot ->
                    val isActive = currentSlot == slot
                    val slotLabel = "Guarantor $slot"
                    if (isActive) {
                        PrimaryButton(
                            text = slotLabel,
                            onClick = {
                                currentSlot = slot
                                currentStep = 1
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        SecondaryButton(
                            text = slotLabel,
                            onClick = {
                                currentSlot = slot
                                currentStep = 1
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Status banner (step 1 only) ───────────────────────────────────
            if (currentStep == 1) {
                val g1StepsComplete = listOf(
                    g1FullName.isNotEmpty(), g1Phone.isNotEmpty(), g1Bvn.length == 11,
                    true,  // step 2 (optional)
                    true,  // step 3 (optional)
                    g1EmployerName.isNotEmpty(),
                    g1DeclarationAccepted,
                    g1MaxGuaranteeLimit.isNotEmpty(),
                    g1GuarantorSig != null && g1WitnessSig != null
                ).count { it }
                val g2StepsComplete = listOf(
                    g2FullName.isNotEmpty(), g2Phone.isNotEmpty(), g2Bvn.length == 11,
                    true,
                    true,
                    g2EmployerName.isNotEmpty(),
                    g2DeclarationAccepted,
                    g2MaxGuaranteeLimit.isNotEmpty(),
                    g2GuarantorSig != null && g2WitnessSig != null
                ).count { it }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FieldTheme.colors.gray900, RoundedCornerShape(8.dp))
                        .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Guarantor 1: $g1StepsComplete/8 checks complete",
                            style = FieldTheme.typography.label,
                            color = if (g1StepsComplete >= 8) FieldTheme.colors.statusSuccess else FieldTheme.colors.gray400
                        )
                        Text(
                            text = "Guarantor 2: $g2StepsComplete/8 checks complete",
                            style = FieldTheme.typography.label,
                            color = if (g2StepsComplete >= 8) FieldTheme.colors.statusSuccess else FieldTheme.colors.gray400
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Scrollable step content ───────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                when (currentStep) {
                    1 -> {
                        FieldCard {
                            Text(
                                text = "Identity",
                                style = FieldTheme.typography.title,
                                color = FieldTheme.colors.gray100
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            FieldTextField(
                                value = fullName,
                                onValueChange = onFullNameChange,
                                label = "Full Name",
                                isRequired = true
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FieldTextField(
                                value = relationship,
                                onValueChange = onRelationshipChange,
                                label = "Relationship to Applicant",
                                isRequired = true
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
                                label = "BVN (11 digits)",
                                isRequired = true,
                                errorText = bvnError
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FieldTextField(
                                value = dateOfBirth,
                                onValueChange = onDateOfBirthChange,
                                label = "Date of Birth",
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
                                value = homeAddress,
                                onValueChange = onHomeAddressChange,
                                label = "Home Address",
                                isRequired = true
                            )
                        }
                    }

                    2 -> {
                        FieldCard {
                            Text(
                                text = "Existing Obligations",
                                style = FieldTheme.typography.title,
                                color = FieldTheme.colors.gray100
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            FieldTextField(
                                value = existingLoans,
                                onValueChange = onExistingLoansChange,
                                label = "Disclose all active loan obligations and guarantees"
                            )
                        }
                    }

                    3 -> {
                        FieldCard {
                            Text(
                                text = "Family & Marital Details",
                                style = FieldTheme.typography.title,
                                color = FieldTheme.colors.gray100
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            FieldDropdown(
                                value = maritalStatus,
                                options = listOf("Single", "Married", "Widowed"),
                                onOptionSelected = onMaritalStatusChange,
                                label = "Marital Status"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FieldTextField(
                                value = numDependants,
                                onValueChange = onNumDependantsChange,
                                label = "Number of Dependants"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FieldTextField(
                                value = spouseName,
                                onValueChange = onSpouseNameChange,
                                label = "Spouse Name & Address"
                            )
                        }
                    }

                    4 -> {
                        FieldCard {
                            Text(
                                text = "Employment",
                                style = FieldTheme.typography.title,
                                color = FieldTheme.colors.gray100
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            FieldTextField(
                                value = employerName,
                                onValueChange = onEmployerNameChange,
                                label = "Employer Name"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FieldTextField(
                                value = monthlySalary,
                                onValueChange = onMonthlySalaryChange,
                                label = "Monthly Salary (₦)"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FieldTextField(
                                value = workplaceAddress,
                                onValueChange = onWorkplaceAddressChange,
                                label = "Workplace Address"
                            )
                        }
                    }

                    5 -> {
                        FieldCard {
                            Text(
                                text = "Business & Documents",
                                style = FieldTheme.typography.title,
                                color = FieldTheme.colors.gray100
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            FieldTextField(
                                value = businessSector,
                                onValueChange = onBusinessSectorChange,
                                label = "Business Sector"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FieldTextField(
                                value = monthlyTurnover,
                                onValueChange = onMonthlyTurnoverChange,
                                label = "Monthly Turnover (₦)"
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(FieldTheme.colors.gray900, RoundedCornerShape(8.dp))
                                    .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(8.dp))
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "DOCUMENT UPLOADS",
                                        style = FieldTheme.typography.label,
                                        color = FieldTheme.colors.gray500
                                    )
                                    Text(
                                        text = "Passport Photograph — upload from device",
                                        style = FieldTheme.typography.body,
                                        color = FieldTheme.colors.gray400
                                    )
                                    Text(
                                        text = "Valid ID Card — upload from device",
                                        style = FieldTheme.typography.body,
                                        color = FieldTheme.colors.gray400
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Document uploads are managed from the Dossier Documents screen.",
                                        style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                        color = FieldTheme.colors.gray500
                                    )
                                }
                            }
                        }
                    }

                    6 -> {
                        FieldCard {
                            Text(
                                text = "Declaration",
                                style = FieldTheme.typography.title,
                                color = FieldTheme.colors.gray100
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(FieldTheme.colors.gray900, RoundedCornerShape(8.dp))
                                    .border(0.5.dp, FieldTheme.colors.gray700, RoundedCornerShape(8.dp))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "I, the undersigned, hereby declare that the information provided in this form is true and correct to the best of my knowledge. I understand my obligations as guarantor and accept full legal liability for the obligations guaranteed herein.",
                                    style = FieldTheme.typography.body,
                                    color = FieldTheme.colors.gray300
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = declarationAccepted,
                                    onCheckedChange = onDeclarationChange,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = FieldTheme.colors.purple600,
                                        uncheckedColor = FieldTheme.colors.gray600,
                                        checkmarkColor = FieldTheme.colors.gray100
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "I accept the terms of this Guarantor Declaration",
                                    style = FieldTheme.typography.body,
                                    color = FieldTheme.colors.gray300
                                )
                            }
                        }
                    }

                    7 -> {
                        FieldCard {
                            Text(
                                text = "Guarantee Limits & Bank",
                                style = FieldTheme.typography.title,
                                color = FieldTheme.colors.gray100
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            FieldTextField(
                                value = maxGuaranteeLimit,
                                onValueChange = onMaxGuaranteeLimitChange,
                                label = "Maximum Guarantee Limit (₦)",
                                isRequired = true
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FieldTextField(
                                value = chequeNumber,
                                onValueChange = onChequeNumberChange,
                                label = "Cheque Number",
                                isRequired = true
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FieldTextField(
                                value = bankName,
                                onValueChange = onBankNameChange,
                                label = "Bank Name",
                                isRequired = true
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FieldTextField(
                                value = accountNumber,
                                onValueChange = onAccountNumberChange,
                                label = "Account Number (10 digits)",
                                isRequired = true,
                                errorText = accountNumberError
                            )
                        }
                    }

                    8 -> {
                        FieldCard {
                            Text(
                                text = "Signatures & Witnesses",
                                style = FieldTheme.typography.title,
                                color = FieldTheme.colors.gray100
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "GUARANTOR SIGNATURE",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray500
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FieldSignaturePad(
                                onConfirm = onGuarantorSigConfirm,
                                onClear = onGuarantorSigClear,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (guarantorSig != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Guarantor signature captured",
                                    style = FieldTheme.typography.label.copy(fontSize = 11.sp),
                                    color = FieldTheme.colors.statusSuccess
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "WITNESS SIGNATURE",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray500
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FieldSignaturePad(
                                onConfirm = onWitnessSigConfirm,
                                onClear = onWitnessSigClear,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (witnessSig != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Witness signature captured",
                                    style = FieldTheme.typography.label.copy(fontSize = 11.sp),
                                    color = FieldTheme.colors.statusSuccess
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            FieldTextField(
                                value = witnessName,
                                onValueChange = onWitnessNameChange,
                                label = "Witness Full Name",
                                isRequired = true
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FieldTextField(
                                value = witnessDate,
                                onValueChange = onWitnessDateChange,
                                label = "Witness Date",
                                isRequired = true,
                                placeholder = "YYYY-MM-DD"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            }
        }
    }
}

@Preview(name = "Compact Phone Guarantors", widthDp = 411, heightDp = 850)
@Composable
fun PreviewGuarantorsCompact() {
    val demoBorrower = BorrowerModel(
        id = "1", org_id = "org_1", loan_officer_id = "LO_1",
        name = "Adaeze Okonkwo", phone = "08012345678", bvn = "222333444", nin = "111222333",
        status = "Active", created_at = "2026-06-18"
    )
    FieldCRMTheme {
        GuarantorsFormContent(borrower = demoBorrower, onBackClick = {}, onSaveComplete = { _, _ -> })
    }
}
