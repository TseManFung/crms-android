package com.crms.crmsAndroid.api.repository

import com.crms.crmsAndroid.api.RetrofitClient
import com.crms.crmsAndroid.api.requestResponse.Room.GetRoomRequest
import com.crms.crmsAndroid.api.requestResponse.Room.GetRoomResponse
import com.crms.crmsAndroid.api.requestResponse.Room.NewRoomRequest

class RoomRepository {
    suspend fun getRooms(
        token: String,
        campusID: Int
    ): Result<List<GetRoomResponse.SingleRoomResponse>> {
        return try {
            val request = GetRoomRequest(token = token, campusID = campusID)
            val response = RetrofitClient.instance.getRooms(request)

            if (response.isSuccessful) {
                Result.success(response.body()?.rooms ?: emptyList())
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun newRoom(
        token: String,
        roomID: Int,
        rfid: String
    ): Result<Boolean> { // 💡 注意返回的是 kotlin.Result
        return try {
            val request = NewRoomRequest(token = token, roomID = roomID, roomRFID = rfid)
            val response = RetrofitClient.instance.newRoom(request)

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