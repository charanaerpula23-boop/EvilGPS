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
import org.json.JSONArray
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
 * Falls back to Nominatim if Geocoder is unavailable or returns nothing.
 */
private suspend fun searchLocations(context: Context, query: String): List<SearchResult> =
    withContext(Dispatchers.IO) {
        val geocoderResults = searchWithGeocoder(context, query)
        if (geocoderResults.isNotEmpty()) return@withContext geocoderResults
        searchNominatim(query)
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

            val nameParts = mutableListOf<String>()
            val name = addr.featureName
            val thoroughfare = addr.thoroughfare
            val subLocality = addr.subLocality
            val locality = addr.locality
            val subAdmin = addr.subAdminArea
            val admin = addr.adminArea
            val country = addr.countryName

            // Build display name: most specific available
            val displayName = when {
                !name.isNullOrBlank() && name != addr.latitude.toString() -> name
                !thoroughfare.isNullOrBlank() -> thoroughfare
                !subLocality.isNullOrBlank() -> subLocality
                !locality.isNullOrBlank() -> locality
                !subAdmin.isNullOrBlank() -> subAdmin
                !admin.isNullOrBlank() -> admin
                else -> {
                    // Use address lines as fallback
                    val line = addr.getAddressLine(0)
                    line?.split(",")?.firstOrNull()?.trim() ?: "Unknown"
                }
            }

            // Build subtitle from remaining parts
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

/** Nominatim as fallback when Geocoder is unavailable */
private fun searchNominatim(query: String): List<SearchResult> {
    return try {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://nominatim.openstreetmap.org/search" +
            "?q=$encoded&format=json&limit=10&addressdetails=1&dedupe=1&accept-language=en"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "EvilGPS/1.0 (Android)")
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        val json = JSONArray(conn.inputStream.bufferedReader().readText())
        conn.disconnect()
        (0 until json.length()).map { i ->
            val obj = json.getJSONObject(i)
            val addr = obj.optJSONObject("address")
            val name = addr?.let { a ->
                a.optString("name").takeIf { it.isNotEmpty() }
                    ?: a.optString("road").takeIf { it.isNotEmpty() }
                    ?: a.optString("suburb").takeIf { it.isNotEmpty() }
                    ?: a.optString("city").takeIf { it.isNotEmpty() }
                    ?: a.optString("town").takeIf { it.isNotEmpty() }
                    ?: a.optString("village").takeIf { it.isNotEmpty() }
            } ?: obj.getString("display_name").split(",").firstOrNull()?.trim() ?: ""
            val subtitle = addr?.let { a ->
                listOfNotNull(
                    a.optString("city").takeIf { it.isNotEmpty() }
                        ?: a.optString("town").takeIf { it.isNotEmpty() },
                    a.optString("state").takeIf { it.isNotEmpty() },
                    a.optString("country").takeIf { it.isNotEmpty() }
                ).joinToString(", ")
            } ?: ""
            SearchResult(
                displayName = name,
                subtitle = subtitle,
                lat = obj.getString("lat").toDouble(),
                lng = obj.getString("lon").toDouble()
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}