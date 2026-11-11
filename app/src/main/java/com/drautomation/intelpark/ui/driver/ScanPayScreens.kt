package com.drautomation.intelpark.ui.driver

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drautomation.intelpark.R
import com.drautomation.intelpark.data.AppBayOut
import com.drautomation.intelpark.data.Format
import com.drautomation.intelpark.ui.common.QrScanner
import com.drautomation.intelpark.ui.common.errorMessage
import com.drautomation.intelpark.ui.common.hasPermissions
import com.drautomation.intelpark.ui.common.rememberPermissionRequest
import com.drautomation.intelpark.ui.common.startNavigation
import com.drautomation.intelpark.ui.components.AppCard
import com.drautomation.intelpark.ui.components.BackButton
import com.drautomation.intelpark.ui.components.ChoiceChip
import com.drautomation.intelpark.ui.components.ErrorText
import com.drautomation.intelpark.ui.components.Field
import com.drautomation.intelpark.ui.components.InfoBanner
import com.drautomation.intelpark.ui.components.Loading
import com.drautomation.intelpark.ui.components.PrimaryButton
import com.drautomation.intelpark.ui.components.SecondaryButton
import com.drautomation.intelpark.ui.components.Segmented
import com.drautomation.intelpark.ui.theme.IP
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ScanScreen(vm: DriverViewModel, onBack: () -> Unit, onBay: (String) -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasPermissions(Manifest.permission.CAMERA)) }
    val request = rememberPermissionRequest { granted = it }
    var torch by remember { mutableStateOf(false) }
    var manual by remember { mutableStateOf(false) }
    var handled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        vm.clearError()
        if (!granted) request(arrayOf(Manifest.permission.CAMERA))
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF0D1014))) {
        if (granted) {
            QrScanner(Modifier.fillMaxSize(), torch) { code ->
                if (!handled && !vm.busy) {
                    handled = true
                    vm.lookupBay(code) { onBay(it) }
                }
            }
        }
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIcon(Icons.Filled.Close, stringResource(R.string.close), onBack)
                Text(stringResource(R.string.scan_the_bay), color = Color.White, style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                RoundIcon(if (torch) Icons.Filled.FlashlightOff else Icons.Filled.FlashlightOn,
                    stringResource(if (torch) R.string.torch_off else R.string.torch_on)) { torch = !torch }
            }
            Text(stringResource(R.string.scan_hint), color = Color(0xFFD3D7DD), style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 24.dp, start = 20.dp, end = 20.dp))
            Spacer(Modifier.height(32.dp))
            Box(Modifier.align(Alignment.CenterHorizontally).size(260.dp).border(5.dp, IP.Yellow, RoundedCornerShape(24.dp)))
            Spacer(Modifier.weight(1f))
            if (!granted) {
                Text(stringResource(R.string.camera_needed), color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            vm.error?.let {
                ErrorText(stringResource(errorMessage(it)))
                LaunchedEffect(it) { handled = false }
            }
            TextButton(onClick = { manual = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Keyboard, contentDescription = null, tint = Color.White)
                Text("  " + stringResource(R.string.enter_bay_number), color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        }
        if (vm.busy) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Loading() }
    }
    if (manual) ManualBayDialog(onDismiss = { manual = false }) { code ->
        manual = false
        vm.lookupBay(code) { onBay(it) }
    }
}

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp),
        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.14f))) {
        Icon(icon, contentDescription = label, tint = Color.White)
    }
}

