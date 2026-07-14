package com.fieldcrm.android.data.api

import kotlinx.serialization.Serializable

@Serializable
data class OcrExtractedField(
    val field_name: String,
    val ocr_value: String? = null,
    val final_value: String? = null,
    val confidence: Float? = null,
    val is_critical: Boolean = false,
    val verified: Boolean = false,
    val page_number: Int? = null,
    val form_type: String = "",
    val ocr_status: String = "pending",
    val doc_type: String = ""
)
