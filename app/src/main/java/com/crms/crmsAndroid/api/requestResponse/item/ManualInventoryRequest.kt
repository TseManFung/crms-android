package com.crms.crmsAndroid.api.requestResponse.item

import com.crms.crmsAndroid.api.requestResponse.Request


data class ManualInventoryRequest(
    val token: String,
    val manualInventoryLists: List<String>,
    val roomID: Int
) : Request