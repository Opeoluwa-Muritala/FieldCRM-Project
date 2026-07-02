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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel

private data class PendingSignoffItem(
    val applicantName: String,
    val loanOfficer: String,
    val visitDate: String,
    val reportId: String
)

private val placeholderSignoffItems = listOf(
    PendingSignoffItem("Adaeze Okonkwo", "Samuel Okeke", "2026-07-01", ""),
    PendingSignoffItem("Ngozi Adeyemi", "Grace Nwosu", "2026-06-30", ""),
    PendingSignoffItem("Emeka Chukwu", "Samuel Okeke", "2026-06-30", ""),
    PendingSignoffItem("Chinedu Obi", "Aisha Mohammed", "2026-06-29", ""),
    PendingSignoffItem("Fatima Bello", "Grace Nwosu", "2026-06-29", "")
)

@Composable
fun PendingSignoffsScreen(
    applications: List<LoanApplicationModel> = emptyList(),
    borrowers: List<BorrowerModel> = emptyList(),
    onBackClick: () -> Unit,
    onViewReport: (String) -> Unit = {}
) {
    val signoffItems = remember(applications, borrowers) {
        if (applications.isNotEmpty()) {
            applications.map { app ->
                val borrower = borrowers.find { it.id == app.borrower_id }
                PendingSignoffItem(
                    applicantName = borrower?.name ?: "Unknown Applicant",
                    loanOfficer = "Loan Officer",
                    visitDate = "2026-07-02",
                    reportId = app.id
                )
            }
        } else placeholderSignoffItems
    }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        topBar = {
            FieldTopAppBar(
                title = "Pending Sign-offs",
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
                            text = "${signoffItems.size} REPORTS",
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
                    items(5) {
                        FieldCard(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                            Column {
                                LoadingSkeleton(height = 16.dp, width = 150.dp)
                                Spacer(modifier = Modifier.height(6.dp))
                                LoadingSkeleton(height = 12.dp, width = 100.dp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(signoffItems) { item ->
                        FieldCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = item.reportId.isNotEmpty()) { onViewReport(item.reportId) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.applicantName,
                                        style = FieldTheme.typography.bodyStrong,
                                        color = FieldTheme.colors.gray100
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Filed by ${item.loanOfficer}",
                                        style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                        color = FieldTheme.colors.gray400
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Visit: ${item.visitDate}",
                                        style = FieldTheme.typography.mono.copy(fontSize = 10.sp),
                                        color = FieldTheme.colors.gray500
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    StatusChip(variant = StatusChipVariant.NeedsReview)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                FieldTheme.colors.purple950,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .border(
                                                0.5.dp,
                                                FieldTheme.colors.purple400,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable(enabled = item.reportId.isNotEmpty()) { onViewReport(item.reportId) }
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "REVIEW",
                                            style = FieldTheme.typography.label.copy(fontSize = 10.sp),
                                            color = FieldTheme.colors.purple200
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Pending Sign-offs Screen", widthDp = 411, heightDp = 850)
@Composable
fun PreviewPendingSignoffsScreen() {
    FieldCRMTheme {
        PendingSignoffsScreen(onBackClick = {})
    }
}
