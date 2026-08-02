package com.fieldcrm.android.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.fieldcrm.android.core.network.ApiResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface LoginOutcome {
    data class Success(val token: String) : LoginOutcome
    data object InvalidCredentials : LoginOutcome
    data object NetworkError : LoginOutcome
    data class ServerError(val code: Int) : LoginOutcome
}

interface MobileApiService {
    fun setToken(token: String)
    /** Full credential login through /auth/login-mobile. */
    suspend fun login(username: String, password: String): TokenResponse?
    suspend fun loginWithResult(username: String, password: String): LoginOutcome
    suspend fun revokeSession(refreshToken: String?): Boolean
    suspend fun getMe(): MobileUser?
    suspend fun getDashboard(): String?
    suspend fun getDashboardMetrics(): DashboardMetrics?
    suspend fun getQueue(queueName: String): String?
    suspend fun getBorrowers(): String?
    suspend fun createBorrower(data: Map<String, JsonElement>): String?
    suspend fun searchExistingCustomers(query: String, limit: Int = 20): ApiResult<ExistingCustomerSearchResponse>
    suspend fun getApplicationProfile(borrowerId: String): ApiResult<ApplicationProfileResponse>
    suspend fun generateShareLink(): ShareLinkResponse?
    suspend fun createApplication(customerType: String, loanType: String, applicantName: String): String?
    suspend fun getApplicationDetail(id: String): String?
    suspend fun fetchDocumentPreview(url: String): ByteArray?
    suspend fun saveIntakeStep(id: String, step: Int, data: Map<String, JsonElement>): String?
    suspend fun getGuarantorData(id: String, slot: Int): String?
    suspend fun saveGuarantorStep(id: String, slot: Int, step: Int, data: Map<String, JsonElement>): String?
    suspend fun uploadDocument(id: String, category: String, fileBytes: ByteArray? = null, fileName: String = "document"): String?
    suspend fun getOcrFields(id: String): OcrFieldsResponse?
    suspend fun submitOcrReview(id: String, corrections: Map<String, String>): String?
    suspend fun getVisitationReport(id: String): String?
    suspend fun submitVisitationReport(id: String, metWith: String, premises: String, direction: String): String?
    suspend fun submitVisitationSignoff(id: String, decision: String, notes: String): String?
    suspend fun submitCreditReview(id: String, decision: String, notes: String): String?
    suspend fun approveApplication(id: String, notes: String, kycAttested: Boolean, collateralAttested: Boolean): String?
    suspend fun returnApplication(id: String, reason: String, corrections: List<String> = emptyList(), notes: String): String?
    suspend fun getNotifications(): List<ApiNotification>
    suspend fun markNotificationRead(id: String): Boolean
    suspend fun clearNotifications(): Boolean
    suspend fun getConfig(): AppConfig?
    suspend fun search(query: String): SearchResponse?
    suspend fun getAuditTrail(applicationId: String): List<AuditTrailEvent>
    suspend fun getGlobalAuditTrail(): List<AuditTrailEvent>
    suspend fun getBureauData(applicationId: String): BureauData?
    suspend fun getAuditChecklist(applicationId: String): AuditChecklist?
    suspend fun saveAuditChecklist(applicationId: String, checklist: AuditChecklist): Boolean
    suspend fun getFaqs(): List<FaqItem>
    suspend fun getOnboarding(role: String): List<OnboardingSlide>
    suspend fun forgotPassword(email: String): Boolean
    suspend fun resetPassword(token: String, newPassword: String): Boolean

    // CRM review
    suspend fun submitCrmReview(id: String, decision: String, notes: String, bureau1: Boolean, bureau2: Boolean, crms: Boolean, ncr: Boolean): String?

    // Executive approval
    suspend fun submitExecutiveApprove(id: String): String?

    // Repayment schedule & payments
    suspend fun getRepaymentSchedule(id: String): RepaymentScheduleResponse?
    suspend fun recordPayment(id: String, amountPaid: Double, channel: String, bankRef: String?, paymentDate: String?): String?

