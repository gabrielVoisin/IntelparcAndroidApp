package com.drautomation.intelpark.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/** Driver app: the "app" routes of /api/v1 (backend/app/api/v1/app_driver.py). */
interface DriverApi {
    @POST("app/auth/register") suspend fun register(@Body body: RegisterIn): DriverTokenOut
    @POST("app/auth/login") suspend fun login(@Body body: AppLoginIn): DriverTokenOut
    @POST("app/auth/refresh") suspend fun refresh(@Body body: RefreshIn): DriverTokenOut
    @POST("app/auth/logout") suspend fun logout(@Body body: RefreshIn): Response<Unit>

    @GET("app/me") suspend fun me(): DriverOut
    @PATCH("app/me") suspend fun updateMe(@Body body: DriverPatch): DriverOut
    @POST("app/me/phone") suspend fun sendPhoneCode(@Body body: PhoneIn): PhoneCodeOut
    @POST("app/me/phone/verify") suspend fun verifyPhone(@Body body: PhoneVerifyIn): DriverOut
    @POST("app/vehicles") suspend fun addVehicle(@Body body: VehicleIn): VehicleOut
    @GET("app/home") suspend fun home(): HomeOut

    @GET("app/sites") suspend fun sites(@Query("lat") lat: Double? = null, @Query("lng") lng: Double? = null): List<AppSiteOut>
    @GET("app/nearest") suspend fun nearest(@Query("lat") lat: Double, @Query("lng") lng: Double): NearestOut
    @GET("app/bays/by-code") suspend fun bayByCode(@Query("code") code: String): AppBayOut
    @GET("app/bays/{id}") suspend fun bay(@Path("id") id: String): AppBayOut
    @POST("app/quote") suspend fun quote(@Body body: QuoteIn): QuoteOut

    // The current session comes with GET app/home (a JSON null body cannot be read as a Retrofit result).
    @POST("app/sessions") suspend fun startSession(@Body body: SessionIn): AppSessionOut
    @POST("app/sessions/{id}/extend") suspend fun extendSession(@Path("id") id: String, @Body body: ExtendIn): AppSessionOut

    @GET("app/reservations") suspend fun reservations(): List<AppReservationOut>
    @POST("app/reservations") suspend fun book(@Body body: AppReservationIn): AppReservationOut
    @POST("app/reservations/{id}/cancel") suspend fun cancelBooking(@Path("id") id: String): AppReservationOut

    @GET("app/subscription-plans") suspend fun plans(): List<AppPlanOut>
    @GET("app/subscriptions") suspend fun subscriptions(): List<AppSubscriptionOut>
    @POST("app/subscriptions") suspend fun subscribe(@Body body: AppSubscriptionIn): AppSubscriptionOut

    @POST("app/ble/token") suspend fun bleToken(@Body body: BleTokenIn): BleTokenOut
}

/** Agents and administrators: the same API as the website (backend/app/api/v1). */
interface StaffApi {
    @POST("auth/login") suspend fun login(@Body body: StaffLoginIn): StaffTokenOut
    @POST("auth/refresh") suspend fun refresh(@Body body: RefreshIn): StaffTokenOut
    @POST("auth/logout") suspend fun logout(@Body body: RefreshIn): Response<Unit>

    @GET("dashboard") suspend fun dashboard(): DashboardOut
    @GET("sites") suspend fun sites(): List<SiteOut>
    @GET("payment-alarms") suspend fun alarms(@Query("status") status: String = "open"): List<PaymentAlarmOut>
    @POST("payment-alarms/{id}/acknowledge") suspend fun acknowledge(@Path("id") id: String): PaymentAlarmOut
    @POST("payment-alarms/{id}/fine") suspend fun fine(@Path("id") id: String, @Body body: FineIn): PaymentAlarmOut

    @Multipart
    @POST("payment-alarms/{id}/evidence")
    suspend fun evidence(
        @Path("id") id: String,
        @Part photo: MultipartBody.Part,
        @Part("plate_read") plateRead: RequestBody?,
    ): EvidenceOut

    @GET("customers/search") suspend fun searchCustomers(@Query("q") q: String): List<CustomerHit>
    @GET("customers/{id}") suspend fun customer(@Path("id") id: String): CustomerProfile
    @GET("settings") suspend fun settings(): SettingsOut
}
