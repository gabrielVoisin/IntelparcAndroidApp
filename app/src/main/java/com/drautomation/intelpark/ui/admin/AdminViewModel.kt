package com.drautomation.intelpark.ui.admin

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drautomation.intelpark.AppContainer
import com.drautomation.intelpark.data.CustomerHit
import com.drautomation.intelpark.data.CustomerProfile
import com.drautomation.intelpark.data.DashboardOut
import com.drautomation.intelpark.data.Money
import com.drautomation.intelpark.data.PaymentAlarmOut
import com.drautomation.intelpark.data.SiteOut
import com.drautomation.intelpark.ui.auth.code
import com.drautomation.intelpark.ui.common.currentLocation
import com.drautomation.intelpark.ui.common.readPlate
import com.drautomation.intelpark.ui.common.shrinkJpeg
import kotlinx.coroutines.launch
import java.io.File

/** "Likely violation" vs "to check", as on the mock-up. */
fun PaymentAlarmOut.isViolation(): Boolean = reason != "plate_not_recognised"

class AdminViewModel(private val c: AppContainer) : ViewModel() {
    val staffName: String get() = c.store.displayName.orEmpty()

    var error by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var dashboard by mutableStateOf<DashboardOut?>(null)
        private set
    var alarms by mutableStateOf<List<PaymentAlarmOut>>(emptyList())
        private set
    var sites by mutableStateOf<List<SiteOut>>(emptyList())
        private set
    var updatedAt by mutableStateOf(0L)
        private set
    var me by mutableStateOf<Pair<Double, Double>?>(null)
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

    /** Called every 15 s while the live screens are shown. */
    suspend fun refresh() {
        c.staff.dashboard().onSuccess { dashboard = it }.onFailure { error = it.code() }
        c.staff.alarms().onSuccess { alarms = it; updatedAt = System.currentTimeMillis() }.onFailure { error = it.code() }
        if (sites.isEmpty()) c.staff.sites().onSuccess { sites = it }
    }

    fun locate(context: Context) {
        viewModelScope.launch { me = currentLocation(context) }
    }

    fun alarm(id: String): PaymentAlarmOut? = alarms.firstOrNull { it.id == id }
    fun site(id: String?): SiteOut? = sites.firstOrNull { it.id == id }

    // --- Check a spot -----------------------------------------------------------------------
    var hits by mutableStateOf<List<CustomerHit>?>(null)
        private set
    var profile by mutableStateOf<CustomerProfile?>(null)
        private set

    fun search(plate: String) = task {
        profile = null
        c.staff.search(plate.trim()).onSuccess { list ->
            hits = list
            list.singleOrNull()?.let { hit -> c.staff.customer(hit.id).onSuccess { profile = it } }
        }.onFailure { error = it.code() }
    }

    fun resetSearch() {
        hits = null
        profile = null
    }

    fun acknowledge(id: String, onDone: () -> Unit) = task {
        c.staff.acknowledge(id).onSuccess { a -> alarms = alarms.filterNot { it.id == a.id }; onDone() }.onFailure { error = it.code() }
    }

    // --- Photo and fine ------------------------------------------------------------------------
    var photo by mutableStateOf<File?>(null)
        private set
    var plateRead by mutableStateOf("")
    var fineAmount by mutableStateOf<Money?>(null)
        private set
    var fined by mutableStateOf<PaymentAlarmOut?>(null)
        private set

    fun prepareFine() {
        photo = null
        plateRead = ""
        fined = null
        viewModelScope.launch { c.staff.fineAmount().onSuccess { fineAmount = it } }
    }

    fun photoTaken(context: Context, file: File) = task {
        photo = file
        readPlate(context, file)?.let { plateRead = it }
    }

    fun sendFine(alarmId: String) = task {
        photo?.let { original ->
            val small = shrinkJpeg(original)
            c.staff.uploadEvidence(alarmId, small, plateRead).onFailure { error = it.code(); return@task }
        }
        c.staff.fine(alarmId, null)
            .onSuccess { a -> fined = a; alarms = alarms.filterNot { it.id == a.id } }
            .onFailure { error = it.code() }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            c.staff.logout()
            onDone()
        }
    }
}
