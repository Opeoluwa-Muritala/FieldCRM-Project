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
fun MdApprovalScreen(
    application: LoanApplicationModel,
    isSubmitting: Boolean = false,
    onApprove: () -> Unit,
    onAddBoardReferral: (email: String, name: String, notes: String) -> Unit,
    onBack: () -> Unit,
) {
    var showApproveDialog by remember { mutableStateOf(false) }
    var boardEmail by remember { mutableStateOf("") }
    var boardName by remember { mutableStateOf("") }
    var boardNotes by remember { mutableStateOf("") }
    var showBoardConfirm by remember { mutableStateOf(false) }

    val amount = application.amount ?: 0.0

    if (showApproveDialog) {
        AlertDialog(
            onDismissRequest = { showApproveDialog = false },
            title = { Text("Issue Final Approval?") },
            text = { Text("This grants final MD approval and issues a disbursement instruction. This action cannot be undone.") },
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

    if (showBoardConfirm) {
        AlertDialog(
            onDismissRequest = { showBoardConfirm = false },
            title = { Text("Send Advisory Request?") },
            text = {
                Text("Send an advisory request to ${boardName.ifBlank { boardEmail }}? " +
                    "Their opinion is non-binding and does not block approval.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showBoardConfirm = false
                    onAddBoardReferral(boardEmail, boardName, boardNotes)
                    boardEmail = ""; boardName = ""; boardNotes = ""
                }) {
                    Text("Send", color = FieldTheme.colors.purple600)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBoardConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "MD Approval",
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
                LabelValue("Source", "Forwarded by Executive Director")
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

            SectionCard(title = "Final Approval") {
                Text(
                    text = "This file was forwarded to you by the Executive Director for final MD approval.",
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
            }

            SectionCard(title = "Board Advisory (Optional)") {
                Text(
                    text = "Request a non-binding opinion from a board member. This does not pause the approval process.",
                    style = FieldTheme.typography.body,
                    color = FieldTheme.colors.gray400
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = boardEmail,
                    onValueChange = { boardEmail = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Board Member Email") },
                    placeholder = { Text("e.g. boardchair@mmfb.com", color = FieldTheme.colors.gray600) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FieldTheme.colors.purple600,
                        unfocusedBorderColor = FieldTheme.colors.gray700
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = boardName,
                    onValueChange = { boardName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Board Member Name") },
                    placeholder = { Text("e.g. Dr. Adeyemi", color = FieldTheme.colors.gray600) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FieldTheme.colors.purple600,
                        unfocusedBorderColor = FieldTheme.colors.gray700
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = boardNotes,
                    onValueChange = { boardNotes = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    label = { Text("Message (optional)") },
                    placeholder = { Text("Context for the board member…", color = FieldTheme.colors.gray600) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FieldTheme.colors.purple600,
                        unfocusedBorderColor = FieldTheme.colors.gray700
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                SecondaryButton(
                    text = "Send Advisory Request",
                    onClick = { showBoardConfirm = true },
                    enabled = boardEmail.isNotBlank() && !isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
