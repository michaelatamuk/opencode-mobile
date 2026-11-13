package com.opencode.mobile

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted preferences for the server password and local state.
 *
 * Falls back to plain preferences if the keystore is unavailable (some devices
 * and emulators), so the app never becomes unusable because of crypto.
 */
object SecurePrefs {

    private const val FILE = "opencode_secure"
    private const val LEGACY_FILE = "opencode"

    @Volatile
    private var cached: SharedPreferences? = null

    fun get(context: Context): SharedPreferences {
        cached?.let { return it }

        val prefs = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            context.getSharedPreferences(LEGACY_FILE, Context.MODE_PRIVATE)
        }

        migrate(context, prefs)
        cached = prefs
        return prefs
    }

    /** Move values written by older builds into the encrypted store once. */
    private fun migrate(context: Context, target: SharedPreferences) {
        if (target.getBoolean("migrated", false)) return

        val legacy = context.getSharedPreferences(LEGACY_FILE, Context.MODE_PRIVATE)
        val server = legacy.getString("server", null)
        val password = legacy.getString("password", null)
        val watch = legacy.getBoolean("watch", false)

        target.edit().apply {
            if (server != null) putString("server", server)
            if (password != null) putString("password", password)
            putBoolean("watch", watch)
            putBoolean("migrated", true)
        }.apply()

        legacy.edit().remove("server").remove("password").apply()
    }
}
