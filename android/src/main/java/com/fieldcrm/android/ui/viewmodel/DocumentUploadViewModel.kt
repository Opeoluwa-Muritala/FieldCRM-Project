package com.fieldcrm.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldcrm.android.data.api.MobileApiService
import com.fieldcrm.android.data.api.OcrExtractedField
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
    val extractedFields: List<OcrExtractedField> = emptyList(),
    val isOcrProcessing: Boolean = false,
    val isRefreshingOcr: Boolean = false,
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
            extractedFields == other.extractedFields &&
            isOcrProcessing == other.isOcrProcessing &&
            isRefreshingOcr == other.isRefreshingOcr &&
            uploadState == other.uploadState &&
            ocrSubmitState == other.ocrSubmitState
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + extractedName.hashCode()
        result = 31 * result + extractedBvn.hashCode()
        result = 31 * result + ocrConfidence.hashCode()
        result = 31 * result + extractedFields.hashCode()
        result = 31 * result + isOcrProcessing.hashCode()
        result = 31 * result + isRefreshingOcr.hashCode()
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
            if (result != null) refreshOcrFields(applicationId)
        }
    }

    fun refreshOcrFields(applicationId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingOcr = true) }
            val response = apiService.getOcrFields(applicationId)
            _uiState.update { current ->
                if (response == null) {
                    current.copy(isRefreshingOcr = false)
                } else {
                    val applicantName = response.items.firstOrNull {
                        it.field_name == "applicant_name" || it.field_name == "full_name"
                    }
                    val bvn = response.items.firstOrNull { it.field_name == "bvn" }
                    current.copy(
                        extractedFields = response.items,
                        isOcrProcessing = response.processing,
                        isRefreshingOcr = false,
                        extractedName = current.extractedName.ifBlank {
                            applicantName?.final_value ?: applicantName?.ocr_value.orEmpty()
                        },
                        extractedBvn = current.extractedBvn.ifBlank {
                            bvn?.final_value ?: bvn?.ocr_value.orEmpty()
                        }
                    )
                }
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
