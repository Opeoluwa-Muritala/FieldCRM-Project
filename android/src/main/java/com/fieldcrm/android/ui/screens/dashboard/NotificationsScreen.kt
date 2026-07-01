package com.fieldcrm.android.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.fieldcrm.android.ui.theme.FieldIcons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.data.api.ApiNotification
import com.fieldcrm.android.ui.viewmodel.NotificationsViewModel
import com.fieldcrm.android.ui.viewmodel.Screen
import com.fieldcrm.android.ui.theme.FieldTheme
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// onNavigateTo: destination screen + optional application_id when tapping a loan notification
@Composable
fun NotificationsScreen(
    onBackClick: () -> Unit,
    onNavigateTo: (Screen, String?) -> Unit,
    viewModel: NotificationsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    fun handleTap(notif: ApiNotification) {
        viewModel.markRead(notif.id)
        if (notif.application_id != null) {
            onNavigateTo(Screen.ApplicationDetail, notif.application_id)
        } else {
            onNavigateTo(Screen.Dashboard, null)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950)
    ) {
        val isTablet = maxWidth >= 600.dp

        if (isTablet) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { onBackClick() }
                )
                Box(
                    modifier = Modifier
                        .width(380.dp)
                        .fillMaxHeight()
                        .background(FieldTheme.colors.gray950)
                ) {
                    NotificationsContent(
                        notifications = uiState.notifications,
                        isLoading = uiState.isLoading,
                        onBackClick = onBackClick,
                        onClearAll = { viewModel.clearAll() },
                        onNotificationClick = { handleTap(it) },
                        onDismissNotification = { viewModel.dismiss(it.id) },
                        isTablet = true
                    )
                }
            }
        } else {
            NotificationsContent(
                notifications = uiState.notifications,
                isLoading = uiState.isLoading,
                onBackClick = onBackClick,
                onClearAll = { viewModel.clearAll() },
                onNotificationClick = { handleTap(it) },
                onDismissNotification = { viewModel.dismiss(it.id) },
                isTablet = false
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsContent(
    notifications: List<ApiNotification>,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onClearAll: () -> Unit,
    onNotificationClick: (ApiNotification) -> Unit,
    onDismissNotification: (ApiNotification) -> Unit,
    isTablet: Boolean
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notifications",
                        style = FieldTheme.typography.title,
                        color = FieldTheme.colors.gray100
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = if (isTablet) FieldIcons.CloseOutlined else FieldIcons.ArrowBackOutlined,
                            contentDescription = "Close",
                            tint = FieldTheme.colors.gray400
                        )
                    }
                },
                actions = {
                    if (notifications.isNotEmpty()) {
                        TextButton(
                            onClick = onClearAll,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = "Clear all",
                                style = FieldTheme.typography.bodyStrong,
                                color = FieldTheme.colors.purple600
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FieldTheme.colors.gray950
                )
            )
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = FieldTheme.colors.purple600)
                    }
                }

                notifications.isEmpty() -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize().padding(32.dp)
                    ) {
                        Icon(
                            imageVector = FieldIcons.ShieldOutlined,
                            contentDescription = null,
                            tint = FieldTheme.colors.purple600,
                            modifier = Modifier.size(80.dp).alpha(0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "You're all caught up",
                            style = FieldTheme.typography.display,
                            color = FieldTheme.colors.gray100,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "New activity on your applications will appear here.",
                            style = FieldTheme.typography.body,
                            color = FieldTheme.colors.gray400,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(notifications, key = { it.id }) { notif ->
                            NotificationCard(
                                notification = notif,
                                onClick = { onNotificationClick(notif) },
                                onDismiss = { onDismissNotification(notif) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: ApiNotification,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val rowBg = if (!notification.is_read) {
        FieldTheme.colors.purple900.copy(alpha = 0.1f)
    } else {
        FieldTheme.colors.gray900
    }
    val border = if (!notification.is_read) {
        androidx.compose.foundation.BorderStroke(1.dp, FieldTheme.colors.purple600)
    } else {
        androidx.compose.foundation.BorderStroke(0.5.dp, FieldTheme.colors.gray800)
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = rowBg),
        border = border,
        shape = RoundedCornerShape(FieldTheme.shapes.cardRadius)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp, end = 12.dp)
                    .size(8.dp)
                    .background(
                        color = if (!notification.is_read) FieldTheme.colors.purple600 else FieldTheme.colors.gray600,
                        shape = CircleShape
                    )
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = if (!notification.is_read) FieldTheme.typography.bodyStrong else FieldTheme.typography.body,
                    color = FieldTheme.colors.gray100
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    style = FieldTheme.typography.body.copy(fontSize = 14.sp),
                    color = FieldTheme.colors.gray400
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatRelativeTime(notification.created_at),
                    style = FieldTheme.typography.label,
                    color = FieldTheme.colors.gray500
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = FieldIcons.CloseOutlined,
                    contentDescription = "Dismiss",
                    tint = FieldTheme.colors.gray500,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun formatRelativeTime(isoTimestamp: String): String {
    return try {
        val instant = Instant.parse(isoTimestamp)
        val now = Instant.now()
        val minutes = ChronoUnit.MINUTES.between(instant, now)
        when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            minutes < 1440 -> "${minutes / 60}h ago"
            minutes < 10080 -> "${minutes / 1440}d ago"
            else -> DateTimeFormatter
                .ofPattern("d MMM")
                .withZone(ZoneId.systemDefault())
                .format(instant)
        }
    } catch (e: Exception) {
        isoTimestamp
    }
}
