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
import com.fieldcrm.android.data.api.LoginOutcome
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

    // On cold start: restore from local store immediately so the user is never blocked on a
    // network call. Then validate the token in the background — only clear the session if the
    // server definitively rejects it (false). Network errors (null) leave the session untouched.
    private fun restoreSession() {
        _uiState.update { it.copy(isRestoringSession = true) }
        viewModelScope.launch {
            val stored = sessionStore.load()
            if (stored == null) {
                _uiState.update { it.copy(isRestoringSession = false) }
                return@launch
            }

            // Apply token to network client immediately so any post-restore requests
            // (syncQueue, refreshBorrowers, etc.) go out authenticated.
            authRepository.applyStoredToken(stored.token)

            // Restore immediately — no network wait, no blank screen.
            _restoredSession.value = stored
            _uiState.update { it.copy(isRestoringSession = false) }

            // Background validation: only act on a definitive server rejection.
            val result = authRepository.validateToken(stored.token)
            when (result) {
                true -> sessionStore.extendSession()
                false -> {
                    sessionStore.clear()
                    _restoredSession.value = null
                    _sessionInvalidated.value = true
                }
                null -> { /* network unavailable — local TTL governs expiry */ }
            }
        }
    }

    // Signals that a background token check found the session definitively rejected.
    private val _sessionInvalidated = MutableStateFlow(false)
    val sessionInvalidated: StateFlow<Boolean> = _sessionInvalidated.asStateFlow()

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
            when (val outcome = authRepository.authenticate(state.email, state.password)) {
                is LoginOutcome.NetworkError -> {
                    _uiState.update { it.copy(isLoading = false, error = "network_error") }
                    return@launch
                }
                is LoginOutcome.InvalidCredentials -> {
                    _uiState.update { it.copy(isLoading = false, error = "invalid_credentials") }
                    return@launch
                }
                is LoginOutcome.ServerError -> {
                    _uiState.update { it.copy(isLoading = false, error = "Server error (${outcome.code}). Try again later.") }
                    return@launch
                }
                is LoginOutcome.Success -> {
                    val me = authRepository.fetchMe()
                    val session = UserSession(
                        token = outcome.token,
                        role = if (me != null) UserRole.fromServerRole(me.role) else UserRole.LOAN_OFFICER,
                        orgId = me?.org_id ?: "org_1",
                        userEmail = me?.email ?: state.email,
                        userName = me?.full_name ?: state.email.substringBefore("@"),
                        loginExpiresAt = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000
                    )
                    sessionStore.save(session)
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess(session)
                }
            }
        }
    }

    // Called when the user re-authenticates from the SessionExpiredScreen
    // Re-runs login with the stored email so the session is persisted again
    fun reauthenticate(email: String, password: String, onSuccess: (UserSession) -> Unit) {
        _uiState.update { it.copy(email = email, password = password) }
        login(onSuccess)
    }

    // Periodic sync — only clears session on a definitive server rejection (false).
    // Network errors (null) leave the session untouched — offline users stay logged in.
    fun syncSession(onExpired: () -> Unit) {
        val stored = sessionStore.load() ?: return
        viewModelScope.launch {
            when (authRepository.validateToken(stored.token)) {
                true -> sessionStore.extendSession()
                false -> { sessionStore.clear(); onExpired() }
                null -> { /* network error — keep session alive via local expiry */ }
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
