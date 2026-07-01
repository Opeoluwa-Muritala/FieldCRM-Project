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

private data class PipelineEntry(
    val applicantName: String,
    val amount: String,
    val stage: String,
    val appId: String
)

private val placeholderPipelineEntries = listOf(
    PipelineEntry("Adaeze Okonkwo", "₦250,000", "Intake", "app_001"),
    PipelineEntry("Fatima Bello", "₦320,000", "Intake", "app_002"),
    PipelineEntry("Emeka Chukwu", "₦1,200,000", "OCR Review", "app_003"),
    PipelineEntry("Chinedu Obi", "₦450,000", "OCR Review", "app_004"),
    PipelineEntry("Ngozi Adeyemi", "₦350,000", "Credit Review", "app_005"),
    PipelineEntry("Chukwuemeka Eze", "₦750,000", "Credit Review", "app_006"),
    PipelineEntry("Amaka Okafor", "₦600,000", "Approved", "app_007"),
    PipelineEntry("Bola Tinubu-Adeyemi", "₦900,000", "Disbursed", "app_008")
)

private val stageOrder = listOf("Intake", "OCR Review", "Credit Review", "Approved", "Disbursed")

@Composable
fun PipelineScreen(
    onBackClick: () -> Unit,
    onViewApplication: (String) -> Unit = {}
) {
    var isLoading by remember { mutableStateOf(false) }

    val groupedByStage = remember(placeholderPipelineEntries) {
        stageOrder.associateWith { stage ->
            placeholderPipelineEntries.filter { it.stage == stage }
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
                            text = "${placeholderPipelineEntries.size} TOTAL",
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
                                    "Intake" -> StatusChipVariant.Verified
                                    "OCR Review" -> StatusChipVariant.NeedsReview
                                    "Credit Review" -> StatusChipVariant.NeedsReview
                                    "Approved" -> StatusChipVariant.Approved
                                    "Disbursed" -> StatusChipVariant.Signed
                                    else -> StatusChipVariant.NeedsReview
                                }

                                FieldCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                        .clickable { onViewApplication(entry.appId) }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entry.applicantName,
                                                style = FieldTheme.typography.bodyStrong,
                                                color = FieldTheme.colors.gray100
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
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
