package com.example.otoservice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Switch
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.card.MaterialCardView
import com.example.otoservice.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var licenseManager: LicenseManager
    private lateinit var prefs: PreferenceStore
    private var licenseState: LicenseState? = null

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { updatePermissionStatuses() }

    private val requestLocationPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { updatePermissionStatuses() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        licenseManager = LicenseManager(this)
        prefs = PreferenceStore(this)

        setupLicenseUI()
        setupAutoReplyUI()
        setupAppSelection()
        setupLocationUI()
        setupPermissionUI()
        setupPermissionRequests()

        refreshLicenseState()
    }

    override fun onResume() {
        super.onResume()
        refreshLicenseState()
        updatePermissionStatuses()
    }

    private fun setupLicenseUI() {
        binding.buttonValidate.setOnClickListener {
            val name = binding.inputLicenseName.text?.toString().orEmpty().trim()
            val code = binding.inputLicenseCode.text?.toString().orEmpty().trim()
            val expiry = binding.inputExpiry.text?.toString().orEmpty().trim()
            if (name.isBlank() || code.isBlank() || expiry.isBlank()) {
                Toast.makeText(this, "Lisans bilgilerini eksiksiz girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val valid = licenseManager.validateLicense(name, code, expiry)
            if (valid) {
                val expiryMillis = licenseManager.parseExpiry(expiry)
                val cloneFlag = AntiCloneChecker.isClonedOrTampered(this)
                val newState = LicenseState(
                    licenseName = name,
                    licenseCode = code,
                    expiryMillis = expiryMillis,
                    lastKnownTime = System.currentTimeMillis(),
                    timeTampered = false,
                    cloneTampered = cloneFlag
                )
                licenseManager.saveState(newState)
                licenseState = newState
                Toast.makeText(this, "Lisans başarıyla tanımlandı", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Lisans kodu geçersiz", Toast.LENGTH_SHORT).show()
            }
            refreshLicenseState()
        }
    }

    private fun refreshLicenseState() {
        val state = licenseManager.loadState()
        val updated = licenseManager.updateTimeIntegrity(state)
        val cloneNow = AntiCloneChecker.isClonedOrTampered(this)
        val merged = updated.copy(cloneTampered = updated.cloneTampered || cloneNow)
        licenseManager.saveState(merged)
        licenseState = merged

        val expired = licenseManager.isLicenseExpired(merged)
        val timeBlocked = merged.timeTampered
        val cloneBlocked = merged.cloneTampered
        val active = merged.isActive() && !expired && !timeBlocked && !cloneBlocked

        binding.textLicenseStatus.text = when {
            cloneBlocked -> "Durum: Klon tespit edildi."
            timeBlocked -> "Durum: Tarih/saat hatalı."
            expired -> "Durum: Lisans süresi doldu"
            active -> "Durum: Lisans geçerli"
            else -> "Durum: Lisans girilmedi"
        }

        val enabled = active
        setSectionEnabled(binding.cardAutoReply, enabled)
        setSectionEnabled(binding.cardAppSelection, enabled)
        setSectionEnabled(binding.cardLocation, enabled)
        setSectionEnabled(binding.cardPermissions, true)
        if (!active) {
            binding.switchAutoReply.isChecked = false
            prefs.autoReplyEnabled = false
            binding.switchLocation.isChecked = false
            prefs.locationEnabled = false
            stopService(Intent(this, LocationSpoofService::class.java))
        }
    }

    private fun setSectionEnabled(card: MaterialCardView, enabled: Boolean) {
        card.isEnabled = enabled
        card.alpha = if (enabled) 1f else 0.3f
        (0 until card.childCount).forEach { index ->
            card.getChildAt(index)?.isEnabled = enabled
        }
    }

    private fun setupAutoReplyUI() {
        binding.switchAutoReply.isChecked = prefs.autoReplyEnabled
        binding.inputReplyText.setText(prefs.autoReplyText)
        binding.inputDelay.setText(prefs.replyDelaySeconds.toString())
        binding.inputRepeat.setText(prefs.repeatDelayMinutes.toString())

        binding.inputReplyText.doAfterTextChanged { prefs.autoReplyText = it?.toString().orEmpty() }
        binding.inputDelay.doAfterTextChanged {
            val value = it?.toString()?.toIntOrNull()?.coerceIn(0, 60) ?: 0
            prefs.replyDelaySeconds = value
        }
        binding.inputRepeat.doAfterTextChanged {
            val value = it?.toString()?.toIntOrNull()?.coerceIn(1, 180) ?: 10
            prefs.repeatDelayMinutes = value
        }

        binding.switchAutoReply.setOnCheckedChangeListener { _: Switch, isChecked: Boolean ->
            prefs.autoReplyEnabled = isChecked
            if (isChecked && !isNotificationServiceEnabled()) {
                Toast.makeText(this, "Bildirim erişimi yok", Toast.LENGTH_SHORT).show()
                openNotificationAccess()
            }
        }

        binding.buttonNotificationSettings.setOnClickListener {
            openNotificationAccess()
        }
    }

    private fun setupAppSelection() {
        updateSelectedApps()
        binding.buttonSelectApps.setOnClickListener {
            val pm = packageManager
            val apps = pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                .sortedBy { pm.getApplicationLabel(it).toString() }
            if (apps.isEmpty()) {
                Toast.makeText(this, "Kullanıcı uygulaması bulunamadı", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val entries = apps.map { pm.getApplicationLabel(it).toString() }
            val pkgNames = apps.map { it.packageName }
            val checked = BooleanArray(apps.size) { prefs.selectedPackages.contains(pkgNames[it]) }
            AlertDialog.Builder(this)
                .setTitle("En fazla 6 uygulama seçin")
                .setMultiChoiceItems(entries.toTypedArray(), checked) { dialog, which, isChecked ->
                    val current = prefs.selectedPackages.toMutableSet()
                    val targetPackage = pkgNames[which]
                    if (isChecked) {
                        if (current.size >= 6 && !current.contains(targetPackage)) {
                            Toast.makeText(this, "En fazla 6 uygulama seçebilirsiniz", Toast.LENGTH_SHORT).show()
                            (dialog as? AlertDialog)?.listView?.setItemChecked(which, false)
                        } else {
                            current.add(targetPackage)
                        }
                    } else {
                        current.remove(targetPackage)
                    }
                    prefs.selectedPackages = current
                }
                .setPositiveButton("Tamam") { _, _ -> updateSelectedApps() }
                .setNegativeButton("İptal", null)
                .show()
        }
    }

    private fun updateSelectedApps() {
        val summary = if (prefs.selectedPackages.isEmpty()) {
            "Seçili uygulamalar: -"
        } else {
            val labels = prefs.selectedPackages.mapNotNull { pkg ->
                runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString() }
                    .getOrNull()
            }
            "Seçili uygulamalar: ${labels.joinToString(", ")}".ifBlank { "Seçili uygulamalar: -" }
        }
        binding.textSelectedApps.text = summary
    }

    private fun setupLocationUI() {
        binding.inputDuration.setText(prefs.districtDurationMinutes.toString())
        binding.switchLocation.isChecked = prefs.locationEnabled
        updateSelectedDistricts()

        binding.inputDuration.doAfterTextChanged {
            val value = it?.toString()?.toIntOrNull()?.coerceIn(5, 180) ?: 15
            prefs.districtDurationMinutes = value
        }

        binding.buttonSelectDistricts.setOnClickListener { showDistrictDialog() }

        binding.switchLocation.setOnCheckedChangeListener { _: Switch, isChecked: Boolean ->
            prefs.locationEnabled = isChecked
            if (isChecked) {
                startLocationServiceIfPossible()
            } else {
                stopService(Intent(this, LocationSpoofService::class.java))
            }
        }
    }

    private fun showDistrictDialog() {
        val districts = LocationSpoofService.DISTRICT_COORDINATES.keys.toList()
        val current = prefs.selectedDistricts.toMutableList()
        val checked = BooleanArray(districts.size) { current.contains(districts[it]) }
        AlertDialog.Builder(this)
            .setTitle("İlçe sırasını seçin")
            .setMultiChoiceItems(districts.toTypedArray(), checked) { _, which, isChecked ->
                val item = districts[which]
                if (isChecked) {
                    if (!current.contains(item)) {
                        current.add(item)
                    }
                } else {
                    current.remove(item)
                }
            }
            .setPositiveButton("Kaydet") { _, _ ->
                prefs.selectedDistricts = current.take(20) // güvenlik amacıyla kısıtlıyoruz
                updateSelectedDistricts()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun updateSelectedDistricts() {
        val text = if (prefs.selectedDistricts.isEmpty()) "Seçili ilçeler: -"
        else "Seçili ilçeler: ${prefs.selectedDistricts.joinToString(" → ")}"
        binding.textSelectedDistricts.text = text
    }

    private fun startLocationServiceIfPossible() {
        val duration = binding.inputDuration.text?.toString()?.toIntOrNull() ?: prefs.districtDurationMinutes
        prefs.districtDurationMinutes = duration
        if (prefs.selectedDistricts.isEmpty()) {
            Toast.makeText(this, "En az bir ilçe seçmelisiniz", Toast.LENGTH_SHORT).show()
            binding.switchLocation.isChecked = false
            return
        }
        if (!hasLocationPermissions()) {
            Toast.makeText(this, "Konum izni gerekli", Toast.LENGTH_SHORT).show()
            requestLocationPermission()
            binding.switchLocation.isChecked = false
            return
        }
        val intent = Intent(this, LocationSpoofService::class.java).apply {
            putStringArrayListExtra(LocationSpoofService.EXTRA_DISTRICTS, ArrayList(prefs.selectedDistricts))
            putExtra(LocationSpoofService.EXTRA_DURATION, duration)
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun setupPermissionUI() {
        binding.buttonNotificationAccess.setOnClickListener { openNotificationAccess() }
        binding.buttonRequestLocation.setOnClickListener { requestLocationPermission() }
        binding.buttonBattery.setOnClickListener { openBatterySettings() }
    }

    private fun updatePermissionStatuses() {
        binding.textNotificationPermission.text = "Bildirim erişimi: ${if (isNotificationServiceEnabled()) "Verildi" else "Verilmedi"}"
        val hasFine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        binding.textLocationPermission.text = "Konum izni: ${if (hasFine) "Verildi" else "Verilmedi"}"
        val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val ignoring = pm.isIgnoringBatteryOptimizations(packageName)
        binding.textBatteryPermission.text = "Pil optimizasyonu: ${if (ignoring) "Dahil değil" else "Açık"}"
    }

    private fun openNotificationAccess() {
        try {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } catch (_: Exception) {
            Toast.makeText(this, "Ayar açılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestLocationPermission() {
        requestLocationPermissions.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    private fun hasLocationPermissions(): Boolean {
        val fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun setupPermissionRequests() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openBatterySettings() {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        startActivity(intent)
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val sets = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return sets.contains(applicationContext.packageName)
    }
}
