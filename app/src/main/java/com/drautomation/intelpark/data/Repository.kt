package com.drautomation.intelpark.data

import android.os.Build
import com.drautomation.intelpark.ble.DeviceKey
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.time.OffsetDateTime

/** Error code of the server ({"detail": "<code>"}), or "network", "validation", "bad_response". */
class AppError(val code: String, val httpStatus: Int? = null) : Exception(code)

suspend fun <T> apiCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: HttpException) {
    val detail = runCatching {
        val body = e.response()?.errorBody()?.string().orEmpty()
        when (val d = AppJson.decodeFromString(ErrorBody.serializer(), body).detail) {
            is JsonPrimitive -> d.contentOrNull
            is JsonArray -> "validation"  // FastAPI 422: list of field errors
            else -> null
        }
    }.getOrNull()
    Result.failure(AppError(detail ?: "http_${e.code()}", e.code()))
} catch (e: IOException) {
    Result.failure(AppError("network"))
} catch (e: SerializationException) {
    Result.failure(AppError("bad_response"))
} catch (e: IllegalArgumentException) {
    Result.failure(AppError("server_url_invalid"))
}

private val clientName = "Android · ${Build.MANUFACTURER} ${Build.MODEL}".take(80)

class DriverRepository(private val api: ApiClient, private val store: SessionStore) {
    private fun d() = api.driver()

    suspend fun register(first: String, last: String, email: String, password: String, locale: String) = apiCall {
        d().register(RegisterIn(first, last, email, password, locale, clientName)).also { remember(it) }
    }

    suspend fun login(email: String, password: String) = apiCall {
        d().login(AppLoginIn(email, password, clientName)).also { remember(it) }
    }

    private fun remember(t: DriverTokenOut) =
        store.signIn(Role.DRIVER, t.accessToken, t.refreshToken, "${t.driver.firstName} ${t.driver.lastName}")

    suspend fun logout() {
        store.refreshToken?.let { token -> apiCall { d().logout(RefreshIn(token)) } }
        store.signOut()
    }

    suspend fun me() = apiCall { d().me() }
    suspend fun home() = apiCall { d().home() }
    suspend fun addVehicle(plate: String, country: String) = apiCall { d().addVehicle(VehicleIn(plate, country)) }
    suspend fun updateAddress(line: String, postcode: String, city: String, country: String) = apiCall {
        d().updateMe(DriverPatch(addressLine = line, postcode = postcode, city = city, country = country))
    }
    suspend fun sendPhoneCode(phone: String) = apiCall { d().sendPhoneCode(PhoneIn(phone)) }
    suspend fun verifyPhone(code: String) = apiCall { d().verifyPhone(PhoneVerifyIn(code)) }

    suspend fun sites(lat: Double?, lng: Double?) = apiCall { d().sites(lat, lng) }
    suspend fun nearest(lat: Double, lng: Double) = apiCall { d().nearest(lat, lng) }
    suspend fun bayByCode(code: String) = apiCall { d().bayByCode(code) }
    suspend fun bay(id: String) = apiCall { d().bay(id) }
    suspend fun quote(bayId: String, minutes: Int) = apiCall { d().quote(QuoteIn(bayId, minutes)) }
    suspend fun pay(bayId: String, minutes: Int, vehicleId: String?) = apiCall { d().startSession(SessionIn(bayId, minutes, vehicleId)) }
    suspend fun extend(sessionId: String, minutes: Int) = apiCall { d().extendSession(sessionId, ExtendIn(minutes)) }

    suspend fun reservations() = apiCall { d().reservations() }
    suspend fun book(siteId: String, start: OffsetDateTime, end: OffsetDateTime, vehicleId: String?) = apiCall {
        d().book(AppReservationIn(siteId, start.toString(), end.toString(), vehicleId = vehicleId))
    }
    suspend fun cancelBooking(id: String) = apiCall { d().cancelBooking(id) }
    suspend fun plans() = apiCall { d().plans() }
    suspend fun subscriptions() = apiCall { d().subscriptions() }
    suspend fun subscribe(siteId: String, planId: String, startsOn: String, vehicleId: String?) = apiCall {
        d().subscribe(AppSubscriptionIn(siteId, planId, startsOn, vehicleId))
    }

    /** Identity token for the cabinets, renewed when it expires in less than 2 hours. */
    suspend fun bleToken(): Result<String> {
        val now = System.currentTimeMillis() / 1000
        val cached = store.bleToken
        if (cached != null && store.bleTokenExpiresAt - now > 2 * 3600) return Result.success(cached)
        return apiCall {
            val out = d().bleToken(BleTokenIn(DeviceKey.publicKeyBase64(), clientName))
            store.bleToken = out.token
            store.bleTokenExpiresAt = OffsetDateTime.parse(out.expiresAt).toEpochSecond()
            out.token
        }
    }
}

class StaffRepository(private val api: ApiClient, private val store: SessionStore) {
    private fun s() = api.staff()

    suspend fun login(email: String, password: String) = apiCall {
        s().login(StaffLoginIn(email, password, clientName)).also {
            store.signIn(Role.STAFF, it.accessToken, it.refreshToken, it.user.fullName, it.user.role)
        }
    }

    suspend fun logout() {
        store.refreshToken?.let { token -> apiCall { s().logout(RefreshIn(token)) } }
        store.signOut()
    }

    suspend fun dashboard() = apiCall { s().dashboard() }
    suspend fun sites() = apiCall { s().sites() }
    suspend fun alarms() = apiCall { s().alarms("open") }
    suspend fun acknowledge(id: String) = apiCall { s().acknowledge(id) }
    suspend fun fine(id: String, amountCents: Int?) = apiCall { s().fine(id, FineIn(amountCents)) }
    suspend fun search(q: String) = apiCall { s().searchCustomers(q) }
    suspend fun customer(id: String) = apiCall { s().customer(id) }
    /** Default fine of the back office settings, in the site currency. */
    suspend fun fineAmount() = apiCall {
        val values = s().settings().values
        Money(
            (values["control.fine_cents"]?.jsonPrimitive?.intOrNull ?: 3500).toLong(),
            values["general.currency"]?.jsonPrimitive?.contentOrNull ?: "EUR",
        )
    }

    suspend fun uploadEvidence(alarmId: String, jpeg: File, plateRead: String?) = apiCall {
        val photo = MultipartBody.Part.createFormData("photo", jpeg.name, jpeg.readBytes().toRequestBody("image/jpeg".toMediaType()))
        val plate = plateRead?.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaType())
        s().evidence(alarmId, photo, plate)
    }
}
