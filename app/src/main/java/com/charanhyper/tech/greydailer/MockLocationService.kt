package com.charanhyper.tech.greydailer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class MockLocationService : Service() {

    companion object {
        const val ACTION_START        = "com.charanhyper.tech.greydailer.START_MOCK"
        const val ACTION_STOP         = "com.charanhyper.tech.greydailer.STOP_MOCK"
        const val ACTION_TRAVEL_START = "com.charanhyper.tech.greydailer.TRAVEL_START"
        const val ACTION_RESTORE      = "com.charanhyper.tech.greydailer.RESTORE_MOCK"
        const val EXTRA_LAT           = "extra_lat"
        const val EXTRA_LNG           = "extra_lng"
        const val EXTRA_ACC           = "extra_acc"
        const val EXTRA_SPEED_KMH     = "extra_speed_kmh"
        const val BROADCAST_STATUS          = "com.charanhyper.tech.greydailer.MOCK_STATUS"
        const val BROADCAST_TRAVEL_DONE     = "com.charanhyper.tech.greydailer.TRAVEL_DONE"
        const val BROADCAST_TRAVEL_PROGRESS = "com.charanhyper.tech.greydailer.TRAVEL_PROGRESS"
        const val EXTRA_ERROR    = "extra_error"
        const val EXTRA_PROGRESS = "extra_progress"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "mock_location_channel"
        private const val PREFS_NAME = "mock_location_service_state"
        private const val KEY_MODE = "mode"
        private const val KEY_LAT = "lat"
        private const val KEY_LNG = "lng"
        private const val KEY_ACC = "acc"
        private const val KEY_SPEED_KMH = "speed_kmh"
        private const val KEY_ROUTE = "route"
        private const val MODE_STATIC = "static"
        private const val MODE_TRAVEL = "travel"
        private const val TAG = "MockLocationService"
    }

    private lateinit var locationManager: LocationManager
    private var mockJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val servicePrefs by lazy {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
                val lng = intent.getDoubleExtra(EXTRA_LNG, 0.0)
                val acc = intent.getFloatExtra(EXTRA_ACC, 5f)
                persistStaticSession(lat, lng, acc)
                startForeground(NOTIFICATION_ID, buildStaticNotification(lat, lng))
                startMocking(lat, lng, acc)
            }
            ACTION_TRAVEL_START -> {
                val speedKmh = intent.getFloatExtra(EXTRA_SPEED_KMH, 50f)
                if (!persistTravelSession(speedKmh)) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startForeground(NOTIFICATION_ID, buildTravelNotification(speedKmh))
                startTravel(speedKmh)
            }
            ACTION_STOP -> {
                clearPersistedSession()
                stopMocking()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_RESTORE, null -> restorePersistedSession()
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (!hasPersistedSession()) {
            return
        }
        val restartIntent = Intent(applicationContext, MockLocationService::class.java).apply {
            action = ACTION_RESTORE
        }
        try {
            startForegroundService(restartIntent)
        } catch (_: Exception) {
            startService(restartIntent)
        }
    }

    // ── Static mock ────────────────────────────────────────────────────────────

    private fun startMocking(lat: Double, lng: Double, acc: Float) {
        removeTestProvider(LocationManager.GPS_PROVIDER)
        removeTestProvider(LocationManager.NETWORK_PROVIDER)
        val gpsOk = addTestProvider(LocationManager.GPS_PROVIDER)
        val netOk = addTestProvider(LocationManager.NETWORK_PROVIDER)
        if (!gpsOk && !netOk) return
        mockJob?.cancel()
        mockJob = scope.launch {
            while (isActive) {
                if (gpsOk) pushMockLocation(LocationManager.GPS_PROVIDER, lat, lng, acc)
                if (netOk) pushMockLocation(LocationManager.NETWORK_PROVIDER, lat, lng, acc)
                delay(500L)
            }
        }
    }

    // ── Travel mode ────────────────────────────────────────────────────────────

    private fun startTravel(speedKmh: Float) {
        val route = RouteRepository.route
        if (route.size < 2) {
            broadcastError("Route has fewer than 2 points. Fetch a route first.")
            clearPersistedSession()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        removeTestProvider(LocationManager.GPS_PROVIDER)
        removeTestProvider(LocationManager.NETWORK_PROVIDER)
        val gpsOk = addTestProvider(LocationManager.GPS_PROVIDER)
        val netOk = addTestProvider(LocationManager.NETWORK_PROVIDER)
        if (!gpsOk && !netOk) return

        val speedMs     = speedKmh / 3.6           // m/s
        val intervalMs  = 500L
        val distPerTick = speedMs * (intervalMs / 1000.0)

        // Precompute cumulative segment distances
        val cumDist = ArrayList<Double>(route.size)
        cumDist.add(0.0)
        for (i in 1 until route.size) {
            cumDist.add(cumDist[i - 1] + haversineMeters(route[i - 1], route[i]))
        }
        val totalDist = cumDist.last()

        mockJob?.cancel()
        mockJob = scope.launch {
            var traveled = 0.0
            while (isActive && traveled <= totalDist) {
                val pos = interpolateRoute(route, cumDist, traveled)
                if (gpsOk) pushMockLocation(LocationManager.GPS_PROVIDER, pos.lat, pos.lng, 5f)
                if (netOk) pushMockLocation(LocationManager.NETWORK_PROVIDER, pos.lat, pos.lng, 5f)
                sendBroadcast(Intent(BROADCAST_TRAVEL_PROGRESS).apply {
                    putExtra(EXTRA_PROGRESS, (traveled / totalDist).toFloat().coerceIn(0f, 1f))
                    setPackage(packageName)
                })
                traveled += distPerTick
                delay(intervalMs)
            }
            // Push final destination
            val last = route.last()
            if (gpsOk) pushMockLocation(LocationManager.GPS_PROVIDER, last.lat, last.lng, 5f)
            if (netOk) pushMockLocation(LocationManager.NETWORK_PROVIDER, last.lat, last.lng, 5f)
            sendBroadcast(Intent(BROADCAST_TRAVEL_DONE).apply { setPackage(packageName) })
            clearPersistedSession()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    // ── Geometry helpers ───────────────────────────────────────────────────────

    private fun haversineMeters(from: LatLng, to: LatLng): Double {
        val r    = 6_371_000.0
        val dLat = Math.toRadians(to.lat - from.lat)
        val dLng = Math.toRadians(to.lng - from.lng)
        val a    = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(from.lat)) * cos(Math.toRadians(to.lat)) * sin(dLng / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }

    private fun interpolateRoute(
        route: List<LatLng>,
        cumDist: List<Double>,
        traveled: Double
    ): LatLng {
        if (traveled <= 0.0) return route.first()
        if (traveled >= cumDist.last()) return route.last()
        val idx    = cumDist.indexOfFirst { it > traveled }.coerceAtLeast(1) - 1
        val segLen = cumDist[idx + 1] - cumDist[idx]
        val t      = if (segLen > 0) (traveled - cumDist[idx]) / segLen else 0.0
        return LatLng(
            lat = route[idx].lat + t * (route[idx + 1].lat - route[idx].lat),
            lng = route[idx].lng + t * (route[idx + 1].lng - route[idx].lng)
        )
    }

    // ── Provider management ────────────────────────────────────────────────────

    private fun stopMocking() {
        mockJob?.cancel()
        mockJob = null
        removeTestProvider(LocationManager.GPS_PROVIDER)
        removeTestProvider(LocationManager.NETWORK_PROVIDER)
    }

    /** Returns true if provider was added successfully. */
    private fun addTestProvider(provider: String): Boolean {
        return try {
            locationManager.addTestProvider(
                provider,
                false, false, false, false,
                false, true, true,
                Criteria.POWER_LOW, Criteria.ACCURACY_FINE
            )
            locationManager.setTestProviderEnabled(provider, true)
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "addTestProvider($provider) SecurityException: ${e.message}")
            broadcastError("Security error \u2013 is this app selected as the mock location app in Dev Options?")
            false
        } catch (e: Exception) {
            Log.e(TAG, "addTestProvider($provider) failed: ${e.message}")
            broadcastError("${e.javaClass.simpleName}: ${e.message}")
            false
        }
    }

    private fun removeTestProvider(provider: String) {
        try {
            locationManager.setTestProviderEnabled(provider, false)
            locationManager.removeTestProvider(provider)
        } catch (_: Exception) {}
    }

    private fun pushMockLocation(provider: String, lat: Double, lng: Double, acc: Float) {
        val loc = Location(provider).apply {
            latitude  = lat
            longitude = lng
            accuracy  = acc
            altitude  = 0.0
            speed     = 0f
            bearing   = 0f
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        try {
            locationManager.setTestProviderLocation(provider, loc)
        } catch (e: Exception) {
            Log.e(TAG, "setTestProviderLocation($provider): ${e.message}")
        }
    }

    private fun broadcastError(message: String) {
        sendBroadcast(Intent(BROADCAST_STATUS).apply {
            putExtra(EXTRA_ERROR, message)
            setPackage(packageName)
        })
    }

    private fun hasPersistedSession(): Boolean =
        !servicePrefs.getString(KEY_MODE, null).isNullOrEmpty()

    private fun persistStaticSession(lat: Double, lng: Double, acc: Float) {
        servicePrefs.edit()
            .putString(KEY_MODE, MODE_STATIC)
            .putString(KEY_LAT, lat.toString())
            .putString(KEY_LNG, lng.toString())
            .putFloat(KEY_ACC, acc)
            .remove(KEY_SPEED_KMH)
            .remove(KEY_ROUTE)
            .apply()
    }

    private fun persistTravelSession(speedKmh: Float): Boolean {
        val route = RouteRepository.route
        if (route.size < 2) {
            broadcastError("No route available. Fetch a route first.")
            clearPersistedSession()
            return false
        }
        servicePrefs.edit()
            .putString(KEY_MODE, MODE_TRAVEL)
            .putFloat(KEY_SPEED_KMH, speedKmh)
            .putString(KEY_ROUTE, serializeRoute(route))
            .remove(KEY_LAT)
            .remove(KEY_LNG)
            .remove(KEY_ACC)
            .apply()
        return true
    }

    private fun restorePersistedSession() {
        when (servicePrefs.getString(KEY_MODE, null)) {
            MODE_STATIC -> {
                val lat = servicePrefs.getString(KEY_LAT, null)?.toDoubleOrNull()
                val lng = servicePrefs.getString(KEY_LNG, null)?.toDoubleOrNull()
                val acc = servicePrefs.getFloat(KEY_ACC, 5f)
                if (lat == null || lng == null) {
                    clearPersistedSession()
                    stopSelf()
                    return
                }
                startForeground(NOTIFICATION_ID, buildStaticNotification(lat, lng))
                startMocking(lat, lng, acc)
            }
            MODE_TRAVEL -> {
                val speedKmh = servicePrefs.getFloat(KEY_SPEED_KMH, 50f)
                val route = deserializeRoute(servicePrefs.getString(KEY_ROUTE, "").orEmpty())
                if (route.size < 2) {
                    broadcastError("Saved travel session could not be restored.")
                    clearPersistedSession()
                    stopSelf()
                    return
                }
                RouteRepository.route = route
                startForeground(NOTIFICATION_ID, buildTravelNotification(speedKmh))
                startTravel(speedKmh)
            }
            else -> stopSelf()
        }
    }

    private fun clearPersistedSession() {
        servicePrefs.edit().clear().apply()
    }

    private fun serializeRoute(route: List<LatLng>): String =
        route.joinToString("|") { "${it.lat},${it.lng}" }

    private fun deserializeRoute(serialized: String): List<LatLng> =
        serialized.split('|').mapNotNull { point ->
            val parts = point.split(',')
            if (parts.size != 2) {
                return@mapNotNull null
            }
            val lat = parts[0].toDoubleOrNull()
            val lng = parts[1].toDoubleOrNull()
            if (lat == null || lng == null) {
                null
            } else {
                LatLng(lat = lat, lng = lng)
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        stopMocking()
        scope.cancel()
    }

    // ── Notifications ──────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Mock Location Service", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Keeps mock location active in the background" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildStaticNotification(lat: Double, lng: Double): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mock Location Active")
            .setContentText("%.6f, %.6f".format(lat, lng))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

    private fun buildTravelNotification(speedKmh: Float): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Travel Simulation Active")
            .setContentText("Speed: ${speedKmh.toInt()} km/h")
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setOngoing(true)
            .build()
}

