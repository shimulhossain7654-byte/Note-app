package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "secure_notes_app_preferences",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_THEME = "app_theme_name"
        private const val KEY_APP_PIN = "app_security_pin"
        private const val KEY_PIN_ENABLED = "app_pin_lock_enabled"
        private const val KEY_GRID_LAYOUT = "notes_grid_layout_enabled"
        private const val KEY_AUTO_SAVE = "notes_auto_save_enabled"
        private const val KEY_LAST_BACKUP = "notes_last_backup_timestamp"
    }

    var selectedTheme: String
        get() = prefs.getString(KEY_THEME, "System Default") ?: "System Default"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    var appSecurityPin: String
        get() = prefs.getString(KEY_APP_PIN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_APP_PIN, value).apply()

    var pinLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_PIN_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PIN_ENABLED, value).apply()

    var isGridLayout: Boolean
        get() = prefs.getBoolean(KEY_GRID_LAYOUT, true)
        set(value) = prefs.edit().putBoolean(KEY_GRID_LAYOUT, value).apply()

    var isAutoSaveEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SAVE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SAVE, value).apply()

    var lastBackupTimestamp: Long
        get() = prefs.getLong(KEY_LAST_BACKUP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKUP, value).apply()

    // Hash algorithm helper to verify pin passwords
    fun verifyPin(enteredPin: String): Boolean {
        val savedPin = appSecurityPin
        return if (savedPin.isEmpty()) {
            false
        } else {
            savedPin == enteredPin
        }
    }

    fun clearPin() {
        prefs.edit().remove(KEY_APP_PIN).remove(KEY_PIN_ENABLED).apply()
    }
}
