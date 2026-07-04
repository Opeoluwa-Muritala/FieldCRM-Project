package com.fieldcrm.android.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

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
    val data: Map<String, JsonElement>
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
    val corrections: List<String> = emptyList(),
    val notes: String
)

@Serializable
data class CrmReviewRequest(
    val decision: String, // "advance" or "return"
    val notes: String = ""
)

@Serializable
data class RecordPaymentRequest(
    val amount_paid: Double,
    val channel: String = "cash",
    val bank_ref: String? = null,
    val payment_date: String? = null
)

@Serializable
data class RepaymentScheduleRow(
    val installment_no: Int,
    val due_date: String,
    val principal_due: Double,
    val interest_due: Double,
    val total_due: Double
)

@Serializable
data class PaymentRecord(
    val payment_date: String,
    val amount_paid: Double,
    val channel: String,
    val bank_ref: String? = null
)

@Serializable
data class RepaymentScheduleResponse(
    val schedule: List<RepaymentScheduleRow> = emptyList(),
    val payments: List<PaymentRecord> = emptyList(),
    val total_due: Double = 0.0,
    val total_paid: Double = 0.0,
    val outstanding: Double = 0.0
)

@Serializable
data class ParSummary(
    val total_loans: Int = 0,
    val total_portfolio: Double = 0.0,
    val par1_count: Int = 0,
    val par1_amount: Double = 0.0,
    val par1_pct: Double = 0.0,
    val par30_count: Int = 0,
    val par30_amount: Double = 0.0,
    val par30_pct: Double = 0.0,
    val par90_count: Int = 0,
    val par90_amount: Double = 0.0,
    val par90_pct: Double = 0.0,
    val olem_count: Int = 0,
    val substandard_count: Int = 0,
    val doubtful_count: Int = 0,
    val lost_count: Int = 0
)
