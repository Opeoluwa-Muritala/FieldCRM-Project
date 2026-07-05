package com.fieldcrm.android.data.repository

import com.fieldcrm.shared.api.FieldCRMClient
import com.fieldcrm.shared.db.AppDatabase
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel
import com.fieldcrm.shared.repository.SyncRepository
import com.fieldcrm.android.data.api.MobileApiService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

data class ApplicationDetailResult(
    val readiness: Map<String, Any> = emptyMap(),
    val documents: List<Map<String, Any>> = emptyList(),
    val intake: Map<String, Any> = emptyMap(),
    val visitation: Map<String, Any> = emptyMap()
)

class BorrowerRepository(
    private val database: AppDatabase,
    private val client: FieldCRMClient
) {
    private val queries = database.appDatabaseQueries

    suspend fun getAllBorrowers(): List<BorrowerModel> {
        return try {
            client.fetchApplications().map { it.toBorrowerModel() }
        } catch (e: Exception) {
            queries.selectAllApplications().executeAsList().map { row ->
                row.toBorrowerModel()
            }.distinctBy { it.id }
        }
    }

    suspend fun createBorrower(borrower: BorrowerModel): Boolean {
        val application = LoanApplicationModel(
            id = borrower.id,
            org_id = borrower.org_id,
            borrower_id = borrower.id,
            applicant_name = borrower.name,
            current_stage = 1,
            current_owner_id = borrower.loan_officer_id,
            status = "Draft",
            amount = 0.0,
            tenure = 0,
            product_type = "other",
            interest_rate = 15.0,
            repayment_frequency = "Monthly",
            created_at = borrower.created_at
        )
        return ApplicationRepository(database, client, apiService = NoopMobileApiService).createApplication(application)
    }

    suspend fun getBorrowerById(id: String): BorrowerModel? {
        return getAllBorrowers().find { it.id == id }
    }

    private fun LoanApplicationModel.toBorrowerModel(): BorrowerModel {
        return BorrowerModel(
            id = id,
            org_id = org_id,
            loan_officer_id = current_owner_id,
            name = applicant_name,
            phone = "",
            bvn = "",
            nin = "",
            photo_url = null,
            status = if (status == "Rejected") "INACTIVE" else "ACTIVE",
            created_at = created_at
        )
    }

    private fun com.fieldcrm.shared.db.LoanApplication.toBorrowerModel(): BorrowerModel {
        return BorrowerModel(
            id = id,
            org_id = org_id,
            loan_officer_id = current_owner_id,
            name = applicant_name,
            phone = "",
            bvn = "",
            nin = "",
            photo_url = null,
            status = if (status == "Rejected") "INACTIVE" else "ACTIVE",
            created_at = ""
        )
    }
}

