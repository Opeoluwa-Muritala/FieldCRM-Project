package com.fieldcrm.android.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class TokenResponse(
    val access_token: String,
    val token_type: String
)

/** Full loan application response matching loan_applications table columns. */
@Serializable
data class LoanApplicationResponse(
    val id: String,
    val org_id: String,
    val ref_no: String = "",
    val customer_type: String = "new",
    val loan_type: String = "enterprise",
    val stage: String = "intake",
    val applicant_name: String = "",
    val bvn: String? = null,
    val phone: String? = null,
    val amount: Double? = null,
    val tenor_months: Int? = null,
    val purpose: String? = null,
    val repayment_mode: String? = null,
    val created_by: String = "",
    val current_owner_id: String? = null,
    val credit_officer_id: String? = null,
    val branch_manager_id: String? = null,
    val return_reason: String? = null,
    val returned_at: String? = null,
    val approved_by: String? = null,
    val approved_at: String? = null,
    val disbursed_at: String? = null,
    val interest_rate: Double? = null,
    val repayment_frequency: String? = null,
    val schedule_method: String? = null,
    val classification: String? = "current",
    val days_past_due: Int = 0,
    val crm_notes: String? = null,
    val crm_reviewed_by: String? = null,
    val crm_reviewed_at: String? = null,
    val executive_approved_by: String? = null,
    val executive_approved_at: String? = null,
    val disbursed_amount: Double? = null,
    val disbursement_method: String? = null,
    val disbursement_ref: String? = null,
    val sector: String? = null,
    val created_at: String = "",
    val updated_at: String? = null
)

/** Guarantor response matching guarantors table columns. */
@Serializable
data class GuarantorResponse(
    val id: String,
    val loan_id: String,
    val org_id: String,
    val slot: Int,
    val full_name: String? = null,
    val relationship_to_client: String? = null,
    val bvn: String? = null,
    val phone: String? = null,
    val home_address: String? = null,
    val employment_type: String? = null,
    val monthly_salary: Double? = null,
    val max_guarantee_amount: Double? = null,
    val max_guarantee_amount_words: String? = null,
    val bank_name: String? = null,
    val account_number: String? = null,
    val cheque_number: String? = null,
    val form_stage: String = "draft",
    val signature_detected: Boolean = false,
    val witness_signature_detected: Boolean = false,
    val created_at: String = "",
    val updated_at: String? = null
)

/** Document response matching documents table columns. */
@Serializable
data class DocumentResponse(
    val id: String,
    val loan_id: String,
    val org_id: String,
    val guarantor_id: String? = null,
    val doc_type: String,
    val form_code: String? = null,
    val original_name: String,
    val stored_path: String,
    val mime_type: String,
    val size_bytes: Int,
    val quality_status: String = "pending",
    val verified: Boolean = false,
    val verified_by: String? = null,
    val verified_at: String? = null,
    val uploaded_by: String,
    val uploaded_at: String,
    val ocr_status: String = "pending",
    val cloud_public_id: String? = null,
    val cloud_preview_url: String? = null
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
data class CommitteeVoteRequest(
    val recommendation: String,
    val notes: String = ""
)

@Serializable
data class CommitteeCompleteRequest(
    val recommendation: String
)

@Serializable
data class EdApproveRequest(
    val action: String
)

@Serializable
data class MdApproveRequest(
    val action: String,
    val notes: String = ""
)

@Serializable
data class BoardReferralRequest(
    val board_member_email: String,
    val board_member_name: String,
    val notes: String = ""
)

@Serializable
data class CommitteeVoteItem(
    val member_name: String,
    val recommendation: String,
    val notes: String? = null,
    val voted_at: String
)

@Serializable
data class CommitteeVotesFullResponse(
    val votes: List<CommitteeVoteItem> = emptyList(),
    val loan_amount: Double? = null,
    val committee_recommendation: String? = null
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
