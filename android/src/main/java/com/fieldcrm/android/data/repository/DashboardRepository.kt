package com.fieldcrm.android.data.repository

import com.fieldcrm.android.data.api.DashboardMetrics
import com.fieldcrm.android.data.api.MobileApiService

class DashboardRepository(private val apiService: MobileApiService) {
    suspend fun getMetrics(): DashboardMetrics? = apiService.getDashboardMetrics()
}
