package com.fieldcrm.android.core.network

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T, val statusCode: Int) : ApiResult<T>
    data class Error(
        val statusCode: Int,
        val detail: String,
        val cause: Throwable? = null
    ) : ApiResult<Nothing>
    data class NetworkError(val message: String, val cause: Throwable? = null) : ApiResult<Nothing>
    data object Loading : ApiResult<Nothing>
}
