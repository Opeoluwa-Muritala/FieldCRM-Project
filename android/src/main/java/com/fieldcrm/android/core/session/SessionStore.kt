package com.fieldcrm.android.core.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionStore(context: Context) {

    private val masterKey = MasterKey.Builder(context, "fieldcrm_session_key")
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = try {
        EncryptedSharedPreferences.create(
            context,
            "fieldcrm_session_enc",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Key mismatch after reinstall — wipe and recreate
        context.deleteSharedPreferences("fieldcrm_session_enc")
        EncryptedSharedPreferences.create(
            context,
            "fieldcrm_session_enc",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_NAME = "user_name"
        private const val KEY_ROLE = "user_role"
        private const val KEY_ORG_ID = "org_id"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_BIOMETRIC_ENROLLED = "biometric_enrolled"
        private const val KEY_BIOMETRIC_SHOWN = "biometric_enrollment_shown"
        private const val KEY_PASSCODE_HASH = "passcode_hash"
        private const val KEY_ONBOARDING_SEEN = "onboarding_seen"
        private const val KEY_PERMISSIONS_SEEN = "permissions_seen"
        private const val KEY_DARK_MODE = "dark_mode"

        // 30-day session TTL — matches the mobile JWT lifetime issued by login-mobile
        private const val SESSION_TTL_MS = 30L * 24 * 60 * 60 * 1000
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
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_EMAIL)
            .remove(KEY_NAME)
            .remove(KEY_ROLE)
            .remove(KEY_ORG_ID)
            .remove(KEY_EXPIRES_AT)
            .apply()
    }

    fun isStored(): Boolean {
        val token = prefs.getString(KEY_TOKEN, null) ?: return false
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        return token.isNotEmpty() && System.currentTimeMillis() <= expiresAt
    }

    // Biometric
    fun isBiometricEnrolled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_ENROLLED, false)
    fun setBiometricEnrolled(enrolled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENROLLED, enrolled).apply()
    }
    fun hasBiometricEnrollmentBeenShown(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_SHOWN, false)
    fun markBiometricEnrollmentShown() {
        prefs.edit().putBoolean(KEY_BIOMETRIC_SHOWN, true).apply()
    }

    // Passcode
    fun savePasscodeHash(hash: String) {
        prefs.edit().putString(KEY_PASSCODE_HASH, hash).apply()
    }
    fun getPasscodeHash(): String? = prefs.getString(KEY_PASSCODE_HASH, null)
    fun hasPasscode(): Boolean = getPasscodeHash() != null
    fun clearPasscode() {
        prefs.edit().remove(KEY_PASSCODE_HASH).apply()
    }

    // Onboarding / permissions (persisted so they survive process restart)
    fun hasSeenOnboarding(): Boolean = prefs.getBoolean(KEY_ONBOARDING_SEEN, false)
    fun setOnboardingSeen() { prefs.edit().putBoolean(KEY_ONBOARDING_SEEN, true).apply() }
    fun hasSeenPermissions(): Boolean = prefs.getBoolean(KEY_PERMISSIONS_SEEN, false)
    fun setPermissionsSeen() { prefs.edit().putBoolean(KEY_PERMISSIONS_SEEN, true).apply() }

    // Dark mode
    fun isDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)
    fun setDarkMode(enabled: Boolean) { prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply() }
}
