package com.drautomation.intelpark.ui.driver

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drautomation.intelpark.R
import com.drautomation.intelpark.data.AppSiteOut
import com.drautomation.intelpark.data.Format
import com.drautomation.intelpark.ui.common.MapPin
import com.drautomation.intelpark.ui.common.OsmMap
import com.drautomation.intelpark.ui.common.errorMessage
import com.drautomation.intelpark.ui.common.rememberPermissionRequest
import com.drautomation.intelpark.ui.common.startNavigation
import com.drautomation.intelpark.ui.components.AppCard
import com.drautomation.intelpark.ui.components.ChoiceChip
import com.drautomation.intelpark.ui.components.ErrorText
import com.drautomation.intelpark.ui.components.InfoBanner
import com.drautomation.intelpark.ui.components.Pill
import com.drautomation.intelpark.ui.components.PrimaryButton
import com.drautomation.intelpark.ui.components.ScreenHeader
import com.drautomation.intelpark.ui.components.SecondaryButton
import com.drautomation.intelpark.ui.components.Segmented
import com.drautomation.intelpark.ui.theme.IP
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MapScreen(vm: DriverViewModel, onBook: (String) -> Unit) {
    val context = LocalContext.current
    val request = rememberPermissionRequest { vm.loadMap(context) }
    LaunchedEffect(Unit) {
        request(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val nearestId = vm.nearest?.site?.id
    val selected: AppSiteOut? = vm.sites.firstOrNull { it.id == (selectedId ?: nearestId) } ?: vm.sites.firstOrNull()
    val pins = vm.sites.filter { it.latitude != null && it.longitude != null }.map {
        MapPin(it.id, it.latitude!!, it.longitude!!, it.freeBays.toString(), it.name,
            color = (if (it.id == selected?.id) IP.Yellow else if (it.freeBays > 0) IP.Blue else IP.Muted).toArgb(),
            textColor = (if (it.id == selected?.id) IP.Ink else Color.White).toArgb(),
            selected = it.id == selected?.id)
    }
    Box(Modifier.fillMaxSize().background(Color(0xFFE4E1D7))) {
        OsmMap(pins, vm.me, Modifier.fillMaxSize()) { selectedId = it }
        Row(Modifier.padding(16.dp).clip(RoundedCornerShape(18.dp)).background(IP.Ink).padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text(stringResource(R.string.free_bays_total, vm.sites.sumOf { it.freeBays }), color = Color.White,
                style = MaterialTheme.typography.labelMedium)
        }
        selected?.let { site ->
            Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                color = Color.White, shadowElevation = 12.dp) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (site.id == nearestId) Pill(stringResource(R.string.nearest), IP.Yellow, IP.Ink)
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(site.name, style = MaterialTheme.typography.titleLarge)
                            Text(stringResource(R.string.free_bays_price, site.freeBays, Format.money(site.hourly)),
                                style = MaterialTheme.typography.bodyMedium, color = IP.Muted)
                            vm.nearest?.bay?.takeIf { site.id == nearestId }?.let {
                                Text(stringResource(R.string.suggested_bay, "${it.zoneCode} · ${it.number}"),
                                    style = MaterialTheme.typography.bodyMedium, color = IP.Blue, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(Format.distance(site.distanceM), color = IP.Blue, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton(stringResource(R.string.go_there), {
                            if (site.latitude != null && site.longitude != null) startNavigation(context, site.latitude, site.longitude, site.name)
                        }, Modifier.weight(1f))
                        PrimaryButton(stringResource(R.string.book), { onBook(site.id) }, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun BookingScreen(vm: DriverViewModel, preselectedSite: String?) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        vm.clearError()
        vm.clearConfirmations()
        vm.loadBooking(context)
    }
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var siteId by rememberSaveable { mutableStateOf(preselectedSite) }
    val site = vm.sites.firstOrNull { it.id == siteId } ?: vm.sites.firstOrNull()
    Column(
        Modifier.fillMaxSize().background(IP.Ground).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(stringResource(R.string.booking_title))
        Segmented(listOf(stringResource(R.string.one_off), stringResource(R.string.subscription)), mode, { mode = it; vm.clearError() })
        Text(stringResource(R.string.car_park), style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            vm.sites.take(3).forEach { s ->
                ChoiceChip(s.code, s.id == site?.id, { siteId = s.id }, Modifier.weight(1f), sub = stringResource(R.string.n_free, s.freeBays))
            }
        }
        site?.let { Text(it.name, style = MaterialTheme.typography.bodyMedium, color = IP.Muted) }
        if (mode == 0) OneOffBooking(vm, site) else SubscriptionForm(vm, site)
    }
}

@Composable
private fun OneOffBooking(vm: DriverViewModel, site: AppSiteOut?) {
    val zone = ZoneId.systemDefault()
    var day by rememberSaveable { mutableIntStateOf(0) }
    var start by rememberSaveable { mutableIntStateOf(((LocalTime.now().hour + 1).coerceAtMost(22)) * 60) }
    var end by rememberSaveable { mutableIntStateOf(start + 120) }
    val date = LocalDate.now().plusDays(day.toLong())
    val startAt = ZonedDateTime.of(date, LocalTime.of(start / 60, start % 60), zone).toOffsetDateTime()
    val endAt = startAt.plusMinutes((end - start).toLong())
    val hourly = site?.hourly
    val estimate = hourly?.let { it.copy(amountCents = it.amountCents * ((end - start + 59) / 60)) }

    val confirmed = vm.lastBooking
    if (confirmed != null) {
        AppCard(color = IP.Ink, border = false) {
            Text(stringResource(R.string.booking_confirmed).uppercase(), color = IP.Yellow, style = MaterialTheme.typography.labelSmall)
            Text(confirmed.bayLabel, color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.booked_from_to, Format.dateTime(confirmed.startsAt), Format.time(confirmed.endsAt)), color = Color(0xFFC9CDD3))
            Text(stringResource(R.string.paid_amount, Format.money(confirmed.price)), color = Color(0xFFC9CDD3))
            SecondaryButton(stringResource(R.string.new_booking), { vm.clearConfirmations() }, Modifier.fillMaxWidth())
        }
    } else {
        Text(stringResource(R.string.day), style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (0..3).forEach { i ->
                val d = LocalDate.now().plusDays(i.toLong())
                val label = if (i == 0) stringResource(R.string.today) else d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                ChoiceChip(label, day == i, { day = i }, Modifier.weight(1f), sub = d.dayOfMonth.toString())
            }
        }
        AppCard {
            TimeRow(stringResource(R.string.arrival), start, { start = (it).coerceIn(6 * 60, 22 * 60); if (end <= start) end = start + 30 })
            TimeRow(stringResource(R.string.departure), end, { end = it.coerceIn(start + 30, 23 * 60 + 30) })
        }
        InfoBanner(stringResource(R.string.booking_rules))
        ErrorText(vm.error?.let { stringResource(errorMessage(it)) })
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${date.format(DateTimeFormatter.ofPattern("EEE d MMM"))} · ${Format.duration((end - start).toLong())}",
                color = IP.Muted, modifier = Modifier.weight(1f))
            Text("≈ " + Format.money(estimate), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        PrimaryButton(stringResource(R.string.book_now), { site?.let { vm.book(it.id, startAt, endAt) } }, yellow = true,
            enabled = site != null && startAt.isAfter(OffsetDateTime.now().minusMinutes(15)), loading = vm.busy)
    }
    if (vm.reservations.isNotEmpty()) {
        Text(stringResource(R.string.my_bookings), style = MaterialTheme.typography.titleMedium)
        vm.reservations.take(5).forEach { r ->
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.bayLabel, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.booked_from_to, Format.dateTime(r.startsAt), Format.time(r.endsAt)),
                            style = MaterialTheme.typography.bodyMedium, color = IP.Muted)
                        Text(statusLabel(r.status), style = MaterialTheme.typography.labelMedium, color = IP.Blue)
                    }
                    if (r.status == "confirmed") {
                        TextButton(onClick = { vm.cancel(r.id) }) { Text(stringResource(R.string.cancel), color = IP.Amber) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeRow(label: String, minutes: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = IP.Muted)
            Text("%02d:%02d".format(minutes / 60, minutes % 60), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        }
        OutlinedIconButton(onClick = { onChange(minutes - 30) }, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.earlier, label))
        }
        OutlinedIconButton(onClick = { onChange(minutes + 30) }, modifier = Modifier.padding(start = 8.dp).size(44.dp)) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.later, label))
        }
    }
}

