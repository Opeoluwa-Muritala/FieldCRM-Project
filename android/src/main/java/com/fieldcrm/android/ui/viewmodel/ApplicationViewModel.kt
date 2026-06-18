package com.fieldcrm.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel
import java.util.UUID

class ApplicationViewModel : ViewModel() {
    private val _applications = mutableStateOf<List<LoanApplicationModel>>(emptyList())
    val applications: State<List<LoanApplicationModel>> = _applications

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _selectedBorrowerForApp = mutableStateOf<BorrowerModel?>(null)
    val selectedBorrowerForApp: State<BorrowerModel?> = _selectedBorrowerForApp

    private val _newAppAmount = mutableStateOf("")
    val newAppAmount: State<String> = _newAppAmount

    private val _newAppTenure = mutableStateOf("")
    val newAppTenure: State<String> = _newAppTenure

    private val _newAppInterestRate = mutableStateOf("18.5")
    val newAppInterestRate: State<String> = _newAppInterestRate

    private val _newAppProductType = mutableStateOf("PERSONAL_LOAN")
    val newAppProductType: State<String> = _newAppProductType

    init {
        loadApplications()
    }

    private fun loadApplications() {
        _isLoading.value = true
        // Mock data - replace with actual API call
        _applications.value = listOf(
            LoanApplicationModel(
                id = UUID.randomUUID().toString(),
                org_id = "org_1",
                borrower_id = "borrower_1",
                current_stage = 1,
                current_owner_id = "lo_1",
                status = "PENDING_CREDIT_REVIEW",
                amount = 500000.0,
                tenure = 12,
                product_type = "PERSONAL_LOAN",
                interest_rate = 18.5,
                repayment_frequency = "MONTHLY",
                created_at = "2024-01-15"
            )
        )
        _isLoading.value = false
    }

    fun setSelectedBorrowerForApp(borrower: BorrowerModel?) {
        _selectedBorrowerForApp.value = borrower
    }

    fun setNewAppAmount(value: String) {
        _newAppAmount.value = value
    }

    fun setNewAppTenure(value: String) {
        _newAppTenure.value = value
    }

    fun setNewAppInterestRate(value: String) {
        _newAppInterestRate.value = value
    }

    fun setNewAppProductType(value: String) {
        _newAppProductType.value = value
    }

    fun createApplication(onSuccess: (LoanApplicationModel) -> Unit) {
        val borrower = _selectedBorrowerForApp.value
        if (borrower == null || _newAppAmount.value.isEmpty() || _newAppTenure.value.isEmpty()) {
            _errorMessage.value = "Please fill in all required fields"
            return
        }

        _isLoading.value = true
        val newApp = LoanApplicationModel(
            id = UUID.randomUUID().toString(),
            org_id = "org_1",
            borrower_id = borrower.id,
            current_stage = 1,
            current_owner_id = borrower.loan_officer_id,
            status = "PENDING_CREDIT_REVIEW",
            amount = _newAppAmount.value.toDoubleOrNull() ?: 0.0,
            tenure = _newAppTenure.value.toIntOrNull() ?: 0,
            product_type = _newAppProductType.value,
            interest_rate = _newAppInterestRate.value.toDoubleOrNull() ?: 18.5,
            repayment_frequency = "MONTHLY",
            created_at = System.currentTimeMillis().toString()
        )

        _applications.value = _applications.value + newApp
        clearNewAppFields()
        _isLoading.value = false
        onSuccess(newApp)
    }

    private fun clearNewAppFields() {
        _selectedBorrowerForApp.value = null
        _newAppAmount.value = ""
        _newAppTenure.value = ""
        _newAppInterestRate.value = "18.5"
        _newAppProductType.value = "PERSONAL_LOAN"
        _errorMessage.value = null
    }

    fun refreshApplications() {
        loadApplications()
    }
}
