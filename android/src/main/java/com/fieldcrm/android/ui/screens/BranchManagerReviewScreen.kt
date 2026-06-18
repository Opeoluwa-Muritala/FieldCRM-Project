package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import java.util.Locale

@Composable
fun BranchManagerReviewScreen(
    onBackClick: () -> Unit,
    onDecisionSubmitted: () -> Unit
) {
    var managerComment by remember { mutableStateOf("Verified collateral deed metrics against external registry. Flow recommended for Admin disbursement.") }
    var selectedReason by remember { mutableStateOf("High Confidence Business Site Check") }

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "Manager Review Console",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                                    DetailItem(label = "Applicant", value = "Adaeze Okonkwo")
                                    DetailItem(label = "Requested Amount", value = "₦ 250,000", isMono = true)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    DetailItem(label = "Underwritten Stage", value = "Branch Approval")
                                    DetailItem(label = "Primary Product", value = "Asset Capitalization")
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            FieldDivider()
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            ReadinessChecklist(
                                gates = listOf(
                                    ChecklistGate("Identity Verification Passed", true, StatusChipVariant.Verified),
                                    ChecklistGate("GPS Site Visitation Logged", true, StatusChipVariant.Verified),
                                    ChecklistGate("Guarantor Registry Validation", true, StatusChipVariant.Verified)
                                )
                            )
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
                                text = "Approve & Push to Board",
                                onClick = onDecisionSubmitted
                            )
                            SecondaryButton(
                                text = "Return to Underwriting Pool",
                                onClick = onDecisionSubmitted
                            )
                            DangerButton(
                                text = "Reject & Deactivate Dossier",
                                onClick = onDecisionSubmitted
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(name = "Compact Phone BM Review", widthDp = 411, heightDp = 850)
@Composable
fun PreviewBMReviewCompact() {
    FieldCRMTheme {
        BranchManagerReviewScreen(onBackClick = {}, onDecisionSubmitted = {})
    }
}
