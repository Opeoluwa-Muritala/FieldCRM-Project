package com.fieldcrm.android.data.api

import kotlinx.serialization.Serializable

@Serializable
data class WorkflowAdvanceRequest(
    val notes: String = ""
)
