package com.crms.crmsAndroid.api.requestResponse

data class LoginRequest(val username: String, val password: String)
data class LoggedInUser(val token: String, val displayName: String)
