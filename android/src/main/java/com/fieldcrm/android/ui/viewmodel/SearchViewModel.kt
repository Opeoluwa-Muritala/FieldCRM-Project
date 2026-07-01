package com.fieldcrm.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldcrm.android.data.api.SearchResponse
import com.fieldcrm.android.data.repository.SearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val results: SearchResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val query: String = ""
)

class SearchViewModel(private val repo: SearchRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        if (query.trim().length < 2) {
            _uiState.value = _uiState.value.copy(results = null, isLoading = false)
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val results = repo.search(query.trim())
            _uiState.value = _uiState.value.copy(results = results, isLoading = false)
        }
    }

    fun clear() {
        searchJob?.cancel()
        _uiState.value = SearchUiState()
    }
}
