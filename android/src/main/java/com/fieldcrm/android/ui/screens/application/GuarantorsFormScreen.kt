package com.fieldcrm.android.ui.screens.application

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import com.fieldcrm.android.ui.viewmodel.BorrowerViewModel
import com.fieldcrm.shared.model.BorrowerModel
import java.util.Locale

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
    var isGuarantor1Expanded by remember { mutableStateOf(true) }
    var isGuarantor2Expanded by remember { mutableStateOf(false) }

    var g1Name by remember { mutableStateOf(borrower.guarantor_name ?: "") }
    var g1Bvn by remember { mutableStateOf("22244455588") } // 11 digits
    var g1Phone by remember { mutableStateOf(borrower.guarantor_phone ?: "") }

    var g2Name by remember { mutableStateOf("Adaeze Okonkwo") }
    var g2Bvn by remember { mutableStateOf("22233344499") } // 11 digits
    var g2Phone by remember { mutableStateOf("08099988877") }

    val g1BvnError = if (g1Bvn.isNotEmpty() && g1Bvn.length != 11) "BVN must be exactly 11 digits" else null
    val g2BvnError = if (g2Bvn.isNotEmpty() && g2Bvn.length != 11) "BVN must be exactly 11 digits" else null

    val isG1Valid = g1Name.isNotEmpty() && g1Phone.isNotEmpty() && g1Bvn.length == 11
    val isG2Valid = g2Name.isNotEmpty() && g2Phone.isNotEmpty() && g2Bvn.length == 11
    val isFormValid = isG1Valid && isG2Valid

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "Co-Guarantor Declarations",
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Guarantors Form",
                            style = FieldTheme.typography.title,
                            color = FieldTheme.colors.gray100
                        )
                        Text(
                            text = "Ensure each guarantor signature is physically matched and verified with state identity registries.",
                            style = FieldTheme.typography.body,
                            color = FieldTheme.colors.gray400
                        )
                        
                        // Guarantor 1 Card
                        Column {
                            AccordionHeader(
                                title = "Guarantor 1: ${g1Name.ifEmpty { "New Profile" }}",
                                isExpanded = isGuarantor1Expanded,
                                onToggle = { isGuarantor1Expanded = !isGuarantor1Expanded }
                            )
                            if (isGuarantor1Expanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                FieldCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Identity Parameters", style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray300)
                                        StatusChip(variant = if (isG1Valid) StatusChipVariant.Verified else StatusChipVariant.NeedsReview)
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    FieldTextField(
                                        value = g1Name,
                                        onValueChange = { g1Name = it },
                                        label = "Full Legal Name",
                                        isRequired = true
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    FieldTextField(
                                        value = g1Phone,
                                        onValueChange = { g1Phone = it },
                                        label = "Primary Phone",
                                        isRequired = true
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    FieldTextField(
                                        value = g1Bvn,
                                        onValueChange = { g1Bvn = it },
                                        label = "BVN Identifier Number",
                                        isRequired = true,
                                        errorText = g1BvnError
                                    )
                                }
                            }
                        }

                        // Guarantor 2 Card
                        Column {
                            AccordionHeader(
                                title = "Guarantor 2: ${g2Name.ifEmpty { "New Profile" }}",
                                isExpanded = isGuarantor2Expanded,
                                onToggle = { isGuarantor2Expanded = !isGuarantor2Expanded }
                            )
                            if (isGuarantor2Expanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                FieldCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Identity Parameters", style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray300)
                                        StatusChip(variant = if (isG2Valid) StatusChipVariant.Verified else StatusChipVariant.NeedsReview)
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    FieldTextField(
                                        value = g2Name,
                                        onValueChange = { g2Name = it },
                                        label = "Full Legal Name",
                                        isRequired = true
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    FieldTextField(
                                        value = g2Phone,
                                        onValueChange = { g2Phone = it },
                                        label = "Primary Phone",
                                        isRequired = true
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    FieldTextField(
                                        value = g2Bvn,
                                        onValueChange = { g2Bvn = it },
                                        label = "BVN Identifier Number",
                                        isRequired = true,
                                        errorText = g2BvnError
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        PrimaryButton(
                            text = "Save Guarantors Configuration",
                            onClick = { onSaveComplete(g1Name, g1Phone) },
                            enabled = isFormValid
                        )
                    }
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
