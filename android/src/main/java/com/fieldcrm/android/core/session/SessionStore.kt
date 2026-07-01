package com.fieldcrm.android.core.session

import android.content.Context
import android.content.SharedPreferences

class SessionStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("fieldcrm_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_NAME = "user_name"
        private const val KEY_ROLE = "user_role"
        private const val KEY_ORG_ID = "org_id"
        private const val KEY_EXPIRES_AT = "expires_at"

        // 48-hour session TTL
        private const val SESSION_TTL_MS = 48L * 60 * 60 * 1000
    }

    fun save(session: UserSession) {
        prefs.edit()
            .putString(KEY_TOKEN, session.token)
            .putString(KEY_EMAIL, session.userEmail)
            .putString(KEY_NAME, session.userName)
            .putString(KEY_ROLE, session.role.name)
            .putString(KEY_ORG_ID, session.orgId)
            .putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + SESSION_TTL_MS)
            .apply()
    }

    fun load(): UserSession? {
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        if (System.currentTimeMillis() > expiresAt) {
            clear()
            return null
        }
        val roleStr = prefs.getString(KEY_ROLE, null) ?: return null
        val role = runCatching { UserRole.valueOf(roleStr) }.getOrNull() ?: return null
        return UserSession(
            token = token,
            role = role,
            orgId = prefs.getString(KEY_ORG_ID, "org_1") ?: "org_1",
            userEmail = prefs.getString(KEY_EMAIL, "") ?: "",
            userName = prefs.getString(KEY_NAME, "") ?: "",
            loginExpiresAt = expiresAt
        )
    }

    fun extendSession() {
        val current = prefs.getLong(KEY_EXPIRES_AT, 0L)
        if (current > 0L) {
            prefs.edit()
                .putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + SESSION_TTL_MS)
                .apply()
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun isStored(): Boolean {
        val token = prefs.getString(KEY_TOKEN, null) ?: return false
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        return token.isNotEmpty() && System.currentTimeMillis() <= expiresAt
    }
}
