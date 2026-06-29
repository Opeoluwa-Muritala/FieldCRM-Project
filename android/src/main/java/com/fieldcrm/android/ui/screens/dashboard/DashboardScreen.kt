package com.fieldcrm.android.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.theme.FieldIcons
import java.util.Locale

@Composable
fun DashboardScreenView(
    role: UserRole?,
    onNavigateToBorrowers: () -> Unit,
    onNavigateToApplications: () -> Unit,
    onNavigateToOfflineQueue: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSearchResults: () -> Unit = {}
) {
    val resolvedRole = role ?: UserRole.LOAN_OFFICER
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var syncInProgress by remember { mutableStateOf(false) }
    var showSignOutConfirmation by remember { mutableStateOf(false) }

    // User details mapping
    val (userName, userEmail) = when (resolvedRole) {
        UserRole.LOAN_OFFICER -> "Chidi Okafor" to "chidi@mainstreetmfb.com"
        UserRole.BRANCH_MANAGER -> "Alhaji Ibrahim" to "ibrahim@mainstreetmfb.com"
        UserRole.CREDIT_OFFICER -> "Tunde Bakare" to "tunde@mainstreetmfb.com"
        UserRole.AUDITOR -> "Sarah Philip" to "sarah@mainstreetmfb.com"
        UserRole.ADMIN_MCR -> "Kemi Adeosun" to "kemi@mainstreetmfb.com"
    }

    // Role-specific metrics mapping
    val metrics = when (resolvedRole) {
        UserRole.LOAN_OFFICER -> listOf(
            MetricData("12", "APPS TODAY", Icons.Outlined.Assignment, FieldTheme.colors.purple600),
            MetricData("2", "PENDING SYNC", Icons.Outlined.CloudQueue, FieldTheme.colors.statusWarning),
            MetricData("4", "VISITS DUE", Icons.Outlined.LocationOn, FieldTheme.colors.purple600),
            MetricData("3", "MISSING DOCS", Icons.Outlined.ErrorOutline, FieldTheme.colors.statusDanger)
        )
        UserRole.BRANCH_MANAGER -> listOf(
            MetricData("5", "AWAITING SIGNOFF", Icons.Outlined.RateReview, FieldTheme.colors.statusWarning),
            MetricData("₦14.2M", "BRANCH DISBURSED", Icons.Outlined.Payments, FieldTheme.colors.statusSuccess),
            MetricData("94%", "TARGET MET", Icons.Outlined.TrendingUp, FieldTheme.colors.purple600),
            MetricData("8", "ACTIVE AGENTS", Icons.Outlined.Group, FieldTheme.colors.purple600)
        )
        UserRole.CREDIT_OFFICER -> listOf(
            MetricData("8", "UNDERWRITING QUEUE", Icons.Outlined.FactCheck, FieldTheme.colors.purple600),
            MetricData("24m", "AVG TURNAROUND", Icons.Outlined.Timer, FieldTheme.colors.purple600),
            MetricData("3", "HIGH RISK CASES", Icons.Outlined.WarningAmber, FieldTheme.colors.statusDanger),
            MetricData("12", "APPROVED TODAY", Icons.Outlined.CheckCircleOutline, FieldTheme.colors.statusSuccess)
        )
        UserRole.AUDITOR -> listOf(
            MetricData("14", "FLAGS RAISED", Icons.Outlined.Report, FieldTheme.colors.statusDanger),
            MetricData("98.2%", "OCR CONFIDENCE", Icons.Outlined.AutoFixHigh, FieldTheme.colors.statusSuccess),
            MetricData("2", "POLICY BREACHES", Icons.Outlined.Gavel, FieldTheme.colors.statusWarning),
            MetricData("42", "AUDITED TODAY", Icons.Outlined.Task, FieldTheme.colors.purple600)
        )
        UserRole.ADMIN_MCR -> listOf(
            MetricData("6", "BOARD TICKETS", Icons.Outlined.Description, FieldTheme.colors.statusWarning),
            MetricData("₦84.0M", "MCR DISBURSED", Icons.Outlined.AccountBalance, FieldTheme.colors.statusSuccess),
            MetricData("0", "ALERT ESCALATIONS", Icons.Outlined.NotificationsActive, FieldTheme.colors.purple600),
            MetricData("12", "DECISIONS SIGNED", Icons.Outlined.DoneAll, FieldTheme.colors.statusSuccess)
        )
    }

    // Role-specific Work Queue Items mapping
    val rawQueueItems = when (resolvedRole) {
        UserRole.LOAN_OFFICER -> listOf(
            QueueItem("Adaeze Kalu", "MMFB-041", "Missing Guarantor Signature", StatusChipVariant.NeedsReview),
            QueueItem("Bola Tinub.", "MMFB-039", "Visitation Geotag Pending", StatusChipVariant.LowConfidence),
            QueueItem("Chioma Eze", "MMFB-022", "Ready for Concurrence", StatusChipVariant.Verified)
        )
        UserRole.BRANCH_MANAGER -> listOf(
            QueueItem("Adaeze Kalu", "MMFB-041", "Concurrence review needed", StatusChipVariant.NeedsReview),
            QueueItem("Musa Bello", "MMFB-037", "High value ticket (₦2.0M)", StatusChipVariant.Verified),
            QueueItem("Ngozi Obi", "MMFB-031", "Returned for Guarantor Address", StatusChipVariant.Returned)
        )
        UserRole.CREDIT_OFFICER -> listOf(
            QueueItem("David Okoro", "MMFB-054", "Credit Risk Analysis required", StatusChipVariant.NeedsReview),
            QueueItem("Joy Amadi", "MMFB-051", "OCR mismatch on Passport photo", StatusChipVariant.LowConfidence),
            QueueItem("Chike Okafor", "MMFB-049", "Verified Income (₦850k)", StatusChipVariant.Verified)
        )
        UserRole.AUDITOR -> listOf(
            QueueItem("Kalu Udoh", "MMFB-044", "OCR confidence below threshold (42%)", StatusChipVariant.LowConfidence),
            QueueItem("Emeka Onu", "MMFB-042", "Geotag LGA mismatch alert", StatusChipVariant.NeedsReview),
            QueueItem("Mary Jane", "MMFB-040", "Perfect OCR check details", StatusChipVariant.Verified)
        )
        UserRole.ADMIN_MCR -> listOf(
            QueueItem("Musa Bello", "MMFB-037", "Awaiting MCR Sign-off (₦2.0M)", StatusChipVariant.NeedsReview),
            QueueItem("Fatima Yusuf", "MMFB-028", "Awaiting Disbursal Approval", StatusChipVariant.Approved),
            QueueItem("Obinna K.", "MMFB-025", "MCR concurrence completed", StatusChipVariant.Signed)
        )
    }

    // Filter queue items based on search query
    val filteredQueueItems = remember(rawQueueItems, searchQuery) {
        if (searchQuery.isBlank()) rawQueueItems else {
            rawQueueItems.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.refNo.contains(searchQuery, ignoreCase = true) ||
                it.detail.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Handlers for quick action clicks
    val onQuickActionClick = { actionType: String ->
        when (actionType) {
            "REG_BORROWER" -> onNavigateToBorrowers()
            "NEW_APP" -> onNavigateToApplications()
            "SYNC_QUEUE" -> onNavigateToOfflineQueue()
            "VISITS" -> selectedTab = 1
            "NOTIFICATIONS" -> onNavigateToNotifications()
            "SEARCH" -> onNavigateToSearchResults()
            "SIGNOUT" -> showSignOutConfirmation = true
        }
    }

    if (showSignOutConfirmation) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FieldTheme.colors.gray950),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
                    FieldCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            // Outlined signout icon
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(FieldTheme.colors.statusDanger.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                                    contentDescription = "Sign Out",
                                    tint = FieldTheme.colors.statusDanger,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Sign Out",
                                style = FieldTheme.typography.display.copy(fontSize = 20.sp),
                                color = FieldTheme.colors.gray100,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Are you sure you want to sign out of FieldCRM? Any unsynced data will be stored locally on this device.",
                                style = FieldTheme.typography.body,
                                color = FieldTheme.colors.gray400,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(32.dp))

                            PrimaryButton(
                                text = "Sign Out",
                                onClick = onLogout,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            SecondaryButton(
                                text = "Cancel",
                                onClick = { showSignOutConfirmation = false },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    } else {
        if (isTablet) {
            // Tablet Navigation Side Rail Layout
            val sideRailItems = listOf(
                NavigationItem("Home", FieldIcons.HomeOutlined, FieldIcons.HomeFilled),
                NavigationItem("Queue", FieldIcons.QueueOutlined, FieldIcons.QueueFilled),
                NavigationItem("Sync", FieldIcons.SyncOutlined, FieldIcons.SyncFilled),
                NavigationItem("Settings", FieldIcons.SettingsOutlined, FieldIcons.SettingsFilled)
            )

            Row(modifier = Modifier.fillMaxSize().background(FieldTheme.colors.gray950)) {
                FieldNavigationRail(
                    items = sideRailItems,
                    selectedItemIndex = selectedTab,
                    onItemSelect = { selectedTab = it }
                )

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (selectedTab) {
                        0 -> TabletDashboardHome(
                            userName = userName,
                            role = resolvedRole,
                            metrics = metrics,
                            queueItems = filteredQueueItems,
                            onQuickActionClick = onQuickActionClick
                        )
                        1 -> QueueTab(
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            queueItems = filteredQueueItems,
                            onItemClick = { refNo -> onNavigateToApplications() }
                        )
                        2 -> SyncTab(
                            syncInProgress = syncInProgress,
                            onStartSync = {
                                syncInProgress = true
                            }
                        )
                        3 -> SettingsScreen(
                            userName = userName,
                            userEmail = userEmail,
                            role = resolvedRole,
                            onBackClick = { selectedTab = 0 },
                            onNavigateToOfflineQueue = onNavigateToOfflineQueue,
                            onSignOutClick = { showSignOutConfirmation = true }
                        )
                    }
                }
            }
        } else {
            // Phone Bottom Navigation Layout
            val bottomBarItems = listOf(
                NavigationItem("Home", FieldIcons.HomeOutlined, FieldIcons.HomeFilled),
                NavigationItem("Queue", FieldIcons.QueueOutlined, FieldIcons.QueueFilled),
                NavigationItem("Sync", FieldIcons.SyncOutlined, FieldIcons.SyncFilled),
                NavigationItem("Settings", FieldIcons.SettingsOutlined, FieldIcons.SettingsFilled)
            )

            Scaffold(
                bottomBar = {
                    FieldBottomBar(
                        items = bottomBarItems,
                        selectedItemIndex = selectedTab,
                        onItemSelect = { selectedTab = it }
                    )
                },
                containerColor = FieldTheme.colors.gray950
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (selectedTab) {
                        0 -> PhoneDashboardHome(
                            userName = userName,
                            role = resolvedRole,
                            metrics = metrics,
                            queueItems = filteredQueueItems,
                            onQuickActionClick = onQuickActionClick
                        )
                        1 -> QueueTab(
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            queueItems = filteredQueueItems,
                            onItemClick = { refNo -> onNavigateToApplications() }
                        )
                        2 -> SyncTab(
                            syncInProgress = syncInProgress,
                            onStartSync = {
                                syncInProgress = true
                            }
                        )
                        3 -> SettingsScreen(
                            userName = userName,
                            userEmail = userEmail,
                            role = resolvedRole,
                            onBackClick = { selectedTab = 0 },
                            onNavigateToOfflineQueue = onNavigateToOfflineQueue,
                            onSignOutClick = { showSignOutConfirmation = true }
                        )
                    }
                }
            }
        }
    }

    // Simulate Sync Progress
    LaunchedEffect(syncInProgress) {
        if (syncInProgress) {
            kotlinx.coroutines.delay(2000)
            syncInProgress = false
        }
    }
}

