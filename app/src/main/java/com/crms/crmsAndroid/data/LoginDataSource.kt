package com.crms.crmsAndroid.data


import com.crms.crmsAndroid.api.RetrofitClient
import com.crms.crmsAndroid.api.requestResponse.Room.GetRoomRequest
import com.crms.crmsAndroid.api.requestResponse.login.LoginByPwRequest
import com.crms.crmsAndroid.api.requestResponse.login.LoginResponse
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

    fun logout() {
        // TODO: revoke authentication
        // TODO: API, get /, post /.
    }
}