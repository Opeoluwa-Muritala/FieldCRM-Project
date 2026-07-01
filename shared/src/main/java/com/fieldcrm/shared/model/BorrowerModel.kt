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

@Serializable
data class LoanApplicationModel(
    val id: String,
    val org_id: String,
    val borrower_id: String,
    val applicant_name: String = "Applicant",
    val current_stage: Int,
    val current_owner_id: String,
    val status: String,
    val amount: Double,
    val tenure: Int,
    val product_type: String,
    val interest_rate: Double,
    val repayment_frequency: String,
    val collateral_desc: String? = null,
    val collateral_value: Double? = null,
    val officer_recommendation: String? = null,
    val created_at: String
)

@Serializable
data class SyncPayload(
    val action: String,
    val entity_id: String,
    val payload_json: String,
    val timestamp: Long
)
