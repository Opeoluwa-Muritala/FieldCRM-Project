package com.fieldcrm.android.ui.screens.review

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.shared.model.LoanApplicationModel
import com.fieldcrm.android.core.session.UserRole

/**
 * CRM credit file completeness review — CBN §1.6 gate before executive disbursement.
 * All four bureau / CRMS checkboxes must be ticked to advance.
 */
@Composable
fun CrmReviewScreen(
    application: LoanApplicationModel,
    role: UserRole = UserRole.CRM,
    isSubmitting: Boolean = false,
    savedChecklist: Map<String, Boolean> = emptyMap(),
    onAdvanceToExecutive: (String, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onReturnToBranchManager: (String) -> Unit,
    onUploadDocument: () -> Unit = {},
    onBack: () -> Unit,
) {
    var bureau1 by remember(savedChecklist) { mutableStateOf(savedChecklist["bureau_1_verified"] == true) }
    var bureau2 by remember(savedChecklist) { mutableStateOf(savedChecklist["bureau_2_verified"] == true) }
    var crmsSearch by remember(savedChecklist) { mutableStateOf(savedChecklist["crms_verified"] == true) }
    var ncrReg by remember(savedChecklist) { mutableStateOf(savedChecklist["ncr_verified"] == true) }
    var notes by remember { mutableStateOf("") }
    var showReturnDialog by remember { mutableStateOf(false) }

    val allChecked = bureau1 && bureau2 && crmsSearch && ncrReg

    if (showReturnDialog) {
        ReviewDecisionSheet(
            title = if (role == UserRole.HEAD_CRM) "Return this file to the CRM Officer?" else "Return this file to the Credit Analyst?",
            message = if (role == UserRole.HEAD_CRM) "The CRM Officer will receive the file for correction." else "The Credit Analyst will receive the file for correction.",
            confirmLabel = "Return for correction",
            destructive = true,
            onConfirm = { showReturnDialog = false; onReturnToBranchManager(notes) },
            onDismiss = { showReturnDialog = false }
        )
    }

    Scaffold(
        topBar = { 
            FieldTopAppBar(
                title = if (role == UserRole.HEAD_CRM) "Head CRM Approval" else "CRM Review",
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            imageVector = com.fieldcrm.android.ui.theme.FieldIcons.ArrowBackOutlined, 
                            contentDescription = "Back", 
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            ) 
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard(title = "Loan Summary") {
                LabelValue("Applicant", application.applicant_name)
                LabelValue("Ref",       application.id.take(8))
                LabelValue("Amount",    application.amount?.let { "₦%,.0f".format(it) } ?: "—")
                LabelValue("Tenor",     application.tenor_months?.let { "$it months" } ?: "—")
                LabelValue("Stage",     application.stage)
            }

            SectionCard(title = "CBN §1.6 Credit File Checklist") {
                ChecklistItem(
                    label = "Credit Bureau 1 (CRC / FirstCentral) search obtained",
                    checked = bureau1,
                    onCheckedChange = { bureau1 = it },
                    enabled = role == UserRole.CRM
                )
                ChecklistItem(
                    label = "Credit Bureau 2 (CreditRegistry) search obtained",
                    checked = bureau2,
                    onCheckedChange = { bureau2 = it },
                    enabled = role == UserRole.CRM
                )
                ChecklistItem(
                    label = "CRMS (CBN Credit Risk Management System) search done",
                    checked = crmsSearch,
                    onCheckedChange = { crmsSearch = it },
                    enabled = role == UserRole.CRM
                )
                ChecklistItem(
                    label = "NCR (National Collateral Registry) registration verified",
                    checked = ncrReg,
                    onCheckedChange = { ncrReg = it },
                    enabled = role == UserRole.CRM
                )
            }

            SectionCard(title = "CRM Notes") {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("Add notes for the Executive…", color = FieldTheme.colors.gray500) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FieldTheme.colors.purple600,
                        unfocusedBorderColor = FieldTheme.colors.gray700
                    )
                )
            }

            SectionCard(title = "Supporting Documents") {
                Text(
                    text = "Attach any additional documents required to complete this credit file.",
                    style = FieldTheme.typography.body,
                    color = FieldTheme.colors.gray400
                )
                Spacer(modifier = Modifier.height(10.dp))
                SecondaryButton(
                    text = "Upload Document",
                    onClick = onUploadDocument,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            PrimaryButton(
                text = if (isSubmitting) "Submitting…" else if (role == UserRole.HEAD_CRM) "Approve and Send to ED" else "Send to Head CRM",
                    onClick = { onAdvanceToExecutive(notes, bureau1, bureau2, crmsSearch, ncrReg) },
                enabled = allChecked && !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            )

            SecondaryButton(
                text = if (role == UserRole.HEAD_CRM) "Return to CRM Officer" else "Return to Credit Analyst",
                onClick = { showReturnDialog = true },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ChecklistItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = FieldTheme.colors.purple600,
                uncheckedColor = FieldTheme.colors.gray600
            )
        )
        Text(
            text = label,
            style = FieldTheme.typography.body,
            color = FieldTheme.colors.gray400,
            fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
