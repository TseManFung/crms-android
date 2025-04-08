package com.crms.crmsAndroid.api.requestResponse

data class ErrorResponse(
    val errorCode: String,
    val description: String
) : Response
