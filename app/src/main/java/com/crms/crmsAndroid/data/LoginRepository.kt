package com.crms.crmsAndroid.data

import com.crms.crmsAndroid.api.requestResponse.login.LoginResponse
import java.io.IOException

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */

class LoginRepository(val dataSource: LoginDataSource) {

    // in-memory cache of the loggedInUser object
    var user: LoginResponse? = null
        private set

    val isLoggedIn: Boolean
        get() = user != null

    init {
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
        user = null
    }

    fun logout() {
        user = null
    }

    suspend fun renewToken() {
        val refreshToken = user?.refreshToken ?: throw IllegalStateException("No refresh token")
        val result = dataSource.renewToken(refreshToken)

        if (result is Result.Success) {
            user?.token = result.data.token
        } else {
            logout() // 刷新失敗時登出
            throw IOException("Token renewal failed")
        }
    }


    suspend fun login(username: String, password: String): Result<LoginResponse> {

        // handle login
        val result = dataSource.login(removeDomain(username), password)

        if (result is Result.Success) {
            setLoggedInUser(result.data)
        }

        return result
    }

    private fun removeDomain(email: String): String {
        return if (email.contains("@")) {
            email.split("@")[0] // Return the part before the '@'
        } else {
            email // Return the original string if no domain is present
        }
    }

    private fun setLoggedInUser(loggedInUser: LoginResponse) {
        this.user = loggedInUser
        // If user credentials will be cached in local storage, it is recommended it be encrypted
        // @see https://developer.android.com/training/articles/keystore
    }
}