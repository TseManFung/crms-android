package com.crms.crmsAndroid.api.requestResponse.Room

import com.crms.crmsAndroid.api.requestResponse.Response

data class GetRoomResponse(
    val rooms: List<SingleRoomResponse>
): Response {
    data class SingleRoomResponse(
        val room: Int ? = null,
        val campusId: Int ? = null ,
        val roomNumber: String ? = null,
        val roomName: String ? = null,
    )
}