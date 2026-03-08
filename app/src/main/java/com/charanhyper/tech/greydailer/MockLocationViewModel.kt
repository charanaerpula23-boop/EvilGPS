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

class MockLocationViewModel(app: Application) : AndroidViewModel(app) {

    var latitude by mutableStateOf("37.7749")
        private set
    var longitude by mutableStateOf("-122.4194")
        private set
    var accuracy by mutableStateOf("5.0")
        private set
    var isMocking by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf("")
        private set

    private val errorReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.getStringExtra(MockLocationService.EXTRA_ERROR)?.let { msg ->
                errorMessage = msg
                isMocking = false
            }
        }
    }

    init {
        val filter = IntentFilter(MockLocationService.BROADCAST_STATUS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            app.registerReceiver(errorReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            app.registerReceiver(errorReceiver, filter)
        }
    }

    fun onLatitudeChange(value: String) { latitude = value }
    fun onLongitudeChange(value: String) { longitude = value }
    fun onAccuracyChange(value: String) { accuracy = value }
    fun clearError() { errorMessage = "" }
    fun onLatLngPicked(lat: Double, lng: Double) {
        latitude = "%.6f".format(lat)
        longitude = "%.6f".format(lng)
    }

    fun startMocking() {
        val lat = latitude.toDoubleOrNull() ?: run {
            errorMessage = "Invalid latitude"
            return
        }
        val lng = longitude.toDoubleOrNull() ?: run {
            errorMessage = "Invalid longitude"
            return
        }
        val acc = accuracy.toFloatOrNull() ?: 5f
        errorMessage = ""
        val intent = Intent(getApplication(), MockLocationService::class.java).apply {
            action = MockLocationService.ACTION_START
            putExtra(MockLocationService.EXTRA_LAT, lat)
            putExtra(MockLocationService.EXTRA_LNG, lng)
            putExtra(MockLocationService.EXTRA_ACC, acc)
        }
        getApplication<Application>().startForegroundService(intent)
        isMocking = true
    }

    fun stopMocking() {
        val intent = Intent(getApplication(), MockLocationService::class.java).apply {
            action = MockLocationService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
        isMocking = false
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(errorReceiver)
    }
}

