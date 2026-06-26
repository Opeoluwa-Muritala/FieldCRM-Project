package com.fieldcrm.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.viewmodel.Screen
import com.fieldcrm.android.ui.theme.FieldTheme

data class NotificationModel(
    val id: String,
    val title: String,
    val message: String,
    val time: String,
    val isRead: Boolean,
    val destination: Screen
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBackClick: () -> Unit,
    onNavigateTo: (Screen) -> Unit
) {
    var notifications by remember {
        mutableStateOf(
            listOf(
                NotificationModel(
                    id = "1",
                    title = "Application returned",
                    message = "MMFB-041 needs correction",
                    time = "2 minutes ago",
                    isRead = false,
                    destination = Screen.ApplicationList
                ),
                NotificationModel(
                    id = "2",
                    title = "Visitation signed off",
                    message = "Branch Manager concurred",
                    time = "1 hour ago",
                    isRead = true,
                    destination = Screen.ApplicationList
                ),
                NotificationModel(
                    id = "3",
                    title = "Document upload complete",
                    message = "Guarantor proof of address verified",
                    time = "3 hours ago",
                    isRead = true,
                    destination = Screen.ApplicationList
                )
            )
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(FieldTheme.colors.gray950)
    ) {
        val isTablet = maxWidth >= 600.dp

        if (isTablet) {
            // Tablet Layout: Drawer-style panel overlay
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
                        .background(FieldTheme.colors.gray900)
                ) {
                    NotificationsContent(
                        notifications = notifications,
                        onBackClick = onBackClick,
                        onClearAll = { notifications = emptyList() },
                        onNotificationClick = { notif ->
                            notifications = notifications.map {
                                if (it.id == notif.id) it.copy(isRead = true) else it
                            }
                            onNavigateTo(notif.destination)
                        },
                        onDismissNotification = { notif ->
                            notifications = notifications.filter { it.id != notif.id }
                        },
                        isTablet = true
                    )
                }
            }
        } else {
            // Phone Layout: Full Screen
            NotificationsContent(
                notifications = notifications,
                onBackClick = onBackClick,
                onClearAll = { notifications = emptyList() },
                onNotificationClick = { notif ->
                    notifications = notifications.map {
                        if (it.id == notif.id) it.copy(isRead = true) else it
                    }
                    onNavigateTo(notif.destination)
                },
                onDismissNotification = { notif ->
                    notifications = notifications.filter { it.id != notif.id }
                },
                isTablet = false
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsContent(
    notifications: List<NotificationModel>,
    onBackClick: () -> Unit,
    onClearAll: () -> Unit,
    onNotificationClick: (NotificationModel) -> Unit,
    onDismissNotification: (NotificationModel) -> Unit,
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
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isTablet) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
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
                                text = "Clear All",
                                style = FieldTheme.typography.bodyStrong,
                                color = FieldTheme.colors.purple600
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FieldTheme.colors.gray900
                )
            )
        },
        containerColor = FieldTheme.colors.gray950
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (notifications.isEmpty()) {
                // Empty state conforming to SKILL rules: Shield mark + caught up text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "No Notifications",
                        tint = FieldTheme.colors.gray500,
                        modifier = Modifier
                            .size(72.dp)
                            .alpha(0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "You're all caught up",
                        style = FieldTheme.typography.display,
                        color = FieldTheme.colors.gray300,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No new alerts or action items waiting for you.",
                        style = FieldTheme.typography.body,
                        color = FieldTheme.colors.gray500,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(notifications) { notif ->
                        val rowBg = if (!notif.isRead) {
                            FieldTheme.colors.purple950.copy(alpha = 0.2f)
                        } else {
                            FieldTheme.colors.gray900
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(rowBg)
                                .clickable { onNotificationClick(notif) }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Unread status indicator
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (!notif.isRead) FieldTheme.colors.purple600 else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = notif.title,
                                    style = if (!notif.isRead) FieldTheme.typography.bodyStrong else FieldTheme.typography.body,
                                    color = FieldTheme.colors.gray100
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = notif.message,
                                    style = FieldTheme.typography.body,
                                    color = FieldTheme.colors.gray400
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = notif.time,
                                    style = FieldTheme.typography.label,
                                    color = FieldTheme.colors.gray500
                                )
                            }
                            
                            // Dismiss action button (minimum 48dp tap target area)
                            IconButton(
                                onClick = { onDismissNotification(notif) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = FieldTheme.colors.gray500,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
