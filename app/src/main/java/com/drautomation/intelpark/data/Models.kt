package com.drautomation.intelpark.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/*
 * JSON contract of the server (backend/app/schemas.py and schemas_app.py).
 * Property names are camelCase here and snake_case on the wire (JsonNamingStrategy.SnakeCase).
 * Dates are ISO 8601 strings, parsed for display with java.time.OffsetDateTime.
 */

@Serializable
data class Money(val amountCents: Long, val currency: String = "EUR")

@Serializable
data class RefreshIn(val refreshToken: String)

// --- Driver account ------------------------------------------------------------------------
@Serializable
data class RegisterIn(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val locale: String = "fr",
    val clientName: String? = null,
)

/** TEST ONLY (server TURBO_LOGIN=true): password-free sign-in, like the website's Turbo button. */
@Serializable
data class TurboIn(val clientName: String? = null)

@Serializable
data class TurboStatusOut(val enabled: Boolean = false)

@Serializable
data class AppLoginIn(val email: String, val password: String, val clientName: String? = null)

@Serializable
data class VehicleOut(
    val id: String,
    val plateDisplay: String,
    val plateNorm: String = "",
    val country: String? = null,
    val make: String? = null,
    val model: String? = null,
    val color: String? = null,
)

@Serializable
data class VehicleIn(val plateDisplay: String, val country: String? = null)

@Serializable
data class DriverOut(
    val id: String,
    val reference: String,
    val firstName: String,
    val lastName: String,
    val email: String? = null,
    val phone: String? = null,
    val phoneVerified: Boolean = false,
    val addressLine: String? = null,
    val postcode: String? = null,
    val city: String? = null,
    val country: String = "FR",
    val locale: String = "fr",
    val vehicles: List<VehicleOut> = emptyList(),
    val profileComplete: Boolean = false,
)

@Serializable
data class DriverTokenOut(val accessToken: String, val refreshToken: String, val expiresIn: Int, val driver: DriverOut)

@Serializable
data class DriverPatch(
    val firstName: String? = null,
    val lastName: String? = null,
    val addressLine: String? = null,
    val postcode: String? = null,
    val city: String? = null,
    val country: String? = null,
    val locale: String? = null,
)

@Serializable
data class PhoneIn(val phone: String)

@Serializable
data class PhoneCodeOut(val sentTo: String, val expiresIn: Int, val debugCode: String? = null)

@Serializable
data class PhoneVerifyIn(val code: String)

// --- Car parks, bays, prices ----------------------------------------------------------------
@Serializable
data class AppSiteOut(
    val id: String,
    val code: String,
    val name: String,
    val addressLine: String? = null,
    val city: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val freeBays: Int = 0,
    val totalBays: Int = 0,
    val hourly: Money? = null,
    val distanceM: Int? = null,
)

