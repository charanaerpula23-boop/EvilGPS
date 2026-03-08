package com.charanhyper.tech.greydailer

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

enum class TravelState { IDLE, FETCHING, ROUTE_READY, TRAVELLING, ARRIVED, ERROR }

class TravelViewModel(app: Application) : AndroidViewModel(app) {

    var startLat by mutableStateOf("37.7749")
        private set
    var startLng by mutableStateOf("-122.4194")
        private set
    var endLat by mutableStateOf("37.3382")
        private set
    var endLng by mutableStateOf("-121.8863")
        private set
    var speedKmh by mutableStateOf("50")
        private set
    var travelState by mutableStateOf(TravelState.IDLE)
        private set
    var errorMessage by mutableStateOf("")
        private set
    var routeInfo by mutableStateOf("")
        private set
    var progress by mutableStateOf(0f)
        private set

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                MockLocationService.BROADCAST_STATUS -> {
                    intent.getStringExtra(MockLocationService.EXTRA_ERROR)?.let { msg ->
                        errorMessage = msg
                        travelState = TravelState.ERROR
                    }
                }
                MockLocationService.BROADCAST_TRAVEL_DONE -> {
                    travelState = TravelState.ARRIVED
                    progress = 1f
                }
                MockLocationService.BROADCAST_TRAVEL_PROGRESS -> {
                    progress = intent.getFloatExtra(MockLocationService.EXTRA_PROGRESS, 0f)
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(MockLocationService.BROADCAST_STATUS)
            addAction(MockLocationService.BROADCAST_TRAVEL_DONE)
            addAction(MockLocationService.BROADCAST_TRAVEL_PROGRESS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            app.registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            app.registerReceiver(statusReceiver, filter)
        }
    }

    fun onStartLatChange(v: String) { startLat = v }
    fun onStartLngChange(v: String) { startLng = v }
    fun onEndLatChange(v: String) { endLat = v }
    fun onEndLngChange(v: String) { endLng = v }
    fun onSpeedChange(v: String) { speedKmh = v }
    fun clearError() { errorMessage = ""; if (travelState == TravelState.ERROR) travelState = TravelState.IDLE }
    fun onStartPicked(lat: Double, lng: Double) {
        startLat = "%.6f".format(lat)
        startLng = "%.6f".format(lng)
    }
    fun onEndPicked(lat: Double, lng: Double) {
        endLat = "%.6f".format(lat)
        endLng = "%.6f".format(lng)
    }

    fun fetchRoute() {
        val sLatVal = startLat.toDoubleOrNull() ?: return setError("Invalid start latitude")
        val sLngVal = startLng.toDoubleOrNull() ?: return setError("Invalid start longitude")
        val eLat = endLat.toDoubleOrNull() ?: return setError("Invalid end latitude")
        val eLng = endLng.toDoubleOrNull() ?: return setError("Invalid end longitude")

        travelState = TravelState.FETCHING
        routeInfo = ""

        // OSRM expects longitude,latitude order
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = "https://router.project-osrm.org/route/v1/driving/" +
                    "$sLngVal,$sLatVal;$eLng,$eLat?overview=full&geometries=geojson"
                val response = URL(url).readText()
                val json = JSONObject(response)

                if (json.getString("code") != "Ok") {
                    val msg = json.optString("message", "No route found")
                    withContext(Dispatchers.Main) { setError("Routing failed: $msg") }
                    return@launch
                }

                val routeObj  = json.getJSONArray("routes").getJSONObject(0)
                val coords    = routeObj.getJSONObject("geometry").getJSONArray("coordinates")
                val distM     = routeObj.getDouble("distance")
                val durS      = routeObj.getDouble("duration")

                val points = (0 until coords.length()).map { i ->
                    val pt = coords.getJSONArray(i)
                    LatLng(lat = pt.getDouble(1), lng = pt.getDouble(0))
                }

                RouteRepository.route           = points
                RouteRepository.distanceMeters  = distM
                RouteRepository.durationSeconds = durS

                withContext(Dispatchers.Main) {
                    routeInfo = "%.1f km · ~%d min · %d waypoints".format(
                        distM / 1000.0,
                        (durS / 60).toInt(),
                        points.size
                    )
                    travelState = TravelState.ROUTE_READY
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { setError("Fetch failed: ${e.message}") }
            }
        }
    }

    fun startTravel() {
        if (RouteRepository.route.size < 2) { setError("No route available. Fetch route first."); return }
        val speed = speedKmh.toFloatOrNull() ?: 50f
        errorMessage = ""
        progress = 0f
        val intent = Intent(getApplication(), MockLocationService::class.java).apply {
            action = MockLocationService.ACTION_TRAVEL_START
            putExtra(MockLocationService.EXTRA_SPEED_KMH, speed)
        }
        getApplication<Application>().startForegroundService(intent)
        travelState = TravelState.TRAVELLING
    }

    fun stopTravel() {
        val intent = Intent(getApplication(), MockLocationService::class.java).apply {
            action = MockLocationService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
        travelState = TravelState.ROUTE_READY
    }

    private fun setError(msg: String) {
        errorMessage = msg
        travelState = TravelState.ERROR
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(statusReceiver)
    }
}
