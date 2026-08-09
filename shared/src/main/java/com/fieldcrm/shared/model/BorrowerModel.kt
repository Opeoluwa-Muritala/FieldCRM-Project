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
    val created_at: String,
    // Profile fields returned when an existing customer is selected. These are
    // transient form defaults; the local database schema remains unchanged.
    val date_of_birth: String? = null,
    val marital_status: String? = null,
    val state: String? = null,
    val lga: String? = null,
    val email: String? = null
)

/**
 * Mirrors the backend loan_applications table columns.
 * Active web workflow: intake -> branch_manager_review -> branch_supervisor_review
 * -> credit_analyst_review -> crm_review -> head_crm_review -> ed_approval
 * -> optional md_approval -> disbursement_ready -> disbursed.
 * Audit and Legal inspect applications but are not mandatory workflow stages.
 * Loan type values: enterprise, msef, payee, other
 * Customer type values: new, existing
 */
@Serializable
data class LoanApplicationModel(
    val id: String,
    val org_id: String,
    val borrower_id: String? = null,
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
    val classification: String? = null,
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
        "intake" -> "Relationship Officer Intake"
        "branch_manager_review" -> "Team Lead Review"
        "branch_supervisor_review" -> "Supervisor Review"
        "credit_analyst_review" -> "Credit Analysis"
        "crm_review" -> "CRM Dossier Review"
        "head_crm_review" -> "Head CRM Approval"
        "ed_approval", "executive_approval" -> "Executive Director Approval"
        "md_approval" -> "Managing Director Input"
        "disbursement_ready" -> "CRM Disbursement"
        "disbursed" -> "Disbursed"
        "returned" -> "Returned"
        "rejected" -> "Rejected"
        // Labels retained for historical records created by the retired workflow.
        "ocr_review" -> "Legacy OCR Review"
        "credit_review" -> "Legacy Credit Review"
        "branch_approval" -> "Legacy Branch Approval"
        "committee_review" -> "Legacy Committee Review"
        else -> stage.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    val isActive: Boolean get() = stage !in setOf("disbursed", "returned", "rejected")

    val stageIndex: Int get() = when (stage) {
        "intake" -> 0
        "branch_manager_review" -> 1
        "branch_supervisor_review" -> 2
        "credit_analyst_review" -> 3
        "crm_review" -> 4
        "head_crm_review" -> 5
        "ed_approval", "executive_approval" -> 6
        "md_approval" -> 7
        "disbursement_ready" -> 8
        "disbursed" -> 9
        "ocr_review" -> 0
        "credit_review" -> 3
        "branch_approval" -> 1
        "committee_review" -> 6
        else -> 0
    }
}

@Serializable
data class SyncPayload(
    val action: String,
    val entity_id: String,
    val payload_json: String,
    val timestamp: Long
)
