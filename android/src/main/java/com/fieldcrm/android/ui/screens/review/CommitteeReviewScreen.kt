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
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.shared.model.LoanApplicationModel
import java.util.Locale

@Composable
fun CommitteeReviewScreen(
    application: LoanApplicationModel,
    isSubmitting: Boolean = false,
    onSubmitVote: (recommendation: String, notes: String) -> Unit,
    onCompleteReview: (recommendation: String) -> Unit,
    onBack: () -> Unit,
) {
    var recommendation by remember { mutableStateOf("approve") }
    var notes by remember { mutableStateOf("") }
    var chairRecommendation by remember { mutableStateOf("approve") }
    var showCompleteDialog by remember { mutableStateOf(false) }

    if (showCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteDialog = false },
            title = { Text("Complete Committee Review?") },
            text = {
                Text("This submits the committee's final recommendation (${
                    if (chairRecommendation == "approve") "Approve" else "Return"
                }) and routes the file to the next approver.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showCompleteDialog = false
                    onCompleteReview(chairRecommendation)
                }) {
                    Text("Confirm", color = FieldTheme.colors.purple600)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "Committee Review",
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
                LabelValue("Amount", application.amount?.let { "₦${String.format(Locale.US, "%,.0f", it)}" } ?: "—")
                LabelValue("Tenor", application.tenor_months?.let { "$it months" } ?: "—")
                LabelValue("Purpose", application.purpose ?: "—")
                LabelValue("Loan Type", application.loan_type.replaceFirstChar { it.uppercase() })
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

            SectionCard(title = "My Vote") {
                Text(
                    text = "Recommendation",
                    style = FieldTheme.typography.label,
                    color = FieldTheme.colors.gray500
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = recommendation == "approve",
                        onClick = { recommendation = "approve" },
                        label = { Text("Approve") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FieldTheme.colors.statusSuccess.copy(alpha = 0.2f),
                            selectedLabelColor = FieldTheme.colors.statusSuccess
                        )
                    )
                    FilterChip(
                        selected = recommendation == "return",
                        onClick = { recommendation = "return" },
                        label = { Text("Return") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FieldTheme.colors.statusDanger.copy(alpha = 0.2f),
                            selectedLabelColor = FieldTheme.colors.statusDanger
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("Add your observations…", color = FieldTheme.colors.gray500) },
                    label = { Text("Notes") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FieldTheme.colors.purple600,
                        unfocusedBorderColor = FieldTheme.colors.gray700
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                PrimaryButton(
                    text = if (isSubmitting) "Submitting…" else "Submit My Vote",
                    onClick = { onSubmitVote(recommendation, notes) },
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SectionCard(title = "Chair: Complete Review") {
                Text(
                    text = "If you are the committee chair, submit the committee's final recommendation once all members have voted.",
                    style = FieldTheme.typography.body,
                    color = FieldTheme.colors.gray400
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Final Recommendation",
                    style = FieldTheme.typography.label,
                    color = FieldTheme.colors.gray500
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = chairRecommendation == "approve",
                        onClick = { chairRecommendation = "approve" },
                        label = { Text("Approve") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FieldTheme.colors.statusSuccess.copy(alpha = 0.2f),
                            selectedLabelColor = FieldTheme.colors.statusSuccess
                        )
                    )
                    FilterChip(
                        selected = chairRecommendation == "return",
                        onClick = { chairRecommendation = "return" },
                        label = { Text("Return") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FieldTheme.colors.statusDanger.copy(alpha = 0.2f),
                            selectedLabelColor = FieldTheme.colors.statusDanger
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                SecondaryButton(
                    text = if (isSubmitting) "Processing…" else "Complete Committee Review",
                    onClick = { showCompleteDialog = true },
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
