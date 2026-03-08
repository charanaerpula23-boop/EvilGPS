package com.charanhyper.tech.greydailer.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@Composable
fun MapPickerScreen(
    initialLat: Double,
    initialLng: Double,
    title: String = "Pick Location",
    onLocationPicked: (lat: Double, lng: Double) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var pickedLat by remember { mutableStateOf(initialLat) }
    var pickedLng by remember { mutableStateOf(initialLng) }

    val tapHandler by rememberUpdatedState { lat: Double, lng: Double ->
        pickedLat = lat
        pickedLng = lng
    }

    val context = LocalContext.current

    val servers = remember {
        arrayOf(
            "https://mt0.google.com/vt/lyrs=y",
            "https://mt1.google.com/vt/lyrs=y",
            "https://mt2.google.com/vt/lyrs=y",
            "https://mt3.google.com/vt/lyrs=y"
        )
    }

    val googleSatTileSource = remember {
        object : XYTileSource(
            "GoogleSat", 0, 20, 256, ".png", servers
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val z = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                val s = servers[((x + y) % servers.size).toInt()]
                return "$s&x=$x&y=$y&z=$z"
            }
        }
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(googleSatTileSource)
            setMultiTouchControls(true)
            controller.setZoom(14.0)
            controller.setCenter(GeoPoint(initialLat, initialLng))
            overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    p?.let {
                        tapHandler(it.latitude, it.longitude)
                        controller.animateTo(it)
                    }
                    return true
                }

                override fun longPressHelper(p: GeoPoint?): Boolean = false
            }))
        }
    }

    DisposableEffect(mapView) {
        onDispose { mapView.onDetach() }
    }

    val markerHolder = remember { arrayOf<Marker?>(null) }
    val redPinDrawable = remember(context) { BitmapDrawable(context.resources, createRedPinBitmap()) }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView },
            update = { mv ->
                val lat = pickedLat
                val lng = pickedLng
                markerHolder[0]?.let { mv.overlays.remove(it) }
                val marker = Marker(mv).apply {
                    position = GeoPoint(lat, lng)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = redPinDrawable
                }
                mv.overlays.add(marker)
                markerHolder[0] = marker
                mv.invalidate()
            }
        )

        // ── Top bar ──────────────────────────────────────────────────────────
        Surface(
            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(),
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                LocationSearchBar(
                    placeholder = "Search location\u2026",
                    onLocationSelected = { lat, lng, _ ->
                        pickedLat = lat
                        pickedLng = lng
                        mapView.controller.animateTo(GeoPoint(lat, lng))
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // ── Bottom confirm panel ─────────────────────────────────────────────
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Tap on the map to move the pin",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "%.6f, %.6f".format(pickedLat, pickedLng),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Button(
                    onClick = { onLocationPicked(pickedLat, pickedLng) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Use This Location")
                }
            }
        }
    }
}

/** Draws a teardrop-shaped red pin (48×64 dp worth of pixels). */
private fun createRedPinBitmap(): Bitmap {
    val w = 96
    val h = 128
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E53935")
        style = Paint.Style.FILL
    }
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B71C1C")
        style = Paint.Style.FILL
    }
    val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFCDD2")
        style = Paint.Style.FILL
        alpha = 180
    }

    val cx = w / 2f
    val radius = w / 2f - 4f

    // Teardrop body: circle + downward triangle
    canvas.drawCircle(cx, radius + 4f, radius, fillPaint)
    val path = Path().apply {
        moveTo(cx - radius * 0.55f, radius + 4f + radius * 0.6f)
        lineTo(cx + radius * 0.55f, radius + 4f + radius * 0.6f)
        lineTo(cx, h.toFloat() - 2f)
        close()
    }
    canvas.drawPath(path, fillPaint)

    // Inner shadow at the tip
    val tipPath = Path().apply {
        moveTo(cx - radius * 0.25f, radius + 4f + radius * 0.8f)
        lineTo(cx + radius * 0.25f, radius + 4f + radius * 0.8f)
        lineTo(cx, h.toFloat() - 2f)
        close()
    }
    canvas.drawPath(tipPath, shadowPaint)

    // White inner circle (centre hole)
    canvas.drawCircle(cx, radius + 4f, radius * 0.38f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    })

    // Highlight glint
    canvas.drawOval(
        RectF(cx - radius * 0.28f, 8f, cx + radius * 0.05f, 8f + radius * 0.42f),
        highlightPaint
    )

    return bmp
}
