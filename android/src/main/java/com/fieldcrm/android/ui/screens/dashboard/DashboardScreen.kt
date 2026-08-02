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
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
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
import com.fieldcrm.android.data.api.RelationshipOfficerMetrics
import com.fieldcrm.android.data.api.DashboardMetrics
import com.fieldcrm.android.ui.viewmodel.SyncItemStatus
import com.fieldcrm.android.ui.viewmodel.SyncUiState
import com.fieldcrm.android.ui.screens.admin.SystemActivityScreen
import com.fieldcrm.android.ui.screens.admin.UsersScreen
import com.fieldcrm.android.ui.navigation.WorkspaceRegistry
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel
import androidx.activity.compose.BackHandler
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun DashboardScreenView(
    role: UserRole?,
    borrowers: List<BorrowerModel> = emptyList(),
    applications: List<LoanApplicationModel> = emptyList(),
    isLoading: Boolean = false,
    sessionEmail: String? = null,
    sessionName: String? = null,
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
    onNavigateToEdQueue: () -> Unit = {},
    onNavigateToMdQueue: () -> Unit = {},
    onNavigateToLegalWorkspace: () -> Unit = {},
    onNavigateToMccWorkspace: () -> Unit = {},
    onNavigateToInterestPresets: () -> Unit = {},
    onNavigateToBranches: () -> Unit = {},
    syncState: SyncUiState = SyncUiState(),
    onSyncNow: () -> Unit = {}
) {
    val resolvedRole = role ?: UserRole.ACCOUNT_OFFICER
    val context = LocalContext.current
    val activity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) break
            ctx = ctx.baseContext
        }
        ctx as? android.app.Activity
    }
    val isTablet = activity?.let {
        calculateWindowSizeClass(it).widthSizeClass != WindowWidthSizeClass.Compact
    } ?: false

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
    val relationshipData = liveMetrics?.data

    // Derive display name from session email, fall back to role-based placeholder
    val userName = liveMetrics?.user?.full_name?.takeIf { it.isNotBlank() }
        ?: sessionName?.takeIf { it.isNotBlank() }
        ?: if (!sessionEmail.isNullOrBlank()) {
        sessionEmail.substringBefore("@")
            .split(".", "_", "-")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
    } else {
        when (resolvedRole) {
            UserRole.LOAN_OFFICER, UserRole.ACCOUNT_OFFICER -> "Relationship Officer"
            UserRole.BRANCH_MANAGER -> "Team Lead"
            UserRole.BRANCH_SUPERVISOR -> "Supervisor"
            UserRole.AUDITOR -> "Audit"
            UserRole.CRM -> "CRM Officer"
            UserRole.EXECUTIVE -> "Executive"
            UserRole.HEAD_CRM -> "Head CRM"
            UserRole.CREDIT_ANALYST -> "Credit Analyst"
            UserRole.ED -> "Executive Director"
            UserRole.MD -> "Managing Director"
            UserRole.SYSTEM_ADMIN -> "System Admin"
            UserRole.LEGAL -> "Legal Officer"
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
        UserRole.LOAN_OFFICER, UserRole.ACCOUNT_OFFICER -> listOf(
            MetricData(relationshipData?.metrics?.my_applications?.toString() ?: liveMetrics?.apps_today?.toString() ?: "—", "APPLICATIONS TODAY", FieldIcons.DocumentOutlined, FieldTheme.colors.purple600),
            MetricData(relationshipData?.metrics?.pending_upload?.toString() ?: liveMetrics?.pending_sync?.toString() ?: "—", "PENDING SYNC", FieldIcons.SyncOutlined, FieldTheme.colors.statusWarning),
            MetricData(relationshipData?.metrics?.visits_due?.toString() ?: liveMetrics?.visits_due?.toString() ?: "—", "VISITS DUE", FieldIcons.LocationOutlined, FieldTheme.colors.purple600),
            MetricData(relationshipData?.metrics?.returned?.toString() ?: liveMetrics?.missing_docs?.toString() ?: "—", "MISSING DOCS", FieldIcons.AlertOutlined, FieldTheme.colors.statusDanger)
        )
        UserRole.BRANCH_MANAGER -> listOf(
            MetricData(relationshipData?.metrics?.awaiting_concurrence?.toString() ?: "—", "AWAITING SIGNOFF", FieldIcons.PenOutlined, FieldTheme.colors.statusWarning),
            MetricData(relationshipData?.metrics?.pending_signoffs?.toString() ?: "—", "ACTIVE AGENTS", FieldIcons.GroupOutlined, FieldTheme.colors.purple600),
            MetricData(relationshipData?.metrics?.approved_today?.toString() ?: "—", "REVIEWED TODAY", FieldIcons.CheckCircleOutlined, FieldTheme.colors.statusSuccess),
            MetricData(relationshipData?.metrics?.returned_this_week?.let { "$it%" } ?: "—", "TARGET MET", FieldIcons.PaymentsOutlined, FieldTheme.colors.purple600)
        )
        UserRole.BRANCH_SUPERVISOR -> listOf(
            MetricData(relationshipData?.metrics?.supervisory_reviews?.toString() ?: "—", "AWAITING REVIEW", FieldIcons.PenOutlined, FieldTheme.colors.statusWarning),
            MetricData(relationshipData?.metrics?.returned_this_week?.toString() ?: "—", "RETURNED THIS WEEK", FieldIcons.AlertOutlined, FieldTheme.colors.statusDanger)
        )
        UserRole.AUDITOR -> listOf(
            MetricData(relationshipData?.metrics?.unverified_documents?.toString() ?: "—", "FLAGS RAISED", FieldIcons.AlertOutlined, FieldTheme.colors.statusWarning),
            MetricData(relationshipData?.metrics?.critical_ocr_gaps?.toString() ?: "—", "POLICY BREACHES", FieldIcons.AlertOutlined, FieldTheme.colors.statusDanger),
            MetricData(relationshipData?.metrics?.workflow_exceptions?.toString() ?: "—", "AUDITED TODAY", FieldIcons.QueueOutlined, FieldTheme.colors.purple600),
            MetricData(relationshipData?.metrics?.audit_events_today?.toString() ?: "—", "WORKFLOW EXCEPTIONS", FieldIcons.DocumentOutlined, FieldTheme.colors.purple600)
        )
        UserRole.CRM -> listOf(
            MetricData(relationshipData?.metrics?.crm_queue?.toString() ?: "—", "DOSSIERS TO REVIEW", FieldIcons.QueueOutlined, FieldTheme.colors.purple600),
            MetricData(relationshipData?.metrics?.disbursed_total?.toString() ?: "—", "ACTIVE LOANS", FieldIcons.CheckCircleOutlined, FieldTheme.colors.statusSuccess),
            MetricData(relationshipData?.metrics?.par30_pct?.let { "$it%" } ?: "—", "PAR-30", FieldIcons.AlertOutlined, FieldTheme.colors.statusDanger)
        )
        UserRole.HEAD_CRM -> listOf(
            MetricData(relationshipData?.metrics?.crm_queue?.toString() ?: "—", "DOSSIERS AWAITING APPROVAL", FieldIcons.QueueOutlined, FieldTheme.colors.purple600),
            MetricData(relationshipData?.metrics?.disbursed_total?.toString() ?: "—", "ACTIVE LOANS", FieldIcons.CheckCircleOutlined, FieldTheme.colors.statusSuccess),
            MetricData(relationshipData?.metrics?.par30_pct?.let { "$it%" } ?: "—", "PAR-30", FieldIcons.AlertOutlined, FieldTheme.colors.statusDanger)
        )
        UserRole.EXECUTIVE -> emptyList()
        UserRole.CREDIT_ANALYST -> listOf(
            MetricData(relationshipData?.metrics?.reviews_due?.toString() ?: "—", "REVIEWS DUE", FieldIcons.QueueOutlined, FieldTheme.colors.purple600),
            MetricData(relationshipData?.metrics?.ocr_exceptions?.toString() ?: "—", "OCR EXCEPTIONS", FieldIcons.AlertOutlined, FieldTheme.colors.statusWarning),
            MetricData(relationshipData?.metrics?.reviewed_today?.toString() ?: "—", "REVIEWED TODAY", FieldIcons.CheckCircleOutlined, FieldTheme.colors.statusSuccess),
            MetricData(relationshipData?.metrics?.returned_this_week?.toString() ?: "—", "RETURNED THIS WEEK", FieldIcons.AlertOutlined, FieldTheme.colors.statusDanger)
        )
        UserRole.ED -> listOf(
            MetricData(relationshipData?.metrics?.ed_queue?.toString() ?: "—", "AWAITING ED APPROVAL", FieldIcons.QueueOutlined, FieldTheme.colors.purple600),
            MetricData(relationshipData?.metrics?.par30_pct?.let { "$it%" } ?: "—", "PAR-30", FieldIcons.AlertOutlined, FieldTheme.colors.statusDanger)
        )
        UserRole.MD -> listOf(
            MetricData(relationshipData?.metrics?.md_queue?.toString() ?: "—", "AWAITING MD APPROVAL", FieldIcons.QueueOutlined, FieldTheme.colors.purple600),
            MetricData(relationshipData?.metrics?.par30_pct?.let { "$it%" } ?: "—", "PAR-30", FieldIcons.AlertOutlined, FieldTheme.colors.statusDanger)
        )
        UserRole.SYSTEM_ADMIN -> listOf(
            MetricData(relationshipData?.metrics?.active_users?.toString() ?: "—", "ACTIVE USERS", FieldIcons.GroupOutlined, FieldTheme.colors.purple600),
            MetricData(relationshipData?.metrics?.system_events?.toString() ?: "—", "SYSTEM EVENTS", FieldIcons.DocumentOutlined, FieldTheme.colors.statusSuccess),
            MetricData(relationshipData?.metrics?.failed_jobs?.toString() ?: "0", "FAILED JOBS", FieldIcons.AlertOutlined, FieldTheme.colors.statusDanger),
            MetricData(relationshipData?.metrics?.config_alerts?.toString() ?: "0", "CONFIG ALERTS", FieldIcons.AlertOutlined, FieldTheme.colors.statusWarning)
        )
        UserRole.LEGAL -> listOf(
            MetricData(relationshipData?.metrics?.legal_queue?.toString() ?: "—", "LEGAL REVIEW QUEUE", FieldIcons.QueueOutlined, FieldTheme.colors.purple600)
        )
    }

    // Role-based status sets — values must match backend _stage_status() output (title-case)
    val relevantStatuses: Set<String> = remember(resolvedRole) {
        when (resolvedRole) {
            UserRole.LOAN_OFFICER, UserRole.ACCOUNT_OFFICER -> emptySet()
            UserRole.BRANCH_MANAGER -> setOf("branch_manager_review", "returned")
            UserRole.BRANCH_SUPERVISOR -> setOf("branch_supervisor_review", "returned")
            UserRole.CRM -> setOf("crm_review", "disbursement_ready")
            UserRole.HEAD_CRM -> setOf("head_crm_review")
            UserRole.EXECUTIVE -> setOf("executive_approval")
            UserRole.CREDIT_ANALYST -> setOf("credit_analyst_review")
            UserRole.ED -> setOf("ed_approval")
            UserRole.MD -> setOf("md_approval")
            UserRole.AUDITOR, UserRole.SYSTEM_ADMIN -> emptySet()
            UserRole.LEGAL -> setOf("branch_manager_review", "credit_analyst_review", "crm_review")
        }
    }

    // Build queue from real borrowers + applications (merged view), filtered by role
    val rawQueueItems = remember(borrowers, applications, resolvedRole, relationshipData) {
        if (resolvedRole == UserRole.ACCOUNT_OFFICER || resolvedRole == UserRole.LOAN_OFFICER) {
            relationshipData?.tasks.orEmpty().map { task ->
                QueueItem(
                    name = task.applicant_name,
                    appId = task.loan_id,
                    refNo = task.ref_no,
                    detail = task.task_description,
                    status = when (task.task_type) {
                        "returned" -> StatusChipVariant.Returned
                        "ocr_review" -> StatusChipVariant.LowConfidence
                        else -> StatusChipVariant.NeedsReview
                    }
                )
            }
        } else if (resolvedRole == UserRole.EXECUTIVE) {
            emptyList()
        } else if (resolvedRole == UserRole.SYSTEM_ADMIN) {
            emptyList()
        } else if (resolvedRole == UserRole.LEGAL) {
            relationshipData?.legal_queue.orEmpty().map { item ->
                QueueItem(item.applicant_name, appId = item.id, refNo = item.ref_no, detail = "${item.days_waiting}d waiting", status = StatusChipVariant.NeedsReview)
            }
        } else if (resolvedRole == UserRole.MD) {
            relationshipData?.md_queue.orEmpty().map { item ->
                QueueItem(item.applicant_name, appId = item.id, refNo = item.ref_no, detail = "${item.days_waiting}d waiting", status = StatusChipVariant.NeedsReview)
            }
        } else if (resolvedRole == UserRole.ED) {
            relationshipData?.ed_queue.orEmpty().map { item ->
                QueueItem(item.applicant_name, appId = item.id, refNo = item.ref_no, detail = "${item.days_waiting}d waiting", status = StatusChipVariant.NeedsReview)
            }
        } else if (resolvedRole == UserRole.CRM || resolvedRole == UserRole.HEAD_CRM) {
            relationshipData?.crm_queue.orEmpty().map { item ->
                QueueItem(
                    name = item.applicant_name,
                    appId = item.id,
                    refNo = item.ref_no,
                    detail = "${item.days_waiting}d waiting",
                    status = StatusChipVariant.NeedsReview,
                    officerName = item.officer_name
                )
            }
        } else if (resolvedRole == UserRole.CREDIT_ANALYST) {
            relationshipData?.reviews.orEmpty().map { item ->
                QueueItem(
                    name = item.applicant_name,
                    appId = item.id,
                    refNo = item.ref_no,
                    detail = "${item.loan_type.replaceFirstChar { it.uppercase() }} · ${item.exception_count} OCR issues",
                    status = StatusChipVariant.NeedsReview
                )
            }
        } else if (resolvedRole == UserRole.BRANCH_MANAGER || resolvedRole == UserRole.BRANCH_SUPERVISOR) {
            relationshipData?.queue.orEmpty().map { item ->
                QueueItem(
                    name = item.applicant_name,
                    appId = item.id,
                    refNo = item.ref_no,
                    detail = "${item.days_waiting}d waiting",
                    status = StatusChipVariant.NeedsReview,
                    officerName = item.officer_name
                )
            }
        } else borrowers.mapNotNull { borrower ->
            val app = applications
                .filter { it.id == borrower.id || it.phone == borrower.phone || it.bvn == borrower.bvn || it.applicant_name == borrower.name }
                .maxByOrNull { it.stageIndex }

            val canSeeBorrowersWithoutTasks = resolvedRole in setOf(
                UserRole.LOAN_OFFICER,
                UserRole.ACCOUNT_OFFICER,
                UserRole.AUDITOR,
                UserRole.SYSTEM_ADMIN
            )
            val isVisible = if (relevantStatuses.isEmpty()) {
                app != null || canSeeBorrowersWithoutTasks
            } else {
                app?.stage in relevantStatuses
            }

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
                    app.stage in setOf("ocr_review", "crm_review") -> StatusChipVariant.LowConfidence
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
            "ED_QUEUE" -> onNavigateToEdQueue()
            "MD_QUEUE" -> onNavigateToMdQueue()
            "LEGAL_WORKSPACE" -> onNavigateToLegalWorkspace()
            "MCC_WORKSPACE" -> onNavigateToMccWorkspace()
            "INTEREST_PRESETS" -> onNavigateToInterestPresets()
            "BRANCHES" -> onNavigateToBranches()
        }
    }

    val onQueueItemClick = { appId: String ->
        if (appId.isNotEmpty()) onNavigateToApplication(appId)
        else if (resolvedRole == UserRole.ACCOUNT_OFFICER || resolvedRole == UserRole.LOAN_OFFICER) {
            onNavigateToCreateApplication()
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
            val sideRailItems = if (resolvedRole == UserRole.SYSTEM_ADMIN) {
                listOf(
                    NavigationItem("Dashboard", FieldIcons.HomeOutlined, FieldIcons.HomeFilled),
                    NavigationItem("Users", FieldIcons.GroupOutlined, FieldIcons.GroupOutlined),
                    NavigationItem("System Activity", FieldIcons.DocumentOutlined, FieldIcons.DocumentOutlined)
                )
            } else {
                listOf(
                    NavigationItem("Home", FieldIcons.HomeOutlined, FieldIcons.HomeFilled),
                    NavigationItem(resolvedRole.queueLabel(), FieldIcons.QueueOutlined, FieldIcons.QueueFilled),
                    NavigationItem("Settings", FieldIcons.SettingsOutlined, FieldIcons.SettingsFilled)
                )
            }

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
                    when {
                        resolvedRole == UserRole.SYSTEM_ADMIN && selectedTab == 1 -> UsersScreen(
                            onBackClick = { navigateToTab(0) }
                        )
                        resolvedRole == UserRole.SYSTEM_ADMIN && selectedTab == 2 -> SystemActivityScreen(
                            onBackClick = { navigateToTab(0) }
                        )
                        selectedTab == 0 -> TabletDashboardHome(
                            userName = userName,
                            role = resolvedRole,
                            metrics = metrics,
                            queueItems = filteredQueueItems,
                            isLoading = isLoading,
                            onQuickActionClick = onQuickActionClick,
                            onQueueItemClick = onQueueItemClick,
                            onNavigateToCreateApplication = onNavigateToCreateApplication
                        )
                        selectedTab == 1 -> QueueTab(
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            queueItems = filteredQueueItems,
                            onItemClick = { appId -> onQueueItemClick(appId) }
                        )
                        else -> SettingsScreen(
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
            val bottomBarItems = if (resolvedRole == UserRole.SYSTEM_ADMIN) {
                listOf(
                    NavigationItem("Dashboard", FieldIcons.HomeOutlined, FieldIcons.HomeFilled),
                    NavigationItem("Users", FieldIcons.GroupOutlined, FieldIcons.GroupOutlined),
                    NavigationItem("Activity", FieldIcons.DocumentOutlined, FieldIcons.DocumentOutlined)
                )
            } else {
                listOf(
                    NavigationItem("Home", FieldIcons.HomeOutlined, FieldIcons.HomeFilled),
                    NavigationItem(resolvedRole.queueLabel(), FieldIcons.QueueOutlined, FieldIcons.QueueFilled),
                    NavigationItem("Settings", FieldIcons.SettingsOutlined, FieldIcons.SettingsFilled)
                )
            }

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
                    when {
                        resolvedRole == UserRole.SYSTEM_ADMIN && selectedTab == 1 -> UsersScreen(
                            onBackClick = { navigateToTab(0) }
                        )
                        resolvedRole == UserRole.SYSTEM_ADMIN && selectedTab == 2 -> SystemActivityScreen(
                            onBackClick = { navigateToTab(0) }
                        )
                        selectedTab == 0 -> PhoneDashboardHome(
                            userName = userName,
                            role = resolvedRole,
                            metrics = metrics,
                            queueItems = filteredQueueItems,
                            isLoading = isLoading,
                            onQuickActionClick = onQuickActionClick,
                            onQueueItemClick = onQueueItemClick,
                            onNavigateToCreateApplication = onNavigateToCreateApplication
                        )
                        selectedTab == 1 -> QueueTab(
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            queueItems = filteredQueueItems,
                            onItemClick = { appId -> onQueueItemClick(appId) }
                        )
                        else -> SettingsScreen(
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
    val status: StatusChipVariant,
    val officerName: String? = null
)

private fun UserRole.queueLabel(): String =
    WorkspaceRegistry.forRole(this).queues.firstOrNull()?.title ?: "Workspace"

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
                        UserRole.LOAN_OFFICER, UserRole.ACCOUNT_OFFICER -> {
                            item { ShuttleChip("New Loan", FieldIcons.AddOutlined) { onQuickActionClick("NEW_APP") } }
                            item { ShuttleChip("My Queue", FieldIcons.QueueOutlined) { onQuickActionClick("MY_QUEUE") } }
                            item { ShuttleChip("Visits Due", FieldIcons.MapOutlined) { onQuickActionClick("VISITS_DUE") } }
                        }
                        UserRole.BRANCH_MANAGER -> {
                            item { ShuttleChip("Awaiting Me", FieldIcons.CheckCircleOutlined) { onQuickActionClick("AWAITING_CONCURRENCE") } }
                            item { ShuttleChip("Visit Signoffs", FieldIcons.PenOutlined) { onQuickActionClick("PENDING_SIGNOFFS") } }
                            item { ShuttleChip("Pipeline", FieldIcons.QueueOutlined) { onQuickActionClick("PIPELINE") } }
                            item { ShuttleChip("Current Loans", FieldIcons.GroupOutlined) { onQuickActionClick("REG_BORROWER") } }
                        }
                        UserRole.BRANCH_SUPERVISOR -> {
                            item { ShuttleChip("Review Queue", FieldIcons.ShieldOutlined) { onQuickActionClick("CREDIT_REVIEW_QUEUE") } }
                            item { ShuttleChip("Borrowers", FieldIcons.GroupOutlined) { onQuickActionClick("REG_BORROWER") } }
                        }
                        UserRole.AUDITOR -> {
                            item { ShuttleChip("Compliance Flags", FieldIcons.AlertOutlined) { onQuickActionClick("COMPLIANCE_FLAGS") } }
                            item { ShuttleChip("Audit Trail", FieldIcons.QueueOutlined) { onQuickActionClick("AUDIT_TRAIL") } }
                            item { ShuttleChip("Current Loans", FieldIcons.GroupOutlined) { onQuickActionClick("REG_BORROWER") } }
                        }
                        UserRole.CRM -> {
                            item { ShuttleChip("Dossier Review Queue", FieldIcons.CheckCircleOutlined) { onQuickActionClick("CRM_QUEUE") } }
                            item { ShuttleChip("Current Loans", FieldIcons.GroupOutlined) { onQuickActionClick("REG_BORROWER") } }
                            item { ShuttleChip("Portfolio at Risk", FieldIcons.PaymentsOutlined) { onQuickActionClick("PAR_DASHBOARD") } }
                        }
                        UserRole.EXECUTIVE -> {
                            item { ShuttleChip("PAR Dashboard", FieldIcons.PaymentsOutlined) { onQuickActionClick("PAR_DASHBOARD") } }
                            item { ShuttleChip("Pipeline", FieldIcons.QueueOutlined) { onQuickActionClick("PIPELINE") } }
                        }
                        UserRole.HEAD_CRM -> {
                            item { ShuttleChip("Head CRM Queue", FieldIcons.CheckCircleOutlined) { onQuickActionClick("CRM_QUEUE") } }
                            item { ShuttleChip("Current Loans", FieldIcons.GroupOutlined) { onQuickActionClick("REG_BORROWER") } }
                            item { ShuttleChip("Portfolio at Risk", FieldIcons.PaymentsOutlined) { onQuickActionClick("PAR_DASHBOARD") } }
                        }
                        UserRole.CREDIT_ANALYST -> {
                            item { ShuttleChip("Underwriting Queue", FieldIcons.QueueOutlined) { onQuickActionClick("CREDIT_REVIEW_QUEUE") } }
                            item { ShuttleChip("OCR Exceptions", FieldIcons.AlertOutlined) { onQuickActionClick("OCR_EXCEPTIONS") } }
                            item { ShuttleChip("Current Loans", FieldIcons.GroupOutlined) { onQuickActionClick("REG_BORROWER") } }
                        }
                        UserRole.ED -> {
                            item { ShuttleChip("ED Queue", FieldIcons.ShieldOutlined) { onQuickActionClick("ED_QUEUE") } }
                            item { ShuttleChip("MCC", FieldIcons.GroupOutlined) { onQuickActionClick("MCC_WORKSPACE") } }
                            item { ShuttleChip("PAR Report", FieldIcons.PaymentsOutlined) { onQuickActionClick("PAR_DASHBOARD") } }
                            item { ShuttleChip("Borrowers", FieldIcons.GroupOutlined) { onQuickActionClick("REG_BORROWER") } }
                        }
                        UserRole.MD -> {
                            item { ShuttleChip("MD Queue", FieldIcons.ShieldOutlined) { onQuickActionClick("MD_QUEUE") } }
                            item { ShuttleChip("MCC", FieldIcons.GroupOutlined) { onQuickActionClick("MCC_WORKSPACE") } }
                            item { ShuttleChip("PAR Report", FieldIcons.PaymentsOutlined) { onQuickActionClick("PAR_DASHBOARD") } }
                            item { ShuttleChip("Borrowers", FieldIcons.GroupOutlined) { onQuickActionClick("REG_BORROWER") } }
                        }
                        UserRole.SYSTEM_ADMIN -> {
                            item { ShuttleChip("Users", FieldIcons.GroupOutlined) { onQuickActionClick("USERS") } }
                            item { ShuttleChip("System Activity", FieldIcons.DocumentOutlined) { onQuickActionClick("SYSTEM_ACTIVITY") } }
                        }
                        UserRole.LEGAL -> {
                            item { ShuttleChip("Legal Queue", FieldIcons.QueueOutlined) { onQuickActionClick("LEGAL_WORKSPACE") } }
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
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
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
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (role) {
                        UserRole.LOAN_OFFICER, UserRole.ACCOUNT_OFFICER -> {
                            ShuttleChip("New Loan", FieldIcons.AddOutlined) { onQuickActionClick("NEW_APP") }
                            ShuttleChip("My Queue", FieldIcons.QueueOutlined) { onQuickActionClick("MY_QUEUE") }
                            ShuttleChip("Visits Due", FieldIcons.MapOutlined) { onQuickActionClick("VISITS_DUE") }
                        }
                        UserRole.BRANCH_MANAGER -> {
                            ShuttleChip("Awaiting Me", FieldIcons.CheckCircleOutlined) { onQuickActionClick("AWAITING_CONCURRENCE") }
                            ShuttleChip("Visit Signoffs", FieldIcons.PenOutlined) { onQuickActionClick("PENDING_SIGNOFFS") }
                            ShuttleChip("Pipeline", FieldIcons.QueueOutlined) { onQuickActionClick("PIPELINE") }
                            ShuttleChip("Current Loans", FieldIcons.GroupOutlined) { onQuickActionClick("REG_BORROWER") }
                        }
                        UserRole.BRANCH_SUPERVISOR -> {
                            ShuttleChip("Review Queue", FieldIcons.ShieldOutlined) { onQuickActionClick("CREDIT_REVIEW_QUEUE") }
                            ShuttleChip("Borrowers", FieldIcons.GroupOutlined) { onQuickActionClick("REG_BORROWER") }
                        }
                        UserRole.AUDITOR -> {
                            ShuttleChip("Compliance Flags", FieldIcons.AlertOutlined) { onQuickActionClick("COMPLIANCE_FLAGS") }
                            ShuttleChip("Audit Trail", FieldIcons.QueueOutlined) { onQuickActionClick("AUDIT_TRAIL") }
                            ShuttleChip("Current Loans", FieldIcons.GroupOutlined) { onQuickActionClick("REG_BORROWER") }
                        }
                        UserRole.CRM -> {
                            ShuttleChip("Dossier Review Queue", FieldIcons.CheckCircleOutlined) { onQuickActionClick("CRM_QUEUE") }
                            ShuttleChip("Current Loans", FieldIcons.GroupOutlined) { onQuickActionClick("REG_BORROWER") }
                            ShuttleChip("Portfolio at Risk", FieldIcons.PaymentsOutlined) { onQuickActionClick("PAR_DASHBOARD") }
                        }
                        UserRole.EXECUTIVE -> {
                        }
                        UserRole.HEAD_CRM -> {
                            ShuttleChip("Head CRM Queue", FieldIcons.CheckCircleOutlined) { onQuickActionClick("CRM_QUEUE") }
                            ShuttleChip("Current Loans", FieldIcons.GroupOutlined) { onQuickActionClick("REG_BORROWER") }
                            ShuttleChip("Portfolio at Risk", FieldIcons.PaymentsOutlined) { onQuickActionClick("PAR_DASHBOARD") }
                        }
                        UserRole.CREDIT_ANALYST -> {
                            ShuttleChip("Underwriting Queue", FieldIcons.QueueOutlined) { onQuickActionClick("CREDIT_REVIEW_QUEUE") }
                            ShuttleChip("OCR Exceptions", FieldIcons.AlertOutlined) { onQuickActionClick("OCR_EXCEPTIONS") }
                            ShuttleChip("Current Loans", FieldIcons.GroupOutlined) { onQuickActionClick("REG_BORROWER") }
                        }
                        UserRole.ED -> {
                            ShuttleChip("ED Queue", FieldIcons.ShieldOutlined) { onQuickActionClick("ED_QUEUE") }
                            ShuttleChip("MCC", FieldIcons.GroupOutlined) { onQuickActionClick("MCC_WORKSPACE") }
                            ShuttleChip("PAR Report", FieldIcons.PaymentsOutlined) { onQuickActionClick("PAR_DASHBOARD") }
                            ShuttleChip("Borrowers", FieldIcons.GroupOutlined) { onQuickActionClick("REG_BORROWER") }
                        }
                        UserRole.MD -> {
                            ShuttleChip("MD Queue", FieldIcons.ShieldOutlined) { onQuickActionClick("MD_QUEUE") }
                            ShuttleChip("MCC", FieldIcons.GroupOutlined) { onQuickActionClick("MCC_WORKSPACE") }
                            ShuttleChip("PAR Report", FieldIcons.PaymentsOutlined) { onQuickActionClick("PAR_DASHBOARD") }
                            ShuttleChip("Borrowers", FieldIcons.GroupOutlined) { onQuickActionClick("REG_BORROWER") }
                        }
                        UserRole.SYSTEM_ADMIN -> {
                            ShuttleChip("Users", FieldIcons.GroupOutlined) { onQuickActionClick("USERS") }
                            ShuttleChip("System Activity", FieldIcons.DocumentOutlined) { onQuickActionClick("SYSTEM_ACTIVITY") }
                        }
                        UserRole.LEGAL -> {
                            ShuttleChip("Legal Queue", FieldIcons.QueueOutlined) { onQuickActionClick("LEGAL_WORKSPACE") }
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
                Text(
                    text = item.name,
                    style = FieldTheme.typography.bodyStrong.copy(fontSize = 15.sp),
                    color = FieldTheme.colors.gray100
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.refNo,
                        style = FieldTheme.typography.mono.copy(fontSize = 10.sp),
                        color = FieldTheme.colors.purple400
                    )
                    val roName = item.officerName?.takeIf { it.isNotBlank() } ?: ""
                    if (roName.isNotEmpty()) {
                        Text(
                            text = "·",
                            style = FieldTheme.typography.body.copy(fontSize = 13.sp),
                            color = FieldTheme.colors.gray500
                        )
                        Text(
                            text = roName,
                            style = FieldTheme.typography.bodyStrong.copy(fontSize = 13.sp),
                            color = FieldTheme.colors.gray300
                        )
                    }
                    Text(
                        text = "·",
                        style = FieldTheme.typography.body.copy(fontSize = 13.sp),
                        color = FieldTheme.colors.gray500
                    )
                    Text(
                        text = item.detail,
                        style = FieldTheme.typography.body.copy(fontSize = 13.sp),
                        color = FieldTheme.colors.gray400
                    )
                }
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
