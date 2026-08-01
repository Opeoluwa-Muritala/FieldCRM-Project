package com.fieldcrm.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldcrm.android.data.api.MobileApiService
import com.fieldcrm.android.core.network.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CrmReviewUiState(
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val checklist: Map<String, Boolean> = emptyMap()
)

class CrmReviewViewModel(private val api: MobileApiService) : ViewModel() {

    private val _uiState = MutableStateFlow(CrmReviewUiState())
    val uiState: StateFlow<CrmReviewUiState> = _uiState.asStateFlow()

    fun loadChecklist(applicationId: String) {
        viewModelScope.launch {
            when (val result = api.getCreditChecklist(applicationId, "crm_review")) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(
                    checklist = result.data.items.associate { it.item_key to it.is_checked }
                )
                else -> Unit
            }
        }
    }

    fun submitCrmReview(
        applicationId: String,
        decision: String,
        notes: String,
        bureau1: Boolean = false,
        bureau2: Boolean = false,
        crms: Boolean = false,
        ncr: Boolean = false,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
            try {
                api.submitCrmReview(applicationId, decision, notes, bureau1, bureau2, crms, ncr)
                    ?: error("The server did not confirm this review")
                _uiState.value = _uiState.value.copy(isSubmitting = false)
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = e.message)
            }
        }
    }

    fun submitExecutiveApprove(applicationId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
            try {
                api.submitExecutiveApprove(applicationId)
                    ?: error("The server did not confirm this approval")
                _uiState.value = _uiState.value.copy(isSubmitting = false)
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = e.message)
            }
        }
    }

    fun submitEdApprove(id: String, action: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
            try {
                api.submitEdApprove(id, action)
                    ?: error("The server did not confirm this ED decision")
                _uiState.value = _uiState.value.copy(isSubmitting = false)
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = e.message)
            }
        }
    }

    fun submitMdApprove(id: String, action: String, notes: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
            try {
                api.submitMdApprove(id, action, notes)
                    ?: error("The server did not confirm this MD decision")
                _uiState.value = _uiState.value.copy(isSubmitting = false)
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = e.message)
            }
        }
    }

    fun addBoardReferral(id: String, email: String, name: String, notes: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
            try {
                api.addBoardReferral(id, email, name, notes)
                    ?: error("The server did not confirm this referral")
                _uiState.value = _uiState.value.copy(isSubmitting = false)
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = e.message)
            }
        }
    }
}
