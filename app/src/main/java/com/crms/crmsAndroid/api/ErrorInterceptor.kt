package com.crms.crmsAndroid.api

import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.crms.crmsAndroid.CAMS
import com.crms.crmsAndroid.api.exception.ErrorCodeException
import com.crms.crmsAndroid.api.requestResponse.ErrorResponse
import com.crms.crmsAndroid.data.LoginRepository
import com.crms.crmsAndroid.utils.DialogUtils
import com.crms.crmsAndroid.utils.ErrorCode
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import org.json.JSONObject
import java.io.IOException

class ErrorInterceptor(private val loginRepository: LoginRepository) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        try {
            // 使用 peekBody 复制响应内容（不关闭原始流）
            val responseBodyCopy = response.peekBody(Long.MAX_VALUE)
            val (errorCode, description) = parseErrorResponse(responseBodyCopy.string())

            return errorCode?.let {
                handleBusinessError(chain, request, it, description ?: "")
            } ?: response
        } catch (e: Exception) {
            Log.e("ErrorInterceptor", "Error parsing response: ${e.message}")
            return buildErrorResponse(response, "Error: ${e.message ?: "Unknown error"}")
        }
    }
    private fun buildErrorResponse(originalResponse: Response, message: String): Response {
        return originalResponse.newBuilder()
            .code(500)
            .body(message.toResponseBody("application/json".toMediaType()))
            .build()
    }
    private fun parseErrorResponse(responseBody: String): Pair<String?, String?> {
        return try {
            val jsonObject = Gson().fromJson(responseBody, ErrorResponse::class.java)
            jsonObject.errorCode to jsonObject.description
        } catch (e: JsonSyntaxException) {
            null to null
        }
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
    private fun handleBusinessError(
        chain: Interceptor.Chain,
        currentRequest: Request,
        errorCode: String,
        description: String
    ): Response {
        return when (ErrorCode.toErrorCode(errorCode)) {
            ErrorCode.E04 -> handleTokenExpired(chain, currentRequest)
            // more Error handle in here

            else -> {
                // 构建错误 JSON
                val errorResponse = ErrorResponse(errorCode, description)
                val errorJson = Gson().toJson(errorResponse)

                // 使用现有 response 创建 Builder
                chain.proceed(currentRequest).newBuilder()
                    .code(400)
                    .body(
                        errorJson.toResponseBody("application/json".toMediaType())
                    )
                    .build()
            }
        }
    }
}