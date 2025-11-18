package com.otoservice

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial

class AutomationSettingsFragment : Fragment() {

    private lateinit var prefs: PreferenceStore
    private lateinit var chipContainer: LinearLayout
    private lateinit var etReply: EditText

    // Uygulama bilgilerini tutmak için güvenli bir data class
    data class AppInfo(val name: String, val packageName: String)

    // Yaygın mesajlaşma/sosyal medya uygulama paketleri
    private val supportedPackages = mapOf(
        "com.whatsapp" to "WhatsApp",
        "org.telegram.messenger" to "Telegram",
        "com.instagram.android" to "Instagram",
        "com.facebook.orca" to "Messenger",
        "com.facebook.katana" to "Facebook",
        "com.snapchat.android" to "Snapchat",
        "com.discord" to "Discord",
        "com.viber.voip" to "Viber",
        "com.skype.raider" to "Skype",
        "com.tinder" to "Tinder",
        "com.bumble.app" to "Bumble",
        "com.ftw_and_co.happn" to "Happn",
        "com.linkedin.android" to "LinkedIn",
        "com.signal.android" to "Signal",
        "com.google.android.apps.messaging" to "Mesajlar",
        "com.samsung.android.messaging" to "Samsung Mesajlar"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_automation_settings, container, false)
        prefs = PreferenceStore.get(requireContext())
        chipContainer = view.findViewById(R.id.chipContainer)
        val switchAuto = view.findViewById<SwitchMaterial>(R.id.switchAutoReply)
        etReply = view.findViewById(R.id.etReply)
        val btnSelect = view.findViewById<Button>(R.id.btnSelectApps)
        val spinner = view.findViewById<Spinner>(R.id.spinnerFrequency)

