package com.crms.crmsAndroid.api

//import com.crms.crmsAndroid.api.requestResponse.*
//import retrofit2.Call
//import retrofit2.http.Body
//import retrofit2.http.POST
//
//interface IApiService {
//    @POST("login")
//    fun login(@Body request: LoginRequest): Call<LoggedInUser>
//
//
//    @POST("/getcampus")
//    fun getCampus(@Body request: GetCampusRequest): Call<GetCampusResponse>
//}

import com.crms.crmsAndroid.api.requestResponse.*
import com.crms.crmsAndroid.api.requestResponse.Room.GetRoomRequest
import com.crms.crmsAndroid.api.requestResponse.Room.GetRoomResponse
import com.crms.crmsAndroid.api.requestResponse.campus.GetCampusRequest
import com.crms.crmsAndroid.api.requestResponse.campus.GetCampusResponse
import com.crms.crmsAndroid.api.requestResponse.item.GetItemRequest
import com.crms.crmsAndroid.api.requestResponse.item.GetItemResponse
import com.crms.crmsAndroid.api.requestResponse.item.ManualInventoryRequest
import com.crms.crmsAndroid.api.requestResponse.item.ManualInventoryResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface IApiService {
    //xd
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoggedInUser>

    @POST("getcampus")
    suspend fun getCampus(@Body request: GetCampusRequest): Response<GetCampusResponse>

    @POST("getrooms")
    suspend fun getRooms(@Body request: GetRoomRequest): Response<GetRoomResponse>

    @POST("manualinventory")
    suspend fun manualinventory(@Body request: ManualInventoryRequest): Response<ManualInventoryResponse>

    @POST("getitems")
    suspend fun getItems(@Body request: GetItemRequest): Response<GetItemResponse>


}