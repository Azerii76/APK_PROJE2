package com.otoservice

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

        val extras = sbn.notification.extras
        val text = extras.getCharSequence("android.text")?.toString() ?: return
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val contactKey = sbn.packageName + "|" + title

        val freq = prefs.getLong(PreferenceStore.KEY_FREQUENCY_SECONDS, 5L).coerceAtLeast(5L) * 1000
        val last = lastSend[contactKey] ?: 0L
        if (System.currentTimeMillis() - last < freq) return

        val replyText = prefs.getString(PreferenceStore.KEY_REPLY_TEXT, "Merhaba, müsait değilim") ?: return
        
        // GERÇEK YANIT GÖNDERME İŞLEMİ
        sendReply(sbn, replyText)
        
        lastSend[contactKey] = System.currentTimeMillis()
        appendLog(sbn.packageName, title.ifEmpty { "Bilinmeyen" }, replyText.take(60))
    }

    private fun sendReply(sbn: StatusBarNotification, replyText: String) {
        val notification = sbn.notification
        val actions = notification.actions
        val extras = notification.extras

        if (actions.isNullOrEmpty()) return

        // 1. "Yanıtla" eylemini bul
        var replyAction: PendingIntent? = null
        var remoteInput: RemoteInput? = null
        for (action in actions) {
            if (action.remoteInputs.isNullOrEmpty()) continue
            for (input in action.remoteInputs) {
                if (input.resultKey.equals("direct_reply", ignoreCase = true) || input.label.toString().contains("Yanıtla", ignoreCase = true)) {
                    remoteInput = input
                    replyAction = action.actionIntent
                    break
                }
            }
            if (remoteInput != null) break
        }

        if (replyAction == null || remoteInput == null) {
            Log.d("OtoService", "Bu bildirimde yanıt eylemi bulunamadı: ${sbn.packageName}")
            return
        }

        // 2. Yanıtı hazırla ve gönder
        val resultBundle = Bundle()
        resultBundle.putCharSequence(remoteInput.resultKey, replyText)

        val replyIntent = Intent()
        RemoteInput.addResultsToIntent(arrayOf(remoteInput), replyIntent, resultBundle)

        try {
            replyAction.send(applicationContext, 0, replyIntent)
            Log.d("OtoService", "Yanıt başarıyla gönderildi -> ${sbn.packageName}: $replyText")
        } catch (e: PendingIntent.CanceledException) {
            Log.e("OtoService", "Yanıt gönderme işlemi iptal edildi.", e)
        }
    }

    private fun appendLog(pkg: String, target: String, msg: String) {
        val logsRaw = prefs.getString(PreferenceStore.KEY_LOGS, null)
        val arr = if (logsRaw != null) runCatching { JSONArray(logsRaw) }.getOrDefault(JSONArray()) else JSONArray()
        val obj = JSONObject()
        obj.put("app", pkg)
        obj.put("target", target)
        obj.put("time", System.currentTimeMillis())
        obj.put("msg", msg)
        arr.put(obj)
        prefs.setString(PreferenceStore.KEY_LOGS, arr.toString())
    }
}