        switchAuto.isChecked = prefs.getBoolean(PreferenceStore.KEY_AUTO_REPLY_ENABLED, false)
        etReply.setText(prefs.getString(PreferenceStore.KEY_REPLY_TEXT, getString(R.string.default_reply)))
        cleanupMissingSelections()
        renderSelectedApps()

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, FrequencyDefaults.replySeconds)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        val savedFreq = prefs.getLong(PreferenceStore.KEY_FREQUENCY_SECONDS, 15L).toInt()
        spinner.setSelection(FrequencyDefaults.replySeconds.indexOf(savedFreq).coerceAtLeast(0))

        switchAuto.setOnCheckedChangeListener { _, isChecked ->
            prefs.setBoolean(PreferenceStore.KEY_AUTO_REPLY_ENABLED, isChecked)
            if (isChecked && !isNotificationServiceEnabled()) {
                Toast.makeText(requireContext(), "Bildirim erişimi kapalı. İzin ekranı açılıyor.", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
        btnSelect.setOnClickListener { showAppChooserDialog() }
        spinner.setOnItemSelectedListener(SimpleItemSelectedListener { index ->
            val value = FrequencyDefaults.replySeconds.getOrElse(index) { 15 }
            prefs.setLong(PreferenceStore.KEY_FREQUENCY_SECONDS, value.toLong())
        })

        return view
    }

    override fun onPause() {
        super.onPause()
        prefs.setString(PreferenceStore.KEY_REPLY_TEXT, etReply.text.toString().trim())
    }

    /**
     * Cihazda kurulu olan ve desteklenen mesajlaşma/sosyal medya uygulamalarını döndürür.
     * Diğer uygulamaları listeye dahil etmez; böylece kullanıcı gerçekten bildirim almaya uygun
     * uygulamaları seçer.
     */
    private fun getInstalledApps(pm: PackageManager): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()
        supportedPackages.forEach { (pkg, fallbackName) ->
            val appInfo = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull() ?: return@forEach
            // Sadece kullanıcı tarafından açılabilir uygulamaları göster
            if (pm.getLaunchIntentForPackage(pkg) == null) return@forEach

            val appName = runCatching { pm.getApplicationLabel(appInfo).toString() }
                .getOrDefault(fallbackName)
            // Sistem uygulamalarını hariç tut; güncellenmiş sistem uygulamaları gösterilebilir
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            if (isSystemApp && !isUpdatedSystemApp) return@forEach

            apps.add(AppInfo(appName, pkg))
        }

        if (apps.isEmpty()) {
            Log.w("OtoService", "Desteklenen uygulama bulunamadı. Geniş tarama başlatılıyor.")
            // Nadiren kullanılan paketleri yakalamak için ACTION_SEND filtresine sahip uygulamaları kontrol et
            val candidates = pm.queryIntentActivities(Intent(Intent.ACTION_SEND).setType("text/plain"), 0)
            candidates.forEach { info ->
                val pkg = info.activityInfo.packageName
                val appName = info.loadLabel(pm)?.toString() ?: pkg
                apps.add(AppInfo(appName, pkg))
            }
        }

        return apps.distinctBy { it.packageName }.sortedBy { it.name.lowercase() }
    }

    /**
     * Kaldırılmış uygulamaları tercih listesinden siler ve UI'ı temizler.
     */
    private fun cleanupMissingSelections() {
        val pm = requireContext().packageManager
        val selected = prefs.getStringList(PreferenceStore.KEY_SELECTED_APPS)
        val stillInstalled = selected.filter { pkg ->
            runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull() != null
        }
        if (stillInstalled.size != selected.size) {
            prefs.setStringList(PreferenceStore.KEY_SELECTED_APPS, stillInstalled)
        }
    }


    private fun showAppChooserDialog() {
        val pm = requireContext().packageManager
        val allApps = getInstalledApps(pm)

        val appNames = allApps.map { it.name }.toTypedArray()
        val appPackages = allApps.map { it.packageName }
        val selectedPackages = prefs.getStringList(PreferenceStore.KEY_SELECTED_APPS).toMutableSet()
        val isCheckedArray = BooleanArray(appPackages.size) { i -> selectedPackages.contains(appPackages[i]) }

        AlertDialog.Builder(requireContext())
            .setTitle("Uygulama Seç (En Fazla 6)")
            .setMultiChoiceItems(appNames, isCheckedArray) { dialog, which, isChecked ->
                val clickedPackage = appPackages.getOrNull(which) ?: return@setMultiChoiceItems
                if (isChecked && selectedPackages.size >= 6 && !selectedPackages.contains(clickedPackage)) {
                    Toast.makeText(requireContext(), "En fazla 6 uygulama seçebilirsiniz.", Toast.LENGTH_SHORT).show()
                    (dialog as AlertDialog).listView.setItemChecked(which, false)
                } else {
                    if (isChecked) selectedPackages.add(clickedPackage) else selectedPackages.remove(clickedPackage)
                }
            }
            .setPositiveButton("Kaydet") { dialog, _ ->
                prefs.setStringList(PreferenceStore.KEY_SELECTED_APPS, selectedPackages.toList())
                renderSelectedApps()
                dialog.dismiss()
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    private fun renderSelectedApps() {
        chipContainer.removeAllViews()
        val pm = requireContext().packageManager
        val selectedApps = prefs.getStringList(PreferenceStore.KEY_SELECTED_APPS)

        if (selectedApps.isEmpty()) {
            val tv = TextView(requireContext()).apply { text = getString(R.string.no_selected_apps) }
            chipContainer.addView(tv)
        } else {
            selectedApps.forEach { pkg ->
                val appLabel = runCatching {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                }.getOrDefault(pkg)
                val chipView = TextView(requireContext()).apply {
                    text = appLabel
                    setPadding(16, 8, 16, 8)
                    setBackgroundResource(R.drawable.chip_background)
                }
                chipContainer.addView(chipView)
            }
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(requireContext().contentResolver, "enabled_notification_listeners")
        val componentName = "${requireContext().packageName}/${AutomationService::class.java.name}"
        return enabled?.contains(componentName) == true
    }
}

