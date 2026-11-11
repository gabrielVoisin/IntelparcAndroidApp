package com.drautomation.intelpark.ui.driver

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drautomation.intelpark.AppContainer
import com.drautomation.intelpark.ble.BleException
import com.drautomation.intelpark.ble.DeviceKey
import com.drautomation.intelpark.ble.ResultMsg
import com.drautomation.intelpark.data.AppBayOut
import com.drautomation.intelpark.data.AppPlanOut
import com.drautomation.intelpark.data.AppReservationOut
import com.drautomation.intelpark.data.AppSessionOut
import com.drautomation.intelpark.data.AppSiteOut
import com.drautomation.intelpark.data.AppSubscriptionOut
import com.drautomation.intelpark.data.HomeOut
import com.drautomation.intelpark.data.NearestOut
import com.drautomation.intelpark.data.QuoteOut
import com.drautomation.intelpark.ui.auth.code
import com.drautomation.intelpark.ui.common.currentLocation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

sealed interface BleState {
    data object Idle : BleState
    data object Running : BleState
    data class Done(val result: ResultMsg, val controller: String, val site: String?) : BleState
    data class Failed(val code: String) : BleState
}

class DriverViewModel(private val c: AppContainer) : ViewModel() {
    var error by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set

    private fun task(block: suspend () -> Unit) {
        if (busy) return
        viewModelScope.launch {
            busy = true
            error = null
            try {
                block()
            } finally {
                busy = false
            }
        }
    }

    fun clearError() {
        error = null
    }

    // --- Home -----------------------------------------------------------------------------
    var home by mutableStateOf<HomeOut?>(null)
        private set

    fun refreshHome() {
        viewModelScope.launch {
            c.driver.home().onSuccess { home = it }.onFailure { error = it.code() }
        }
    }

    // --- Scan a bay and pay -----------------------------------------------------------------------
    var bay by mutableStateOf<AppBayOut?>(null)
        private set
    var minutes by mutableIntStateOf(60)
        private set
    var quote by mutableStateOf<QuoteOut?>(null)
        private set
    var paidSession by mutableStateOf<AppSessionOut?>(null)
        private set
    private var quoteJob: Job? = null

    fun lookupBay(code: String, onFound: (String) -> Unit) = task {
        c.driver.bayByCode(code)
            .onSuccess { bay = it; paidSession = null; bayBooking = null; laterQuote = null; setDuration(minutes); onFound(it.id) }
            .onFailure { error = it.code() }
    }

    fun openBay(bayId: String) {
        if (bay?.id == bayId) return
        viewModelScope.launch {
            c.driver.bay(bayId).onSuccess { bay = it; paidSession = null; bayBooking = null; laterQuote = null; setDuration(minutes) }
                .onFailure { error = it.code() }
        }
    }

    fun setDuration(value: Int) {
        val max = bay?.maxStayMin?.takeIf { it > 0 } ?: (24 * 60)
        minutes = value.coerceIn(15, max)
        val bayId = bay?.id ?: return
        quoteJob?.cancel()
        quoteJob = viewModelScope.launch {
            delay(250)  // the driver may tap +/- several times
            c.driver.quote(bayId, minutes).onSuccess { quote = it }.onFailure { error = it.code() }
        }
    }

    fun pay(onPaid: () -> Unit) = task {
        val target = bay ?: return@task
        val vehicle = home?.driver?.vehicles?.firstOrNull()?.id
        c.driver.pay(target.id, minutes, vehicle)
            .onSuccess { paidSession = it; refreshHome(); onPaid() }
            .onFailure { error = it.code() }
    }

    // --- Scanned bay: book it for a longer stay (reservation on this bay) ----------------------------
    /** Start of the longer stay and its length in minutes (chosen on the scanned bay's screen). */
    var laterStart by mutableStateOf<OffsetDateTime?>(null)
        private set
    var laterMinutes by mutableIntStateOf(24 * 60)
        private set
    var laterQuote by mutableStateOf<QuoteOut?>(null)
        private set
    var bayBooking by mutableStateOf<AppReservationOut?>(null)
        private set
    private var laterQuoteJob: Job? = null

    fun setLater(start: OffsetDateTime, minutes: Int) {
        laterStart = start
        laterMinutes = minutes
        val bayId = bay?.id ?: return
        laterQuoteJob?.cancel()
        laterQuoteJob = viewModelScope.launch {
            delay(250)
            c.driver.quote(bayId, minutes, start).onSuccess { laterQuote = it }.onFailure { error = it.code() }
        }
    }

