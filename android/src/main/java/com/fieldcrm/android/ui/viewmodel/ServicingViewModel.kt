package com.fieldcrm.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldcrm.android.data.api.MobileApiService
import com.fieldcrm.android.data.api.ParSummary
import com.fieldcrm.android.data.api.PaymentRecord
import com.fieldcrm.android.data.api.RepaymentScheduleRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class ServicingUiState(
    val schedule: List<RepaymentScheduleRow> = emptyList(),
    val payments: List<PaymentRecord> = emptyList(),
    val totalDue: Double = 0.0,
    val totalPaid: Double = 0.0,
    val outstanding: Double = 0.0,
    val par: ParSummary? = null,
    val parLoans: List<Map<String, Any>> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null
)

class ServicingViewModel(private val api: MobileApiService) : ViewModel() {

    private val _uiState = MutableStateFlow(ServicingUiState())
    val uiState: StateFlow<ServicingUiState> = _uiState.asStateFlow()

    fun loadRepaymentSchedule(applicationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = api.getRepaymentSchedule(applicationId)
                if (response != null) {
                    _uiState.value = _uiState.value.copy(
                        schedule = response.schedule,
                        payments = response.payments,
                        totalDue = response.total_due,
                        totalPaid = response.total_paid,
                        outstanding = response.outstanding,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to load repayment schedule")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun recordPayment(
        applicationId: String,
        amount: Double,
        channel: String,
        bankRef: String?,
        paymentDate: String?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
            try {
                api.recordPayment(applicationId, amount, channel, bankRef, paymentDate)
                _uiState.value = _uiState.value.copy(isSubmitting = false)
                loadRepaymentSchedule(applicationId)
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = e.message)
            }
        }
    }

    fun loadParDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val json = api.getParDashboard()
                if (json != null) {
                    val parsed = Json.decodeFromString<ParSummary>(json)
                    _uiState.value = _uiState.value.copy(par = parsed, isLoading = false)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to load PAR data")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
