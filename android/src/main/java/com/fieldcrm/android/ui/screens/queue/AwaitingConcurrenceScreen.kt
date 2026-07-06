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

private data class ConcurrenceItem(
    val applicantName: String,
    val loanOfficer: String,
    val amount: String,
    val submittedAt: String,
    val appId: String
)

private val placeholderConcurrenceItems = listOf(
    ConcurrenceItem("Adaeze Okonkwo", "Samuel Okeke", "₦500,000", "2026-07-01 09:14", ""),
    ConcurrenceItem("Emeka Chukwu", "Grace Nwosu", "₦1,200,000", "2026-07-01 08:45", ""),
    ConcurrenceItem("Fatima Bello", "Samuel Okeke", "₦320,000", "2026-06-30 17:30", ""),
    ConcurrenceItem("Chukwuemeka Eze", "Aisha Mohammed", "₦750,000", "2026-06-30 16:00", "")
)

@Composable
fun AwaitingConcurrenceScreen(
    applications: List<LoanApplicationModel> = emptyList(),
    borrowers: List<BorrowerModel> = emptyList(),
    onBackClick: () -> Unit,
    onViewApplication: (String) -> Unit = {}
) {
    val concurrenceItems = remember(applications, borrowers) {
        if (applications.isNotEmpty()) {
            applications.map { app ->
                val borrower = borrowers.find { it.id == app.id }
                ConcurrenceItem(
                    applicantName = borrower?.name ?: "Unknown Applicant",
                    loanOfficer = "Loan Officer",
                    amount = "₦${String.format(Locale.US, "%,.0f", app.amount)}",
                    submittedAt = "—",
                    appId = app.id
                )
            }
        } else placeholderConcurrenceItems
    }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        topBar = {
            FieldTopAppBar(
                title = "Awaiting Concurrence",
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
                            text = "${concurrenceItems.size} PENDING",
                            style = FieldTheme.typography.mono.copy(fontSize = 10.sp),
                            color = FieldTheme.colors.statusWarning
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
                    items(4) {
                        FieldCard(modifier = Modifier.fillMaxWidth().height(88.dp)) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    LoadingSkeleton(height = 16.dp, width = 150.dp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LoadingSkeleton(height = 12.dp, width = 100.dp)
                                }
                                LoadingSkeleton(height = 20.dp, width = 70.dp, cornerRadius = 10.dp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(concurrenceItems) { item ->
                        FieldCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = item.appId.isNotEmpty()) { onViewApplication(item.appId) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.applicantName,
                                        style = FieldTheme.typography.bodyStrong,
                                        color = FieldTheme.colors.gray100
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "LO: ${item.loanOfficer}",
                                        style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                        color = FieldTheme.colors.gray400
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.amount,
                                            style = FieldTheme.typography.mono.copy(
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = FieldTheme.colors.gray300
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = item.submittedAt,
                                            style = FieldTheme.typography.mono.copy(fontSize = 10.sp),
                                            color = FieldTheme.colors.gray500
                                        )
                                    }
                                }
                                StatusChip(variant = StatusChipVariant.NeedsReview)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Awaiting Concurrence Screen", widthDp = 411, heightDp = 850)
@Composable
fun PreviewAwaitingConcurrenceScreen() {
    FieldCRMTheme {
        AwaitingConcurrenceScreen(onBackClick = {})
    }
}