// ==========================================
// METRIC DATA MODEL
// ==========================================
data class MetricData(
    val value: String,
    val label: String,
    val icon: ImageVector,
    val tint: Color
)

// ==========================================
// WORK QUEUE ITEM MODEL
// ==========================================
data class QueueItem(
    val name: String,
    val refNo: String,
    val detail: String,
    val status: StatusChipVariant
)

// ==========================================
// PHONE DASHBOARD VIEW
// ==========================================
@Composable
fun PhoneDashboardHome(
    userName: String,
    role: UserRole,
    metrics: List<MetricData>,
    queueItems: List<QueueItem>,
    onQuickActionClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Brand & Logo Area
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(FieldTheme.colors.purple900.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = FieldTheme.colors.purple600,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "FIELDRCM",
                        style = FieldTheme.typography.title.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = FieldTheme.colors.gray100
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { onQuickActionClick("SEARCH") },
                        modifier = Modifier
                            .size(40.dp)
                            .background(FieldTheme.colors.gray900, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = FieldTheme.colors.gray400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { onQuickActionClick("NOTIFICATIONS") },
                        modifier = Modifier
                            .size(40.dp)
                            .background(FieldTheme.colors.gray900, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = FieldTheme.colors.gray400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Welcome / Greeting Banner
        item {
            FieldCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Good morning,",
                            style = FieldTheme.typography.body,
                            color = FieldTheme.colors.gray400
                        )
                        Text(
                            text = userName,
                            style = FieldTheme.typography.display.copy(fontSize = 24.sp),
                            color = FieldTheme.colors.gray100
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = role.displayName.uppercase(Locale.getDefault()),
                            style = FieldTheme.typography.label.copy(
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            ),
                            color = FieldTheme.colors.purple600
                        )
                    }
                    // Profile Icon Initials Circle
                    val initials = userName.split(" ").map { it.take(1) }.joinToString("").uppercase()
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(FieldTheme.colors.purple900.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = FieldTheme.typography.bodyStrong.copy(fontSize = 14.sp),
                            color = FieldTheme.colors.purple600
                        )
                    }
                }
            }
        }

        // Metrics Grid (2x2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "PERFORMANCE OVERVIEW",
                    style = FieldTheme.typography.label,
                    color = FieldTheme.colors.gray500
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.weight(1f)) { MetricCard(metrics[0]) }
                    Box(modifier = Modifier.weight(1f)) { MetricCard(metrics[1]) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.weight(1f)) { MetricCard(metrics[2]) }
                    Box(modifier = Modifier.weight(1f)) { MetricCard(metrics[3]) }
                }
            }
        }

        // Quick Actions Scroll Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "QUICK SHUTTLES",
                    style = FieldTheme.typography.label,
                    color = FieldTheme.colors.gray500
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (role) {
                        UserRole.LOAN_OFFICER -> {
                            item { ShuttleChip("New Client", Icons.Outlined.PersonAdd) { onQuickActionClick("REG_BORROWER") } }
                            item { ShuttleChip("New Loan", Icons.Outlined.NoteAdd) { onQuickActionClick("NEW_APP") } }
                            item { ShuttleChip("Offline Queue", Icons.Outlined.CloudQueue) { onQuickActionClick("SYNC_QUEUE") } }
                            item { ShuttleChip("Route Visits", Icons.Outlined.Map) { onQuickActionClick("VISITS") } }
                        }
                        UserRole.BRANCH_MANAGER -> {
                            item { ShuttleChip("Underwriting Queue", Icons.Outlined.RateReview) { onQuickActionClick("VISITS") } }
                            item { ShuttleChip("Offline Database", Icons.Outlined.CloudQueue) { onQuickActionClick("SYNC_QUEUE") } }
                            item { ShuttleChip("Sign Out", Icons.Outlined.ExitToApp) { onQuickActionClick("SIGNOUT") } }
                        }
                        UserRole.CREDIT_OFFICER -> {
                            item { ShuttleChip("Assess Queue", Icons.Outlined.FactCheck) { onQuickActionClick("VISITS") } }
                            item { ShuttleChip("Offline Queue", Icons.Outlined.CloudQueue) { onQuickActionClick("SYNC_QUEUE") } }
                            item { ShuttleChip("Sign Out", Icons.Outlined.ExitToApp) { onQuickActionClick("SIGNOUT") } }
                        }
                        else -> {
                            item { ShuttleChip("View Queue", Icons.Outlined.List) { onQuickActionClick("VISITS") } }
                            item { ShuttleChip("Sign Out", Icons.Outlined.ExitToApp) { onQuickActionClick("SIGNOUT") } }
                        }
                    }
                }
            }
        }

        // Priority Tasks Feed
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "PRIORITY ACTION FEED",
                    style = FieldTheme.typography.label,
                    color = FieldTheme.colors.gray500
                )
                if (queueItems.isEmpty()) {
                    EmptyState(text = "No pending actions found.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        queueItems.forEach { item ->
                            ActionFeedCard(item, onActionClick = { onQuickActionClick("NEW_APP") })
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TABLET DASHBOARD VIEW
// ==========================================
@Composable
fun TabletDashboardHome(
    userName: String,
    role: UserRole,
    metrics: List<MetricData>,
    queueItems: List<QueueItem>,
    onQuickActionClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Left Column: Greetings, Metrics & Quick Shuttles
        Column(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(FieldTheme.colors.purple900.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = FieldTheme.colors.purple600,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "FIELDRCM TABLET",
                        style = FieldTheme.typography.title.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = FieldTheme.colors.gray100
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { onQuickActionClick("SEARCH") }) {
                        Icon(Icons.Outlined.Search, "Search", tint = FieldTheme.colors.gray400)
                    }
                    IconButton(onClick = { onQuickActionClick("NOTIFICATIONS") }) {
                        Icon(Icons.Outlined.Notifications, "Notifications", tint = FieldTheme.colors.gray400)
                    }
                }
            }

            // Welcome Card
            FieldCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Good morning,",
                            style = FieldTheme.typography.body,
                            color = FieldTheme.colors.gray400
                        )
                        Text(
                            text = userName,
                            style = FieldTheme.typography.display.copy(fontSize = 28.sp),
                            color = FieldTheme.colors.gray100
                        )
                        Text(
                            text = role.displayName.uppercase(Locale.getDefault()) + " · INTUITIVE OVERVIEW",
                            style = FieldTheme.typography.label.copy(fontSize = 11.sp, letterSpacing = 1.sp),
                            color = FieldTheme.colors.purple600
                        )
                    }
                }
            }

            // Metrics Grid (4 columns side-by-side on tablet)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "PERFORMANCE METRICS",
                    style = FieldTheme.typography.label,
                    color = FieldTheme.colors.gray500
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    metrics.forEach { metric ->
                        Box(modifier = Modifier.weight(1f)) {
                            MetricCard(metric)
                        }
                    }
                }
            }

            // Shuttles
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "QUICK SHUTTLES",
                    style = FieldTheme.typography.label,
                    color = FieldTheme.colors.gray500
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (role) {
                        UserRole.LOAN_OFFICER -> {
                            ShuttleChip("New Client", Icons.Outlined.PersonAdd) { onQuickActionClick("REG_BORROWER") }
                            ShuttleChip("New Loan", Icons.Outlined.NoteAdd) { onQuickActionClick("NEW_APP") }
                            ShuttleChip("Offline Sync", Icons.Outlined.CloudQueue) { onQuickActionClick("SYNC_QUEUE") }
                            ShuttleChip("Visits Map", Icons.Outlined.Map) { onQuickActionClick("VISITS") }
                        }
                        UserRole.BRANCH_MANAGER -> {
                            ShuttleChip("Underwrite", Icons.Outlined.RateReview) { onQuickActionClick("VISITS") }
                            ShuttleChip("Offline Db", Icons.Outlined.CloudQueue) { onQuickActionClick("SYNC_QUEUE") }
                            ShuttleChip("Sign Out", Icons.Outlined.ExitToApp) { onQuickActionClick("SIGNOUT") }
                        }
                        else -> {
                            ShuttleChip("View Queue", Icons.Outlined.List) { onQuickActionClick("VISITS") }
                            ShuttleChip("Sign Out", Icons.Outlined.ExitToApp) { onQuickActionClick("SIGNOUT") }
                        }
                    }
                }
            }
        }

        // Right Column: Priority Task Feed / Action Center
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "ACTION CENTER",
                style = FieldTheme.typography.label,
                color = FieldTheme.colors.gray500
            )

            FieldCard(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (queueItems.isEmpty()) {
                        item {
                            EmptyState(text = "No pending tasks in queue.")
                        }
                    } else {
                        items(queueItems) { item ->
                            ActionFeedCard(item, onActionClick = { onQuickActionClick("NEW_APP") })
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// WORK QUEUE TAB VIEW
// ==========================================
@Composable
fun QueueTab(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    queueItems: List<QueueItem>,
    onItemClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Active Work Queue",
            style = FieldTheme.typography.display.copy(fontSize = 22.sp),
            color = FieldTheme.colors.gray100
        )

        FieldTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            label = "Filter Active Queue",
            placeholder = "Filter by client name, ref number...",
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = FieldTheme.colors.gray500
                )
            }
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (queueItems.isEmpty()) {
                item {
                    EmptyState(text = "No match found for '$searchQuery'.")
                }
            } else {
                items(queueItems) { item ->
                    FieldCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemClick(item.refNo) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = item.name,
                                        style = FieldTheme.typography.bodyStrong,
                                        color = FieldTheme.colors.gray100
                                    )
                                    Text(
                                        text = item.refNo,
                                        style = FieldTheme.typography.mono.copy(fontSize = 11.sp),
                                        color = FieldTheme.colors.purple400
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.detail,
                                    style = FieldTheme.typography.body.copy(fontSize = 13.sp),
                                    color = FieldTheme.colors.gray400
                                )
                            }
                            StatusChip(variant = item.status)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// OFFLINE SYNC TAB VIEW
// ==========================================
@Composable
fun SyncTab(
    syncInProgress: Boolean,
    onStartSync: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(FieldTheme.colors.purple900.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (syncInProgress) Icons.Outlined.Sync else Icons.Outlined.CloudQueue,
                contentDescription = "Sync Queue",
                tint = FieldTheme.colors.purple600,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (syncInProgress) "Syncing Database..." else "Offline Sync Manager",
            style = FieldTheme.typography.display.copy(fontSize = 22.sp),
            color = FieldTheme.colors.gray100,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (syncInProgress) {
                "Uploading cached intake logs and verification records to Mainstreet core banking database..."
            } else {
                "You have 2 pending loan applications and 3 biometric logs cached locally. Sync now to push changes."
            },
            style = FieldTheme.typography.body,
            color = FieldTheme.colors.gray400,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (syncInProgress) {
            CircularProgressIndicator(
                color = FieldTheme.colors.purple600,
                modifier = Modifier.size(36.dp)
            )
        } else {
            PrimaryButton(
                text = "Sync Database (5 items)",
                onClick = onStartSync,
                modifier = Modifier.widthIn(max = 280.dp)
            )
        }
    }
}

// ==========================================
// SUB COMPONENT: METRIC CARD
// ==========================================
@Composable
fun MetricCard(data: MetricData) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, FieldTheme.colors.gray800, RoundedCornerShape(FieldTheme.shapes.cardRadius))
            .background(FieldTheme.colors.gray900, RoundedCornerShape(FieldTheme.shapes.cardRadius))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.label,
                    style = FieldTheme.typography.label.copy(fontSize = 9.sp, letterSpacing = 0.5.sp),
                    color = FieldTheme.colors.gray500
                )
                Icon(
                    imageVector = data.icon,
                    contentDescription = null,
                    tint = data.tint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = data.value,
                style = FieldTheme.typography.display.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = FieldTheme.colors.gray100
            )
        }
    }
}