    // PAR dashboard
    suspend fun getParDashboard(): String?

    // Multi-page document upload (PDF assembled on device)
    suspend fun uploadDocumentPdf(id: String, category: String, pdfBytes: ByteArray, fileName: String): String?

    // ED approval
    suspend fun getEdReview(id: String): String?
    suspend fun submitEdApprove(id: String, action: String): String?

    // MD approval
    suspend fun getMdReview(id: String): String?
    suspend fun submitMdApprove(id: String, action: String, notes: String): String?
    suspend fun addBoardReferral(id: String, email: String, name: String, notes: String): String?

    // Canonical operational workflow: Relationship Officer through CRM disbursement readiness.
    suspend fun advanceWorkflow(id: String, notes: String): WorkflowAdvanceResponse?

    // User management (admin only)
    suspend fun listUsers(): ApiResult<List<MobileUserItem>>
    suspend fun createUser(fullName: String, email: String, role: String, password: String): Boolean

    suspend fun pullCreditBureau(id: String): ApiResult<JsonElement>
    suspend fun getCreditChecklist(id: String, context: String = "credit"): ApiResult<CreditChecklistResponse>
    suspend fun updateCreditChecklist(id: String, request: CreditChecklistUpdateRequest): ApiResult<CreditChecklistItem>
    suspend fun generateClientLink(id: String): ApiResult<SigningLinkResponse>
    suspend fun generateGuarantorLink(id: String, slot: Int): ApiResult<SigningLinkResponse>
    suspend fun getOffer(id: String): ApiResult<OfferReadinessResponse>
    suspend fun generateOffer(id: String): ApiResult<JsonElement>
    suspend fun getDisbursement(id: String): ApiResult<JsonElement>
    suspend fun recordDisbursement(id: String, request: DisbursementRequest): ApiResult<JsonElement>
    suspend fun changePassword(current: String, new: String, confirm: String): ApiResult<ChangePasswordResponse>
    suspend fun getSystemActivity(page: Int = 1, size: Int = 50): ApiResult<SystemActivityResponse>
    suspend fun getLegalQueue(page: Int = 1): ApiResult<JsonElement>
    suspend fun getValuation(id: String): ApiResult<JsonElement>
    suspend fun updateValuation(id: String, payload: JsonElement): ApiResult<JsonElement>
    suspend fun getMcc(page: Int = 1): ApiResult<JsonElement>
    suspend fun getMccApplication(id: String): ApiResult<JsonElement>
    suspend fun submitMccVote(id: String, amount: Double, notes: String): ApiResult<JsonElement>
    suspend fun finalizeMcc(id: String, amount: Double): ApiResult<JsonElement>
    suspend fun getInterestPresets(): ApiResult<JsonElement>
    suspend fun createInterestPreset(loanType: String, rate: Double, rateType: String): ApiResult<JsonElement>
    suspend fun deleteInterestPreset(id: String): ApiResult<JsonElement>
    suspend fun updateInterestPreset(id: String, loanType: String, rate: Double, rateType: String): ApiResult<JsonElement>
    suspend fun getBranches(): ApiResult<JsonElement>
    suspend fun createBranch(name: String, code: String): ApiResult<JsonElement>
    suspend fun updateUserRole(id: String, role: String, branchId: String? = null): ApiResult<JsonElement>
    suspend fun deactivateUser(id: String): ApiResult<JsonElement>
    suspend fun getParLoans(page: Int = 1): ApiResult<JsonElement>
    suspend fun getRoleDashboard(role: String): ApiResult<JsonElement>
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
    val alert_escalations: Int = 0,
    val decisions_signed: Int = 0,
    val user: DashboardUser? = null,
    val data: DashboardData? = null
)

@kotlinx.serialization.Serializable
data class DashboardUser(
    val id: String = "",
    val full_name: String = "",
    val role: String = "",
    val display_role: String = ""
)

