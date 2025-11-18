package com.example.otoservice

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * License doğrulama ve zaman bütünlüğü kontrollerini yapan sınıf.
 * Python'daki license_generator.py ile bire bir aynı algoritmayı kullanır.
 */
class LicenseManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun loadState(): LicenseState {
        return LicenseState(
            licenseName = prefs.getString(KEY_LICENSE_NAME, null),
            licenseCode = prefs.getString(KEY_LICENSE_CODE, null),
            expiryMillis = prefs.getLong(KEY_EXPIRY_MILLIS, 0L),
            lastKnownTime = prefs.getLong(KEY_LAST_TIME, 0L),
            timeTampered = prefs.getBoolean(KEY_TIME_TAMPERED, false),
            cloneTampered = prefs.getBoolean(KEY_CLONE_TAMPERED, false)
        )
    }

    fun saveState(state: LicenseState) {
        prefs.edit()
            .putString(KEY_LICENSE_NAME, state.licenseName)
            .putString(KEY_LICENSE_CODE, state.licenseCode)
            .putLong(KEY_EXPIRY_MILLIS, state.expiryMillis)
            .putLong(KEY_LAST_TIME, state.lastKnownTime)
            .putBoolean(KEY_TIME_TAMPERED, state.timeTampered)
            .putBoolean(KEY_CLONE_TAMPERED, state.cloneTampered)
            .apply()
    }

    fun clearLicense() {
        prefs.edit().clear().apply()
    }

    fun generateExpectedCode(licenseName: String, expiryDateStr: String): String {
        val base = "$SECRET_SEED|$licenseName|$expiryDateStr"
        val encodedBytes = mutableListOf<Int>()
        base.forEachIndexed { index, ch ->
            val val1 = (ch.code * 7 + index * 13 + 11) % 256
            val val2 = (val1 * 5 + 19) % 256
            encodedBytes.add(val2)
        }
        val encodedString = encodedBytes.map { byte ->
            val idx = byte % ALPHABET.length
            ALPHABET[idx]
        }.joinToString("")

        val trimmed = if (encodedString.length >= 16) encodedString.substring(0, 16) else encodedString
        return trimmed.chunked(4).joinToString("-")
    }

    fun validateLicense(licenseName: String, licenseCode: String, expiryDateStr: String): Boolean {
        if (!isChecksumIntact()) return false
        val expected = generateExpectedCode(licenseName, expiryDateStr)
        if (!licenseCode.equals(expected, ignoreCase = true)) return false
        val expiryMillis = parseExpiry(expiryDateStr)
        return System.currentTimeMillis() <= expiryMillis
    }

    fun parseExpiry(expiryDateStr: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.parse(expiryDateStr)
            val base = date?.time ?: 0L
            // Gün sonuna kadar geçerli olması için 23:59:59'a uzatılır.
            base + TimeUnit.DAYS.toMillis(1) - 1000
        } catch (_: Exception) {
            0L
        }
    }

    fun updateTimeIntegrity(state: LicenseState): LicenseState {
        val now = System.currentTimeMillis()
        val abnormalRange = now < MIN_VALID_TIME || now > MAX_VALID_TIME
        val tamperedBackwards = state.lastKnownTime > 0 && now < state.lastKnownTime
        val timeTampered = state.timeTampered || abnormalRange || tamperedBackwards
        val correctedLastTime = if (!timeTampered) now else state.lastKnownTime
        return state.copy(timeTampered = timeTampered, lastKnownTime = correctedLastTime)
    }

    fun isLicenseExpired(state: LicenseState): Boolean {
        return state.expiryMillis > 0 && System.currentTimeMillis() > state.expiryMillis
    }

    private fun isChecksumIntact(): Boolean {
        val checksum = CHECKSUM_STRING.sumOf { it.code } % 997
        return checksum == EXPECTED_CHECKSUM
    }

    companion object {
        private const val PREF_NAME = "oto_license"
        private const val KEY_LICENSE_NAME = "license_name"
        private const val KEY_LICENSE_CODE = "license_code"
        private const val KEY_EXPIRY_MILLIS = "expiry_date"
        private const val KEY_LAST_TIME = "last_known_time"
        private const val KEY_TIME_TAMPERED = "time_tampered"
        private const val KEY_CLONE_TAMPERED = "clone_tampered"

        const val SECRET_SEED = "YASIN_SUPER_SECRET_2025_XYZ"
        const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private const val CHECKSUM_STRING = "OTOSERVICE_INTEGRITY_BLOCK"
        private const val EXPECTED_CHECKSUM = 33  // basit bütünlük kontrolü (karakter toplamı mod 997)
        private val MIN_VALID_TIME = TimeUnit.DAYS.toMillis(365 * 3) // 1970+ ~3 yıl sonrası
        private val MAX_VALID_TIME = TimeUnit.DAYS.toMillis(365 * 200) // 200 yıl sonrası
    }
}

/**
 * Bellekte tutulan lisans durumu.
 */
data class LicenseState(
    val licenseName: String?,
    val licenseCode: String?,
    val expiryMillis: Long,
    val lastKnownTime: Long,
    val timeTampered: Boolean,
    val cloneTampered: Boolean
) {
    fun isActive(): Boolean = !timeTampered && !cloneTampered && licenseName != null && licenseCode != null
}
