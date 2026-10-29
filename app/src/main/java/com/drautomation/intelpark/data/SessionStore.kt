package com.drautomation.intelpark.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.drautomation.intelpark.BuildConfig

enum class Role { DRIVER, STAFF }

/**
 * Tokens and settings, encrypted with a key of the Android Keystore (Jetpack Security).
 * The refresh token works once: every refresh stores the new pair.
 */
class SessionStore(context: Context) {
    private val prefs: SharedPreferences = open(context)

    private fun open(context: Context): SharedPreferences {
        fun create(): SharedPreferences = EncryptedSharedPreferences.create(
            context,
            FILE,
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        return try {
            create()
        } catch (e: Exception) {
            // Keystore key lost (backup restored on another phone...): start again with an empty store
            context.deleteSharedPreferences(FILE)
            create()
        }
    }

    var serverUrl: String
        get() = prefs.getString("server_url", null) ?: BuildConfig.DEFAULT_SERVER_URL
        set(value) = prefs.edit().putString("server_url", value.trim().trimEnd('/')).apply()

    val role: Role?
        get() = prefs.getString("role", null)?.let { runCatching { Role.valueOf(it) }.getOrNull() }

    val accessToken: String? get() = prefs.getString("access", null)
    val refreshToken: String? get() = prefs.getString("refresh", null)
    val displayName: String? get() = prefs.getString("name", null)
    val staffRole: String? get() = prefs.getString("staff_role", null)

    fun signIn(role: Role, access: String, refresh: String, name: String, staffRole: String? = null) {
        prefs.edit()
            .putString("role", role.name)
            .putString("access", access)
            .putString("refresh", refresh)
            .putString("name", name)
            .putString("staff_role", staffRole)
            .remove("ble_token").remove("ble_exp")
            .apply()
    }

    @Synchronized
    fun saveTokens(access: String, refresh: String) {
        prefs.edit().putString("access", access).putString("refresh", refresh).commit()
    }

    fun signOut() {
        prefs.edit().remove("role").remove("access").remove("refresh").remove("name").remove("staff_role")
            .remove("ble_token").remove("ble_exp").apply()
    }

    /** Bluetooth identity token (signed by the server, shown to the cabinets). */
    var bleToken: String?
        get() = prefs.getString("ble_token", null)
        set(value) = prefs.edit().putString("ble_token", value).apply()

    var bleTokenExpiresAt: Long
        get() = prefs.getLong("ble_exp", 0L)
        set(value) = prefs.edit().putLong("ble_exp", value).apply()

    var autoBluetooth: Boolean
        get() = prefs.getBoolean("auto_ble", true)
        set(value) = prefs.edit().putBoolean("auto_ble", value).apply()

    private companion object {
        const val FILE = "intelpark_secure"
    }
}
