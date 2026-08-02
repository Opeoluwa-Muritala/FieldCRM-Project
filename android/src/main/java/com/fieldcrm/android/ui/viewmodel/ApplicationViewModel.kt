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
import com.fieldcrm.android.core.network.ApiResult
import kotlinx.serialization.json.JsonElement
import com.fieldcrm.android.core.session.SessionStore
import com.fieldcrm.android.data.api.ExistingCustomerSearchItem
import com.fieldcrm.android.data.api.PersonalProfileSnapshot

@Immutable
data class ApplicationUiState(
    val applications: List<LoanApplicationModel> = emptyList(),
    val isLoading: Boolean = false,
    val isStale: Boolean = false,
    val errorMessage: String? = null,
    // new-application form fields
    val customerType: String = "existing",          // "new" | "existing"
    val loanCategory: String = "enterprise",        // "enterprise" | "msef" | "payee" | "other"
    val selectedBorrowerForApp: BorrowerModel? = null,
    val customerSearchQuery: String = "",
    val customerSearchResults: List<ExistingCustomerSearchItem> = emptyList(),
    val isSearchingCustomers: Boolean = false,
    val selectedCustomerProfile: PersonalProfileSnapshot? = null,
    val newCustomerName: String = "",
    val newCustomerPhone: String = "",
    val newCustomerBvn: String = "",
    val newCustomerNin: String = "",
    val newAppAmount: String = "",
    val newAppTenorMonths: String = "",
    val newAppInterestRate: String = "",
    val newAppRepaymentFrequency: String = "",
    val newAppPurpose: String = "",
    val selectedAppDetail: ApplicationDetailResult? = null,
    val isLoadingDetail: Boolean = false,
    val shareUrl: String? = null,
    val isGeneratingLink: Boolean = false,
    val isMutating: Boolean = false,
    val bureauReport: JsonElement? = null,
    val bureauData: com.fieldcrm.android.data.api.BureauData? = null,
    val reviewChecklist: Map<String, Boolean> = emptyMap(),
    val ocrFields: List<com.fieldcrm.android.data.api.OcrExtractedField> = emptyList()
)

