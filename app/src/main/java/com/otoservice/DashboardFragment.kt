package com.otoservice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView

class DashboardFragment : Fragment() {

    private lateinit var permissionCard: MaterialCardView
    private lateinit var permissionStatusText: TextView
    private lateinit var permissionButton: Button

    // İzinleri istemek için launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // İzin istendikten sonra UI'ı tazele
        updatePermissionUI()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        permissionCard = view.findViewById(R.id.cardPermissions)
        permissionStatusText = view.findViewById(R.id.tvPermissionStatus)
        permissionButton = view.findViewById(R.id.btnOpenPermissions)
        setupCards(view)
        return view
    }

    override fun onResume() {
        super.onResume()
        // Ekran her göründüğünde bilgileri ve izinleri tazele
        view?.let { 
            setupCards(it)
            updatePermissionUI()
        }
    }

    private fun setupCards(view: View) {
        val prefs = PreferenceStore.get(requireContext())
        val lm = LicenseManager(requireContext())

        val tvLicense = view.findViewById<TextView>(R.id.tvLicenseStatus)
        val btnLicense = view.findViewById<Button>(R.id.btnOpenLicense)

        val status = lm.checkLicenseStatus()
        val remaining = lm.getRemainingDays()
        tvLicense.text = if (status == LicenseManager.STATUS_ACTIVE) {
            "Durum: $status (Kalan Süre: $remaining gün)"
        } else {
            "Durum: $status"
        }

        btnLicense.setOnClickListener {
            startActivity(Intent(requireContext(), LicenseActivity::class.java))
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(requireContext().contentResolver, "enabled_notification_listeners")
        val componentName = "${requireContext().packageName}/${AutomationService::class.java.name}"
        return enabledListeners?.contains(componentName) == true
    }

    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // HATA DÜZELTMESİ: Güvenilir olmayan ve derleme hatasına neden olan 'isMockLocationEnabled' kontrolü kaldırıldı.
    // Bu kontrol artık doğrudan 'fragment_location_settings.xml' içindeki uyarı metni ile kullanıcıya bildirilmektedir.

    private fun updatePermissionUI() {
        val missingPermissions = mutableListOf<String>()
        var nextAction: (() -> Unit)? = null

        if (!isNotificationServiceEnabled()) {
            missingPermissions.add("Bildirim Okuma İzni")
            if (nextAction == null) nextAction = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        }
        if (!isLocationPermissionGranted()) {
            missingPermissions.add("Konum İzni (ACCESS_FINE_LOCATION)")
            if (nextAction == null) nextAction = { requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
        }

        // Sahte Konum İzni kontrolü artık kullanıcıya metinle bildirildiği için buradan kaldırıldı.

        if (missingPermissions.isEmpty()) {
            permissionCard.visibility = View.GONE // İzinler tamamsa kartı gizle
        } else {
            permissionCard.visibility = View.VISIBLE // Eksik izin varsa kartı göster
            permissionCard.strokeWidth = 4 // Uyarı çerçevesi
            permissionStatusText.text = "Uygulamanın tam çalışması için eksik izinler var: \n- ${missingPermissions.joinToString("\n- ")}"
            permissionButton.visibility = View.VISIBLE
            permissionButton.text = "Eksik İzni Gider"
            permissionButton.setOnClickListener { nextAction?.invoke() }
        }
    }
}
