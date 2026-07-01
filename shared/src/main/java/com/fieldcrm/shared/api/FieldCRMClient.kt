package com.fieldcrm.shared.api

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.fieldcrm.shared.model.LoanApplicationModel

class FieldCRMClient(private val baseUrl: String) {
    private var accessToken: String? = null

    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    fun setToken(token: String) {
        accessToken = token
    }

    private fun HttpRequestBuilder.secureHeaders() {
        accessToken?.let {
            header(HttpHeaders.Authorization, "Bearer $it")
        }
    }

    suspend fun createApplication(app: LoanApplicationModel): HttpResponse {
        return client.post("$baseUrl/api/v1/mobile/applications") {
            contentType(ContentType.Application.Json)
            secureHeaders()
            setBody(
                MobileApplicationRequest(
                    customer_type = "new",
                    loan_type = app.product_type.toMobileLoanType(),
                    applicant_name = app.applicant_name,
                    amount = app.amount,
                    tenure = app.tenure,
                    product_type = app.product_type
                )
            )
        }
    }

    suspend fun fetchApplications(): List<LoanApplicationModel> {
        val response = client.get("$baseUrl/api/v1/mobile/applications") {
            secureHeaders()
        }
        return if (response.status == HttpStatusCode.OK) {
            Json.parseToJsonElement(response.bodyAsText())
                .jsonObject["items"]
                ?.jsonArray
                ?.map { it.jsonObject.toLoanApplicationModel() }
                ?: emptyList()
        } else {
            emptyList()
        }
    }

    private fun JsonObject.toLoanApplicationModel(): LoanApplicationModel {
        val stage = string("stage")
        val id = string("id")
        return LoanApplicationModel(
            id = id,
            org_id = string("org_id", "online_org"),
            borrower_id = string("borrower_id", id),
            applicant_name = string("applicant_name", "Applicant"),
            current_stage = int("current_stage", stage.toCurrentStage()),
            current_owner_id = string("current_owner_id", string("created_by", "")),
            status = string("status", stage.toDisplayStatus()),
            amount = double("amount", 0.0),
            tenure = int("tenure", int("tenor_months", 0)),
            product_type = string("product_type", string("loan_type", "enterprise")),
            interest_rate = double("interest_rate", 15.0),
            repayment_frequency = string("repayment_frequency", string("repayment_mode", "Monthly")),
            collateral_desc = stringOrNull("collateral_desc") ?: stringOrNull("purpose"),
            collateral_value = doubleOrNull("collateral_value"),
            officer_recommendation = stringOrNull("officer_recommendation"),
            created_at = string("created_at")
        )
    }

    private fun JsonObject.string(key: String, default: String = ""): String {
        return this[key]?.jsonPrimitive?.content ?: default
    }

    private fun JsonObject.stringOrNull(key: String): String? {
        return this[key]?.jsonPrimitive?.content
    }

    private fun JsonObject.int(key: String, default: Int): Int {
        return this[key]?.jsonPrimitive?.intOrNull ?: default
    }

    private fun JsonObject.double(key: String, default: Double): Double {
        return this[key]?.jsonPrimitive?.doubleOrNull ?: default
    }

    private fun JsonObject.doubleOrNull(key: String): Double? {
        return this[key]?.jsonPrimitive?.doubleOrNull
    }

    private fun String.toCurrentStage(): Int {
        return when (this) {
            "intake" -> 1
            "ocr_review" -> 2
            "credit_review" -> 3
            "branch_approval" -> 4
            "disbursement_ready" -> 5
            "disbursed" -> 6
            "returned" -> 7
            "rejected" -> 8
            else -> 1
        }
    }

    private fun String.toDisplayStatus(): String {
        return when (this) {
            "intake" -> "Draft"
            "ocr_review" -> "OCR Review"
            "credit_review" -> "Credit Review"
            "branch_approval" -> "Branch Approval"
            "disbursement_ready" -> "Disbursement Ready"
            "disbursed" -> "Disbursed"
            "returned" -> "Returned"
            "rejected" -> "Rejected"
            else -> "Draft"
        }
    }

    private fun String.toMobileLoanType(): String {
        return when (lowercase()) {
            "enterprise", "msef", "payee", "other" -> lowercase()
            "sme", "business", "business_loan" -> "enterprise"
            "personal_loan", "personal" -> "payee"
            else -> "other"
        }
    }
}

@Serializable
private data class MobileApplicationRequest(
    val customer_type: String,
    val loan_type: String,
    val applicant_name: String,
    val amount: Double,
    val tenure: Int,
    val product_type: String
)
