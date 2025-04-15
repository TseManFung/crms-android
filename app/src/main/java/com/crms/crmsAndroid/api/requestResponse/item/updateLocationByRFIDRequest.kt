package com.crms.crmsAndroid.api.requestResponse.item

import com.crms.crmsAndroid.api.requestResponse.Request


data class updateLocationByRFIDRequest(
    val token: String,
    val roomID: Int,
    val itemList: List<String>
) : Request
