package com.fieldcrm.android.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.Immutable
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.fieldcrm.android.data.repository.BorrowerRepository
import com.fieldcrm.shared.api.FieldCRMClient
import com.fieldcrm.shared.db.AppDatabase
import com.fieldcrm.shared.model.BorrowerModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

@Immutable
data class BorrowerUiState(
    val borrowers: List<BorrowerModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val newBorrowerName: String = "",
    val newBorrowerPhone: String = "",
    val newBorrowerBvn: String = "",
    val newBorrowerNin: String = ""
)

class BorrowerViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(BorrowerUiState())
    val uiState: StateFlow<BorrowerUiState> = _uiState.asStateFlow()

    private val database: AppDatabase
    private val client: FieldCRMClient
    private val repository: BorrowerRepository

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
        repository = BorrowerRepository(database, client)

        loadBorrowers()
    }

    private fun loadBorrowers() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val list = repository.getAllBorrowers()
            _uiState.update { it.copy(borrowers = list, isLoading = false) }
        }
    }

    fun setNewBorrowerName(value: String) {
        _uiState.update { it.copy(newBorrowerName = value, errorMessage = null) }
    }

    fun setNewBorrowerPhone(value: String) {
        _uiState.update { it.copy(newBorrowerPhone = value, errorMessage = null) }
    }

    fun setNewBorrowerBvn(value: String) {
        _uiState.update { it.copy(newBorrowerBvn = value, errorMessage = null) }
    }

    fun setNewBorrowerNin(value: String) {
        _uiState.update { it.copy(newBorrowerNin = value, errorMessage = null) }
    }

    fun createBorrower(onSuccess: (BorrowerModel) -> Unit) {
        val state = _uiState.value
        if (state.newBorrowerName.isBlank() || state.newBorrowerPhone.isBlank() ||
            state.newBorrowerBvn.isBlank() || state.newBorrowerNin.isBlank()
        ) {
            _uiState.update { it.copy(errorMessage = "Please fill in all fields") }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        val newBorrower = BorrowerModel(
            id = UUID.randomUUID().toString(),
            org_id = "org_1",
            loan_officer_id = "lo_1",
            name = state.newBorrowerName,
            phone = state.newBorrowerPhone,
            bvn = state.newBorrowerBvn,
            nin = state.newBorrowerNin,
            status = "ACTIVE",
            created_at = System.currentTimeMillis().toString()
        )

        viewModelScope.launch {
            val success = repository.createBorrower(newBorrower)
            if (success) {
                _uiState.update { it.copy(borrowers = it.borrowers + newBorrower) }
                clearNewBorrowerFields()
                _uiState.update { it.copy(isLoading = false) }
                onSuccess(newBorrower)
            } else {
                _uiState.update { it.copy(errorMessage = "Network error. Queued for offline sync.", isLoading = false) }
            }
        }
    }

    private fun clearNewBorrowerFields() {
        _uiState.update {
            it.copy(
                newBorrowerName = "",
                newBorrowerPhone = "",
                newBorrowerBvn = "",
                newBorrowerNin = "",
                errorMessage = null
            )
        }
    }

    fun refreshBorrowers() {
        loadBorrowers()
    }
}
