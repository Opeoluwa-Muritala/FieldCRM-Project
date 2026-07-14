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
            applicant_name = borrower.name,
            phone = borrower.phone,
            bvn = borrower.bvn,
            stage = "intake",
            loan_type = "other",
            customer_type = "new",
            created_by = borrower.loan_officer_id,
            current_owner_id = borrower.loan_officer_id,
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
            loan_officer_id = current_owner_id ?: created_by,
            name = applicant_name,
            phone = phone ?: "",
            bvn = bvn ?: "",
            nin = "",
            photo_url = null,
            status = if (stage == "rejected") "INACTIVE" else "ACTIVE",
            created_at = created_at
        )
    }

    private fun com.fieldcrm.shared.db.LoanApplication.toBorrowerModel(): BorrowerModel {
        return BorrowerModel(
            id = id,
            org_id = org_id,
            loan_officer_id = current_owner_id ?: created_by,
            name = applicant_name,
            phone = phone ?: "",
            bvn = bvn ?: "",
            nin = "",
            photo_url = null,
            status = if (stage == "rejected") "INACTIVE" else "ACTIVE",
            created_at = created_at
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
    override suspend fun generateShareLink(): com.fieldcrm.android.data.api.ShareLinkResponse? = null
    override suspend fun getApplicationDetail(id: String): String? = null
    override suspend fun saveIntakeStep(id: String, step: Int, data: Map<String, JsonElement>): String? = null
    override suspend fun getGuarantorData(id: String, slot: Int): String? = null
    override suspend fun saveGuarantorStep(id: String, slot: Int, step: Int, data: Map<String, JsonElement>): String? = null
    override suspend fun uploadDocument(id: String, category: String, fileBytes: ByteArray?, fileName: String): String? = null
    override suspend fun getOcrFields(id: String): com.fieldcrm.android.data.api.OcrFieldsResponse? = null
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
    override suspend fun getCommitteeVotesFull(applicationId: String): com.fieldcrm.android.data.api.CommitteeVotesFullResponse? = null
    override suspend fun submitCommitteeVote(id: String, recommendation: String, notes: String): String? = null
    override suspend fun completeCommitteeReview(id: String, recommendation: String): String? = null
    override suspend fun getEdReview(id: String): String? = null
    override suspend fun submitEdApprove(id: String, action: String): String? = null
    override suspend fun getMdReview(id: String): String? = null
    override suspend fun submitMdApprove(id: String, action: String, notes: String): String? = null
    override suspend fun addBoardReferral(id: String, email: String, name: String, notes: String): String? = null
    override suspend fun advanceWorkflow(id: String, notes: String): com.fieldcrm.android.data.api.WorkflowAdvanceResponse? = null
    override suspend fun listUsers() = emptyList<com.fieldcrm.android.data.api.MobileUserItem>()
    override suspend fun createUser(fullName: String, email: String, role: String, password: String) = false
}

class ApplicationRepository(
    private val database: AppDatabase,
    private val client: FieldCRMClient,
    private val apiService: MobileApiService
) {
    private val queries = database.appDatabaseQueries
    private val syncRepository = SyncRepository(database, client)

    fun getCachedApplications(): List<LoanApplicationModel> {
        return queries.selectAllApplications().executeAsList().map { it.toModel() }
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

            // If intake lacks flat guarantor keys, fetch from guarantor endpoint and merge
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
                                "full_name" -> "${prefix}name"
                                "phone" -> "${prefix}phone"
                                "home_address" -> "${prefix}address"
                                "bvn" -> "${prefix}bvn"
                                "bank_name" -> "${prefix}bank"
                                "account_number" -> "${prefix}account"
                                "employment_type" -> "${prefix}employment_type"
                                "monthly_salary" -> "${prefix}monthly_salary"
                                "relationship_to_client" -> "${prefix}relationship"
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
            for (app in remote) { queries.upsert(app) }
            remote
        } catch (e: Exception) {
            getCachedApplications()
        }
    }

    suspend fun generateShareLink(): com.fieldcrm.android.data.api.ShareLinkResponse? {
        return apiService.generateShareLink()
    }

    suspend fun createApplication(application: LoanApplicationModel): Boolean {
        queries.upsert(application)
        return try {
            client.createApplication(application)
            true
        } catch (e: Exception) {
            queries.insertQueueItem(
                id = java.util.UUID.randomUUID().toString(),
                action = "CREATE_APPLICATION",
                entity_id = application.id,
                payload_json = """{"id":"${application.id}","applicant_name":"${application.applicant_name}","loan_type":"${application.loan_type}","amount":${application.amount}}""",
                timestamp = System.currentTimeMillis(),
                attempts = 0
            )
            true
        }
    }

    suspend fun syncWithServer(): Boolean {
        return try {
            if (apiService.getMe() == null) return false

            val pushSuccess = syncRepository.syncQueueWithServer()

            try {
                val apps = client.fetchApplications()
                for (a in apps) { queries.upsert(a) }
            } catch (_: Exception) {}

            pushSuccess
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getApplicationById(id: String): LoanApplicationModel? {
        return try {
            client.fetchApplications().find { it.id == id }
        } catch (e: Exception) {
            queries.selectApplicationById(id).executeAsOneOrNull()?.toModel()
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

    suspend fun submitIntakeToServer(
        id: String,
        amount: Double,
        tenorMonths: Int,
        loanType: String,
        purpose: String
    ): Boolean {
        return apiService.saveIntakeStep(id, 6, mapOf(
            "amount" to JsonPrimitive(amount),
            "tenor_months" to JsonPrimitive(tenorMonths),
            "loan_type" to JsonPrimitive(loanType),
            "purpose" to JsonPrimitive(purpose)
        )) != null
    }

    suspend fun patchApplicationMeta(id: String, purpose: String, pledgeValue: Double): Boolean {
        return apiService.saveIntakeStep(id, 8, mapOf(
            "purpose" to JsonPrimitive(purpose),
            "pledge_value" to JsonPrimitive(pledgeValue)
        )) != null
    }

    private fun com.fieldcrm.shared.db.AppDatabaseQueries.upsert(app: LoanApplicationModel) {
        insertApplication(
            id = app.id,
            org_id = app.org_id,
            ref_no = app.ref_no,
            customer_type = app.customer_type,
            loan_type = app.loan_type,
            stage = app.stage,
            applicant_name = app.applicant_name,
            bvn = app.bvn,
            phone = app.phone,
            amount = app.amount,
            tenor_months = app.tenor_months?.toLong(),
            purpose = app.purpose,
            repayment_mode = app.repayment_mode,
            created_by = app.created_by,
            current_owner_id = app.current_owner_id,
            credit_officer_id = app.credit_officer_id,
            branch_manager_id = app.branch_manager_id,
            return_reason = app.return_reason,
            approved_by = app.approved_by,
            approved_at = app.approved_at,
            disbursed_at = app.disbursed_at,
            interest_rate = app.interest_rate,
            repayment_frequency = app.repayment_frequency,
            schedule_method = app.schedule_method,
            classification = app.classification,
            days_past_due = app.days_past_due.toLong(),
            crm_notes = app.crm_notes,
            crm_reviewed_by = app.crm_reviewed_by,
            executive_approved_by = app.executive_approved_by,
            created_at = app.created_at,
            updated_at = app.updated_at
        )
    }

    private fun com.fieldcrm.shared.db.LoanApplication.toModel() = LoanApplicationModel(
        id = id,
        org_id = org_id,
        ref_no = ref_no,
        customer_type = customer_type,
        loan_type = loan_type,
        stage = stage,
        applicant_name = applicant_name,
        bvn = bvn,
        phone = phone,
        amount = amount,
        tenor_months = tenor_months?.toInt(),
        purpose = purpose,
        repayment_mode = repayment_mode,
        created_by = created_by,
        current_owner_id = current_owner_id,
        credit_officer_id = credit_officer_id,
        branch_manager_id = branch_manager_id,
        return_reason = return_reason,
        approved_by = approved_by,
        approved_at = approved_at,
        disbursed_at = disbursed_at,
        interest_rate = interest_rate,
        repayment_frequency = repayment_frequency,
        schedule_method = schedule_method,
        classification = classification,
        days_past_due = days_past_due.toInt(),
        crm_notes = crm_notes,
        crm_reviewed_by = crm_reviewed_by,
        executive_approved_by = executive_approved_by,
        created_at = created_at,
        updated_at = updated_at
    )
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

    fun applyStoredToken(token: String) {
        client.setToken(token)
        apiService.setToken(token)
    }

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
