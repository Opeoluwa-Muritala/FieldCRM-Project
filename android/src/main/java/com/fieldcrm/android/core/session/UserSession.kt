package com.fieldcrm.android.core.session

data class UserSession(
    val token: String,
    val role: UserRole,
    val orgId: String,
    val userEmail: String,
    val userName: String = "",
    val loginExpiresAt: Long = 0L
)
