package com.fieldcrm.android.ui.screens.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel
import java.util.Locale

private data class PipelineEntry(
    val applicantName: String,
    val amount: String,
    val stage: String,
    val appId: String
)

private val stageOrder = listOf(
    "Relationship Officer Intake",
    "Team Lead Review",
    "Supervisor Review",
    "Credit Analysis",
    "CRM Dossier Review",
    "Head CRM Approval",
    "Executive Director Approval",
    "Managing Director Input",
    "CRM Disbursement",
    "Disbursed",
    "Returned"
)

private val stageMapping = mapOf(
    "intake" to "Relationship Officer Intake",
    "branch_manager_review" to "Team Lead Review",
    "branch_supervisor_review" to "Supervisor Review",
    "credit_analyst_review" to "Credit Analysis",
    "crm_review" to "CRM Dossier Review",
    "head_crm_review" to "Head CRM Approval",
    "ed_approval" to "Executive Director Approval",
    "executive_approval" to "Executive Director Approval",
    "md_approval" to "Managing Director Input",
    "disbursement_ready" to "CRM Disbursement",
    "disbursed" to "Disbursed",
    "returned" to "Returned",
    "rejected" to "Returned",
    // Historical stages are shown at their closest active workflow position.
    "ocr_review" to "Relationship Officer Intake",
    "credit_review" to "Credit Analysis",
    "branch_approval" to "Team Lead Review",
    "committee_review" to "Executive Director Approval"
)

@Composable
fun PipelineScreen(
    applications: List<LoanApplicationModel> = emptyList(),
    borrowers: List<BorrowerModel> = emptyList(),
    onBackClick: () -> Unit,
    onViewApplication: (String) -> Unit = {}
) {
    var isLoading by remember { mutableStateOf(false) }

    val pipelineEntries = remember(applications, borrowers) {
        if (applications.isNotEmpty()) {
            applications.map { app ->
                val borrower = borrowers.find { it.id == app.borrower_id }
                PipelineEntry(
                    applicantName = borrower?.name ?: "Applicant details unavailable",
                    amount = "₦${String.format(Locale.US, "%,.0f", app.amount ?: 0.0)}",
                    stage = stageMapping[app.stage] ?: "Relationship Officer Intake",
                    appId = app.id
                )
            }
        } else emptyList()
    }

    val groupedByStage = remember(pipelineEntries) {
        stageOrder.associateWith { stage ->
            pipelineEntries.filter { it.stage == stage }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        topBar = {
            FieldTopAppBar(
                title = "Pipeline",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = FieldIcons.ArrowBackOutlined,
                            contentDescription = "Back",
                            tint = FieldTheme.colors.gray400
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .background(
                                FieldTheme.colors.gray800,
                                RoundedCornerShape(FieldTheme.shapes.cardRadius)
                            )
                            .border(
                                0.5.dp,
                                FieldTheme.colors.gray700,
                                RoundedCornerShape(FieldTheme.shapes.cardRadius)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${pipelineEntries.size} TOTAL",
                            style = FieldTheme.typography.mono.copy(fontSize = 10.sp),
                            color = FieldTheme.colors.purple400
                        )
                    }
                }
            )
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(6) {
                        FieldCard(modifier = Modifier.fillMaxWidth().height(72.dp)) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    LoadingSkeleton(height = 16.dp, width = 140.dp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LoadingSkeleton(height = 12.dp, width = 80.dp)
                                }
                                LoadingSkeleton(height = 20.dp, width = 60.dp, cornerRadius = 10.dp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    stageOrder.forEach { stage ->
                        val stageItems = groupedByStage[stage] ?: emptyList()
                        if (stageItems.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stage.uppercase(Locale.getDefault()),
                                        style = FieldTheme.typography.label.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = FieldTheme.colors.purple400
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                FieldTheme.colors.gray800,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${stageItems.size}",
                                            style = FieldTheme.typography.mono.copy(fontSize = 10.sp),
                                            color = FieldTheme.colors.gray400
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .background(FieldTheme.colors.gray700.copy(alpha = 0.5f))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            items(stageItems) { entry ->
                                val chipVariant = when (stage) {
                                    "Relationship Officer Intake" -> StatusChipVariant.Verified
                                    "Executive Director Approval", "Managing Director Input", "CRM Disbursement" -> StatusChipVariant.Approved
                                    "Disbursed" -> StatusChipVariant.Signed
                                    "Returned" -> StatusChipVariant.Returned
                                    else -> StatusChipVariant.NeedsReview
                                }

                                FieldCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                        .clickable(enabled = entry.appId.isNotEmpty()) { onViewApplication(entry.appId) }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val initials = entry.applicantName.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(FieldTheme.colors.gray800, RoundedCornerShape(20.dp))
                                                .border(1.dp, FieldTheme.colors.gray700, RoundedCornerShape(20.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = initials,
                                                style = FieldTheme.typography.title.copy(fontSize = 14.sp),
                                                color = FieldTheme.colors.gray300
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entry.applicantName,
                                                style = FieldTheme.typography.bodyStrong.copy(fontSize = 15.sp),
                                                color = FieldTheme.colors.gray100
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = entry.amount,
                                                style = FieldTheme.typography.mono.copy(
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = FieldTheme.colors.gray300
                                            )
                                        }
                                        StatusChip(variant = chipVariant)
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Preview(name = "Pipeline Screen", widthDp = 411, heightDp = 850)
@Composable
fun PreviewPipelineScreen() {
    FieldCRMTheme {
        PipelineScreen(onBackClick = {})
    }
}
