package com.charanhyper.tech.greydailer

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.charanhyper.tech.greydailer.ui.DisclaimerScreen
import com.charanhyper.tech.greydailer.ui.MapPickerScreen
import com.charanhyper.tech.greydailer.ui.MockLocationScreen
import com.charanhyper.tech.greydailer.ui.TravelScreen
import com.charanhyper.tech.greydailer.ui.theme.GreydailerTheme

enum class PickerMode { None, MockLocation, TravelStart, TravelEnd }

class MainActivity : ComponentActivity() {

    private val mockViewModel: MockLocationViewModel by viewModels()
    private val travelViewModel: TravelViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        setContent {
            GreydailerTheme {
                var selectedTab by remember { mutableStateOf(0) }
                var pickerMode by remember { mutableStateOf(PickerMode.None) }
                var showDisclaimer by remember { mutableStateOf(false) }

                // â”€â”€ Full-screen map picker overlay â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                if (showDisclaimer) {
                    DisclaimerScreen(onBack = { showDisclaimer = false })
                } else if (pickerMode != PickerMode.None) {
                    val initLat = when (pickerMode) {
                        PickerMode.MockLocation -> mockViewModel.latitude.toDoubleOrNull() ?: 37.7749
                        PickerMode.TravelStart  -> travelViewModel.startLat.toDoubleOrNull() ?: 37.7749
                        PickerMode.TravelEnd    -> travelViewModel.endLat.toDoubleOrNull() ?: 37.3382
                        else -> 37.7749
                    }
                    val initLng = when (pickerMode) {
                        PickerMode.MockLocation -> mockViewModel.longitude.toDoubleOrNull() ?: -122.4194
                        PickerMode.TravelStart  -> travelViewModel.startLng.toDoubleOrNull() ?: -122.4194
                        PickerMode.TravelEnd    -> travelViewModel.endLng.toDoubleOrNull() ?: -121.8863
                        else -> -122.4194
                    }
                    val pickerTitle = when (pickerMode) {
                        PickerMode.MockLocation -> "Pick Mock Location"
                        PickerMode.TravelStart  -> "Pick Start Location"
                        PickerMode.TravelEnd    -> "Pick End Location"
                        else -> "Pick Location"
                    }
                    MapPickerScreen(
                        initialLat = initLat,
                        initialLng = initLng,
                        title = pickerTitle,
                        onLocationPicked = { lat, lng ->
                            when (pickerMode) {
                                PickerMode.MockLocation -> mockViewModel.onLatLngPicked(lat, lng)
                                PickerMode.TravelStart  -> travelViewModel.onStartPicked(lat, lng)
                                PickerMode.TravelEnd    -> travelViewModel.onEndPicked(lat, lng)
                                else -> {}
                            }
                            pickerMode = PickerMode.None
                        },
                        onBack = { pickerMode = PickerMode.None },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // â”€â”€ Main scaffold with bottom nav â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background,
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp
                            ) {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    icon = {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = "Static Mock"
                                        )
                                    },
                                    label = { Text("Static Mock") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = {
                                        Icon(
                                            Icons.Default.Navigation,
                                            contentDescription = "Travel"
                                        )
                                    },
                                    label = { Text("Travel") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                NavigationBarItem(
                                    selected = false,
                                    onClick = { showDisclaimer = true },
                                    icon = {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = "Disclaimer"
                                        )
                                    },
                                    label = { Text("Info") },
                                    colors = NavigationBarItemDefaults.colors(
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    ) { innerPadding ->
                        when (selectedTab) {
                            0 -> MockLocationScreen(
                                viewModel = mockViewModel,
                                onPickOnMap = { pickerMode = PickerMode.MockLocation },
                                modifier = Modifier.padding(innerPadding)
                            )
                            1 -> TravelScreen(
                                viewModel = travelViewModel,
                                onPickStart = { pickerMode = PickerMode.TravelStart },
                                onPickEnd   = { pickerMode = PickerMode.TravelEnd },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}
