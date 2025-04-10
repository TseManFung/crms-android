package com.crms.crmsAndroid.api.requestResponse.Room

import com.crms.crmsAndroid.api.requestResponse.Request

data class NewRoomRequest(
    val token: String,
    val roomID: Int,
    val roomRFID: String
) : Request