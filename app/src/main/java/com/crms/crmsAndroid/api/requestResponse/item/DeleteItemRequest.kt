package com.crms.crmsAndroid.api.requestResponse.item

import com.crms.crmsAndroid.api.requestResponse.Request

data class DeleteItemRequest(
    val token: String,
    val deviceID: Int
) : Request