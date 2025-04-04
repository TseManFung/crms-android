package com.crms.crmsAndroid.api.repository

import com.crms.crmsAndroid.api.RetrofitClient
import com.crms.crmsAndroid.api.requestResponse.Room.GetRoomRequest
import com.crms.crmsAndroid.api.requestResponse.Room.GetRoomResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RoomRepository {
    suspend fun getRooms(token: String, campusID: Int): Result<List<GetRoomResponse.SingleRoomResponse>> {
        return try {
            val request = GetRoomRequest(token = token, campusID = campusID)
            val response = RetrofitClient.instance.getRooms(request)

            if (response.isSuccessful) {
                Result.success(response.body()?.Rooms ?: emptyList())
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}