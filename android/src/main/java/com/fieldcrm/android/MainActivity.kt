package com.fieldcrm.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fieldcrm.android.ui.screens.auth.*
import com.fieldcrm.android.ui.screens.onboarding.*
import com.fieldcrm.android.ui.screens.dashboard.*
import com.fieldcrm.android.ui.screens.borrower.*
import com.fieldcrm.android.ui.screens.application.*
import com.fieldcrm.android.ui.screens.document.*
import com.fieldcrm.android.ui.screens.review.*
import com.fieldcrm.android.ui.screens.common.*
import com.fieldcrm.android.ui.viewmodel.*
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.core.session.UserSession

import com.fieldcrm.android.ui.theme.FieldCRMTheme
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import org.koin.androidx.compose.koinViewModel
import com.fieldcrm.android.ui.components.OfflineSyncDialog

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appViewModel: AppViewModel = koinViewModel()
            val appUiState by appViewModel.uiState.collectAsState()
            
            FieldCRMTheme(
                role = appUiState.session?.role
            ) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    FieldCRMApp(appViewModel)
                }
            }
        }
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

    var showSyncModal by remember { mutableStateOf(false) }

    if (showSyncModal) {
        OfflineSyncDialog(
            onDismiss = { showSyncModal = false },
            onTriggerSync = { onComplete ->
                applicationViewModel.syncQueue { success ->
                    onComplete(success)
                }
            }
        )
    }

    if (appUiState.isSessionExpired) {
        SessionExpiredScreen(
            userEmail = appUiState.session?.userEmail ?: "chidi@mmfb.com",
            onReauthSuccess = {
                appViewModel.setSessionExpired(false)
            }
        )
    } else {
        when (appUiState.currentScreen) {
            Screen.Login -> {
                LoginScreenView(
                    viewModel = loginViewModel,
                    hasEnrolledBiometrics = appUiState.hasEnrolledBiometrics,
                    onLoginSuccess = { session ->
                        appViewModel.setSession(session)
                        val next = when {
                            !appUiState.hasEnrolledBiometrics -> Screen.BiometricEnrollment
                            !appUiState.hasSeenPermissions -> Screen.PermissionsPrimer
                            !appUiState.hasSeenOnboarding -> Screen.Onboarding
                            else -> Screen.Dashboard
                        }
                        appViewModel.navigateTo(next)
                    },
                    onForgotPasswordClick = {
                        appViewModel.navigateTo(Screen.ForgotPassword)
                    }
                )
            }
            Screen.ForgotPassword -> {
                ForgotPasswordScreen(
                    onBackClick = { appViewModel.navigateTo(Screen.Login) },
                    onNavigateToLogin = { appViewModel.navigateTo(Screen.Login) }
                )
            }
            Screen.ResetPassword -> {
                ResetPasswordScreen(
                    onNavigateToLogin = { _, _ -> appViewModel.navigateTo(Screen.Login) }
                )
            }
            Screen.BiometricEnrollment -> {
                BiometricEnrollmentScreen(
                    onEnableClick = {
                        appViewModel.setBiometricsEnrolled(true)
                        val next = when {
                            !appUiState.hasSeenPermissions -> Screen.PermissionsPrimer
                            !appUiState.hasSeenOnboarding -> Screen.Onboarding
                            else -> Screen.Dashboard
                        }
                        appViewModel.navigateTo(next)
                    },
                    onNotNowClick = {
                        val next = when {
                            !appUiState.hasSeenPermissions -> Screen.PermissionsPrimer
                            !appUiState.hasSeenOnboarding -> Screen.Onboarding
                            else -> Screen.Dashboard
                        }
                        appViewModel.navigateTo(next)
                    }
                )
            }
            Screen.PermissionsPrimer -> {
                PermissionsPrimerScreen(
                    role = appUiState.session?.role,
                    onContinueClick = {
                        appViewModel.setPermissionsSeen(true)
                        val next = when {
                            !appUiState.hasSeenOnboarding -> Screen.Onboarding
                            else -> Screen.Dashboard
                        }
                        appViewModel.navigateTo(next)
                    }
                )
            }
            Screen.Notifications -> {
                NotificationsScreen(
                    onBackClick = { appViewModel.navigateTo(Screen.Dashboard) },
                    onNavigateTo = { screen -> appViewModel.navigateTo(screen) }
                )
            }
            Screen.SearchResults -> {
                SearchResultsScreen(
                    onBackClick = { appViewModel.navigateTo(Screen.Dashboard) },
                    onNavigateToApplication = { refNo ->
                        // Simulate selecting application detail from search results
                        val app = applicationUiState.applications.find { it.id == refNo }
                        if (app != null) {
                            appViewModel.setSelectedApplication(app)
                        }
                        appViewModel.navigateTo(Screen.ApplicationDetail)
                    }
                )
            }
            Screen.Onboarding -> {
                OnboardingScreen(
                    role = appUiState.session?.role,
                    onDismiss = {
                        appViewModel.setOnboardingSeen(true)
                        appViewModel.navigateTo(Screen.Dashboard)
                    }
                )
            }
            Screen.Confirmation -> {
                ConfirmationScreen(
                    title = appUiState.successTitle,
                    subtitle = appUiState.successSubtitle,
                    onPrimaryClick = {
                        appViewModel.navigateTo(Screen.ApplicationDetail)
                    },
                    onSecondaryClick = {
                        appViewModel.navigateTo(appUiState.successDestination)
                    }
                )
            }
            Screen.Dashboard -> {
                DashboardScreenView(
                    role = appUiState.session?.role,
                    onNavigateToBorrowers = {
                        appViewModel.navigateTo(Screen.BorrowerList)
                    },
                    onNavigateToApplications = {
                        appViewModel.navigateTo(Screen.ApplicationList)
                    },
                    onNavigateToOfflineQueue = {
                        showSyncModal = true
                    },
                    onLogout = {
                        appViewModel.logout()
                    },
                    onNavigateToNotifications = {
                        appViewModel.navigateTo(Screen.Notifications)
                    },
                    onNavigateToSearchResults = {
                        appViewModel.navigateTo(Screen.SearchResults)
                    }
                )
            }
            Screen.BorrowerList -> {
                BorrowerListScreenView(
                    viewModel = borrowerViewModel,
                    onBorrowerSelected = { borrower ->
                        appViewModel.setSelectedBorrower(borrower)
                        appViewModel.navigateTo(Screen.BorrowerDetail)
                    },
                    onAddBorrower = {
                        appViewModel.navigateTo(Screen.CreateBorrower)
                    },
                    onBackClick = {
                        appViewModel.navigateTo(Screen.Dashboard)
                    }
                )
            }
            Screen.BorrowerDetail -> {
                appUiState.selectedBorrower?.let { borrower ->
                    BorrowerDetailScreenView(
                        borrower = borrower,
                        onBackClick = {
                            appViewModel.setSelectedBorrower(null)
                            appViewModel.navigateTo(Screen.BorrowerList)
                        },
                        onCreateApplication = {
                            applicationViewModel.setSelectedBorrowerForApp(borrower)
                            appViewModel.navigateTo(Screen.CreateApplication)
                        }
                    )
                }
            }
            Screen.CreateBorrower -> {
                CreateBorrowerScreenView(
                    viewModel = borrowerViewModel,
                    onBorrowerCreated = { newBorrower ->
                        appViewModel.navigateTo(Screen.BorrowerList)
                    },
                    onBackClick = {
                        appViewModel.navigateTo(Screen.BorrowerList)
                    }
                )
            }
            Screen.ApplicationList -> {
                ApplicationListScreenView(
                    viewModel = applicationViewModel,
                    borrowers = borrowerUiState.borrowers,
                    onApplicationSelected = { app ->
                        appViewModel.setSelectedApplication(app)
                        appViewModel.navigateTo(Screen.ApplicationDetail)
                    },
                    onAddApplication = {
                        appViewModel.navigateTo(Screen.CreateApplication)
                    },
                    onBackClick = {
                        appViewModel.navigateTo(Screen.Dashboard)
                    }
                )
            }
            Screen.ApplicationDetail -> {
                appUiState.selectedApplication?.let { app ->
                    ApplicationDetailScreenView(
                        application = app,
                        borrower = borrowerUiState.borrowers.find { it.id == app.borrower_id },
                        onBackClick = {
                            appViewModel.setSelectedApplication(null)
                            appViewModel.navigateTo(Screen.ApplicationList)
                        },
                        onNavigateToDocumentUpload = {
                            appViewModel.navigateTo(Screen.DocumentUpload)
                        },
                        onNavigateToPledgeTrust = {
                            appViewModel.navigateTo(Screen.PledgeTrust)
                        },
                        onNavigateToVisitationReport = {
                            appViewModel.navigateTo(Screen.VisitationReport)
                        },
                        onNavigateToGuarantorsForm = {
                            appViewModel.navigateTo(Screen.GuarantorsForm)
                        },
                        onNavigateToReview = {
                            val reviewScreen = when (appUiState.session?.role) {
                                UserRole.BRANCH_MANAGER -> Screen.BranchManagerReview
                                UserRole.CREDIT_OFFICER -> Screen.CreditOfficerReview
                                UserRole.AUDITOR -> Screen.AuditorCompliance
                                UserRole.ADMIN_MCR -> Screen.AdminMcrApproval
                                else -> Screen.CreditOfficerReview
                            }
                            appViewModel.navigateTo(reviewScreen)
                        },
                        onNavigateToAuditTrail = {
                            appViewModel.navigateTo(Screen.WorkflowEventAudit)
                        },
                        onNavigateToFormWizard = {
                            appViewModel.navigateTo(Screen.LoanApplicationForm)
                        },
                        onNavigateToDocumentViewer = {
                            appViewModel.navigateTo(Screen.DocumentViewer)
                        }
                    )
                }   
            }
            Screen.CreateApplication -> {
                CreateApplicationScreenView(
                    viewModel = applicationViewModel,
                    borrowers = borrowerUiState.borrowers,
                    onApplicationCreated = { newApp ->
                        appViewModel.navigateTo(Screen.ApplicationList)
                    },
                    onBackClick = {
                        appViewModel.navigateTo(Screen.ApplicationList)
                    }
                )
            }
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
                        onBackClick = { appViewModel.navigateTo(Screen.ApplicationDetail) }
                    )
                } else {
                    appViewModel.navigateTo(Screen.ApplicationDetail)
                }
            }
            Screen.DocumentUpload -> {
                val app = appUiState.selectedApplication
                val borrower = borrowerUiState.borrowers.find { it.id == app?.borrower_id }
                DocumentUploadScreen(
                    borrower = borrower,
                    onBackClick = { appViewModel.navigateTo(Screen.ApplicationDetail) },
                    onComplete = { updatedBorrower ->
                        borrowerViewModel.updateBorrowerLocal(updatedBorrower) {
                            appViewModel.navigateTo(Screen.ApplicationDetail)
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
                        onBackClick = { appViewModel.navigateTo(Screen.ApplicationDetail) },
                        onSave = { appViewModel.navigateTo(Screen.ApplicationDetail) }
                    )
                } else {
                    appViewModel.navigateTo(Screen.ApplicationDetail)
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
                        onBackClick = { appViewModel.navigateTo(Screen.ApplicationDetail) },
                        onSignComplete = { appViewModel.navigateTo(Screen.ApplicationDetail) }
                    )
                } else {
                    appViewModel.navigateTo(Screen.ApplicationDetail)
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
                        onBackClick = { appViewModel.navigateTo(Screen.ApplicationDetail) },
                        onSubmit = { appViewModel.navigateTo(Screen.ApplicationDetail) }
                    )
                } else {
                    appViewModel.navigateTo(Screen.ApplicationDetail)
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
                        onBackClick = { appViewModel.navigateTo(Screen.ApplicationDetail) },
                        onDecisionSubmitted = { appViewModel.navigateTo(Screen.ApplicationDetail) }
                    )
                } else {
                    appViewModel.navigateTo(Screen.ApplicationDetail)
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
                        onBackClick = { appViewModel.navigateTo(Screen.ApplicationDetail) },
                        onCompleteReview = { appViewModel.navigateTo(Screen.ApplicationDetail) }
                    )
                } else {
                    appViewModel.navigateTo(Screen.ApplicationDetail)
                }
            }
            Screen.AuditorCompliance -> {
                AuditorComplianceScreen(
                    onBackClick = { appViewModel.navigateTo(Screen.ApplicationDetail) },
                    onAuditComplete = { appViewModel.navigateTo(Screen.ApplicationDetail) }
                )
            }
            Screen.AdminMcrApproval -> {
                val app = appUiState.selectedApplication
                val borrower = borrowerUiState.borrowers.find { it.id == app?.borrower_id }
                if (app != null) {
                    AdminMcrApprovalScreen(
                        application = app,
                        borrower = borrower,
                        applicationViewModel = applicationViewModel,
                        onBackClick = { appViewModel.navigateTo(Screen.ApplicationDetail) },
                        onDisburseTriggered = { appViewModel.navigateTo(Screen.ApplicationDetail) }
                    )
                } else {
                    appViewModel.navigateTo(Screen.ApplicationDetail)
                }
            }
            Screen.DocumentViewer -> {
                DocumentViewerScreen(
                    onBackClick = { appViewModel.navigateTo(Screen.ApplicationDetail) }
                )
            }
            Screen.WorkflowEventAudit -> {
                WorkflowEventAuditScreen(
                    onBackClick = { appViewModel.navigateTo(Screen.ApplicationDetail) }
                )
            }
            Screen.Settings -> {
                SettingsScreen(
                    userName = appUiState.session?.userEmail?.substringBefore("@")?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: "Chidi Okafor",
                    userEmail = appUiState.session?.userEmail ?: "chidi@mmfb.com",
                    role = appUiState.session?.role,
                    onBackClick = { appViewModel.navigateTo(Screen.Dashboard) },
                    onNavigateToOfflineQueue = { showSyncModal = true },
                    onSignOutClick = { appViewModel.logout() }
                )
            }
            Screen.OfflineQueue -> {
                OfflineQueueScreen(
                    onBackClick = { appViewModel.navigateTo(Screen.Dashboard) }
                )
            }
        }
    }
}
