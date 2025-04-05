package com.crms.crmsAndroid.data


import com.crms.crmsAndroid.api.RetrofitClient
import com.crms.crmsAndroid.api.requestResponse.Room.GetRoomRequest
import com.crms.crmsAndroid.api.requestResponse.login.LoginByPwRequest
import com.crms.crmsAndroid.api.requestResponse.login.LoginResponse
import com.crms.crmsAndroid.api.requestResponse.login.RenewTokenRequest
import com.crms.crmsAndroid.api.requestResponse.login.RenewTokenResponse
import java.io.IOException
import java.util.UUID

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class LoginDataSource {

    suspend fun login(username: String, password: String): Result<LoginResponse> {
        try {
            // TODO: API, get token + display name, post username + password.
            val request = LoginByPwRequest(username, password)
            val response = RetrofitClient.instance.login(request)

            val user = response.body()
                ?: return Result.Error(IOException("Error logging in"))

            return Result.Success(user)
        } catch (e: Throwable) {
            return Result.Error(IOException("Error logging in", e))
        }
    }

    suspend fun renewToken(refreshToken: String): Result<RenewTokenResponse> {
        try {
            val request = RenewTokenRequest(refreshToken)
            val response = RetrofitClient.instance.renewToken(request)

            val user = response.body()
                ?: return Result.Error(IOException("Error renewing token"))

            return Result.Success(user)
        } catch (e: Throwable) {
            return Result.Error(IOException("Error renewing token", e))
        }
    }
}