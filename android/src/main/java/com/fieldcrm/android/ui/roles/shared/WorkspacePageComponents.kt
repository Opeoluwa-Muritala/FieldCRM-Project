package com.fieldcrm.android.ui.roles.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.components.EmptyState
import com.fieldcrm.android.ui.components.FieldCard
import com.fieldcrm.android.ui.components.LoadingSkeleton
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.viewmodel.Screen

data class WorkspaceMetric(val label: String, val value: String)
data class WorkspaceAction(val label: String, val destination: Screen)

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceDashboardPage(
    title: String,
    subtitle: String,
    userName: String,
    userRole: String,
    metrics: List<WorkspaceMetric>,
    actions: List<WorkspaceAction>,
    isLoading: Boolean,
    error: String?,
    onOpen: (Screen) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val initial = remember(userName) {
        userName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "Welcome back",
                            style = FieldTheme.typography.label.copy(fontSize = 11.sp),
                            color = FieldTheme.colors.gray400
                        )
                        Text(
                            text = if (userName.isNotBlank()) userName else "User",
                            style = FieldTheme.typography.bodyStrong,
                            color = FieldTheme.colors.gray100
                        )
                        Text(userRole, style = FieldTheme.typography.label.copy(fontSize = 11.sp), color = FieldTheme.colors.purple400)
                    }
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp, end = 8.dp)
                            .size(40.dp)
                            .background(FieldTheme.colors.purple900.copy(alpha = 0.35f), CircleShape)
                            .border(1.dp, FieldTheme.colors.purple600, CircleShape),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text(initial, style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.purple400)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FieldTheme.colors.gray950)
            )
        },
        containerColor = FieldTheme.colors.gray950
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text(subtitle, style = FieldTheme.typography.body, color = FieldTheme.colors.gray400) }
            when {
                isLoading -> item {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val columns = 2
                        val cardWidth = (maxWidth - 12.dp * (columns - 1)) / columns
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), maxItemsInEachRow = columns) {
                            repeat(4) { LoadingSkeleton(modifier = Modifier.width(cardWidth), height = 104.dp, width = 320.dp) }
                        }
                    }
                }
                error != null -> item { EmptyState(error) }
                metrics.isEmpty() -> item { EmptyState("No performance data is available for this workspace.") }
                else -> item {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val columns = 2
                        val cardWidth = (maxWidth - 12.dp * (columns - 1)) / columns
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), maxItemsInEachRow = columns) {
                            metrics.take(4).forEach { metric -> PerformanceMetricCard(metric, Modifier.width(cardWidth)) }
                        }
                    }
                }
            }
            if (actions.isNotEmpty()) {
                item { Text("WORKSPACE", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(actions, key = { it.label }) { action ->
                            WorkspacePreviewCard(action = action, onOpen = onOpen)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PerformanceMetricCard(metric: WorkspaceMetric, modifier: Modifier = Modifier) {
    FieldCard(modifier = modifier.height(104.dp)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(metric.value, style = FieldTheme.typography.display, color = FieldTheme.colors.gray100)
            Text(metric.label, style = FieldTheme.typography.label, color = FieldTheme.colors.gray400)
        }
    }
}

@Composable
private fun WorkspacePreviewCard(action: WorkspaceAction, onOpen: (Screen) -> Unit) {
    val summary = when (action.destination) {
        Screen.CreateApplication -> "Start and prepare a customer application"
        Screen.SearchResults -> "Search the records available to your role"
        Screen.Users -> "View and manage organization users"
        Screen.SystemActivity -> "Inspect recent administrative activity"
        Screen.MyQueue, Screen.AwaitingConcurrence, Screen.CreditReviewQueue,
        Screen.CrmQueue, Screen.EdQueue, Screen.MdQueue, Screen.ComplianceFlags ->
            "Open the latest work awaiting your attention"
        else -> "Open this workspace and review its latest information"
    }
    FieldCard(
        modifier = Modifier
            .width(230.dp)
            .height(112.dp)
            .clickable { onOpen(action.destination) }
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(action.label, style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray100)
            Text(summary, style = FieldTheme.typography.label, color = FieldTheme.colors.gray400, maxLines = 2)
            Text("View", style = FieldTheme.typography.label, color = FieldTheme.colors.purple400)
        }
    }
}
