package com.fieldcrm.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldcrm.android.data.api.MobileApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CrmReviewUiState(
    val isSubmitting: Boolean = false,
    val error: String? = null
)

class CrmReviewViewModel(private val api: MobileApiService) : ViewModel() {

    private val _uiState = MutableStateFlow(CrmReviewUiState())
    val uiState: StateFlow<CrmReviewUiState> = _uiState.asStateFlow()

    fun submitCrmReview(
        applicationId: String,
        decision: String,
        notes: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
            try {
                api.submitCrmReview(applicationId, decision, notes)
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
                _uiState.value = _uiState.value.copy(isSubmitting = false)
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = e.message)
            }
        }
    }

    fun submitCommitteeVote(id: String, recommendation: String, notes: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
            try {
                api.submitCommitteeVote(id, recommendation, notes)
                _uiState.value = _uiState.value.copy(isSubmitting = false)
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = e.message)
            }
        }
    }

    fun completeCommitteeReview(id: String, recommendation: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
            try {
                api.completeCommitteeReview(id, recommendation)
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
                _uiState.value = _uiState.value.copy(isSubmitting = false)
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = e.message)
            }
        }
    }
}
