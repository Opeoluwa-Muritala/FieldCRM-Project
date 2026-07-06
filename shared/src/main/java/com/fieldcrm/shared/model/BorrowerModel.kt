package com.fieldcrm.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class BorrowerModel(
    val id: String,
    val org_id: String,
    val loan_officer_id: String,
    val name: String,
    val phone: String,
    val bvn: String,
    val nin: String,
    val photo_url: String? = null,
    val status: String,
    val gps_coordinates: String? = null,
    val physical_address: String? = null,
    val employment_status: String? = null,
    val employer_name: String? = null,
    val monthly_income: Double? = null,
    val bank_name: String? = null,
    val account_number: String? = null,
    val guarantor_name: String? = null,
    val guarantor_phone: String? = null,
    val created_at: String
)

/**
 * Mirrors the backend loan_applications table columns.
 * Stage values: intake, ocr_review, credit_review, branch_approval,
 *               crm_review, executive_approval, disbursement_ready, disbursed, returned, rejected
 * Loan type values: enterprise, msef, payee, other
 * Customer type values: new, existing
 */
@Serializable
data class LoanApplicationModel(
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
    val sector: String? = null,
    val created_at: String = "",
    val updated_at: String? = null,
) {
    val displayStatus: String get() = when (stage) {
        "intake" -> "Draft"
        "ocr_review" -> "OCR Review"
        "credit_review" -> "Credit Review"
        "branch_approval" -> "Branch Approval"
        "crm_review" -> "CRM Review"
        "executive_approval" -> "Executive Approval"
        "disbursement_ready" -> "Disbursement Ready"
        "disbursed" -> "Disbursed"
        "returned" -> "Returned"
        "rejected" -> "Rejected"
        else -> stage.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    val isActive: Boolean get() = stage !in setOf("disbursed", "returned", "rejected")

    val stageIndex: Int get() = when (stage) {
        "intake" -> 1
        "ocr_review" -> 2
        "credit_review" -> 3
        "branch_approval" -> 4
        "crm_review" -> 4
        "executive_approval" -> 4
        "disbursement_ready", "disbursed" -> 5
        else -> 1
    }
}

@Serializable
data class SyncPayload(
    val action: String,
    val entity_id: String,
    val payload_json: String,
    val timestamp: Long
)
