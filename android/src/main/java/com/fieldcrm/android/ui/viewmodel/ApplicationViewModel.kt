package com.fieldcrm.android.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.Immutable
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.fieldcrm.android.data.repository.ApplicationRepository
import com.fieldcrm.shared.api.FieldCRMClient
import com.fieldcrm.shared.db.AppDatabase
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

@Immutable
data class ApplicationUiState(
    val applications: List<LoanApplicationModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedBorrowerForApp: BorrowerModel? = null,
    val newAppAmount: String = "",
    val newAppTenure: String = "",
    val newAppInterestRate: String = "18.5",
    val newAppProductType: String = "PERSONAL_LOAN"
)

class ApplicationViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ApplicationUiState())
    val uiState: StateFlow<ApplicationUiState> = _uiState.asStateFlow()

    private val database: AppDatabase
    private val client: FieldCRMClient
    private val repository: ApplicationRepository

    init {
        val driver = AndroidSqliteDriver(
            schema = AppDatabase.Schema,
            context = application,
            name = "fieldcrm_offline.db"
        )
        database = AppDatabase(driver)

        // Device IP resolution: uses loopback for physical USB device with adb reverse,
        // and 10.0.2.2 loopback for Android SDK emulator.
        val isEmulator = android.os.Build.FINGERPRINT.startsWith("generic")
            || android.os.Build.MODEL.contains("google_sdk")
            || android.os.Build.MODEL.contains("Emulator")
            || android.os.Build.MODEL.contains("Android SDK built for x86")
        val baseUrl = if (isEmulator) "http://10.0.2.2:8000" else "http://localhost:8000"

        client = FieldCRMClient(baseUrl)
        repository = ApplicationRepository(database, client)

        loadApplications()
    }

    private fun loadApplications() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val list = repository.getAllApplications()
            _uiState.update { it.copy(applications = list, isLoading = false) }
        }
    }

    fun setSelectedBorrowerForApp(borrower: BorrowerModel?) {
        _uiState.update { it.copy(selectedBorrowerForApp = borrower, errorMessage = null) }
    }

    fun setNewAppAmount(value: String) {
        _uiState.update { it.copy(newAppAmount = value, errorMessage = null) }
    }

    fun setNewAppTenure(value: String) {
        _uiState.update { it.copy(newAppTenure = value, errorMessage = null) }
    }

    fun setNewAppInterestRate(value: String) {
        _uiState.update { it.copy(newAppInterestRate = value, errorMessage = null) }
    }

    fun setNewAppProductType(value: String) {
        _uiState.update { it.copy(newAppProductType = value, errorMessage = null) }
    }

    fun createApplication(onSuccess: (LoanApplicationModel) -> Unit) {
        val state = _uiState.value
        val borrower = state.selectedBorrowerForApp
        if (borrower == null || state.newAppAmount.isBlank() || state.newAppTenure.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please fill in all required fields") }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        val newApp = LoanApplicationModel(
            id = UUID.randomUUID().toString(),
            org_id = "org_1",
            borrower_id = borrower.id,
            current_stage = 1,
            current_owner_id = borrower.loan_officer_id,
            status = "PENDING_CREDIT_REVIEW",
            amount = state.newAppAmount.toDoubleOrNull() ?: 0.0,
            tenure = state.newAppTenure.toIntOrNull() ?: 0,
            product_type = state.newAppProductType,
            interest_rate = state.newAppInterestRate.toDoubleOrNull() ?: 18.5,
            repayment_frequency = "MONTHLY",
            created_at = System.currentTimeMillis().toString()
        )

        viewModelScope.launch {
            val success = repository.createApplication(newApp)
            if (success) {
                _uiState.update { it.copy(applications = it.applications + newApp) }
                clearNewAppFields()
                _uiState.update { it.copy(isLoading = false) }
                onSuccess(newApp)
            } else {
                _uiState.update { it.copy(errorMessage = "Network error. Queued for offline sync.", isLoading = false) }
            }
        }
    }

    fun updateApplicationLocal(updatedApp: LoanApplicationModel, onComplete: () -> Unit = {}) {
        _uiState.update { state ->
            val updatedList = state.applications.map {
                if (it.id == updatedApp.id) updatedApp else it
            }
            state.copy(applications = updatedList)
        }
        viewModelScope.launch {
            repository.createApplication(updatedApp)
            onComplete()
        }
    }

    private fun clearNewAppFields() {
        _uiState.update {
            it.copy(
                selectedBorrowerForApp = null,
                newAppAmount = "",
                newAppTenure = "",
                newAppInterestRate = "18.5",
                newAppProductType = "PERSONAL_LOAN",
                errorMessage = null
            )
        }
    }

    fun refreshApplications() {
        loadApplications()
    }
}
