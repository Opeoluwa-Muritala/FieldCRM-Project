package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.shared.model.BorrowerModel

data class WizardTab(val index: Int, val name: String, val icon: String)

@Composable
fun LoanApplicationFormScreen(
    onBackClick: () -> Unit,
    onFinish: () -> Unit
) {
    var currentTab by remember { mutableIntStateOf(0) }
    
    val tabs = listOf(
        WizardTab(0, "Personal Details", "01"),
        WizardTab(1, "ID & Contact", "02"),
        WizardTab(2, "Employment", "03"),
        WizardTab(3, "Income & Expense", "04"),
        WizardTab(4, "Loan Request", "05"),
        WizardTab(5, "Collateral Info", "06"),
        WizardTab(6, "Guarantors", "07"),
        WizardTab(7, "Documents", "08"),
        WizardTab(8, "Final Review", "09")
    )

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
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                    // Left panel: Side-rail progress showing 9 steps
                    Column(
                        modifier = Modifier
                            .width(260.dp)
                            .fillMaxHeight()
                            .background(FieldTheme.colors.gray900)
                            .borderRight(0.5.dp, FieldTheme.colors.gray700)
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
                                            imageVector = Icons.Default.Check,
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
                
                // Right panel: Content block of active tab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        TabContent(currentTab)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Wizard Navigation Footer
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
                                if (currentTab < 8) currentTab++ else onFinish()
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
fun TabContent(tabIndex: Int) {
    var nameInput by remember { mutableStateOf("Emeka Chukwu") }
    var phoneInput by remember { mutableStateOf("08012345678") }
    var bvnInput by remember { mutableStateOf("222333444") }
    var addressInput by remember { mutableStateOf("No. 12 Airport Road") }
    var amountInput by remember { mutableStateOf("450000") }
    var tenureInput by remember { mutableStateOf("12") }
    var dobInput by remember { mutableStateOf("1992-04-12") }

    when (tabIndex) {
        0 -> {
            FieldCard {
                Text("Primary Personal Details", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                Spacer(modifier = Modifier.height(16.dp))
                FieldTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = "Applicant Full Name",
                    isRequired = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                 FieldTextField(
                     value = dobInput,
                     onValueChange = { dobInput = it },
                     label = "Date of Birth",
                     isRequired = true
                 )
                Spacer(modifier = Modifier.height(16.dp))
                FieldDropdown(
                    value = "Married",
                    options = listOf("Single", "Married", "Widowed", "Divorced"),
                    onOptionSelected = {},
                    label = "Marital Status"
                )
            }
        }
        1 -> {
            FieldCard {
                Text("Verification & Contacts", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                Spacer(modifier = Modifier.height(16.dp))
                FieldTextField(
                    value = bvnInput,
                    onValueChange = { bvnInput = it },
                    label = "Bank Verification Number (BVN)",
                    isRequired = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                FieldTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = "Phone Number",
                    isRequired = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                FieldTextField(
                    value = addressInput,
                    onValueChange = { addressInput = it },
                    label = "Residential Address",
                    isRequired = true
                )
            }
        }
        2 -> {
            FieldCard {
                Text("Employment Information", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                Spacer(modifier = Modifier.height(16.dp))
                FieldDropdown(
                    value = "Self Employed",
                    options = listOf("Public Service", "Private Sector", "Self Employed", "Unemployed"),
                    onOptionSelected = {},
                    label = "Employment Status"
                )
                Spacer(modifier = Modifier.height(16.dp))
                FieldTextField(
                    value = "Mainstreet Food Stalls Ltd",
                    onValueChange = {},
                    label = "Employer/Business Name"
                )
                Spacer(modifier = Modifier.height(16.dp))
                FieldTextField(
                    value = "General Manager",
                    onValueChange = {},
                    label = "Job Title/Role"
                )
            }
        }
        3 -> {
            FieldCard {
                Text("Income Statement & Financials", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                Spacer(modifier = Modifier.height(16.dp))
                FieldAmountField(
                    value = "350000",
                    onValueChange = {},
                    label = "Monthly Average Income",
                    isRequired = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                FieldAmountField(
                    value = "120000",
                    onValueChange = {},
                    label = "Monthly Operational Expenses",
                    isRequired = true
                )
            }
        }
        4 -> {
            FieldCard {
                Text("Principal Loan Request", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                Spacer(modifier = Modifier.height(16.dp))
                FieldAmountField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = "Requested Loan Principal",
                    isRequired = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                FieldTextField(
                    value = tenureInput,
                    onValueChange = { tenureInput = it },
                    label = "Requested Tenure (Months)",
                    isRequired = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                FieldDropdown(
                    value = "Working Capital",
                    options = listOf("Working Capital", "Asset Purchase", "Inventory Expansion", "Education"),
                    onOptionSelected = {},
                    label = "Loan Product Segment"
                )
            }
        }
        5 -> {
            FieldCard {
                Text("Collateral Evaluation", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                Spacer(modifier = Modifier.height(16.dp))
                FieldTextField(
                    value = "Inventory pledge on trade goods and storage kiosk",
                    onValueChange = {},
                    label = "Collateral Description",
                    isRequired = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                FieldAmountField(
                    value = "900000",
                    onValueChange = {},
                    label = "Estimated Market Value",
                    isRequired = true
                )
            }
        }
        6 -> {
            FieldCard {
                Text("Co-Guarantor Profiles", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                Spacer(modifier = Modifier.height(16.dp))
                FieldTextField(
                    value = "Tunde Bakare",
                    onValueChange = {},
                    label = "Guarantor Full Name",
                    isRequired = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                FieldTextField(
                    value = "22244455588",
                    onValueChange = {},
                    label = "Guarantor BVN Identifier",
                    isRequired = true
                )
            }
        }
        7 -> {
            FieldCard {
                Text("Document Attachments", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                Spacer(modifier = Modifier.height(16.dp))
                 FieldUploadDropzone(
                     title = "Upload Applicant Utility Bill",
                     subtitle = "PDF or PNG format up to 5MB",
                     onClick = {}
                 )
                Spacer(modifier = Modifier.height(12.dp))
                DocumentThumbnail(
                    fileName = "Identity_Verification_NIN_Emeka.pdf",
                    fileSize = "1.8 MB",
                    fileType = "pdf"
                )
            }
        }
        else -> {
            FieldCard {
                Text("Dossier Summary Review", style = FieldTheme.typography.title, color = FieldTheme.colors.gray100)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Please confirm that all extracted identity parameters match physical documents prior to triggering underwriting pipeline.", style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)
                Spacer(modifier = Modifier.height(16.dp))
                ReadinessChecklist(
                    gates = listOf(
                        ChecklistGate("BVN Verification Status", true, StatusChipVariant.Verified),
                        ChecklistGate("Employment Reference Stamped", true, StatusChipVariant.Verified),
                        ChecklistGate("Guarantor Registry Validation", false, StatusChipVariant.Missing)
                    )
                )
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(name = "Compact Phone Form Wizard", widthDp = 411, heightDp = 850)
@Composable
fun PreviewWizardCompact() {
    FieldCRMTheme {
        LoanApplicationFormScreen(onBackClick = {}, onFinish = {})
    }
}

@Preview(name = "Tablet Form Wizard Layout", widthDp = 1280, heightDp = 800)
@Composable
fun PreviewWizardTablet() {
    FieldCRMTheme {
        LoanApplicationFormScreen(onBackClick = {}, onFinish = {})
    }
}
