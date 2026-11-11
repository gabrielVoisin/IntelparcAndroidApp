package com.drautomation.intelpark.ui.common

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.core.content.ContextCompat
import com.drautomation.intelpark.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

fun Context.hasPermissions(vararg permissions: String): Boolean =
    permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

/** Asks for runtime permissions; [onResult] receives true when all were granted. */
@Composable
fun rememberPermissionRequest(onResult: (Boolean) -> Unit): (Array<String>) -> Unit {
    val callback = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        callback.value(granted.values.all { it })
    }
    return { permissions -> launcher.launch(permissions) }
}

/** Last known position of the phone, or null (no permission, location off). */
@SuppressLint("MissingPermission")
suspend fun currentLocation(context: Context): Pair<Double, Double>? {
    if (!context.hasPermissions(android.Manifest.permission.ACCESS_COARSE_LOCATION)) return null
    return runCatching {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val location = client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
            ?: client.lastLocation.await()
        location?.let { it.latitude to it.longitude }
    }.getOrNull()
}

/** GPS guidance: Google Maps turn-by-turn when installed, else any map application. */
fun startNavigation(context: Context, lat: Double, lng: Double, label: String, walking: Boolean = true) {
    val google = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lng&mode=${if (walking) "w" else "d"}"))
        .setPackage("com.google.android.apps.maps")
    val generic = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})"))
    try {
        context.startActivity(google)
    } catch (e: ActivityNotFoundException) {
        try {
            context.startActivity(generic)
        } catch (_: ActivityNotFoundException) {
            // no map application: nothing to open
        }
    }
}

/** Server and Bluetooth error codes -> message shown to the user. */
@StringRes
fun errorMessage(code: String?): Int = when (code) {
    null -> R.string.err_generic
    "network" -> R.string.err_network
    "server_url_invalid" -> R.string.err_server_url
    "invalid_credentials" -> R.string.err_invalid_credentials
    "turbo_disabled", "no_admin" -> R.string.err_turbo_disabled
    "email_already_used" -> R.string.err_email_used
    "plate_already_registered" -> R.string.err_plate_used
    "phone_code_invalid" -> R.string.err_code_invalid
    "phone_code_expired" -> R.string.err_code_expired
    "too_many_codes" -> R.string.err_too_many_codes
    "bay_not_found", "bay_code_invalid" -> R.string.err_bay_not_found
    "bay_blocked" -> R.string.err_bay_blocked
    "bay_rented" -> R.string.err_bay_rented
    "bay_taken_by_other" -> R.string.err_bay_taken
    "bay_already_booked", "no_free_bay" -> R.string.err_no_free_bay
    "max_stay_exceeded" -> R.string.err_max_stay
    "payment_provider_not_configured" -> R.string.err_payment_provider
    "permission_denied", "insufficient_role" -> R.string.err_permission
    "alarm_not_open" -> R.string.err_alarm_handled
    "refresh_token_invalid", "token_expired", "user_inactive", "not_authenticated" -> R.string.err_session_expired
    "validation" -> R.string.err_validation
    // Bluetooth
    "bluetooth_off" -> R.string.ble_err_off
    "ble_unsupported" -> R.string.ble_err_unsupported
    "no_cabinet" -> R.string.ble_err_no_cabinet
    "no_answer", "disconnected", "connect_failed", "gatt_busy", "scan_failed" -> R.string.ble_err_link
    "not_intelpark" -> R.string.ble_err_not_intelpark
    "ble_disabled" -> R.string.ble_err_disabled
    else -> if (code.startsWith("gatt_error")) R.string.ble_err_link else R.string.err_generic
}
