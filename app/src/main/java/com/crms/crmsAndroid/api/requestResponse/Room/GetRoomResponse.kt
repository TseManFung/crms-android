package com.crms.crmsAndroid.api.requestResponse.Room

import com.crms.crmsAndroid.api.requestResponse.Response

data class GetRoomResponse(
    val Rooms: List<SingleRoomResponse>
): Response {
    data class SingleRoomResponse(
        val room: Int,
        val campusId: Int,
        val roomNumber: String,
        val roomName: String,
    )
}