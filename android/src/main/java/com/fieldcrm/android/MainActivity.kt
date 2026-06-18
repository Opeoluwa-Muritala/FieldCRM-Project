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
import com.fieldcrm.android.ui.screens.*
import com.fieldcrm.android.ui.viewmodel.*
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.core.session.UserSession

import com.fieldcrm.android.ui.theme.FieldCRMTheme
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appViewModel: AppViewModel = viewModel()
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
fun FieldCRMApp(appViewModel: AppViewModel = viewModel()) {
    val loginViewModel: LoginViewModel = viewModel()
    val borrowerViewModel: BorrowerViewModel = viewModel()
    val applicationViewModel: ApplicationViewModel = viewModel()
    val appUiState by appViewModel.uiState.collectAsState()
    val borrowerUiState by borrowerViewModel.uiState.collectAsState()
    val applicationUiState by applicationViewModel.uiState.collectAsState()

    when (appUiState.currentScreen) {
        Screen.Login -> {
            LoginScreenView(
                viewModel = loginViewModel,
                onLoginSuccess = { session ->
                    appViewModel.setSession(session)
                    appViewModel.navigateTo(Screen.Dashboard)
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
                    appViewModel.navigateTo(Screen.OfflineQueue)
                },
                onLogout = {
                    appViewModel.logout()
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
            LoanApplicationFormScreen(
                onBackClick = { appViewModel.navigateTo(Screen.ApplicationDetail) },
                onFinish = { appViewModel.navigateTo(Screen.ApplicationDetail) }
            )
        }
        Screen.DocumentUpload -> {
            DocumentUploadScreen(
                onBackClick = { appViewModel.navigateTo(Screen.ApplicationDetail) },
                onComplete = { appViewModel.navigateTo(Screen.ApplicationDetail) }
            )
        }
        Screen.GuarantorsForm -> {
            GuarantorsFormScreen(
                onBackClick = { appViewModel.navigateTo(Screen.ApplicationDetail) },
                onSave = { appViewModel.navigateTo(Screen.ApplicationDetail) }
            )
        }
        Screen.PledgeTrust -> {
            PledgeTrustScreen(
                onBackClick = { appViewModel.navigateTo(Screen.ApplicationDetail) },
                onSignComplete = { appViewModel.navigateTo(Screen.ApplicationDetail) }
            )
        }
        Screen.VisitationReport -> {
            VisitationReportScreen(
                onBackClick = { appViewModel.navigateTo(Screen.ApplicationDetail) },
                onSubmit = { appViewModel.navigateTo(Screen.ApplicationDetail) }
            )
        }
        Screen.BranchManagerReview -> {
            BranchManagerReviewScreen(
                onBackClick = { appViewModel.navigateTo(Screen.ApplicationDetail) },
                onDecisionSubmitted = { appViewModel.navigateTo(Screen.ApplicationDetail) }
            )
        }
        Screen.CreditOfficerReview -> {
            CreditOfficerReviewScreen(
                onBackClick = { appViewModel.navigateTo(Screen.ApplicationDetail) },
                onCompleteReview = { appViewModel.navigateTo(Screen.ApplicationDetail) }
            )
        }
        Screen.AuditorCompliance -> {
            AuditorComplianceScreen(
                onBackClick = { appViewModel.navigateTo(Screen.ApplicationDetail) },
                onAuditComplete = { appViewModel.navigateTo(Screen.ApplicationDetail) }
            )
        }
        Screen.AdminMcrApproval -> {
            AdminMcrApprovalScreen(
                onBackClick = { appViewModel.navigateTo(Screen.ApplicationDetail) },
                onDisburseTriggered = { appViewModel.navigateTo(Screen.ApplicationDetail) }
            )
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
                onBackClick = { appViewModel.navigateTo(Screen.Dashboard) },
                onNavigateToOfflineQueue = { appViewModel.navigateTo(Screen.OfflineQueue) }
            )
        }
        Screen.OfflineQueue -> {
            OfflineQueueScreen(
                onBackClick = { appViewModel.navigateTo(Screen.Dashboard) }
            )
        }
    }
}
