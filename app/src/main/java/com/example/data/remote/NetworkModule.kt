package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class RetryInterceptor(private val maxRetries: Int = 2) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var lastException: IOException? = null
        while (attempt <= maxRetries) {
            try {
                val response = chain.proceed(chain.request())
                if (response.isSuccessful || response.code !in 500..599 || attempt == maxRetries) {
                    return response
                }
                response.close()
            } catch (e: IOException) {
                lastException = e
                if (attempt == maxRetries) throw e
            }
            attempt++
        }
        throw lastException ?: IOException("Unknown network error")
    }
}

object NetworkModule {
    val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(OdptLocalizedTitleAdapter())
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("Authorization")
            redactHeader("X-RapidAPI-Key")
        }
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(RetryInterceptor(maxRetries = 2))
            .addInterceptor(logging)
            .build()
    }

    val openMeteoApi: OpenMeteoApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoApi::class.java)
    }

    fun createOdptApi(
        baseUrl: String = "https://api.odpt.org/api/v4/",
        client: OkHttpClient = okHttpClient
    ): OdptApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(OdptApi::class.java)

    val odptApi: OdptApi by lazy { createOdptApi() }

    fun createEkispertApi(
        baseUrl: String = "https://api.ekispert.jp/",
        client: OkHttpClient = okHttpClient
    ): EkispertApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .build()
        .create(EkispertApi::class.java)

    val ekispertApi: EkispertApi by lazy { createEkispertApi() }

    fun createMixwayApi(
        baseUrl: String = "https://mixway.ekispert.jp/",
        client: OkHttpClient = okHttpClient
    ): MixwayApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .build()
        .create(MixwayApi::class.java)

    val mixwayApi: MixwayApi by lazy { createMixwayApi() }

    fun createJrTrainformationApi(
        baseUrl: String = "https://localhost.invalid/",
        client: OkHttpClient = okHttpClient
    ): JrTrainformationApi = Retrofit.Builder()
        .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
        .client(client)
        .build()
        .create(JrTrainformationApi::class.java)

    fun createJrShikokuRealtimeApi(
        baseUrl: String = "https://localhost.invalid/",
        client: OkHttpClient = okHttpClient
    ): JrShikokuRealtimeApi = Retrofit.Builder()
        .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
        .client(client)
        .build()
        .create(JrShikokuRealtimeApi::class.java)

    fun createRestCountriesApi(
        baseUrl: String = "https://restcountries.com/",
        client: OkHttpClient = okHttpClient
    ): RestCountriesApi = Retrofit.Builder()
        .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
        .client(client)
        .build()
        .create(RestCountriesApi::class.java)

    val restCountriesApi: RestCountriesApi by lazy { createRestCountriesApi() }
}
