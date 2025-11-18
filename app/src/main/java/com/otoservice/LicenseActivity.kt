package com.otoservice

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LicenseActivity : AppCompatActivity() {

    private lateinit var manager: LicenseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license)
        manager = LicenseManager(this)

        val etKey = findViewById<EditText>(R.id.etLicenseKey)
        val tvDevice = findViewById<TextView>(R.id.tvDeviceId)
        val tvInfo = findViewById<TextView>(R.id.tvLicenseInfo)
        val btnActivate = findViewById<Button>(R.id.btnActivate)

        tvDevice.text = "Cihaz ID: ${manager.getDeviceId()}"
        updateLicenseInfo()

        btnActivate.setOnClickListener {
            val key = etKey.text.toString().trim()
            if (key.isBlank()) {
                Toast.makeText(this, "Lütfen bir lisans anahtarı girin.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val result = manager.validateAndActivateLicense(key)
            Toast.makeText(this, "Sonuç: $result", Toast.LENGTH_LONG).show()
            updateLicenseInfo()

            // Aktivasyon başarılıysa ve ana aktiviteye geri dönmek isterseniz:
            if (result == LicenseManager.STATUS_ACTIVE) {
                // Aktivasyon sonrası ana ekrana dönmek için bu kısmı etkinleştirebilirsiniz
                // finish()
            }
        }
    }

    private fun updateLicenseInfo() {
        val status = manager.checkLicenseStatus()
        val remainingDays = manager.getRemainingDays()
        val tvInfo = findViewById<TextView>(R.id.tvLicenseInfo)
        tvInfo.text = "Lisans Durumu: $status\nKalan Süre: $remainingDays gün"
    }
}
