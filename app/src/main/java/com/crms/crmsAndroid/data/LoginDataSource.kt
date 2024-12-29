package com.crms.crmsAndroid.data


import com.crms.crmsAndroid.api.requestResponse.LoggedInUser
import java.io.IOException

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class LoginDataSource {

    fun login(username: String, password: String): Result<LoggedInUser> {
        try {
            // TODO: API, get token + display name, post username + password.

            val fakeUser = LoggedInUser(java.util.UUID.randomUUID().toString(), "Andy")
            return Result.Success(fakeUser)
        } catch (e: Throwable) {
            return Result.Error(IOException("Error logging in", e))
        }
    }

    fun logout() {
        // TODO: revoke authentication
        // TODO: API, get /, post /.
    }
}