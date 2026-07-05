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

/**
 * CRM credit file completeness review — CBN §1.6 gate before executive disbursement.
 * All four bureau / CRMS checkboxes must be ticked to advance.
 */
@Composable
fun CrmReviewScreen(
    application: LoanApplicationModel,
    isSubmitting: Boolean = false,
    onAdvanceToExecutive: () -> Unit,
    onReturnToBranchManager: () -> Unit,
    onBack: () -> Unit,
) {
    var bureau1 by remember { mutableStateOf(false) }
    var bureau2 by remember { mutableStateOf(false) }
    var crmsSearch by remember { mutableStateOf(false) }
    var ncrReg by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var showReturnDialog by remember { mutableStateOf(false) }

    val allChecked = bureau1 && bureau2 && crmsSearch && ncrReg

    if (showReturnDialog) {
        AlertDialog(
            onDismissRequest = { showReturnDialog = false },
            title = { Text("Return to Branch Manager?") },
            text = { Text("This will send the file back to the Branch Manager for correction.") },
            confirmButton = {
                TextButton(onClick = { showReturnDialog = false; onReturnToBranchManager() }) {
                    Text("Return", color = FieldTheme.colors.statusDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReturnDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = { 
            FieldTopAppBar(
                title = "CRM Review",
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
                LabelValue("Tenor",     application.tenure?.let { "$it months" } ?: "—")
                LabelValue("Stage",     application.current_stage.toString())
            }

            SectionCard(title = "CBN §1.6 Credit File Checklist") {
                ChecklistItem(
                    label = "Credit Bureau 1 (CRC / FirstCentral) search obtained",
                    checked = bureau1,
                    onCheckedChange = { bureau1 = it }
                )
                ChecklistItem(
                    label = "Credit Bureau 2 (CreditRegistry) search obtained",
                    checked = bureau2,
                    onCheckedChange = { bureau2 = it }
                )
                ChecklistItem(
                    label = "CRMS (CBN Credit Risk Management System) search done",
                    checked = crmsSearch,
                    onCheckedChange = { crmsSearch = it }
                )
                ChecklistItem(
                    label = "NCR (National Collateral Registry) registration verified",
                    checked = ncrReg,
                    onCheckedChange = { ncrReg = it }
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

            PrimaryButton(
                text = if (isSubmitting) "Submitting…" else "Advance to Executive Approval",
                onClick = onAdvanceToExecutive,
                enabled = allChecked && !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            )

            SecondaryButton(
                text = "Return to Branch Manager",
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
    onCheckedChange: (Boolean) -> Unit
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
