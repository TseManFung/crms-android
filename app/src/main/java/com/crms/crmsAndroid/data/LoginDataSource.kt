package com.crms.crmsAndroid.data


import com.crms.crmsAndroid.api.RetrofitClient
import com.crms.crmsAndroid.api.exception.ErrorCodeException
import com.crms.crmsAndroid.api.requestResponse.Room.GetRoomRequest
import com.crms.crmsAndroid.api.requestResponse.login.LoginByPwRequest
import com.crms.crmsAndroid.api.requestResponse.login.LoginResponse
import com.crms.crmsAndroid.api.requestResponse.login.RenewTokenRequest
import com.crms.crmsAndroid.api.requestResponse.login.RenewTokenResponse
import com.crms.crmsAndroid.utils.ErrorCode
import java.io.IOException
import java.util.UUID

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class LoginDataSource {

    // LoginDataSource.kt
    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return try {
            val request = LoginByPwRequest(username, password)
            val response = RetrofitClient.instance.login(request)

            // 检查 HTTP 状态码
            if (!response.isSuccessful) {
                return Result.Error(IOException("HTTP Error: ${response.code()}"))
            }

            val responseBody = response.body()
            // 获取响应体
            if (responseBody == null) {
                return Result.Error(IOException("emtry response body"))
            }
            // 验证必要字段
            if (responseBody.token.isBlank() || responseBody.refreshToken.isBlank()) {
                return Result.Error(IOException("invalid response token"))
            }


            Result.Success(responseBody)
        } catch (e: Throwable) {
            Result.Error(IOException("login failed", e))
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