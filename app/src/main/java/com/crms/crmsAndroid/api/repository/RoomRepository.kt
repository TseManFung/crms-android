package com.crms.crmsAndroid.api.repository

import com.crms.crmsAndroid.api.RetrofitClient
import com.crms.crmsAndroid.api.requestResponse.Room.GetRoomRequest
import com.crms.crmsAndroid.api.requestResponse.Room.GetRoomResponse
import com.crms.crmsAndroid.api.requestResponse.Room.NewRoomRequest
import com.crms.crmsAndroid.api.requestResponse.Room.NewRoomResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.Result

class RoomRepository {
    suspend fun getRooms(token: String, campusID: Int): Result<List<GetRoomResponse.SingleRoomResponse>> {
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

    suspend fun newRoom(token: String, roomID: Int, rfid: String): Result<Boolean> { // 💡 注意返回的是 kotlin.Result
        return try {
            val request = NewRoomRequest(token = token, roomID = roomID, roomRFID = rfid)
            val response = RetrofitClient.instance.newRoom(request) // 假设已定义该API接口

            // 💡 明确处理响应状态
            if (response.isSuccessful) {
                val status = response.body()?.status ?: false
                Result.success(status) // 使用标准库的Result
            } else {
                Result.failure(Exception("API错误: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e) // 使用标准库的Result
        }
    }

}