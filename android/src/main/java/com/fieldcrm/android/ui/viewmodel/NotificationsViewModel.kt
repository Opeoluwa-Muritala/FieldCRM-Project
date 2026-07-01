package com.fieldcrm.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldcrm.android.data.api.ApiNotification
import com.fieldcrm.android.data.repository.NotificationsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notifications: List<ApiNotification> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class NotificationsViewModel(
    private val repository: NotificationsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.getNotifications()
            _uiState.update { it.copy(notifications = result, isLoading = false) }
        }
    }

    fun markRead(id: String) {
        // Optimistic update
        _uiState.update { state ->
            state.copy(notifications = state.notifications.map {
                if (it.id == id) it.copy(is_read = true) else it
            })
        }
        viewModelScope.launch { repository.markRead(id) }
    }

    fun dismiss(id: String) {
        _uiState.update { state ->
            state.copy(notifications = state.notifications.filter { it.id != id })
        }
        viewModelScope.launch { repository.markRead(id) }
    }

    fun clearAll() {
        _uiState.update { it.copy(notifications = emptyList()) }
        viewModelScope.launch { repository.clearAll() }
    }
}
