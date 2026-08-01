package com.fieldcrm.android.ui.screens.audit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.fieldcrm.android.data.api.MobileApiService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.compose.koinInject

private data class ComplianceFlag(
    val flagType: String,
    val applicantName: String,
    val status: String,
    val raisedAt: String,
    val flagId: String
)

@Composable
fun ComplianceFlagsScreen(
    onBackClick: () -> Unit,
    onViewFlag: (String) -> Unit = {}
) {
    val api: MobileApiService = koinInject()
    var flags by remember { mutableStateOf<List<ComplianceFlag>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        loadError = null
        val response = api.getQueue("compliance-flags")
        flags = try {
            val root = response?.let(Json::parseToJsonElement)?.jsonObject
            root?.get("items")?.jsonArray.orEmpty().map { element ->
                val item = element.jsonObject
                fun text(key: String) = item[key]?.jsonPrimitive?.contentOrNull.orEmpty()
                ComplianceFlag(
                    flagType = text("flag_label").ifBlank { text("flag_type") },
                    applicantName = text("applicant_name"),
                    status = text("flag_status"),
                    raisedAt = text("created_at"),
                    flagId = text("loan_id")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
        if (response == null) loadError = "Unable to load compliance flags."
        isLoading = false
    }

    val openCount = flags.size

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        topBar = {
            FieldTopAppBar(
                title = "Compliance Flags",
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
                                FieldTheme.colors.statusDanger.copy(alpha = 0.12f),
                                RoundedCornerShape(FieldTheme.shapes.cardRadius)
                            )
                            .border(
                                0.5.dp,
                                FieldTheme.colors.statusDanger.copy(alpha = 0.4f),
                                RoundedCornerShape(FieldTheme.shapes.cardRadius)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$openCount OPEN",
                            style = FieldTheme.typography.mono.copy(fontSize = 10.sp),
                            color = FieldTheme.colors.statusDanger
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
                                    LoadingSkeleton(height = 16.dp, width = 150.dp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LoadingSkeleton(height = 12.dp, width = 100.dp)
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
                    loadError?.let { message -> item { Text(message, color = FieldTheme.colors.statusDanger) } }
                    if (flags.isEmpty() && loadError == null) {
                        item { EmptyState(text = "No compliance flags found.") }
                    }
                    items(flags) { flag ->
                        val chipVariant = when (flag.status) {
                            "resolved", "verified" -> StatusChipVariant.Verified
                            else -> StatusChipVariant.NeedsReview
                        }

                        FieldCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = flag.flagType,
                                        style = FieldTheme.typography.bodyStrong,
                                        color = if (flag.status !in setOf("resolved", "verified")) FieldTheme.colors.statusDanger
                                        else FieldTheme.colors.gray100
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = flag.applicantName,
                                        style = FieldTheme.typography.body.copy(fontSize = 12.sp),
                                        color = FieldTheme.colors.gray300
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = flag.raisedAt,
                                        style = FieldTheme.typography.mono.copy(fontSize = 10.sp),
                                        color = FieldTheme.colors.gray500
                                    )
                                }
                                StatusChip(variant = chipVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Compliance Flags Screen", widthDp = 411, heightDp = 850)
@Composable
fun PreviewComplianceFlagsScreen() {
    FieldCRMTheme {
        ComplianceFlagsScreen(onBackClick = {})
    }
}
