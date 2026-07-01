package com.fieldcrm.android.ui.screens.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.viewmodel.ApplicationViewModel
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel
import java.util.Locale

@Composable
fun AdminMcrApprovalScreen(
    application: LoanApplicationModel,
    borrower: BorrowerModel?,
    applicationViewModel: ApplicationViewModel,
    onBackClick: () -> Unit,
    onDisburseTriggered: () -> Unit
) {
    var yesVotes by remember { mutableIntStateOf(4) }
    val totalCommitteeVotes = 5
    var isDisbursedState by remember { mutableStateOf(false) }

    val hasQuorum = yesVotes >= 3
    val appState by applicationViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "Board Disbursement Panel",
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
                            text = "MCR Board Approval Gate",
                            style = FieldTheme.typography.title,
                            color = FieldTheme.colors.gray100
                        )
                        
                        // Committee Votes Card
                        FieldCard {
                            Text("COMMITTEE VOTE TRACKER", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Affirmative Committee Votes", style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray300)
                                Text("$yesVotes / $totalCommitteeVotes", style = FieldTheme.typography.mono.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = if (hasQuorum) FieldTheme.colors.statusSuccess else FieldTheme.colors.statusDanger)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            ConfidenceBar(percentage = yesVotes.toFloat() / totalCommitteeVotes.toFloat())
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Interactive Voting Adjuster
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Adjust Affirmative Board Count", style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SecondaryButton(
                                        text = "-",
                                        onClick = { if (yesVotes > 0) yesVotes-- },
                                        modifier = Modifier.width(48.dp)
                                    )
                                    SecondaryButton(
                                        text = "+",
                                        onClick = { if (yesVotes < totalCommitteeVotes) yesVotes++ },
                                        modifier = Modifier.width(48.dp)
                                    )
                                }
                            }
                        }
                        
                        // Disbursement Readiness Checklist
                        FieldCard {
                            Text("DISBURSEMENT STATUS GATES", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            ReadinessChecklist(
                                gates = listOf(
                                    ChecklistGate("Branch Manager Approval Logged", true, StatusChipVariant.Approved),
                                    ChecklistGate("External Audit Trail Sign-off Verified", true, StatusChipVariant.Verified),
                                    ChecklistGate("Committee Quorum Satisfied (3+ Votes)", hasQuorum, if (hasQuorum) StatusChipVariant.Verified else StatusChipVariant.Missing)
                                )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (isDisbursedState) {
                            FieldCard(modifier = Modifier.border(0.5.dp, FieldTheme.colors.statusSuccess, RoundedCornerShape(FieldTheme.shapes.cardRadius))) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = FieldIcons.CheckCircleOutlined,
                                            contentDescription = "Success",
                                            tint = FieldTheme.colors.statusSuccess,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text("DISBURSEMENT TRANSMITTED", style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray100)
                                            Text("Funding instruction transmitted to bank ledger system.", style = FieldTheme.typography.body.copy(fontSize = 12.sp), color = FieldTheme.colors.gray400)
                                        }
                                    }
                                    FieldDivider()
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("TRANSACTION DETAILS", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Tx Ref", style = FieldTheme.typography.body.copy(fontSize = 13.sp), color = FieldTheme.colors.gray400)
                                            Text("TX-MMFB-2849102-LOAN", style = FieldTheme.typography.mono, color = FieldTheme.colors.gray300)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Ledger Hash", style = FieldTheme.typography.body.copy(fontSize = 13.sp), color = FieldTheme.colors.gray400)
                                            Text("0x7d8a9f4c3b2a1e...", style = FieldTheme.typography.mono, color = FieldTheme.colors.gray300)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Disbursed Amount", style = FieldTheme.typography.body.copy(fontSize = 13.sp), color = FieldTheme.colors.gray400)
                                            Text("₦${String.format(java.util.Locale.US, "%,.0f", application.amount)}", style = FieldTheme.typography.mono.copy(fontWeight = FontWeight.Bold), color = FieldTheme.colors.purple400)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Recipient", style = FieldTheme.typography.body.copy(fontSize = 13.sp), color = FieldTheme.colors.gray400)
                                            Text("${borrower?.name ?: "Adaeze Okonkwo"} (${borrower?.bank_name ?: "Access Bank"})", style = FieldTheme.typography.bodyStrong.copy(fontSize = 13.sp), color = FieldTheme.colors.gray300)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    PrimaryButton(
                                        text = "Complete",
                                        onClick = onDisburseTriggered
                                    )
                                }
                            }
                        } else {
                            PrimaryButton(
                                text = if (appState.isLoading) "Transmitting..." else "Trigger Ledger Disbursement",
                                onClick = {
                                    applicationViewModel.approveApplication(application.id) {
                                        isDisbursedState = true
                                    }
                                },
                                enabled = hasQuorum && !appState.isLoading
                            )
                        }
                    }
                }
            }
        }
    }
}
