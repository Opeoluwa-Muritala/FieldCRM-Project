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

private data class AuditEvent(
    val action: String,
    val actorName: String,
    val actorRole: String,
    val timestamp: String,
    val stageChange: String?,
    val diff: String?
)

private val placeholderAuditEvents = listOf(
    AuditEvent(
        action = "Application submitted for OCR processing",
        actorName = "Samuel Okeke",
        actorRole = "Loan Officer",
        timestamp = "2026-07-01 09:14",
        stageChange = "Intake → OCR Review",
        diff = null
    ),
    AuditEvent(
        action = "OCR fields extracted and confidence scores assigned",
        actorName = "System",
        actorRole = "System Admin",
        timestamp = "2026-07-01 09:15",
        stageChange = null,
        diff = "BVN: 42% | Full Name: 88% | Loan Amount: 91%"
    ),
    AuditEvent(
        action = "BVN field flagged as low confidence — manual review required",
        actorName = "Aisha Mohammed",
        actorRole = "Credit Officer",
        timestamp = "2026-07-01 10:02",
        stageChange = null,
        diff = null
    ),
    AuditEvent(
        action = "BVN manually corrected and verified",
        actorName = "Aisha Mohammed",
        actorRole = "Credit Officer",
        timestamp = "2026-07-01 10:18",
        stageChange = "OCR Review → Credit Review",
        diff = "BVN: 22244455566 → 22244455567"
    ),
    AuditEvent(
        action = "Credit review completed — application recommended for approval",
        actorName = "Chidi Okafor",
        actorRole = "Branch Manager",
        timestamp = "2026-07-01 14:30",
        stageChange = "Credit Review → Approved",
        diff = null
    )
)

@Composable
fun AuditTrailScreen(
    onBackClick: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950),
        topBar = {
            FieldTopAppBar(
                title = "Audit Trail",
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
                            text = "${placeholderAuditEvents.size} EVENTS",
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
                        FieldCard(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    LoadingSkeleton(height = 14.dp, width = 120.dp)
                                    LoadingSkeleton(height = 12.dp, width = 80.dp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LoadingSkeleton(height = 12.dp, width = 240.dp)
                                Spacer(modifier = Modifier.height(6.dp))
                                LoadingSkeleton(height = 12.dp, width = 180.dp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(placeholderAuditEvents) { event ->
                        Column {
                            AuditTrailEntry(
                                timestamp = event.timestamp,
                                actorName = event.actorName,
                                actorRole = event.actorRole,
                                action = event.action,
                                diff = event.diff,
                                isCurrentUserAction = event.actorRole == "Loan Officer"
                            )
                            if (event.stageChange != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                FieldTheme.colors.purple950,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .border(
                                                0.5.dp,
                                                FieldTheme.colors.purple400.copy(alpha = 0.4f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = event.stageChange,
                                            style = FieldTheme.typography.mono.copy(fontSize = 10.sp),
                                            color = FieldTheme.colors.purple300
                                        )
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

@Preview(name = "Audit Trail Screen", widthDp = 411, heightDp = 850)
@Composable
fun PreviewAuditTrailScreen() {
    FieldCRMTheme {
        AuditTrailScreen(onBackClick = {})
    }
}
