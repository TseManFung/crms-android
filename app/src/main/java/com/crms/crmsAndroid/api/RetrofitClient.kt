package com.crms.crmsAndroid.api


import com.crms.crmsAndroid.SharedViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private val baseUrls = listOf(
        "http://172.18.37.6:8787/api/",
        "http://192.168.30.10:8787/api/",
    )

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(BaseUrlSwitcherInterceptor(baseUrls))
        .addInterceptor(ErrorInterceptor(SharedViewModel().loginRepository))
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val instance: IApiService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrls.first())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(IApiService::class.java)
    }
}