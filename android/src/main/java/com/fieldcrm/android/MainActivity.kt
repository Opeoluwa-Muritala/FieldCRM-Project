package com.fieldcrm.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.koin.android.ext.android.inject
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.rememberNavBackStack
import com.fieldcrm.android.ui.screens.auth.*
import com.fieldcrm.android.ui.screens.onboarding.*
import com.fieldcrm.android.ui.screens.dashboard.*
import com.fieldcrm.android.ui.screens.borrower.*
import com.fieldcrm.android.ui.screens.application.*
import com.fieldcrm.android.ui.screens.document.*
import com.fieldcrm.android.ui.screens.review.*
import com.fieldcrm.android.ui.screens.common.*
import com.fieldcrm.android.ui.screens.queue.*
import com.fieldcrm.android.ui.screens.admin.*
import com.fieldcrm.android.ui.screens.audit.*
import com.fieldcrm.android.ui.viewmodel.*
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.theme.FieldTheme
import com.fieldcrm.android.core.notification.NotificationSyncWorker
import org.koin.androidx.compose.koinViewModel
import com.fieldcrm.android.ui.components.OfflineSyncDialog

class MainActivity : ComponentActivity() {

    private val loginViewModel: LoginViewModel by inject()
    private val appViewModel: AppViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appUiState by appViewModel.uiState.collectAsState()
            FieldCRMTheme(role = appUiState.session?.role) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    FieldCRMApp(appViewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loginViewModel.syncSession(onExpired = {
            appViewModel.setSessionExpired(true)
        })
    }
}

