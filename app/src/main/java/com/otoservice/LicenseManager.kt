package com.otoservice

import android.content.Context
import android.provider.Settings
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class LicenseManager(private val context: Context) {

    private val prefs = PreferenceStore.get(context)

    companion object {
        const val STATUS_ACTIVE = "AKTİF"
        const val STATUS_INVALID = "Geçersiz Lisans"
        const val STATUS_EXPIRED = "Lisans Süresi Dolmuş"
    }

    fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
    }

    fun getRemainingDays(): Long {
        val expire = prefs.getLong(PreferenceStore.KEY_LICENSE_EXPIRE, 0L)
        if (expire == 0L) return 0
        val diff = expire - System.currentTimeMillis()
        return if (diff <= 0) 0 else diff / (1000 * 60 * 60 * 24)
    }

    fun checkLicenseStatus(): String {
        val storedKey = prefs.getString(PreferenceStore.KEY_LICENSE)
        val storedDevice = prefs.getString(PreferenceStore.KEY_LICENSE_DEVICE)
        val storedStatus = prefs.getString(PreferenceStore.KEY_LICENSE_STATUS)
        val expire = prefs.getLong(PreferenceStore.KEY_LICENSE_EXPIRE, 0L)
        val activation = prefs.getLong(PreferenceStore.KEY_LICENSE_ACTIVATION, 0L)

        if (storedKey.isNullOrEmpty() || storedDevice.isNullOrEmpty() || activation == 0L) {
            return STATUS_INVALID
        }
        if (prefs.getString(PreferenceStore.KEY_LICENSE_BLACKLIST) == storedKey) {
            prefs.clearLicense()
            return STATUS_INVALID
        }
        if (storedDevice != getDeviceId()) {
            prefs.clearLicense()
            return STATUS_INVALID
        }
        if (expire < System.currentTimeMillis()) {
            prefs.setString(PreferenceStore.KEY_LICENSE_STATUS, STATUS_EXPIRED)
            prefs.setString(PreferenceStore.KEY_LICENSE_BLACKLIST, storedKey)
            return STATUS_EXPIRED
        }
        return storedStatus ?: STATUS_INVALID
    }

    fun validateAndActivateLicense(rawKey: String): String {
        if (prefs.getString(PreferenceStore.KEY_LICENSE_BLACKLIST) == rawKey) {
            return STATUS_INVALID
        }
        val decrypted = try {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, g1())
            // DÜZELTME: Karakter seti UTF-8 olarak sabitlendi
            String(cipher.doFinal(Base64.decode(rawKey, Base64.DEFAULT)), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            prefs.setString(PreferenceStore.KEY_LICENSE_BLACKLIST, rawKey)
            return STATUS_INVALID
        }
        val parts = decrypted.split("|")
        if (parts.size < 3) return STATUS_INVALID
        val deviceId = parts[0]
        val expire = parts[1].toLongOrNull() ?: 0L
        val signature = parts[2]
        if (deviceId != getDeviceId()) {
            prefs.clearLicense()
            prefs.setString(PreferenceStore.KEY_LICENSE_BLACKLIST, rawKey)
            return STATUS_INVALID
        }
        val expected = h1("${deviceId}|${expire}")
        if (expected != signature) {
            prefs.setString(PreferenceStore.KEY_LICENSE_BLACKLIST, rawKey)
            return STATUS_INVALID
        }
        if (expire < System.currentTimeMillis()) {
            prefs.setString(PreferenceStore.KEY_LICENSE_BLACKLIST, rawKey)
            return STATUS_EXPIRED
        }

        prefs.setString(PreferenceStore.KEY_LICENSE, rawKey)
        prefs.setString(PreferenceStore.KEY_LICENSE_DEVICE, deviceId)
        prefs.setLong(PreferenceStore.KEY_LICENSE_EXPIRE, expire)
        prefs.setString(PreferenceStore.KEY_LICENSE_STATUS, STATUS_ACTIVE)
        prefs.setLong(PreferenceStore.KEY_LICENSE_ACTIVATION, System.currentTimeMillis())
        prefs.setLong(PreferenceStore.KEY_LICENSE_LAST_CHECK, System.currentTimeMillis())
        return STATUS_ACTIVE
    }

    private fun g1(): SecretKeySpec {
        val seed = q1()
        val digest = MessageDigest.getInstance("SHA-256").digest(seed)
        return SecretKeySpec(digest.copyOf(16), "AES")
    }

    private fun h1(value: String): String {
        // DÜZELTME: Karakter seti UTF-8 olarak sabitlendi
        val a = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8))
        val b = MessageDigest.getInstance("SHA-256").digest((value.reversed() + r1()).toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(a + b, Base64.NO_WRAP)
    }

    private fun q1(): ByteArray {
        val salt = "OTSRV!@#".toByteArray(StandardCharsets.UTF_8)
        val base = getDeviceId().toByteArray(StandardCharsets.UTF_8)
        return (salt + base + salt.reversedArray())
    }

    private fun r1(): String {
        return "OTSRV_S1"
    }
}
