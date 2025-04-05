package com.crms.crmsAndroid.api.requestResponse.login

import com.crms.crmsAndroid.api.requestResponse.Request

data class LoginByPwRequest(
    val CNA: String,
    val password: String
) : Request
