package com.crms.crmsAndroid.api

import com.crms.crmsAndroid.api.requestResponse.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface IApiService {
    @POST("login")
    fun login(@Body request: LoginRequest): Call<LoggedInUser>
}