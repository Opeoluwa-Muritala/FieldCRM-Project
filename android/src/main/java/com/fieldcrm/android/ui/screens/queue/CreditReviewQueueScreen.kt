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
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.ui.viewmodel.DashboardViewModel
import org.koin.androidx.compose.koinViewModel

private data class CreditReviewItem(
    val applicantName: String,
    val productType: String,
    val amount: String,
    val tenure: String,
    val appId: String
)

@Composable
fun CreditReviewQueueScreen(
    applications: List<LoanApplicationModel> = emptyList(),
    borrowers: List<BorrowerModel> = emptyList(),
    role: UserRole? = null,
    onBackClick: (() -> Unit)? = null,
    onReviewApplication: (String) -> Unit = {}
) {
    val dashboardViewModel: DashboardViewModel = koinViewModel()
    val dashboardState by dashboardViewModel.uiState.collectAsState()
    val dashboardQueue = dashboardState.metrics?.data?.queue.orEmpty()
    val reviewItems = remember(applications, borrowers, dashboardQueue, role) {
        if (role == UserRole.CREDIT_ANALYST) {
            dashboardState.metrics?.data?.reviews.orEmpty().map { app ->
                CreditReviewItem(
                    applicantName = app.applicant_name,
                    productType = app.loan_type.replaceFirstChar { it.uppercase() },
                    amount = "₦${String.format(Locale.US, "%,.0f", app.amount ?: 0.0)}",
                    tenure = "${app.exception_count} OCR issues",
                    appId = app.id
                )
            }
        } else if (role == UserRole.BRANCH_SUPERVISOR) {
            dashboardQueue.map { app ->
                CreditReviewItem(
                    applicantName = app.applicant_name,
                    productType = app.loan_type.replaceFirstChar { it.uppercase() },
                    amount = "₦${String.format(Locale.US, "%,.0f", app.amount ?: 0.0)}",
                    tenure = "Supervisory review",
                    appId = app.id
                )
            }
        } else if (applications.isNotEmpty()) {
            applications.map { app ->
                val borrower = borrowers.find { it.id == app.id }
                CreditReviewItem(
                    applicantName = borrower?.name ?: app.applicant_name,
                    productType = app.loan_type.replaceFirstChar { it.uppercase() },
                    amount = "₦${String.format(Locale.US, "%,.0f", app.amount ?: 0.0)}",
                    tenure = app.tenor_months?.let { "$it MO" } ?: "—",
                    appId = app.id
                )
            }
        } else emptyList()
    }
    val isLoading = if (role in setOf(UserRole.BRANCH_SUPERVISOR, UserRole.CREDIT_ANALYST)) dashboardState.isLoading else false

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        topBar = {
            FieldTopAppBar(
                title = when (role) {
                    UserRole.BRANCH_SUPERVISOR -> "Supervisory Review Queue"
                    UserRole.CREDIT_ANALYST -> "Underwriting Queue"
                    else -> "Review Queue"
                },
                navigationIcon = if (onBackClick != null) {
                    {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = FieldIcons.ArrowBackOutlined,
                                contentDescription = "Back",
                                tint = FieldTheme.colors.gray400
                            )
                        }
                    }
                } else null,
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
                            text = "${reviewItems.size} QUEUED",
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

            if (dashboardState.error != null) {
                FinanceErrorState(dashboardState.error ?: "Unable to load review work.", dashboardViewModel::loadMetrics)
            } else if (isLoading) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(5) {
                        FieldCard(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    LoadingSkeleton(height = 16.dp, width = 140.dp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LoadingSkeleton(height = 12.dp, width = 90.dp)
                                }
                                LoadingSkeleton(height = 20.dp, width = 70.dp, cornerRadius = 10.dp)
                            }
                        }
                    }
                }
            } else if (reviewItems.isEmpty()) {
                FinanceEmptyState("No reviews waiting", "No dossiers currently require this review.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(reviewItems) { item ->
                        FieldCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = item.appId.isNotEmpty()) { onReviewApplication(item.appId) }
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
                                            text = item.tenure,
                                            style = FieldTheme.typography.label.copy(fontSize = 10.sp),
                                            color = FieldTheme.colors.gray500
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.productType.uppercase(Locale.getDefault()),
                                        style = FieldTheme.typography.label.copy(fontSize = 10.sp),
                                        color = FieldTheme.colors.purple400
                                    )
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

@Preview(name = "Credit Review Queue Screen", widthDp = 411, heightDp = 850)
@Composable
fun PreviewCreditReviewQueueScreen() {
    FieldCRMTheme {
        CreditReviewQueueScreen(onBackClick = {})
    }
}
