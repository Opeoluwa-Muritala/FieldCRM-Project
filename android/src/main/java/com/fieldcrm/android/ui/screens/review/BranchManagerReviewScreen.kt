package com.fieldcrm.android.ui.screens.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
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
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel
import java.util.Locale

@Composable
fun BranchManagerReviewScreen(
    application: LoanApplicationModel,
    borrower: BorrowerModel?,
    applicationViewModel: ApplicationViewModel,
    onBackClick: () -> Unit,
    onDecisionSubmitted: () -> Unit
) {
    var managerComment by remember { mutableStateOf("Verified collateral deed metrics against external registry. Flow recommended for Admin disbursement.") }
    var selectedReason by remember { mutableStateOf("High Confidence Business Site Check") }
    
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
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = FieldTheme.colors.gray400
                        )
                    }
                }
            )
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            val isWide = maxWidth >= 600.dp
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
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
                        Text(
                            text = "Branch Manager Decision Gates",
                            style = FieldTheme.typography.title,
                            color = FieldTheme.colors.gray100
                        )
                        
                        // Borrower overview summary card
                        FieldCard {
                            Text("DOSSIER SUMMARY", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    DetailItem(label = "Applicant", value = borrower?.name ?: "Adaeze Okonkwo")
                                    DetailItem(label = "Requested Amount", value = "₦ ${application.amount}", isMono = true)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    DetailItem(label = "Underwritten Stage", value = "Branch Approval")
                                    DetailItem(label = "Primary Product", value = application.product_type)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            FieldDivider()
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            ReadinessChecklist(
                                hasCollateral = application.collateral_desc != null,
                                hasGps = borrower?.gps_coordinates != null,
                                hasGuarantor = borrower?.guarantor_name != null
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
                                options = listOf(
                                    "High Confidence Business Site Check",
                                    "Strong Co-Guarantor Attestation",
                                    "Collateral Evaluation Mismatch",
                                    "Insufficient Credit Score"
                                ),
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
                        
                        // Action buttons
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            PrimaryButton(
                                text = if (appState.isLoading) "Processing Approval..." else "Approve & Push to Board",
                                onClick = {
                                    applicationViewModel.approveApplication(application.id) {
                                        onDecisionSubmitted()
                                    }
                                },
                                enabled = isKycAttested && isCollateralAttested && managerComment.isNotEmpty() && !appState.isLoading
                            )
                            SecondaryButton(
                                text = "Return to Underwriting Pool",
                                onClick = {
                                    applicationViewModel.returnApplication(application.id, selectedReason, managerComment) {
                                        onDecisionSubmitted()
                                    }
                                },
                                enabled = !appState.isLoading
                            )
                            DangerButton(
                                text = "Reject & Deactivate Dossier",
                                onClick = {
                                    applicationViewModel.returnApplication(application.id, "REJECTED: $selectedReason", managerComment) {
                                        onDecisionSubmitted()
                                    }
                                },
                                enabled = !appState.isLoading
                            )
                        }
                    }
                }
            }
        }
    }
}
