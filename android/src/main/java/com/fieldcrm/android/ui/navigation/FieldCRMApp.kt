package com.fieldcrm.android.ui.navigation

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.navigation3.runtime.rememberNavBackStack
import com.fieldcrm.android.core.biometric.BiometricPromptManager
import com.fieldcrm.android.core.biometric.BiometricPromptManager.BiometricResult
import com.fieldcrm.android.core.notification.NotificationSyncWorker
import com.fieldcrm.android.core.session.UserRole
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
import com.fieldcrm.android.ui.viewmodel.BiometricAction
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
@Composable
fun FieldCRMApp(
    appViewModel: AppViewModel = koinViewModel(),
    promptManager: BiometricPromptManager? = null
) {
    val loginViewModel: LoginViewModel = koinViewModel()
    val borrowerViewModel: BorrowerViewModel = koinViewModel()
    val applicationViewModel: ApplicationViewModel = koinViewModel()
    val servicingViewModel: ServicingViewModel = koinViewModel()
    val crmReviewViewModel: CrmReviewViewModel = koinViewModel()

    val appUiState by appViewModel.uiState.collectAsState()
    val borrowerUiState by borrowerViewModel.uiState.collectAsState()
    val applicationUiState by applicationViewModel.uiState.collectAsState()
    val loginUiState by loginViewModel.uiState.collectAsState()
    val restoredSession by loginViewModel.restoredSession.collectAsState()
    val servicingUiState by servicingViewModel.uiState.collectAsState()
    val crmReviewUiState by crmReviewViewModel.uiState.collectAsState()

    val backStack = rememberNavBackStack(Screen.Login)

    val activity = LocalContext.current as? android.app.Activity
    var backPressedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) {
            delay(2000)
            backPressedOnce = false
        }
    }

    // Biometric state — action lives in ViewModel; result collected from manager
    val biometricResult by promptManager?.promptResults?.collectAsState(initial = null)
        ?: remember { mutableStateOf(null) }

    // Launcher to send user to system biometric enrollment when none are set
    val enrollLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {}
    )

    // Dispatch biometric results through ViewModel
    LaunchedEffect(biometricResult) {
        when (val result = biometricResult) {
            BiometricResult.AuthenticationSuccess -> {
                when (appUiState.pendingBiometricAction) {
                    BiometricAction.LOGIN -> loginViewModel.restoreStoredSession(
                        onSuccess = { session ->
                            appViewModel.setSession(session)
                            backStack.clear()
                            backStack.add(Screen.Dashboard)
                        },
                        onError = {
                            appViewModel.setBiometricsEnrolled(false)
                            activity?.runOnUiThread {
                                android.widget.Toast.makeText(activity, "Session invalid. Please log in with your password.", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                    BiometricAction.ENROLL -> {
                        appViewModel.setBiometricsEnrolled(true)
                        appViewModel.markBiometricEnrollmentShown()
                        val next: Screen = when {
                            !appUiState.hasSeenPermissions -> Screen.PermissionsPrimer
                            !appUiState.hasSeenOnboarding -> Screen.Onboarding
                            else -> Screen.Dashboard
                        }
                        backStack.clear()
                        backStack.add(next)
                    }
                    null -> {}
                }
                appViewModel.setBiometricAction(null)
            }
            BiometricResult.AuthenticationNotSet -> {
                if (Build.VERSION.SDK_INT >= 30) {
                    val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                        putExtra(
                            Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
                        )
                    }
                    enrollLauncher.launch(enrollIntent)
                }
                appViewModel.setBiometricAction(null)
            }
            else -> {}
        }
    }

    // Handle back press globally
    BackHandler(enabled = true) {
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

    val context = LocalContext.current

    // Auto-restore persisted session
    LaunchedEffect(restoredSession) {
        val session = restoredSession ?: return@LaunchedEffect
        if (appUiState.session == null) {
            appViewModel.setSession(session)
            NotificationSyncWorker.schedule(context)
            val next: Screen = when {
                !appUiState.hasSeenBiometricEnrollment -> Screen.BiometricEnrollment
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

    // Auto-sync when session becomes active
    LaunchedEffect(appUiState.session) {
        if (appUiState.session != null) {
            applicationViewModel.syncQueue {}
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
        when (screen) {
        Screen.Login -> {
            LoginScreenView(
                viewModel = loginViewModel,
                hasEnrolledBiometrics = appUiState.hasEnrolledBiometrics,
                hasPasscode = appUiState.hasPasscode,
                onLoginSuccess = { session ->
                    appViewModel.setSession(session)
                    NotificationSyncWorker.schedule(context)
                    val next: Screen = when {
                        !appUiState.hasSeenBiometricEnrollment -> Screen.BiometricEnrollment
                        !appUiState.hasSeenPermissions -> Screen.PermissionsPrimer
                        !appUiState.hasSeenOnboarding -> Screen.Onboarding
                        else -> Screen.Dashboard
                    }
                    backStack.clear()
                    backStack.add(next)
                },
                onForgotPasswordClick = { backStack.add(Screen.ForgotPassword) },
                onBiometricClick = {
                    appViewModel.setBiometricAction(BiometricAction.LOGIN)
                    promptManager?.showBiometricPrompt()
                },
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

        Screen.BiometricEnrollment -> {
            val next: Screen = when {
                !appUiState.hasSeenPermissions -> Screen.PermissionsPrimer
                !appUiState.hasSeenOnboarding -> Screen.Onboarding
                else -> Screen.Dashboard
            }
            BiometricEnrollmentScreen(
                onEnableClick = {
                    appViewModel.setBiometricAction(BiometricAction.ENROLL)
                    promptManager?.showBiometricPrompt(
                        title = "Enable Biometric Login",
                        description = "Verify your biometric to enable quick sign-in"
                    ) ?: run {
                        // No biometric hardware — skip enrollment
                        appViewModel.markBiometricEnrollmentShown()
                        backStack.clear()
                        backStack.add(next)
                    }
                },
                onNotNowClick = {
                    appViewModel.markBiometricEnrollmentShown()
                    backStack.clear()
                    backStack.add(next)
                }
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
            onNavigateToCrmQueue = { backStack.add(Screen.CrmReview) },
            onNavigateToExecutiveQueue = { backStack.add(Screen.ExecutiveApproval) },
            onNavigateToParDashboard = { servicingViewModel.loadParDashboard(); backStack.add(Screen.ParDashboard) }
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
                        UserRole.CRM -> Screen.CrmReview
                        UserRole.EXECUTIVE -> Screen.ExecutiveApproval
                        else -> Screen.CreditOfficerReview
                    }
                    backStack.add(reviewScreen)
                },
                onNavigateToAuditTrail = { backStack.add(Screen.WorkflowEventAudit) },
                onNavigateToFormWizard = { backStack.add(Screen.LoanApplicationForm) },
                onNavigateToDocumentViewer = { backStack.add(Screen.DocumentViewer) },
                onNavigateToOcrReview = { backStack.add(Screen.OcrReview) }
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
            val borrower = borrowerUiState.borrowers.find { it.id == app?.borrower_id } ?: appUiState.selectedBorrower
            if (app != null) {
                LoanApplicationFormScreen(
                    application = app,
                    borrower = borrower,
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
            onBackClick = { backStack.removeLastOrNull() },
            onReviewApplication = { appId ->
                val app = applicationUiState.applications.find { it.id == appId }
                if (app != null) appViewModel.setSelectedApplication(app)
                backStack.add(Screen.CreditOfficerReview)
            }
        )

        Screen.OcrExceptions -> OcrExceptionsScreen(
            applications = applicationUiState.applications,
            borrowers = borrowerUiState.borrowers,
            onBackClick = { backStack.removeLastOrNull() },
            onResolveException = { appId ->
                val app = applicationUiState.applications.find { it.id == appId }
                if (app != null) appViewModel.setSelectedApplication(app)
                backStack.add(Screen.OcrReview)
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

        Screen.AuditTrail -> AuditTrailScreen(
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
                        backStack.add(Screen.CreditOfficerReview)
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
                CrmReviewScreen(
                    application = app,
                    isSubmitting = crmReviewUiState.isSubmitting,
                    onAdvanceToExecutive = {
                        crmReviewViewModel.submitCrmReview(
                            applicationId = app.id,
                            decision = "advance",
                            notes = "",
                            onDone = { backStack.removeLastOrNull() }
                        )
                    },
                    onReturnToBranchManager = {
                        crmReviewViewModel.submitCrmReview(
                            applicationId = app.id,
                            decision = "return",
                            notes = "",
                            onDone = { backStack.removeLastOrNull() }
                        )
                    },
                    onBack = { backStack.removeLastOrNull() }
                )
            } else {
                backStack.removeLastOrNull()
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
                backStack.removeLastOrNull()
            }
        }

        Screen.RepaymentSchedule -> {
            val app = appUiState.selectedApplication
            val role = appUiState.session?.role
            val canRecord = role == UserRole.CRM || role == UserRole.SYSTEM_ADMIN
            RepaymentScheduleScreen(
                applicantName = app?.applicantName ?: "—",
                refNo = app?.refNo ?: "—",
                schedule = servicingUiState.schedule,
                payments = servicingUiState.payments,
                totalDue = servicingUiState.totalDue,
                totalPaid = servicingUiState.totalPaid,
                outstanding = servicingUiState.outstanding,
                canRecordPayment = canRecord,
                onRecordPayment = { /* TODO: wire record-payment bottom sheet */ },
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

            else -> {}
        }
    }
}
