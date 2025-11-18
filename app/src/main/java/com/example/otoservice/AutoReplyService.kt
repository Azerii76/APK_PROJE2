package com.example.otoservice

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.RemoteInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Bildirimleri dinleyip seçilen paketlere otomatik yanıt gönderir.
 */
class AutoReplyService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Job() + Dispatchers.Default)
    private lateinit var prefs: PreferenceStore
    private val lastReplyMap = mutableMapOf<String, Long>()

    override fun onCreate() {
        super.onCreate()
        prefs = PreferenceStore(this)
        Log.d(TAG, "AutoReplyService başladı")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val selectedPackages = prefs.selectedPackages
        if (!prefs.autoReplyEnabled || selectedPackages.isEmpty()) return
        if (!selectedPackages.contains(sbn.packageName)) return

        val notification = sbn.notification
        val extras = notification.extras ?: return
        val messageText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val conversationKey = listOf(sbn.packageName, sender.ifBlank { "unknown" }).joinToString(":")

        if (prefs.autoReplyText.isBlank()) return

        val cooldownMillis = prefs.repeatDelayMinutes * 60 * 1000L
        val last = lastReplyMap[conversationKey] ?: 0L
        val now = System.currentTimeMillis()
        if (now - last < cooldownMillis) {
            return
        }

        val replyAction = notification.actions?.firstOrNull { action ->
            action.remoteInputs?.any { it.allowFreeFormInput } == true
        }
        if (replyAction == null) {
            Log.d(TAG, "Yanıtlanabilir eylem yok: ${sbn.packageName}")
            return
        }

        val delaySeconds = prefs.replyDelaySeconds
        serviceScope.launch {
            if (delaySeconds > 0) {
                delay(delaySeconds * 1000L)
            }
            try {
                val remoteInput = replyAction.remoteInputs?.firstOrNull { it.allowFreeFormInput }
                if (remoteInput != null) {
                    val input = android.content.Intent()
                    val results = android.os.Bundle()
                    results.putCharSequence(remoteInput.resultKey, prefs.autoReplyText)
                    RemoteInput.addResultsToIntent(arrayOf(remoteInput), input, results)
                    replyAction.actionIntent.send(this@AutoReplyService, 0, input)
                    lastReplyMap[conversationKey] = System.currentTimeMillis()
                    Log.d(TAG, "Yanıt gönderildi: $conversationKey")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Yanıt gönderilemedi", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.coroutineContext[Job]?.cancel()
    }

    companion object {
        private const val TAG = "AutoReplyService"
    }
}
