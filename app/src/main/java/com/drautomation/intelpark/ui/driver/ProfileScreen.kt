package com.drautomation.intelpark.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.drautomation.intelpark.R
import com.drautomation.intelpark.ui.components.AppCard
import com.drautomation.intelpark.ui.components.ScreenHeader
import com.drautomation.intelpark.ui.components.SecondaryButton
import com.drautomation.intelpark.ui.theme.IP

@Composable
fun ProfileScreen(vm: DriverViewModel, serverUrl: String, onLogout: () -> Unit) {
    LaunchedEffect(Unit) { if (vm.home == null) vm.refreshHome() }
    val driver = vm.home?.driver
    var auto by remember { mutableStateOf(vm.autoBluetooth) }
    val startBle = rememberBleStarter(vm) { vm.home?.currentSession?.bayId }
    Column(
        Modifier.fillMaxSize().background(IP.Ground).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(stringResource(R.string.profile))
        driver?.let { d ->
            AppCard {
                Text("${d.firstName} ${d.lastName}", style = MaterialTheme.typography.titleLarge)
                Text(d.email.orEmpty(), color = IP.Muted)
                Text(
                    (d.phone ?: "—") + "  ·  " + stringResource(if (d.phoneVerified) R.string.phone_verified else R.string.phone_not_verified),
                    color = IP.Muted,
                )
                Text(listOfNotNull(d.addressLine, listOfNotNull(d.postcode, d.city).joinToString(" ")).joinToString(", "), color = IP.Muted)
                Text(stringResource(R.string.customer_ref, d.reference), style = MaterialTheme.typography.labelMedium, color = IP.Blue)
            }
            AppCard {
                Text(stringResource(R.string.my_vehicles), style = MaterialTheme.typography.titleMedium)
                d.vehicles.forEach { v -> Text(v.plateDisplay, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleMedium) }
            }
        }
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.ble_auto), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.ble_auto_text), style = MaterialTheme.typography.bodyMedium, color = IP.Muted)
                }
                Switch(auto, { auto = it; vm.setAutoBluetooth(it) }, colors = SwitchDefaults.colors(checkedTrackColor = IP.Ink))
            }
        }
        BleCard(vm.ble, onIdentify = startBle)
        AppCard {
            Text(stringResource(R.string.server_title), style = MaterialTheme.typography.titleMedium)
            Text(serverUrl, fontFamily = FontFamily.Monospace, color = IP.Muted)
        }
        SecondaryButton(stringResource(R.string.logout), { vm.logout(onLogout) }, Modifier.fillMaxWidth().padding(bottom = 8.dp))
    }
}
