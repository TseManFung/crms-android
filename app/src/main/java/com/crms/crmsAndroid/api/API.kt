//package com.crms.crmsAndroid.api
//
//import com.crms.crmsAndroid.api.requestResponse.*
//
//private const val baseUrl:String = "https://yourapi.com/"
//
//class API {
//
//    private val retrofit = retrofit2.Retrofit.Builder()
//        .baseUrl(baseUrl) // 替换为你的 API 地址
//        .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
//        .build()
//
//    private val apiService: IApiService = retrofit.create(IApiService::class.java)
//
//    // 提供登录方法
//    fun login(username: String, password: String, callback: retrofit2.Callback<LoggedInUser>) {
//        val request = LoginRequest(username, password)
//        apiService.login(request).enqueue(callback)
//    }
//}