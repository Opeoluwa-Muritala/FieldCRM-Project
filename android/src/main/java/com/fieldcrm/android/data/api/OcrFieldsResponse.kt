package com.fieldcrm.android.data.api

import kotlinx.serialization.Serializable

@Serializable
data class OcrFieldsResponse(
    val items: List<OcrExtractedField> = emptyList(),
    val processing: Boolean = false
)
