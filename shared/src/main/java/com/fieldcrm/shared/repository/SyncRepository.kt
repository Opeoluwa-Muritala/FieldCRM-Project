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

    /**
     * Queues an offline application write without creating a separate borrower business model.
     */
    fun queueApplicationWrite(application: LoanApplicationModel, action: String = "CREATE_APPLICATION") {
        queries.insertApplication(
            id = application.id,
            borrower_id = application.borrower_id,
            applicant_name = application.applicant_name,
            org_id = application.org_id,
            current_stage = application.current_stage.toLong(),
            current_owner_id = application.current_owner_id,
            status = application.status,
            amount = application.amount,
            tenure = application.tenure.toLong(),
            product_type = application.product_type
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

    /**
     * Sequentially replays outstanding queue actions:
     * Server remains authoritative for workflow stages and permissions.
     */
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
                            val payload = kotlinx.serialization.json.Json.parseToJsonElement(item.payload_json).jsonObject
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
                            val payload = kotlinx.serialization.json.Json.parseToJsonElement(item.payload_json).jsonObject
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
                    "EXECUTE_PLEDGE" -> {
                        try {
                            val payload = kotlinx.serialization.json.Json.parseToJsonElement(item.payload_json).jsonObject
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
                    "SUBMIT_INTAKE" -> {
                        try {
                            val payload = kotlinx.serialization.json.Json.parseToJsonElement(item.payload_json).jsonObject
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
                    // Remove resolved item from local queue
                    queries.deleteQueueItem(item.id)
                } else {
                    overallSuccess = false
                }
            } catch (e: Exception) {
                overallSuccess = false
                // Retain in queue for next connectivity recovery
            }
        }
        return overallSuccess
    }
}

object ClockSystem {
    fun nowEpochMillis(): Long {
        // expect/actual could be used, or returning system current time in milliseconds
        return 1774828800000L // Standard baseline mock time
    }
}
