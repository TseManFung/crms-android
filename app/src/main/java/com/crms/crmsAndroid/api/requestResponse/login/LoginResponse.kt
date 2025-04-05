package com.crms.crmsAndroid.api.requestResponse.login

import com.crms.crmsAndroid.api.requestResponse.Response

data class LoginResponse(
    val token: String,
    val refreshToken: String,
    val accessLevel: Int,
    val accessPage: Int,
    val firstName: String,
    val lastName: String,
) : Response
