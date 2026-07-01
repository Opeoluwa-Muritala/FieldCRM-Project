package com.fieldcrm.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldcrm.android.data.api.AuditChecklist
import com.fieldcrm.android.data.api.AuditTrailEvent
import com.fieldcrm.android.data.api.MobileApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuditTrailUiState(
    val events: List<AuditTrailEvent> = emptyList(),
    val checklist: AuditChecklist? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

class AuditTrailViewModel(private val apiService: MobileApiService) : ViewModel() {

    private val _uiState = MutableStateFlow(AuditTrailUiState())
    val uiState: StateFlow<AuditTrailUiState> = _uiState.asStateFlow()

    fun load(applicationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val events = apiService.getAuditTrail(applicationId)
            _uiState.value = _uiState.value.copy(events = events, isLoading = false)
        }
    }

    fun loadChecklist(applicationId: String) {
        viewModelScope.launch {
            val checklist = apiService.getAuditChecklist(applicationId)
            if (checklist != null) {
                _uiState.value = _uiState.value.copy(checklist = checklist)
            }
        }
    }

    fun saveChecklist(applicationId: String, checklist: AuditChecklist, onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            if (applicationId.isNotEmpty()) {
                apiService.saveAuditChecklist(applicationId, checklist)
            }
            _uiState.value = _uiState.value.copy(isSaving = false)
            onDone()
        }
    }
}
