package com.crms.crmsAndroid.api

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class BaseUrlSwitcherInterceptor(private val baseUrls: List<String>) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        var lastException: IOException? = null
        var lastResponse: Response? = null

        for (baseUrl in baseUrls) {
            try {
                val httpUrl = baseUrl.toHttpUrlOrNull() ?: continue
                val newUrl = originalRequest.url.newBuilder()
                    .scheme(httpUrl.scheme)
                    .host(httpUrl.host)
                    .port(httpUrl.port)
                    .build()

                val newRequest = originalRequest.newBuilder()
                    .url(newUrl)
                    .build()

                val response = chain.proceed(newRequest)

                if (response.isSuccessful) {
                    return response
                } else {
                    if (response.code in 500..599) {
                        lastResponse = response
                        response.close()
                        continue
                    }
                    return response
                }
            } catch (e: IOException) {
                lastException = e
            }
        }

        lastException?.let { throw it }
        lastResponse?.let { return it }
        throw IOException("All base URLs failed")
    }
}