package com.charanhyper.tech.greydailer.ui

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.net.URL

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
                        results = searchNominatim(newQuery)
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

private suspend fun searchNominatim(query: String): List<SearchResult> =
    withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search" +
                "?q=$encoded" +
                "&format=json" +
                "&limit=8" +
                "&addressdetails=1" +
                "&dedupe=1" +
                "&extratags=0"
            val conn = URL(url).openConnection().apply {
                setRequestProperty("User-Agent", "EvilGPS/1.0")
                setRequestProperty("Accept-Language", "en")
                connectTimeout = 8000
                readTimeout = 8000
            }
            val json = JSONArray(conn.getInputStream().bufferedReader().readText())
            (0 until json.length()).map { i ->
                val obj = json.getJSONObject(i)
                val addr = obj.optJSONObject("address")
                SearchResult(
                    displayName = buildDisplayName(obj, addr),
                    subtitle = buildSubtitle(addr),
                    lat = obj.getDouble("lat"),
                    lng = obj.getDouble("lon")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

/** Primary label: name > road > suburb > city > county */
private fun buildDisplayName(obj: JSONObject, addr: JSONObject?): String {
    if (addr == null) {
        // Fall back to first part of display_name
        return obj.getString("display_name").split(",").firstOrNull()?.trim() ?: ""
    }
    return (addr.optString("name").takeIf { it.isNotEmpty() }
        ?: addr.optString("amenity").takeIf { it.isNotEmpty() }
        ?: addr.optString("tourism").takeIf { it.isNotEmpty() }
        ?: addr.optString("road").takeIf { it.isNotEmpty() }
        ?: addr.optString("suburb").takeIf { it.isNotEmpty() }
        ?: addr.optString("neighbourhood").takeIf { it.isNotEmpty() }
        ?: addr.optString("city").takeIf { it.isNotEmpty() }
        ?: addr.optString("town").takeIf { it.isNotEmpty() }
        ?: addr.optString("village").takeIf { it.isNotEmpty() }
        ?: obj.getString("display_name").split(",").firstOrNull()?.trim()
        ?: "")
}

/** Secondary label: city/state/country */
private fun buildSubtitle(addr: JSONObject?): String {
    if (addr == null) return ""
    val parts = listOfNotNull(
        addr.optString("city").takeIf { it.isNotEmpty() }
            ?: addr.optString("town").takeIf { it.isNotEmpty() }
            ?: addr.optString("village").takeIf { it.isNotEmpty() },
        addr.optString("state").takeIf { it.isNotEmpty() },
        addr.optString("country").takeIf { it.isNotEmpty() }
    )
    return parts.joinToString(", ")
}
