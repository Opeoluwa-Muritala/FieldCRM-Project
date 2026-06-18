package com.fieldcrm.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.Immutable
import com.fieldcrm.android.core.session.UserRole
import com.fieldcrm.android.core.session.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

@Immutable
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

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
        val session = UserSession(
            token = "token_${UUID.randomUUID()}",
            role = UserRole.fromLoginIdentifier(state.email),
            orgId = "org_1",
            userEmail = state.email
        )
        _uiState.update { it.copy(isLoading = false) }
        onSuccess(session)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
