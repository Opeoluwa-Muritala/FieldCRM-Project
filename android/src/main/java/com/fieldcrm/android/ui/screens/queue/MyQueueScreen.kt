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
import androidx.compose.ui.graphics.Color
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

private data class MyQueueItem(
    val applicantName: String,
    val stage: String,
    val amount: String,
    val appId: String
)

private val placeholderItems = listOf(
    MyQueueItem("Adaeze Okonkwo", "Intake", "₦250,000", ""),
    MyQueueItem("Emeka Chukwu", "OCR Review", "₦1,200,000", ""),
    MyQueueItem("Ngozi Adeyemi", "Intake", "₦500,000", ""),
    MyQueueItem("Chukwuemeka Eze", "Needs Review", "₦750,000", ""),
    MyQueueItem("Fatima Bello", "Intake", "₦320,000", "")
)

@Composable
fun MyQueueScreen(
    applications: List<LoanApplicationModel> = emptyList(),
    borrowers: List<BorrowerModel> = emptyList(),
    onBackClick: () -> Unit,
    onViewApplication: (String) -> Unit = {}
) {
    val queueItems = remember(applications, borrowers) {
        if (applications.isNotEmpty()) {
            applications.map { app ->
                val borrower = borrowers.find { it.id == app.borrower_id }
                MyQueueItem(
                    applicantName = borrower?.name ?: "Unknown Applicant",
                    stage = app.status.replaceFirstChar { it.uppercase(Locale.getDefault()) },
                    amount = "₦${String.format(Locale.US, "%,.0f", app.amount)}",
                    appId = app.id
                )
            }
        } else placeholderItems
    }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        topBar = {
            FieldTopAppBar(
                title = "Action Required",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // High-End Priority Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FieldTheme.colors.purple600.copy(alpha = 0.05f))
                    .border(width = 0.5.dp, color = FieldTheme.colors.purple600.copy(alpha = 0.1f))
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "My Active Tasks",
                    style = FieldTheme.typography.title.copy(fontSize = 28.sp),
                    color = FieldTheme.colors.gray100
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You have ${queueItems.size} dossiers requiring immediate review or processing.",
                    style = FieldTheme.typography.body.copy(fontSize = 14.sp),
                    color = FieldTheme.colors.gray400
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(5) {
                        FieldCard(modifier = Modifier.fillMaxWidth().height(88.dp)) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LoadingSkeleton(height = 48.dp, width = 48.dp, cornerRadius = 24.dp)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    LoadingSkeleton(height = 16.dp, width = 140.dp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LoadingSkeleton(height = 12.dp, width = 90.dp)
                                }
                                LoadingSkeleton(height = 24.dp, width = 24.dp, cornerRadius = 12.dp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(queueItems) { item ->
                        val chipVariant = when (item.stage.lowercase(Locale.getDefault())) {
                            "intake" -> StatusChipVariant.Verified
                            "ocr review", "credit review", "needs review" -> StatusChipVariant.NeedsReview
                            "approved", "bm approved" -> StatusChipVariant.Approved
                            "returned" -> StatusChipVariant.Returned
                            else -> StatusChipVariant.NeedsReview
                        }
                        val initials = item.applicantName.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")

                        FieldCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = item.appId.isNotEmpty()) { onViewApplication(item.appId) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar Circle
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(FieldTheme.colors.gray800, RoundedCornerShape(24.dp))
                                        .border(1.dp, FieldTheme.colors.gray700, RoundedCornerShape(24.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials,
                                        style = FieldTheme.typography.title.copy(fontSize = 18.sp),
                                        color = FieldTheme.colors.gray300
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.applicantName,
                                        style = FieldTheme.typography.bodyStrong.copy(fontSize = 16.sp),
                                        color = FieldTheme.colors.gray100
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.amount,
                                            style = FieldTheme.typography.mono.copy(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = FieldTheme.colors.purple200
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        StatusChip(variant = chipVariant)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                // Action chevron instead of clunky box
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(FieldTheme.colors.purple600.copy(alpha = 0.1f), RoundedCornerShape(18.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = FieldIcons.ChevronRightOutlined,
                                        contentDescription = "View Task",
                                        tint = FieldTheme.colors.purple400,
                                        modifier = Modifier.size(20.dp)
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

@Preview(name = "My Queue Screen", widthDp = 411, heightDp = 850)
@Composable
fun PreviewMyQueueScreen() {
    FieldCRMTheme {
        MyQueueScreen(onBackClick = {})
    }
}
