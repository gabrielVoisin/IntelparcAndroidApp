package com.drautomation.intelpark.ui.common

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

data class MapPin(val id: String, val lat: Double, val lng: Double, val label: String, val title: String,
                  val color: Int, val textColor: Int, val selected: Boolean = false)

/**
 * OpenStreetMap (osmdroid): no API key needed. Pins are drawn as rounded labels
 * (free bays on the driver map, open alarms on the agent map).
 */
@Composable
fun OsmMap(pins: List<MapPin>, me: Pair<Double, Double>?, modifier: Modifier, onPinClick: (String) -> Unit) {
    val context = LocalContext.current
    val click = rememberUpdatedState(onPinClick)
    val map = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
        }
    }
    DisposableEffect(Unit) {
        map.onResume()
        onDispose {
            map.onPause()
            map.onDetach()
        }
    }
    AndroidView(factory = { map }, modifier = modifier, update = { view ->
        view.overlays.clear()
        val density = context.resources.displayMetrics.density
        pins.forEach { pin ->
            view.overlays.add(Marker(view).apply {
                position = GeoPoint(pin.lat, pin.lng)
                title = pin.title
                icon = BitmapDrawable(context.resources, labelBitmap(pin.label, pin.color, pin.textColor, pin.selected, density))
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _, _ -> click.value(pin.id); true }
            })
        }
        me?.let { (lat, lng) ->
            view.overlays.add(Marker(view).apply {
                position = GeoPoint(lat, lng)
                icon = BitmapDrawable(context.resources, dotBitmap(density))
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setInfoWindow(null)
            })
        }
        val focus = pins.firstOrNull { it.selected } ?: pins.firstOrNull()
        val center = me?.let { GeoPoint(it.first, it.second) } ?: focus?.let { GeoPoint(it.lat, it.lng) }
        if (center != null && view.tag == null) {
            view.controller.setCenter(center)
            view.tag = "centered"
        }
        view.invalidate()
    })
}

private fun labelBitmap(text: String, color: Int, textColor: Int, selected: Boolean, density: Float): Bitmap {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = textColor
        textSize = (if (selected) 17 else 15) * density
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    val padH = 10 * density
    val padV = 7 * density
    val tail = 7 * density
    val w = paint.measureText(text) + 2 * padH
    val h = paint.textSize + 2 * padV
    val stroke = (if (selected) 3 else 2) * density
    val bitmap = Bitmap.createBitmap((w + 2 * stroke).toInt(), (h + tail + 2 * stroke).toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val rect = RectF(stroke, stroke, stroke + w, stroke + h)
    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = if (selected) 0xFF12161B.toInt() else 0xFFFFFFFF.toInt() }
    canvas.drawRoundRect(RectF(0f, 0f, w + 2 * stroke, h + 2 * stroke), 12 * density, 12 * density, border)
    canvas.drawRoundRect(rect, 10 * density, 10 * density, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    val path = android.graphics.Path().apply {
        moveTo(rect.centerX() - tail, rect.bottom)
        lineTo(rect.centerX(), rect.bottom + tail)
        lineTo(rect.centerX() + tail, rect.bottom)
        close()
    }
    canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    canvas.drawText(text, rect.left + padH, rect.bottom - padV - paint.descent() / 2, paint)
    return bitmap
}

private fun dotBitmap(density: Float): Bitmap {
    val size = (26 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val c = size / 2f
    canvas.drawCircle(c, c, c, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x332547D0 })
    canvas.drawCircle(c, c, 7 * density, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
    canvas.drawCircle(c, c, 5 * density, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2547D0.toInt() })
    return bitmap
}