@Composable
private fun ManualBayDialog(onDismiss: () -> Unit, onCode: (String) -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.enter_bay_number)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.bay_code_hint), style = MaterialTheme.typography.bodyMedium)
                Field(code, { code = it.uppercase() }, stringResource(R.string.bay_code), placeholder = "P1/EXT/05", mono = true)
            }
        },
        confirmButton = { TextButton(onClick = { onCode(code.trim()) }, enabled = code.count { it == '/' } == 2) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun PayScreen(vm: DriverViewModel, bayId: String, onBack: () -> Unit, onHome: () -> Unit) {
    LaunchedEffect(bayId) {
        vm.clearError()
        vm.openBay(bayId)
        if (vm.home == null) vm.refreshHome()
    }
    val paid = vm.paidSession
    if (paid != null && paid.bayId == bayId) {
        ParkedScreen(vm, onHome)
        return
    }
    val bay = vm.bay
    // The QR code offers two ways to use the bay: now for a short stay (pay), or later / longer (booking)
    var later by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(IP.Ground).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BackButton(onBack)
            Column {
                Text(stringResource(if (later) R.string.stay_later_title else R.string.how_long), style = MaterialTheme.typography.headlineMedium)
                bay?.let { Text("${it.siteName} · ${it.zoneCode} · ${it.number}", style = MaterialTheme.typography.bodyMedium, color = IP.Muted) }
            }
        }
        if (bay == null) {
            Loading()
            ErrorText(vm.error?.let { stringResource(errorMessage(it)) })
            return@Column
        }
        BayPlaceCard(bay)
        if (!bay.bookable) {
            ErrorText(stringResource(if (bay.status == "blocked") R.string.err_bay_blocked else R.string.bay_rented_out))
            return@Column
        }
        if (bay.rentalStatus == "rented_sublet") InfoBanner(stringResource(R.string.bay_sublet))
        Segmented(listOf(stringResource(R.string.stay_now), stringResource(R.string.stay_later)), if (later) 1 else 0,
            { later = it == 1; vm.clearError() })
        if (later) {
            LaterBooking(vm, bay)
            return@Column
        }
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedIconButton(onClick = { vm.setDuration(vm.minutes - 15) }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.minus_15))
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Format.duration(vm.minutes.toLong()), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 42.sp)
                    Text(stringResource(R.string.until, Format.time(vm.quote?.endsAt)), color = IP.Muted, style = MaterialTheme.typography.bodyMedium)
                }
                FilledIconButton(onClick = { vm.setDuration(vm.minutes + 15) }, modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = IP.Ink, contentColor = IP.Yellow)) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.plus_15))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30, 60, 120, 180).filter { bay.maxStayMin == 0 || it <= bay.maxStayMin }.forEach { m ->
                    ChoiceChip(Format.duration(m.toLong()), vm.minutes == m, { vm.setDuration(m) }, Modifier.weight(1f))
                }
            }
            if (bay.maxStayMin > 0) {
                Text(stringResource(R.string.max_stay, Format.duration(bay.maxStayMin.toLong())), style = MaterialTheme.typography.bodyMedium,
                    color = IP.Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
        AppCard {
            Text(stringResource(R.string.pay_with), style = MaterialTheme.typography.labelMedium)
            Text(stringResource(R.string.demo_payment), style = MaterialTheme.typography.bodyMedium, color = IP.Muted)
        }
        ErrorText(vm.error?.let { stringResource(errorMessage(it)) })
        Row(verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.price_per_hour, Format.money(bay.hourly)), color = IP.Muted, modifier = Modifier.weight(1f))
            Text(Format.money(vm.quote?.price), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }
        PrimaryButton(stringResource(R.string.pay_amount, Format.money(vm.quote?.price)), { vm.pay {} },
            enabled = vm.quote != null, loading = vm.busy)
    }
}

