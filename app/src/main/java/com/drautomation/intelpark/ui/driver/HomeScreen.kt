package com.drautomation.intelpark.ui.driver

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drautomation.intelpark.R
import com.drautomation.intelpark.data.AppSessionOut
import com.drautomation.intelpark.data.Format
import com.drautomation.intelpark.ui.common.errorMessage
import com.drautomation.intelpark.ui.components.AppCard
import com.drautomation.intelpark.ui.components.ErrorText
import com.drautomation.intelpark.ui.components.Logo
import com.drautomation.intelpark.ui.components.Pill
import com.drautomation.intelpark.ui.components.SecondaryButton
import com.drautomation.intelpark.ui.theme.IP

@Composable
fun HomeScreen(vm: DriverViewModel, onScan: () -> Unit, onBook: () -> Unit, onMap: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        vm.refreshHome()
        vm.loadMap(context)
    }
    val home = vm.home
    val session = home?.currentSession
    val startBle = rememberBleStarter(vm) { vm.home?.currentSession?.bayId ?: vm.home?.nextReservation?.bayId }
    Column(
        Modifier.fillMaxSize().background(IP.Ground).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Logo(size = 34, subtitle = true)
            Spacer(Modifier.weight(1f))
            Pill(stringResource(R.string.active_account), IP.BlueSoft, IP.BlueDark)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.hello, home?.driver?.firstName.orEmpty()), color = IP.Muted, style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.where_park), style = MaterialTheme.typography.headlineMedium)
            }
            home?.driver?.vehicles?.firstOrNull()?.let {
                Text(it.plateDisplay, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White).padding(horizontal = 12.dp, vertical = 10.dp))
            }
        }
        ErrorText(vm.error?.let { stringResource(errorMessage(it)) })

        // Big yellow action: scan the QR code of the bay
        Surface(onClick = onScan, shape = RoundedCornerShape(24.dp), color = IP.Yellow, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(IP.Ink), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = IP.Yellow, modifier = Modifier.size(30.dp))
                }
                Text(stringResource(R.string.scan_a_bay), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.scan_a_bay_text), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Shortcut(Icons.Filled.CalendarMonth, stringResource(R.string.book), stringResource(R.string.book_sub), onBook, Modifier.weight(1f))
            Shortcut(Icons.Filled.Map, stringResource(R.string.map), stringResource(R.string.map_sub), onMap, Modifier.weight(1f))
        }

        session?.let { SessionCard(it, onExtend = { vm.extend(it.id, 30) }, busy = vm.busy) }
        BleCard(vm.ble, onIdentify = startBle)

        vm.nearest?.let { nearest ->
            Surface(shape = RoundedCornerShape(20.dp), color = IP.Ink, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.closest_to_you).uppercase(), color = IP.OnDarkMuted, style = MaterialTheme.typography.labelSmall)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text(nearest.bay?.let { "${it.siteName} · ${it.zoneCode} ${it.number}" } ?: nearest.site.name,
                                color = Color.White, style = MaterialTheme.typography.titleLarge)
                            Text(stringResource(R.string.free_bays_price, nearest.site.freeBays, Format.money(nearest.site.hourly)),
                                color = Color(0xFFC9CDD3), style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(Format.distance(nearest.site.distanceM), color = IP.Yellow, fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    }
                    Surface(onClick = onMap, shape = RoundedCornerShape(14.dp), color = Color.White, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.see_on_map), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(14.dp))
                        }
                    }
                }
            }
        }

        home?.nextReservation?.let { r ->
            AppCard {
                Text(stringResource(R.string.next_booking).uppercase(), style = MaterialTheme.typography.labelSmall, color = IP.Muted)
                Text(r.bayLabel, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.booked_from_to, Format.dateTime(r.startsAt), Format.time(r.endsAt)),
                    style = MaterialTheme.typography.bodyMedium, color = IP.Muted)
            }
        }
        home?.subscriptions?.forEach { s ->
            AppCard {
                Text(stringResource(R.string.subscription).uppercase(), style = MaterialTheme.typography.labelSmall, color = IP.Muted)
                Text("${s.planName} · ${s.siteName}", style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.valid_until, Format.date(s.endsOn)), style = MaterialTheme.typography.bodyMedium, color = IP.Muted)
            }
        }
    }
}

@Composable
private fun Shortcut(icon: ImageVector, title: String, sub: String, onClick: () -> Unit, modifier: Modifier) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = Color.White, border = BorderStroke(1.dp, IP.Line), modifier = modifier) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(IP.BlueSoft), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = IP.Blue)
            }
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(sub, style = MaterialTheme.typography.bodyMedium, color = IP.Muted)
        }
    }
}

@Composable
fun SessionCard(session: AppSessionOut, onExtend: () -> Unit, busy: Boolean) {
    val left = Format.minutesLeft(session.paidUntil)
    AppCard(color = IP.Ink, border = false) {
        Text(stringResource(R.string.parked_now).uppercase(), color = IP.OnDarkMuted, style = MaterialTheme.typography.labelSmall)
        Text(session.bayLabel ?: session.siteName, color = Color.White, style = MaterialTheme.typography.titleLarge)
        Text(
            when {
                session.right == "subscription" -> stringResource(R.string.covered_by_subscription)
                left != null && left > 0 -> stringResource(R.string.time_left_until, Format.duration(left), Format.time(session.paidUntil))
                else -> stringResource(R.string.time_over)
            },
            color = IP.Yellow, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 18.sp,
        )
        session.identifiedBy?.let { Text(identifiedLabel(it), color = Color(0xFFC9CDD3), style = MaterialTheme.typography.bodyMedium) }
        if (session.right != "subscription") {
            SecondaryButton(stringResource(R.string.extend_30), onExtend, Modifier.fillMaxWidth(), enabled = !busy)
        }
    }
}

@Composable
fun identifiedLabel(method: String): String = stringResource(
    when (method) {
        "bluetooth" -> R.string.identified_bluetooth
        "camera" -> R.string.identified_camera
        "app" -> R.string.identified_app
        else -> R.string.identified_manual
    },
)
