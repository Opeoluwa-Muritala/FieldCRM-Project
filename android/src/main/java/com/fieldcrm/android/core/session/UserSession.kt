package com.fieldcrm.android.core.session

data class UserSession(
    val token: String,
    val role: UserRole,
    val orgId: String,
    val userEmail: String
)
