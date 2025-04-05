package com.crms.crmsAndroid.api.requestResponse.login

import com.crms.crmsAndroid.api.requestResponse.Request

data class RenewTokenRequest(
    val refreshToken: String
) : Request
