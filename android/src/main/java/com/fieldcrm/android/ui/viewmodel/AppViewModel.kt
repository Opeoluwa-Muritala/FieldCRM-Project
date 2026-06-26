package com.fieldcrm.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.Immutable
import com.fieldcrm.android.core.session.UserSession
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed class Screen {
    data object Login : Screen()
    data object ForgotPassword : Screen()
    data object ResetPassword : Screen()
    data object BiometricEnrollment : Screen()
    data object PermissionsPrimer : Screen()
    data object Notifications : Screen()
    data object SearchResults : Screen()
    data object Onboarding : Screen()
    data object Confirmation : Screen()
    data object Dashboard : Screen()
    data object BorrowerList : Screen()
    data object BorrowerDetail : Screen()
    data object CreateBorrower : Screen()
    data object ApplicationList : Screen()
    data object ApplicationDetail : Screen()
    data object CreateApplication : Screen()
    data object LoanApplicationForm : Screen()
    data object DocumentUpload : Screen()
    data object GuarantorsForm : Screen()
    data object PledgeTrust : Screen()
    data object VisitationReport : Screen()
    data object BranchManagerReview : Screen()
    data object CreditOfficerReview : Screen()
    data object AuditorCompliance : Screen()
    data object AdminMcrApproval : Screen()
    data object DocumentViewer : Screen()
    data object WorkflowEventAudit : Screen()
    data object Settings : Screen()
    data object OfflineQueue : Screen()
}

@Immutable
data class AppUiState(
    val currentScreen: Screen = Screen.Login,
    val session: UserSession? = null,
    val selectedBorrower: BorrowerModel? = null,
    val selectedApplication: LoanApplicationModel? = null,
    val isSessionExpired: Boolean = false,
    val hasEnrolledBiometrics: Boolean = false,
    val hasSeenOnboarding: Boolean = false,
    val hasSeenPermissions: Boolean = false,
    val successTitle: String = "",
    val successSubtitle: String = "",
    val successDestination: Screen = Screen.Dashboard
)

class AppViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    fun navigateTo(screen: Screen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun setSession(session: UserSession) {
        _uiState.update { it.copy(session = session) }
    }

    fun setSelectedBorrower(borrower: BorrowerModel?) {
        _uiState.update { it.copy(selectedBorrower = borrower) }
    }

    fun setSelectedApplication(application: LoanApplicationModel?) {
        _uiState.update { it.copy(selectedApplication = application) }
    }

    fun setSessionExpired(expired: Boolean) {
        _uiState.update { it.copy(isSessionExpired = expired) }
    }

    fun setBiometricsEnrolled(enrolled: Boolean) {
        _uiState.update { it.copy(hasEnrolledBiometrics = enrolled) }
    }

    fun setOnboardingSeen(seen: Boolean) {
        _uiState.update { it.copy(hasSeenOnboarding = seen) }
    }

    fun setPermissionsSeen(seen: Boolean) {
        _uiState.update { it.copy(hasSeenPermissions = seen) }
    }

    fun triggerSuccessScreen(title: String, subtitle: String, destination: Screen) {
        _uiState.update {
            it.copy(
                currentScreen = Screen.Confirmation,
                successTitle = title,
                successSubtitle = subtitle,
                successDestination = destination
            )
        }
    }

    fun logout() {
        _uiState.value = AppUiState()
    }
}
