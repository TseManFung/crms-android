package com.crms.crmsAndroid.api.repository

import com.crms.crmsAndroid.api.RetrofitClient
import com.crms.crmsAndroid.api.requestResponse.item.GetItemByRFIDRequest
import com.crms.crmsAndroid.api.requestResponse.item.GetItemByRFIDResponse
import com.crms.crmsAndroid.api.requestResponse.item.GetItemRequest
import com.crms.crmsAndroid.api.requestResponse.item.GetItemResponse

class DeviceRepository {
    suspend fun getItems(
        token: String,
        roomID: Int,
        stateList: List<String>
    ): Result<List<GetItemResponse.Devices>> {
        return try {
            val request = GetItemRequest(token = token, roomID = roomID, stateList = stateList)
            val response = RetrofitClient.instance.getItems(request)

            if (response.isSuccessful) {
                Result.success(response.body()?.device ?: emptyList())
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getItemByRFID(token: String, rfid:String): Result<GetItemByRFIDResponse> {
        return try{
            val request = GetItemByRFIDRequest(token = token, RFID = rfid)
            val response = RetrofitClient.instance.getItemByRFID(request)

            if(response.isSuccessful){
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            }   else {
            Result.failure(Exception("API error: ${response.code()} ${response.errorBody()?.string()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
        }
    }


