package com.fieldcrm.android.ui.screens.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.fieldcrm.android.ui.theme.FieldIcons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.screens.common.DetailItem
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.viewmodel.ApplicationViewModel
import com.fieldcrm.android.ui.viewmodel.ConfigViewModel
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@Composable
fun BranchManagerReviewScreen(
    application: LoanApplicationModel,
    borrower: BorrowerModel?,
    applicationViewModel: ApplicationViewModel,
    onBackClick: () -> Unit,
    onDecisionSubmitted: () -> Unit
) {
    val configViewModel: ConfigViewModel = koinViewModel()
    val configState by configViewModel.uiState.collectAsState()
    val reviewReasons = configState.config?.dropdowns?.review_reasons?.takeIf { it.isNotEmpty() }
        ?: listOf("High Confidence Business Site Check", "Strong Co-Guarantor Attestation",
                  "Collateral Evaluation Mismatch", "Insufficient Credit Score")

    var managerComment by remember { mutableStateOf("") }
    var selectedReason by remember { mutableStateOf(reviewReasons.first()) }

    // Update selectedReason if reviewReasons loaded after initial composition
    LaunchedEffect(reviewReasons) {
        if (selectedReason !in reviewReasons) selectedReason = reviewReasons.first()
    }

    // Interactive Attestations
    var isKycAttested by remember { mutableStateOf(false) }
    var isCollateralAttested by remember { mutableStateOf(false) }

    val appState by applicationViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "Manager Review Console",
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
                PrimaryButton(
                    text = if (appState.isLoading) "Processing Approval..." else "Approve & Push to Board",
                    onClick = {
                        applicationViewModel.approveApplication(application.id) {
                            onDecisionSubmitted()
                        }
                    },
                    enabled = isKycAttested && isCollateralAttested && managerComment.isNotEmpty() && !appState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecondaryButton(
                        text = "Return to Pool",
                        onClick = {
                            applicationViewModel.returnApplication(application.id, selectedReason, emptyList(), managerComment) {
                                onDecisionSubmitted()
                            }
                        },
                        enabled = !appState.isLoading,
                        modifier = Modifier.weight(1f)
                    )
                    DangerButton(
                        text = "Reject Dossier",
                        onClick = {
                            applicationViewModel.returnApplication(application.id, "REJECTED: $selectedReason", emptyList(), managerComment) {
                                onDecisionSubmitted()
                            }
                        },
                        enabled = !appState.isLoading,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 140.dp) // extra padding for bottom bar
            ) {
                // High-End Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FieldTheme.colors.purple600.copy(alpha = 0.05f))
                        .border(width = 0.5.dp, color = FieldTheme.colors.purple600.copy(alpha = 0.1f))
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    Text(
                        text = "Decision Gates",
                        style = FieldTheme.typography.title.copy(fontSize = 28.sp),
                        color = FieldTheme.colors.gray100
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Final branch-level authorization. Please audit all KYC and collateral checks.",
                        style = FieldTheme.typography.body.copy(fontSize = 14.sp),
                        color = FieldTheme.colors.gray400
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        
                        // Borrower overview summary card
                        FieldCard {
                            Text("DOSSIER SUMMARY", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    DetailItem(label = "Applicant", value = borrower?.name ?: "Adaeze Okonkwo")
                                    DetailItem(label = "Requested Amount", value = "₦${String.format(Locale.US, "%,.0f", application.amount)}", isMono = true)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    DetailItem(label = "Underwritten Stage", value = "Branch Approval")
                                    DetailItem(label = "Primary Product", value = application.loan_type.replaceFirstChar { it.uppercase() })
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            FieldDivider()
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            ReadinessChecklist(
                                gates = listOf(
                                    ChecklistGate(
                                        label = "Collateral Valuation Registered",
                                        isVerified = application.purpose != null,
                                        variant = if (application.purpose != null) StatusChipVariant.Verified else StatusChipVariant.Missing
                                    ),
                                    ChecklistGate(
                                        label = "GPS Visitation Coordinates Logged",
                                        isVerified = borrower?.gps_coordinates != null,
                                        variant = if (borrower?.gps_coordinates != null) StatusChipVariant.Verified else StatusChipVariant.Missing
                                    ),
                                    ChecklistGate(
                                        label = "Guarantor Profile Completed",
                                        isVerified = borrower?.guarantor_name != null,
                                        variant = if (borrower?.guarantor_name != null) StatusChipVariant.Verified else StatusChipVariant.Missing
                                    )
                                )
                            )
                        }

                        // Attestation Gate Checklist card
                        FieldCard {
                            Text("COMPLIANCE ATTESTATION", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isKycAttested = !isKycAttested }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("I attest that KYC audits meet MFB requirements", style = FieldTheme.typography.body, color = FieldTheme.colors.gray300)
                                Checkbox(
                                    checked = isKycAttested,
                                    onCheckedChange = { isKycAttested = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = FieldTheme.colors.purple600,
                                        uncheckedColor = FieldTheme.colors.gray700
                                    )
                                )
                            }
                            
                            FieldDivider()
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isCollateralAttested = !isCollateralAttested }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("I audit-check the collateral value registry", style = FieldTheme.typography.body, color = FieldTheme.colors.gray300)
                                Checkbox(
                                    checked = isCollateralAttested,
                                    onCheckedChange = { isCollateralAttested = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = FieldTheme.colors.purple600,
                                        uncheckedColor = FieldTheme.colors.gray700
                                    )
                                )
                            }
                        }

                        // Decision configuration card
                        FieldCard {
                            Text("DECISION FLOW OPTIONS", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            FieldDropdown(
                                value = selectedReason,
                                options = reviewReasons,
                                onOptionSelected = { selectedReason = it },
                                label = "Primary Recommendation Reason"
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            FieldTextField(
                                value = managerComment,
                                onValueChange = { managerComment = it },
                                label = "Manager Review Notes",
                                isRequired = true
                            )
                        }
                        
                        }
                    }
                }
            }
        }
    }
}
