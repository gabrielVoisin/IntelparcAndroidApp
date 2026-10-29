package com.drautomation.intelpark.ui.driver

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.drautomation.intelpark.R
import com.drautomation.intelpark.ui.common.errorMessage
import com.drautomation.intelpark.ui.components.AppCard
import com.drautomation.intelpark.ui.components.PrimaryButton
import com.drautomation.intelpark.ui.theme.IP

/**
 * Returns a function that makes Bluetooth ready (permissions, then "turn Bluetooth on?" system dialog)
 * and then starts the identification.
 */
@Composable
fun rememberBleStarter(vm: DriverViewModel, bayId: () -> String?): () -> Unit {
    val currentBay = rememberUpdatedState(bayId)
    val enable = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (vm.bleEnabled()) vm.identify(currentBay.value())
    }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.all { it }) {
            if (vm.bleEnabled()) vm.identify(currentBay.value()) else enable.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } else {
            vm.identify(currentBay.value())  // reports "permission denied" in the card
        }
    }
    return {
        when {
            !vm.bleSupported() -> vm.identify(currentBay.value())
            !vm.bleHasPermissions() -> permissions.launch(vm.bleRequiredPermissions())
            !vm.bleEnabled() -> enable.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            else -> vm.identify(currentBay.value())
        }
    }
}

/** "Identification at the bay" card: what the cabinet answered. */
@Composable
fun BleCard(state: BleState, onIdentify: () -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(IP.BlueSoft), contentAlignment = Alignment.Center) {
                when (state) {
                    BleState.Running -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = IP.Blue)
                    is BleState.Done -> Icon(
                        if (state.result.status == "authorised" || state.result.status == "pending") Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                        contentDescription = null, tint = if (state.result.status == "authorised") IP.Blue else IP.Amber,
                    )
                    is BleState.Failed -> Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = IP.Amber)
                    BleState.Idle -> Icon(Icons.Filled.Bluetooth, contentDescription = null, tint = IP.Blue)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.ble_title), style = MaterialTheme.typography.titleMedium)
                Text(bleText(state), style = MaterialTheme.typography.bodyMedium,
                    color = if (state is BleState.Failed) IP.Amber else IP.Muted)
            }
        }
        if (state != BleState.Running) {
            PrimaryButton(
                stringResource(if (state is BleState.Idle) R.string.ble_identify else R.string.ble_retry), onIdentify,
            )
        }
    }
}

@Composable
private fun bleText(state: BleState): String = when (state) {
    BleState.Idle -> stringResource(R.string.ble_idle)
    BleState.Running -> stringResource(R.string.ble_running)
    is BleState.Failed -> stringResource(if (state.code == "permission_denied") R.string.ble_err_permission else errorMessage(state.code))
    is BleState.Done -> {
        val bay = state.result.bay ?: state.controller
        when (state.result.status) {
            "authorised" -> stringResource(R.string.ble_ok, bay)
            "pending" -> stringResource(R.string.ble_pending, state.controller)
            "payment_required" -> stringResource(R.string.ble_payment_required, bay)
            "reserved_by_other" -> stringResource(R.string.ble_reserved_by_other, bay)
            "wrong_bay" -> stringResource(R.string.ble_wrong_bay, bay)
            "expired_token", "invalid_token" -> stringResource(R.string.ble_err_token)
            else -> stringResource(R.string.ble_err_refused, state.result.status)
        }
    }
}
