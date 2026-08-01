package com.fieldcrm.android.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.rememberNavBackStack
import com.fieldcrm.android.core.notification.NotificationSyncWorker
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.core.session.RoleAccessPolicy
import com.fieldcrm.android.ui.screens.admin.*
import com.fieldcrm.android.ui.screens.application.*
import com.fieldcrm.android.ui.screens.audit.*
import com.fieldcrm.android.ui.screens.auth.*
import com.fieldcrm.android.ui.screens.borrower.*
import com.fieldcrm.android.ui.screens.common.*
import com.fieldcrm.android.ui.screens.dashboard.*
import com.fieldcrm.android.ui.screens.document.*
import com.fieldcrm.android.ui.screens.onboarding.*
import com.fieldcrm.android.ui.screens.queue.*
import com.fieldcrm.android.ui.screens.review.*
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.ui.viewmodel.*
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
@Composable
fun FieldCRMApp(
    appViewModel: AppViewModel = koinViewModel(),
    deepLinkApplicationId: String? = null
) {
    val loginViewModel: LoginViewModel = koinViewModel()
    val borrowerViewModel: BorrowerViewModel = koinViewModel()
    val applicationViewModel: ApplicationViewModel = koinViewModel()
    val servicingViewModel: ServicingViewModel = koinViewModel()
    val crmReviewViewModel: CrmReviewViewModel = koinViewModel()
    val syncViewModel: com.fieldcrm.android.ui.viewmodel.SyncViewModel = koinViewModel()
    val dashboardViewModel: DashboardViewModel = koinViewModel()
    val notificationsViewModel: NotificationsViewModel = koinViewModel()

    val appUiState by appViewModel.uiState.collectAsState()
    val borrowerUiState by borrowerViewModel.uiState.collectAsState()
    val applicationUiState by applicationViewModel.uiState.collectAsState()
    val loginUiState by loginViewModel.uiState.collectAsState()
    val restoredSession by loginViewModel.restoredSession.collectAsState()
    val sessionInvalidated by loginViewModel.sessionInvalidated.collectAsState()
    val servicingUiState by servicingViewModel.uiState.collectAsState()
    val crmReviewUiState by crmReviewViewModel.uiState.collectAsState()
    val syncUiState by syncViewModel.uiState.collectAsState()

    val backStack = rememberNavBackStack(Screen.Login)

    LaunchedEffect(deepLinkApplicationId, appUiState.session) {
        val id = deepLinkApplicationId
        if (id != null && appUiState.session != null) {
            applicationViewModel.resolveAuthorizedApplication(id) { application ->
                if (application != null) {
                    appViewModel.setSelectedApplication(application)
                    backStack.clear()
                    backStack.add(Screen.Dashboard)
                    backStack.add(Screen.ApplicationDetail)
                }
            }
        }
    }

    var selectedDocUrl by remember { mutableStateOf("") }
    var selectedDocName by remember { mutableStateOf("") }

    val activity = LocalContext.current as? android.app.Activity
    var backPressedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) {
            delay(2000)
            backPressedOnce = false
        }
    }

    // Handle back press globally
    BackHandler(enabled = true) {
        val onRoot = backStack.isEmpty() ||
            backStack.last() == Screen.Dashboard ||
            backStack.last() == Screen.Login ||
            backStack.last() == Screen.PermissionsPrimer ||
            backStack.last() == Screen.Onboarding
        when {
            !onRoot -> { backStack.removeLastOrNull(); backPressedOnce = false }
            backPressedOnce -> activity?.finish()
            else -> {
                backPressedOnce = true
                android.widget.Toast.makeText(
                    activity, "Press back again to exit", android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val context = LocalContext.current

    // Auto-restore persisted session
    LaunchedEffect(restoredSession) {
        val session = restoredSession ?: return@LaunchedEffect
        if (appUiState.session == null) {
            appViewModel.setSession(session)
            NotificationSyncWorker.schedule(context)
            val next: Screen = when {
                !appUiState.hasSeenPermissions -> Screen.PermissionsPrimer
                !appUiState.hasSeenOnboarding -> Screen.Onboarding
                else -> Screen.Dashboard
            }
            backStack.clear()
            backStack.add(next)
        }
    }

    // Blank surface while checking stored session
    if (loginUiState.isRestoringSession) {
        Surface(modifier = Modifier.fillMaxSize(), color = FieldTheme.colors.gray950) {}
        return
    }

    // Observe Lifecycle events to refresh stale data when returning to foreground
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (appUiState.session != null) {
                    applicationViewModel.refreshIfStale()
                    borrowerViewModel.refreshIfStale()
                    dashboardViewModel.refreshIfStale()
                    notificationsViewModel.refreshIfStale()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Auto-sync when session becomes active
    LaunchedEffect(appUiState.session) {
        val session = appUiState.session
        if (session != null) {
            syncViewModel.syncNow { success ->
                dashboardViewModel.loadMetrics()
                val role = session.role
                if (role == UserRole.LOAN_OFFICER) {
                    borrowerViewModel.refreshBorrowers()
                }
                applicationViewModel.refreshApplications()
                notificationsViewModel.load()
            }
        }
    }

    // Background token validation found session definitively rejected — log out cleanly.
    LaunchedEffect(sessionInvalidated) {
        if (sessionInvalidated) {
            appViewModel.logout()
            backStack.clear()
            backStack.add(Screen.Login)
        }
    }

    // Session expiry overlay
    if (appUiState.isSessionExpired) {
        SessionExpiredScreen(
            userEmail = appUiState.session?.userEmail ?: "",
            onReauthSuccess = { appViewModel.setSessionExpired(false) }
        )
        return
    }

    // Render the top of the back stack with Navigation 3 UI.
    FieldCRMNavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() }
    ) { screen ->
        val activeRole = appUiState.session?.role
        if (activeRole != null && !RoleAccessPolicy.canAccess(activeRole, screen)) {
            LaunchedEffect(screen) {
                android.widget.Toast.makeText(
                    context,
                    "This workspace is not available to ${activeRole.displayName}.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                backStack.removeLastOrNull()
                if (backStack.isEmpty()) backStack.add(Screen.Dashboard)
            }
            Surface(modifier = Modifier.fillMaxSize(), color = FieldTheme.colors.gray950) {}
        } else {
        when (screen) {
        Screen.Login -> {
            LoginScreenView(
                viewModel = loginViewModel,
                hasPasscode = appUiState.hasPasscode,
                onLoginSuccess = { session ->
                    appViewModel.setSession(session)
                    NotificationSyncWorker.schedule(context)
                    com.fieldcrm.android.sync.AndroidSyncWorker.schedulePeriodic(context)
                    val next: Screen = when {
                        !appUiState.hasSeenPermissions -> Screen.PermissionsPrimer
                        !appUiState.hasSeenOnboarding -> Screen.Onboarding
                        else -> Screen.Dashboard
                    }
                    backStack.clear()
                    backStack.add(next)
                },
                onForgotPasswordClick = { backStack.add(Screen.ForgotPassword) },
                onPasscodeClick = { backStack.add(Screen.PasscodeLogin) }
            )
        }

        Screen.ForgotPassword -> ForgotPasswordScreen(
            onBackClick = { backStack.removeLastOrNull() },
            onNavigateToLogin = { backStack.clear(); backStack.add(Screen.Login) }
        )

        Screen.ResetPassword -> ResetPasswordScreen(
            onNavigateToLogin = { _, _ -> backStack.clear(); backStack.add(Screen.Login) }
        )

        Screen.PermissionsPrimer -> {
            val next: Screen = if (!appUiState.hasSeenOnboarding) Screen.Onboarding else Screen.Dashboard
            PermissionsPrimerScreen(
                role = appUiState.session?.role,
                onContinueClick = { appViewModel.setPermissionsSeen(true); backStack.add(next) }
            )
        }

        Screen.Onboarding -> OnboardingScreen(
            role = appUiState.session?.role,
            onDismiss = { appViewModel.setOnboardingSeen(true); backStack.add(Screen.Dashboard) }
        )

        Screen.Confirmation -> ConfirmationScreen(
            title = appUiState.successTitle,
            subtitle = appUiState.successSubtitle,
            onPrimaryClick = { backStack.add(appUiState.successDestination) },
            onSecondaryClick = { backStack.add(Screen.Dashboard) }
        )

        Screen.Notifications -> NotificationsScreen(
            onBackClick = { backStack.removeLastOrNull() },
            onNavigateTo = { screen, appId ->
                if (screen == Screen.ApplicationDetail && appId != null) {
                    applicationViewModel.resolveAuthorizedApplication(appId) { app ->
                        if (app != null) {
                            appViewModel.setSelectedApplication(app)
                            backStack.add(Screen.ApplicationDetail)
                        }
                    }
                } else {
                    backStack.add(screen)
                }
            }
        )

        Screen.SearchResults -> SearchResultsScreen(
            onBackClick = { backStack.removeLastOrNull() },
            onNavigateToApplication = { appId ->
                val app = applicationUiState.applications.find { it.id == appId }
                if (app != null) appViewModel.setSelectedApplication(app)
                backStack.add(Screen.ApplicationDetail)
            }
        )

        Screen.Dashboard -> DashboardScreenView(
            role = appUiState.session?.role,
            borrowers = borrowerUiState.borrowers,
            applications = applicationUiState.applications,
            isLoading = applicationUiState.isLoading,
            sessionEmail = appUiState.session?.userEmail,
            sessionName = appUiState.session?.userName,
            onNavigateToBorrowers = { backStack.add(Screen.BorrowerList) },
            onNavigateToCreateApplication = { backStack.add(Screen.CreateApplication) },
            onNavigateToApplication = { appId ->
                val app = applicationUiState.applications.find { it.id == appId }
                if (app != null) {
                    appViewModel.setSelectedApplication(app)
                    backStack.add(Screen.ApplicationDetail)
                } else {
                    backStack.add(Screen.CreateApplication)
                }
            },
            onLogout = { appViewModel.logout(); NotificationSyncWorker.cancel(context); backStack.clear(); backStack.add(Screen.Login) },
            onNavigateToNotifications = { backStack.add(Screen.Notifications) },
            onNavigateToSearchResults = { backStack.add(Screen.SearchResults) },
            onNavigateToMyQueue = { backStack.add(Screen.MyQueue) },
            onNavigateToVisitsDue = { backStack.add(Screen.VisitsDue) },
            onNavigateToAwaitingConcurrence = { backStack.add(Screen.AwaitingConcurrence) },
            onNavigateToPendingSignoffs = { backStack.add(Screen.PendingSignoffs) },
            onNavigateToCreditReviewQueue = { backStack.add(Screen.CreditReviewQueue) },
            onNavigateToOcrExceptions = { backStack.add(Screen.OcrExceptions) },
            onNavigateToPipeline = { backStack.add(Screen.Pipeline) },
            onNavigateToUsers = { backStack.add(Screen.Users) },
            onNavigateToSystemActivity = { backStack.add(Screen.SystemActivity) },
            onNavigateToAuditTrail = { backStack.add(Screen.AuditTrail) },
            onNavigateToComplianceFlags = { backStack.add(Screen.ComplianceFlags) },
            onNavigateToOfflineQueue = { backStack.add(Screen.OfflineQueue) },
            onNavigateToCrmQueue = { backStack.add(Screen.CrmQueue) },
            onNavigateToExecutiveQueue = { backStack.add(Screen.ExecutiveQueue) },
            onNavigateToParDashboard = { servicingViewModel.loadParDashboard(); backStack.add(Screen.ParDashboard) },
            onNavigateToEdQueue = { backStack.add(Screen.EdQueue) },
            onNavigateToMdQueue = { backStack.add(Screen.MdQueue) },
            onNavigateToLegalWorkspace = { backStack.add(Screen.LegalWorkspace) },
            onNavigateToMccWorkspace = { backStack.add(Screen.MccWorkspace) },
            onNavigateToInterestPresets = { backStack.add(Screen.InterestPresets) },
            onNavigateToBranches = { backStack.add(Screen.BranchManagement) },
            syncState = syncUiState,
            onSyncNow = { syncViewModel.syncNow() }
        )

        Screen.Settings -> {
            val sessionEmail = appUiState.session?.userEmail ?: ""
            val settingsName = appUiState.session?.userName?.takeIf { it.isNotBlank() }
                ?: appUiState.session?.role?.displayName
                ?: "User"
            SettingsScreen(
                userName = settingsName,
                userEmail = sessionEmail,
                role = appUiState.session?.role,
                onBackClick = { backStack.removeLastOrNull() },
                onSignOutClick = { appViewModel.logout(); NotificationSyncWorker.cancel(context); backStack.clear(); backStack.add(Screen.Login) }
            )
        }

        Screen.BorrowerList -> BorrowerListScreenView(
            viewModel = borrowerViewModel,
            onBorrowerSelected = { borrower ->
                appViewModel.setSelectedBorrower(borrower)
                backStack.add(Screen.BorrowerDetail)
            },
            onAddBorrower = { backStack.add(Screen.CreateBorrower) },
            onBackClick = { backStack.removeLastOrNull() }
        )

        Screen.BorrowerDetail -> appUiState.selectedBorrower?.let { borrower ->
            BorrowerDetailScreenView(
                borrower = borrower,
                onBackClick = { backStack.removeLastOrNull() },
                onCreateApplication = {
                    applicationViewModel.setSelectedBorrowerForApp(borrower)
                    backStack.add(Screen.CreateApplication)
                }
            )
        }

        Screen.CreateBorrower -> CreateBorrowerScreenView(
            viewModel = borrowerViewModel,
            onBorrowerCreated = { newBorrower ->
                appViewModel.setSelectedBorrower(newBorrower)
                backStack.removeLastOrNull()
                backStack.add(Screen.BorrowerDetail)
            },
            onBackClick = { backStack.removeLastOrNull() }
        )

        Screen.ApplicationDetail -> appUiState.selectedApplication?.let { app ->
            LaunchedEffect(app.id) { applicationViewModel.loadApplicationDetail(app.id) }
            val isRelationshipOfficer = appUiState.session?.role in setOf(
                UserRole.ACCOUNT_OFFICER, UserRole.LOAN_OFFICER
            )
            val canEditOfficerWork = !isRelationshipOfficer || app.stage in setOf(
                "intake", "returned", "ocr_review"
            )
            ApplicationDetailScreenView(
                application = app,
                borrower = borrowerUiState.borrowers.find { it.id == app.id || it.phone == app.phone || it.bvn == app.bvn || it.name == app.applicant_name },
                role = appUiState.session?.role,
                appDetail = applicationUiState.selectedAppDetail,
                isLoadingDetail = applicationUiState.isLoadingDetail,
                onBackClick = { backStack.removeLastOrNull() },
                onNavigateToDocumentUpload = { if (canEditOfficerWork) backStack.add(Screen.DocumentUpload) },
                onNavigateToPledgeTrust = { if (canEditOfficerWork) backStack.add(Screen.PledgeTrust) },
                onNavigateToVisitationReport = { if (canEditOfficerWork) backStack.add(Screen.VisitationReport) },
                onNavigateToGuarantorsForm = { if (canEditOfficerWork) backStack.add(Screen.GuarantorsForm) },
                onNavigateToReview = {
                    val reviewScreen: Screen? = when (appUiState.session?.role) {
                        UserRole.BRANCH_MANAGER, UserRole.BRANCH_SUPERVISOR -> Screen.BranchManagerReview
                        UserRole.CREDIT_ANALYST -> Screen.CreditOfficerReview
                        UserRole.AUDITOR -> null
                        UserRole.CRM, UserRole.HEAD_CRM -> Screen.CrmReview
                        UserRole.EXECUTIVE -> Screen.ExecutiveApproval
                        UserRole.ED -> Screen.EdApproval
                        UserRole.MD -> Screen.MdApproval
                        UserRole.SYSTEM_ADMIN -> null
                        else -> null
                    }
                    if (reviewScreen != null) backStack.add(reviewScreen)
                },
                onNavigateToAuditTrail = { backStack.add(Screen.WorkflowEventAudit) },
                onNavigateToFormWizard = { if (canEditOfficerWork) backStack.add(Screen.LoanApplicationForm) },
                onNavigateToDocumentViewer = { url, name ->
                    selectedDocUrl = url
                    selectedDocName = name
                    backStack.add(Screen.DocumentViewer)
                },
                onNavigateToOcrReview = {
                    if (isRelationshipOfficer && app.stage == "ocr_review") {
                        backStack.add(Screen.OcrReview)
                    }
                },
                onOpenClientSigning = {
                    applicationViewModel.generateApplicationSigningLink(app.id) { url ->
                        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
                    }
                },
                onShareClientSigning = {
                    applicationViewModel.generateApplicationSigningLink(app.id) { url ->
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, url)
                        }
                        context.startActivity(Intent.createChooser(share, "Share signing link"))
                    }
                },
                onOpenGuarantorSigning = { slot ->
                    applicationViewModel.generateApplicationSigningLink(app.id, slot) { url ->
                        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
                    }
                },
                onGenerateOffer = {
                    applicationViewModel.generateOffer(app.id) {
                        applicationViewModel.loadApplicationDetail(app.id)
                    }
                }
            )
        }

        Screen.CreateApplication -> CreateApplicationScreenView(
            viewModel = applicationViewModel,
            borrowers = borrowerUiState.borrowers,
            onApplicationCreated = { newApp, borrower ->
                appViewModel.setSelectedApplication(newApp)
                appViewModel.setSelectedBorrower(borrower)
                borrowerViewModel.refreshBorrowers()
                backStack.removeLastOrNull()
                backStack.add(Screen.LoanApplicationForm)
            },
            onBackClick = { backStack.removeLastOrNull() }
        )

        Screen.LoanApplicationForm -> {
            val app = appUiState.selectedApplication
            val borrower = borrowerUiState.borrowers.find { it.id == app?.id || it.phone == app?.phone || it.bvn == app?.bvn || it.name == app?.applicant_name } ?: appUiState.selectedBorrower
            if (app != null) {
                LoanApplicationFormScreen(
                    application = app,
                    borrower = borrower,
                    appDetail = applicationUiState.selectedAppDetail,
                    applicationViewModel = applicationViewModel,
                    borrowerViewModel = borrowerViewModel,
                    appViewModel = appViewModel,
                    onBackClick = { backStack.removeLastOrNull() },
                    onNavigateToGuarantorsForm = { backStack.add(Screen.GuarantorsForm) }
                )
            } else {
                backStack.removeLastOrNull()
            }
        }

        Screen.DocumentUpload -> {
            val app = appUiState.selectedApplication
            val borrower = borrowerUiState.borrowers.find { it.id == app?.id || it.phone == app?.phone || it.bvn == app?.bvn || it.name == app?.applicant_name }
            DocumentUploadScreen(
                applicationId = app?.id ?: "",
                borrower = borrower,
                role = appUiState.session?.role,
                onBackClick = { backStack.removeLastOrNull() },
                onComplete = { updatedBorrower ->
                    borrowerViewModel.updateBorrowerLocal(updatedBorrower) {
                        backStack.removeLastOrNull()
                    }
                }
            )
        }

        Screen.GuarantorsForm -> {
            val app = appUiState.selectedApplication
            val borrower = borrowerUiState.borrowers.find { it.id == app?.id || it.phone == app?.phone || it.bvn == app?.bvn || it.name == app?.applicant_name }
            if (borrower != null) {
                GuarantorsFormScreen(
                    borrower = borrower,
                    borrowerViewModel = borrowerViewModel,
                    onBackClick = { backStack.removeLastOrNull() },
                    onSave = { backStack.removeLastOrNull() }
                )
            } else {
                backStack.removeLastOrNull()
            }
        }

        Screen.PledgeTrust -> {
            val app = appUiState.selectedApplication
            val borrower = borrowerUiState.borrowers.find { it.id == app?.id || it.phone == app?.phone || it.bvn == app?.bvn || it.name == app?.applicant_name }
            if (app != null) {
                PledgeTrustScreen(
                    application = app,
                    borrower = borrower,
                    applicationViewModel = applicationViewModel,
                    onBackClick = { backStack.removeLastOrNull() },
                    onSignComplete = { backStack.removeLastOrNull() }
                )
            } else {
                backStack.removeLastOrNull()
            }
        }

        Screen.VisitationReport -> {
            val app = appUiState.selectedApplication
            val borrower = borrowerUiState.borrowers.find { it.id == app?.id || it.phone == app?.phone || it.bvn == app?.bvn || it.name == app?.applicant_name }
            if (app != null) {
                VisitationReportScreen(
                    application = app,
                    borrower = borrower,
                    applicationViewModel = applicationViewModel,
                    borrowerViewModel = borrowerViewModel,
                    onBackClick = { backStack.removeLastOrNull() },
                    onSubmit = { backStack.removeLastOrNull() }
                )
            } else {
                backStack.removeLastOrNull()
            }
        }

        Screen.BranchManagerReview -> {
            val app = appUiState.selectedApplication
            val borrower = borrowerUiState.borrowers.find { it.id == app?.id || it.phone == app?.phone || it.bvn == app?.bvn || it.name == app?.applicant_name }
            if (app != null) {
                LaunchedEffect(app.id, appUiState.session?.role) {
                    applicationViewModel.loadApplicationDetail(app.id)
                    val context = if (appUiState.session?.role == UserRole.BRANCH_SUPERVISOR) "supervisor_review" else "team_lead_review"
                    applicationViewModel.loadReviewChecklist(app.id, context)
                }
                BranchManagerReviewScreen(
                    application = app,
                    borrower = borrower,
                    role = appUiState.session?.role ?: UserRole.BRANCH_MANAGER,
                    applicationViewModel = applicationViewModel,
                    onBackClick = { backStack.removeLastOrNull() },
                    onDecisionSubmitted = { backStack.removeLastOrNull() }
                )
            } else {
                backStack.removeLastOrNull()
            }
        }

        Screen.DocumentViewer -> DocumentViewerScreen(
            applicationId = appUiState.selectedApplication?.id ?: "",
            docType = selectedDocName,
            initialDocUrl = selectedDocUrl,
            onBackClick = { backStack.removeLastOrNull() }
        )

        Screen.WorkflowEventAudit -> WorkflowEventAuditScreen(
            applicationId = appUiState.selectedApplication?.id ?: "",
            onBackClick = { backStack.removeLastOrNull() }
        )

        Screen.OfflineQueue -> OfflineQueueScreen(
            onBackClick = { backStack.removeLastOrNull() }
        )

        Screen.MyQueue -> MyQueueScreen(
            applications = applicationUiState.applications,
            borrowers = borrowerUiState.borrowers,
            onBackClick = { backStack.removeLastOrNull() },
            onViewApplication = { appId ->
                val app = applicationUiState.applications.find { it.id == appId }
                if (app != null) appViewModel.setSelectedApplication(app)
                backStack.add(Screen.ApplicationDetail)
            }
        )

        Screen.VisitsDue -> VisitsDueScreen(
            applications = applicationUiState.applications,
            borrowers = borrowerUiState.borrowers,
            onBackClick = { backStack.removeLastOrNull() },
            onStartVisit = { appId ->
                val app = applicationUiState.applications.find { it.id == appId }
                if (app != null) appViewModel.setSelectedApplication(app)
                backStack.add(Screen.VisitationReport)
            }
        )

        Screen.AwaitingConcurrence -> AwaitingConcurrenceScreen(
            applications = applicationUiState.applications,
            borrowers = borrowerUiState.borrowers,
            onBackClick = { backStack.removeLastOrNull() },
            onViewApplication = { appId ->
                val app = applicationUiState.applications.find { it.id == appId }
                if (app != null) appViewModel.setSelectedApplication(app)
                backStack.add(Screen.ApplicationDetail)
            }
        )

        Screen.PendingSignoffs -> PendingSignoffsScreen(
            applications = applicationUiState.applications,
            borrowers = borrowerUiState.borrowers,
            onBackClick = { backStack.removeLastOrNull() },
            onViewReport = { appId ->
                val app = applicationUiState.applications.find { it.id == appId }
                if (app != null) appViewModel.setSelectedApplication(app)
                backStack.add(Screen.VisitationReport)
            }
        )

        Screen.CreditReviewQueue -> CreditReviewQueueScreen(
            applications = applicationUiState.applications,
            borrowers = borrowerUiState.borrowers,
            role = appUiState.session?.role,
            onBackClick = { backStack.removeLastOrNull() },
            onReviewApplication = { appId ->
                val app = applicationUiState.applications.find { it.id == appId }
                if (app != null) appViewModel.setSelectedApplication(app)
                backStack.add(Screen.ApplicationDetail)
            }
        )

        Screen.OcrExceptions -> OcrExceptionsScreen(
            applications = applicationUiState.applications,
            borrowers = borrowerUiState.borrowers,
            onBackClick = { backStack.removeLastOrNull() },
            onResolveException = { appId ->
                val app = applicationUiState.applications.find { it.id == appId }
                if (app != null) appViewModel.setSelectedApplication(app)
                backStack.add(Screen.ApplicationDetail)
            }
        )

        Screen.Pipeline -> PipelineScreen(
            applications = applicationUiState.applications,
            borrowers = borrowerUiState.borrowers,
            onBackClick = { backStack.removeLastOrNull() },
            onViewApplication = { appId ->
                val app = applicationUiState.applications.find { it.id == appId }
                if (app != null) appViewModel.setSelectedApplication(app)
                backStack.add(Screen.ApplicationDetail)
            }
        )

        Screen.Users -> UsersScreen(
            onBackClick = { backStack.removeLastOrNull() }
        )

        Screen.SystemActivity -> SystemActivityScreen(
            applications = applicationUiState.applications,
            borrowers = borrowerUiState.borrowers,
            onBackClick = { backStack.removeLastOrNull() },
            onViewApplication = { appId ->
                val app = applicationUiState.applications.find { it.id == appId }
                if (app != null) appViewModel.setSelectedApplication(app)
                backStack.add(Screen.ApplicationDetail)
            }
        )

        Screen.LegalWorkspace -> LegalWorkspaceScreen(
            onBack = { backStack.removeLastOrNull() },
            onOpenApplication = { appId ->
                applicationViewModel.resolveAuthorizedApplication(appId) { app ->
                    if (app != null) {
                        appViewModel.setSelectedApplication(app)
                        backStack.add(Screen.ValuationEditor)
                    }
                }
            }
        )

        Screen.ValuationEditor -> ValuationEditorScreen(
            applicationId = appUiState.selectedApplication?.id.orEmpty(),
            onBack = { backStack.removeLastOrNull() }
        )

        Screen.MccWorkspace -> MccWorkspaceScreen(
            onBack = { backStack.removeLastOrNull() },
            canManage = appUiState.session?.role in setOf(UserRole.ED, UserRole.MD)
        )

        Screen.InterestPresets -> InterestPresetScreen(
            onBack = { backStack.removeLastOrNull() }
        )

        Screen.BranchManagement -> BranchManagementScreen(
            onBack = { backStack.removeLastOrNull() }
        )

        Screen.AuditTrail -> AuditTrailScreen(
            applicationId = appUiState.selectedApplication?.id.orEmpty(),
            onBackClick = { backStack.removeLastOrNull() }
        )

        Screen.ComplianceFlags -> ComplianceFlagsScreen(
            onBackClick = { backStack.removeLastOrNull() }
        )

        Screen.OcrReview -> {
            val app = appUiState.selectedApplication
            if (app != null) {
                com.fieldcrm.android.ui.screens.application.OcrReviewScreen(
                    application = app,
                    applicationViewModel = applicationViewModel,
                    onBackClick = { backStack.removeLastOrNull() },
                    onVerified = {
                        backStack.removeLastOrNull()
                    }
                )
            } else {
                backStack.removeLastOrNull()
            }
        }

        Screen.PasscodeSetup -> PasscodeScreen(
            mode = PasscodeMode.SETUP,
            onSetupComplete = { hash ->
                appViewModel.setPasscodeHash(hash)
                backStack.removeLastOrNull()
            },
            onBackClick = { backStack.removeLastOrNull() }
        )

        Screen.PasscodeLogin -> PasscodeScreen(
            mode = PasscodeMode.LOGIN,
            storedHash = appUiState.passcodeHash,
            onLoginSuccess = {
                loginViewModel.restoreStoredSession(
                    onSuccess = { session ->
                        appViewModel.setSession(session)
                        NotificationSyncWorker.schedule(context)
                        com.fieldcrm.android.sync.AndroidSyncWorker.schedulePeriodic(context)
                        backStack.clear()
                        backStack.add(Screen.Dashboard)
                    },
                    onError = { backStack.removeLastOrNull() }
                )
            },
            onBackClick = { backStack.removeLastOrNull() }
        )

        Screen.CrmReview -> {
            val app = appUiState.selectedApplication
            if (app != null) {
                LaunchedEffect(app.id) { crmReviewViewModel.loadChecklist(app.id) }
                CrmReviewScreen(
                    application = app,
                    role = appUiState.session?.role ?: UserRole.CRM,
                    isSubmitting = crmReviewUiState.isSubmitting,
                    savedChecklist = crmReviewUiState.checklist,
                    onAdvanceToExecutive = { notes, bureau1, bureau2, crms, ncr ->
                        crmReviewViewModel.submitCrmReview(
                            applicationId = app.id,
                            decision = "advance",
                            notes = notes,
                            bureau1 = bureau1,
                            bureau2 = bureau2,
                            crms = crms,
                            ncr = ncr,
                            onDone = { backStack.removeLastOrNull() }
                        )
                    },
                    onReturnToBranchManager = { notes ->
                        crmReviewViewModel.submitCrmReview(
                            applicationId = app.id,
                            decision = "return",
                            notes = notes,
                            onDone = { backStack.removeLastOrNull() }
                        )
                    },
                    onUploadDocument = { backStack.add(Screen.DocumentUpload) },
                    onBack = { backStack.removeLastOrNull() }
                )
            } else {
                LaunchedEffect(Unit) { backStack.removeLastOrNull() }
            }
        }

        Screen.ExecutiveApproval -> {
            val app = appUiState.selectedApplication
            if (app != null) {
                ExecutiveApprovalScreen(
                    application = app,
                    isSubmitting = crmReviewUiState.isSubmitting,
                    onIssueInstruction = {
                        crmReviewViewModel.submitExecutiveApprove(
                            applicationId = app.id,
                            onDone = { backStack.removeLastOrNull() }
                        )
                    },
                    onBack = { backStack.removeLastOrNull() }
                )
            } else {
                LaunchedEffect(Unit) { backStack.removeLastOrNull() }
            }
        }

        Screen.RepaymentSchedule -> {
            val app = appUiState.selectedApplication
            val role = appUiState.session?.role
            val canRecord = role == UserRole.CRM || role == UserRole.SYSTEM_ADMIN
            RepaymentScheduleScreen(
                applicantName = app?.applicant_name ?: "—",
                refNo = app?.id?.take(8) ?: "—",
                schedule = servicingUiState.schedule,
                payments = servicingUiState.payments,
                totalDue = servicingUiState.totalDue,
                totalPaid = servicingUiState.totalPaid,
                outstanding = servicingUiState.outstanding,
                canRecordPayment = canRecord,
                onRecordPayment = { amount, channel, reference ->
                    app?.let {
                        servicingViewModel.recordPayment(
                            applicationId = it.id,
                            amount = amount,
                            channel = channel,
                            bankRef = reference,
                            paymentDate = null,
                            onDone = {}
                        )
                    }
                },
                onBack = { backStack.removeLastOrNull() }
            )
        }

        Screen.ParDashboard -> {
            ParDashboardScreen(
                par = servicingUiState.par,
                isLoading = servicingUiState.isLoading,
                onBack = { backStack.removeLastOrNull() },
                onOpenSchedule = { loanId ->
                    val app = applicationUiState.applications.find { it.id == loanId }
                    if (app != null) {
                        appViewModel.setSelectedApplication(app)
                        servicingViewModel.loadRepaymentSchedule(loanId)
                        backStack.add(Screen.RepaymentSchedule)
                    }
                }
            )
        }

        Screen.CrmQueue -> CrmQueueScreen(
            applications = applicationUiState.applications,
            borrowers = borrowerUiState.borrowers,
            onBackClick = { backStack.removeLastOrNull() },
            onReviewApplication = { appId ->
                val app = applicationUiState.applications.find { it.id == appId }
                if (app != null) appViewModel.setSelectedApplication(app)
                backStack.add(Screen.CrmReview)
            }
        )

        Screen.ExecutiveQueue -> ExecutiveQueueScreen(
            applications = applicationUiState.applications,
            borrowers = borrowerUiState.borrowers,
            onBackClick = { backStack.removeLastOrNull() },
            onReviewApplication = { appId ->
                val app = applicationUiState.applications.find { it.id == appId }
                if (app != null) appViewModel.setSelectedApplication(app)
                backStack.add(Screen.ExecutiveApproval)
            }
        )

        Screen.EdQueue -> EdQueueScreen(
            applications = applicationUiState.applications,
            onBackClick = { backStack.removeLastOrNull() },
            onReviewApplication = { appId ->
                val app = applicationUiState.applications.find { it.id == appId }
                if (app != null) appViewModel.setSelectedApplication(app)
                backStack.add(Screen.EdApproval)
            }
        )

        Screen.EdApproval -> {
            val app = appUiState.selectedApplication
            if (app != null) {
                EdApprovalScreen(
                    application = app,
                    isSubmitting = crmReviewUiState.isSubmitting,
                    onApprove = {
                        crmReviewViewModel.submitEdApprove(
                            id = app.id,
                            action = "approve",
                            onDone = { backStack.removeLastOrNull() }
                        )
                    },
                    onForwardToMd = {
                        crmReviewViewModel.submitEdApprove(
                            id = app.id,
                            action = "escalate_md",
                            onDone = { backStack.removeLastOrNull() }
                        )
                    },
                    onBack = { backStack.removeLastOrNull() }
                )
            } else {
                LaunchedEffect(Unit) { backStack.removeLastOrNull() }
            }
        }

        Screen.MdQueue -> MdQueueScreen(
            applications = applicationUiState.applications,
            onBackClick = { backStack.removeLastOrNull() },
            onReviewApplication = { appId ->
                val app = applicationUiState.applications.find { it.id == appId }
                if (app != null) appViewModel.setSelectedApplication(app)
                backStack.add(Screen.MdApproval)
            }
        )

        Screen.MdApproval -> {
            val app = appUiState.selectedApplication
            if (app != null) {
                MdApprovalScreen(
                    application = app,
                    isSubmitting = crmReviewUiState.isSubmitting,
                    onApprove = {
                        crmReviewViewModel.submitMdApprove(
                            id = app.id,
                            action = "approve",
                            notes = "",
                            onDone = { backStack.removeLastOrNull() }
                        )
                    },
                    onReturnToEd = { notes ->
                        crmReviewViewModel.submitMdApprove(
                            id = app.id,
                            action = "comment",
                            notes = notes,
                            onDone = { backStack.removeLastOrNull() }
                        )
                    },
                    onAddBoardReferral = { email, name, notes ->
                        crmReviewViewModel.addBoardReferral(
                            id = app.id,
                            email = email,
                            name = name,
                            notes = notes,
                            onDone = {}
                        )
                    },
                    onBack = { backStack.removeLastOrNull() }
                )
            } else {
                LaunchedEffect(Unit) { backStack.removeLastOrNull() }
            }
        }

            else -> {}
        }
        }
    }
}
