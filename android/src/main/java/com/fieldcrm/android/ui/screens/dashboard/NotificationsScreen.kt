package com.fieldcrm.android.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.ui.viewmodel.Screen
import com.fieldcrm.android.ui.components.FieldCard
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
                            imageVector = if (isTablet) Icons.Outlined.Close else Icons.AutoMirrored.Outlined.ArrowBack,
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
                                text = "Clear",
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
            if (notifications.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = "All Caught Up",
                        tint = FieldTheme.colors.purple600,
                        modifier = Modifier
                            .size(80.dp)
                            .alpha(0.3f)
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
                        text = "When you have new notifications, they'll show up here.",
                        style = FieldTheme.typography.body,
                        color = FieldTheme.colors.gray400,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(notifications) { notif ->
                        val rowBg = if (!notif.isRead) {
                            FieldTheme.colors.purple900.copy(alpha = 0.1f)
                        } else {
                            FieldTheme.colors.gray900
                        }
                        
                        val borderStroke = if (!notif.isRead) {
                            androidx.compose.foundation.BorderStroke(1.dp, FieldTheme.colors.purple600)
                        } else {
                            androidx.compose.foundation.BorderStroke(0.5.dp, FieldTheme.colors.gray800)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNotificationClick(notif) },
                            colors = CardDefaults.cardColors(containerColor = rowBg),
                            border = borderStroke,
                            shape = RoundedCornerShape(FieldTheme.shapes.cardRadius)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Dot unread status indicator
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp, end = 12.dp)
                                        .size(8.dp)
                                        .background(
                                            color = if (!notif.isRead) FieldTheme.colors.purple600 else FieldTheme.colors.gray600,
                                            shape = CircleShape
                                        )
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = notif.title,
                                        style = if (!notif.isRead) FieldTheme.typography.bodyStrong else FieldTheme.typography.body,
                                        color = FieldTheme.colors.gray100
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = notif.message,
                                        style = FieldTheme.typography.body.copy(fontSize = 14.sp),
                                        color = FieldTheme.colors.gray400
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = notif.time,
                                        style = FieldTheme.typography.label,
                                        color = FieldTheme.colors.gray500
                                    )
                                }
                                
                                IconButton(
                                    onClick = { onDismissNotification(notif) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Dismiss",
                                        tint = FieldTheme.colors.gray500,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

