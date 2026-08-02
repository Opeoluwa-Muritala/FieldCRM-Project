package com.fieldcrm.android.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldcrm.android.data.api.SearchResponse
import com.fieldcrm.android.data.repository.SearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val results: SearchResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val history: List<String> = emptyList()
)

class SearchViewModel(
    private val repo: SearchRepository,
    private val context: Context
) : ViewModel() {

    private var roleScope: String = "anonymous"
    private val prefs get() = context.getSharedPreferences("search_history_$roleScope", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadSearchHistory()
    }

    private fun loadSearchHistory() {
        val historyStr = prefs.getString("history", "") ?: ""
        val list = if (historyStr.isBlank()) emptyList() else historyStr.split("\n").filter { it.isNotBlank() }
        _uiState.update { it.copy(history = list) }
    }

    fun setRoleScope(role: String) {
        if (roleScope == role) return
        roleScope = role
        _uiState.value = SearchUiState()
        loadSearchHistory()
    }

    fun clearHistory() {
        prefs.edit().clear().apply()
        _uiState.value = SearchUiState()
    }

    private fun saveSearchHistory(query: String) {
        val current = _uiState.value.history.toMutableList()
        current.remove(query)
        current.add(0, query)
        val limited = current.take(5)
        prefs.edit().putString("history", limited.joinToString("\n")).apply()
        _uiState.update { it.copy(history = limited) }
    }

    fun search(query: String) {
        _uiState.update { it.copy(query = query) }
        if (query.trim().length < 2) {
            _uiState.update { it.copy(results = null, isLoading = false) }
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.update { it.copy(isLoading = true, error = null) }
            val results = repo.search(query.trim())
            _uiState.update { it.copy(results = results, isLoading = false) }
            if (results != null) {
                saveSearchHistory(query.trim())
            }
        }
    }

    fun clear() {
        searchJob?.cancel()
        val currentHistory = _uiState.value.history
        _uiState.value = SearchUiState(history = currentHistory)
    }
}
