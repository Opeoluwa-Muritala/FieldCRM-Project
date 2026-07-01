package com.fieldcrm.android.data.repository

import com.fieldcrm.android.data.api.AppConfig
import com.fieldcrm.android.data.api.MobileApiService

class ConfigRepository(private val apiService: MobileApiService) {
    private var cached: AppConfig? = null

    suspend fun getConfig(forceRefresh: Boolean = false): AppConfig? {
        if (!forceRefresh && cached != null) return cached
        val result = apiService.getConfig()
        if (result != null) cached = result
        return result
    }
}
