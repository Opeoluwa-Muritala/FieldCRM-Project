package com.fieldcrm.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldcrm.android.core.network.ApiResult
import com.fieldcrm.android.data.api.MobileApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray

data class MccRecommendation(
    val memberName: String,
    val recommendation: String,
    val recommendedAmount: Double?,
    val notes: String,
)

data class MccEvidenceUiState(
    val applicationId: String? = null,
    val isLoading: Boolean = false,
    val recommendations: List<MccRecommendation> = emptyList(),
    val errorMessage: String? = null,
)

class MccEvidenceViewModel(private val api: MobileApiService) : ViewModel() {
    private val _uiState = MutableStateFlow(MccEvidenceUiState())
    val uiState: StateFlow<MccEvidenceUiState> = _uiState.asStateFlow()

    fun load(applicationId: String) {
        if (_uiState.value.applicationId == applicationId &&
            (_uiState.value.isLoading || _uiState.value.errorMessage == null)
        ) return

        _uiState.value = MccEvidenceUiState(applicationId = applicationId, isLoading = true)
        viewModelScope.launch {
            when (val result = api.getMccApplication(applicationId)) {
                is ApiResult.Success -> {
                    val root = result.data as? JsonObject
                    val recommendations = root?.get("votes")?.jsonArray.orEmpty().mapNotNull { element ->
                        val vote = element as? JsonObject ?: return@mapNotNull null
                        MccRecommendation(
                            memberName = vote.string("member_name") ?: "Name not available",
                            recommendation = vote.string("recommendation") ?: "Recommendation not available",
                            recommendedAmount = (vote["recommended_amount"] as? JsonPrimitive)?.doubleOrNull,
                            notes = vote.string("notes").orEmpty(),
                        )
                    }
                    _uiState.update { it.copy(isLoading = false, recommendations = recommendations) }
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.detail.ifBlank { "Unable to load MCC recommendations" })
                }
                is ApiResult.NetworkError -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message.ifBlank { "Unable to load MCC recommendations" })
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    private fun JsonObject.string(key: String): String? =
        (get(key) as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
}
