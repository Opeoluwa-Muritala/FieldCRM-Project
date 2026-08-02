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
import com.fieldcrm.shared.model.LoanApplicationModel
import java.util.Locale
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MdApprovalScreen(
    application: LoanApplicationModel,
    isSubmitting: Boolean = false,
    onApprove: () -> Unit,
    onReturnToEd: (String) -> Unit,
    onAddBoardReferral: (email: String, name: String, notes: String) -> Unit,
    onBack: () -> Unit,
) {
    val mccViewModel: MccEvidenceViewModel = koinViewModel()
    val mccState by mccViewModel.uiState.collectAsState()
    LaunchedEffect(application.id) { mccViewModel.load(application.id) }
    var finalAmount by remember { mutableStateOf(application.amount?.toString() ?: "") }
    var approvalNotes by remember { mutableStateOf("") }
    
    // Board Advisory States
    var boardEmail by remember { mutableStateOf("") }
    var boardName by remember { mutableStateOf("") }
    var boardNotes by remember { mutableStateOf("") }
    var referralList by remember { mutableStateOf(emptyList<String>()) }
    
    var showApproveDialog by remember { mutableStateOf(false) }
    var showReturnDialog by remember { mutableStateOf(false) }
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
            },
            containerColor = FieldTheme.colors.gray900
        )
    }

    if (showReturnDialog) {
        AlertDialog(
            onDismissRequest = { showReturnDialog = false },
            title = { Text("Return to ED?") },
            text = { Text("This will return the application to the Executive Director for further review.") },
            confirmButton = {
                TextButton(onClick = { 
                    showReturnDialog = false
                    onReturnToEd(approvalNotes) 
                }) {
                    Text("Return", color = FieldTheme.colors.statusDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReturnDialog = false }) { Text("Cancel") }
            },
            containerColor = FieldTheme.colors.gray900
        )
    }

    if (showBoardConfirm) {
        AlertDialog(
            onDismissRequest = { showBoardConfirm = false },
            title = { Text("Send Advisory Request?") },
            text = {
                Text("Send an advisory request to ${boardName.ifBlank { boardEmail }}? Their opinion is non-binding.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showBoardConfirm = false
                    onAddBoardReferral(boardEmail, boardName, boardNotes)
                    referralList = referralList + "Referral to $boardName (${boardEmail}) — SENT"
                    boardEmail = ""; boardName = ""; boardNotes = ""
                }) {
                    Text("Send", color = FieldTheme.colors.purple600)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBoardConfirm = false }) { Text("Cancel") }
            },
            containerColor = FieldTheme.colors.gray900
        )
    }

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "MD Decision Screen",
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
                    "Tapping Approve issues the final disbursement instruction. Return comment routes the dossier back to the ED."
                )
                // A7 Distinct: 3-Button Action Layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { if (boardEmail.isNotBlank()) showBoardConfirm = true },
                        enabled = boardEmail.isNotBlank() && !isSubmitting,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Board Advice", color = if (boardEmail.isNotBlank()) FieldTheme.colors.purple400 else FieldTheme.colors.gray600)
                    }
                    OutlinedButton(
                        onClick = { showReturnDialog = true },
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Text("Return to ED", color = FieldTheme.colors.gray300)
                    }
                    PrimaryButton(
                        text = if (isSubmitting) "Processing..." else "Final Approve",
                        onClick = { showApproveDialog = true },
                        enabled = !isSubmitting,
                        modifier = Modifier.weight(1.3f)
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

                    Divider(color = FieldTheme.colors.gray800)

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
                                Text(vote.memberName, style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray100)
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

            // Z4.3 Editable Final Amount formulation
            SectionCard(title = "Approved Amount Formulation") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Verify or modify the final approved loan amount (₦):",
                        style = FieldTheme.typography.label,
                        color = FieldTheme.colors.gray400
                    )
                    OutlinedTextField(
                        value = finalAmount,
                        onValueChange = { finalAmount = it },
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

            // Z4.4 Approval Notes
            SectionCard(title = "Approval Notes") {
                OutlinedTextField(
                    value = approvalNotes,
                    onValueChange = { approvalNotes = it },
                    placeholder = { Text("Enter final MD decision comments or return reason notes...") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = FieldTheme.colors.gray900,
                        unfocusedContainerColor = FieldTheme.colors.gray900,
                        focusedTextColor = FieldTheme.colors.gray100,
                        unfocusedTextColor = FieldTheme.colors.gray100
                    )
                )
            }

            // Z4.5 Board Referral Form & Status List
            SectionCard(title = "Board Advisory & Referral Form") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Request advisory opinion from a board member. This is optional and does not block approval.",
                        style = FieldTheme.typography.label,
                        color = FieldTheme.colors.gray400
                    )
                    OutlinedTextField(
                        value = boardEmail,
                        onValueChange = { boardEmail = it },
                        label = { Text("Board Member Email") },
                        placeholder = { Text("boardchair@mainstreetmfb.com") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(focusedContainerColor = FieldTheme.colors.gray900, unfocusedContainerColor = FieldTheme.colors.gray900, focusedTextColor = FieldTheme.colors.gray100, unfocusedTextColor = FieldTheme.colors.gray100)
                    )
                    OutlinedTextField(
                        value = boardName,
                        onValueChange = { boardName = it },
                        label = { Text("Board Member Name") },
                        placeholder = { Text("Board member name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(focusedContainerColor = FieldTheme.colors.gray900, unfocusedContainerColor = FieldTheme.colors.gray900, focusedTextColor = FieldTheme.colors.gray100, unfocusedTextColor = FieldTheme.colors.gray100)
                    )
                    OutlinedTextField(
                        value = boardNotes,
                        onValueChange = { boardNotes = it },
                        label = { Text("Referral Message / Context") },
                        placeholder = { Text("Please review the affordability metrics of this dossier...") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        colors = TextFieldDefaults.colors(focusedContainerColor = FieldTheme.colors.gray900, unfocusedContainerColor = FieldTheme.colors.gray900, focusedTextColor = FieldTheme.colors.gray100, unfocusedTextColor = FieldTheme.colors.gray100)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Board Referral History & Status", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                    Divider(color = FieldTheme.colors.gray800)
                    
                    referralList.forEach { ref ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FieldTheme.colors.gray900, RoundedCornerShape(4.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = FieldIcons.InfoOutlined,
                                contentDescription = "Referral",
                                tint = FieldTheme.colors.purple400,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(ref, style = FieldTheme.typography.body, color = FieldTheme.colors.gray300)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