@Serializable
data class AppBayOut(
    val id: String,
    val code: String,
    val siteId: String,
    val siteCode: String,
    val siteName: String,
    val zoneCode: String,
    val zoneName: String,
    val number: String,
    val kind: String,
    val status: String,
    val hourly: Money,
    val daily: Money,
    val billingMode: String,
    val freeMinutes: Int,
    val maxStayMin: Int,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
data class NearestOut(val site: AppSiteOut, val bay: AppBayOut? = null)

@Serializable
data class QuoteIn(val bayId: String, val minutes: Int)

@Serializable
data class QuoteOut(val bayId: String, val minutes: Int, val startsAt: String, val endsAt: String, val price: Money)

// --- Parking sessions -----------------------------------------------------------------------
@Serializable
data class SessionIn(val bayId: String, val minutes: Int, val vehicleId: String? = null, val paymentMethod: String = "card")

@Serializable
data class ExtendIn(val minutes: Int)

@Serializable
data class AppSessionOut(
    val id: String,
    val bayId: String? = null,
    val bayLabel: String? = null,
    val siteName: String = "",
    val plate: String? = null,
    val enteredAt: String,
    val paidUntil: String? = null,
    val status: String,
    val identifiedBy: String? = null,
    val right: String? = null,
    val lastPayment: Money? = null,
)

// --- Reservations and subscriptions --------------------------------------------------------
@Serializable
data class AppReservationIn(
    val siteId: String,
    val startsAt: String,
    val endsAt: String,
    val bayId: String? = null,
    val vehicleId: String? = null,
)

@Serializable
data class AppReservationOut(
    val id: String,
    val bayId: String,
    val bayLabel: String,
    val siteName: String,
    val startsAt: String,
    val endsAt: String,
    val status: String,
    val price: Money,
    val freeCancellationUntil: String,
)

@Serializable
data class AppPlanOut(
    val id: String,
    val code: String,
    val name: String,
    val price: Money,
    val periodMonths: Int,
    val accessHours: String? = null,
)

@Serializable
data class AppSubscriptionIn(val siteId: String, val planId: String, val startsOn: String, val vehicleId: String? = null)

@Serializable
data class AppSubscriptionOut(
    val id: String,
    val siteName: String,
    val planName: String,
    val price: Money,
    val periodMonths: Int,
    val startsOn: String,
    val endsOn: String,
    val autoRenew: Boolean,
    val status: String,
    val bayLabel: String? = null,
)

@Serializable
data class HomeOut(
    val driver: DriverOut,
    val currentSession: AppSessionOut? = null,
    val nextReservation: AppReservationOut? = null,
    val subscriptions: List<AppSubscriptionOut> = emptyList(),
)

// --- Bluetooth identity -----------------------------------------------------------------------
@Serializable
data class BleTokenIn(val publicKey: String, val deviceName: String? = null)

@Serializable
data class BleTokenOut(val token: String, val expiresAt: String, val serverPublicKey: String)

// --- Staff (agents and administrators) ------------------------------------------------------
@Serializable
data class StaffLoginIn(val email: String, val password: String, val clientName: String? = null)

@Serializable
data class StaffOut(val id: String, val email: String, val fullName: String, val role: String, val locale: String = "fr")

@Serializable
data class StaffTokenOut(val accessToken: String, val refreshToken: String, val expiresIn: Int, val user: StaffOut)

@Serializable
data class DashboardOut(
    val freeBays: Int = 0,
    val totalBays: Int = 0,
    val occupancyPct: Int = 0,
    val openAlarms: Int = 0,
    val entriesToday: Int = 0,
    val exitsToday: Int = 0,
)

@Serializable
data class PaymentAlarmOut(
    val id: String,
    val siteId: String,
    val bayId: String? = null,
    val bayLabel: String? = null,
    val plate: String? = null,
    val reason: String,
    val detail: String? = null,
    val detectedAt: String,
    val status: String,
    val handledAt: String? = null,
    val identifiedBy: String? = null,
    val customerName: String? = null,
    val evidenceCount: Int = 0,
)

@Serializable
data class SiteOut(
    val id: String,
    val code: String,
    val name: String,
    val city: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val freeBays: Int = 0,
    val totalBays: Int = 0,
    val occupiedBays: Int = 0,
)

@Serializable
data class CustomerHit(
    val id: String,
    val reference: String,
    val firstName: String,
    val lastName: String,
    val plate: String? = null,
)

@Serializable
data class CustomerProfile(
    val id: String,
    val reference: String,
    val firstName: String,
    val lastName: String,
    val paymentState: String = "up_to_date",
    val email: String? = null,
    val phone: String? = null,
    val vehicles: List<VehicleOut> = emptyList(),
)

@Serializable
data class FineIn(val amountCents: Int? = null)

@Serializable
data class SettingsOut(val values: Map<String, JsonElement> = emptyMap())

@Serializable
data class EvidenceOut(val id: String, val sha256: String, val sizeBytes: Int, val plateRead: String? = null)

@Serializable
data class ErrorBody(val detail: JsonElement? = null)
