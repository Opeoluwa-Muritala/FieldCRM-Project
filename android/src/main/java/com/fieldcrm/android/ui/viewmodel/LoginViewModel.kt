package com.fieldcrm.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.Immutable
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.core.session.UserSession
import com.fieldcrm.android.core.session.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope
import com.fieldcrm.android.data.repository.AuthRepository
import kotlinx.coroutines.launch

@Immutable
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isRestoringSession: Boolean = true,
    val error: String? = null
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        restoreSession()
    }

    // On cold start: check if a stored session exists and is still valid on the server
    private fun restoreSession() {
        _uiState.update { it.copy(isRestoringSession = true) }
        viewModelScope.launch {
            val stored = sessionStore.load()
            if (stored != null) {
                val stillValid = authRepository.validateToken(stored.token)
                if (stillValid) {
                    // Extend TTL on each successful validation — keeps active users logged in
                    sessionStore.extendSession()
                    _uiState.update { it.copy(isRestoringSession = false) }
                    _restoredSession.value = stored
                    return@launch
                } else {
                    sessionStore.clear()
                }
            }
            _uiState.update { it.copy(isRestoringSession = false) }
        }
    }

    // Emits a non-null value when a restored session is ready — observed by MainActivity
    private val _restoredSession = MutableStateFlow<UserSession?>(null)
    val restoredSession: StateFlow<UserSession?> = _restoredSession.asStateFlow()

    fun setEmail(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun setPassword(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun login(onSuccess: (UserSession) -> Unit) {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "Please fill in all fields") }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val token = authRepository.authenticate(state.email, state.password)
            if (token == null) {
                _uiState.update { it.copy(isLoading = false, error = "Invalid credentials. Please check your email and password.") }
                return@launch
            }

            // Fetch real profile from server — role, name, orgId
            val me = authRepository.fetchMe()
            val session = UserSession(
                token = token,
                role = if (me != null) UserRole.fromServerRole(me.role) else UserRole.LOAN_OFFICER,
                orgId = me?.org_id ?: "org_1",
                userEmail = me?.email ?: state.email,
                userName = me?.full_name ?: state.email.substringBefore("@"),
                loginExpiresAt = System.currentTimeMillis() + 48L * 60 * 60 * 1000
            )

            sessionStore.save(session)
            _uiState.update { it.copy(isLoading = false) }
            onSuccess(session)
        }
    }

    // Called when the user re-authenticates from the SessionExpiredScreen
    // Re-runs login with the stored email so the session is persisted again
    fun reauthenticate(email: String, password: String, onSuccess: (UserSession) -> Unit) {
        _uiState.update { it.copy(email = email, password = password) }
        login(onSuccess)
    }

    // Periodic sync — call from MainActivity's onResume or a WorkManager job
    // Returns true if the session is still valid and extends the TTL
    fun syncSession(onExpired: () -> Unit) {
        val stored = sessionStore.load() ?: return
        viewModelScope.launch {
            val valid = authRepository.validateToken(stored.token)
            if (valid) {
                sessionStore.extendSession()
            } else {
                sessionStore.clear()
                onExpired()
            }
        }
    }

    fun restoreStoredSession(onSuccess: (UserSession) -> Unit, onError: () -> Unit) {
        val stored = sessionStore.load()
        if (stored != null) onSuccess(stored) else onError()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
