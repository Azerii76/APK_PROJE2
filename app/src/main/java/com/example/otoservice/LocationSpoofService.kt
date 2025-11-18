package com.example.otoservice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * İstanbul ilçeleri arasında sırayla gezinen foreground sahte konum servisi.
 */
class LocationSpoofService : Service() {
    private val scope = CoroutineScope(Job() + Dispatchers.Default)
    private lateinit var locationManager: LocationManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createChannel()
        Log.d(TAG, "LocationSpoofService hazır")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val districts = intent?.getStringArrayListExtra(EXTRA_DISTRICTS)?.toList().orEmpty()
        val durationMinutes = intent?.getIntExtra(EXTRA_DURATION, 15) ?: 15
        if (districts.isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        startSpoofLoop(districts, durationMinutes)
        return START_STICKY
    }

    private fun startSpoofLoop(districts: List<String>, durationMinutes: Int) {
        scope.launch {
            while (true) {
                for (district in districts) {
                    val center = DISTRICT_COORDINATES[district] ?: continue
                    val stayMillis = durationMinutes * 60 * 1000L
                    val start = System.currentTimeMillis()
                    while (System.currentTimeMillis() - start < stayMillis) {
                        applyMockLocation(center)
                        delay(Random.nextLong(20000L, 35000L))
                    }
                }
                // Tüm ilçeler tamamlandığında yeniden başa sarıyoruz.
            }
        }
    }

    private fun applyMockLocation(center: Pair<Double, Double>) {
        val hasPermission = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            Log.w(TAG, "Konum izni yok, mock uygulanamadı")
            return
        }
        try {
            val provider = LocationManager.GPS_PROVIDER
            if (!locationManager.allProviders.contains(provider)) {
                locationManager.addTestProvider(
                    provider,
                    false,
                    false,
                    false,
                    false,
                    true,
                    true,
                    true,
                    0,
                    5
                )
                locationManager.setTestProviderEnabled(provider, true)
            }
            val location = Location(provider).apply {
                val latOffset = Random.nextDouble(-0.005, 0.005)
                val lonOffset = Random.nextDouble(-0.005, 0.005)
                latitude = center.first + latOffset
                longitude = center.second + lonOffset
                accuracy = 5f
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = System.nanoTime()
            }
            locationManager.setTestProviderLocation(provider, location)
        } catch (e: Exception) {
            Log.e(TAG, "Mock konum atanamadı", e)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OtoService")
            .setContentText("Sahte konum aktif (İstanbul)")
            .setSmallIcon(R.drawable.ic_location)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Konum Servisi",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        private const val TAG = "LocationSpoofService"
        private const val CHANNEL_ID = "location_service_channel"
        private const val NOTIFICATION_ID = 9981

        const val EXTRA_DISTRICTS = "extra_districts"
        const val EXTRA_DURATION = "extra_duration"

        val DISTRICT_COORDINATES = mapOf(
            "Esenyurt" to Pair(41.0451, 28.6775),
            "Bağcılar" to Pair(41.0334, 28.8567),
            "Avcılar" to Pair(40.9800, 28.7214),
            "Kadıköy" to Pair(40.9923, 29.0250),
            "Beşiktaş" to Pair(41.0430, 29.0038),
            "Şişli" to Pair(41.0605, 28.9870),
            "Üsküdar" to Pair(41.0220, 29.0137),
            "Bakırköy" to Pair(40.9710, 28.8334),
            "Fatih" to Pair(41.0201, 28.9497),
            "Beyoğlu" to Pair(41.0351, 28.9770),
            "Ataşehir" to Pair(40.9922, 29.1271),
            "Maltepe" to Pair(40.9376, 29.1316),
            "Kartal" to Pair(40.8991, 29.1857),
            "Pendik" to Pair(40.8742, 29.2297),
            "Başakşehir" to Pair(41.0915, 28.8027)
        )
    }
}
