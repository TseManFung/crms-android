package com.crms.crmsAndroid.api

import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.crms.crmsAndroid.CAMS
import com.crms.crmsAndroid.api.requestResponse.ErrorResponse
import com.crms.crmsAndroid.data.LoginRepository
import com.crms.crmsAndroid.utils.DialogUtils
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import okio.BufferedSource
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
                }
            } catch (e: Exception) {
                Log.e("ErrorInterceptor", "Error parsing error response", e)
                showErrorDialog("Error parsing error response")
                return response
            }
        }
        return response
    }

    private fun handleTokenExpired(chain: Interceptor.Chain, originalRequest: Request): Response {
        synchronized(this) {
            // 刷新 Token
            val newToken = runBlocking {
                loginRepository.renewToken()
                loginRepository.user?.token
            }

            return if (newToken != null) {
                // 修改请求体中的 Token
                val newRequest = modifyRequestWithNewToken(originalRequest, newToken)
                chain.proceed(newRequest)
            } else {
                throw IOException("Token 刷新失败")
            }
        }
    }

    private fun modifyRequestWithNewToken(request: Request, newToken: String): Request {
        val requestBody = request.body ?: return request
        val buffer = Buffer()
        requestBody.writeTo(buffer)
        val originalJson = buffer.readUtf8()

        // 解析并替换 Token 字段
        return try {
            val jsonObject = Gson().fromJson(originalJson, JsonObject::class.java)
            when (request.url.encodedPath) {
                "/renewtoken" -> jsonObject.addProperty("refreshToken", newToken)
                else -> jsonObject.addProperty("token", newToken)
            }
            val newBody = RequestBody.create(requestBody.contentType(), jsonObject.toString())
            request.newBuilder()
                .method(request.method, newBody)
                .build()
        } catch (e: Exception) {
            request // 解析失败时返回原始请求
        }
    }
    private fun showErrorDialog(message: String) {
        DialogUtils.showErrorDialog(
            context = CAMS.getAppContext(),
            message = message
        )
    }
}