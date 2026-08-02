package com.fieldcrm.android.ui.roles.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    val filteredActions = remember(actions) {
        actions.filter { action ->
            val isQueue = when (action.destination) {
                Screen.MyQueue,
                Screen.AwaitingConcurrence,
                Screen.CreditReviewQueue,
                Screen.CrmQueue,
                Screen.EdQueue,
                Screen.MdQueue,
                Screen.LegalWorkspace,
                Screen.ComplianceFlags,
                Screen.Users -> true
                else -> false
            }
            !isQueue
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (userName.isNotBlank()) userName else "User",
                            style = FieldTheme.typography.bodyStrong,
                            color = FieldTheme.colors.gray100
                        )
                        Text(
                            text = userRole,
                            style = FieldTheme.typography.label.copy(fontSize = 11.sp),
                            color = FieldTheme.colors.purple400
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateToSettings) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(FieldTheme.colors.purple900.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape)
                                .border(1.dp, FieldTheme.colors.purple600, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Icon(
                                imageVector = com.fieldcrm.android.ui.theme.FieldIcons.SettingsOutlined,
                                contentDescription = "Settings",
                                tint = FieldTheme.colors.purple400,
                                modifier = Modifier.size(18.dp)
                            )
                        }
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
                        val columns = when { maxWidth < 400.dp -> 1; maxWidth < 840.dp -> 2; else -> 3 }
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
                        val columns = when { maxWidth < 400.dp -> 1; maxWidth < 840.dp -> 2; else -> 3 }
                        val cardWidth = (maxWidth - 12.dp * (columns - 1)) / columns
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), maxItemsInEachRow = columns) {
                            metrics.take(6).forEach { metric -> PerformanceMetricCard(metric, Modifier.width(cardWidth)) }
                        }
                    }
                }
            }
            if (filteredActions.isNotEmpty()) {
                item { Text("WORKSPACE", style = FieldTheme.typography.label, color = FieldTheme.colors.gray500) }
                items(filteredActions) { action ->
                    OutlinedButton(onClick = { onOpen(action.destination) }, modifier = Modifier.fillMaxWidth()) {
                        Text(action.label)
                    }
                }
            }
        }
    }
}

@Composable
fun PerformanceMetricCard(metric: WorkspaceMetric, modifier: Modifier = Modifier) {
    FieldCard(modifier = modifier.heightIn(min = 104.dp)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(metric.value, style = FieldTheme.typography.display, color = FieldTheme.colors.gray100)
            Text(metric.label, style = FieldTheme.typography.label, color = FieldTheme.colors.gray400)
        }
    }
}
