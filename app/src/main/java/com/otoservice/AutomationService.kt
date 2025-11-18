package com.otoservice

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class AutomationService : NotificationListenerService() {

    private val prefs by lazy { PreferenceStore.get(this) }
    private val lastSend = ConcurrentHashMap<String, Long>()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val licenseStatus = LicenseManager(this).checkLicenseStatus()
        if (licenseStatus != LicenseManager.STATUS_ACTIVE) return
        if (!prefs.getBoolean(PreferenceStore.KEY_AUTO_REPLY_ENABLED, false)) return

        val allowed = prefs.getStringList(PreferenceStore.KEY_SELECTED_APPS)
        if (allowed.isNotEmpty() && !allowed.contains(sbn.packageName)) return

        val notification = sbn.notification
        if (notification.category == Notification.CATEGORY_SERVICE) return

        val extras = notification.extras
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val conversation = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString().orEmpty()
        val contactKey = sbn.packageName + "|" + (conversation.ifBlank { title.ifBlank { sbn.packageName } })

        val freq = prefs.getLong(PreferenceStore.KEY_FREQUENCY_SECONDS, 15L).coerceAtLeast(5L) * 1000
        val last = lastSend[contactKey] ?: 0L
        if (System.currentTimeMillis() - last < freq) return

        val replyText = prefs.getString(PreferenceStore.KEY_REPLY_TEXT, "")?.trim().orEmpty()
        if (replyText.isBlank()) {
            Log.d("OtoService", "Yanıt metni boş olduğu için otomatik yanıt gönderilmedi.")
            return
        }

        // GERÇEK YANIT GÖNDERME İŞLEMİ
        if (!sendReply(sbn, replyText)) return

        lastSend[contactKey] = System.currentTimeMillis()
        val preview = "Gelen: ${text.take(80)} | Gönderilen: ${replyText.take(80)}"
        appendLog(sbn.packageName, conversation.ifEmpty { title.ifEmpty { "Bilinmeyen" } }, preview)
    }

    private fun sendReply(sbn: StatusBarNotification, replyText: String): Boolean {
        val notification = sbn.notification
        val actions = notification.actions ?: emptyArray()

        if (actions.isEmpty()) return false

        // 1. "Yanıtla" eylemini bul
        var replyAction: PendingIntent? = null
        var remoteInput: RemoteInput? = null
        for (action in actions) {
            val inputs = action.remoteInputs ?: continue
            inputs.firstOrNull { it.allowFreeFormInput || it.resultKey.contains("reply", true) }
                ?.let { input ->
                    remoteInput = input
                    replyAction = action.actionIntent
                    return@for
                }
        }

        if (replyAction == null || remoteInput == null) {
            Log.d("OtoService", "Bu bildirimde yanıt eylemi bulunamadı: ${sbn.packageName}")
            return false
        }

        // 2. Yanıtı hazırla ve gönder
        val resultBundle = Bundle().apply { putCharSequence(remoteInput!!.resultKey, replyText) }
        val replyIntent = Intent()
        RemoteInput.addResultsToIntent(arrayOf(remoteInput!!), replyIntent, resultBundle)

        return try {
            replyAction?.send(applicationContext, 0, replyIntent)
            Log.d("OtoService", "Yanıt başarıyla gönderildi -> ${sbn.packageName}: $replyText")
            true
        } catch (e: PendingIntent.CanceledException) {
            Log.e("OtoService", "Yanıt gönderme işlemi iptal edildi.", e)
            false
        }
    }

    private fun appendLog(pkg: String, target: String, msg: String) {
        val appLabel = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)
        val logsRaw = prefs.getString(PreferenceStore.KEY_LOGS, null)
        val arr = if (logsRaw != null) runCatching { JSONArray(logsRaw) }.getOrDefault(JSONArray()) else JSONArray()
        val obj = JSONObject()
        obj.put("app", appLabel)
        obj.put("target", target)
        obj.put("time", System.currentTimeMillis())
        obj.put("msg", msg)
        arr.put(obj)
        prefs.setString(PreferenceStore.KEY_LOGS, arr.toString())
    }
}
