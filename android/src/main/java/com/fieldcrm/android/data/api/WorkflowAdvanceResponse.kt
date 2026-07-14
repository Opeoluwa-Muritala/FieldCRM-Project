package com.fieldcrm.android.data.api

import kotlinx.serialization.Serializable

@Serializable
data class WorkflowAdvanceResponse(
    val stage: String,
    val notified_role: String
)