class ApplicationViewModel(
    application: Application,
    private val repository: ApplicationRepository,
    private val borrowerRepository: BorrowerRepository,
    private val sessionStore: SessionStore
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ApplicationUiState())
    val uiState: StateFlow<ApplicationUiState> = _uiState.asStateFlow()
    private var lastRefreshTime = 0L

    fun refreshIfStale() {
        val now = System.currentTimeMillis()
        if (now - lastRefreshTime > 30000L) {
            refreshApplications()
        }
    }

    init {
        loadApplications()
    }

    fun loadBureauData(applicationId: String) {
        viewModelScope.launch {
            val data = repository.getBureauData(applicationId)
            _uiState.update { it.copy(bureauData = data) }
        }
    }

    fun loadReviewChecklist(applicationId: String, context: String) {
        viewModelScope.launch {
            when (val result = repository.getCreditChecklist(applicationId, context)) {
                is ApiResult.Success -> _uiState.update { state ->
                    state.copy(reviewChecklist = result.data.items.associate { it.item_key to it.is_checked })
                }
                else -> Unit
            }
        }
    }

    fun loadOcrFields(applicationId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val response = repository.getOcrFields(applicationId)
            _uiState.update { it.copy(ocrFields = response?.items ?: emptyList(), isLoading = false) }
        }
    }

    private fun loadApplications() {
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) { repository.getCachedApplications() }
            if (cached.isNotEmpty()) {
                _uiState.update { it.copy(applications = cached, isStale = true) }
            }
            _uiState.update { it.copy(isLoading = cached.isEmpty()) }
            val fresh = repository.getAllApplications()
            if (fresh.isNotEmpty()) {
                lastRefreshTime = System.currentTimeMillis()
            }
            _uiState.update {
                it.copy(
                    applications = if (fresh.isNotEmpty()) fresh else it.applications,
                    isLoading = false,
                    isStale = fresh.isEmpty() && it.applications.isNotEmpty()
                )
            }
        }
    }

    fun loadApplicationDetail(id: String) {
        _uiState.update { it.copy(isLoadingDetail = true, selectedAppDetail = null) }
        viewModelScope.launch {
            val detail = repository.getFullDetail(id)
            _uiState.update { it.copy(selectedAppDetail = detail, isLoadingDetail = false) }
        }
    }

    fun resolveAuthorizedApplication(
        id: String,
        onResolved: (LoanApplicationModel?) -> Unit
    ) {
        viewModelScope.launch {
            val application = repository.getApplicationById(id)
            onResolved(application)
        }
    }

    fun setSelectedBorrowerForApp(borrower: BorrowerModel?) {
        _uiState.update { it.copy(selectedBorrowerForApp = borrower, errorMessage = null) }
    }

    fun setCustomerSearchQuery(value: String) {
        _uiState.update { it.copy(customerSearchQuery = value, errorMessage = null) }
    }

    fun searchExistingCustomers() {
        val query = _uiState.value.customerSearchQuery.trim()
        if (query.length < 3) {
            _uiState.update { it.copy(customerSearchResults = emptyList(), errorMessage = "Enter at least 3 characters") }
            return
        }
        _uiState.update { it.copy(isSearchingCustomers = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.searchExistingCustomers(query)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isSearchingCustomers = false, customerSearchResults = result.data.items)
                }
                is ApiResult.Error -> _uiState.update { it.copy(isSearchingCustomers = false, errorMessage = result.detail) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isSearchingCustomers = false, errorMessage = result.message) }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun selectExistingCustomer(customer: ExistingCustomerSearchItem) {
        _uiState.update { it.copy(isSearchingCustomers = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.getApplicationProfile(customer.id)) {
                is ApiResult.Success -> {
                    val profile = result.data.personal_profile
                    val borrower = BorrowerModel(
                        id = result.data.borrower.id,
                        org_id = sessionStore.load()?.orgId.orEmpty(),
                        loan_officer_id = "",
                        name = profile.applicant_name,
                        phone = profile.phone.orEmpty(),
                        bvn = profile.bvn.orEmpty(),
                        nin = profile.nin.orEmpty(),
                        status = if (customer.active) "ACTIVE" else "INACTIVE",
                        created_at = ""
                    )
                    _uiState.update {
                        it.copy(
                            isSearchingCustomers = false,
                            selectedBorrowerForApp = borrower,
                            selectedCustomerProfile = profile,
                            customerSearchResults = emptyList()
                        )
                    }
                }
                is ApiResult.Error -> _uiState.update { it.copy(isSearchingCustomers = false, errorMessage = result.detail) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isSearchingCustomers = false, errorMessage = result.message) }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun setCustomerType(value: String) {
        val normalized = if (value.equals("New Customer", ignoreCase = true)) "new" else "existing"
        _uiState.update {
            it.copy(
                customerType = normalized,
                selectedBorrowerForApp = if (normalized == "new") null else it.selectedBorrowerForApp,
                selectedCustomerProfile = if (normalized == "new") null else it.selectedCustomerProfile,
                customerSearchResults = if (normalized == "new") emptyList() else it.customerSearchResults,
                errorMessage = null
            )
        }
    }

    fun setLoanCategory(value: String) {
        val normalized = when (value.lowercase()) {
            "enterprise loan" -> "enterprise"
            "other option" -> "other"
            else -> value.lowercase()
        }
        _uiState.update { it.copy(loanCategory = normalized, errorMessage = null) }
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

    fun generateClientIntakeLink(onCompleted: (String) -> Unit = {}) {
        _uiState.update { it.copy(isGeneratingLink = true, errorMessage = null) }
        viewModelScope.launch {
            val response = repository.generateShareLink()
            if (response != null) {
                _uiState.update { it.copy(shareUrl = response.share_url, isGeneratingLink = false) }
                onCompleted(response.share_url)
            } else {
                _uiState.update { 
                    it.copy(
                        isGeneratingLink = false,
                        errorMessage = "Failed to generate intake link. Please check network."
                    )
                }
            }
        }
    }

    fun generateApplicationSigningLink(
        applicationId: String,
        guarantorSlot: Int? = null,
        onCompleted: (String) -> Unit
    ) {
        _uiState.update { it.copy(isGeneratingLink = true, errorMessage = null) }
        viewModelScope.launch {
            val result = if (guarantorSlot == null) {
                repository.generateClientSigningLink(applicationId)
            } else {
                repository.generateGuarantorSigningLink(applicationId, guarantorSlot)
            }
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isGeneratingLink = false, shareUrl = result.data.share_url) }
                    onCompleted(result.data.share_url)
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isGeneratingLink = false, errorMessage = result.detail)
                }
                is ApiResult.NetworkError -> _uiState.update {
                    it.copy(isGeneratingLink = false, errorMessage = result.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun generateOffer(applicationId: String, onCompleted: () -> Unit = {}) {
        _uiState.update { it.copy(isMutating = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.generateOffer(applicationId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isMutating = false) }
                    onCompleted()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isMutating = false, errorMessage = result.detail) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isMutating = false, errorMessage = result.message) }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun pullCreditBureau(applicationId: String) {
        _uiState.update { it.copy(isMutating = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.pullCreditBureau(applicationId)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isMutating = false, bureauReport = result.data)
                }
                is ApiResult.Error -> _uiState.update { it.copy(isMutating = false, errorMessage = result.detail) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isMutating = false, errorMessage = result.message) }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun recordDisbursement(
        applicationId: String,
        request: com.fieldcrm.android.data.api.DisbursementRequest,
        onCompleted: () -> Unit = {}
    ) {
        _uiState.update { it.copy(isMutating = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.recordDisbursement(applicationId, request)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isMutating = false) }
                    refreshApplications()
                    onCompleted()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isMutating = false, errorMessage = result.detail) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isMutating = false, errorMessage = result.message) }
                ApiResult.Loading -> Unit
            }
        }
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
                    org_id = sessionStore.load()?.orgId ?: "",
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
                org_id = sessionStore.load()?.orgId ?: "",
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

            val success = repository.createApplication(newApp, if (isNew) null else borrower.id)
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
                selectedCustomerProfile = null,
                customerSearchQuery = "",
                customerSearchResults = emptyList(),
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

    fun approveApplication(
        id: String,
        notes: String,
        kycAttested: Boolean,
        collateralAttested: Boolean,
        onComplete: () -> Unit = {}
    ) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val success = repository.approveApplication(id, notes, kycAttested, collateralAttested)
            if (success) loadApplications()
            _uiState.update { it.copy(isLoading = false) }
            if (success) onComplete()
        }
    }

    fun returnApplication(id: String, reason: String, corrections: List<String> = emptyList(), notes: String, onComplete: () -> Unit = {}) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val success = repository.returnApplication(id, reason, corrections, notes)
            if (success) loadApplications()
            _uiState.update { it.copy(isLoading = false) }
            if (success) onComplete()
        }
    }

    fun submitCreditReview(id: String, decision: String, notes: String, onComplete: () -> Unit = {}) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val success = repository.submitCreditReview(id, decision, notes)
            if (success) loadApplications()
            _uiState.update { it.copy(isLoading = false) }
            if (success) onComplete()
        }
    }

    fun advanceWorkflow(id: String, notes: String, onComplete: () -> Unit = {}) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val response = repository.advanceWorkflow(id, notes)
            if (response != null) loadApplications()
            _uiState.update { it.copy(isLoading = false) }
            if (response != null) onComplete()
        }
    }

    fun submitIntakeForm(
        updatedApp: LoanApplicationModel,
        updatedBorrower: BorrowerModel,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val ok = repository.submitIntakeToServer(
                id = updatedApp.id,
                amount = updatedApp.amount ?: 0.0,
                tenorMonths = updatedApp.tenor_months ?: 0,
                loanType = updatedApp.loan_type,
                purpose = updatedApp.purpose ?: ""
            )
            if (!ok) {
                _uiState.update { it.copy(errorMessage = "Could not save the intake. Check your connection and try again.") }
                return@launch
            }
            val advanced = repository.advanceWorkflow(updatedApp.id, "Relationship Officer submitted completed intake")
            if (advanced == null) {
                _uiState.update { it.copy(errorMessage = "The application is not ready to submit. Complete the server-required gates first.") }
                return@launch
            }
            refreshApplications()
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
            // Optimistic local advance to branch_approval
            val advanced = _uiState.value.applications.find { it.id == id }
                ?.copy(stage = "branch_approval")
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