@Composable
fun FieldCRMApp(appViewModel: AppViewModel = koinViewModel()) {
    val loginViewModel: LoginViewModel = koinViewModel()
    val borrowerViewModel: BorrowerViewModel = koinViewModel()
    val applicationViewModel: ApplicationViewModel = koinViewModel()

    val appUiState by appViewModel.uiState.collectAsState()
    val borrowerUiState by borrowerViewModel.uiState.collectAsState()
    val applicationUiState by applicationViewModel.uiState.collectAsState()
    val loginUiState by loginViewModel.uiState.collectAsState()
    val restoredSession by loginViewModel.restoredSession.collectAsState()

    var showSyncModal by remember { mutableStateOf(false) }

    val backStack = rememberNavBackStack(Screen.Login)

    val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
    var backPressedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) {
            kotlinx.coroutines.delay(2000)
            backPressedOnce = false
        }
    }

    // Handle back press globally — NavDisplay is not used so we wire it manually
    androidx.activity.compose.BackHandler(enabled = true) {
        val onRoot = backStack.isEmpty() ||
            backStack.last() == Screen.Dashboard ||
            backStack.last() == Screen.Login
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

    val context = androidx.compose.ui.platform.LocalContext.current

    // Auto-restore persisted session
    LaunchedEffect(restoredSession) {
        val session = restoredSession ?: return@LaunchedEffect
        if (appUiState.session == null) {
            appViewModel.setSession(session)
            NotificationSyncWorker.schedule(context)
            val next: Screen = when {
                !appUiState.hasEnrolledBiometrics -> Screen.BiometricEnrollment
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

    if (showSyncModal) {
        OfflineSyncDialog(
            onDismiss = { showSyncModal = false },
            onTriggerSync = { onComplete ->
                applicationViewModel.syncQueue { success -> onComplete(success) }
            }
        )
    }

    // Session expiry overlay — shown above the screen stack
    if (appUiState.isSessionExpired) {
        SessionExpiredScreen(
            userEmail = appUiState.session?.userEmail ?: "",
            onReauthSuccess = { appViewModel.setSessionExpired(false) }
        )
        return
    }

    // Render the top of the back stack
    when (val screen = backStack.lastOrNull() ?: Screen.Login) {
        Screen.Login -> LoginScreenView(
            viewModel = loginViewModel,
            hasEnrolledBiometrics = appUiState.hasEnrolledBiometrics,
            onLoginSuccess = { session ->
                appViewModel.setSession(session)
                NotificationSyncWorker.schedule(context)
                val next: Screen = when {
                    !appUiState.hasEnrolledBiometrics -> Screen.BiometricEnrollment
                    !appUiState.hasSeenPermissions -> Screen.PermissionsPrimer
                    !appUiState.hasSeenOnboarding -> Screen.Onboarding
                    else -> Screen.Dashboard
                }
                backStack.clear()
                backStack.add(next)
            },
            onForgotPasswordClick = { backStack.add(Screen.ForgotPassword) }
        )

        Screen.ForgotPassword -> ForgotPasswordScreen(
            onBackClick = { backStack.removeLastOrNull() },
            onNavigateToLogin = { backStack.clear(); backStack.add(Screen.Login) }
        )

        Screen.ResetPassword -> ResetPasswordScreen(
            onNavigateToLogin = { _, _ -> backStack.clear(); backStack.add(Screen.Login) }
        )

        Screen.BiometricEnrollment -> {
            val next: Screen = when {
                !appUiState.hasSeenPermissions -> Screen.PermissionsPrimer
                !appUiState.hasSeenOnboarding -> Screen.Onboarding
                else -> Screen.Dashboard
            }
            BiometricEnrollmentScreen(
                onEnableClick = { appViewModel.setBiometricsEnrolled(true); backStack.add(next) },
                onNotNowClick = { backStack.add(next) }
            )
        }

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
                    val app = applicationUiState.applications.find { it.id == appId }
                    if (app != null) appViewModel.setSelectedApplication(app)
                }
                backStack.add(screen)
            }
        )

        Screen.SearchResults -> SearchResultsScreen(
            onBackClick = { backStack.removeLastOrNull() },
            onNavigateToApplication = { refNo ->
                val app = applicationUiState.applications.find { it.id == refNo }
                if (app != null) appViewModel.setSelectedApplication(app)
                backStack.add(Screen.ApplicationDetail)
            }
        )

        Screen.Dashboard -> DashboardScreenView(
            role = appUiState.session?.role,
            borrowers = borrowerUiState.borrowers,
            applications = applicationUiState.applications,
            sessionEmail = appUiState.session?.userEmail,
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
            onNavigateToOfflineQueue = { showSyncModal = true },
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
            onNavigateToComplianceFlags = { backStack.add(Screen.ComplianceFlags) }
        )

        Screen.Settings -> {
            val sessionEmail = appUiState.session?.userEmail ?: ""
            val settingsName = if (sessionEmail.isNotBlank()) {
                sessionEmail.substringBefore("@")
                    .split(".", "_", "-")
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
            } else {
                appUiState.session?.role?.displayName ?: "User"
            }
            SettingsScreen(
                userName = settingsName,
                userEmail = sessionEmail,
                role = appUiState.session?.role,
                onBackClick = { backStack.removeLastOrNull() },
                onNavigateToOfflineQueue = { showSyncModal = true },
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
            onBorrowerCreated = { _ -> backStack.removeLastOrNull() },
            onBackClick = { backStack.removeLastOrNull() }
        )

        Screen.ApplicationDetail -> appUiState.selectedApplication?.let { app ->
            ApplicationDetailScreenView(
                application = app,
                borrower = borrowerUiState.borrowers.find { it.id == app.borrower_id },
                role = appUiState.session?.role,
                onBackClick = { backStack.removeLastOrNull() },
                onNavigateToDocumentUpload = { backStack.add(Screen.DocumentUpload) },
                onNavigateToPledgeTrust = { backStack.add(Screen.PledgeTrust) },
                onNavigateToVisitationReport = { backStack.add(Screen.VisitationReport) },
                onNavigateToGuarantorsForm = { backStack.add(Screen.GuarantorsForm) },
                onNavigateToReview = {
                    val reviewScreen = when (appUiState.session?.role) {
                        UserRole.BRANCH_MANAGER -> Screen.BranchManagerReview
                        UserRole.CREDIT_OFFICER -> Screen.CreditOfficerReview
                        UserRole.AUDITOR -> Screen.AuditorCompliance
                        UserRole.ADMIN_MCR -> Screen.AdminMcrApproval
                        else -> Screen.CreditOfficerReview
                    }
                    backStack.add(reviewScreen)
                },
                onNavigateToAuditTrail = { backStack.add(Screen.WorkflowEventAudit) },
                onNavigateToFormWizard = { backStack.add(Screen.LoanApplicationForm) },
                onNavigateToDocumentViewer = { backStack.add(Screen.DocumentViewer) }
            )
        }

        Screen.CreateApplication -> CreateApplicationScreenView(
            viewModel = applicationViewModel,
            borrowers = borrowerUiState.borrowers,
            onApplicationCreated = { newApp ->
                appViewModel.setSelectedApplication(newApp)
                backStack.add(Screen.ApplicationDetail)
            },
            onBackClick = { backStack.removeLastOrNull() }
        )

        Screen.LoanApplicationForm -> {
            val app = appUiState.selectedApplication
            val borrower = borrowerUiState.borrowers.find { it.id == app?.borrower_id }
            if (app != null) {
                LoanApplicationFormScreen(
                    application = app,
                    borrower = borrower,
                    applicationViewModel = applicationViewModel,
                    borrowerViewModel = borrowerViewModel,
                    appViewModel = appViewModel,
                    onBackClick = { backStack.removeLastOrNull() }
                )
            } else {
                backStack.removeLastOrNull()
            }
        }

        Screen.DocumentUpload -> {
            val app = appUiState.selectedApplication
            val borrower = borrowerUiState.borrowers.find { it.id == app?.borrower_id }
            DocumentUploadScreen(
                applicationId = app?.id ?: "",
                borrower = borrower,
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
            val borrower = borrowerUiState.borrowers.find { it.id == app?.borrower_id }
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
            val borrower = borrowerUiState.borrowers.find { it.id == app?.borrower_id }
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
            val borrower = borrowerUiState.borrowers.find { it.id == app?.borrower_id }
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
            val borrower = borrowerUiState.borrowers.find { it.id == app?.borrower_id }
            if (app != null) {
                BranchManagerReviewScreen(
                    application = app,
                    borrower = borrower,
                    applicationViewModel = applicationViewModel,
                    onBackClick = { backStack.removeLastOrNull() },
                    onDecisionSubmitted = { backStack.removeLastOrNull() }
                )
            } else {
                backStack.removeLastOrNull()
            }
        }

        Screen.CreditOfficerReview -> {
            val app = appUiState.selectedApplication
            val borrower = borrowerUiState.borrowers.find { it.id == app?.borrower_id }
            if (app != null) {
                CreditOfficerReviewScreen(
                    application = app,
                    borrower = borrower,
                    applicationViewModel = applicationViewModel,
                    onBackClick = { backStack.removeLastOrNull() },
                    onCompleteReview = { backStack.removeLastOrNull() }
                )
            } else {
                backStack.removeLastOrNull()
            }
        }

        Screen.AuditorCompliance -> AuditorComplianceScreen(
            applicationId = appUiState.selectedApplication?.id ?: "",
            onBackClick = { backStack.removeLastOrNull() },
            onAuditComplete = { backStack.removeLastOrNull() }
        )

        Screen.AdminMcrApproval -> {
            val app = appUiState.selectedApplication
            val borrower = borrowerUiState.borrowers.find { it.id == app?.borrower_id }
            if (app != null) {
                AdminMcrApprovalScreen(
                    application = app,
                    borrower = borrower,
                    applicationViewModel = applicationViewModel,
                    onBackClick = { backStack.removeLastOrNull() },
                    onDisburseTriggered = { backStack.removeLastOrNull() }
                )
            } else {
                backStack.removeLastOrNull()
            }
        }

        Screen.DocumentViewer -> DocumentViewerScreen(
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
            onBackClick = { backStack.removeLastOrNull() },
            onViewApplication = { appId ->
                val app = applicationUiState.applications.find { it.id == appId }
                if (app != null) appViewModel.setSelectedApplication(app)
                backStack.add(Screen.ApplicationDetail)
            }
        )

        Screen.VisitsDue -> VisitsDueScreen(
            onBackClick = { backStack.removeLastOrNull() },
            onStartVisit = { appId ->
                val app = applicationUiState.applications.find { it.id == appId }
                if (app != null) appViewModel.setSelectedApplication(app)
                backStack.add(Screen.VisitationReport)
            }
        )

        Screen.AwaitingConcurrence -> AwaitingConcurrenceScreen(
            onBackClick = { backStack.removeLastOrNull() },
            onViewApplication = { appId ->
                val app = applicationUiState.applications.find { it.id == appId }
                if (app != null) appViewModel.setSelectedApplication(app)
                backStack.add(Screen.ApplicationDetail)
            }
        )

        Screen.PendingSignoffs -> PendingSignoffsScreen(
            onBackClick = { backStack.removeLastOrNull() },
            onViewReport = { appId ->
                val app = applicationUiState.applications.find { it.id == appId }
                if (app != null) appViewModel.setSelectedApplication(app)
                backStack.add(Screen.VisitationReport)
            }
        )

        Screen.CreditReviewQueue -> CreditReviewQueueScreen(
            onBackClick = { backStack.removeLastOrNull() },
            onReviewApplication = { appId ->
                val app = applicationUiState.applications.find { it.id == appId }
                if (app != null) appViewModel.setSelectedApplication(app)
                backStack.add(Screen.CreditOfficerReview)
            }
        )

        Screen.OcrExceptions -> OcrExceptionsScreen(
            onBackClick = { backStack.removeLastOrNull() },
            onResolveException = { appId ->
                val app = applicationUiState.applications.find { it.id == appId }
                if (app != null) appViewModel.setSelectedApplication(app)
                backStack.add(Screen.OcrReview)
            }
        )

        Screen.Pipeline -> PipelineScreen(
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
            onBackClick = { backStack.removeLastOrNull() },
            onViewApplication = { appId ->
                val app = applicationUiState.applications.find { it.id == appId }
                if (app != null) appViewModel.setSelectedApplication(app)
                backStack.add(Screen.ApplicationDetail)
            }
        )

        Screen.AuditTrail -> AuditTrailScreen(
            onBackClick = { backStack.removeLastOrNull() }
        )

        Screen.ComplianceFlags -> ComplianceFlagsScreen(
            onBackClick = { backStack.removeLastOrNull() }
        )

        Screen.OcrReview -> {
            val app = appUiState.selectedApplication
            if (app != null) {
                OcrReviewScreen(
                    application = app,
                    onBackClick = { backStack.removeLastOrNull() },
                    onVerified = { backStack.removeLastOrNull() },
                    onReturnForReupload = { backStack.removeLastOrNull() }
                )
            } else {
                backStack.removeLastOrNull()
            }
        }

        // Exhaustive — compiler will warn if a new Screen subtype is added without a case
        else -> {}
    }
}
