package com.crms.crmsAndroid.api.requestResponse.item

import com.crms.crmsAndroid.api.requestResponse.Request

data class GetItemByRFIDRequest(val token: String, val RFID: String) : Request
