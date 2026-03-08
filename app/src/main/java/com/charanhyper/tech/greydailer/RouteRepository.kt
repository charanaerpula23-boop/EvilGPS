package com.charanhyper.tech.greydailer

/** Holds the current road route fetched from OSRM. Shared between ViewModel and Service. */
object RouteRepository {
    @Volatile var route: List<LatLng> = emptyList()
    @Volatile var distanceMeters: Double = 0.0
    @Volatile var durationSeconds: Double = 0.0
}
