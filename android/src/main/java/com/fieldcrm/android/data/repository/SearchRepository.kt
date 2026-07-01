package com.fieldcrm.android.data.repository

import com.fieldcrm.android.data.api.MobileApiService
import com.fieldcrm.android.data.api.SearchResponse

class SearchRepository(private val apiService: MobileApiService) {
    suspend fun search(query: String): SearchResponse? = apiService.search(query)
}
