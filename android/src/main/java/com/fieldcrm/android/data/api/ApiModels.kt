package com.fieldcrm.android.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class ExistingCustomerSearchItem(
    val id: String,
    val legal_name: String,
    val customer_reference: String? = null,
    val masked_bvn: String? = null,
    val masked_nin: String? = null,
    val masked_phone: String? = null,
    val branch: String? = null,
    val relationship_owner: String? = null,
    val active: Boolean = true
)

@Serializable
data class ExistingCustomerSearchResponse(
    val items: List<ExistingCustomerSearchItem> = emptyList(),
    val query: String
)

@Serializable
data class ApplicationProfileBorrower(val id: String, val legal_name: String)

@Serializable
data class PersonalProfileSnapshot(
    val applicant_name: String,
    val full_name: String? = null,
    val phone: String? = null,
    val alternative_phone: String? = null,
    val email: String? = null,
    val date_of_birth: String? = null,
    val dob: String? = null,
    val gender: String? = null,
    val marital_status: String? = null,
    val bvn: String? = null,
    val nin: String? = null,
    val id_type: String? = null,
    val id_number: String? = null,
    val id_expiry: String? = null,
    val residential_address: String? = null,
    val home_address: String? = null,
    val state: String? = null,
    val state_of_origin: String? = null,
    val lga: String? = null,
    val locality: String? = null,
    val landmark: String? = null,
    val photo_url: String? = null,
    val customer_reference: String? = null,
    val account_reference: String? = null,
    val employment_status: String? = null,
    val employment_type: String? = null,
    val industry: String? = null,
    val years_employed: String? = null,
    val employer_name: String? = null,
    val monthly_salary: String? = null,
    val employer_address: String? = null,
    val business_type: String? = null,
    val years_in_business: String? = null,
    val monthly_sales: String? = null,
    val monthly_turnover: String? = null,
    val business_address: String? = null,
    val pnl_period_label: String? = null,
    val pnl_revenue: String? = null,
    val pnl_expenses: String? = null,
    val account_name: String? = null,
    val account_number: String? = null,
    val bank_name: String? = null,
    val sort_code: String? = null,
    val spouse_name: String? = null,
    val spouse_phone: String? = null,
    val spouse_children: String? = null,
    val spouse_dependants: String? = null,
    val spouse_business_address: String? = null
)

@Serializable
data class ApplicationProfileResponse(
    val borrower: ApplicationProfileBorrower,
    val personal_profile: PersonalProfileSnapshot
)

/** Reusable customer facts saved into a loan intake. Loan-specific decisions stay elsewhere. */
data class ProfileIntakeFields(
    val fullName: String,
    val phone: String,
    val alternativePhone: String,
    val email: String,
    val gender: String,
    val bvn: String,
    val maritalStatus: String,
    val dateOfBirth: String,
    val idType: String,
    val idNumber: String,
    val idExpiry: String,
    val residentialAddress: String,
    val stateOfOrigin: String,
    val lga: String,
    val landmark: String,
    val photoUrl: String,
    val customerReference: String,
    val accountReference: String,
    val spouseName: String,
    val spousePhone: String,
    val spouseChildren: String,
    val spouseDependants: String,
    val spouseBusinessAddress: String,
    val employmentType: String,
    val industry: String,
    val yearsEmployed: String,
    val employerName: String,
    val monthlySalary: String,
    val employerAddress: String,
    val businessType: String,
    val yearsInBusiness: String,
    val monthlySales: String,
    val monthlyTurnover: String,
    val businessAddress: String,
    val pnlPeriodLabel: String,
    val pnlRevenue: String,
    val pnlExpenses: String,
    val accountName: String,
    val accountNumber: String,
    val bankName: String,
    val sortCode: String
)

@Serializable
data class DirectUploadAuthorizationRequest(
    val filename: String,
    val mime_type: String,
    val size_bytes: Int,
    val doc_type: String,
    val form_code: String? = null
)

@Serializable
data class DirectUploadAuthorization(
    val intent_id: String,
    val upload_url: String,
    val fields: Map<String, String>,
    val expires_at: String,
    val fallback_available: Boolean = true
)

@Serializable
data class DirectUploadAuthorizationEnvelope(val authorization: DirectUploadAuthorization)

@Serializable
data class CloudinaryUploadResponse(
    val public_id: String,
    val version: Long,
    val signature: String
)

@Serializable
data class DirectUploadFinalizeRequest(
    val intent_id: String,
    val public_id: String,
    val version: Long,
    val signature: String
)

@Serializable
data class TokenResponse(
    val access_token: String,
    val token_type: String,
    val refresh_token: String? = null,
    val access_expires_in: Int = 600,
    val session_expires_at: String? = null
)

@Serializable data class RefreshTokenRequest(val refresh_token: String)
@Serializable data class LogoutTokenRequest(val refresh_token: String? = null)

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
    val applicant_name: String,
    val borrower_id: String? = null,
    val client_request_id: String? = null
)

@Serializable
data class SigningLinkResponse(val share_url: String, val expires_at: String)

@Serializable
data class CreditChecklistItem(
    val item_key: String,
    val item_label: String = "",
    val is_checked: Boolean,
    val checked_at: String? = null
)

@Serializable
data class CreditChecklistResponse(
    val application_id: String,
    val context: String,
    val items: List<CreditChecklistItem> = emptyList()
)

@Serializable
data class CreditChecklistUpdateRequest(
    val item_key: String,
    val item_label: String,
    val is_checked: Boolean,
    val context: String = "credit"
)

@Serializable
data class ChangePasswordRequest(
    val current_password: String,
    val new_password: String,
    val confirm_password: String
)

@Serializable
data class ChangePasswordResponse(val changed: Boolean)

@Serializable
data class OfferReadinessResponse(
    val ready: Boolean,
    val stage: String,
    val offer: JsonElement? = null
)

@Serializable
data class DisbursementRequest(
    val disbursed_amount: Double,
    val disbursement_method: String,
    val disbursed_bank_ref: String? = null,
    val payment_date: String,
    val interest_rate: Double,
    val repayment_frequency: String,
    val schedule_method: String = "flat_rate"
)

@Serializable
data class SystemActivityItem(
    val id: String,
    val loan_id: String? = null,
    val event_type: String,
    val from_stage: String? = null,
    val to_stage: String? = null,
    val triggered_by: String? = null,
    val triggered_role: String? = null,
    val actor_name: String? = null,
    val notes: String? = null,
    val created_at: String
)

@Serializable
data class SystemActivityResponse(
    val items: List<SystemActivityItem> = emptyList(),
    val page: Int,
    val size: Int,
    val total: Int
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

@Serializable
data class ApplicationDetailResponse(
    val readiness: Map<String, JsonElement>? = null,
    val documents: List<Map<String, JsonElement>>? = null,
    val intake: Map<String, JsonElement>? = null,
    val visitation: Map<String, JsonElement>? = null,
    val workflow_events: List<Map<String, JsonElement>>? = null,
)
