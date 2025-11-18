package com.example.otoservice

import android.content.Context
import android.os.Process
import android.os.UserManager

/**
 * Paket adı ve kullanıcı profili kontrolleri ile klon/ikincil kullanıcı tespiti.
 */
object AntiCloneChecker {
    private const val EXPECTED_PACKAGE = "com.example.otoservice"
    private val suspiciousTokens = listOf("clone", "dual", "parallel", "work", "island")

    fun isClonedOrTampered(context: Context): Boolean {
        if (context.packageName != EXPECTED_PACKAGE) return true

        val processName = context.applicationInfo.processName.orEmpty().lowercase()
        if (suspiciousTokens.any { processName.contains(it) }) return true

        val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager ?: return false
        val currentUser = android.os.Process.myUserHandle()
        val serial = runCatching { userManager.getSerialNumberForUser(currentUser) }.getOrDefault(0L)
        if (serial > 0 && serial != 0L && serial != 10L) {
            // Bilinen ana kullanıcı dışındaki profilleri şüpheli kabul ediyoruz.
            return true
        }
        val userName = runCatching { userManager.getUserName() }.getOrNull()?.lowercase().orEmpty()
        if (suspiciousTokens.any { userName.contains(it) }) return true
        return false
    }
}
