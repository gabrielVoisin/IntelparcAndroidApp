package com.drautomation.intelpark.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drautomation.intelpark.AppContainer
import com.drautomation.intelpark.data.AppError
import com.drautomation.intelpark.data.DriverOut
import kotlinx.coroutines.launch
import java.util.Locale

fun Throwable.code(): String = (this as? AppError)?.code ?: "generic"

/** Next sign-up step still to do, or null when the profile is complete. */
fun DriverOut.missingStep(): String? = when {
    vehicles.isEmpty() -> "signup/plate"
    addressLine.isNullOrBlank() -> "signup/address"
    !phoneVerified -> "signup/phone"
    else -> null
}

class AuthViewModel(private val c: AppContainer) : ViewModel() {
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var driver by mutableStateOf<DriverOut?>(null)
        private set
    var codeSentTo by mutableStateOf<String?>(null)
        private set
    var debugCode by mutableStateOf<String?>(null)
        private set
    var serverUrl by mutableStateOf(c.store.serverUrl)
        private set

    // Sign-up form, kept while the driver moves between the 4 steps
    var plateCountry by mutableStateOf("CH")
    var plate by mutableStateOf("")

    fun saveServer(url: String) {
        c.store.serverUrl = url
        serverUrl = c.store.serverUrl
    }

    fun clearError() {
        error = null
    }

    private fun task(block: suspend () -> Unit) {
        if (loading) return
        viewModelScope.launch {
            loading = true
            error = null
            try {
                block()
            } finally {
                loading = false
            }
        }
    }

    /** onDone(staff, next route for a driver whose sign-up is not finished) */
    fun login(staff: Boolean, email: String, password: String, onDone: (Boolean, String?) -> Unit) = task {
        if (staff) {
            c.staff.login(email.trim(), password)
                .onSuccess { onDone(true, null) }
                .onFailure { error = it.code() }
        } else {
            c.driver.login(email.trim(), password)
                .onSuccess { driver = it.driver; onDone(false, it.driver.missingStep()) }
                .onFailure { error = it.code() }
        }
    }

    fun register(first: String, last: String, email: String, password: String, onDone: () -> Unit) = task {
        val locale = Locale.getDefault().language.takeIf { it in setOf("fr", "en", "de") } ?: "fr"
        c.driver.register(first.trim(), last.trim(), email.trim(), password, locale)
            .onSuccess { driver = it.driver; onDone() }
            .onFailure { error = it.code() }
    }

    fun addPlate(onDone: () -> Unit) = task {
        c.driver.addVehicle(plate.trim(), plateCountry)
            .onSuccess { onDone() }
            .onFailure { error = it.code() }
    }

    fun saveAddress(line: String, postcode: String, city: String, country: String, onDone: () -> Unit) = task {
        c.driver.updateAddress(line.trim(), postcode.trim(), city.trim(), country)
            .onSuccess { driver = it; onDone() }
            .onFailure { error = it.code() }
    }

    fun sendCode(dialCode: String, number: String) = task {
        val phone = dialCode + number.filter { it.isDigit() }.trimStart('0')
        c.driver.sendPhoneCode(phone)
            .onSuccess { codeSentTo = it.sentTo; debugCode = it.debugCode }
            .onFailure { error = it.code() }
    }

    fun verify(code: String, onDone: () -> Unit) = task {
        c.driver.verifyPhone(code)
            .onSuccess { driver = it; onDone() }
            .onFailure { error = it.code() }
    }
}
