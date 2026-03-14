package com.charanhyper.tech.greydailer.ui

import android.content.Context
import android.location.Geocoder
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class SearchResult(
    val displayName: String,
    val subtitle: String,
    val lat: Double,
    val lng: Double
)

@Composable
fun LocationSearchBar(
    placeholder: String = "Search location\u2026",
    onLocationSelected: (lat: Double, lng: Double, name: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = { newQuery ->
                query = newQuery
                searchJob?.cancel()
                if (newQuery.length >= 2) {
                    searchJob = scope.launch {
                        delay(300)
                        isSearching = true
                        results = searchLocations(context, newQuery)
                        isSearching = false
                    }
                } else {
                    results = emptyList()
                }
            },
            placeholder = { Text(placeholder) },
            leadingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        results = emptyList()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        AnimatedVisibility(visible = results.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column {
                    results.forEachIndexed { index, result ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onLocationSelected(result.lat, result.lng, result.displayName)
                                    query = result.displayName
                                    results = emptyList()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = result.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (result.subtitle.isNotEmpty()) {
                                    Text(
                                        text = result.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (index < results.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Search using Android's built-in Geocoder (Google Play Services backend).
 * Falls back to ArcGIS World Geocoding Service if Geocoder is unavailable or returns nothing.
 */
private suspend fun searchLocations(context: Context, query: String): List<SearchResult> =
    withContext(Dispatchers.IO) {
        val geocoderResults = searchWithGeocoder(context, query)
        if (geocoderResults.isNotEmpty()) return@withContext geocoderResults
        searchArcGIS(query)
    }

/** Android built-in Geocoder — uses Google Maps data, no API key needed */
@Suppress("DEPRECATION")
private fun searchWithGeocoder(context: Context, query: String): List<SearchResult> {
    if (!Geocoder.isPresent()) return emptyList()
    return try {
        val geocoder = Geocoder(context, Locale.ENGLISH)
        val addresses = geocoder.getFromLocationName(query, 10) ?: return emptyList()
        addresses.mapNotNull { addr ->
            if (!addr.hasLatitude() || !addr.hasLongitude()) return@mapNotNull null

            val name = addr.featureName
            val thoroughfare = addr.thoroughfare
            val subLocality = addr.subLocality
            val locality = addr.locality
            val subAdmin = addr.subAdminArea
            val admin = addr.adminArea
            val country = addr.countryName

            val displayName = when {
                !name.isNullOrBlank() && name != addr.latitude.toString() -> name
                !thoroughfare.isNullOrBlank() -> thoroughfare
                !subLocality.isNullOrBlank() -> subLocality
                !locality.isNullOrBlank() -> locality
                !subAdmin.isNullOrBlank() -> subAdmin
                !admin.isNullOrBlank() -> admin
                else -> {
                    val line = addr.getAddressLine(0)
                    line?.split(",")?.firstOrNull()?.trim() ?: "Unknown"
                }
            }

            val subtitleParts = listOfNotNull(
                subLocality?.takeIf { it != displayName },
                locality?.takeIf { it != displayName },
                admin?.takeIf { it != displayName },
                country?.takeIf { it != displayName }
            )

            SearchResult(
                displayName = displayName,
                subtitle = subtitleParts.joinToString(", "),
                lat = addr.latitude,
                lng = addr.longitude
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

/** ArcGIS World Geocoding Service — superior global coverage, free tier, no API key */
private fun searchArcGIS(query: String): List<SearchResult> {
    return try {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer/suggest" +
            "?text=$encoded&f=json&maxSuggestions=10"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "EvilGPS/1.0")
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        val root = JSONObject(conn.inputStream.bufferedReader().readText())
        conn.disconnect()

        val suggestions = root.optJSONArray("suggestions") ?: return emptyList()
        val results = mutableListOf<SearchResult>()

        for (i in 0 until suggestions.length()) {
            val suggestion = suggestions.getJSONObject(i)
            val magicKey = suggestion.optString("magicKey", "")
            val text = suggestion.optString("text", "")
            if (magicKey.isEmpty()) continue

            // Resolve each suggestion to coordinates via findAddressCandidates
            val candidate = resolveArcGISCandidate(text, magicKey) ?: continue
            results.add(candidate)
        }
        results
    } catch (_: Exception) {
        emptyList()
    }
}

/** Resolve an ArcGIS suggestion to lat/lng using findAddressCandidates */
private fun resolveArcGISCandidate(text: String, magicKey: String): SearchResult? {
    return try {
        val encodedText = URLEncoder.encode(text, "UTF-8")
        val encodedKey = URLEncoder.encode(magicKey, "UTF-8")
        val url = "https://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer/findAddressCandidates" +
            "?SingleLine=$encodedText&magicKey=$encodedKey&f=json&maxLocations=1&outFields=City,Region,CntryName"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "EvilGPS/1.0")
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        val root = JSONObject(conn.inputStream.bufferedReader().readText())
        conn.disconnect()

        val candidates = root.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) return null

        val candidate = candidates.getJSONObject(0)
        val location = candidate.getJSONObject("location")
        val attrs = candidate.optJSONObject("attributes")

        val fullAddress = candidate.optString("address", "")
        val parts = fullAddress.split(",").map { it.trim() }
        val displayName = parts.firstOrNull() ?: "Unknown"
        val subtitle = if (attrs != null) {
            listOfNotNull(
                attrs.optString("City").takeIf { it.isNotEmpty() },
                attrs.optString("Region").takeIf { it.isNotEmpty() },
                attrs.optString("CntryName").takeIf { it.isNotEmpty() }
            ).filter { it != displayName }.joinToString(", ")
        } else {
            parts.drop(1).joinToString(", ")
        }

        SearchResult(
            displayName = displayName,
            subtitle = subtitle,
            lat = location.getDouble("y"),
            lng = location.getDouble("x")
        )
    } catch (_: Exception) {
        null
    }
}