package com.crms.crmsAndroid.api.requestResponse.Room
import com.crms.crmsAndroid.api.requestResponse.Request



data class GetRoomByRFIDRequest(
    val token: String,
    val RFID: String,

    ) : Request
