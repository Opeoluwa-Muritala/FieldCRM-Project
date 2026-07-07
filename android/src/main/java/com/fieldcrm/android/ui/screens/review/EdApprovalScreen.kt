package com.fieldcrm.android.ui.screens.review

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.shared.model.LoanApplicationModel
import java.util.Locale

@Composable
fun EdApprovalScreen(
    application: LoanApplicationModel,
    isSubmitting: Boolean = false,
    onApprove: () -> Unit,
    onForwardToMd: () -> Unit,
    onBack: () -> Unit,
) {
    var showApproveDialog by remember { mutableStateOf(false) }
    var showForwardDialog by remember { mutableStateOf(false) }

    val amount = application.amount ?: 0.0
    val isSmallLoan = amount < 10_000_000

    if (showApproveDialog) {
        AlertDialog(
            onDismissRequest = { showApproveDialog = false },
            title = { Text("Issue Disbursement Instruction?") },
            text = { Text("This approves the loan and issues a disbursement instruction. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { showApproveDialog = false; onApprove() }) {
                    Text("Approve", color = FieldTheme.colors.statusSuccess)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApproveDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showForwardDialog) {
        AlertDialog(
            onDismissRequest = { showForwardDialog = false },
            title = { Text("Forward to MD?") },
            text = { Text("This forwards the file to the Managing Director for final approval.") },
            confirmButton = {
                TextButton(onClick = { showForwardDialog = false; onForwardToMd() }) {
                    Text("Forward", color = FieldTheme.colors.purple600)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForwardDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "ED Approval",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = FieldIcons.ArrowBackOutlined,
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
                LabelValue("Ref", application.ref_no.ifBlank { application.id.take(8).uppercase() })
                LabelValue("Amount", "₦${String.format(Locale.US, "%,.0f", amount)}")
                LabelValue("Tenor", application.tenor_months?.let { "$it months" } ?: "—")
                LabelValue("Purpose", application.purpose ?: "—")
                LabelValue("Threshold", if (isSmallLoan) "< ₦10M — ED authority" else ">= ₦10M — may forward to MD")
            }

            application.crm_notes?.takeIf { it.isNotBlank() }?.let { crmNotes ->
                SectionCard(title = "CRM Notes") {
                    Text(
                        text = crmNotes,
                        style = FieldTheme.typography.body,
                        color = FieldTheme.colors.gray300
                    )
                }
            }

            SectionCard(title = "Decision") {
                Text(
                    text = if (isSmallLoan)
                        "This loan is within your direct approval authority (< ₦10M). You may approve directly or forward to the MD."
                    else
                        "This loan exceeds ₦10M. You may still forward to the MD for final approval.",
                    style = FieldTheme.typography.body,
                    color = FieldTheme.colors.gray400
                )
                Spacer(modifier = Modifier.height(16.dp))

                PrimaryButton(
                    text = if (isSubmitting) "Processing…" else "Approve — Issue Disbursement Instruction",
                    onClick = { showApproveDialog = true },
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                SecondaryButton(
                    text = if (isSubmitting) "Processing…" else "Forward to MD for Final Approval",
                    onClick = { showForwardDialog = true },
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
