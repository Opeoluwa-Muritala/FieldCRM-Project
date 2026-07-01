package com.fieldcrm.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldcrm.android.data.repository.ApplicationRepository
import com.fieldcrm.shared.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SyncItemStatus { PENDING, FAILED }

data class SyncItem(
    val id: String,
    val label: String,
    val action: String,
    val status: SyncItemStatus,
    val attempts: Long,
    val errorMsg: String? = null
)

data class SyncUiState(
    val items: List<SyncItem> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val lastResult: Boolean? = null   // null = not yet synced this session
)

class SyncViewModel(
    private val database: AppDatabase,
    private val applicationRepository: ApplicationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    val pendingCount: Int get() = _uiState.value.items.size

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val items = readQueueFromDb()
            _uiState.update { it.copy(items = items, isLoading = false) }
        }
    }

    fun syncNow(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, lastResult = null) }
            val success = applicationRepository.syncWithServer()
            val items = readQueueFromDb()
            _uiState.update { it.copy(isSyncing = false, items = items, lastResult = success) }
            onComplete?.invoke(success)
        }
    }

    private suspend fun readQueueFromDb(): List<SyncItem> = withContext(Dispatchers.IO) {
        database.appDatabaseQueries.selectQueuedItems().executeAsList().map { row ->
            val label = when (row.action) {
                "CREATE_APPLICATION"  -> "Loan application"
                "UPDATE_APPLICATION"  -> "Application update"
                "RECORD_REPAYMENT"    -> "Repayment entry"
                "VISITATION_REPORT"   -> "Visitation report"
                else -> row.action.replace("_", " ")
                    .lowercase()
                    .replaceFirstChar { it.uppercaseChar() }
            }
            val refSuffix = row.entity_id.take(8).uppercase()
            val status = if (row.attempts >= 3) SyncItemStatus.FAILED else SyncItemStatus.PENDING
            val error = if (status == SyncItemStatus.FAILED)
                "Failed after ${row.attempts} attempt${if (row.attempts == 1L) "" else "s"} — will retry automatically"
            else null
            SyncItem(
                id = row.id,
                label = "$label — $refSuffix",
                action = row.action,
                status = status,
                attempts = row.attempts,
                errorMsg = error
            )
        }
    }
}
