package com.fieldcrm.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldcrm.android.data.api.AppConfig
import com.fieldcrm.android.data.repository.ConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConfigUiState(
    val config: AppConfig? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ConfigViewModel(private val repo: ConfigRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState: StateFlow<ConfigUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val config = repo.getConfig(forceRefresh)
            _uiState.value = if (config != null) {
                _uiState.value.copy(config = config, isLoading = false)
            } else {
                _uiState.value.copy(isLoading = false, error = "Could not load configuration")
            }
        }
    }
}
