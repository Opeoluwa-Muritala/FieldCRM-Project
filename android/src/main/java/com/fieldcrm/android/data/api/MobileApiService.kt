package com.fieldcrm.android.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import com.fieldcrm.shared.model.BorrowerModel

interface MobileApiService {
    suspend fun login(username: String, password: String): TokenResponse?
    suspend fun getMe(): MobileUser?
    suspend fun getDashboard(): String?
    suspend fun getQueue(queueName: String): String?
    suspend fun createApplication(customerType: String, loanType: String, applicantName: String): String?
    suspend fun getApplicationDetail(id: String): String?
    suspend fun saveIntakeStep(id: String, step: Int, data: Map<String, String>): String?
    suspend fun saveGuarantorStep(id: String, slot: Int, step: Int, data: Map<String, String>): String?
    suspend fun uploadDocument(id: String, category: String): String?
    suspend fun submitOcrReview(id: String, corrections: Map<String, String>): String?
    suspend fun getVisitationReport(id: String): String?
    suspend fun submitVisitationReport(id: String, metWith: String, premises: String, direction: String): String?
    suspend fun submitVisitationSignoff(id: String, decision: String, notes: String): String?
    suspend fun submitCreditReview(id: String, decision: String, notes: String): String?
    suspend fun approveApplication(id: String): String?
    suspend fun returnApplication(id: String, reason: String, notes: String): String?
}

class MobileApiServiceImpl(
    private val client: HttpClient,
    private val baseUrl: String = "https://fieldcrm.onrender.com"
) : MobileApiService {

    private var token: String? = null

    fun setToken(newToken: String) {
        token = newToken
    }

    private fun HttpRequestBuilder.authHeader() {
        token?.let {
            header(HttpHeaders.Authorization, "Bearer $it")
        }
    }

    override suspend fun login(username: String, password: String): TokenResponse? {
        return try {
            val response: HttpResponse = client.submitForm(
                url = "$baseUrl/api/v1/auth/login-bearer",
                formParameters = parameters {
                    append("username", username)
                    append("password", password)
                }
            )
            if (response.status == HttpStatusCode.OK) {
                val tokenResponse = response.body<TokenResponse>()
                setToken(tokenResponse.access_token)
                tokenResponse
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getMe(): MobileUser? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/v1/mobile/me") {
                authHeader()
            }
            if (response.status == HttpStatusCode.OK) response.body() else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getDashboard(): String? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/v1/mobile/dashboard") {
                authHeader()
            }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getQueue(queueName: String): String? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/v1/mobile/queues/$queueName") {
                authHeader()
            }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun createApplication(customerType: String, loanType: String, applicantName: String): String? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/v1/mobile/applications") {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(CreateAppRequest(customerType, loanType, applicantName))
            }
            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getApplicationDetail(id: String): String? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/v1/mobile/applications/$id") {
                authHeader()
            }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveIntakeStep(id: String, step: Int, data: Map<String, String>): String? {
        return try {
            val response: HttpResponse = client.put("$baseUrl/api/v1/mobile/applications/$id/intake/steps/$step") {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(SaveStepRequest(data))
            }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveGuarantorStep(id: String, slot: Int, step: Int, data: Map<String, String>): String? {
        return try {
            val response: HttpResponse = client.put("$baseUrl/api/v1/mobile/applications/$id/guarantors/$slot/steps/$step") {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(mapOf("data" to data))
            }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun uploadDocument(id: String, category: String): String? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/v1/mobile/applications/$id/documents") {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(mapOf("category" to category))
            }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun submitOcrReview(id: String, corrections: Map<String, String>): String? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/v1/mobile/applications/$id/ocr-review") {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(OcrReviewRequest("verify", corrections))
            }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getVisitationReport(id: String): String? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/v1/mobile/applications/$id/visitation") {
                authHeader()
            }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun submitVisitationReport(id: String, metWith: String, premises: String, direction: String): String? {
        return try {
            val response: HttpResponse = client.put("$baseUrl/api/v1/mobile/applications/$id/visitation") {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(VisitationReportRequest(metWith, premises, direction))
            }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun submitVisitationSignoff(id: String, decision: String, notes: String): String? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/v1/mobile/applications/$id/visitation/signoff") {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(VisitationSignoffRequest(decision, notes))
            }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun submitCreditReview(id: String, decision: String, notes: String): String? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/v1/mobile/applications/$id/credit-review") {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(CreditReviewRequest(decision, notes))
            }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun approveApplication(id: String): String? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/v1/mobile/applications/$id/approve") {
                authHeader()
            }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun returnApplication(id: String, reason: String, notes: String): String? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/v1/mobile/applications/$id/return") {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(ReturnApplicationRequest(reason, notes))
            }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) {
            null
        }
    }
}
