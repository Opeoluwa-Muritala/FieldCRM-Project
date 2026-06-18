package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import java.util.Locale

data class AuditLogEvent(
    val timestamp: String,
    val actor: String,
    val role: String,
    val description: String,
    val stateDiff: String?,
    val isMine: Boolean
)

@Composable
fun WorkflowEventAuditScreen(
    onBackClick: () -> Unit
) {
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filters = listOf("ALL CONSOLE LOGS", "MY ACTIVITIES", "UNDERWRITING", "DISBURSEMENTS")

    val events = listOf(
        AuditLogEvent(
            timestamp = "2026-06-18 13:40",
            actor = "Chidi Okafor",
            role = "Loan Officer",
            description = "Created initial borrower intake profile",
            stateDiff = "Name: Adaeze Okonkwo\nBVN: 222333444",
            isMine = true
        ),
        AuditLogEvent(
            timestamp = "2026-06-18 13:52",
            actor = "Fatima Al-Hassan",
            role = "Credit Officer",
            description = "Completed credit evaluation score pull",
            stateDiff = "Score: 740\nDTI Ratio: 32%",
            isMine = false
        ),
        AuditLogEvent(
            timestamp = "2026-06-18 14:02",
            actor = "Samuel Adebayo",
            role = "Branch Manager",
            description = "Approved loan application and signed collateral receipts",
            stateDiff = null,
            isMine = false
        )
    )

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "Immutable Workflow Audit Trail",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
            // Horizontal scrollable filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FieldTheme.colors.gray900)
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEachIndexed { index, filter ->
                    val isSelected = index == selectedFilterIndex
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) FieldTheme.colors.purple600 else FieldTheme.colors.gray800,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { selectedFilterIndex = index }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filter,
                            style = FieldTheme.typography.label.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            color = if (isSelected) Color.White else FieldTheme.colors.gray400
                        )
                    }
                }
            }
            
            // Audit Log List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val filteredEvents = if (selectedFilterIndex == 1) {
                    events.filter { it.isMine }
                } else {
                    events
                }
                
                itemsIndexed(filteredEvents) { index, item ->
                    AuditTrailEntry(
                        timestamp = item.timestamp,
                        actorName = item.actor,
                        actorRole = item.role,
                        action = item.description,
                        diff = item.stateDiff,
                        isCurrentUserAction = item.isMine
                    )
                }
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(name = "Compact Phone Audit Trail", widthDp = 411, heightDp = 850)
@Composable
fun PreviewAuditTrailCompact() {
    FieldCRMTheme {
        WorkflowEventAuditScreen(onBackClick = {})
    }
}
