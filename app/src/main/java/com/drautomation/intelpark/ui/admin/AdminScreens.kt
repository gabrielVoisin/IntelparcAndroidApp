package com.drautomation.intelpark.ui.admin

import android.Manifest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drautomation.intelpark.R
import com.drautomation.intelpark.data.Format
import com.drautomation.intelpark.data.PaymentAlarmOut
import com.drautomation.intelpark.ui.common.CameraPreview
import com.drautomation.intelpark.ui.common.MapPin
import com.drautomation.intelpark.ui.common.OsmMap
import com.drautomation.intelpark.ui.common.errorMessage
import com.drautomation.intelpark.ui.common.hasPermissions
import com.drautomation.intelpark.ui.common.rememberPermissionRequest
import com.drautomation.intelpark.ui.common.rememberPhotoController
import com.drautomation.intelpark.ui.common.startNavigation
import com.drautomation.intelpark.ui.common.takePhoto
import com.drautomation.intelpark.ui.components.AppCard
import com.drautomation.intelpark.ui.components.BackButton
import com.drautomation.intelpark.ui.components.ErrorText
import com.drautomation.intelpark.ui.components.Field
import com.drautomation.intelpark.ui.components.InfoBanner
import com.drautomation.intelpark.ui.components.Logo
import com.drautomation.intelpark.ui.components.Pill
import com.drautomation.intelpark.ui.components.PrimaryButton
import com.drautomation.intelpark.ui.components.SecondaryButton
import com.drautomation.intelpark.ui.components.Segmented
import com.drautomation.intelpark.ui.components.Stat
import com.drautomation.intelpark.ui.driver.identifiedLabel
import com.drautomation.intelpark.ui.theme.IP
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun reasonLabel(reason: String): String = stringResource(
    when (reason) {
        "no_payment" -> R.string.reason_no_payment
        "overstay" -> R.string.reason_overstay
        "plate_not_recognised" -> R.string.reason_plate_not_recognised
        "lost_ticket" -> R.string.reason_lost_ticket
        "accessible_bay_misuse" -> R.string.reason_accessible_bay_misuse
        "reserved_bay_misuse" -> R.string.reason_reserved_bay_misuse
        else -> R.string.reason_other
    },
)

/** Refreshes the alarms every 15 s while the screen is shown. */
@Composable
private fun LiveRefresh(vm: AdminViewModel) {
    LaunchedEffect(Unit) {
        while (true) {
            vm.refresh()
            delay(15_000)
        }
    }
}

@Composable
fun AdminLiveScreen(vm: AdminViewModel, onAlarm: (String) -> Unit, onMap: () -> Unit, onLogout: () -> Unit) {
    LiveRefresh(vm)
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val violations = vm.alarms.count { it.isViolation() }
    val toCheck = vm.alarms.size - violations
    val shown = when (filter) {
        1 -> vm.alarms.filter { it.isViolation() }
        2 -> vm.alarms.filterNot { it.isViolation() }
        else -> vm.alarms
    }
    Column(Modifier.fillMaxSize().background(IP.Ground).verticalScroll(rememberScrollState())) {
        Column(Modifier.background(IP.Ink).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Logo(size = 32, dark = true)
                Text("  ADMIN", color = IP.Yellow, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { vm.logout(onLogout) }) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringResource(R.string.logout), tint = IP.OnDarkMuted)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(IP.Yellow))
                Text(stringResource(R.string.live_updated, if (vm.updatedAt == 0L) "—" else Format.clock(vm.updatedAt)),
                    color = Color(0xFFD3D7DD), style = MaterialTheme.typography.labelMedium)
            }
            Text(stringResource(R.string.live_title), color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat(violations.toString(), stringResource(R.string.likely_violations), IP.Yellow, IP.Ink)
                Stat(toCheck.toString(), stringResource(R.string.to_check), IP.Blue, Color.White)
                val compliant = vm.dashboard?.let { (it.totalBays - it.freeBays - vm.alarms.size).coerceAtLeast(0) }
                Stat(compliant?.toString() ?: "—", stringResource(R.string.compliant), IP.Dark2, Color.White)
            }
            Surface(onClick = onMap, shape = RoundedCornerShape(14.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Map, contentDescription = null, tint = IP.Ink)
                    Text("  " + stringResource(R.string.see_alarms_on_map), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Segmented(listOf(stringResource(R.string.all_n, vm.alarms.size), stringResource(R.string.violations), stringResource(R.string.to_check)),
                filter, { filter = it })
            ErrorText(vm.error?.let { stringResource(errorMessage(it)) })
            if (shown.isEmpty()) {
                Text(stringResource(R.string.nothing_to_report), color = IP.Muted, modifier = Modifier.padding(24.dp))
            }
            shown.forEach { a -> AlarmRow(a) { onAlarm(a.id) } }
        }
    }
}

