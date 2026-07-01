package com.fieldcrm.android.data.repository

import com.fieldcrm.android.data.api.ApiNotification
import com.fieldcrm.android.data.api.MobileApiService

class NotificationsRepository(private val apiService: MobileApiService) {
    suspend fun getNotifications(): List<ApiNotification> = apiService.getNotifications()
    suspend fun markRead(id: String): Boolean = apiService.markNotificationRead(id)
    suspend fun clearAll(): Boolean = apiService.clearNotifications()
}