@kotlinx.serialization.Serializable
data class RelationshipOfficerMetrics(
    val my_applications: Int = 0,
    val pending_upload: Int = 0,
    val visits_due: Int = 0,
    val returned: Int = 0,
    val ocr_review: Int = 0,
    val drafts: Int = 0,
    val awaiting_concurrence: Int = 0,
    val pending_signoffs: Int = 0,
    val approved_today: Int = 0,
    val returned_this_week: Int = 0,
    val supervisory_reviews: Int = 0,
    val reviews_due: Int = 0,
    val ocr_exceptions: Int = 0,
    val reviewed_today: Int = 0,
    val crm_queue: Int = 0,
    val disbursed_total: Int = 0,
    val par30_pct: Double = 0.0,
    val unverified_documents: Int = 0,
    val critical_ocr_gaps: Int = 0,
    val workflow_exceptions: Int = 0,
    val audit_events_today: Int = 0,
    val ed_queue: Int = 0,
    val md_queue: Int = 0,
    val legal_queue: Int = 0,
    val active_users: Int = 0,
    val system_events: Int = 0,
    val failed_jobs: Int = 0,
    val config_alerts: Int = 0,
    val active_loans: Int = 0,
    val blocked_files: Int = 0,
    val ready_for_disbursement: Int = 0,
    val active_assigned: Int = 0,
    val ready_amount: Double = 0.0
)

@kotlinx.serialization.Serializable
data class DashboardTask(
    val loan_id: String = "",
    val ref_no: String = "",
    val applicant_name: String = "",
    val amount: Double? = null,
    val stage: String = "",
    val task_type: String = "",
    val task_description: String = "",
    val updated_at: String = ""
)

@kotlinx.serialization.Serializable
data class DashboardQueueEntry(
    val id: String = "",
    val ref_no: String = "",
    val applicant_name: String = "",
    val loan_type: String = "",
    val amount: Double? = null,
    val stage: String = "",
    val status: String = "",
    val updated_at: String = "",
    val officer_name: String? = null,
    val visitation_status: String? = null,
    val days_waiting: Int = 0
)

@kotlinx.serialization.Serializable
data class DashboardSignoff(
    val id: String = "",
    val loan_id: String = "",
    val ref_no: String = "",
    val applicant_name: String = "",
    val amount: Double? = null,
    val visit_date: String? = null,
    val met_with: String? = null,
    val status: String = "",
    val updated_at: String = "",
    val visiting_officer_name: String? = null
)

@kotlinx.serialization.Serializable
data class DashboardPipelineStage(
    val stage: String = "",
    val count: Int = 0
)

@kotlinx.serialization.Serializable
data class DashboardCreditReview(
    val id: String = "",
    val ref_no: String = "",
    val applicant_name: String = "",
    val loan_type: String = "",
    val amount: Double? = null,
    val exception_count: Int = 0
)

@kotlinx.serialization.Serializable
data class DashboardOcrException(
    val id: String = "",
    val loan_id: String = "",
    val applicant_name: String = "",
    val doc_type: String = "",
    val field_name: String = "",
    val confidence: Double? = null
)

@kotlinx.serialization.Serializable
data class DashboardVisit(
    val loan_id: String = "",
    val ref_no: String = "",
    val applicant_name: String = "",
    val amount: Double? = null,
    val stage: String = "",
    val application_date: String = ""
)

