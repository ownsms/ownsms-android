package uz.ownsms.sender.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    fun create(baseUrl: String, deviceToken: String): OwnSmsApi = retrofit(baseUrl, deviceToken).create(OwnSmsApi::class.java)

    /** Shared Retrofit instance (Bearer-token auth) — used for both the device and dev APIs. */
    fun retrofit(baseUrl: String, token: String): Retrofit {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val auth = Interceptor { chain ->
            val req = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/json")
                .build()
            chain.proceed(req)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(auth)
            .connectTimeout(15, TimeUnit.SECONDS)
            // long-poll holds ~30s server-side; allow margin so it isn't cut off
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizeBase(baseUrl))
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    /** Retrofit requires the base URL to end with '/'. */
    private fun normalizeBase(b: String): String = if (b.endsWith("/")) b else "$b/"
}
