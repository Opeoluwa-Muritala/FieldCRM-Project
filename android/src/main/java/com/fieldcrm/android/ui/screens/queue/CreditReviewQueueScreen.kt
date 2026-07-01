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
import java.util.Locale

private data class CreditReviewItem(
    val applicantName: String,
    val productType: String,
    val amount: String,
    val tenure: String,
    val appId: String
)

private val placeholderCreditItems = listOf(
    CreditReviewItem("Adaeze Okonkwo", "SME Loan", "₦500,000", "6 MO", "app_001"),
    CreditReviewItem("Emeka Chukwu", "Asset Loan", "₦1,200,000", "12 MO", "app_002"),
    CreditReviewItem("Ngozi Adeyemi", "Working Capital", "₦350,000", "3 MO", "app_003"),
    CreditReviewItem("Chukwuemeka Eze", "SME Loan", "₦750,000", "9 MO", "app_004"),
    CreditReviewItem("Fatima Bello", "Agric Loan", "₦200,000", "6 MO", "app_005")
)

@Composable
fun CreditReviewQueueScreen(
    onBackClick: () -> Unit,
    onReviewApplication: (String) -> Unit = {}
) {
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        topBar = {
            FieldTopAppBar(
                title = "Review Queue",
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
                            text = "${placeholderCreditItems.size} QUEUED",
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
                    items(placeholderCreditItems) { item ->
                        FieldCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onReviewApplication(item.appId) }
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
