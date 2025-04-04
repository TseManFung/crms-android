package com.crms.crmsAndroid.api.requestResponse.Room

import com.crms.crmsAndroid.api.requestResponse.Request


data class GetRoomRequest(
    val token: String,
    val campusID: Int
) : Request

