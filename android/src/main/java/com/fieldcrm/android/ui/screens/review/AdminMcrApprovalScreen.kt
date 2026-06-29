package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import java.util.Locale

@Composable
fun AdminMcrApprovalScreen(
    onBackClick: () -> Unit,
    onDisburseTriggered: () -> Unit
) {
    var yesVotes by remember { mutableIntStateOf(4) }
    var totalCommitteeVotes by remember { mutableIntStateOf(5) }
    var isDisbursedState by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "Board Disbursement Panel",
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
                            text = "MCR Board Approval Gate",
                            style = FieldTheme.typography.title,
                            color = FieldTheme.colors.gray100
                        )
                        
                        // Committee Votes
                        FieldCard {
                            Text("COMMITTEE VOTE TRACKER", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Affirmative Committee Votes", style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray300)
                                Text("$yesVotes / $totalCommitteeVotes", style = FieldTheme.typography.mono.copy(fontSize = 16.sp), color = FieldTheme.colors.statusSuccess)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            ConfidenceBar(percentage = yesVotes.toFloat() / totalCommitteeVotes.toFloat())
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Under bank constitution, minimum 3 board members must approve for asset allocation.",
                                style = FieldTheme.typography.body.copy(fontSize = 11.sp),
                                color = FieldTheme.colors.gray500
                            )
                        }
                        
                        // Disbursement Readiness Checklist
                        FieldCard {
                            Text("DISBURSEMENT STATUS GATES", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            ReadinessChecklist(
                                gates = listOf(
                                    ChecklistGate("Branch Manager Approval Logged", true, StatusChipVariant.Approved),
                                    ChecklistGate("External Audit Trail Sign-off Verified", true, StatusChipVariant.Verified),
                                    ChecklistGate("Credit Risk DTI Ratio verified", true, StatusChipVariant.Verified)
                                )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (isDisbursedState) {
                            FieldCard(modifier = Modifier.border(1.dp, FieldTheme.colors.statusSuccess, RoundedCornerShape(10.dp))) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(FieldTheme.colors.statusSuccess.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("✓", color = FieldTheme.colors.statusSuccess, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("DISBURSEMENT TRANSMITTED", style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray100)
                                        Text("Funding instruction transmitted to bank ledger system.", style = FieldTheme.typography.body.copy(fontSize = 12.sp), color = FieldTheme.colors.gray400)
                                    }
                                }
                            }
                        } else {
                            PrimaryButton(
                                text = "Trigger Ledger Disbursement",
                                onClick = {
                                    isDisbursedState = true
                                    onDisburseTriggered()
                                }
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

@Preview(name = "Compact Phone Admin Board", widthDp = 411, heightDp = 850)
@Composable
fun PreviewAdminBoardCompact() {
    FieldCRMTheme {
        AdminMcrApprovalScreen(onBackClick = {}, onDisburseTriggered = {})
    }
}
