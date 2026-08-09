package com.fieldcrm.android.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.fieldcrm.android.core.session.SessionStore
import com.fieldcrm.android.data.api.MobileApiServiceImpl
import com.fieldcrm.shared.api.FieldCRMClient
import com.fieldcrm.shared.db.AppDatabase
import com.fieldcrm.shared.repository.SyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.fieldcrm.android.BuildConfig

class AndroidSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Instantiate Android SQLDelight SQLite driver
            val driver = AndroidSqliteDriver(
                schema = AppDatabase.Schema,
                context = applicationContext,
                name = "fieldcrm_offline.db"
            )
            
            val database = AppDatabase(driver)
            
            val session = SessionStore(applicationContext).load() ?: return@withContext Result.failure()

            // Build Ktor client pointing to the deployed online backend.
            val client = FieldCRMClient(baseUrl = BuildConfig.API_BASE_URL)
            client.setToken(session.token)

            val mobileApi = MobileApiServiceImpl(client.httpClient, BuildConfig.API_BASE_URL, SessionStore(applicationContext))
            mobileApi.setToken(session.token)
            if (mobileApi.getMe() == null) {
                return@withContext Result.failure()
            }
            
            val repository = SyncRepository(database, client)

            // Phase 1 — PUSH: replay local offline queue to server
            val pushSuccess = repository.syncQueueWithServer()

            // Phase 2 — PULL: refresh local cache with latest server state
            try {
                val applications = client.fetchApplications()
                for (a in applications) {
                    database.appDatabaseQueries.insertApplication(
                        id = a.id,
                        org_id = a.org_id,
                        ref_no = a.ref_no,
                        customer_type = a.customer_type,
                        loan_type = a.loan_type,
                        stage = a.stage,
                        applicant_name = a.applicant_name,
                        bvn = a.bvn,
                        phone = a.phone,
                        amount = a.amount,
                        tenor_months = a.tenor_months?.toLong(),
                        purpose = a.purpose,
                        repayment_mode = a.repayment_mode,
                        created_by = a.created_by,
                        current_owner_id = a.current_owner_id,
                        credit_officer_id = a.credit_officer_id,
                        branch_manager_id = a.branch_manager_id,
                        return_reason = a.return_reason,
                        approved_by = a.approved_by,
                        approved_at = a.approved_at,
                        disbursed_at = a.disbursed_at,
                        interest_rate = a.interest_rate,
                        repayment_frequency = a.repayment_frequency,
                        schedule_method = a.schedule_method,
                        classification = a.classification,
                        days_past_due = a.days_past_due.toLong(),
                        crm_notes = a.crm_notes,
                        crm_reviewed_by = a.crm_reviewed_by,
                        executive_approved_by = a.executive_approved_by,
                        created_at = a.created_at,
                        updated_at = a.updated_at
                    )
                }

                // Refresh only dossiers the user has opened before. This
                // keeps stored review data current without interrupting the
                // visible screen or downloading every loan dossier at once.
                val staleBefore = System.currentTimeMillis() - DETAIL_REFRESH_INTERVAL_MILLIS
                val cachedDetailIds = database.appDatabaseQueries
                    .selectStaleReviewDetailCacheIds(staleBefore, DETAIL_REFRESH_BATCH_SIZE)
                    .executeAsList()
                for (applicationId in cachedDetailIds) {
                    val payload = mobileApi.getApplicationDetail(applicationId) ?: continue
                    database.appDatabaseQueries.upsertReviewDetailCache(
                        applicationId,
                        payload,
                        System.currentTimeMillis()
                    )
                }
            } catch (_: Exception) {
                // Pull failure is non-fatal — cached data is still valid
            }

            // Phase 3 — UPLOAD: push pending encrypted documents to backend
            try {
                val docRepo = com.fieldcrm.android.data.repository.LocalDocumentRepository(applicationContext, database)
                val pending = docRepo.getPendingUploads()
                for (doc in pending) {
                    val bytes = doc.decryptWith(docRepo.getStore()) ?: continue
                    val ok = mobileApi.uploadDocument(
                        id = doc.loanId,
                        category = doc.docType,
                        fileBytes = bytes,
                        fileName = doc.filename
                    )
                    if (ok != null) docRepo.markSynced(doc.id)
                }
            } catch (_: Exception) {
                // Document upload failure is non-fatal
            }

            if (pushSuccess) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "AndroidSyncWorker"
        private const val DETAIL_REFRESH_INTERVAL_MILLIS = 15L * 60L * 1000L
        private const val DETAIL_REFRESH_BATCH_SIZE = 10L

        fun scheduleOneTime(context: Context) {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()

            val workRequest = androidx.work.OneTimeWorkRequestBuilder<AndroidSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .build()

            androidx.work.WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "AndroidSyncWorker_OneTime_" + java.util.UUID.randomUUID().toString(),
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    workRequest
                )
        }

        fun schedulePeriodic(context: Context) {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()

            val workRequest = androidx.work.PeriodicWorkRequestBuilder<AndroidSyncWorker>(
                15, java.util.concurrent.TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .build()

            androidx.work.WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
        }
    }
}
