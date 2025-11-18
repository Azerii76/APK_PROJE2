package com.otoservice

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class PreferenceStore private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("otoservice_prefs", Context.MODE_PRIVATE)

    fun setBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    fun getBoolean(key: String, default: Boolean = false): Boolean = prefs.getBoolean(key, default)

    fun setString(key: String, value: String?) = prefs.edit().putString(key, value).apply()
    fun getString(key: String, default: String? = null): String? = prefs.getString(key, default)

    fun setLong(key: String, value: Long) = prefs.edit().putLong(key, value).apply()
    fun getLong(key: String, default: Long = 0L): Long = prefs.getLong(key, default)

    // EKSİK FONKSİYONLAR EKLENDİ
    fun setInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    fun getInt(key: String, default: Int = 0): Int = prefs.getInt(key, default)

    fun setStringList(key: String, values: List<String>) {
        val array = JSONArray()
        values.forEach { array.put(it) }
        prefs.edit().putString(key, array.toString()).apply()
    }

    fun getStringList(key: String): List<String> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrDefault(emptyList())
    }

    fun setJsonObject(key: String, obj: JSONObject) {
        prefs.edit().putString(key, obj.toString()).apply()
    }

    fun getJsonObject(key: String): JSONObject? {
        val raw = prefs.getString(key, null) ?: return null
        return runCatching { JSONObject(raw) }.getOrNull()
    }

    fun clearLicense() {
        prefs.edit().remove(KEY_LICENSE).remove(KEY_LICENSE_DEVICE).remove(KEY_LICENSE_EXPIRE)
            .remove(KEY_LICENSE_ACTIVATION).remove(KEY_LICENSE_STATUS)
            .remove(KEY_LICENSE_BLACKLIST).apply()
    }

    companion object {
        const val KEY_AUTO_REPLY_ENABLED = "auto_reply_enabled"
        const val KEY_LOCATION_ENABLED = "location_enabled"
        const val KEY_REPLY_TEXT = "reply_text"
        const val KEY_SELECTED_APPS = "selected_apps"
        const val KEY_FREQUENCY_SECONDS = "frequency_seconds"
        const val KEY_LOCATION_INTERVAL_MIN = "location_interval_min"
        // EKSİK ANAHTAR EKLENDİ
        const val KEY_LOCATION_DISTRICT = "location_district"
        const val KEY_LOGS = "logs"
        const val KEY_LICENSE = "license_key"
        const val KEY_LICENSE_STATUS = "license_status"
        const val KEY_LICENSE_DEVICE = "license_device"
        const val KEY_LICENSE_EXPIRE = "license_expire"
        const val KEY_LICENSE_ACTIVATION = "license_activation"
        const val KEY_LICENSE_LAST_CHECK = "license_last_check"
        const val KEY_LICENSE_BLACKLIST = "license_blacklist"

        @Volatile
        private var INSTANCE: PreferenceStore? = null

        fun get(context: Context): PreferenceStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferenceStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