/** Where the bay is: street and address, GPS position (with directions) and QR code reference. */
@Composable
private fun BayPlaceCard(bay: AppBayOut) {
    val context = LocalContext.current
    if (bay.street == null && bay.latitude == null && bay.reference == null) return
    AppCard {
        bay.street?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
        val lat = bay.latitude
        val lng = bay.longitude
        if (lat != null && lng != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("GPS %.6f, %.6f".format(Locale.ROOT, lat, lng), fontFamily = FontFamily.Monospace, color = IP.Muted,
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { startNavigation(context, lat, lng, bay.code, walking = false) }) {
                    Text(stringResource(R.string.directions))
                }
            }
        }
        bay.reference?.let {
            Text(stringResource(R.string.bay_ref, it), fontFamily = FontFamily.Monospace, color = IP.Muted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Longer stay on the scanned bay: a booking from a day and time, for a few hours up to a week. */
@Composable
private fun LaterBooking(vm: DriverViewModel, bay: AppBayOut) {
    val confirmed = vm.bayBooking
    if (confirmed != null) {
        AppCard(color = IP.Ink, border = false) {
            Text(stringResource(R.string.booking_confirmed).uppercase(), color = IP.Yellow, style = MaterialTheme.typography.labelSmall)
            Text(stringResource(R.string.bay_booked, confirmed.bayLabel), color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.booked_from_to, Format.dateTime(confirmed.startsAt), Format.dateTime(confirmed.endsAt)), color = Color(0xFFC9CDD3))
            Text(stringResource(R.string.paid_amount, Format.money(confirmed.price)), color = Color(0xFFC9CDD3))
            SecondaryButton(stringResource(R.string.new_booking), { vm.clearBayBooking() }, Modifier.fillMaxWidth())
        }
        return
    }
    val zone = ZoneId.systemDefault()
    var day by rememberSaveable { mutableIntStateOf(0) }
    var start by rememberSaveable { mutableIntStateOf(((LocalTime.now().hour + 1).coerceAtMost(23)) * 60) }
    var length by rememberSaveable { mutableIntStateOf(24 * 60) }
    val date = LocalDate.now().plusDays(day.toLong())
    val startAt = ZonedDateTime.of(date, LocalTime.of(start / 60, start % 60), zone).toOffsetDateTime()
    LaunchedEffect(startAt, length, bay.id) { vm.setLater(startAt, length) }

    Text(stringResource(R.string.day), style = MaterialTheme.typography.labelMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (0..4).forEach { i ->
            val d = LocalDate.now().plusDays(i.toLong())
            val label = if (i == 0) stringResource(R.string.today) else d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            ChoiceChip(label, day == i, { day = i }, Modifier.weight(1f), sub = d.dayOfMonth.toString())
        }
    }
    AppCard { TimeRow(stringResource(R.string.arrival), start, { start = it.coerceIn(0, 23 * 60 + 30) }) }
    Text(stringResource(R.string.stay_length), style = MaterialTheme.typography.labelMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(4 * 60, 8 * 60, 24 * 60, 2 * 24 * 60, 7 * 24 * 60).forEach { m ->
            val label = when {
                m == 7 * 24 * 60 -> stringResource(R.string.one_week)
                m >= 24 * 60 -> stringResource(R.string.days_n, m / (24 * 60))
                else -> Format.duration(m.toLong())
            }
            ChoiceChip(label, length == m, { length = m }, Modifier.weight(1f))
        }
    }
    vm.laterQuote?.let {
        Text(stringResource(R.string.booked_from_to, Format.dateTime(it.startsAt), Format.dateTime(it.endsAt)),
            color = IP.Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
    InfoBanner(stringResource(R.string.booking_rules))
    ErrorText(vm.error?.let { stringResource(errorMessage(it)) })
    Row(verticalAlignment = Alignment.Bottom) {
        Text(stringResource(R.string.price_per_hour, Format.money(bay.hourly)), color = IP.Muted, modifier = Modifier.weight(1f))
        Text(Format.money(vm.laterQuote?.price), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    }
    PrimaryButton(stringResource(R.string.book_this_bay, Format.money(vm.laterQuote?.price)), { vm.bookScannedBay() }, yellow = true,
        enabled = vm.laterQuote != null && startAt.isAfter(OffsetDateTime.now().minusMinutes(15)), loading = vm.busy)
}

@Composable
private fun ParkedScreen(vm: DriverViewModel, onHome: () -> Unit) {
    val session = vm.paidSession ?: return
    val startBle = rememberBleStarter(vm) { session.bayId }
    LaunchedEffect(session.id) {
        vm.resetBle()
        if (vm.autoBluetooth && vm.bleHasPermissions() && vm.bleEnabled()) vm.identify(session.bayId)
    }
    Column(
        Modifier.fillMaxSize().background(IP.Ground).verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        Box(Modifier.size(96.dp).clip(CircleShape).background(IP.Yellow), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = IP.Ink, modifier = Modifier.size(48.dp))
        }
        Text(stringResource(R.string.you_are_parked), style = MaterialTheme.typography.headlineLarge)
        Text("${session.bayLabel.orEmpty()} · ${session.plate.orEmpty()}", color = IP.Muted, textAlign = TextAlign.Center)
        SessionCard(session, onExtend = { vm.extend(session.id, 30) }, busy = vm.busy)
        session.lastPayment?.let { Text(stringResource(R.string.paid_amount, Format.money(it)), color = IP.Muted) }
        BleCard(vm.ble, onIdentify = startBle)
        ErrorText(vm.error?.let { stringResource(errorMessage(it)) })
        SecondaryButton(stringResource(R.string.back_home), onHome, Modifier.fillMaxWidth())
    }
}
