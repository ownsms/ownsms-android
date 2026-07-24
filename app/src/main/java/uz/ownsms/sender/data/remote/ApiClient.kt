package uz.ownsms.sender.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds Retrofit APIs over ONE shared [OkHttpClient] (shared connection pool + dispatcher), so the
 * ~12 requests/min from the poll and report loops reuse TCP/TLS instead of handshaking every call.
 * The bearer token is read PER-REQUEST from a provider, so a re-registration that changes the token
 * takes effect with no rebuild. The device/dev proxies are cached and rebuilt only if the base URL changes.
 */
object ApiClient {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    // Per-API clients are derived from this via newBuilder(), so they share its pool + dispatcher.
    private val shared = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        // long-poll holds ~30s server-side; allow margin so it isn't cut off
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun bearer(token: () -> String) = Interceptor { chain ->
        chain.proceed(
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${token()}")
                .addHeader("Accept", "application/json")
                .build(),
        )
    }

    private fun retrofit(baseUrl: String, token: () -> String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(normalizeBase(baseUrl))
            .client(shared.newBuilder().addInterceptor(bearer(token)).build())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    /** One-off device API with a fixed token (e.g. registration / health-check before auth). */
    fun create(baseUrl: String, token: String): OwnSmsApi = retrofit(baseUrl) { token }.create(OwnSmsApi::class.java)

    private var deviceBase: String? = null
    private var deviceApi: OwnSmsApi? = null

    /** Cached device API (Bearer = device token, read per request). */
    @Synchronized
    fun device(baseUrl: String, token: () -> String): OwnSmsApi {
        if (deviceApi == null || deviceBase != baseUrl) {
            deviceApi = retrofit(baseUrl, token).create(OwnSmsApi::class.java)
            deviceBase = baseUrl
        }
        return deviceApi!!
    }

    private var devBase: String? = null
    private var devApi: DevApi? = null

    /** Cached developer API (Bearer = api_key, read per request). */
    @Synchronized
    fun dev(baseUrl: String, token: () -> String): DevApi {
        if (devApi == null || devBase != baseUrl) {
            devApi = retrofit(baseUrl, token).create(DevApi::class.java)
            devBase = baseUrl
        }
        return devApi!!
    }

    /** Retrofit requires the base URL to end with '/'. */
    private fun normalizeBase(b: String): String = if (b.endsWith("/")) b else "$b/"
}
