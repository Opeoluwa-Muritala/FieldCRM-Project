package com.fieldcrm.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import com.fieldcrm.shared.model.BorrowerModel
import java.util.UUID

class BorrowerViewModel : ViewModel() {
    private val _borrowers = mutableStateOf<List<BorrowerModel>>(emptyList())
    val borrowers: State<List<BorrowerModel>> = _borrowers

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _newBorrowerName = mutableStateOf("")
    val newBorrowerName: State<String> = _newBorrowerName

    private val _newBorrowerPhone = mutableStateOf("")
    val newBorrowerPhone: State<String> = _newBorrowerPhone

    private val _newBorrowerBvn = mutableStateOf("")
    val newBorrowerBvn: State<String> = _newBorrowerBvn

    private val _newBorrowerNin = mutableStateOf("")
    val newBorrowerNin: State<String> = _newBorrowerNin

    init {
        loadBorrowers()
    }

    private fun loadBorrowers() {
        _isLoading.value = true
        // Mock data - replace with actual API call
        _borrowers.value = listOf(
            BorrowerModel(
                id = UUID.randomUUID().toString(),
                org_id = "org_1",
                loan_officer_id = "lo_1",
                name = "John Doe",
                phone = "+2348012345678",
                bvn = "12345678901",
                nin = "11223344556",
                status = "ACTIVE",
                created_at = "2024-01-15"
            ),
            BorrowerModel(
                id = UUID.randomUUID().toString(),
                org_id = "org_1",
                loan_officer_id = "lo_1",
                name = "Jane Smith",
                phone = "+2348087654321",
                bvn = "98765432109",
                nin = "99887766554",
                status = "ACTIVE",
                created_at = "2024-01-16"
            )
        )
        _isLoading.value = false
    }

    fun setNewBorrowerName(value: String) {
        _newBorrowerName.value = value
    }

    fun setNewBorrowerPhone(value: String) {
        _newBorrowerPhone.value = value
    }

    fun setNewBorrowerBvn(value: String) {
        _newBorrowerBvn.value = value
    }

    fun setNewBorrowerNin(value: String) {
        _newBorrowerNin.value = value
    }

    fun createBorrower(onSuccess: (BorrowerModel) -> Unit) {
        if (_newBorrowerName.value.isEmpty() || _newBorrowerPhone.value.isEmpty() ||
            _newBorrowerBvn.value.isEmpty() || _newBorrowerNin.value.isEmpty()
        ) {
            _errorMessage.value = "Please fill in all fields"
            return
        }

        _isLoading.value = true
        val newBorrower = BorrowerModel(
            id = UUID.randomUUID().toString(),
            org_id = "org_1",
            loan_officer_id = "lo_1",
            name = _newBorrowerName.value,
            phone = _newBorrowerPhone.value,
            bvn = _newBorrowerBvn.value,
            nin = _newBorrowerNin.value,
            status = "ACTIVE",
            created_at = System.currentTimeMillis().toString()
        )

        _borrowers.value = _borrowers.value + newBorrower
        clearNewBorrowerFields()
        _isLoading.value = false
        onSuccess(newBorrower)
    }

    private fun clearNewBorrowerFields() {
        _newBorrowerName.value = ""
        _newBorrowerPhone.value = ""
        _newBorrowerBvn.value = ""
        _newBorrowerNin.value = ""
        _errorMessage.value = null
    }

    fun refreshBorrowers() {
        loadBorrowers()
    }
}
