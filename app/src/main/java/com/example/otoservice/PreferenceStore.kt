package com.example.otoservice

import android.content.Context
import androidx.core.content.edit

class PreferenceStore(context: Context) {
    private val prefs = context.getSharedPreferences("oto_prefs", Context.MODE_PRIVATE)

    var autoReplyEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_REPLY_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_REPLY_ENABLED, value) }

    var autoReplyText: String
        get() = prefs.getString(KEY_AUTO_REPLY_TEXT, "") ?: ""
        set(value) = prefs.edit { putString(KEY_AUTO_REPLY_TEXT, value) }

    var replyDelaySeconds: Int
        get() = prefs.getInt(KEY_REPLY_DELAY, 0)
        set(value) = prefs.edit { putInt(KEY_REPLY_DELAY, value) }

    var repeatDelayMinutes: Int
        get() = prefs.getInt(KEY_REPEAT_MINUTES, 10)
        set(value) = prefs.edit { putInt(KEY_REPEAT_MINUTES, value) }

    var selectedPackages: Set<String>
        get() = prefs.getStringSet(KEY_SELECTED_PACKAGES, emptySet()) ?: emptySet()
        set(value) = prefs.edit { putStringSet(KEY_SELECTED_PACKAGES, value) }

    var selectedDistricts: List<String>
        get() = prefs.getString(KEY_DISTRICTS, "")?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
        set(value) = prefs.edit { putString(KEY_DISTRICTS, value.joinToString(",")) }

    var districtDurationMinutes: Int
        get() = prefs.getInt(KEY_DISTRICT_DURATION, 15)
        set(value) = prefs.edit { putInt(KEY_DISTRICT_DURATION, value) }

    var locationEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCATION_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_LOCATION_ENABLED, value) }

    companion object {
        private const val KEY_AUTO_REPLY_ENABLED = "auto_reply_enabled"
        private const val KEY_AUTO_REPLY_TEXT = "auto_reply_text"
        private const val KEY_REPLY_DELAY = "auto_reply_delay"
        private const val KEY_REPEAT_MINUTES = "auto_reply_repeat"
        private const val KEY_SELECTED_PACKAGES = "selected_packages"
        private const val KEY_DISTRICTS = "selected_districts"
        private const val KEY_DISTRICT_DURATION = "district_duration"
        private const val KEY_LOCATION_ENABLED = "location_enabled"
    }
}
