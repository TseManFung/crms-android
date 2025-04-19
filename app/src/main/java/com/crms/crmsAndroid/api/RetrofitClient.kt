package com.crms.crmsAndroid.api


import androidx.lifecycle.ViewModelProvider
import com.crms.crmsAndroid.MainActivity
import com.crms.crmsAndroid.SharedViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private val errorInterceptor = ErrorInterceptor(SharedViewModel().loginRepository)
    fun initialize(viewModel: SharedViewModel) {
        errorInterceptor.setLoginRepository(viewModel.loginRepository)
    }
    private val baseUrls = listOf(
       //"http://172.18.37.6:8787/api/",
        "http://192.168.30.10:8787/api/",
    )

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(errorInterceptor)
        .addInterceptor(BaseUrlSwitcherInterceptor(baseUrls))

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