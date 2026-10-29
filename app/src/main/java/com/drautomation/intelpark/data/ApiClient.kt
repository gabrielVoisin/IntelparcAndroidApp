package com.drautomation.intelpark.data

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalSerializationApi::class)
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    namingStrategy = JsonNamingStrategy.SnakeCase
}

/**
 * Retrofit clients for the server chosen in the settings (the Raspberry Pi of the prototype).
 * Rebuilt when the server address changes. Every request carries "Authorization: Bearer <token>";
 * on 401 the refresh token is used once, then the request is replayed.
 */
class ApiClient(private val store: SessionStore) {
    private class Built(val url: String, val driver: DriverApi, val staff: StaffApi)

    @Volatile
    private var built: Built? = null

    fun driver(): DriverApi = current().driver
    fun staff(): StaffApi = current().staff

    @Synchronized
    private fun current(): Built {
        val url = store.serverUrl
        built?.let { if (it.url == url) return it }
        return build(url).also { built = it }
    }

    private fun build(serverUrl: String): Built {
        val base = serverUrl.trimEnd('/') + "/api/v1/"
        val converter = AppJson.asConverterFactory("application/json".toMediaType())
        val plain = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        val bare = Retrofit.Builder().baseUrl(base).client(plain).addConverterFactory(converter).build()
        val client = plain.newBuilder()
            .addInterceptor { chain ->
                val token = store.accessToken
                val request = chain.request()
                if (token != null && request.header("Authorization") == null) {
                    chain.proceed(request.newBuilder().header("Authorization", "Bearer $token").build())
                } else {
                    chain.proceed(request)
                }
            }
            .authenticator(TokenAuthenticator(store, bare.create(DriverApi::class.java), bare.create(StaffApi::class.java)))
            .build()
        val retrofit = Retrofit.Builder().baseUrl(base).client(client).addConverterFactory(converter).build()
        return Built(serverUrl, retrofit.create(DriverApi::class.java), retrofit.create(StaffApi::class.java))
    }
}

private class TokenAuthenticator(
    private val store: SessionStore,
    private val driverAuth: DriverApi,
    private val staffAuth: StaffApi,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        val request = response.request
        if (request.header("X-Retry") != null || request.url.encodedPath.contains("/auth/")) return null
        synchronized(this) {
            val sent = request.header("Authorization")?.removePrefix("Bearer ")
            val current = store.accessToken
            if (current != null && current != sent) {  // another request refreshed meanwhile
                return request.newBuilder().header("Authorization", "Bearer $current").header("X-Retry", "1").build()
            }
            val refresh = store.refreshToken ?: return null
            val access = runCatching {
                runBlocking {
                    when (store.role) {
                        Role.DRIVER -> driverAuth.refresh(RefreshIn(refresh)).also { store.saveTokens(it.accessToken, it.refreshToken) }.accessToken
                        Role.STAFF -> staffAuth.refresh(RefreshIn(refresh)).also { store.saveTokens(it.accessToken, it.refreshToken) }.accessToken
                        null -> null
                    }
                }
            }.getOrNull()
            if (access == null) {
                store.signOut()
                return null
            }
            return request.newBuilder().header("Authorization", "Bearer $access").header("X-Retry", "1").build()
        }
    }
}
