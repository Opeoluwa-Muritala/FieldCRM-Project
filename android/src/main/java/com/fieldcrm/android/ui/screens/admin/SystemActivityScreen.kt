package com.fieldcrm.android.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fieldcrm.android.core.network.ApiResult
import com.fieldcrm.android.data.api.MobileApiService
import com.fieldcrm.android.data.api.SystemActivityItem
import com.fieldcrm.android.ui.components.EmptyState
import com.fieldcrm.android.ui.components.FieldCard
import com.fieldcrm.android.ui.components.FieldTopAppBar
import com.fieldcrm.android.ui.components.LoadingSkeleton
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme
import org.koin.compose.koinInject

private sealed interface ActivityPageState {
    data object Loading : ActivityPageState
    data class Loaded(val items: List<SystemActivityItem>, val page: Int, val total: Int) : ActivityPageState
    data object Empty : ActivityPageState
    data object PermissionDenied : ActivityPageState
    data object SessionExpired : ActivityPageState
    data class Error(val message: String) : ActivityPageState
}

@Composable
fun SystemActivityScreen(onBackClick: (() -> Unit)? = null) {
    val api: MobileApiService = koinInject()
    var page by remember { mutableIntStateOf(1) }
    var state by remember { mutableStateOf<ActivityPageState>(ActivityPageState.Loading) }
    var selected by remember { mutableStateOf<SystemActivityItem?>(null) }

    LaunchedEffect(page) {
        state = ActivityPageState.Loading
        state = when (val result = api.getSystemActivity(page, 25)) {
            is ApiResult.Success -> if (result.data.items.isEmpty()) ActivityPageState.Empty
                else ActivityPageState.Loaded(result.data.items, result.data.page, result.data.total)
            is ApiResult.Error -> when (result.statusCode) {
                401 -> ActivityPageState.SessionExpired
                403 -> ActivityPageState.PermissionDenied
                else -> ActivityPageState.Error(result.detail)
            }
            is ApiResult.NetworkError -> ActivityPageState.Error(result.message)
            ApiResult.Loading -> ActivityPageState.Loading
        }
    }

    selected?.let { item ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(item.event_type.replace('_', ' ').replaceFirstChar { it.uppercase() }) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Actor: ${item.actor_name ?: item.triggered_role ?: "Not available"}")
                    Text("Resource: ${item.loan_id ?: "Not available"}")
                    Text("Stage: ${listOfNotNull(item.from_stage, item.to_stage).joinToString(" → ").ifBlank { "Not available" }}")
                    Text("Time: ${item.created_at}")
                    if (!item.notes.isNullOrBlank()) Text("Notes: ${item.notes}")
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Close") } }
        )
    }

    Scaffold(
        topBar = {
            FieldTopAppBar(
                title = "System Activity",
                navigationIcon = if (onBackClick != null) {
                    {
                        IconButton(onClick = onBackClick) {
                            Icon(FieldIcons.ArrowBackOutlined, "Back", tint = FieldTheme.colors.gray400)
                        }
                    }
                } else null
            )
        },
        containerColor = FieldTheme.colors.gray950
    ) { padding ->
        when (val current = state) {
            ActivityPageState.Loading -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) { items(6) { LoadingSkeleton(height = 76.dp) } }
            ActivityPageState.Empty -> Box(Modifier.fillMaxSize().padding(padding)) { EmptyState("No system activity has been recorded.") }
            ActivityPageState.PermissionDenied -> Box(Modifier.fillMaxSize().padding(padding)) { EmptyState("You do not have permission to view system activity.") }
            ActivityPageState.SessionExpired -> Box(Modifier.fillMaxSize().padding(padding)) { EmptyState("Your session has expired. Sign in again.") }
            is ActivityPageState.Error -> Box(Modifier.fillMaxSize().padding(padding)) { EmptyState(current.message) }
            is ActivityPageState.Loaded -> Column(Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(current.items, key = { it.id }) { item -> SystemActivityRow(item) { selected = item } }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(enabled = page > 1, onClick = { page-- }) { Text("Previous") }
                    Text("Page ${current.page}", color = FieldTheme.colors.gray400)
                    TextButton(enabled = current.page * 25 < current.total, onClick = { page++ }) { Text("Next") }
                }
            }
        }
    }
}

@Composable
private fun SystemActivityRow(item: SystemActivityItem, onClick: () -> Unit) {
    FieldCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(item.event_type.replace('_', ' ').replaceFirstChar { it.uppercase() }, style = FieldTheme.typography.bodyStrong, color = FieldTheme.colors.gray100)
            Text(item.actor_name ?: item.triggered_role ?: "Unknown actor", style = FieldTheme.typography.body, color = FieldTheme.colors.gray400)
            Text(item.created_at, style = FieldTheme.typography.mono, color = FieldTheme.colors.gray500)
        }
    }
}
