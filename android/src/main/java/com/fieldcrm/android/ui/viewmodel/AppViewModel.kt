package com.fieldcrm.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey
import com.fieldcrm.android.core.session.SessionStore
import com.fieldcrm.android.core.session.UserSession
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable

// ── Route keys ────────────────────────────────────────────────────────────────
// Each object / data class is a type-safe key pushed onto the Nav 3 back stack.

@Serializable
sealed class Screen : NavKey {
    @Serializable
    data object Login : Screen()
    @Serializable
    data object ForgotPassword : Screen()
    @Serializable
    data object ResetPassword : Screen()
    @Serializable
    data object BiometricEnrollment : Screen()
    @Serializable
    data object PermissionsPrimer : Screen()
    @Serializable
    data object Notifications : Screen()
    @Serializable
    data object SearchResults : Screen()
    @Serializable
    data object Onboarding : Screen()
    @Serializable
    data object Confirmation : Screen()
    @Serializable
    data object Dashboard : Screen()
    @Serializable
    data object BorrowerList : Screen()
    @Serializable
    data object BorrowerDetail : Screen()
    @Serializable
    data object CreateBorrower : Screen()
    @Serializable
    data object ApplicationDetail : Screen()
    @Serializable
    data object CreateApplication : Screen()
    @Serializable
    data object LoanApplicationForm : Screen()
    @Serializable
    data object DocumentUpload : Screen()
    @Serializable
    data object GuarantorsForm : Screen()
    @Serializable
    data object PledgeTrust : Screen()
    @Serializable
    data object VisitationReport : Screen()
    @Serializable
    data object BranchManagerReview : Screen()
    @Serializable
    data object CreditOfficerReview : Screen()
    @Serializable
    data object AuditorCompliance : Screen()
    @Serializable
    data object AdminMcrApproval : Screen()
    @Serializable
    data object DocumentViewer : Screen()
    @Serializable
    data object WorkflowEventAudit : Screen()
    @Serializable
    data object Settings : Screen()
    @Serializable
    data object OfflineQueue : Screen()
    @Serializable
    data object MyQueue : Screen()
    @Serializable
    data object VisitsDue : Screen()
    @Serializable
    data object AwaitingConcurrence : Screen()
    @Serializable
    data object PendingSignoffs : Screen()
    @Serializable
    data object CreditReviewQueue : Screen()
    @Serializable
    data object OcrExceptions : Screen()
    @Serializable
    data object Pipeline : Screen()
    @Serializable
    data object Users : Screen()
    @Serializable
    data object SystemActivity : Screen()
    @Serializable
    data object AuditTrail : Screen()
    @Serializable
    data object ComplianceFlags : Screen()
    @Serializable
    data object OcrReview : Screen()
    @Serializable
    data object PasscodeSetup : Screen()
    @Serializable
    data object PasscodeLogin : Screen()
}

// ── Biometric action intent ────────────────────────────────────────────────────

enum class BiometricAction { ENROLL, LOGIN }

// ── App-level UI state (session + selection only — nav lives in the back stack) ──

@Immutable
data class AppUiState(
    val session: UserSession? = null,
    val selectedBorrower: BorrowerModel? = null,
    val selectedApplication: LoanApplicationModel? = null,
    val isSessionExpired: Boolean = false,
    val hasEnrolledBiometrics: Boolean = false,
    val hasSeenBiometricEnrollment: Boolean = false,
    val hasSeenOnboarding: Boolean = false,
    val hasSeenPermissions: Boolean = false,
    val hasPasscode: Boolean = false,
    val passcodeHash: String? = null,
    val pendingBiometricAction: BiometricAction? = null,
    val successTitle: String = "",
    val successSubtitle: String = "",
    val successDestination: Screen = Screen.Dashboard
)

class AppViewModel(private val sessionStore: SessionStore) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AppUiState(
            hasEnrolledBiometrics = sessionStore.isBiometricEnrolled(),
            hasSeenBiometricEnrollment = sessionStore.hasBiometricEnrollmentBeenShown(),
            hasSeenOnboarding = sessionStore.hasSeenOnboarding(),
            hasSeenPermissions = sessionStore.hasSeenPermissions(),
            hasPasscode = sessionStore.hasPasscode(),
            passcodeHash = sessionStore.getPasscodeHash()
        )
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

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
        sessionStore.setBiometricEnrolled(enrolled)
        _uiState.update { it.copy(hasEnrolledBiometrics = enrolled) }
    }

    fun setOnboardingSeen(seen: Boolean) {
        if (seen) sessionStore.setOnboardingSeen()
        _uiState.update { it.copy(hasSeenOnboarding = seen) }
    }

    fun setPermissionsSeen(seen: Boolean) {
        if (seen) sessionStore.setPermissionsSeen()
        _uiState.update { it.copy(hasSeenPermissions = seen) }
    }

    fun markBiometricEnrollmentShown() {
        sessionStore.markBiometricEnrollmentShown()
        _uiState.update { it.copy(hasSeenBiometricEnrollment = true) }
    }

    fun setPasscodeHash(hash: String) {
        sessionStore.savePasscodeHash(hash)
        _uiState.update { it.copy(hasPasscode = true, passcodeHash = hash) }
    }

    fun clearPasscode() {
        sessionStore.clearPasscode()
        _uiState.update { it.copy(hasPasscode = false, passcodeHash = null) }
    }

    fun setBiometricAction(action: BiometricAction?) {
        _uiState.update { it.copy(pendingBiometricAction = action) }
    }

    fun triggerSuccessScreen(title: String, subtitle: String, destination: Screen) {
        _uiState.update { it.copy(successTitle = title, successSubtitle = subtitle, successDestination = destination) }
    }

    fun logout() {
        sessionStore.clear()
        _uiState.value = AppUiState()
    }
}
