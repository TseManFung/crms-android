package com.crms.crmsAndroid.api.requestResponse.item

import com.crms.crmsAndroid.api.requestResponse.Request

data class GetItemRequest(
    val token: String,
    val roomID: Int,
    val stateList: List<String>? = null
) : Request