// ==========================================
// SUB COMPONENT: SHUTTLE CHIP
// ==========================================
@Composable
fun ShuttleChip(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .border(0.5.dp, FieldTheme.colors.purple600.copy(alpha = 0.3f), RoundedCornerShape(FieldTheme.shapes.inputRadius))
            .background(FieldTheme.colors.purple900.copy(alpha = 0.05f), RoundedCornerShape(FieldTheme.shapes.inputRadius))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FieldTheme.colors.purple600,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                style = FieldTheme.typography.bodyStrong.copy(fontSize = 12.sp),
                color = FieldTheme.colors.gray100
            )
        }
    }
}

// ==========================================
// SUB COMPONENT: ACTION FEED CARD
// ==========================================
@Composable
fun ActionFeedCard(
    item: QueueItem,
    onActionClick: () -> Unit
) {
    FieldCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.name,
                        style = FieldTheme.typography.bodyStrong,
                        color = FieldTheme.colors.gray100
                    )
                    Text(
                        text = item.refNo,
                        style = FieldTheme.typography.mono.copy(fontSize = 11.sp),
                        color = FieldTheme.colors.purple400
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.detail,
                    style = FieldTheme.typography.body.copy(fontSize = 13.sp),
                    color = FieldTheme.colors.gray400
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                StatusChip(variant = item.status)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ACTION",
                    style = FieldTheme.typography.label.copy(fontSize = 10.sp),
                    color = FieldTheme.colors.purple600,
                    modifier = Modifier
                        .clickable(onClick = onActionClick)
                        .padding(4.dp)
                )
            }
        }
    }
}