private object NoopMobileApiService : MobileApiService {
    override fun setToken(token: String) = Unit
    override suspend fun login(username: String, password: String) = null
    override suspend fun loginWithResult(username: String, password: String) = com.fieldcrm.android.data.api.LoginOutcome.NetworkError
    override suspend fun getMe() = null
    override suspend fun getDashboard(): String? = null
    override suspend fun getDashboardMetrics() = null
    override suspend fun getQueue(queueName: String): String? = null
    override suspend fun createApplication(customerType: String, loanType: String, applicantName: String): String? = null
    override suspend fun getApplicationDetail(id: String): String? = null
    override suspend fun saveIntakeStep(id: String, step: Int, data: Map<String, JsonElement>): String? = null
    override suspend fun getGuarantorData(id: String, slot: Int): String? = null
    override suspend fun saveGuarantorStep(id: String, slot: Int, step: Int, data: Map<String, JsonElement>): String? = null
    override suspend fun uploadDocument(id: String, category: String, fileBytes: ByteArray?, fileName: String): String? = null
    override suspend fun submitOcrReview(id: String, corrections: Map<String, String>): String? = null
    override suspend fun getVisitationReport(id: String): String? = null
    override suspend fun submitVisitationReport(id: String, metWith: String, premises: String, direction: String): String? = null
    override suspend fun submitVisitationSignoff(id: String, decision: String, notes: String): String? = null
    override suspend fun submitCreditReview(id: String, decision: String, notes: String): String? = null
    override suspend fun submitCrmReview(id: String, decision: String, notes: String): String? = null
    override suspend fun approveApplication(id: String): String? = null
    override suspend fun returnApplication(id: String, reason: String, corrections: List<String>, notes: String): String? = null
    override suspend fun getBorrowers(): String? = null
    override suspend fun createBorrower(data: Map<String, JsonElement>): String? = null
    override suspend fun getNotifications() = emptyList<com.fieldcrm.android.data.api.ApiNotification>()
    override suspend fun markNotificationRead(id: String) = false
    override suspend fun clearNotifications() = false
    override suspend fun getConfig() = null
    override suspend fun search(query: String) = null
    override suspend fun getAuditTrail(applicationId: String) = emptyList<com.fieldcrm.android.data.api.AuditTrailEvent>()
    override suspend fun getBureauData(applicationId: String) = null
    override suspend fun getCommitteeVotes(applicationId: String) = null
    override suspend fun getAuditChecklist(applicationId: String) = null
    override suspend fun saveAuditChecklist(applicationId: String, checklist: com.fieldcrm.android.data.api.AuditChecklist) = false
    override suspend fun getFaqs() = emptyList<com.fieldcrm.android.data.api.FaqItem>()
    override suspend fun getOnboarding(role: String) = emptyList<com.fieldcrm.android.data.api.OnboardingSlide>()
    override suspend fun forgotPassword(email: String): Boolean = false
    override suspend fun resetPassword(token: String, newPassword: String): Boolean = false
    override suspend fun submitExecutiveApprove(id: String): String? = null
    override suspend fun getRepaymentSchedule(id: String): com.fieldcrm.android.data.api.RepaymentScheduleResponse? = null
    override suspend fun recordPayment(id: String, amountPaid: Double, channel: String, bankRef: String?, paymentDate: String?): String? = null
    override suspend fun getParDashboard(): String? = null
    override suspend fun uploadDocumentPdf(id: String, category: String, pdfBytes: ByteArray, fileName: String): String? = null
}

