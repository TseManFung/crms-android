package com.crms.crmsAndroid.api

import com.crms.crmsAndroid.api.requestResponse.Response

data class ErrorResponse(
    val errorCode: String,
    val description: String
) : Response