@kotlinx.serialization.Serializable
data class DashboardData(
    val metrics: RelationshipOfficerMetrics = RelationshipOfficerMetrics(),
    val tasks: List<DashboardTask> = emptyList(),
    val queue: List<DashboardQueueEntry> = emptyList(),
    val visits_due: List<DashboardVisit> = emptyList(),
    val signoffs: List<DashboardSignoff> = emptyList(),
    val pipeline: List<DashboardPipelineStage> = emptyList(),
    val reviews: List<DashboardCreditReview> = emptyList(),
    val exceptions: List<DashboardOcrException> = emptyList(),
    val crm_queue: List<DashboardQueueEntry> = emptyList(),
    val ed_queue: List<DashboardQueueEntry> = emptyList(),
    val md_queue: List<DashboardQueueEntry> = emptyList(),
    val legal_queue: List<DashboardQueueEntry> = emptyList()
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
    val active: Boolean = true,
    val organization_name: String? = null,
    val branch_name: String? = null,
    val last_activity_at: String? = null
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
    private val baseUrl: String,
    private val sessionStore: com.fieldcrm.android.core.session.SessionStore
) : MobileApiService {
    private val refreshMutex = Mutex()

    private fun checkAdminDenial() {
        if (sessionStore.load()?.role == com.fieldcrm.android.core.session.UserRole.SYSTEM_ADMIN) {
            throw IllegalStateException("403 Forbidden: System Admin cannot access loan processing endpoints")
        }
    }

    private var token: String? = null

    override fun setToken(token: String) {
        this.token = token
    }

    private fun HttpRequestBuilder.authHeader() {
        token?.let {
            header(HttpHeaders.Authorization, "Bearer $it")
        }
        if (sessionStore.load()?.role == com.fieldcrm.android.core.session.UserRole.SYSTEM_ADMIN) {
            val path = url.encodedPath
            val allowedPaths = setOf(
                "/api/v1/mobile/me",
                "/api/v1/mobile/dashboard",
                "/api/v1/mobile/system-activity",
                "/api/v1/mobile/users",
                "/api/v1/mobile/config",
                "/api/v1/mobile/faqs",
                "/api/v1/mobile/onboarding"
            )
            val isAllowed = allowedPaths.any { path.startsWith(it) } || path.startsWith("/api/v1/auth")
            if (!isAllowed) {
                throw IllegalStateException("403 Forbidden: System Admin cannot access loan processing endpoints ($path)")
            }
        }
    }

    private suspend inline fun <reified T> resultOf(crossinline request: suspend () -> HttpResponse): ApiResult<T> {
        return try {
            val response = request()
            val resolved = if (response.status == HttpStatusCode.Unauthorized && refreshAccessToken()) request() else response
            if (resolved.status.value in 200..299) {
                ApiResult.Success(resolved.body<T>(), resolved.status.value)
            } else {
                ApiResult.Error(resolved.status.value, resolved.bodyAsText())
            }
        } catch (e: java.io.IOException) {
            ApiResult.NetworkError(e.message ?: "Network unavailable", e)
        } catch (e: Exception) {
            ApiResult.NetworkError(e.message ?: "Request failed", e)
        }
    }

    private suspend fun refreshAccessToken(): Boolean = refreshMutex.withLock {
        val refresh = sessionStore.refreshToken() ?: return@withLock false
        try {
            val response = client.post("$baseUrl/api/v1/auth/refresh-mobile") {
                contentType(ContentType.Application.Json)
                setBody(RefreshTokenRequest(refresh))
            }
            if (response.status != HttpStatusCode.OK) {
                sessionStore.clear()
                false
            } else {
                val bundle = response.body<TokenResponse>()
                setToken(bundle.access_token)
                sessionStore.saveTokenBundle(bundle.access_token, bundle.refresh_token, bundle.access_expires_in, bundle.session_expires_at)
                true
            }
        } catch (_: Exception) { false }
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
                sessionStore.saveTokenBundle(tokenResponse.access_token, tokenResponse.refresh_token, tokenResponse.access_expires_in, tokenResponse.session_expires_at)
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
                    sessionStore.saveTokenBundle(tokenResponse.access_token, tokenResponse.refresh_token, tokenResponse.access_expires_in, tokenResponse.session_expires_at)
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

    override suspend fun revokeSession(refreshToken: String?): Boolean = try {
        val response = client.post("$baseUrl/api/v1/auth/logout-mobile") {
            contentType(ContentType.Application.Json)
            setBody(LogoutTokenRequest(refreshToken))
        }
        response.status.value in 200..299
    } catch (_: Exception) { false }

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

    override suspend fun searchExistingCustomers(query: String, limit: Int) = resultOf<ExistingCustomerSearchResponse> {
        client.get("$baseUrl/api/v1/mobile/borrowers/search") {
            authHeader()
            parameter("q", query)
            parameter("limit", limit)
        }
    }

    override suspend fun getApplicationProfile(borrowerId: String) = resultOf<ApplicationProfileResponse> {
        client.get("$baseUrl/api/v1/mobile/borrowers/$borrowerId/application-profile") { authHeader() }
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

    override suspend fun fetchDocumentPreview(url: String): ByteArray? {
        return try {
            val resolvedUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "$baseUrl${if (url.startsWith('/')) url else "/$url"}"
            val response: HttpResponse = client.get(resolvedUrl) { authHeader() }
            if (response.status == HttpStatusCode.OK) response.body<ByteArray>() else null
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getOcrFields(id: String): OcrFieldsResponse? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/v1/mobile/applications/$id/ocr-fields") {
                authHeader()
            }
            if (response.status == HttpStatusCode.OK) response.body() else null
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
            val authorizationResponse: HttpResponse = client.post(
                "$baseUrl/api/v1/mobile/applications/$id/documents/upload-authorizations"
            ) {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(
                    DirectUploadAuthorizationRequest(
                        filename = fileName,
                        mime_type = contentType.toString(),
                        size_bytes = fileBytes.size,
                        doc_type = category
                    )
                )
            }
            if (authorizationResponse.status == HttpStatusCode.ServiceUnavailable) {
                return uploadDocumentThroughServer(id, category, fileBytes, fileName, contentType)
            }
            if (authorizationResponse.status != HttpStatusCode.OK) return null
            val authorization = authorizationResponse.body<DirectUploadAuthorizationEnvelope>().authorization
            val cloudResponse: HttpResponse = client.submitFormWithBinaryData(
                url = authorization.upload_url,
                formData = formData {
                    authorization.fields.forEach { (key, value) -> append(key, value) }
                    append("file", fileBytes, io.ktor.http.Headers.build {
                        append(HttpHeaders.ContentType, contentType.toString())
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"$fileName\"")
                    })
                }
            )
            if (cloudResponse.status.value !in 200..299) return null
            val cloud = cloudResponse.body<CloudinaryUploadResponse>()
            val finalizeResponse: HttpResponse = client.post(
                "$baseUrl/api/v1/mobile/applications/$id/documents/finalize"
            ) {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(DirectUploadFinalizeRequest(
                    authorization.intent_id, cloud.public_id, cloud.version, cloud.signature
                ))
            }
            if (finalizeResponse.status.value in 200..299) finalizeResponse.bodyAsText() else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun uploadDocumentThroughServer(
        id: String,
        category: String,
        fileBytes: ByteArray,
        fileName: String,
        contentType: ContentType
    ): String? {
        return try {
            val crmCategories = setOf(
                "offer_acceptance", "disbursement_mandate", "direct_debit_mandate",
                "insurance_certificate", "legal_clearance", "other_crm"
            )
            val isCrmCategory = category in crmCategories
            val response: HttpResponse = client.submitFormWithBinaryData(
                url = "$baseUrl/api/v1/mobile/applications/$id/${if (isCrmCategory) "crm-documents" else "documents"}",
                formData = formData {
                    append(if (isCrmCategory) "category" else "doc_type", category)
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

    override suspend fun approveApplication(id: String, notes: String, kycAttested: Boolean, collateralAttested: Boolean): String? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/v1/mobile/applications/$id/approve") {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "notes" to notes,
                    "kyc_attested" to kycAttested,
                    "collateral_attested" to collateralAttested
                ))
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

    override suspend fun getGlobalAuditTrail(): List<AuditTrailEvent> {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/v1/mobile/audit-trail") { authHeader() }
            if (response.status == HttpStatusCode.OK) response.body() else emptyList()
        } catch (_: Exception) { emptyList() }
    }

    override suspend fun getBureauData(applicationId: String): BureauData? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/v1/mobile/applications/$applicationId/bureau") {
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

    override suspend fun submitCrmReview(id: String, decision: String, notes: String, bureau1: Boolean, bureau2: Boolean, crms: Boolean, ncr: Boolean): String? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/v1/mobile/applications/$id/crm-review") {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "decision" to decision, "notes" to notes,
                    "bureau_1_verified" to bureau1, "bureau_2_verified" to bureau2,
                    "crms_verified" to crms, "ncr_verified" to ncr
                ))
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
        return uploadDocument(id, category, pdfBytes, fileName)
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

    override suspend fun advanceWorkflow(id: String, notes: String): WorkflowAdvanceResponse? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/v1/mobile/applications/$id/workflow/advance") {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(WorkflowAdvanceRequest(notes))
            }
            if (response.status == HttpStatusCode.OK) response.body() else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun listUsers() = resultOf<List<MobileUserItem>> {
        client.get("$baseUrl/api/v1/mobile/users") { authHeader() }
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

    override suspend fun pullCreditBureau(id: String) = resultOf<JsonElement> {
        client.post("$baseUrl/api/v1/mobile/applications/$id/credit-bureau-pull") { authHeader() }
    }

    override suspend fun getCreditChecklist(id: String, context: String) = resultOf<CreditChecklistResponse> {
        client.get("$baseUrl/api/v1/mobile/applications/$id/credit-checklist") {
            authHeader()
            parameter("context", context)
        }
    }

    override suspend fun updateCreditChecklist(id: String, request: CreditChecklistUpdateRequest) =
        resultOf<CreditChecklistItem> {
            client.patch("$baseUrl/api/v1/mobile/applications/$id/credit-checklist") {
                authHeader(); contentType(ContentType.Application.Json); setBody(request)
            }
        }

    override suspend fun generateClientLink(id: String) = resultOf<SigningLinkResponse> {
        client.post("$baseUrl/api/v1/mobile/applications/$id/client-link") { authHeader() }
    }

    override suspend fun generateGuarantorLink(id: String, slot: Int) = resultOf<SigningLinkResponse> {
        client.post("$baseUrl/api/v1/mobile/applications/$id/guarantor-link/$slot") { authHeader() }
    }

    override suspend fun getOffer(id: String) = resultOf<OfferReadinessResponse> {
        client.get("$baseUrl/api/v1/mobile/applications/$id/offer") { authHeader() }
    }

    override suspend fun generateOffer(id: String) = resultOf<JsonElement> {
        client.post("$baseUrl/api/v1/mobile/applications/$id/offer") { authHeader() }
    }

    override suspend fun getDisbursement(id: String) = resultOf<JsonElement> {
        client.get("$baseUrl/api/v1/mobile/applications/$id/disbursement") { authHeader() }
    }

    override suspend fun recordDisbursement(id: String, request: DisbursementRequest) = resultOf<JsonElement> {
        client.post("$baseUrl/api/v1/mobile/applications/$id/disbursement") {
            authHeader(); contentType(ContentType.Application.Json); setBody(request)
        }
    }

    override suspend fun changePassword(current: String, new: String, confirm: String) =
        resultOf<ChangePasswordResponse> {
            client.post("$baseUrl/api/v1/mobile/settings/change-password") {
                authHeader()
                contentType(ContentType.Application.Json)
                setBody(ChangePasswordRequest(current, new, confirm))
            }
        }

    override suspend fun getSystemActivity(page: Int, size: Int) = resultOf<SystemActivityResponse> {
        client.get("$baseUrl/api/v1/mobile/system-activity") {
            authHeader(); parameter("page", page); parameter("size", size)
        }
    }

    override suspend fun getLegalQueue(page: Int) = resultOf<JsonElement> {
        client.get("$baseUrl/api/v1/mobile/queues/legal") { authHeader(); parameter("page", page) }
    }
    override suspend fun getValuation(id: String) = resultOf<JsonElement> {
        client.get("$baseUrl/api/v1/mobile/applications/$id/valuation") { authHeader() }
    }
    override suspend fun updateValuation(id: String, payload: JsonElement) = resultOf<JsonElement> {
        client.put("$baseUrl/api/v1/mobile/applications/$id/valuation") {
            authHeader(); contentType(ContentType.Application.Json); setBody(payload)
        }
    }
    override suspend fun getMcc(page: Int) = resultOf<JsonElement> {
        client.get("$baseUrl/api/v1/mobile/mcc") { authHeader(); parameter("page", page) }
    }
    override suspend fun getMccApplication(id: String) = resultOf<JsonElement> {
        client.get("$baseUrl/api/v1/mobile/applications/$id/mcc") { authHeader() }
    }
    override suspend fun submitMccVote(id: String, amount: Double, notes: String) = resultOf<JsonElement> {
        client.post("$baseUrl/api/v1/mobile/applications/$id/mcc-vote") {
            authHeader(); contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("recommended_amount", amount); put("notes", notes) })
        }
    }
    override suspend fun finalizeMcc(id: String, amount: Double) = resultOf<JsonElement> {
        client.post("$baseUrl/api/v1/mobile/applications/$id/mcc-finalize") {
            authHeader(); contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("final_amount", amount) })
        }
    }
    override suspend fun getInterestPresets() = resultOf<JsonElement> {
        client.get("$baseUrl/api/v1/mobile/admin/interest-presets") { authHeader() }
    }
    override suspend fun createInterestPreset(loanType: String, rate: Double, rateType: String) = resultOf<JsonElement> {
        client.post("$baseUrl/api/v1/mobile/admin/interest-presets") {
            authHeader(); contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("loan_type", loanType); put("rate", rate); put("rate_type", rateType)
            })
        }
    }
    override suspend fun deleteInterestPreset(id: String) = resultOf<JsonElement> {
        client.delete("$baseUrl/api/v1/mobile/admin/interest-presets/$id") { authHeader() }
    }
    override suspend fun updateInterestPreset(id: String, loanType: String, rate: Double, rateType: String) =
        resultOf<JsonElement> {
            client.put("$baseUrl/api/v1/mobile/admin/interest-presets/$id") {
                authHeader(); contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("loan_type", loanType); put("rate", rate); put("rate_type", rateType)
                })
            }
        }
    override suspend fun getBranches() = resultOf<JsonElement> {
        client.get("$baseUrl/api/v1/mobile/branches") { authHeader() }
    }
    override suspend fun createBranch(name: String, code: String) = resultOf<JsonElement> {
        client.post("$baseUrl/api/v1/mobile/branches") {
            authHeader(); contentType(ContentType.Application.Json); setBody(mapOf("name" to name, "code" to code))
        }
    }
    override suspend fun updateUserRole(id: String, role: String, branchId: String?) = resultOf<JsonElement> {
        client.put("$baseUrl/api/v1/mobile/users/$id/role") {
            authHeader(); contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("role", role)
                if (branchId != null) put("branch_id", branchId)
            })
        }
    }
    override suspend fun deactivateUser(id: String) = resultOf<JsonElement> {
        client.post("$baseUrl/api/v1/mobile/users/$id/deactivate") { authHeader() }
    }
    override suspend fun getParLoans(page: Int) = resultOf<JsonElement> {
        client.get("$baseUrl/api/v1/mobile/reports/par/loans") { authHeader(); parameter("page", page) }
    }
    override suspend fun getRoleDashboard(role: String) = resultOf<JsonElement> {
        client.get("$baseUrl/api/v1/mobile/dashboards/$role") { authHeader() }
    }
}
