package com.fieldcrm.android.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
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
            
            // Build Ktor Client pointing to backend server
            val client = FieldCRMClient(baseUrl = "http://10.0.2.2:8000") // standard Android emulator localhost loopback
            
            val repository = SyncRepository(database, client)
            
            // Execute chronological offline queue synchronization replay
            val success = repository.syncQueueWithServer()
            
            if (success) {
                Result.success()
            } else {
                Result.retry() // Reschedules to execute on subsequent network trigger
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
