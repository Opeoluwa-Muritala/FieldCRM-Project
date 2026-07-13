package com.fieldcrm.android.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.JsonElement

sealed interface LoginOutcome {
    data class Success(val token: String) : LoginOutcome
    data object InvalidCredentials : LoginOutcome
    data object NetworkError : LoginOutcome
    data class ServerError(val code: Int) : LoginOutcome
}

interface MobileApiService {
    fun setToken(token: String)
    /** Full credential login — calls /auth/login-mobile and returns a 30-day token for biometric reuse. */
    suspend fun login(username: String, password: String): TokenResponse?
    suspend fun loginWithResult(username: String, password: String): LoginOutcome
    suspend fun getMe(): MobileUser?
    suspend fun getDashboard(): String?
    suspend fun getDashboardMetrics(): DashboardMetrics?
    suspend fun getQueue(queueName: String): String?
    suspend fun getBorrowers(): String?
    suspend fun createBorrower(data: Map<String, JsonElement>): String?
    suspend fun generateShareLink(): ShareLinkResponse?
    suspend fun createApplication(customerType: String, loanType: String, applicantName: String): String?
    suspend fun getApplicationDetail(id: String): String?
    suspend fun saveIntakeStep(id: String, step: Int, data: Map<String, JsonElement>): String?
    suspend fun getGuarantorData(id: String, slot: Int): String?
    suspend fun saveGuarantorStep(id: String, slot: Int, step: Int, data: Map<String, JsonElement>): String?
    suspend fun uploadDocument(id: String, category: String, fileBytes: ByteArray? = null, fileName: String = "document"): String?
    suspend fun submitOcrReview(id: String, corrections: Map<String, String>): String?
    suspend fun getVisitationReport(id: String): String?
    suspend fun submitVisitationReport(id: String, metWith: String, premises: String, direction: String): String?
    suspend fun submitVisitationSignoff(id: String, decision: String, notes: String): String?
    suspend fun submitCreditReview(id: String, decision: String, notes: String): String?
    suspend fun approveApplication(id: String): String?
    suspend fun returnApplication(id: String, reason: String, corrections: List<String> = emptyList(), notes: String): String?
    suspend fun getNotifications(): List<ApiNotification>
    suspend fun markNotificationRead(id: String): Boolean
    suspend fun clearNotifications(): Boolean
    suspend fun getConfig(): AppConfig?
    suspend fun search(query: String): SearchResponse?
    suspend fun getAuditTrail(applicationId: String): List<AuditTrailEvent>
    suspend fun getBureauData(applicationId: String): BureauData?
    suspend fun getCommitteeVotes(applicationId: String): CommitteeVotes?
    suspend fun getAuditChecklist(applicationId: String): AuditChecklist?
    suspend fun saveAuditChecklist(applicationId: String, checklist: AuditChecklist): Boolean
    suspend fun getFaqs(): List<FaqItem>
    suspend fun getOnboarding(role: String): List<OnboardingSlide>
    suspend fun forgotPassword(email: String): Boolean
    suspend fun resetPassword(token: String, newPassword: String): Boolean

    // CRM review
    suspend fun submitCrmReview(id: String, decision: String, notes: String): String?

    // Executive approval
    suspend fun submitExecutiveApprove(id: String): String?

    // Repayment schedule & payments
    suspend fun getRepaymentSchedule(id: String): RepaymentScheduleResponse?
    suspend fun recordPayment(id: String, amountPaid: Double, channel: String, bankRef: String?, paymentDate: String?): String?

    // PAR dashboard
    suspend fun getParDashboard(): String?

    // Multi-page document upload (PDF assembled on device)
    suspend fun uploadDocumentPdf(id: String, category: String, pdfBytes: ByteArray, fileName: String): String?

    // Committee review
    suspend fun getCommitteeVotesFull(applicationId: String): CommitteeVotesFullResponse?
    suspend fun submitCommitteeVote(id: String, recommendation: String, notes: String): String?
    suspend fun completeCommitteeReview(id: String, recommendation: String): String?

