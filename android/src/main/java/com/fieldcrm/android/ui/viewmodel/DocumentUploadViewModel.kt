package com.fieldcrm.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldcrm.android.data.api.MobileApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class UploadState {
    object Idle : UploadState()
    object InFlight : UploadState()
    object Done : UploadState()
    data class Failed(val message: String) : UploadState()
}

data class DocumentUploadUiState(
    val fileName: String = "",
    val fileBytes: ByteArray? = null,
    val category: String = "id",
    val extractedName: String = "",
    val extractedBvn: String = "",
    val ocrConfidence: Float = 0f,
    val uploadState: UploadState = UploadState.Idle,
    val ocrSubmitState: UploadState = UploadState.Idle
) {
    // ByteArray breaks structural equality — exclude from equals/hashCode
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DocumentUploadUiState) return false
        return fileName == other.fileName &&
            category == other.category &&
            extractedName == other.extractedName &&
            extractedBvn == other.extractedBvn &&
            ocrConfidence == other.ocrConfidence &&
            uploadState == other.uploadState &&
            ocrSubmitState == other.ocrSubmitState
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + extractedName.hashCode()
        result = 31 * result + extractedBvn.hashCode()
        result = 31 * result + ocrConfidence.hashCode()
        result = 31 * result + uploadState.hashCode()
        result = 31 * result + ocrSubmitState.hashCode()
        return result
    }
}

class DocumentUploadViewModel(private val apiService: MobileApiService) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentUploadUiState())
    val uiState: StateFlow<DocumentUploadUiState> = _uiState.asStateFlow()

    fun setFile(bytes: ByteArray, name: String) {
        _uiState.update { it.copy(fileName = name, fileBytes = bytes, uploadState = UploadState.Idle) }
    }

    fun setCategory(category: String) {
        _uiState.update { it.copy(category = category) }
    }

    fun setOcrResult(name: String, bvn: String, confidence: Float, fileName: String = "") {
        _uiState.update {
            it.copy(
                extractedName = name,
                extractedBvn = bvn,
                ocrConfidence = confidence,
                fileName = fileName.ifBlank { it.fileName }
            )
        }
    }

    fun setExtractedName(name: String) = _uiState.update { it.copy(extractedName = name) }
    fun setExtractedBvn(bvn: String) = _uiState.update { it.copy(extractedBvn = bvn) }

    fun uploadDocument(applicationId: String) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(uploadState = UploadState.InFlight) }
            val result = apiService.uploadDocument(
                id = applicationId,
                category = state.category,
                fileBytes = state.fileBytes,
                fileName = state.fileName.ifBlank { "document" }
            )
            _uiState.update {
                it.copy(uploadState = if (result != null) UploadState.Done else UploadState.Failed("Upload failed — will retry on next sync"))
            }
        }
    }

    fun submitOcrReview(applicationId: String, onComplete: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(ocrSubmitState = UploadState.InFlight) }
            val corrections = mapOf(
                "full_name" to state.extractedName,
                "bvn" to state.extractedBvn
            )
            val result = apiService.submitOcrReview(applicationId, corrections)
            _uiState.update {
                it.copy(ocrSubmitState = if (result != null) UploadState.Done else UploadState.Failed("Review submission failed"))
            }
            onComplete()
        }
    }

    fun reset() {
        _uiState.value = DocumentUploadUiState()
    }
}
