package com.fieldcrm.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldcrm.android.data.api.DashboardMetrics
import com.fieldcrm.android.data.repository.DashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val metrics: DashboardMetrics? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class DashboardViewModel(private val repo: DashboardRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadMetrics()
    }

    fun loadMetrics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val metrics = repo.getMetrics()
            _uiState.value = if (metrics != null) {
                _uiState.value.copy(metrics = metrics, isLoading = false)
            } else {
                _uiState.value.copy(isLoading = false, error = "Could not load dashboard metrics")
            }
        }
    }
}
