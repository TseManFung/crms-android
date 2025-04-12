package com.crms.crmsAndroid.api.repository

import com.crms.crmsAndroid.api.RetrofitClient
import com.crms.crmsAndroid.api.requestResponse.item.DeleteItemRequest
import com.crms.crmsAndroid.api.requestResponse.item.GetItemByRFIDRequest
import com.crms.crmsAndroid.api.requestResponse.item.GetItemByRFIDResponse
import com.crms.crmsAndroid.api.requestResponse.item.GetItemRequest
import com.crms.crmsAndroid.api.requestResponse.item.GetItemResponse

class DeviceRepository {
    suspend fun getItems(token: String, roomID: Int, stateList: List<String>): Result<List<GetItemResponse.Devices>> {
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

    suspend fun getItemByRFID(token: String, rfid: String): Result<GetItemByRFIDResponse> {

        return try {
            val request = GetItemByRFIDRequest(token = token, RFID = rfid)
            val response = RetrofitClient.instance.getItemByRFID(request)

            if (response.isSuccessful) {
                Result.success(response.body() ?: throw Exception("Empty response body"))
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    }

    suspend fun deleteItem(token: String, deviceID: Int): Result<Boolean>  {

        return try {
            val request = DeleteItemRequest(token = token, deviceID = deviceID)
            val response = RetrofitClient.instance.deleteItem(request)

            if (response.isSuccessful) {
                val status = response.body()?.status ?: false
                Result.success(status)
            } else {
                Result.failure(Exception("exist RFID for this room"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

    }


}
