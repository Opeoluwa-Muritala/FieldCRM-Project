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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel
import java.util.Locale

private val EXECUTIVE_STATUSES = setOf("executive_approval", "crm_review")

@Composable
fun ExecutiveQueueScreen(
    applications: List<LoanApplicationModel> = emptyList(),
    borrowers: List<BorrowerModel> = emptyList(),
    onBackClick: () -> Unit,
    onReviewApplication: (String) -> Unit = {}
) {
    val queueItems = remember(applications, borrowers) {
        applications
            .filter { it.stage in EXECUTIVE_STATUSES }
            .map { app ->
                val borrower = borrowers.find { it.id == app.id }
                Triple(
                    borrower?.name ?: app.applicant_name,
                    "₦${String.format(Locale.US, "%,.0f", app.amount ?: 0.0)}",
                    app.id
                )
            }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        topBar = {
            FieldTopAppBar(
                title = "Executive Approval Queue",
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
                            text = "${queueItems.size} PENDING",
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

            if (queueItems.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No applications pending executive approval.",
                        color = FieldTheme.colors.gray500,
                        style = FieldTheme.typography.body
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(queueItems) { (name, amount, appId) ->
                        FieldCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onReviewApplication(appId) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        style = FieldTheme.typography.bodyStrong,
                                        color = FieldTheme.colors.gray100
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = amount,
                                        style = FieldTheme.typography.mono.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = FieldTheme.colors.gray300
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