    // ED approval
    suspend fun getEdReview(id: String): String?
    suspend fun submitEdApprove(id: String, action: String): String?

    // MD approval
    suspend fun getMdReview(id: String): String?
    suspend fun submitMdApprove(id: String, action: String, notes: String): String?
    suspend fun addBoardReferral(id: String, email: String, name: String, notes: String): String?

    // User management (admin only)
    suspend fun listUsers(): List<MobileUserItem>
    suspend fun createUser(fullName: String, email: String, role: String, password: String): Boolean
}

@kotlinx.serialization.Serializable
data class ShareLinkResponse(
    val share_url: String,
    val token: String
)

@kotlinx.serialization.Serializable
data class ApiNotification(
    val id: String,
    val title: String,
    val message: String,
    val created_at: String,
    val is_read: Boolean,
    val application_id: String? = null,
    val type: String = "general"
)

@kotlinx.serialization.Serializable
data class DashboardMetrics(
    val apps_today: Int = 0,
    val pending_sync: Int = 0,
    val visits_due: Int = 0,
    val missing_docs: Int = 0,
    val branch_disbursed: Double = 0.0,
    val target_met_pct: Int = 0,
    val awaiting_signoff: Int = 0,
    val active_agents: Int = 0,
    val underwriting_queue: Int = 0,
    val avg_turnaround_mins: Int = 0,
    val high_risk_cases: Int = 0,
    val approved_today: Int = 0,
    val flags_raised: Int = 0,
    val policy_breaches: Int = 0,
    val audited_today: Int = 0,
    val board_tickets: Int = 0,
    val mcr_disbursed: Double = 0.0,
    val alert_escalations: Int = 0,
    val decisions_signed: Int = 0
)

@kotlinx.serialization.Serializable
data class LoanProduct(val id: String, val name: String)

@kotlinx.serialization.Serializable
data class ConfigDropdowns(
    val marital_status: List<String> = emptyList(),
    val employment_status: List<String> = emptyList(),
    val loan_products: List<LoanProduct> = emptyList(),
    val error_categories: List<String> = emptyList(),
    val review_reasons: List<String> = emptyList(),
    val document_categories: List<String> = emptyList()
)

@kotlinx.serialization.Serializable
data class AppConfig(
    val org_name: String = "",
    val support_phone: String = "",
    val support_email: String = "",
    val node_id: String = "",
    val dti_limit: Double = 0.40,
    val pledge_form_code: String = "MMFB/CRM/02",
    val dropdowns: ConfigDropdowns = ConfigDropdowns()
)

@kotlinx.serialization.Serializable
data class AppSearchResult(
    val id: String,
    val ref_no: String,
    val applicant_name: String,
    val stage: String = "intake"
)

@kotlinx.serialization.Serializable
data class BorrowerSearchResult(
    val id: String,
    val name: String,
    val phone: String
)

@kotlinx.serialization.Serializable
data class SearchResponse(
    val applications: List<AppSearchResult> = emptyList(),
    val borrowers: List<BorrowerSearchResult> = emptyList()
)

@kotlinx.serialization.Serializable
data class AuditTrailEvent(
    val id: String,
    val timestamp: String,
    val actor_name: String,
    val actor_role: String,
    val action: String,
    val state_diff: String,
    val notes: String = "",
    val is_mine: Boolean = false
)

@kotlinx.serialization.Serializable
data class BureauData(
    val credit_score: Int = 0,
    val dti_ratio: Double = 0.0,
    val income_verified: Boolean = false,
    val source: String = ""
)

@kotlinx.serialization.Serializable
data class CommitteeVotes(
    val yes_votes: Int = 0,
    val total_votes: Int = 0,
    val quorum: Int = 3
)

@kotlinx.serialization.Serializable
data class AuditChecklist(
    val consent_verified: Boolean = false,
    val signature_matched: Boolean = false,
    val exhibits_verified: Boolean = false
)

