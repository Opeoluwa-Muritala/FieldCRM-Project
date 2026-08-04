package com.fieldcrm.android.ui.screens.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.fieldcrm.shared.model.LoanApplicationModel
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.ui.viewmodel.ApplicationViewModel
import java.util.Locale
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
    val applicationViewModel: ApplicationViewModel = koinViewModel()
    val applicationState by applicationViewModel.uiState.collectAsState()
    LaunchedEffect(application.id) { applicationViewModel.loadApplicationDetail(application.id) }
    var bureau1 by remember(savedChecklist) { mutableStateOf(savedChecklist["bureau_1_verified"] == true) }
    var bureau2 by remember(savedChecklist) { mutableStateOf(savedChecklist["bureau_2_verified"] == true) }
    var crmsSearch by remember(savedChecklist) { mutableStateOf(savedChecklist["crms_verified"] == true) }
    var ncrReg by remember(savedChecklist) { mutableStateOf(savedChecklist["ncr_verified"] == true) }
    
    var approvedAmount by remember { mutableStateOf(application.amount?.toString() ?: "") }
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
                    if (role == UserRole.HEAD_CRM) "Approval records Head CRM authorization and routes the dossier to the Executive Director." else "Submission records CRM completeness review and routes the dossier to Head CRM."
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showReturnDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (role == UserRole.HEAD_CRM) "Return to CRM" else "Return")
                    }
                    PrimaryButton(
                        text = if (isSubmitting) "Submitting..." else if (role == UserRole.HEAD_CRM) "Approve & Send to Executive Director" else "Advance to Head CRM",
                        onClick = { onAdvanceToExecutive(notes, bureau1, bureau2, crmsSearch, ncrReg) },
                        enabled = allChecked && !isSubmitting,
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
                                text = "₦${String.format(Locale.US, "%,.2f", application.amount ?: 0.0)}",
                                style = FieldTheme.typography.display.copy(fontSize = 28.sp),
                                color = FieldTheme.colors.purple400
                            )
                        }
                    }

                    // A5 Distinct: CRM Officer's Prior Review Badge (Only for Head CRM)
                    if (role == UserRole.HEAD_CRM) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FieldTheme.colors.purple950.copy(alpha = 0.3f), shape = MaterialTheme.shapes.small)
                                .border(width = 0.5.dp, color = FieldTheme.colors.purple600, shape = MaterialTheme.shapes.small)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = FieldIcons.CheckCircleOutlined,
                                contentDescription = "Reviewed",
                                tint = FieldTheme.colors.statusSuccess,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "CRM Officer's Prior Review Summary",
                                    style = FieldTheme.typography.label.copy(fontWeight = FontWeight.Bold),
                                    color = FieldTheme.colors.gray100
                                )
                                Text(
                                    text = "Reviewer details are not available in this response.",
                                    style = FieldTheme.typography.label,
                                    color = FieldTheme.colors.gray400
                                )
                            }
                        }
                    }
                }
            }

            DatabaseDossierSections(applicationState.selectedAppDetail, applicationState.isLoadingDetail)

            // Z4.1 Document Quality / Verification Table
            SectionCard(title = "Document Quality / Verification") {
                Text("Open the dossier Documents section for current upload, verification, and OCR states.", style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)
            }

            // Z4.2 Supporting Document Upload shortcut
            if (role == UserRole.CRM) {
                SectionCard(title = "Supporting Document Upload") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Attach any required compliance checks or external bureau evidence pdf directly to the dossier.",
                            style = FieldTheme.typography.label,
                            color = FieldTheme.colors.gray400
                        )
                        SecondaryButton(
                            text = "Upload Verification Document",
                            onClick = onUploadDocument,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Z4.3 Consent & Declaration Gates (Toggles)
            SectionCard(title = "CBN §1.6 Credit File Checklist & Declaration Gates") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChecklistItem(
                        label = "Verify that applicant credit score has been pulled from CRC & FirstCentral registry systems.",
                        checked = bureau1,
                        onCheckedChange = { bureau1 = it },
                        enabled = role == UserRole.CRM
                    )
                    ChecklistItem(
                        label = "Verify that CreditRegistry search returns no critical default exceptions.",
                        checked = bureau2,
                        onCheckedChange = { bureau2 = it },
                        enabled = role == UserRole.CRM
                    )
                    ChecklistItem(
                        label = "Verify that the loan search query in Credit Risk Management System (CRMS) has been executed.",
                        checked = crmsSearch,
                        onCheckedChange = { crmsSearch = it },
                        enabled = role == UserRole.CRM
                    )
                    ChecklistItem(
                        label = "Verify that NCR (National Collateral Registry) registration details align with collateral pledge documents.",
                        checked = ncrReg,
                        onCheckedChange = { ncrReg = it },
                        enabled = role == UserRole.CRM
                    )
                }
            }

            // Z4.4 Approved/Recommended Amount (Missing Audit Field)
            SectionCard(title = "Approved Amount Formulation") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Specify the exact approved principal value to register for disbursement (₦):",
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

            // Z4.5 Executive Notes
            SectionCard(title = if (role == UserRole.HEAD_CRM) "Oversight Notes" else "Executive Notes") {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text(if (role == UserRole.HEAD_CRM) "Add comments for the Audit / Executive stage..." else "Add completeness confirmation notes...") },
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
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
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
            color = if (checked) FieldTheme.colors.gray100 else FieldTheme.colors.gray400,
            modifier = Modifier.weight(1f)
        )
    }
}
