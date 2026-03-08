package com.charanhyper.tech.greydailer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.charanhyper.tech.greydailer.R
import com.charanhyper.tech.greydailer.TravelState
import com.charanhyper.tech.greydailer.TravelViewModel

@Composable
fun TravelScreen(
    viewModel: TravelViewModel,
    onPickStart: () -> Unit,
    onPickEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inputEnabled = viewModel.travelState != TravelState.FETCHING &&
        viewModel.travelState != TravelState.TRAVELLING

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        if (viewModel.errorMessage.isNotEmpty()) {
            ErrorCard(message = viewModel.errorMessage, onDismiss = viewModel::clearError)
        }

        // â”€â”€ Start location â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        Text("Start Location", style = MaterialTheme.typography.titleSmall)
        LocationSearchBar(
            placeholder = "Search start location\u2026",
            onLocationSelected = { lat, lng, _ -> viewModel.onStartPicked(lat, lng) }
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { if (inputEnabled) onPickStart() },
            enabled = inputEnabled,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Start",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${viewModel.startLat}, ${viewModel.startLng}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Pick Start on Map",
                    tint = if (inputEnabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }

        // â”€â”€ End location â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        Text("End Location", style = MaterialTheme.typography.titleSmall)
        LocationSearchBar(
            placeholder = "Search end location\u2026",
            onLocationSelected = { lat, lng, _ -> viewModel.onEndPicked(lat, lng) }
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { if (inputEnabled) onPickEnd() },
            enabled = inputEnabled,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "End",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${viewModel.endLat}, ${viewModel.endLng}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Pick End on Map",
                    tint = if (inputEnabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }

        // â”€â”€ Speed â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        OutlinedTextField(
            value = viewModel.speedKmh,
            onValueChange = viewModel::onSpeedChange,
            label = { Text("Speed (km/h)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            enabled = viewModel.travelState != TravelState.TRAVELLING
        )

        // â”€â”€ Fetch route â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        Button(
            onClick = viewModel::fetchRoute,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = inputEnabled
        ) {
            if (viewModel.travelState == TravelState.FETCHING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text("Fetching Route\u2026")
            } else {
                Text("Fetch Route via Road")
            }
        }

        // â”€â”€ Route info â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (viewModel.routeInfo.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = viewModel.routeInfo,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // â”€â”€ Travel progress â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (viewModel.travelState == TravelState.TRAVELLING) {
            LinearProgressIndicator(
                progress = { viewModel.progress },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Travelling\u2026 %.0f%%".format(viewModel.progress * 100),
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (viewModel.travelState == TravelState.ARRIVED) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = "Arrived at destination!",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // â”€â”€ Start / Stop travel â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        val canTravel = viewModel.travelState == TravelState.ROUTE_READY ||
            viewModel.travelState == TravelState.ARRIVED ||
            viewModel.travelState == TravelState.TRAVELLING
        Button(
            onClick = if (viewModel.travelState == TravelState.TRAVELLING)
                viewModel::stopTravel
            else
                viewModel::startTravel,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = canTravel,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (viewModel.travelState == TravelState.TRAVELLING)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.secondary
            )
        ) {
            Text(
                text = if (viewModel.travelState == TravelState.TRAVELLING)
                    "Stop Travel"
                else
                    "Start Travel",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "How to Travel",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "1. Set start & end locations (search, type, or pick on map)\n" +
                        "2. Tap \"Fetch Route via Road\" to get the road path\n" +
                        "3. Adjust speed (km/h) as needed\n" +
                        "4. Tap \"Start Travel\" \u2014 location moves along the route\n" +
                        "5. Works even if you swipe the app away\n" +
                        "\nRequires: Developer Options \u2192 Mock location app \u2192 EvilGPS",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }

        Text(
            text = stringResource(R.string.developed_by),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
