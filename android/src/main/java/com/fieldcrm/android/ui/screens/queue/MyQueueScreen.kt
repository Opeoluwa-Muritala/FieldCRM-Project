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
                title = "My Queue",
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
                            text = "${queueItems.size} ITEMS",
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
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    LoadingSkeleton(height = 16.dp, width = 140.dp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LoadingSkeleton(height = 12.dp, width = 90.dp)
                                }
                                LoadingSkeleton(height = 20.dp, width = 60.dp, cornerRadius = 10.dp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(queueItems) { item ->
                        val chipVariant = when (item.stage.lowercase(Locale.getDefault())) {
                            "intake" -> StatusChipVariant.Verified
                            "ocr review" -> StatusChipVariant.NeedsReview
                            "needs review" -> StatusChipVariant.NeedsReview
                            "approved" -> StatusChipVariant.Approved
                            "returned" -> StatusChipVariant.Returned
                            else -> StatusChipVariant.NeedsReview
                        }

                        FieldCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = item.appId.isNotEmpty()) { onViewApplication(item.appId) }
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
                                    Text(
                                        text = item.amount,
                                        style = FieldTheme.typography.mono.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = FieldTheme.colors.gray300
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    StatusChip(variant = chipVariant)
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
                                            .clickable { onViewApplication(item.appId) }
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "VIEW",
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

@Preview(name = "My Queue Screen", widthDp = 411, heightDp = 850)
@Composable
fun PreviewMyQueueScreen() {
    FieldCRMTheme {
        MyQueueScreen(onBackClick = {})
    }
}