@Composable
private fun SubscriptionForm(vm: DriverViewModel, site: AppSiteOut?) {
    var planId by rememberSaveable { mutableStateOf<String?>(null) }
    var startNextMonth by rememberSaveable { mutableStateOf(true) }
    val plan = vm.plans.firstOrNull { it.id == planId } ?: vm.plans.firstOrNull()
    val startsOn = remember(startNextMonth) {
        if (startNextMonth) LocalDate.now().withDayOfMonth(1).plusMonths(1) else LocalDate.now()
    }
    val confirmed = vm.lastSubscription
    if (confirmed != null) {
        AppCard(color = IP.Ink, border = false) {
            Text(stringResource(R.string.subscription_active).uppercase(), color = IP.Yellow, style = MaterialTheme.typography.labelSmall)
            Text("${confirmed.planName} · ${confirmed.siteName}", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.sub_period, Format.date(confirmed.startsOn), Format.date(confirmed.endsOn)), color = Color(0xFFC9CDD3))
            Text(stringResource(R.string.per_month, Format.money(confirmed.price)), color = Color(0xFFC9CDD3))
            SecondaryButton(stringResource(R.string.ok), { vm.clearConfirmations() }, Modifier.fillMaxWidth())
        }
    } else {
        Text(stringResource(R.string.plan), style = MaterialTheme.typography.labelMedium)
        vm.plans.forEach { p ->
            val on = p.id == plan?.id
            Surface(onClick = { planId = p.id }, shape = RoundedCornerShape(16.dp), color = if (on) IP.Yellow else Color.White,
                border = androidx.compose.foundation.BorderStroke(if (on) 2.dp else 1.dp, if (on) IP.Ink else IP.Line),
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (p.periodMonths > 1) stringResource(R.string.commitment_months, p.periodMonths) else stringResource(R.string.no_commitment),
                            style = MaterialTheme.typography.bodyMedium, color = IP.Body,
                        )
                    }
                    Text(stringResource(R.string.per_month, Format.money(p.price)), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(stringResource(R.string.start), style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(stringResource(R.string.start_today), !startNextMonth, { startNextMonth = false }, Modifier.weight(1f))
            ChoiceChip(stringResource(R.string.start_next_month), startNextMonth, { startNextMonth = true }, Modifier.weight(1f),
                sub = Format.date(LocalDate.now().withDayOfMonth(1).plusMonths(1).toString()))
        }
        ErrorText(vm.error?.let { stringResource(errorMessage(it)) })
        PrimaryButton(
            stringResource(R.string.subscribe_for, Format.money(plan?.price)),
            { if (site != null && plan != null) vm.subscribe(site.id, plan.id, startsOn.toString()) },
            yellow = true, enabled = site != null && plan != null, loading = vm.busy,
        )
    }
    if (vm.subscriptions.isNotEmpty()) {
        Text(stringResource(R.string.my_subscriptions), style = MaterialTheme.typography.titleMedium)
        vm.subscriptions.forEach { s ->
            AppCard {
                Text("${s.planName} · ${s.siteName}", style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.sub_period, Format.date(s.startsOn), Format.date(s.endsOn)), color = IP.Muted)
                Text(statusLabel(s.status), style = MaterialTheme.typography.labelMedium, color = IP.Blue)
            }
        }
    }
}

@Composable
fun statusLabel(status: String): String = stringResource(
    when (status) {
        "confirmed" -> R.string.status_confirmed
        "active" -> R.string.status_active
        "completed" -> R.string.status_completed
        "cancelled" -> R.string.status_cancelled
        "suspended" -> R.string.status_suspended
        "terminated", "expired" -> R.string.status_ended
        else -> R.string.status_other
    },
)
