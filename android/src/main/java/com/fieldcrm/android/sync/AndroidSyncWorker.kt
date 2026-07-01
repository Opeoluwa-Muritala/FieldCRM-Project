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
            val client = FieldCRMClient(baseUrl = "https://fieldcrm.onrender.com")
            client.setToken(session.token)

            val mobileApi = MobileApiServiceImpl(client.client, "https://fieldcrm.onrender.com")
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
                        borrower_id = a.borrower_id,
                        applicant_name = a.applicant_name,
                        org_id = a.org_id,
                        current_stage = a.current_stage.toLong(),
                        current_owner_id = a.current_owner_id,
                        status = a.status,
                        amount = a.amount,
                        tenure = a.tenure.toLong(),
                        product_type = a.product_type
                    )
                }
            } catch (_: Exception) {
                // Pull failure is non-fatal — cached data is still valid
            }

            if (pushSuccess) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
