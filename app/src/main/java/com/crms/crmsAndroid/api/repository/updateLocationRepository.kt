package com.crms.crmsAndroid.api.repository

import com.crms.crmsAndroid.api.RetrofitClient
import com.fyp.crms_backend.dto.item.updateLocationByRFIDRequest
import com.fyp.crms_backend.dto.item.updateLocationByRFIDResponse

class updateLocationRepository {
    suspend fun updateLocation(
        token: String,
        roomID: Int,
        itemList: List<String>
    ): Result<updateLocationByRFIDResponse> {
        return try {
            val request =
                updateLocationByRFIDRequest(token = token, roomID = roomID, itemList = itemList)
            val response = RetrofitClient.instance.updateItemLocation(request)

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(
                    Exception(
                        "API error: ${response.code()} ${
                            response.errorBody()?.string()
                        }"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}