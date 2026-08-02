package com.fieldcrm.android.ui.screens.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.data.api.ApiNotification
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.viewmodel.NotificationsViewModel
import com.fieldcrm.android.ui.viewmodel.Screen
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun NotificationsScreen(
    onBackClick: () -> Unit,
    onNavigateTo: (Screen, String?) -> Unit,
    viewModel: NotificationsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    fun handleNotificationTap(notif: ApiNotification) {
        // Tapping a row marks read as a side effect of navigation
        viewModel.markRead(notif.id)
        if (notif.application_id != null) {
            onNavigateTo(Screen.ApplicationDetail, notif.application_id)
        } else {
            onNavigateTo(Screen.Dashboard, null)
        }
    }

    NotificationsContent(
        notifications = uiState.notifications,
        isLoading = uiState.isLoading,
        onBackClick = onBackClick,
        onMarkAllRead = { viewModel.markAllRead() },
        onNotificationClick = { handleNotificationTap(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotificationsContent(
    notifications: List<ApiNotification>,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onMarkAllRead: () -> Unit,
    onNotificationClick: (ApiNotification) -> Unit
) {
    val hasUnreads = remember(notifications) { notifications.any { !it.is_read } }

    // Grouping notifications under "Today" / "Earlier"
    val todayStart = remember {
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
    }
    val (todayNotifications, earlierNotifications) = remember(notifications) {
        notifications.partition { notif ->
            try {
                val instant = Instant.parse(notif.created_at)
                instant.isAfter(todayStart)
            } catch (e: Exception) {
                true
            }
        }
    }

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
                            imageVector = FieldIcons.ArrowBackOutlined,
                            contentDescription = "Back",
                            tint = FieldTheme.colors.gray400
                        )
                    }
                },
                actions = {
                    if (hasUnreads) {
                        TextButton(
                            onClick = onMarkAllRead,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = "Mark all read",
                                style = FieldTheme.typography.bodyStrong,
                                color = FieldTheme.colors.purple400
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
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp)
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
                            style = FieldTheme.typography.title,
                            color = FieldTheme.colors.gray100,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        if (todayNotifications.isNotEmpty()) {
                            stickyHeader {
                                NotificationHeader(title = "Today")
                            }
                            items(todayNotifications, key = { it.id }) { notif ->
                                NotificationRowItem(
                                    notification = notif,
                                    onClick = { onNotificationClick(notif) }
                                )
                            }
                        }

                        if (earlierNotifications.isNotEmpty()) {
                            stickyHeader {
                                NotificationHeader(title = "Earlier")
                            }
                            items(earlierNotifications, key = { it.id }) { notif ->
                                NotificationRowItem(
                                    notification = notif,
                                    onClick = { onNotificationClick(notif) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(FieldTheme.colors.gray950)
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = FieldTheme.typography.label.copy(fontSize = 11.sp, letterSpacing = 1.sp),
            color = FieldTheme.colors.purple400,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun NotificationRowItem(
    notification: ApiNotification,
    onClick: () -> Unit
) {
    val rowBg = if (!notification.is_read) {
        FieldTheme.colors.purple900.copy(alpha = 0.08f)
    } else {
        FieldTheme.colors.gray900
    }
    val border = if (!notification.is_read) {
        androidx.compose.foundation.BorderStroke(1.dp, FieldTheme.colors.purple600.copy(alpha = 0.5f))
    } else {
        androidx.compose.foundation.BorderStroke(0.5.dp, FieldTheme.colors.gray800)
    }

    // Key icon based on type
    val notifIcon: ImageVector = when (notification.type.lowercase()) {
        "approval", "decision" -> FieldIcons.CheckCircleOutlined
        "document", "sync" -> FieldIcons.QueueOutlined
        "system", "alert" -> FieldIcons.AlertOutlined
        else -> FieldIcons.InfoOutlined
    }

    val iconColor = if (!notification.is_read) {
        FieldTheme.colors.purple400
    } else {
        FieldTheme.colors.gray500
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = rowBg),
        border = border,
        shape = RoundedCornerShape(FieldTheme.shapes.cardRadius)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Unread dot indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (!notification.is_read) FieldTheme.colors.purple600 else Color.Transparent,
                        shape = CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Type keyed Icon badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (!notification.is_read) FieldTheme.colors.purple900.copy(alpha = 0.2f) else FieldTheme.colors.gray850,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = notifIcon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.message,
                    style = if (!notification.is_read) FieldTheme.typography.bodyStrong else FieldTheme.typography.body,
                    color = FieldTheme.colors.gray100
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatRelativeTime(notification.created_at),
                    style = FieldTheme.typography.label.copy(fontSize = 11.sp),
                    color = FieldTheme.colors.gray500
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
                .ofPattern("d MMM yyyy")
                .withZone(ZoneId.systemDefault())
                .format(instant)
        }
    } catch (e: Exception) {
        isoTimestamp
    }
}
