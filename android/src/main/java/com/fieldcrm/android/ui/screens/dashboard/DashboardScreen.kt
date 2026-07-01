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
import com.fieldcrm.android.ui.viewmodel.DashboardViewModel
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@Composable
fun DashboardScreenView(
    role: UserRole?,
    borrowers: List<BorrowerModel> = emptyList(),
    applications: List<LoanApplicationModel> = emptyList(),
    sessionEmail: String? = null,
    onNavigateToBorrowers: () -> Unit,
    onNavigateToCreateApplication: () -> Unit = {},
    onNavigateToApplication: (appId: String) -> Unit = {},
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

    val dashboardViewModel: DashboardViewModel = koinViewModel()
    val dashboardState by dashboardViewModel.uiState.collectAsState()
    val liveMetrics = dashboardState.metrics

    // Derive display name from session email, fall back to role-based placeholder
    val userName = if (!sessionEmail.isNullOrBlank()) {
        sessionEmail.substringBefore("@")
            .split(".", "_", "-")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
    } else {
        when (resolvedRole) {
            UserRole.LOAN_OFFICER -> "Loan Officer"
            UserRole.BRANCH_MANAGER -> "Branch Manager"
            UserRole.CREDIT_OFFICER -> "Credit Officer"
            UserRole.AUDITOR -> "Auditor"
            UserRole.ADMIN_MCR -> "Admin MCR"
        }
    }
    val userEmail = sessionEmail ?: ""

    // Role-specific metrics mapping — values overridden by live API data when available
    val metrics = when (resolvedRole) {
        UserRole.LOAN_OFFICER -> listOf(
            MetricData(liveMetrics?.apps_today?.toString() ?: "—", "APPS TODAY", FieldIcons.DocumentOutlined, FieldTheme.colors.purple600),
            MetricData(liveMetrics?.pending_sync?.toString() ?: "—", "PENDING SYNC", FieldIcons.SyncOutlined, FieldTheme.colors.statusWarning),
            MetricData(liveMetrics?.visits_due?.toString() ?: "—", "VISITS DUE", FieldIcons.LocationOutlined, FieldTheme.colors.purple600),
            MetricData(liveMetrics?.missing_docs?.toString() ?: "—", "MISSING DOCS", FieldIcons.AlertOutlined, FieldTheme.colors.statusDanger)
        )
        UserRole.BRANCH_MANAGER -> listOf(
            MetricData(liveMetrics?.awaiting_signoff?.toString() ?: "—", "AWAITING SIGNOFF", FieldIcons.PenOutlined, FieldTheme.colors.statusWarning),
            MetricData(liveMetrics?.let { "₦${String.format(Locale.US, "%,.1fM", it.branch_disbursed / 1_000_000)}" } ?: "—", "BRANCH DISBURSED", FieldIcons.PaymentsOutlined, FieldTheme.colors.statusSuccess),
            MetricData(liveMetrics?.let { "${it.target_met_pct}%" } ?: "—", "TARGET MET", FieldIcons.PaymentsOutlined, FieldTheme.colors.purple600),
            MetricData(liveMetrics?.active_agents?.toString() ?: "—", "ACTIVE AGENTS", FieldIcons.GroupOutlined, FieldTheme.colors.purple600)
        )
        UserRole.CREDIT_OFFICER -> listOf(
            MetricData(liveMetrics?.underwriting_queue?.toString() ?: "—", "UNDERWRITING QUEUE", FieldIcons.CheckCircleOutlined, FieldTheme.colors.purple600),
            MetricData(liveMetrics?.let { "${it.avg_turnaround_mins}m" } ?: "—", "AVG TURNAROUND", FieldIcons.ClockOutlined, FieldTheme.colors.purple600),
            MetricData(liveMetrics?.high_risk_cases?.toString() ?: "—", "HIGH RISK CASES", FieldIcons.AlertOutlined, FieldTheme.colors.statusDanger),
            MetricData(liveMetrics?.approved_today?.toString() ?: "—", "APPROVED TODAY", FieldIcons.CheckCircleOutlined, FieldTheme.colors.statusSuccess)
        )
        UserRole.AUDITOR -> listOf(
            MetricData(liveMetrics?.flags_raised?.toString() ?: "—", "FLAGS RAISED", FieldIcons.AlertOutlined, FieldTheme.colors.statusDanger),
            MetricData("—", "OCR CONFIDENCE", FieldIcons.CameraOutlined, FieldTheme.colors.statusSuccess),
            MetricData(liveMetrics?.policy_breaches?.toString() ?: "—", "POLICY BREACHES", FieldIcons.CheckCircleOutlined, FieldTheme.colors.statusWarning),
            MetricData(liveMetrics?.audited_today?.toString() ?: "—", "AUDITED TODAY", FieldIcons.QueueOutlined, FieldTheme.colors.purple600)
        )
        UserRole.ADMIN_MCR -> listOf(
            MetricData(liveMetrics?.board_tickets?.toString() ?: "—", "BOARD TICKETS", FieldIcons.DocumentOutlined, FieldTheme.colors.statusWarning),
            MetricData(liveMetrics?.let { "₦${String.format(Locale.US, "%,.1fM", it.mcr_disbursed / 1_000_000)}" } ?: "—", "MCR DISBURSED", FieldIcons.ShieldOutlined, FieldTheme.colors.statusSuccess),
            MetricData(liveMetrics?.alert_escalations?.toString() ?: "—", "ALERT ESCALATIONS", FieldIcons.BellFilled, FieldTheme.colors.purple600),
            MetricData(liveMetrics?.decisions_signed?.toString() ?: "—", "DECISIONS SIGNED", FieldIcons.CheckCircleOutlined, FieldTheme.colors.statusSuccess)
        )
    }

    // Build queue from real borrowers + applications (merged view)
    val rawQueueItems = remember(borrowers, applications) {
        borrowers.map { borrower ->
            val app = applications
                .filter { it.borrower_id == borrower.id }
                .maxByOrNull { it.current_stage }
            QueueItem(
                name = borrower.name,
                borrowerId = borrower.id,
                appId = app?.id ?: "",
                refNo = if (app != null) app.id.take(8).uppercase(Locale.getDefault()) else "NO-APP",
                detail = if (app != null)
                    "₦${String.format(Locale.US, "%,.0f", app.amount)} · ${app.product_type}"
                else
                    "No active application",
                status = when {
                    app == null -> StatusChipVariant.NeedsReview
                    app.status.lowercase(Locale.getDefault()) in listOf("approved", "bm approved") -> StatusChipVariant.Approved
                    app.status.lowercase(Locale.getDefault()) == "returned" -> StatusChipVariant.Returned
                    app.status.lowercase(Locale.getDefault()) in listOf("ocr_review", "ocr review", "credit_review", "credit review") -> StatusChipVariant.LowConfidence
                    else -> StatusChipVariant.NeedsReview
                }
            )
        }
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
            "NEW_APP" -> onNavigateToCreateApplication()
            "SYNC_QUEUE" -> onNavigateToOfflineQueue()
            "VISITS" -> selectedTab = 1
            "NOTIFICATIONS" -> onNavigateToNotifications()
            "SEARCH" -> onNavigateToSearchResults()
            "SIGNOUT" -> showSignOutConfirmation = true
        }
    }

    val onQueueItemClick = { appId: String ->
        if (appId.isNotEmpty()) onNavigateToApplication(appId)
        else onNavigateToCreateApplication()
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
                                    imageVector = FieldIcons.CloseOutlined,
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
                            onQuickActionClick = onQuickActionClick,
                            onQueueItemClick = onQueueItemClick
                        )
                        1 -> QueueTab(
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            queueItems = filteredQueueItems,
                            onItemClick = { appId -> onQueueItemClick(appId) }
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
                            onQuickActionClick = onQuickActionClick,
                            onQueueItemClick = onQueueItemClick
                        )
                        1 -> QueueTab(
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            queueItems = filteredQueueItems,
                            onItemClick = { appId -> onQueueItemClick(appId) }
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
    val borrowerId: String = "",
    val appId: String = "",
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
    onQuickActionClick: (String) -> Unit,
    onQueueItemClick: (appId: String) -> Unit = {}
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
                            imageVector = FieldIcons.ShieldOutlined,
                            contentDescription = null,
                            tint = FieldTheme.colors.purple600,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "FIELDCRM",
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
                            imageVector = FieldIcons.SearchOutlined,
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
                            imageVector = FieldIcons.BellOutlined,
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
                            item { ShuttleChip("New Client", FieldIcons.PersonAddOutlined) { onQuickActionClick("REG_BORROWER") } }
                            item { ShuttleChip("New Loan", FieldIcons.AddOutlined) { onQuickActionClick("NEW_APP") } }
                            item { ShuttleChip("Offline Queue", FieldIcons.SyncOutlined) { onQuickActionClick("SYNC_QUEUE") } }
                            item { ShuttleChip("Route Visits", FieldIcons.MapOutlined) { onQuickActionClick("VISITS") } }
                        }
                        UserRole.BRANCH_MANAGER -> {
                            item { ShuttleChip("Underwriting Queue", FieldIcons.PenOutlined) { onQuickActionClick("VISITS") } }
                            item { ShuttleChip("Offline Database", FieldIcons.SyncOutlined) { onQuickActionClick("SYNC_QUEUE") } }
                            item { ShuttleChip("Sign Out", FieldIcons.CloseOutlined) { onQuickActionClick("SIGNOUT") } }
                        }
                        UserRole.CREDIT_OFFICER -> {
                            item { ShuttleChip("Assess Queue", FieldIcons.CheckCircleOutlined) { onQuickActionClick("VISITS") } }
                            item { ShuttleChip("Offline Queue", FieldIcons.SyncOutlined) { onQuickActionClick("SYNC_QUEUE") } }
                            item { ShuttleChip("Sign Out", FieldIcons.CloseOutlined) { onQuickActionClick("SIGNOUT") } }
                        }
                        else -> {
                            item { ShuttleChip("View Queue", FieldIcons.QueueOutlined) { onQuickActionClick("VISITS") } }
                            item { ShuttleChip("Sign Out", FieldIcons.CloseOutlined) { onQuickActionClick("SIGNOUT") } }
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
                    EmptyState(text = "No borrowers found. Register a new client to begin.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        queueItems.forEach { item ->
                            ActionFeedCard(item, onActionClick = { onQueueItemClick(item.appId) })
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
    onQuickActionClick: (String) -> Unit,
    onQueueItemClick: (appId: String) -> Unit = {}
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
                            imageVector = FieldIcons.ShieldOutlined,
                            contentDescription = null,
                            tint = FieldTheme.colors.purple600,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "FIELDCRM TABLET",
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
                        Icon(FieldIcons.SearchOutlined, "Search", tint = FieldTheme.colors.gray400)
                    }
                    IconButton(onClick = { onQuickActionClick("NOTIFICATIONS") }) {
                        Icon(FieldIcons.BellOutlined, "Notifications", tint = FieldTheme.colors.gray400)
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
                            ShuttleChip("New Client", FieldIcons.PersonAddOutlined) { onQuickActionClick("REG_BORROWER") }
                            ShuttleChip("New Loan", FieldIcons.AddOutlined) { onQuickActionClick("NEW_APP") }
                            ShuttleChip("Offline Sync", FieldIcons.SyncOutlined) { onQuickActionClick("SYNC_QUEUE") }
                            ShuttleChip("Visits Map", FieldIcons.MapOutlined) { onQuickActionClick("VISITS") }
                        }
                        UserRole.BRANCH_MANAGER -> {
                            ShuttleChip("Underwrite", FieldIcons.PenOutlined) { onQuickActionClick("VISITS") }
                            ShuttleChip("Offline Db", FieldIcons.SyncOutlined) { onQuickActionClick("SYNC_QUEUE") }
                            ShuttleChip("Sign Out", FieldIcons.CloseOutlined) { onQuickActionClick("SIGNOUT") }
                        }
                        else -> {
                            ShuttleChip("View Queue", FieldIcons.QueueOutlined) { onQuickActionClick("VISITS") }
                            ShuttleChip("Sign Out", FieldIcons.CloseOutlined) { onQuickActionClick("SIGNOUT") }
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
                            EmptyState(text = "No borrowers found. Register a new client to begin.")
                        }
                    } else {
                        items(queueItems) { item ->
                            ActionFeedCard(item, onActionClick = { onQueueItemClick(item.appId) })
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
                    imageVector = FieldIcons.SearchOutlined,
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
                            .clickable { onItemClick(item.appId) }
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
                imageVector = if (syncInProgress) FieldIcons.SyncFilled else FieldIcons.SyncOutlined,
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
