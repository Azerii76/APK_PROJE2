package com.otoservice

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val bottom = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottom.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> switchFragment(DashboardFragment())
                R.id.nav_automation -> switchFragment(AutomationSettingsFragment())
                R.id.nav_location -> switchFragment(LocationSettingsFragment())
                R.id.nav_logs -> switchFragment(LogFragment())
            }
            true
        }
        if (savedInstanceState == null) {
            bottom.selectedItemId = R.id.nav_dashboard
        }
    }

    override fun onResume() {
        super.onResume()
        // Uygulama her açıldığında veya ekrana dönüldüğünde izinleri kontrol et
        checkRequiredPermissions()
    }

    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    /**
     * Bildirim dinleme servisinin etkin olup olmadığını kontrol eder.
     */
    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val componentName = "${packageName}/${AutomationService::class.java.name}"
        return enabledListeners?.contains(componentName) == true
    }

    /**
     * Gerekli izinleri kontrol eder ve kullanıcıyı yönlendirir.
     */
    private fun checkRequiredPermissions() {
        // Sadece lisans aktif ise izin kontrolü yap
        if (LicenseManager(this).checkLicenseStatus() == LicenseManager.STATUS_ACTIVE) {
            if (!isNotificationServiceEnabled()) {
                // İzin verilmemişse, kullanıcıya bir bilgilendirme diyalogu göster.
                AlertDialog.Builder(this)
                    .setTitle("Gerekli İzin")
                    .setMessage("Otomatik yanıt özelliğinin doğru çalışabilmesi için, uygulamanın bildirimlerinizi okuma iznine ihtiyacı var. Lütfen açılan ekrandan \"OtoService\" uygulamasını bularak izni etkinleştirin.")
                    .setPositiveButton("Ayarları Aç") { _, _ ->
                        // Kullanıcıyı doğrudan Bildirim Erişimi ayarlarına yönlendir.
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                    .setNegativeButton("İptal", null)
                    .setCancelable(false) // Kullanıcının bu diyaloğu atlamasını engelle
                    .show()
            }
        }
    }
}
