package com.fieldcrm.android.core.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fieldcrm.android.core.session.SessionStore
import com.fieldcrm.android.data.api.MobileApiService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class NotificationSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val apiService: MobileApiService by inject()
    private val sessionStore: SessionStore by inject()

    override suspend fun doWork(): Result {
        val session = sessionStore.load() ?: return Result.success()

        apiService.setToken(session.token)

        val notifications = apiService.getNotifications()
        val unread = notifications.filter { !it.is_read }

        if (unread.size == 1) {
            val n = unread.first()
            NotificationHelper.post(
                context = context,
                id = n.id.hashCode(),
                title = n.title,
                message = n.message,
                applicationId = n.application_id
            )
        } else if (unread.size > 1) {
            // Group into a summary notification
            NotificationHelper.post(
                context = context,
                id = "fieldcrm_summary".hashCode(),
                title = "FieldCRM — ${unread.size} updates",
                message = unread.take(3).joinToString(" • ") { it.title }
            )
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "fieldcrm_notification_sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationSyncWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
