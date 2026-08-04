package com.fieldcrm.android.ui.screens.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.screens.common.DetailItem
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.viewmodel.MccEvidenceViewModel
import com.fieldcrm.android.ui.viewmodel.ApplicationViewModel
import com.fieldcrm.shared.model.LoanApplicationModel
import java.util.Locale
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdApprovalScreen(
    application: LoanApplicationModel,
    isSubmitting: Boolean = false,
    onApprove: () -> Unit,
    onForwardToMd: () -> Unit,
    onBack: () -> Unit,
) {
    val applicationViewModel: ApplicationViewModel = koinViewModel()
    val applicationState by applicationViewModel.uiState.collectAsState()
    LaunchedEffect(application.id) { applicationViewModel.loadApplicationDetail(application.id) }
    val mccViewModel: MccEvidenceViewModel = koinViewModel()
    val mccState by mccViewModel.uiState.collectAsState()
    LaunchedEffect(application.id) { mccViewModel.load(application.id) }
    var approvedAmount by remember { mutableStateOf(application.amount?.toString() ?: "") }
    var notes by remember { mutableStateOf("") }
    var showApproveDialog by remember { mutableStateOf(false) }
    var showForwardDialog by remember { mutableStateOf(false) }

    val amount = application.amount ?: 0.0
    val isSmallLoan = amount < 10_000_000

    if (showApproveDialog) {
        ReviewDecisionSheet(
            title = "Issue disbursement instruction?",
            message = "This records an approval and issues a disbursement instruction. Confirm only after the full dossier is complete.",
            confirmLabel = "Approve and issue instruction",
            onConfirm = { showApproveDialog = false; onApprove() },
            onDismiss = { showApproveDialog = false }
        )
    }

    if (showForwardDialog) {
        ReviewDecisionSheet(
            title = "Forward to Managing Director?",
            message = "The file will move to the Managing Director’s approval queue with its complete review history.",
            confirmLabel = "Forward to Managing Director",
            onConfirm = { showForwardDialog = false; onForwardToMd() },
            onDismiss = { showForwardDialog = false }
        )
    }

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "ED Decision Screen",
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
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FieldTheme.colors.gray950)
                    .border(width = 0.5.dp, color = FieldTheme.colors.gray800)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DecisionImpactNotice(
                    "Approval registers a final ED authorization. Requesting MD input escalates the file to the MD decision desk."
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showForwardDialog = true },
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("Request MD Input", color = FieldTheme.colors.gray300)
                    }
                    PrimaryButton(
                        text = if (isSubmitting) "Processing..." else "Approve Decision",
                        onClick = { showApproveDialog = true },
                        enabled = !isSubmitting,
                        modifier = Modifier.weight(1.5f)
                    )
                }
            }
        },
        containerColor = FieldTheme.colors.gray950
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Z2: Summary Card
            FieldCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = application.applicant_name ?: "Unknown Applicant",
                                style = FieldTheme.typography.bodyStrong,
                                color = FieldTheme.colors.gray100
                            )
                            Text(
                                text = "Ref: ${application.ref_no}",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray400
                            )
                        }
                        StatusChip(label = application.displayStatus)
                    }

                    HorizontalDivider(color = FieldTheme.colors.gray800)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "REQUESTED AMOUNT",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray400
                            )
                            Text(
                                text = "₦${String.format(Locale.US, "%,.2f", amount)}",
                                style = FieldTheme.typography.display.copy(fontSize = 28.sp),
                                color = FieldTheme.colors.purple400
                            )
                        }
                    }
                }
            }

            DatabaseDossierSections(applicationState.selectedAppDetail, applicationState.isLoadingDetail)

            // Z4.1 MCC Recommendations (Votes, amounts, notes)
            SectionCard(title = "MCC Recommendations & Votes") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (mccState.isLoading) LoadingSkeleton(height = 72.dp, width = 280.dp)
                    mccState.errorMessage?.let { Text(it, style = FieldTheme.typography.body, color = FieldTheme.colors.statusDanger) }
                    if (!mccState.isLoading && mccState.errorMessage == null && mccState.recommendations.isEmpty()) {
                        Text("No MCC recommendations have been recorded.", style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)
                    }
                    mccState.recommendations.forEach { vote ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FieldTheme.colors.gray900, RoundedCornerShape(4.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(vote.memberName, style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray300)
                                Text(vote.recommendedAmount?.let { "NGN ${String.format(Locale.US, "%,.2f", it)}" } ?: "Amount not available", style = FieldTheme.typography.mono, color = FieldTheme.colors.purple400)
                            }
                            Text(vote.recommendation, style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray300)
                            if (vote.notes.isNotBlank()) Text(vote.notes, style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)
                        }
                    }
                }
            }

            // Z4.2 Document Summary (Evidence list)
            SectionCard(title = "Document Evidence Summary") {
                Text("Open the dossier Documents section for current database-backed evidence.", style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)
            }

            // Z4.3 Editable Approved Amount formulation
            SectionCard(title = "Approved Amount Formulation") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Verify or modify the final approved loan amount (₦):",
                        style = FieldTheme.typography.label,
                        color = FieldTheme.colors.gray400
                    )
                    OutlinedTextField(
                        value = approvedAmount,
                        onValueChange = { approvedAmount = it },
                        placeholder = { Text("Enter final approved amount...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = FieldTheme.colors.gray900,
                            unfocusedContainerColor = FieldTheme.colors.gray900,
                            focusedTextColor = FieldTheme.colors.gray100,
                            unfocusedTextColor = FieldTheme.colors.gray100
                        )
                    )
                }
            }

            // Z4.4 Approval Comments
            SectionCard(title = "Approval Comments") {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Enter final ED approval decision comments or referral reason notes...") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = FieldTheme.colors.gray900,
                        unfocusedContainerColor = FieldTheme.colors.gray900,
                        focusedTextColor = FieldTheme.colors.gray100,
                        unfocusedTextColor = FieldTheme.colors.gray100
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
