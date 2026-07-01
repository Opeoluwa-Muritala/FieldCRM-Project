package com.fieldcrm.android.ui.screens.review

import androidx.compose.foundation.background
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
import com.fieldcrm.android.data.api.AuditChecklist
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.viewmodel.AuditTrailViewModel
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@Composable
fun AuditorComplianceScreen(
    applicationId: String = "",
    onBackClick: () -> Unit,
    onAuditComplete: () -> Unit
) {
    val viewModel: AuditTrailViewModel = koinViewModel()
    val auditState by viewModel.uiState.collectAsState()

    var isConsentVerified by remember { mutableStateOf(false) }
    var isSignatureMatched by remember { mutableStateOf(false) }
    var isExhibitsVerified by remember { mutableStateOf(false) }
    var auditComments by remember { mutableStateOf("") }

    LaunchedEffect(applicationId) {
        if (applicationId.isNotEmpty()) viewModel.loadChecklist(applicationId)
    }

    LaunchedEffect(auditState.checklist) {
        auditState.checklist?.let { cl ->
            isConsentVerified = cl.consent_verified
            isSignatureMatched = cl.signature_matched
            isExhibitsVerified = cl.exhibits_verified
        }
    }

    val allChecksPassed = isConsentVerified && isSignatureMatched && isExhibitsVerified

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "Auditor Verification Board",
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
                            text = "Auditing Compliance Checklists",
                            style = FieldTheme.typography.title,
                            color = FieldTheme.colors.gray100
                        )
                        Text(
                            text = "Verify legal and administrative compliance checklist items. Log observations prior to official sign-off.",
                            style = FieldTheme.typography.body,
                            color = FieldTheme.colors.gray400
                        )
                        
                        // Consent & Verification Check Card
                        FieldCard {
                            Text("CONSENT & LEGAL VERIFICATIONS", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Borrower Consent Slip Verified", style = FieldTheme.typography.body, color = FieldTheme.colors.gray300)
                                Checkbox(
                                    checked = isConsentVerified,
                                    onCheckedChange = { isConsentVerified = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = FieldTheme.colors.purple600,
                                        uncheckedColor = FieldTheme.colors.gray700
                                    )
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            FieldDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Physical Signature Matched to NIN", style = FieldTheme.typography.body, color = FieldTheme.colors.gray300)
                                Checkbox(
                                    checked = isSignatureMatched,
                                    onCheckedChange = { isSignatureMatched = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = FieldTheme.colors.purple600,
                                        uncheckedColor = FieldTheme.colors.gray700
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            FieldDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Exhibits Verification Checked", style = FieldTheme.typography.body, color = FieldTheme.colors.gray300)
                                Checkbox(
                                    checked = isExhibitsVerified,
                                    onCheckedChange = { isExhibitsVerified = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = FieldTheme.colors.purple600,
                                        uncheckedColor = FieldTheme.colors.gray700
                                    )
                                )
                            }
                        }

                        // Observations comments card
                        FieldCard {
                            Text("AUDIT CONSOLE OBSERVATIONS", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            FieldTextField(
                                value = auditComments,
                                onValueChange = { auditComments = it },
                                label = "Auditor Compliance Notes",
                                placeholder = "Log physical address verification matches, GPS discrepancy notes, etc."
                            )
                        }

                        // Readiness gates checklist display
                        FieldCard {
                            Text("AUDIT COMPLIANCE STATUS GATES", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            ReadinessChecklist(
                                gates = listOf(
                                    ChecklistGate("KYC Verification File Stamped", isConsentVerified, if (isConsentVerified) StatusChipVariant.Verified else StatusChipVariant.Missing),
                                    ChecklistGate("Deed Registry Signature Verified", isSignatureMatched, if (isSignatureMatched) StatusChipVariant.Signed else StatusChipVariant.Missing),
                                    ChecklistGate("Business Site Photo Exhibits Verified", isExhibitsVerified, if (isExhibitsVerified) StatusChipVariant.Verified else StatusChipVariant.Missing)
                                )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        PrimaryButton(
                            text = if (auditState.isSaving) "Saving..." else "Log Auditor Sign-Off",
                            onClick = {
                                viewModel.saveChecklist(
                                    applicationId,
                                    AuditChecklist(
                                        consent_verified = isConsentVerified,
                                        signature_matched = isSignatureMatched,
                                        exhibits_verified = isExhibitsVerified
                                    )
                                ) { onAuditComplete() }
                            },
                            enabled = allChecksPassed && !auditState.isSaving
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Compact Phone Audit Board", widthDp = 411, heightDp = 850)
@Composable
fun PreviewAuditBoardCompact() {
    FieldCRMTheme {
        AuditorComplianceScreen(onBackClick = {}, onAuditComplete = {})
    }
}
