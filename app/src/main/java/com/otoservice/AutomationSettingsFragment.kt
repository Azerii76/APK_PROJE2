package com.otoservice

import android.app.AlertDialog
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
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
        etReply.setText(prefs.getString(PreferenceStore.KEY_REPLY_TEXT, "Merhaba, otomatik yanıt"))
        renderSelectedApps()

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, FrequencyDefaults.replySeconds)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        val savedFreq = prefs.getLong(PreferenceStore.KEY_FREQUENCY_SECONDS, 5L).toInt()
        spinner.setSelection(FrequencyDefaults.replySeconds.indexOf(savedFreq).coerceAtLeast(0))

        switchAuto.setOnCheckedChangeListener { _, isChecked ->
            prefs.setBoolean(PreferenceStore.KEY_AUTO_REPLY_ENABLED, isChecked)
        }
        btnSelect.setOnClickListener { showAppChooserDialog() }
        spinner.setOnItemSelectedListener(SimpleItemSelectedListener { index ->
            val value = FrequencyDefaults.replySeconds.getOrElse(index) { 5 }
            prefs.setLong(PreferenceStore.KEY_FREQUENCY_SECONDS, value.toLong())
        })

        return view
    }

    override fun onPause() {
        super.onPause()
        prefs.setString(PreferenceStore.KEY_REPLY_TEXT, etReply.text.toString().trim())
    }

    // HATA DÜZELTMESİ: Bu fonksiyon çökmelere karşı tamamen güvenli hale getirildi.
    private fun getInstalledApps(pm: PackageManager): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()
        val installedPackages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (appInfo in installedPackages) {
            try {
                // Adım 1: Sadece kullanıcının başlatabileceği (menüde görünen) uygulamaları değerlendir.
                if (pm.getLaunchIntentForPackage(appInfo.packageName) == null) {
                    continue
                }

                // Adım 2: Saf sistem uygulamalarını filtrele (güncellenmiş veya sonradan yüklenmiş olanlar hariç).
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                if (isSystemApp && !isUpdatedSystemApp) {
                    continue
                }

                // Adım 3: Uygulama adını güvenli bir şekilde al. Adı olmayanları atla.
                val appName = appInfo.loadLabel(pm)?.toString()
                if (appName.isNullOrEmpty()) {
                    continue
                }

                apps.add(AppInfo(appName, appInfo.packageName))

            } catch (e: Exception) {
                // Herhangi bir paketi okurken hata olursa, onu atla ve devam et.
                Log.w("OtoService", "Uygulama listesi oluşturulurken bir paket atlandı: ${appInfo.packageName}", e)
            }
        }
        return apps.sortedBy { it.name.lowercase() } // İsme göre sırala
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
            val tv = TextView(requireContext()).apply { text = "Henüz uygulama seçilmedi." }
            chipContainer.addView(tv)
        } else {
            selectedApps.forEach { pkg ->
                val appLabel = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
                val chipView = TextView(requireContext()).apply {
                    text = appLabel
                    setPadding(16, 8, 16, 8)
                    setBackgroundResource(R.drawable.chip_background)
                }
                chipContainer.addView(chipView)
            }
        }
    }
}