@Composable
private fun AlarmRow(a: PaymentAlarmOut, onClick: () -> Unit) {
    val (bg, fg) = if (a.isViolation()) IP.AmberSoft to IP.Amber else IP.BlueSoft to IP.BlueDark
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, IP.Line),
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.size(58.dp).clip(RoundedCornerShape(14.dp)).background(bg), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Text(a.bayLabel?.substringAfterLast(' ') ?: "—", color = fg, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text(Format.ago(a.detectedAt), color = fg, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(if (a.isViolation()) R.string.likely_violation else R.string.to_check).uppercase(), color = fg,
                    style = MaterialTheme.typography.labelSmall, fontSize = 11.sp)
                Text(reasonLabel(a.reason), style = MaterialTheme.typography.titleMedium)
                Text(listOfNotNull(a.bayLabel, a.plate).joinToString(" · "), color = IP.Muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun AdminMapScreen(vm: AdminViewModel, onAlarm: (String) -> Unit) {
    val context = LocalContext.current
    LiveRefresh(vm)
    val request = rememberPermissionRequest { vm.locate(context) }
    LaunchedEffect(Unit) { request(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
    var selectedSite by rememberSaveable { mutableStateOf<String?>(null) }
    val bySite = vm.alarms.groupBy { it.siteId }
    val pins = vm.sites.filter { it.latitude != null && it.longitude != null }.map { s ->
        val count = bySite[s.id]?.size ?: 0
        val on = s.id == selectedSite
        MapPin(s.id, s.latitude!!, s.longitude!!, "${s.code} · $count", s.name,
            color = (if (on) IP.Yellow else if (count > 0) Color(0xFFB45309) else IP.Blue).toArgb(),
            textColor = (if (on) IP.Ink else Color.White).toArgb(), selected = on)
    }
    val site = vm.site(selectedSite) ?: vm.sites.maxByOrNull { bySite[it.id]?.size ?: 0 }
    Box(Modifier.fillMaxSize()) {
        OsmMap(pins, vm.me, Modifier.fillMaxSize()) { selectedSite = it }
        site?.let { s ->
            Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                color = Color.White, shadowElevation = 12.dp) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(s.name, style = MaterialTheme.typography.titleLarge)
                    val alarms = bySite[s.id].orEmpty()
                    Text(stringResource(R.string.n_reports, alarms.size), color = IP.Muted)
                    alarms.take(3).forEach { a ->
                        Surface(onClick = { onAlarm(a.id) }, shape = RoundedCornerShape(12.dp), color = IP.Ground, modifier = Modifier.fillMaxWidth()) {
                            Text("${a.bayLabel ?: "—"} · ${reasonLabel(a.reason)}", modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    PrimaryButton(stringResource(R.string.gps_guidance), {
                        if (s.latitude != null && s.longitude != null) startNavigation(context, s.latitude, s.longitude, s.name)
                    })
                }
            }
        }
    }
}

@Composable
fun AdminAlarmScreen(vm: AdminViewModel, alarmId: String, onBack: () -> Unit, onFine: () -> Unit) {
    val context = LocalContext.current
    val alarm = vm.alarm(alarmId)
    var plate by rememberSaveable { mutableStateOf(alarm?.plate.orEmpty()) }
    LaunchedEffect(alarmId) {
        vm.clearError()
        vm.resetSearch()
        if (plate.isNotBlank()) vm.search(plate)
    }
    Column(
        Modifier.fillMaxSize().background(IP.Ground).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BackButton(onBack)
            Column {
                Text(alarm?.bayLabel ?: stringResource(R.string.report), style = MaterialTheme.typography.headlineMedium)
                vm.site(alarm?.siteId)?.let { Text(it.name, color = IP.Muted) }
            }
        }
        if (alarm == null) {
            InfoBanner(stringResource(R.string.alarm_gone))
            return@Column
        }
        AppCard(color = if (alarm.isViolation()) IP.AmberSoft else IP.BlueSoft, border = false) {
            Text(stringResource(if (alarm.isViolation()) R.string.likely_violation else R.string.to_check).uppercase(),
                color = if (alarm.isViolation()) IP.Amber else IP.BlueDark, style = MaterialTheme.typography.labelSmall)
            Text(reasonLabel(alarm.reason), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.detected_ago, Format.ago(alarm.detectedAt), Format.time(alarm.detectedAt)), color = IP.Body)
            alarm.identifiedBy?.let { Text(identifiedLabel(it) + (alarm.customerName?.let { n -> " · $n" } ?: ""), color = IP.Body) }
        }
        AppCard {
            Text(stringResource(R.string.identify_vehicle), style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(plate, { plate = it.uppercase() }, stringResource(R.string.plate_number), Modifier.weight(1f), mono = true)
                PrimaryButton(stringResource(R.string.search), { vm.search(plate) }, Modifier.size(width = 120.dp, height = 56.dp),
                    enabled = plate.length >= 2, loading = vm.busy)
            }
            vm.hits?.let { hits ->
                when {
                    vm.profile != null -> vm.profile?.let { p ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(IP.BlueSoft).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.user_found).uppercase(), color = IP.BlueDark, style = MaterialTheme.typography.labelSmall)
                            Text("${p.firstName} ${p.lastName.take(1)}. · ${p.vehicles.firstOrNull()?.plateDisplay.orEmpty()}",
                                style = MaterialTheme.typography.titleMedium)
                            Text(paymentStateLabel(p.paymentState), color = if (p.paymentState == "up_to_date") IP.BlueDark else IP.Amber,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                    hits.isEmpty() -> Text(stringResource(R.string.no_account_for_plate), color = IP.Body,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(IP.Ground).padding(14.dp))
                    else -> hits.forEach { Text("${it.firstName} ${it.lastName} · ${it.plate.orEmpty()}") }
                }
            }
        }
        ErrorText(vm.error?.let { stringResource(errorMessage(it)) })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(stringResource(R.string.mark_compliant), { vm.acknowledge(alarm.id, onBack) }, Modifier.weight(1f))
            PrimaryButton(stringResource(R.string.issue_fine), onFine, Modifier.weight(1f))
        }
        val s = vm.site(alarm.siteId)
        if (s?.latitude != null && s.longitude != null) {
            Surface(onClick = { startNavigation(context, s.latitude, s.longitude, alarm.bayLabel ?: s.name) },
                shape = RoundedCornerShape(14.dp), color = IP.Segment, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Navigation, contentDescription = null)
                    Text("  " + stringResource(R.string.gps_guidance), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun paymentStateLabel(state: String): String = stringResource(
    when (state) {
        "up_to_date" -> R.string.pay_state_ok
        "overdue" -> R.string.pay_state_overdue
        else -> R.string.pay_state_unpaid
    },
)

@Composable
fun AdminPhotoScreen(vm: AdminViewModel, alarmId: String, onBack: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val alarm = vm.alarm(alarmId)
    var granted by remember { mutableStateOf(context.hasPermissions(Manifest.permission.CAMERA)) }
    val request = rememberPermissionRequest { granted = it }
    LaunchedEffect(alarmId) {
        vm.prepareFine()
        if (!granted) request(arrayOf(Manifest.permission.CAMERA))
    }
    val fined = vm.fined
    if (fined != null) {
        FineSent(vm, onDone)
        return
    }
    Column(Modifier.fillMaxSize().background(IP.Ground).verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(340.dp).background(Color(0xFF1C2128))) {
            if (granted && vm.photo == null) {
                val controller = rememberPhotoController()
                CameraPreview(controller, Modifier.fillMaxSize())
                IconButton(
                    onClick = { scope.launch { runCatching { takePhoto(context, controller) }.onSuccess { vm.photoTaken(context, it) } } },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).size(68.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.85f)),
                ) { Icon(Icons.Filled.CameraAlt, contentDescription = stringResource(R.string.take_photo), tint = IP.Ink) }
            } else if (vm.photo != null) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = IP.Yellow, modifier = Modifier.size(48.dp))
                    Text(stringResource(R.string.photo_taken), color = Color.White)
                }
            } else {
                Text(stringResource(R.string.camera_needed), color = Color.White, modifier = Modifier.align(Alignment.Center))
            }
            Box(Modifier.padding(16.dp)) { BackButton(onBack) }
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.fine), style = MaterialTheme.typography.headlineMedium)
            alarm?.let { Text("${it.bayLabel.orEmpty()} · ${reasonLabel(it.reason)}", color = IP.Muted) }
            Field(vm.plateRead, { vm.plateRead = it.uppercase() }, stringResource(R.string.plate_read), mono = true,
                placeholder = alarm?.plate ?: "GE 123456")
            val expected = alarm?.plate
            if (vm.photo != null && vm.plateRead.isNotBlank() && expected != null &&
                vm.plateRead.filter(Char::isLetterOrDigit) != expected.uppercase().filter(Char::isLetterOrDigit)) {
                InfoBanner(stringResource(R.string.plate_differs, expected))
            }
            AppCard {
                Row {
                    Text(stringResource(R.string.fine_per_schedule), modifier = Modifier.weight(1f), color = IP.Body)
                    Text(Format.money(vm.fineAmount),
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
            ErrorText(vm.error?.let { stringResource(errorMessage(it)) })
            PrimaryButton(stringResource(R.string.send_fine), { vm.sendFine(alarmId) }, yellow = true,
                enabled = alarm != null && vm.photo != null, loading = vm.busy)
        }
    }
}