class ApplicationRepository(
    private val database: AppDatabase,
    private val client: FieldCRMClient,
    private val apiService: MobileApiService
) {
    private val queries = database.appDatabaseQueries
    private val syncRepository = SyncRepository(database, client)

    fun getCachedApplications(): List<LoanApplicationModel> {
        return queries.selectAllApplications().executeAsList().map { row ->
            LoanApplicationModel(
                id = row.id,
                borrower_id = row.borrower_id,
                applicant_name = row.applicant_name,
                org_id = row.org_id,
                current_stage = row.current_stage.toInt(),
                current_owner_id = row.current_owner_id,
                status = row.status,
                amount = row.amount,
                tenure = row.tenure.toInt(),
                product_type = row.product_type,
                interest_rate = 15.0,
                repayment_frequency = "Monthly",
                created_at = ""
            )
        }
    }

    suspend fun getFullDetail(id: String): ApplicationDetailResult? {
        val json = apiService.getApplicationDetail(id) ?: return null
        return try {
            val root = Json.parseToJsonElement(json).jsonObject

            fun safeValue(el: JsonElement): Any? = when (el) {
                is JsonNull -> null
                is JsonPrimitive -> el.booleanOrNull ?: el.intOrNull ?: el.doubleOrNull ?: el.content
                is JsonObject -> el["value"]?.let { inner ->
                    when (inner) {
                        is JsonNull -> null
                        is JsonPrimitive -> inner.booleanOrNull ?: inner.intOrNull ?: inner.doubleOrNull ?: inner.content
                        else -> null
                    }
                }
                else -> null
            }

            fun JsonElement.asObjEntries() = (this as? JsonObject)?.entries

            val readiness: Map<String, Any> = root["readiness"]?.asObjEntries()
                ?.mapNotNull { (k, v) -> safeValue(v)?.let { k to it } }
                ?.toMap() ?: emptyMap()

            val documents: List<Map<String, Any>> = (root["documents"] as? JsonArray)?.mapNotNull { docEl ->
                val d = docEl as? JsonObject ?: return@mapNotNull null
                val url = (d["secure_url"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
                    ?: (d["file_url"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
                    ?: (d["cloud_preview_url"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
                    ?: (d["stored_path"] as? JsonPrimitive)?.content ?: ""
                mapOf(
                    "doc_type" to ((d["doc_type"] as? JsonPrimitive)?.content ?: ""),
                    "verified" to ((d["verified"] as? JsonPrimitive)?.booleanOrNull ?: false),
                    "secure_url" to url,
                    "file_url" to url
                )
            } ?: emptyList()

            var intake: Map<String, Any> = root["intake"]?.asObjEntries()
                ?.mapNotNull { (k, v) -> safeValue(v)?.let { k to it } }
                ?.toMap() ?: emptyMap()

            // If intake lacks flat guarantor keys (web-submitted apps store guarantors separately),
            // fetch from the now-open guarantor endpoint and merge as guarantor_1_*/guarantor_2_*
            if (!intake.containsKey("guarantor_1_name")) {
                val merged = intake.toMutableMap()
                for (slot in 1..2) {
                    val gJson = apiService.getGuarantorData(id, slot) ?: continue
                    try {
                        val gRoot = Json.parseToJsonElement(gJson).jsonObject
                        val gData = gRoot["data"]?.asObjEntries() ?: continue
                        val prefix = "guarantor_${slot}_"
                        gData.forEach { (k, v) ->
                            val flatKey = when (k) {
                                "guarantor_full_name" -> "${prefix}name"
                                "phone_number" -> "${prefix}phone"
                                "residential_address" -> "${prefix}address"
                                "bvn" -> "${prefix}bvn"
                                "nin" -> "${prefix}nin"
                                "employer_name", "business_name" -> "${prefix}employer"
                                "bank_name" -> "${prefix}bank"
                                "account_number" -> "${prefix}account"
                                else -> "$prefix$k"
                            }
                            safeValue(v)?.let { merged[flatKey] = it }
                        }
                    } catch (_: Exception) {}
                }
                intake = merged
            }

            val visitation: Map<String, Any> = root["visitation"]?.asObjEntries()
                ?.mapNotNull { (k, v) -> safeValue(v)?.let { k to it } }
                ?.toMap() ?: emptyMap()

            ApplicationDetailResult(readiness, documents, intake, visitation)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAllApplications(): List<LoanApplicationModel> {
        return try {
            val remote = client.fetchApplications()
            for (app in remote) {
                queries.insertApplication(
                    id = app.id,
                    borrower_id = app.borrower_id,
                    applicant_name = app.applicant_name,
                    org_id = app.org_id,
                    current_stage = app.current_stage.toLong(),
                    current_owner_id = app.current_owner_id,
                    status = app.status,
                    amount = app.amount,
                    tenure = app.tenure.toLong(),
                    product_type = app.product_type
                )
            }
            remote
        } catch (e: Exception) {
            getCachedApplications()
        }
    }

    suspend fun createApplication(application: LoanApplicationModel): Boolean {
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
        return try {
            client.createApplication(application)
            true
        } catch (e: Exception) {
            queries.insertQueueItem(
                id = java.util.UUID.randomUUID().toString(),
                action = "CREATE_APPLICATION",
                entity_id = application.id,
                payload_json = """{"id":"${application.id}","applicant_name":"${application.applicant_name}","amount":${application.amount},"tenure":${application.tenure}}""",
                timestamp = System.currentTimeMillis(),
                attempts = 0
            )
            true
        }
    }

    suspend fun syncWithServer(): Boolean {
        return try {
            if (apiService.getMe() == null) return false

            // Phase 1 — PUSH: replay queued offline writes to the server
            val pushSuccess = syncRepository.syncQueueWithServer()

            // Phase 2 — PULL: refresh local cache with authoritative server state
            try {
                val apps = client.fetchApplications()
                for (a in apps) {
                    queries.insertApplication(
                        id = a.id, borrower_id = a.borrower_id, applicant_name = a.applicant_name, org_id = a.org_id,
                        current_stage = a.current_stage.toLong(),
                        current_owner_id = a.current_owner_id,
                        status = a.status, amount = a.amount,
                        tenure = a.tenure.toLong(), product_type = a.product_type
                    )
                }
            } catch (_: Exception) {
                // Pull failure is non-fatal — cached data remains valid
            }

            pushSuccess
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getApplicationById(id: String): LoanApplicationModel? {
        return try {
            client.fetchApplications().find { it.id == id }
        } catch (e: Exception) {
            queries.selectAllApplications().executeAsList()
                .find { it.id == id }?.let { row ->
                    LoanApplicationModel(
                        id = row.id,
                        borrower_id = row.borrower_id,
                        applicant_name = row.applicant_name,
                        org_id = row.org_id,
                        current_stage = row.current_stage.toInt(),
                        current_owner_id = row.current_owner_id,
                        status = row.status,
                        amount = row.amount,
                        tenure = row.tenure.toInt(),
                        product_type = row.product_type,
                        interest_rate = 15.0,
                        repayment_frequency = "Monthly",
                        created_at = ""
                    )
                }
        }
    }

    suspend fun approveApplication(id: String): Boolean {
        return apiService.approveApplication(id) != null
    }

    suspend fun returnApplication(id: String, reason: String, corrections: List<String> = emptyList(), notes: String): Boolean {
        return apiService.returnApplication(id, reason, corrections, notes) != null
    }

    suspend fun submitCreditReview(id: String, decision: String, notes: String): Boolean {
        return apiService.submitCreditReview(id, decision, notes) != null
    }

    suspend fun submitOcrReview(id: String, corrections: Map<String, String> = emptyMap()): Boolean {
        return apiService.submitOcrReview(id, corrections) != null
    }

    suspend fun submitVisitationToServer(id: String, metWith: String, premises: String, direction: String): Boolean {
        return apiService.submitVisitationReport(id, metWith, premises, direction) != null
    }

    fun queueStageAction(action: String, entityId: String, payloadJson: String) {
        queries.insertQueueItem(
            id = java.util.UUID.randomUUID().toString(),
            action = action,
            entity_id = entityId,
            payload_json = payloadJson,
            timestamp = System.currentTimeMillis(),
            attempts = 0
        )
    }

    suspend fun submitIntakeToServer(id: String, amount: Double, tenure: Int, productType: String, collateralDesc: String, collateralValue: Double): Boolean {
        val body = buildString {
            append("{")
            append("\"current_stage\":2,")
            append("\"status\":\"ocr_review\",")
            append("\"amount\":$amount,")
            append("\"tenure\":$tenure,")
            append("\"product_type\":\"$productType\",")
            append("\"collateral_desc\":\"$collateralDesc\",")
            append("\"collateral_value\":$collateralValue")
            append("}")
        }
        // Use saveIntakeStep for step 6 (loan request)
        return apiService.saveIntakeStep(id, 6, mapOf(
            "amount" to kotlinx.serialization.json.JsonPrimitive(amount),
            "tenure" to kotlinx.serialization.json.JsonPrimitive(tenure),
            "product_type" to kotlinx.serialization.json.JsonPrimitive(productType)
        )) != null
    }

    suspend fun patchApplicationMeta(id: String, collateralDesc: String, collateralValue: Double): Boolean {
        // Use the saveIntakeStep endpoint to push collateral data
        return apiService.saveIntakeStep(id, 8, mapOf(
            "collateral_desc" to kotlinx.serialization.json.JsonPrimitive(collateralDesc),
            "collateral_value" to kotlinx.serialization.json.JsonPrimitive(collateralValue)
        )) != null
    }
}

class AuthRepository(
    private val client: FieldCRMClient,
    private val apiService: MobileApiService
) {
    suspend fun authenticate(username: String, password: String): com.fieldcrm.android.data.api.LoginOutcome {
        val outcome = apiService.loginWithResult(username, password)
        if (outcome is com.fieldcrm.android.data.api.LoginOutcome.Success) {
            client.setToken(outcome.token)
        }
        return outcome
    }

    suspend fun fetchMe(): com.fieldcrm.android.data.api.MobileUser? {
        return try { apiService.getMe() } catch (_: Exception) { null }
    }

    // Returns null on network error (caller should treat as "still valid"),
    // false only on a definitive 401/403 from the server.
    suspend fun validateToken(token: String): Boolean? {
        return try {
            client.setToken(token)
            apiService.setToken(token)
            val me = apiService.getMe()
            if (me != null) true else false
        } catch (_: java.net.UnknownHostException) { null }
        catch (_: java.net.ConnectException) { null }
        catch (_: io.ktor.client.plugins.HttpRequestTimeoutException) { null }
        catch (_: io.ktor.client.network.sockets.ConnectTimeoutException) { null }
        catch (_: Exception) { null }
    }
}
