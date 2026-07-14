package com.fieldcrm.android.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.border
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
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.ui.components.*
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.theme.FieldIcons
import com.fieldcrm.android.ui.viewmodel.DashboardViewModel
import com.fieldcrm.android.ui.viewmodel.SyncItemStatus
import com.fieldcrm.android.ui.viewmodel.SyncUiState
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel
import androidx.activity.compose.BackHandler
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@Composable
fun DashboardScreenView(
    role: UserRole?,
    borrowers: List<BorrowerModel> = emptyList(),
    applications: List<LoanApplicationModel> = emptyList(),
    isLoading: Boolean = false,
    sessionEmail: String? = null,
    onNavigateToBorrowers: () -> Unit,
    onNavigateToCreateApplication: () -> Unit = {},
    onNavigateToApplication: (appId: String) -> Unit = {},
    onLogout: () -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSearchResults: () -> Unit = {},
    onNavigateToMyQueue: () -> Unit = {},
    onNavigateToVisitsDue: () -> Unit = {},
    onNavigateToAwaitingConcurrence: () -> Unit = {},
    onNavigateToPendingSignoffs: () -> Unit = {},
    onNavigateToCreditReviewQueue: () -> Unit = {},
    onNavigateToOcrExceptions: () -> Unit = {},
    onNavigateToPipeline: () -> Unit = {},
    onNavigateToUsers: () -> Unit = {},
    onNavigateToSystemActivity: () -> Unit = {},
    onNavigateToAuditTrail: () -> Unit = {},
    onNavigateToComplianceFlags: () -> Unit = {},
    onNavigateToOfflineQueue: () -> Unit = {},
    onNavigateToCrmQueue: () -> Unit = {},
    onNavigateToExecutiveQueue: () -> Unit = {},
    onNavigateToParDashboard: () -> Unit = {},
    onNavigateToCommitteeQueue: () -> Unit = {},
    onNavigateToEdQueue: () -> Unit = {},
    onNavigateToMdQueue: () -> Unit = {},
    syncState: SyncUiState = SyncUiState(),
    onSyncNow: () -> Unit = {}
) {
    val resolvedRole = role?.legacyUiRole ?: UserRole.LOAN_OFFICER
    val activity = LocalContext.current as android.app.Activity
    val windowSizeClass = calculateWindowSizeClass(activity)
    val isTablet = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    var selectedTab by remember { mutableStateOf(0) }
    val tabHistory = remember { mutableStateListOf<Int>() }
    var searchQuery by remember { mutableStateOf("") }
    var showSignOutConfirmation by remember { mutableStateOf(false) }

    fun navigateToTab(index: Int) {
        if (index != selectedTab) {
            tabHistory.add(selectedTab)
        }
        selectedTab = index
    }

    // Back pops to previous tab; if no history, let the outer handler handle exit
    BackHandler(enabled = tabHistory.isNotEmpty()) {
        selectedTab = tabHistory.removeLast()
    }

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
            UserRole.AUDITOR -> "Auditor"
            UserRole.CRM -> "CRM Officer"
            UserRole.EXECUTIVE -> "Executive"
            UserRole.COMMITTEE -> "Committee Member"
            UserRole.ED -> "Executive Director"
            UserRole.MD -> "Managing Director"
            UserRole.SYSTEM_ADMIN -> "System Admin"
        }
    }
    val userEmail = sessionEmail ?: ""
    val failedSyncCount = syncState.items.count { it.status == SyncItemStatus.FAILED }
    val syncBanner = when {
        syncState.isSyncing -> Triple("Syncing saved changes…", SyncStatusTone.Pending, null)
        failedSyncCount > 0 -> Triple("$failedSyncCount change${if (failedSyncCount == 1) "" else "s"} need attention", SyncStatusTone.Failed, "Retry now")
        syncState.items.isNotEmpty() -> Triple("${syncState.items.size} change${if (syncState.items.size == 1) "" else "s"} saved on this device", SyncStatusTone.Pending, "Sync now")
        else -> null
    }

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
        UserRole.AUDITOR -> listOf(
            MetricData(liveMetrics?.flags_raised?.toString() ?: "—", "FLAGS RAISED", FieldIcons.AlertOutlined, FieldTheme.colors.statusDanger),
            MetricData("—", "OCR CONFIDENCE", FieldIcons.CameraOutlined, FieldTheme.colors.statusSuccess),
            MetricData(liveMetrics?.policy_breaches?.toString() ?: "—", "POLICY BREACHES", FieldIcons.CheckCircleOutlined, FieldTheme.colors.statusWarning),
            MetricData(liveMetrics?.audited_today?.toString() ?: "—", "AUDITED TODAY", FieldIcons.QueueOutlined, FieldTheme.colors.purple600)
        )
        UserRole.CRM -> listOf(
            MetricData(liveMetrics?.underwriting_queue?.toString() ?: "—", "CRM QUEUE", FieldIcons.QueueOutlined, FieldTheme.colors.purple600),
            MetricData(liveMetrics?.approved_today?.toString() ?: "—", "ADVANCED TODAY", FieldIcons.CheckCircleOutlined, FieldTheme.colors.statusSuccess),
            MetricData(liveMetrics?.high_risk_cases?.toString() ?: "—", "HIGH RISK CASES", FieldIcons.AlertOutlined, FieldTheme.colors.statusDanger),
            MetricData(liveMetrics?.missing_docs?.toString() ?: "—", "MISSING DOCS", FieldIcons.DocumentOutlined, FieldTheme.colors.statusWarning)
        )
        UserRole.EXECUTIVE -> listOf(
            MetricData(liveMetrics?.decisions_signed?.toString() ?: "—", "DECISIONS SIGNED", FieldIcons.CheckCircleOutlined, FieldTheme.colors.statusSuccess),
            MetricData(liveMetrics?.let { "₦${String.format(Locale.US, "%,.1fM", it.mcr_disbursed / 1_000_000)}" } ?: "—", "DISBURSED", FieldIcons.PaymentsOutlined, FieldTheme.colors.statusSuccess),
            MetricData(liveMetrics?.alert_escalations?.toString() ?: "—", "ESCALATIONS", FieldIcons.BellFilled, FieldTheme.colors.purple600),
            MetricData(liveMetrics?.board_tickets?.toString() ?: "—", "BOARD TICKETS", FieldIcons.DocumentOutlined, FieldTheme.colors.statusWarning)
        )
        UserRole.COMMITTEE -> listOf(
            MetricData(liveMetrics?.underwriting_queue?.toString() ?: "—", "COMMITTEE QUEUE", FieldIcons.QueueOutlined, FieldTheme.colors.purple600),
            MetricData(liveMetrics?.approved_today?.toString() ?: "—", "RECOMMENDED TODAY", FieldIcons.CheckCircleOutlined, FieldTheme.colors.statusSuccess),
            MetricData(liveMetrics?.high_risk_cases?.toString() ?: "—", "RETURNED", FieldIcons.AlertOutlined, FieldTheme.colors.statusDanger),
            MetricData("—", "PENDING VOTES", FieldIcons.PenOutlined, FieldTheme.colors.statusWarning)
        )
        UserRole.ED -> listOf(
            MetricData(liveMetrics?.underwriting_queue?.toString() ?: "—", "ED QUEUE", FieldIcons.QueueOutlined, FieldTheme.colors.purple600),
            MetricData(liveMetrics?.decisions_signed?.toString() ?: "—", "DECISIONS SIGNED", FieldIcons.CheckCircleOutlined, FieldTheme.colors.statusSuccess),
            MetricData(liveMetrics?.alert_escalations?.toString() ?: "—", "FORWARDED TO MD", FieldIcons.DocumentOutlined, FieldTheme.colors.statusWarning),
            MetricData(liveMetrics?.approved_today?.toString() ?: "—", "APPROVED TODAY", FieldIcons.PaymentsOutlined, FieldTheme.colors.purple600)
        )
        UserRole.MD -> listOf(
            MetricData(liveMetrics?.underwriting_queue?.toString() ?: "—", "MD QUEUE", FieldIcons.QueueOutlined, FieldTheme.colors.purple600),
            MetricData(liveMetrics?.decisions_signed?.toString() ?: "—", "DECISIONS SIGNED", FieldIcons.CheckCircleOutlined, FieldTheme.colors.statusSuccess),
            MetricData(liveMetrics?.let { "₦${String.format(Locale.US, "%,.1fM", it.mcr_disbursed / 1_000_000)}" } ?: "—", "VALUE APPROVED", FieldIcons.PaymentsOutlined, FieldTheme.colors.statusSuccess),
            MetricData(liveMetrics?.board_tickets?.toString() ?: "—", "BOARD REFERRALS", FieldIcons.GroupOutlined, FieldTheme.colors.statusWarning)
        )
        UserRole.SYSTEM_ADMIN -> listOf(
            MetricData(liveMetrics?.active_agents?.toString() ?: "—", "ACTIVE USERS", FieldIcons.GroupOutlined, FieldTheme.colors.purple600),
            MetricData(liveMetrics?.pending_sync?.toString() ?: "—", "PENDING SYNC", FieldIcons.SyncOutlined, FieldTheme.colors.statusWarning),
            MetricData(liveMetrics?.policy_breaches?.toString() ?: "—", "POLICY BREACHES", FieldIcons.AlertOutlined, FieldTheme.colors.statusDanger),
            MetricData(liveMetrics?.apps_today?.toString() ?: "—", "APPS TODAY", FieldIcons.DocumentOutlined, FieldTheme.colors.statusSuccess)
        )
    }

    // Role-based status sets — values must match backend _stage_status() output (title-case)
    val relevantStatuses: Set<String> = remember(resolvedRole) {
        when (resolvedRole) {
            UserRole.LOAN_OFFICER -> emptySet()
            UserRole.BRANCH_MANAGER -> setOf("credit review", "branch approval", "returned")
            UserRole.CRM -> setOf("branch approval", "crm review", "disbursement ready")
            UserRole.EXECUTIVE -> setOf("crm review", "executive approval", "disbursement ready")
            UserRole.COMMITTEE -> setOf("committee review")
            UserRole.ED -> setOf("ed approval")
            UserRole.MD -> setOf("md approval")
            UserRole.AUDITOR, UserRole.SYSTEM_ADMIN -> emptySet()
        }
    }

    // Build queue from real borrowers + applications (merged view), filtered by role
    val rawQueueItems = remember(borrowers, applications, resolvedRole) {
        borrowers.mapNotNull { borrower ->
            val app = applications
                .filter { it.id == borrower.id || it.phone == borrower.phone || it.bvn == borrower.bvn || it.applicant_name == borrower.name }
                .maxByOrNull { it.stageIndex }

            val isVisible = relevantStatuses.isEmpty() ||
                app == null ||
                app.stage in relevantStatuses

            if (!isVisible) return@mapNotNull null

            QueueItem(
                name = borrower.name,
                borrowerId = borrower.id,
                appId = app?.id ?: "",
                refNo = if (app != null) (app.ref_no.ifBlank { app.id.take(8) }).uppercase(Locale.getDefault()) else "NO-APP",
                detail = if (app != null)
                    "₦${String.format(Locale.US, "%,.0f", app.amount ?: 0.0)} · ${app.loan_type.replaceFirstChar { it.uppercase() }}"
                else
                    "No active application",
                status = when {
                    app == null -> StatusChipVariant.NeedsReview
                    app.stage in setOf("branch_approval", "executive_approval", "disbursement_ready", "disbursed") -> StatusChipVariant.Approved
                    app.stage == "returned" -> StatusChipVariant.Returned
                    app.stage in setOf("ocr_review", "crm_review", "committee_review") -> StatusChipVariant.LowConfidence
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
            "SYNC_QUEUE" -> { /* auto-sync — no manual trigger needed */ }
            "VISITS" -> navigateToTab(1)
            "NOTIFICATIONS" -> onNavigateToNotifications()
            "SEARCH" -> onNavigateToSearchResults()
            "SIGNOUT" -> showSignOutConfirmation = true
            "MY_QUEUE" -> onNavigateToMyQueue()
            "VISITS_DUE" -> onNavigateToVisitsDue()
            "AWAITING_CONCURRENCE" -> onNavigateToAwaitingConcurrence()
            "PENDING_SIGNOFFS" -> onNavigateToPendingSignoffs()
            "CREDIT_REVIEW_QUEUE" -> onNavigateToCreditReviewQueue()
            "OCR_EXCEPTIONS" -> onNavigateToOcrExceptions()
            "PIPELINE" -> onNavigateToPipeline()
            "USERS" -> onNavigateToUsers()
            "SYSTEM_ACTIVITY" -> onNavigateToSystemActivity()
            "AUDIT_TRAIL" -> onNavigateToAuditTrail()
            "COMPLIANCE_FLAGS" -> onNavigateToComplianceFlags()
            "CRM_QUEUE" -> onNavigateToCrmQueue()
            "EXECUTIVE_QUEUE" -> onNavigateToExecutiveQueue()
            "PAR_DASHBOARD" -> onNavigateToParDashboard()
            "COMMITTEE_QUEUE" -> onNavigateToCommitteeQueue()
            "ED_QUEUE" -> onNavigateToEdQueue()
            "MD_QUEUE" -> onNavigateToMdQueue()
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
                NavigationItem("Settings", FieldIcons.SettingsOutlined, FieldIcons.SettingsFilled)
            )

            Row(modifier = Modifier.fillMaxSize().background(FieldTheme.colors.gray950)) {
                FieldNavigationRail(
                    items = sideRailItems,
                    selectedItemIndex = selectedTab,
                    onItemSelect = { if (it != selectedTab) tabHistory.add(selectedTab); selectedTab = it }
                )

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (syncBanner != null) {
                        SyncStatusBar(
                            message = syncBanner.first,
                            tone = syncBanner.second,
                            actionLabel = syncBanner.third,
                            onActionClick = if (syncBanner.third != null) onSyncNow else null,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> TabletDashboardHome(
                            userName = userName,
                            role = resolvedRole,
                            metrics = metrics,
                            queueItems = filteredQueueItems,
                            isLoading = isLoading,
                            onQuickActionClick = onQuickActionClick,
                            onQueueItemClick = onQueueItemClick,
                            onNavigateToCreateApplication = onNavigateToCreateApplication
                        )
                        1 -> QueueTab(
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            queueItems = filteredQueueItems,
                            onItemClick = { appId -> onQueueItemClick(appId) }
                        )
                        2 -> SettingsScreen(
                            userName = userName,
                            userEmail = userEmail,
                            role = resolvedRole,
                            onBackClick = { navigateToTab(0) },
                            onNavigateToOfflineQueue = onNavigateToOfflineQueue,
                            onSignOutClick = { showSignOutConfirmation = true }
                        )
                    }
                    }
                }
            }
        } else {
            // Phone Bottom Navigation Layout
            val bottomBarItems = listOf(
                NavigationItem("Home", FieldIcons.HomeOutlined, FieldIcons.HomeFilled),
                NavigationItem("Queue", FieldIcons.QueueOutlined, FieldIcons.QueueFilled),
                NavigationItem("Settings", FieldIcons.SettingsOutlined, FieldIcons.SettingsFilled)
            )

            Scaffold(
                bottomBar = {
                    FieldBottomBar(
                        items = bottomBarItems,
                        selectedItemIndex = selectedTab,
                        onItemSelect = { if (it != selectedTab) tabHistory.add(selectedTab); selectedTab = it }
                    )
                },
                containerColor = FieldTheme.colors.gray950
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    if (syncBanner != null) {
                        SyncStatusBar(
                            message = syncBanner.first,
                            tone = syncBanner.second,
                            actionLabel = syncBanner.third,
                            onActionClick = if (syncBanner.third != null) onSyncNow else null,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> PhoneDashboardHome(
                            userName = userName,
                            role = resolvedRole,
                            metrics = metrics,
                            queueItems = filteredQueueItems,
                            isLoading = isLoading,
                            onQuickActionClick = onQuickActionClick,
                            onQueueItemClick = onQueueItemClick,
                            onNavigateToCreateApplication = onNavigateToCreateApplication
                        )
                        1 -> QueueTab(
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            queueItems = filteredQueueItems,
                            onItemClick = { appId -> onQueueItemClick(appId) }
                        )
                        2 -> SettingsScreen(
                            userName = userName,
                            userEmail = userEmail,
                            role = resolvedRole,
                            onBackClick = { navigateToTab(0) },
                            onNavigateToOfflineQueue = onNavigateToOfflineQueue,
                            onSignOutClick = { showSignOutConfirmation = true }
                        )
                    }
                    }
                }
            }
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
    isLoading: Boolean = false,
    onQuickActionClick: (String) -> Unit,
    onQueueItemClick: (appId: String) -> Unit = {},
    onNavigateToCreateApplication: () -> Unit = {}
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    if (metrics.any { it.value == "—" }) {
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            modifier = Modifier
                                .background(FieldTheme.colors.statusWarning.copy(alpha = 0.12f), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = FieldIcons.SyncOutlined,
                                contentDescription = "Offline",
                                tint = FieldTheme.colors.statusWarning,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "OFFLINE",
                                style = FieldTheme.typography.label,
                                color = FieldTheme.colors.statusWarning,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
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
                            item { ShuttleChip("My Queue", FieldIcons.QueueOutlined) { onQuickActionClick("MY_QUEUE") } }
                            item { ShuttleChip("Visits Due", FieldIcons.MapOutlined) { onQuickActionClick("VISITS_DUE") } }
                        }
                        UserRole.BRANCH_MANAGER -> {
                            item { ShuttleChip("Pending Signoffs", FieldIcons.PenOutlined) { onQuickActionClick("PENDING_SIGNOFFS") } }
                            item { ShuttleChip("Awaiting Concurrence", FieldIcons.CheckCircleOutlined) { onQuickActionClick("AWAITING_CONCURRENCE") } }
                            item { ShuttleChip("Pipeline", FieldIcons.QueueOutlined) { onQuickActionClick("PIPELINE") } }
                            item { ShuttleChip("View Clients", FieldIcons.PersonAddOutlined) { onQuickActionClick("REG_BORROWER") } }
                        }
                        UserRole.AUDITOR -> {
                            item { ShuttleChip("Audit Trail", FieldIcons.DocumentOutlined) { onQuickActionClick("AUDIT_TRAIL") } }
                            item { ShuttleChip("Compliance Flags", FieldIcons.AlertOutlined) { onQuickActionClick("COMPLIANCE_FLAGS") } }
                            item { ShuttleChip("Pipeline", FieldIcons.QueueOutlined) { onQuickActionClick("PIPELINE") } }
                            item { ShuttleChip("View Clients", FieldIcons.PersonAddOutlined) { onQuickActionClick("REG_BORROWER") } }
                        }
                        UserRole.CRM -> {
                            item { ShuttleChip("CRM Queue", FieldIcons.CheckCircleOutlined) { onQuickActionClick("CRM_QUEUE") } }
                            item { ShuttleChip("PAR Dashboard", FieldIcons.PaymentsOutlined) { onQuickActionClick("PAR_DASHBOARD") } }
                            item { ShuttleChip("Pipeline", FieldIcons.QueueOutlined) { onQuickActionClick("PIPELINE") } }
                            item { ShuttleChip("View Clients", FieldIcons.PersonAddOutlined) { onQuickActionClick("REG_BORROWER") } }
                        }
                        UserRole.EXECUTIVE -> {
                            item { ShuttleChip("Approvals", FieldIcons.ShieldOutlined) { onQuickActionClick("EXECUTIVE_QUEUE") } }
                            item { ShuttleChip("PAR Dashboard", FieldIcons.PaymentsOutlined) { onQuickActionClick("PAR_DASHBOARD") } }
                            item { ShuttleChip("Pipeline", FieldIcons.QueueOutlined) { onQuickActionClick("PIPELINE") } }
                        }
                        UserRole.COMMITTEE -> {
                            item { ShuttleChip("Committee Queue", FieldIcons.QueueOutlined) { onQuickActionClick("COMMITTEE_QUEUE") } }
                            item { ShuttleChip("Pipeline", FieldIcons.QueueOutlined) { onQuickActionClick("PIPELINE") } }
                        }
                        UserRole.ED -> {
                            item { ShuttleChip("ED Queue", FieldIcons.ShieldOutlined) { onQuickActionClick("ED_QUEUE") } }
                            item { ShuttleChip("PAR Dashboard", FieldIcons.PaymentsOutlined) { onQuickActionClick("PAR_DASHBOARD") } }
                            item { ShuttleChip("Pipeline", FieldIcons.QueueOutlined) { onQuickActionClick("PIPELINE") } }
                        }
                        UserRole.MD -> {
                            item { ShuttleChip("MD Queue", FieldIcons.ShieldOutlined) { onQuickActionClick("MD_QUEUE") } }
                            item { ShuttleChip("PAR Dashboard", FieldIcons.PaymentsOutlined) { onQuickActionClick("PAR_DASHBOARD") } }
                            item { ShuttleChip("Pipeline", FieldIcons.QueueOutlined) { onQuickActionClick("PIPELINE") } }
                        }
                        UserRole.SYSTEM_ADMIN -> {
                            item { ShuttleChip("Users", FieldIcons.GroupOutlined) { onQuickActionClick("USERS") } }
                            item { ShuttleChip("System Activity", FieldIcons.DocumentOutlined) { onQuickActionClick("SYSTEM_ACTIVITY") } }
                            item { ShuttleChip("Pipeline", FieldIcons.QueueOutlined) { onQuickActionClick("PIPELINE") } }
                            item { ShuttleChip("View Clients", FieldIcons.PersonAddOutlined) { onQuickActionClick("REG_BORROWER") } }
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
                if (isLoading) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(4) {
                            FieldCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        LoadingSkeleton(height = 14.dp, width = 140.dp)
                                        LoadingSkeleton(height = 10.dp, width = 90.dp)
                                    }
                                    LoadingSkeleton(height = 24.dp, width = 70.dp, cornerRadius = 12.dp)
                                }
                            }
                        }
                    }
                } else if (queueItems.isEmpty()) {
                    EmptyState(text = "No borrowers found. Register a new client to begin.")
                    Spacer(modifier = Modifier.height(12.dp))
                    PrimaryButton(
                        text = "Start New Application",
                        onClick = onNavigateToCreateApplication,
                        modifier = Modifier.fillMaxWidth(0.7f)
                    )
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
    isLoading: Boolean = false,
    onQuickActionClick: (String) -> Unit,
    onQueueItemClick: (appId: String) -> Unit = {},
    onNavigateToCreateApplication: () -> Unit = {}
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
                            ShuttleChip("My Queue", FieldIcons.QueueOutlined) { onQuickActionClick("MY_QUEUE") }
                            ShuttleChip("Visits Due", FieldIcons.MapOutlined) { onQuickActionClick("VISITS_DUE") }
                        }
                        UserRole.BRANCH_MANAGER -> {
                            ShuttleChip("Pending Signoffs", FieldIcons.PenOutlined) { onQuickActionClick("PENDING_SIGNOFFS") }
                            ShuttleChip("Concurrence", FieldIcons.CheckCircleOutlined) { onQuickActionClick("AWAITING_CONCURRENCE") }
                            ShuttleChip("Pipeline", FieldIcons.QueueOutlined) { onQuickActionClick("PIPELINE") }
                            ShuttleChip("View Clients", FieldIcons.PersonAddOutlined) { onQuickActionClick("REG_BORROWER") }
                        }
                        UserRole.AUDITOR -> {
                            ShuttleChip("Audit Trail", FieldIcons.DocumentOutlined) { onQuickActionClick("AUDIT_TRAIL") }
                            ShuttleChip("Compliance Flags", FieldIcons.AlertOutlined) { onQuickActionClick("COMPLIANCE_FLAGS") }
                            ShuttleChip("Pipeline", FieldIcons.QueueOutlined) { onQuickActionClick("PIPELINE") }
                            ShuttleChip("View Clients", FieldIcons.PersonAddOutlined) { onQuickActionClick("REG_BORROWER") }
                        }
                        UserRole.CRM -> {
                            ShuttleChip("CRM Queue", FieldIcons.CheckCircleOutlined) { onQuickActionClick("CRM_QUEUE") }
                            ShuttleChip("PAR Dashboard", FieldIcons.PaymentsOutlined) { onQuickActionClick("PAR_DASHBOARD") }
                            ShuttleChip("Pipeline", FieldIcons.QueueOutlined) { onQuickActionClick("PIPELINE") }
                            ShuttleChip("View Clients", FieldIcons.PersonAddOutlined) { onQuickActionClick("REG_BORROWER") }
                        }
                        UserRole.EXECUTIVE -> {
                            ShuttleChip("Approvals", FieldIcons.ShieldOutlined) { onQuickActionClick("EXECUTIVE_QUEUE") }
                            ShuttleChip("PAR Dashboard", FieldIcons.PaymentsOutlined) { onQuickActionClick("PAR_DASHBOARD") }
                            ShuttleChip("Pipeline", FieldIcons.QueueOutlined) { onQuickActionClick("PIPELINE") }
                        }
                        UserRole.COMMITTEE -> {
                            ShuttleChip("Committee Queue", FieldIcons.QueueOutlined) { onQuickActionClick("COMMITTEE_QUEUE") }
                            ShuttleChip("Pipeline", FieldIcons.QueueOutlined) { onQuickActionClick("PIPELINE") }
                        }
                        UserRole.ED -> {
                            ShuttleChip("ED Queue", FieldIcons.ShieldOutlined) { onQuickActionClick("ED_QUEUE") }
                            ShuttleChip("PAR Dashboard", FieldIcons.PaymentsOutlined) { onQuickActionClick("PAR_DASHBOARD") }
                            ShuttleChip("Pipeline", FieldIcons.QueueOutlined) { onQuickActionClick("PIPELINE") }
                        }
                        UserRole.MD -> {
                            ShuttleChip("MD Queue", FieldIcons.ShieldOutlined) { onQuickActionClick("MD_QUEUE") }
                            ShuttleChip("PAR Dashboard", FieldIcons.PaymentsOutlined) { onQuickActionClick("PAR_DASHBOARD") }
                            ShuttleChip("Pipeline", FieldIcons.QueueOutlined) { onQuickActionClick("PIPELINE") }
                        }
                        UserRole.SYSTEM_ADMIN -> {
                            ShuttleChip("Users", FieldIcons.GroupOutlined) { onQuickActionClick("USERS") }
                            ShuttleChip("System Activity", FieldIcons.DocumentOutlined) { onQuickActionClick("SYSTEM_ACTIVITY") }
                            ShuttleChip("Pipeline", FieldIcons.QueueOutlined) { onQuickActionClick("PIPELINE") }
                            ShuttleChip("View Clients", FieldIcons.PersonAddOutlined) { onQuickActionClick("REG_BORROWER") }
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
                    if (isLoading) {
                        items(4) {
                            FieldCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        LoadingSkeleton(height = 14.dp, width = 160.dp)
                                        LoadingSkeleton(height = 10.dp, width = 100.dp)
                                    }
                                    LoadingSkeleton(height = 24.dp, width = 80.dp, cornerRadius = 12.dp)
                                }
                            }
                        }
                    } else if (queueItems.isEmpty()) {
                        item {
                            EmptyState(text = "No borrowers found. Register a new client to begin.")
                            Spacer(modifier = Modifier.height(12.dp))
                            PrimaryButton(
                                text = "Start New Application",
                                onClick = onNavigateToCreateApplication,
                                modifier = Modifier.fillMaxWidth(0.7f)
                            )
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val initials = item.name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(FieldTheme.colors.gray800, RoundedCornerShape(24.dp))
                                    .border(1.dp, FieldTheme.colors.gray700, RoundedCornerShape(24.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    style = FieldTheme.typography.title.copy(fontSize = 16.sp),
                                    color = FieldTheme.colors.gray300
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = item.name,
                                        style = FieldTheme.typography.bodyStrong.copy(fontSize = 16.sp),
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
                                    style = FieldTheme.typography.body.copy(fontSize = 14.sp),
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
    val initials = item.name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
    FieldCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onActionClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(FieldTheme.colors.gray800, RoundedCornerShape(20.dp))
                    .border(1.dp, FieldTheme.colors.gray700, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = FieldTheme.typography.title.copy(fontSize = 14.sp),
                    color = FieldTheme.colors.gray300
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.name,
                        style = FieldTheme.typography.bodyStrong.copy(fontSize = 15.sp),
                        color = FieldTheme.colors.gray100
                    )
                    Text(
                        text = item.refNo,
                        style = FieldTheme.typography.mono.copy(fontSize = 10.sp),
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
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = FieldIcons.ChevronRightOutlined,
                contentDescription = "View",
                tint = FieldTheme.colors.gray500,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
