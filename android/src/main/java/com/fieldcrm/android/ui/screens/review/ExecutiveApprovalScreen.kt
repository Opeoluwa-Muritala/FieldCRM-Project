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
 * Executive (MD / ED) disbursement instruction screen.
 * Shown when a loan is in the executive_approval stage.
 * The executive reviews the CRM-prepared file and issues or declines the instruction.
 */
@Composable
fun ExecutiveApprovalScreen(
    application: LoanApplicationModel,
    documents: List<Map<String, Any>> = emptyList(),
    crmNotes: String = "",
    isSubmitting: Boolean = false,
    onIssueInstruction: () -> Unit,
    onBack: () -> Unit,
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            FieldTopAppBar(title = "Disbursement Instruction", onBackClick = onBack)
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
                LabelValue("Applicant", application.applicantName)
                LabelValue("Ref",       application.refNo ?: "—")
                LabelValue("Amount",    application.amount?.let { "₦%,.0f".format(it) } ?: "—")
                LabelValue("Tenor",     application.tenure?.let { "$it months" } ?: "—")
                LabelValue("Type",      application.productType?.uppercase() ?: "—")
            }

            if (crmNotes.isNotBlank()) {
                SectionCard(title = "CRM Notes") {
                    Text(crmNotes, style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)
                }
            }

            if (documents.isNotEmpty()) {
                SectionCard(title = "Document Summary") {
                    documents.forEach { doc ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                (doc["doc_type"] as? String ?: "—")
                                    .replace("_", " ").replaceFirstChar { it.uppercase() },
                                style = FieldTheme.typography.body,
                                modifier = Modifier.weight(1f)
                            )
                            val verified = doc["verified"] as? Boolean ?: false
                            StatusChip(
                                label = if (verified) "Verified" else "Pending",
                                isPositive = verified
                            )
                        }
                    }
                }
            }

            // Authorisation card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E9)
                )
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Issue Disbursement Instruction",
                        fontWeight = FontWeight.SemiBold,
                        color = FieldTheme.colors.gray100
                    )
                    Text(
                        "By confirming, you authorise the CRM to proceed with disbursement of " +
                        "${application.amount?.let { "₦%,.0f".format(it) } ?: "the approved amount"} " +
                        "to ${application.applicantName}. This action is logged and cannot be undone.",
                        style = FieldTheme.typography.body,
                        color = FieldTheme.colors.gray400
                    )
                }
            }

            PrimaryButton(
                text = if (isSubmitting) "Submitting…" else "✓ Issue Disbursement Instruction",
                enabled = !isSubmitting,
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth()
            )

            SecondaryButton(
                text = "Cancel",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Disbursement Instruction") },
            text = {
                Text("Issue disbursement instruction for ${application.applicantName}? This cannot be reversed.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    onIssueInstruction()
                }) { Text("Confirm", color = FieldTheme.colors.purple600, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
}
