package com.crms.crmsAndroid.api

import android.util.Log
import com.crms.crmsAndroid.CAMS
import com.crms.crmsAndroid.data.LoginRepository
import com.crms.crmsAndroid.utils.DialogUtils
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import java.io.IOException

class ErrorInterceptor(private val loginRepository: LoginRepository) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: return response
            try {
                val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                if (errorResponse.errorCode == "E04") {
                    return handleTokenExpired(chain, request)
                } else {
                    // 其他错误码处理
                    showErrorDialog("API 错误: ${errorResponse.errorCode} - ${errorResponse.description}")
                }
            } catch (e: Exception) {
                Log.e("ErrorInterceptor", "Error parsing error response", e)
                showErrorDialog("请求处理失败: ${e.localizedMessage}")
            }
        }
        return response
    }

    private fun handleTokenExpired(chain: Interceptor.Chain, originalRequest: Request): Response {
        synchronized(this) {
            val newToken = runBlocking {
                try {
                    loginRepository.renewToken()
                    loginRepository.user?.token
                } catch (e: Exception) {
                    Log.e("TokenRefresh", "Token 刷新失败", e)
                    null
                }
            }

            return if (newToken != null) {
                val newRequest = modifyRequestWithNewToken(originalRequest, newToken)
                chain.proceed(newRequest)
            } else {
                showErrorDialog("会话已过期，请重新登录")
                throw IOException("Token refresh failed")
            }
        }
    }

    private fun modifyRequestWithNewToken(request: Request, newToken: String): Request {
        val requestBody = request.body ?: return request
        val buffer = Buffer()
        return try {
            requestBody.writeTo(buffer)
            val originalJson = buffer.readUtf8()
            val jsonObject = Gson().fromJson(originalJson, JsonObject::class.java)

            when (request.url.encodedPath) {
                "/renewtoken" -> jsonObject.addProperty("refreshToken", newToken)
                else -> jsonObject.addProperty("token", newToken)
            }

            request.newBuilder()
                .method(request.method, jsonObject.toString().toRequestBody(requestBody.contentType()))
                .build()
        } catch (e: Exception) {
            Log.e("RequestModify", "Failed to modify request", e)
            request
        }
    }

    private fun showErrorDialog(message: String) {
        // 使用 Application Context 显示弹窗
        DialogUtils.showErrorDialog(
            context = CAMS.getAppContext(),
            message = message
        )
    }
}