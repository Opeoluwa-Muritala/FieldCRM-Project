package com.fieldcrm.android.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.Immutable
import com.fieldcrm.android.data.repository.ApplicationRepository
import com.fieldcrm.android.data.repository.BorrowerRepository
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.fieldcrm.android.data.repository.ApplicationDetailResult
import kotlinx.coroutines.withContext
import java.util.UUID

@Immutable
data class ApplicationUiState(
    val applications: List<LoanApplicationModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // new-application form fields
    val customerType: String = "existing",          // "new" | "existing"
    val loanCategory: String = "enterprise",        // "enterprise" | "msef" | "payee" | "other"
    val selectedBorrowerForApp: BorrowerModel? = null,
    val newCustomerName: String = "",
    val newCustomerPhone: String = "",
    val newCustomerBvn: String = "",
    val newCustomerNin: String = "",
    val newAppAmount: String = "",
    val newAppTenorMonths: String = "",
    val newAppInterestRate: String = "18.5",
    val newAppRepaymentFrequency: String = "monthly",
    val newAppPurpose: String = "",
    val selectedAppDetail: ApplicationDetailResult? = null,
    val isLoadingDetail: Boolean = false
)

class ApplicationViewModel(
    application: Application,
    private val repository: ApplicationRepository,
    private val borrowerRepository: BorrowerRepository
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ApplicationUiState())
    val uiState: StateFlow<ApplicationUiState> = _uiState.asStateFlow()

    init {
        loadApplications()
    }

    private fun loadApplications() {
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) { repository.getCachedApplications() }
            if (cached.isNotEmpty()) {
                _uiState.update { it.copy(applications = cached) }
            }
            _uiState.update { it.copy(isLoading = cached.isEmpty()) }
            val fresh = repository.getAllApplications()
            _uiState.update { it.copy(applications = fresh, isLoading = false) }
        }
    }

    fun loadApplicationDetail(id: String) {
        _uiState.update { it.copy(isLoadingDetail = true, selectedAppDetail = null) }
        viewModelScope.launch {
            val detail = repository.getFullDetail(id)
            _uiState.update { it.copy(selectedAppDetail = detail, isLoadingDetail = false) }
        }
    }

    fun setSelectedBorrowerForApp(borrower: BorrowerModel?) {
        _uiState.update { it.copy(selectedBorrowerForApp = borrower, errorMessage = null) }
    }

    fun setCustomerType(value: String) {
        _uiState.update { it.copy(customerType = value, errorMessage = null) }
    }

    fun setLoanCategory(value: String) {
        _uiState.update { it.copy(loanCategory = value, errorMessage = null) }
    }

    fun setNewCustomerName(value: String) {
        _uiState.update { it.copy(newCustomerName = value, errorMessage = null) }
    }

    fun setNewCustomerPhone(value: String) {
        _uiState.update { it.copy(newCustomerPhone = value, errorMessage = null) }
    }

    fun setNewCustomerBvn(value: String) {
        _uiState.update { it.copy(newCustomerBvn = value, errorMessage = null) }
    }

    fun setNewCustomerNin(value: String) {
        _uiState.update { it.copy(newCustomerNin = value, errorMessage = null) }
    }

    fun setNewAppAmount(value: String) {
        _uiState.update { it.copy(newAppAmount = value, errorMessage = null) }
    }

    fun setNewAppTenorMonths(value: String) {
        _uiState.update { it.copy(newAppTenorMonths = value, errorMessage = null) }
    }

    fun setNewAppInterestRate(value: String) {
        _uiState.update { it.copy(newAppInterestRate = value, errorMessage = null) }
    }

    fun setNewAppRepaymentFrequency(value: String) {
        _uiState.update { it.copy(newAppRepaymentFrequency = value, errorMessage = null) }
    }

    fun setNewAppPurpose(value: String) {
        _uiState.update { it.copy(newAppPurpose = value, errorMessage = null) }
    }

    fun createApplication(onSuccess: (LoanApplicationModel, BorrowerModel) -> Unit) {
        val state = _uiState.value
        val isNew = state.customerType == "new"

        if (isNew) {
            if (state.newCustomerName.isBlank() || state.newCustomerPhone.isBlank() ||
                state.newCustomerBvn.isBlank()
            ) {
                _uiState.update { it.copy(errorMessage = "Please fill in name, phone and BVN") }
                return
            }
        } else {
            if (state.selectedBorrowerForApp == null) {
                _uiState.update { it.copy(errorMessage = "Please select a borrower profile") }
                return
            }
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val borrower = if (isNew) {
                val newBorrower = BorrowerModel(
                    id = UUID.randomUUID().toString(),
                    org_id = "org_1",
                    loan_officer_id = "lo_1",
                    name = state.newCustomerName,
                    phone = state.newCustomerPhone,
                    bvn = state.newCustomerBvn,
                    nin = state.newCustomerNin,
                    status = "ACTIVE",
                    created_at = System.currentTimeMillis().toString()
                )
                val bSuccess = borrowerRepository.createBorrower(newBorrower)
                if (!bSuccess) {
                    _uiState.update { it.copy(errorMessage = "Failed to register borrower profile offline.", isLoading = false) }
                    return@launch
                }
                newBorrower
            } else {
                state.selectedBorrowerForApp!!
            }

            val newApp = LoanApplicationModel(
                id = UUID.randomUUID().toString(),
                org_id = "org_1",
                applicant_name = borrower.name,
                phone = if (isNew) state.newCustomerPhone else borrower.phone,
                bvn = if (isNew) state.newCustomerBvn else borrower.bvn,
                stage = "intake",
                loan_type = state.loanCategory,
                customer_type = state.customerType,
                created_by = "lo_1",
                current_owner_id = borrower.loan_officer_id,
                created_at = System.currentTimeMillis().toString()
            )

            val success = repository.createApplication(newApp)
            if (success) {
                _uiState.update { it.copy(applications = it.applications + newApp) }
                clearNewAppFields()
                _uiState.update { it.copy(isLoading = false) }
                onSuccess(newApp, borrower)
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
                customerType = "existing",
                loanCategory = "enterprise",
                selectedBorrowerForApp = null,
                newCustomerName = "",
                newCustomerPhone = "",
                newCustomerBvn = "",
                newCustomerNin = "",
                newAppAmount = "",
                newAppTenorMonths = "",
                newAppInterestRate = "18.5",
                newAppRepaymentFrequency = "monthly",
                newAppPurpose = "",
                errorMessage = null
            )
        }
    }

    fun refreshApplications() {
        loadApplications()
    }

    fun syncQueue(onComplete: (Boolean) -> Unit) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val success = repository.syncWithServer()
            loadApplications()
            onComplete(success)
        }
    }

    fun approveApplication(id: String, onComplete: () -> Unit = {}) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.approveApplication(id)
            loadApplications()
            _uiState.update { it.copy(isLoading = false) }
            onComplete()
        }
    }

    fun returnApplication(id: String, reason: String, corrections: List<String> = emptyList(), notes: String, onComplete: () -> Unit = {}) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.returnApplication(id, reason, corrections, notes)
            loadApplications()
            _uiState.update { it.copy(isLoading = false) }
            onComplete()
        }
    }

    fun submitCreditReview(id: String, decision: String, notes: String, onComplete: () -> Unit = {}) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.submitCreditReview(id, decision, notes)
            loadApplications()
            _uiState.update { it.copy(isLoading = false) }
            onComplete()
        }
    }

    fun submitIntakeForm(
        updatedApp: LoanApplicationModel,
        updatedBorrower: BorrowerModel,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(applications = state.applications.map { if (it.id == updatedApp.id) updatedApp else it })
            }
            repository.createApplication(updatedApp)

            val ok = repository.submitIntakeToServer(
                id = updatedApp.id,
                amount = updatedApp.amount ?: 0.0,
                tenorMonths = updatedApp.tenor_months ?: 0,
                loanType = updatedApp.loan_type,
                purpose = updatedApp.purpose ?: ""
            )
            if (!ok) {
                repository.queueStageAction(
                    action = "SUBMIT_INTAKE",
                    entityId = updatedApp.id,
                    payloadJson = """{"id":"${updatedApp.id}","body":"{\"stage\":\"ocr_review\",\"amount\":${updatedApp.amount},\"tenor_months\":${updatedApp.tenor_months},\"loan_type\":\"${updatedApp.loan_type}\"}"}"""
                )
            }
            onSuccess()
        }
    }

    fun submitOcrReview(
        id: String,
        corrections: Map<String, String> = emptyMap(),
        onSuccess: () -> Unit
    ) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            // Optimistic local advance to credit_review
            val advanced = _uiState.value.applications.find { it.id == id }
                ?.copy(stage = "credit_review")
            if (advanced != null) {
                _uiState.update { s -> s.copy(applications = s.applications.map { if (it.id == id) advanced else it }) }
                repository.createApplication(advanced)
            }

            val ok = repository.submitOcrReview(id, corrections)
            if (!ok) {
                repository.queueStageAction(
                    action = "SUBMIT_OCR_REVIEW",
                    entityId = id,
                    payloadJson = """{"id":"$id","body":"{\"action\":\"verify\",\"corrections\":{}}"}"""
                )
            }
            _uiState.update { it.copy(isLoading = false) }
            onSuccess()
        }
    }

    fun executePledge(
        id: String,
        witnessName: String,
        pledgeValue: Double,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val desc = "Pledge & Trust Receipt Executed (Witness: $witnessName)"
            val updated = _uiState.value.applications.find { it.id == id }
                ?.copy(purpose = desc)
            if (updated != null) {
                _uiState.update { s -> s.copy(applications = s.applications.map { if (it.id == id) updated else it }) }
                repository.createApplication(updated)
            }
            val ok = repository.patchApplicationMeta(id, desc, pledgeValue)
            if (!ok) {
                repository.queueStageAction(
                    action = "EXECUTE_PLEDGE",
                    entityId = id,
                    payloadJson = """{"id":"$id","body":"{\"purpose\":\"$desc\",\"pledge_value\":$pledgeValue}"}"""
                )
            }
            onSuccess()
        }
    }

    fun submitVisitationReport(
        id: String,
        metWith: String,
        premises: String,
        direction: String,
        onSuccess: () -> Unit
    ) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val ok = repository.submitVisitationToServer(id, metWith, premises, direction)
            if (!ok) {
                val bodyJson = """{"met_with":"$metWith","premises_description":"$premises","direction_from_branch":"$direction"}"""
                repository.queueStageAction(
                    action = "SUBMIT_VISITATION",
                    entityId = id,
                    payloadJson = """{"id":"$id","body":"${bodyJson.replace("\"", "\\\"")}"}"""
                )
            }
            _uiState.update { it.copy(isLoading = false) }
            onSuccess()
        }
    }
}