@Composable
private fun FineSent(vm: AdminViewModel, onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(IP.Ground).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(40.dp))
        Box(Modifier.size(80.dp).clip(CircleShape).background(IP.Ink), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = IP.Yellow, modifier = Modifier.size(40.dp))
        }
        Text(stringResource(R.string.fine_sent), style = MaterialTheme.typography.headlineLarge)
        Text(listOfNotNull(vm.fined?.bayLabel, vm.fined?.plate).joinToString(" · "), color = IP.Muted, fontFamily = FontFamily.Monospace)
        AppCard {
            Text(stringResource(R.string.auto_processing).uppercase(), style = MaterialTheme.typography.labelSmall, color = IP.Muted)
            Step(true, stringResource(R.string.step_recorded))
            Step(true, stringResource(R.string.step_notified))
            Step(false, stringResource(R.string.step_online_payment))
            Step(false, stringResource(R.string.step_reminder))
        }
        Pill(stringResource(R.string.evidence_kept), IP.BlueSoft, IP.BlueDark)
        SecondaryButton(stringResource(R.string.back_to_live), onDone, Modifier.fillMaxWidth())
    }
}

@Composable
private fun Step(done: Boolean, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(if (done) IP.Ink else IP.Segment), contentAlignment = Alignment.Center) {
            if (done) Icon(Icons.Filled.Check, contentDescription = null, tint = IP.Yellow, modifier = Modifier.size(14.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
