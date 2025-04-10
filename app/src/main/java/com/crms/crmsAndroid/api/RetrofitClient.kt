package com.crms.crmsAndroid.api


import com.crms.crmsAndroid.SharedViewModel
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.OkHttpClient

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val baseUrl:String = "http://192.168.30.10:8787/api/"
    //private const val baseUrl:String = "http://172.18.37.6:8787/api/"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(ErrorInterceptor(SharedViewModel().loginRepository)) // 加入攔截器
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val instance: IApiService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(IApiService::class.java)
    }
}