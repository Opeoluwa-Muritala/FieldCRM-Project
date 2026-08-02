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

    init {
        prefs.edit().apply {
            remove("biometric_enabled")
            remove("use_biometrics")
            remove("use_fingerprint")
            remove("fingerprint_enabled")
            remove("biometric_active")
            remove("biometric_pref")
            remove("dark_mode")
        }.apply()
    }

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_ACCESS_EXPIRES_AT = "access_expires_at"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_NAME = "user_name"
        private const val KEY_ROLE = "user_role"
        private const val KEY_ORG_ID = "org_id"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_PASSCODE_HASH = "passcode_hash"
        private const val KEY_ONBOARDING_SEEN = "onboarding_seen"
        private const val KEY_PERMISSIONS_SEEN = "permissions_seen"


        // 7-day session TTL — matches the mobile JWT lifetime issued by login-mobile
        private const val SESSION_TTL_MS = 48L * 60 * 60 * 1000
    }

    fun save(session: UserSession) {
        prefs.edit()
            .putString(KEY_TOKEN, session.token)
            .putString(KEY_EMAIL, session.userEmail)
            .putString(KEY_NAME, session.userName)
            .putString(KEY_ROLE, session.role.name)
            .putString(KEY_ORG_ID, session.orgId)
            .putLong(KEY_EXPIRES_AT, prefs.getLong(KEY_EXPIRES_AT, 0L).takeIf { it > System.currentTimeMillis() } ?: (System.currentTimeMillis() + SESSION_TTL_MS))
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
            orgId = prefs.getString(KEY_ORG_ID, "") ?: "",
            userEmail = prefs.getString(KEY_EMAIL, "") ?: "",
            userName = prefs.getString(KEY_NAME, "") ?: "",
            loginExpiresAt = expiresAt
        )
    }

    fun saveTokenBundle(accessToken: String, refreshToken: String?, accessExpiresIn: Int, absoluteExpiresAt: String?) {
        val absolute = runCatching { absoluteExpiresAt?.let { java.time.Instant.parse(it).toEpochMilli() } ?: error("missing expiry") }
            .getOrDefault(System.currentTimeMillis() + SESSION_TTL_MS)
        prefs.edit()
            .putString(KEY_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_ACCESS_EXPIRES_AT, System.currentTimeMillis() + accessExpiresIn * 1000L)
            .putLong(KEY_EXPIRES_AT, absolute)
            .apply()
    }

    fun refreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    fun accessExpiresSoon(): Boolean = System.currentTimeMillis() + 60_000L >= prefs.getLong(KEY_ACCESS_EXPIRES_AT, 0L)

    fun clear() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_ACCESS_EXPIRES_AT)
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

}
