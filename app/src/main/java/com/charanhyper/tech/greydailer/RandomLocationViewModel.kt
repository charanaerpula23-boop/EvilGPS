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

class RandomLocationViewModel(app: Application) : AndroidViewModel(app) {

    var cityName by mutableStateOf("Waiting for first city update")
        private set
    var latitude by mutableStateOf("-")
        private set
    var longitude by mutableStateOf("-")
        private set
    var isMocking by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf("")
        private set

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                MockLocationService.BROADCAST_STATUS -> {
                    intent.getStringExtra(MockLocationService.EXTRA_ERROR)?.let { msg ->
                        errorMessage = msg
                        isMocking = false
                    }
                }
                MockLocationService.BROADCAST_RANDOM_CITY -> {
                    val city = intent.getStringExtra(MockLocationService.EXTRA_CITY)
                    val lat = intent.getDoubleExtra(MockLocationService.EXTRA_LAT, 0.0)
                    val lng = intent.getDoubleExtra(MockLocationService.EXTRA_LNG, 0.0)
                    cityName = city ?: "Unknown city"
                    latitude = "%.6f".format(lat)
                    longitude = "%.6f".format(lng)
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(MockLocationService.BROADCAST_STATUS)
            addAction(MockLocationService.BROADCAST_RANDOM_CITY)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            app.registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            app.registerReceiver(statusReceiver, filter)
        }
    }

    fun clearError() {
        errorMessage = ""
    }

    fun startRandomMocking() {
        errorMessage = ""
        val intent = Intent(getApplication(), MockLocationService::class.java).apply {
            action = MockLocationService.ACTION_RANDOM_START
        }
        getApplication<Application>().startForegroundService(intent)
        isMocking = true
    }

    fun stopRandomMocking() {
        val intent = Intent(getApplication(), MockLocationService::class.java).apply {
            action = MockLocationService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
        isMocking = false
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(statusReceiver)
    }
}