@kotlinx.serialization.Serializable
data class FaqItem(val question: String, val answer: String)

@kotlinx.serialization.Serializable
data class OnboardingSlide(val title: String, val subtitle: String, val body: String)

@kotlinx.serialization.Serializable
data class MobileUserItem(
    val id: String,
    val full_name: String,
    val email: String,
    val role: String,
    val display_role: String = "",
    val active: Boolean = true
)

@kotlinx.serialization.Serializable
data class CreateUserRequest(
    val full_name: String,
    val email: String,
    val role: String,
    val password: String
)

class MobileApiServiceImpl(
    private val client: HttpClient,
    private val baseUrl: String = "https://fieldcrm.onrender.com"
) : MobileApiService {

    private var token: String? = null

    override fun setToken(token: String) {
        this.token = token
    }

    private fun HttpRequestBuilder.authHeader() {
        token?.let {
            header(HttpHeaders.Authorization, "Bearer $it")
        }
    }

    override suspend fun login(username: String, password: String): TokenResponse? {
        return try {
            val response: HttpResponse = client.submitForm(
                url = "$baseUrl/api/v1/auth/login-mobile",
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

    override suspend fun loginWithResult(username: String, password: String): LoginOutcome {
        return try {
            val response: HttpResponse = client.submitForm(
                url = "$baseUrl/api/v1/auth/login-mobile",
                formParameters = parameters {
                    append("username", username)
                    append("password", password)
                }
            )
            when {
                response.status == HttpStatusCode.OK -> {
                    val tokenResponse = response.body<TokenResponse>()
                    setToken(tokenResponse.access_token)
                    LoginOutcome.Success(tokenResponse.access_token)
                }
                response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden ->
                    LoginOutcome.InvalidCredentials
                else -> LoginOutcome.ServerError(response.status.value)
            }
        } catch (_: io.ktor.client.plugins.HttpRequestTimeoutException) {
            LoginOutcome.NetworkError
        } catch (_: io.ktor.client.network.sockets.ConnectTimeoutException) {
            LoginOutcome.NetworkError
        } catch (_: java.net.UnknownHostException) {
            LoginOutcome.NetworkError
        } catch (_: java.net.ConnectException) {
            LoginOutcome.NetworkError
        } catch (_: Exception) {
            LoginOutcome.NetworkError
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

    override suspend fun getDashboardMetrics(): DashboardMetrics? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/v1/mobile/dashboard") {
                authHeader()
            }
            if (response.status == HttpStatusCode.OK) response.body() else null
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

    override suspend fun generateShareLink(): ShareLinkResponse? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/v1/mobile/generate-share-link") {
                authHeader()
            }
            if (response.status == HttpStatusCode.OK) response.body() else null
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

    override suspend fun getBorrowers(): String? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/v1/mobile/borrowers") { authHeader() }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) { null }
    }

    override suspend fun createBorrower(data: Map<String, JsonElement>): String? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/v1/mobile/borrowers") {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(data)
            }
            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) response.bodyAsText() else null
        } catch (e: Exception) { null }
    }

    override suspend fun saveIntakeStep(id: String, step: Int, data: Map<String, JsonElement>): String? {
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

    override suspend fun getGuarantorData(id: String, slot: Int): String? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/v1/mobile/applications/$id/guarantors/$slot") {
                authHeader()
            }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) { null }
    }

    override suspend fun saveGuarantorStep(id: String, slot: Int, step: Int, data: Map<String, JsonElement>): String? {
        return try {
            val response: HttpResponse = client.put("$baseUrl/api/v1/mobile/applications/$id/guarantors/$slot/steps/$step") {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(SaveStepRequest(data))
            }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun uploadDocument(id: String, category: String, fileBytes: ByteArray?, fileName: String): String? {
        if (fileBytes == null) return null
        return try {
            val contentType = when (fileName.substringAfterLast('.', "").lowercase()) {
                "pdf" -> ContentType.Application.Pdf
                "jpg", "jpeg" -> ContentType.Image.JPEG
                "png" -> ContentType.Image.PNG
                else -> ContentType.Application.OctetStream
            }
            val response: HttpResponse = client.submitFormWithBinaryData(
                url = "$baseUrl/api/v1/mobile/applications/$id/documents",
                formData = formData {
                    append("doc_type", category)
                    append("file", fileBytes, io.ktor.http.Headers.build {
                        append(HttpHeaders.ContentType, contentType.toString())
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"$fileName\"")
                    })
                }
            ) { authHeader() }
            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) response.bodyAsText() else null
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

    override suspend fun returnApplication(id: String, reason: String, corrections: List<String>, notes: String): String? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/v1/mobile/applications/$id/return") {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(ReturnApplicationRequest(reason, corrections, notes))
            }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getNotifications(): List<ApiNotification> {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/v1/mobile/notifications") {
                authHeader()
            }
            if (response.status == HttpStatusCode.OK) response.body() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun markNotificationRead(id: String): Boolean {
        return try {
            val response: HttpResponse = client.patch("$baseUrl/api/v1/mobile/notifications/$id/read") {
                authHeader()
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun clearNotifications(): Boolean {
        return try {
            val response: HttpResponse = client.delete("$baseUrl/api/v1/mobile/notifications") {
                authHeader()
            }
            response.status == HttpStatusCode.OK || response.status == HttpStatusCode.NoContent
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getConfig(): AppConfig? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/v1/mobile/config") { authHeader() }
            if (response.status == HttpStatusCode.OK) response.body() else null
        } catch (e: Exception) { null }
    }

    override suspend fun search(query: String): SearchResponse? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/v1/mobile/search") {
                authHeader()
                parameter("q", query)
            }
            if (response.status == HttpStatusCode.OK) response.body() else null
        } catch (e: Exception) { null }
    }

    override suspend fun getAuditTrail(applicationId: String): List<AuditTrailEvent> {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/v1/mobile/applications/$applicationId/audit") {
                authHeader()
            }
            if (response.status == HttpStatusCode.OK) response.body() else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun getBureauData(applicationId: String): BureauData? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/v1/mobile/applications/$applicationId/bureau") {
                authHeader()
            }
            if (response.status == HttpStatusCode.OK) response.body() else null
        } catch (e: Exception) { null }
    }

    override suspend fun getCommitteeVotes(applicationId: String): CommitteeVotes? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/v1/mobile/applications/$applicationId/committee-votes") {
                authHeader()
            }
            if (response.status == HttpStatusCode.OK) response.body() else null
        } catch (e: Exception) { null }
    }

    override suspend fun getAuditChecklist(applicationId: String): AuditChecklist? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/v1/mobile/applications/$applicationId/audit-checklist") {
                authHeader()
            }
            if (response.status == HttpStatusCode.OK) response.body() else null
        } catch (e: Exception) { null }
    }

    override suspend fun saveAuditChecklist(applicationId: String, checklist: AuditChecklist): Boolean {
        return try {
            val response: HttpResponse = client.patch("$baseUrl/api/v1/mobile/applications/$applicationId/audit-checklist") {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(checklist)
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) { false }
    }

    override suspend fun getFaqs(): List<FaqItem> {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/v1/mobile/faqs") { authHeader() }
            if (response.status == HttpStatusCode.OK) response.body() else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun getOnboarding(role: String): List<OnboardingSlide> {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/v1/mobile/onboarding") {
                authHeader()
                parameter("role", role)
            }
            if (response.status == HttpStatusCode.OK) response.body() else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun forgotPassword(email: String): Boolean {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/v1/mobile/auth/forgot-password") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("email" to email))
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) { false }
    }

    override suspend fun resetPassword(token: String, newPassword: String): Boolean {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/v1/mobile/auth/reset-password") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("token" to token, "new_password" to newPassword))
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) { false }
    }

    override suspend fun submitCrmReview(id: String, decision: String, notes: String): String? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/v1/mobile/applications/$id/crm-review") {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(CrmReviewRequest(decision, notes))
            }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) { null }
    }

    override suspend fun submitExecutiveApprove(id: String): String? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/v1/mobile/applications/$id/executive-approve") {
                authHeader()
            }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) { null }
    }

    override suspend fun getRepaymentSchedule(id: String): RepaymentScheduleResponse? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/v1/mobile/applications/$id/repayment-schedule") {
                authHeader()
            }
            if (response.status == HttpStatusCode.OK) response.body() else null
        } catch (e: Exception) { null }
    }

    override suspend fun recordPayment(
        id: String, amountPaid: Double, channel: String, bankRef: String?, paymentDate: String?
    ): String? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/v1/mobile/applications/$id/payments") {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(RecordPaymentRequest(amountPaid, channel, bankRef, paymentDate))
            }
            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created)
                response.bodyAsText() else null
        } catch (e: Exception) { null }
    }

    override suspend fun getParDashboard(): String? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/v1/mobile/reports/par") {
                authHeader()
            }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) { null }
    }

    override suspend fun uploadDocumentPdf(id: String, category: String, pdfBytes: ByteArray, fileName: String): String? {
        return try {
            val response: HttpResponse = client.submitFormWithBinaryData(
                url = "$baseUrl/api/v1/mobile/applications/$id/documents",
                formData = formData {
                    append("doc_type", category)
                    append("file", pdfBytes, io.ktor.http.Headers.build {
                        append(HttpHeaders.ContentType, ContentType.Application.Pdf.toString())
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"$fileName\"")
                    })
                }
            ) { authHeader() }
            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created)
                response.bodyAsText() else null
        } catch (e: Exception) { null }
    }

    override suspend fun getCommitteeVotesFull(applicationId: String): CommitteeVotesFullResponse? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/v1/mobile/applications/$applicationId/committee-votes-full") {
                authHeader()
            }
            if (response.status == HttpStatusCode.OK) response.body() else null
        } catch (e: Exception) { null }
    }

    override suspend fun submitCommitteeVote(id: String, recommendation: String, notes: String): String? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/v1/mobile/applications/$id/committee-vote") {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(CommitteeVoteRequest(recommendation, notes))
            }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) { null }
    }

    override suspend fun completeCommitteeReview(id: String, recommendation: String): String? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/v1/mobile/applications/$id/committee-complete") {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(CommitteeCompleteRequest(recommendation))
            }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) { null }
    }

    override suspend fun getEdReview(id: String): String? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/v1/mobile/applications/$id/ed-review") {
                authHeader()
            }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) { null }
    }

    override suspend fun submitEdApprove(id: String, action: String): String? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/v1/mobile/applications/$id/ed-approve") {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(EdApproveRequest(action))
            }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) { null }
    }

    override suspend fun getMdReview(id: String): String? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/v1/mobile/applications/$id/md-review") {
                authHeader()
            }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) { null }
    }

    override suspend fun submitMdApprove(id: String, action: String, notes: String): String? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/v1/mobile/applications/$id/md-approve") {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(MdApproveRequest(action, notes))
            }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) { null }
    }

    override suspend fun addBoardReferral(id: String, email: String, name: String, notes: String): String? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/v1/mobile/applications/$id/md-refer-board") {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(BoardReferralRequest(email, name, notes))
            }
            if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
        } catch (e: Exception) { null }
    }

    override suspend fun listUsers(): List<MobileUserItem> {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/v1/mobile/users") { authHeader() }
            if (response.status == HttpStatusCode.OK) response.body() else emptyList()
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun createUser(fullName: String, email: String, role: String, password: String): Boolean {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/v1/mobile/users") {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(CreateUserRequest(fullName, email, role, password))
            }
            response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK
        } catch (e: Exception) { false }
    }
}
