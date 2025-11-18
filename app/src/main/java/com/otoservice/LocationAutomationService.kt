package com.otoservice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.otoservice.LocationDefaults.RANDOM_DISTRICT
import kotlin.random.Random

class LocationAutomationService : Service() {

    private val prefs by lazy { PreferenceStore.get(this) }
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var locationManager: LocationManager
    private val providerName = LocationManager.GPS_PROVIDER
    private val NOTIFICATION_ID = 101
    private val CHANNEL_ID = "LocationServiceChannel"

    private val districtCoordinates = mapOf(
        "Adalar" to (40.8711 to 29.1231), "Arnavutköy" to (41.1833 to 28.7450),
        "Ataşehir" to (40.9898 to 29.1223), "Avcılar" to (40.9796 to 28.7194),
        "Bağcılar" to (41.0375 to 28.8461), "Bahçelievler" to (41.0003 to 28.8517),
        "Bakırköy" to (40.9833 to 28.8667), "Başakşehir" to (41.0967 to 28.8083),
        "Bayrampaşa" to (41.0450 to 28.9050), "Beşiktaş" to (41.0442 to 29.0028),
        "Beykoz" to (41.1333 to 29.0833), "Beylikdüzü" to (41.0069 to 28.6436),
        "Beyoğlu" to (41.0333 to 28.9833), "Büyükçekmece" to (41.0167 to 28.5833),
        "Çatalca" to (41.1456 to 28.4636), "Çekmeköy" to (41.0333 to 29.2333),
        "Esenler" to (41.0469 to 28.8681), "Esenyurt" to (41.0333 to 28.6833),
        "Eyüpsultan" to (41.0500 to 28.9333), "Fatih" to (41.0167 to 28.9500),
        "Gaziosmanpaşa" to (41.0667 to 28.9167), "Güngören" to (41.0250 to 28.8667),
        "Kadıköy" to (40.9833 to 29.0333), "Kağıthane" to (41.0833 to 28.9667),
        "Kartal" to (40.9000 to 29.2000), "Küçükçekmece" to (41.0167 to 28.7667),
        "Maltepe" to (40.9500 to 29.1333), "Pendik" to (40.8833 to 29.2500),
        "Sancaktepe" to (41.0167 to 29.2167), "Sarıyer" to (41.1667 to 29.0500),
        "Silivri" to (41.0719 to 28.2436), "Sultanbeyli" to (40.9667 to 29.2667),
        "Sultangazi" to (41.1167 to 28.8833), "Şile" to (41.1756 to 29.6133),
        "Şişli" to (41.0667 to 28.9833), "Tuzla" to (40.8167 to 29.3167),
        "Ümraniye" to (41.0167 to 29.1167), "Üsküdar" to (41.0333 to 29.0167),
        "Zeytinburnu" to (40.9936 to 28.8942)
    )

    private val runnable = object : Runnable {
        override fun run() {
            if (prefs.getBoolean(PreferenceStore.KEY_LOCATION_ENABLED, false)) {
                mockLocation()
            }
            val minutes = try {
                prefs.getLong(PreferenceStore.KEY_LOCATION_INTERVAL_MIN, 15L)
            } catch (e: ClassCastException) {
                val badValue = prefs.getInt(PreferenceStore.KEY_LOCATION_INTERVAL_MIN, 15)
                prefs.setLong(PreferenceStore.KEY_LOCATION_INTERVAL_MIN, badValue.toLong())
                Log.w("OtoService", "Bozuk konum aralığı verisi düzeltildi.")
                badValue.toLong()
            }
            handler.postDelayed(this, minutes * 60 * 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        // HATA DÜZELTMESİ: Servisin sistem tarafından kapatılmasını önlemek için Foreground Service yapıldı.
        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            locationManager.addTestProvider(
                providerName, false, false, false, false, true, true, true,
                1, 1
            )
            locationManager.setTestProviderEnabled(providerName, true)
            Log.d("OtoService", "Sahte konum sağlayıcısı başarıyla eklendi.")
        } catch (e: SecurityException) {
            Log.e("OtoService", "Sahte konum sağlayıcısı eklenemedi. Geliştirici Seçenekleri\'nden bu uygulama için \'Sahte Konum İzni\' verilmemiş olabilir.", e)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.removeCallbacks(runnable)
        handler.post(runnable)
        return START_STICKY
    }

    override fun onDestroy() {
        stopForeground(true)
        handler.removeCallbacks(runnable)
        try {
            locationManager.removeTestProvider(providerName)
            Log.d("OtoService", "Sahte konum sağlayıcısı kaldırıldı.")
        } catch (e: Exception) {
            Log.e("OtoService", "Sahte konum sağlayıcısı kaldırılamadı.", e)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Konum Otomasyon Servisi",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OtoService Aktif")
            .setContentText("Konum otomasyonu arka planda çalışıyor.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun mockLocation() {
        val selectedDistrict = prefs.getString(PreferenceStore.KEY_LOCATION_DISTRICT, RANDOM_DISTRICT)
            ?: RANDOM_DISTRICT
        val centerDistrict = if (selectedDistrict == RANDOM_DISTRICT) {
            districtCoordinates.keys.random()
        } else {
            selectedDistrict
        }
        val centerCoords = districtCoordinates[centerDistrict] ?: (41.0082 to 28.9784)

        val latOffset = Random.nextDouble(-0.045, 0.045)
        val lonOffset = Random.nextDouble(-0.045, 0.045)
        
        val newLat = centerCoords.first + latOffset
        val newLon = centerCoords.second + lonOffset

        try {
            val mockLocation = Location(providerName).apply {
                latitude = newLat
                longitude = newLon
                altitude = 0.0
                accuracy = 10f
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                time = System.currentTimeMillis()
            }
            locationManager.setTestProviderLocation(providerName, mockLocation)
            Log.d("OtoService", "Sahte konum sisteme bildirildi ($centerDistrict): $newLat, $newLon")
        } catch (e: Exception) {
            Log.e("OtoService", "Sahte konum sisteme bildirilemedi.", e)
        }
    }
}
