package com.crms.crmsAndroid.api.requestResponse.login

import com.crms.crmsAndroid.api.requestResponse.Response

data class RenewTokenResponse(
    val token: String
) : Response
