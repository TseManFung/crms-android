package com.crms.crmsAndroid.api.requestResponse.item

import com.crms.crmsAndroid.api.requestResponse.Response


data class GetItemByRFIDResponse(
    val deviceID: Int,
    val deviceName: String,
    val roomID: Int,
    val deviceState: String,
    val remark: String,
    val devicePartID: Int,
    val devicePartName: String,
) : Response
