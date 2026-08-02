package com.fieldcrm.android.ui.roles.supervisor.review

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
import com.fieldcrm.android.ui.viewmodel.ApplicationViewModel
import com.fieldcrm.android.ui.viewmodel.ConfigViewModel
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupervisorReview(
    application: LoanApplicationModel,
    borrower: BorrowerModel?,
    applicationViewModel: ApplicationViewModel,
    onBackClick: () -> Unit,
    onDecisionSubmitted: () -> Unit
) {
    val configViewModel: ConfigViewModel = koinViewModel()
    val configState by configViewModel.uiState.collectAsState()
    val reviewReasons = configState.config?.dropdowns?.review_reasons?.takeIf { it.isNotEmpty() }
        ?: listOf("Business location verification failed", "Collateral valuation needs review", "Guarantor validation failed", "Documentation mismatch")

    var supervisorComment by remember { mutableStateOf("") }
    var selectedReason by remember { mutableStateOf(reviewReasons.first()) }
    var showReturnDialog by remember { mutableStateOf(false) }

    val appState by applicationViewModel.uiState.collectAsState()
    val checklist = appState.reviewChecklist

    // Supervisor verification checklist states
    var identityChecked by remember { mutableStateOf(false) }
    var collateralChecked by remember { mutableStateOf(false) }
    var guarantorChecked by remember { mutableStateOf(false) }

    val isFormValid = identityChecked && collateralChecked && guarantorChecked

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "Supervisor Review",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = FieldIcons.ArrowBackOutlined,
                            contentDescription = "Back",
                            tint = FieldTheme.colors.gray400
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
                    "Approval moves this dossier to Credit Risk Analysis and records your supervisor-level concurrence."
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showReturnDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Return")
                    }
                    PrimaryButton(
                        text = if (appState.isLoading) "Submitting..." else "Approve to Credit",
                        onClick = {
                            applicationViewModel.approveApplication(
                                application.id,
                                supervisorComment,
                                identityChecked && guarantorChecked,
                                collateralChecked,
                                onComplete = onDecisionSubmitted
                            )
                        },
                        enabled = isFormValid && !appState.isLoading,
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
                .padding(padding)
                .verticalScroll(rememberScrollState())
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
                                text = borrower?.name ?: application.applicant_name ?: "Unknown Applicant",
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
                        Column(modifier = Modifier.weight(1f)) {
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

                    Spacer(modifier = Modifier.height(4.dp))

                    // A2 Distinct: Prior Concurrence Badge Row
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
                            contentDescription = "Concurred",
                            tint = FieldTheme.colors.statusSuccess,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Prior Concurrence by Team Lead",
                                style = FieldTheme.typography.label.copy(fontWeight = FontWeight.Bold),
                                color = FieldTheme.colors.gray100
                            )
                            Text(
                                text = "TL: Tunde Yusuf | Decision: Concurred | Date: 2026-08-02",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.gray400
                            )
                        }
                    }
                }
            }

            // Z4.1: Team Lead's Concurrence Evidence (Read-only summary)
            SectionCard(title = "Team Lead Concurrence Evidence") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailItem(
                        label = "KYC Completeness verified",
                        value = if (checklist["kyc_attested"] == true) "PASSED" else "PENDING"
                    )
                    DetailItem(
                        label = "Collateral Valuation verified",
                        value = if (checklist["collateral_attested"] == true) "PASSED" else "PENDING"
                    )
                    DetailItem(
                        label = "Team Lead Comments",
                        value = "Not available"
                    )
                }
            }

            // Z4.2: Supervisor Verification Checklist
            SectionCard(title = "Supervisor Verification Checklist") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "I verify that the following concurrence reviews have been completed:",
                        style = FieldTheme.typography.label,
                        color = FieldTheme.colors.gray400
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { identityChecked = !identityChecked }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = identityChecked,
                            onCheckedChange = { identityChecked = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Verify Borrower Identity, KYC details, & Business Location matches registry records",
                            style = FieldTheme.typography.body,
                            color = FieldTheme.colors.gray300
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { collateralChecked = !collateralChecked }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = collateralChecked,
                            onCheckedChange = { collateralChecked = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Verify Collateral Documents ownership verification & Valuation reports correctness",
                            style = FieldTheme.typography.body,
                            color = FieldTheme.colors.gray300
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { guarantorChecked = !guarantorChecked }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = guarantorChecked,
                            onCheckedChange = { guarantorChecked = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Verify Guarantor Commitment & Financial Standing verification checks pass",
                            style = FieldTheme.typography.body,
                            color = FieldTheme.colors.gray300
                        )
                    }
                }
            }

            // Z4.3: Comments
            SectionCard(title = "Supervisor Comments") {
                OutlinedTextField(
                    value = supervisorComment,
                    onValueChange = { supervisorComment = it },
                    placeholder = { Text("Enter recommendation notes or return instructions...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = FieldTheme.colors.gray900,
                        unfocusedContainerColor = FieldTheme.colors.gray900,
                        focusedTextColor = FieldTheme.colors.gray100,
                        unfocusedTextColor = FieldTheme.colors.gray100
                    )
                )
            }
        }
    }

    if (showReturnDialog) {
        AlertDialog(
            onDismissRequest = { showReturnDialog = false },
            title = {
                Text(
                    text = "Return Application",
                    style = FieldTheme.typography.bodyStrong,
                    color = FieldTheme.colors.gray100
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Select a reason for returning this application to the Team Lead:",
                        style = FieldTheme.typography.body,
                        color = FieldTheme.colors.gray400
                    )
                    reviewReasons.forEach { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = reason,
                                style = FieldTheme.typography.body,
                                color = FieldTheme.colors.gray300
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        applicationViewModel.returnApplication(
                            id = application.id,
                            reason = selectedReason,
                            notes = supervisorComment,
                            onComplete = {
                                showReturnDialog = false
                                onDecisionSubmitted()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FieldTheme.colors.statusDanger)
                ) {
                    Text("Confirm Return")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReturnDialog = false }) {
                    Text("Cancel", color = FieldTheme.colors.gray400)
                }
            },
            containerColor = FieldTheme.colors.gray900
        )
    }
}
