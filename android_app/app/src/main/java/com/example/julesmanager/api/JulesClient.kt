package com.example.julesmanager.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object JulesClient {
    private const val BASE_URL = "https://jules.googleapis.com/"
    private var retrofit: Retrofit? = null
    private var apiKey: String? = null

    fun init(key: String) {
        apiKey = key
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("X-Goog-Api-Key", key)
                // Content-Type is added automatically by Retrofit/Gson
                .build()
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: JulesApi?
        get() = retrofit?.create(JulesApi::class.java)
}
