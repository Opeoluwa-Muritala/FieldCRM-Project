package com.fieldcrm.shared.repository

import com.fieldcrm.shared.api.FieldCRMClient
import com.fieldcrm.shared.db.AppDatabase
import com.fieldcrm.shared.model.LoanApplicationModel
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SyncRepository(
    private val database: AppDatabase,
    private val client: FieldCRMClient
) {
    private val queries = database.appDatabaseQueries

    fun queueApplicationWrite(application: LoanApplicationModel, action: String = "CREATE_APPLICATION") {
        queries.insertApplication(
            id = application.id,
            org_id = application.org_id,
            ref_no = application.ref_no,
            customer_type = application.customer_type,
            loan_type = application.loan_type,
            stage = application.stage,
            applicant_name = application.applicant_name,
            bvn = application.bvn,
            phone = application.phone,
            amount = application.amount,
            tenor_months = application.tenor_months?.toLong(),
            purpose = application.purpose,
            repayment_mode = application.repayment_mode,
            created_by = application.created_by,
            current_owner_id = application.current_owner_id,
            credit_officer_id = application.credit_officer_id,
            branch_manager_id = application.branch_manager_id,
            return_reason = application.return_reason,
            approved_by = application.approved_by,
            approved_at = application.approved_at,
            disbursed_at = application.disbursed_at,
            interest_rate = application.interest_rate,
            repayment_frequency = application.repayment_frequency,
            schedule_method = application.schedule_method,
            classification = application.classification,
            days_past_due = application.days_past_due.toLong(),
            crm_notes = application.crm_notes,
            crm_reviewed_by = application.crm_reviewed_by,
            executive_approved_by = application.executive_approved_by,
            created_at = application.created_at,
            updated_at = application.updated_at
        )

        val payloadJson = Json.encodeToString(LoanApplicationModel.serializer(), application)
        queries.insertQueueItem(
            id = application.id,
            action = action,
            entity_id = application.id,
            payload_json = payloadJson,
            timestamp = ClockSystem.nowEpochMillis(),
            attempts = 0L
        )
    }

    suspend fun syncQueueWithServer(): Boolean {
        val queuedItems = queries.selectQueuedItems().executeAsList()
        if (queuedItems.isEmpty()) return true

        var overallSuccess = true
        for (item in queuedItems) {
            try {
                val success = when (item.action) {
                    "CREATE_APPLICATION", "SUBMIT_APPLICATION" -> {
                        val app = Json.decodeFromString(LoanApplicationModel.serializer(), item.payload_json)
                        val response = client.createApplication(app)
                        response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK
                    }
                    "SUBMIT_OCR_REVIEW" -> {
                        try {
                            val payload = Json.parseToJsonElement(item.payload_json).jsonObject
                            val appId = payload["id"]!!.jsonPrimitive.content
                            val response = client.httpClient.post("${client.baseUrl}/api/v1/mobile/applications/$appId/ocr-review") {
                                contentType(ContentType.Application.Json)
                                client.authHeader(this)
                                setBody("""{"action":"verify","corrections":{}}""")
                            }
                            response.status.value in 200..299
                        } catch (e: Exception) { false }
                    }
                    "SUBMIT_VISITATION" -> {
                        try {
                            val payload = Json.parseToJsonElement(item.payload_json).jsonObject
                            val appId = payload["id"]!!.jsonPrimitive.content
                            val bodyStr = payload["body"]?.jsonPrimitive?.content ?: "{}"
                            val response = client.httpClient.put("${client.baseUrl}/api/v1/mobile/applications/$appId/visitation") {
                                contentType(ContentType.Application.Json)
                                client.authHeader(this)
                                setBody(bodyStr)
                            }
                            response.status.value in 200..299
                        } catch (e: Exception) { false }
                    }
                    "EXECUTE_PLEDGE", "SUBMIT_INTAKE" -> {
                        try {
                            val payload = Json.parseToJsonElement(item.payload_json).jsonObject
                            val appId = payload["id"]!!.jsonPrimitive.content
                            val bodyStr = payload["body"]?.jsonPrimitive?.content ?: "{}"
                            val response = client.httpClient.patch("${client.baseUrl}/api/v1/mobile/applications/$appId") {
                                contentType(ContentType.Application.Json)
                                client.authHeader(this)
                                setBody(bodyStr)
                            }
                            response.status.value in 200..299
                        } catch (e: Exception) { false }
                    }
                    else -> false
                }

                if (success) {
                    queries.deleteQueueItem(item.id)
                } else {
                    overallSuccess = false
                }
            } catch (e: Exception) {
                overallSuccess = false
            }
        }
        return overallSuccess
    }
}

object ClockSystem {
    fun nowEpochMillis(): Long = System.currentTimeMillis()
}