    fun bookScannedBay() = task {
        val target = bay ?: return@task
        val start = laterStart ?: return@task
        c.driver.bookBay(target.siteId, target.id, start, start.plusMinutes(laterMinutes.toLong()), home?.driver?.vehicles?.firstOrNull()?.id)
            .onSuccess { bayBooking = it; reservations = listOf(it) + reservations.filter { r -> r.id != it.id }; refreshHome() }
            .onFailure { error = it.code() }
    }

    fun clearBayBooking() {
        bayBooking = null
    }

    fun extend(sessionId: String, extra: Int) = task {
        c.driver.extend(sessionId, extra)
            .onSuccess { session -> if (paidSession?.id == session.id) paidSession = session; refreshHome() }
            .onFailure { error = it.code() }
    }

    // --- Map ------------------------------------------------------------------------------------
    var sites by mutableStateOf<List<AppSiteOut>>(emptyList())
        private set
    var nearest by mutableStateOf<NearestOut?>(null)
        private set
    var me by mutableStateOf<Pair<Double, Double>?>(null)
        private set

    fun loadMap(context: Context) {
        viewModelScope.launch {
            me = currentLocation(context)
            val location = me
            c.driver.sites(location?.first, location?.second).onSuccess { sites = it }.onFailure { error = it.code() }
            if (location != null) {
                c.driver.nearest(location.first, location.second).onSuccess { nearest = it }.onFailure { nearest = null }
            } else {
                nearest = sites.firstOrNull { it.freeBays > 0 }?.let { NearestOut(it, null) }
            }
        }
    }

    // --- Bookings and subscriptions ---------------------------------------------------------------------
    var reservations by mutableStateOf<List<AppReservationOut>>(emptyList())
        private set
    var plans by mutableStateOf<List<AppPlanOut>>(emptyList())
        private set
    var subscriptions by mutableStateOf<List<AppSubscriptionOut>>(emptyList())
        private set
    var lastBooking by mutableStateOf<AppReservationOut?>(null)
        private set
    var lastSubscription by mutableStateOf<AppSubscriptionOut?>(null)
        private set

    fun loadBooking(context: Context) {
        if (sites.isEmpty()) loadMap(context)
        viewModelScope.launch {
            c.driver.reservations().onSuccess { reservations = it }
            c.driver.plans().onSuccess { plans = it }
            c.driver.subscriptions().onSuccess { subscriptions = it }
        }
    }

    fun book(siteId: String, start: OffsetDateTime, end: OffsetDateTime) = task {
        c.driver.book(siteId, start, end, home?.driver?.vehicles?.firstOrNull()?.id)
            .onSuccess { lastBooking = it; reservations = listOf(it) + reservations; refreshHome() }
            .onFailure { error = it.code() }
    }

    fun cancel(id: String) = task {
        c.driver.cancelBooking(id)
            .onSuccess { r -> reservations = reservations.map { if (it.id == r.id) r else it }; refreshHome() }
            .onFailure { error = it.code() }
    }

    fun subscribe(siteId: String, planId: String, startsOn: String) = task {
        c.driver.subscribe(siteId, planId, startsOn, home?.driver?.vehicles?.firstOrNull()?.id)
            .onSuccess { lastSubscription = it; subscriptions = listOf(it) + subscriptions; refreshHome() }
            .onFailure { error = it.code() }
    }

    fun clearConfirmations() {
        lastBooking = null
        lastSubscription = null
    }

    // --- Bluetooth identification at the cabinet ("smart bay") --------------------------------------
    var ble by mutableStateOf<BleState>(BleState.Idle)
        private set

    val autoBluetooth: Boolean get() = c.store.autoBluetooth
    fun setAutoBluetooth(on: Boolean) {
        c.store.autoBluetooth = on
    }

    fun bleRequiredPermissions(): Array<String> = c.ble.requiredPermissions()
    fun bleHasPermissions(): Boolean = c.ble.hasPermissions()
    fun bleEnabled(): Boolean = c.ble.isEnabled
    fun bleSupported(): Boolean = c.ble.isSupported

    fun identify(bayId: String?) {
        if (ble == BleState.Running) return
        viewModelScope.launch {
            ble = BleState.Running
            val token = c.driver.bleToken().getOrElse {
                ble = BleState.Failed(it.code())
                return@launch
            }
            ble = try {
                val outcome = c.ble.identify(token, bayId) { DeviceKey.sign(it) }
                if (outcome.result.status == "expired_token" || outcome.result.status == "invalid_token") {
                    c.store.bleToken = null  // fetch a new one next time
                }
                BleState.Done(outcome.result, outcome.controller, outcome.site)
            } catch (e: BleException) {
                BleState.Failed(e.code)
            } catch (e: Exception) {
                BleState.Failed("generic")
            }
            refreshHome()
        }
    }

    fun resetBle() {
        if (ble != BleState.Running) ble = BleState.Idle
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            c.driver.logout()
            home = null
            onDone()
        }
    }
}
