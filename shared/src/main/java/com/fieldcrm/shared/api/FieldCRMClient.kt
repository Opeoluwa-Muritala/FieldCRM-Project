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

class FieldCRMClient(internal val baseUrl: String) {
    private var accessToken: String? = null

    val httpClient = HttpClient {
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

    internal fun authHeader(builder: HttpRequestBuilder) {
        accessToken?.let {
            builder.header(HttpHeaders.Authorization, "Bearer $it")
        }
    }

    suspend fun createApplication(app: LoanApplicationModel): HttpResponse {
        return httpClient.post("$baseUrl/api/v1/mobile/applications") {
            contentType(ContentType.Application.Json)
            secureHeaders()
            setBody(
                MobileApplicationRequest(
                    customer_type = app.customer_type,
                    loan_type = app.loan_type,
                    applicant_name = app.applicant_name
                )
            )
        }
    }

    suspend fun fetchApplications(): List<LoanApplicationModel> {
        val response = httpClient.get("$baseUrl/api/v1/mobile/applications") {
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
        return LoanApplicationModel(
            id = string("id"),
            org_id = string("org_id"),
            ref_no = string("ref_no"),
            customer_type = string("customer_type", "new"),
            loan_type = string("loan_type", "enterprise"),
            stage = string("stage", "intake"),
            applicant_name = string("applicant_name"),
            bvn = stringOrNull("bvn"),
            phone = stringOrNull("phone"),
            amount = doubleOrNull("amount"),
            tenor_months = intOrNull("tenor_months"),
            purpose = stringOrNull("purpose"),
            repayment_mode = stringOrNull("repayment_mode"),
            created_by = string("created_by"),
            current_owner_id = stringOrNull("current_owner_id"),
            credit_officer_id = stringOrNull("credit_officer_id"),
            branch_manager_id = stringOrNull("branch_manager_id"),
            return_reason = stringOrNull("return_reason"),
            approved_by = stringOrNull("approved_by"),
            approved_at = stringOrNull("approved_at"),
            disbursed_at = stringOrNull("disbursed_at"),
            interest_rate = doubleOrNull("interest_rate"),
            repayment_frequency = stringOrNull("repayment_frequency"),
            schedule_method = stringOrNull("schedule_method"),
            classification = stringOrNull("classification") ?: "current",
            days_past_due = int("days_past_due", 0),
            crm_notes = stringOrNull("crm_notes"),
            crm_reviewed_by = stringOrNull("crm_reviewed_by"),
            crm_reviewed_at = stringOrNull("crm_reviewed_at"),
            executive_approved_by = stringOrNull("executive_approved_by"),
            executive_approved_at = stringOrNull("executive_approved_at"),
            disbursed_amount = doubleOrNull("disbursed_amount"),
            disbursement_method = stringOrNull("disbursement_method"),
            sector = stringOrNull("sector"),
            created_at = string("created_at"),
            updated_at = stringOrNull("updated_at")
        )
    }

    private fun JsonObject.string(key: String, default: String = ""): String =
        this[key]?.jsonPrimitive?.content ?: default

    private fun JsonObject.stringOrNull(key: String): String? =
        this[key]?.jsonPrimitive?.content

    private fun JsonObject.int(key: String, default: Int): Int =
        this[key]?.jsonPrimitive?.intOrNull ?: default

    private fun JsonObject.intOrNull(key: String): Int? =
        this[key]?.jsonPrimitive?.intOrNull

    private fun JsonObject.doubleOrNull(key: String): Double? =
        this[key]?.jsonPrimitive?.doubleOrNull
}

@Serializable
private data class MobileApplicationRequest(
    val customer_type: String,
    val loan_type: String,
    val applicant_name: String
)
