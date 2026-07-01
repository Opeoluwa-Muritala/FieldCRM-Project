package com.fieldcrm.android.ui.screens.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.fieldcrm.android.ui.theme.FieldIcons
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
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.viewmodel.AuditTrailViewModel
import org.koin.androidx.compose.koinViewModel
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
    applicationId: String = "",
    onBackClick: () -> Unit
) {
    val viewModel: AuditTrailViewModel = koinViewModel()
    val auditState by viewModel.uiState.collectAsState()

    LaunchedEffect(applicationId) {
        if (applicationId.isNotEmpty()) viewModel.load(applicationId)
    }

    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filters = listOf("ALL CONSOLE LOGS", "MY ACTIVITIES", "UNDERWRITING", "DISBURSEMENTS")

    val events = auditState.events.map { e ->
        AuditLogEvent(
            timestamp = e.timestamp.take(16).replace("T", " "),
            actor = e.actor_name,
            role = e.actor_role.replace("_", " ").replaceFirstChar { it.uppercaseChar() },
            description = e.action,
            stateDiff = e.state_diff.takeIf { it.isNotBlank() && it != "- → -" },
            isMine = e.is_mine
        )
    }

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "Immutable Workflow Audit Trail",
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
            // Horizontal scrollable filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FieldTheme.colors.gray900)
                    .horizontalScroll(rememberScrollState())
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
                                fontSize = 10.sp
                            ),
                            color = if (isSelected) Color.White else FieldTheme.colors.gray400
                        )
                    }
                }
            }
            
            // Audit Log List
            if (auditState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FieldTheme.colors.purple600)
                }
            } else {
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

                    if (filteredEvents.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillParentMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.Text(
                                    text = if (applicationId.isEmpty()) "No application selected" else "No audit events found",
                                    style = FieldTheme.typography.body,
                                    color = FieldTheme.colors.gray500
                                )
                            }
                        }
                    }

                    itemsIndexed(filteredEvents) { _, item ->
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
