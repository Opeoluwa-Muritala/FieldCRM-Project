package com.fieldcrm.android.data.api

import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse(
    val access_token: String,
    val token_type: String
)

@Serializable
data class MobileUser(
    val id: String,
    val org_id: String,
    val full_name: String,
    val email: String,
    val role: String,
    val display_role: String
)

@Serializable
data class CreateAppRequest(
    val customer_type: String,
    val loan_type: String,
    val applicant_name: String
)

@Serializable
data class SaveStepRequest(
    val data: Map<String, String>
)

@Serializable
data class OcrReviewRequest(
    val action: String,
    val corrections: Map<String, String>
)

@Serializable
data class VisitationReportRequest(
    val met_with: String?,
    val premises_description: String?,
    val direction_from_branch: String?
)

@Serializable
data class VisitationSignoffRequest(
    val decision: String,
    val notes: String
)

@Serializable
data class CreditReviewRequest(
    val recommendation_decision: String,
    val recommendation_notes: String
)

@Serializable
data class ReturnApplicationRequest(
    val reason_category: String,
    val notes: String
)
