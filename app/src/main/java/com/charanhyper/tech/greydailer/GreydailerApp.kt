package com.charanhyper.tech.greydailer

import android.app.Application
import org.osmdroid.config.Configuration

class GreydailerApp : Application() {
	override fun onCreate() {
		super.onCreate()
		Configuration.getInstance().apply {
			userAgentValue = packageName
			load(this@GreydailerApp, getSharedPreferences("osmdroid_prefs", MODE_PRIVATE))
		}
	}
}
