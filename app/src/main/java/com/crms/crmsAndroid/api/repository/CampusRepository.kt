package com.crms.crmsAndroid.api.repository

import com.crms.crmsAndroid.api.RetrofitClient
import com.crms.crmsAndroid.api.requestResponse.campus.GetCampusRequest
import com.crms.crmsAndroid.api.requestResponse.campus.GetCampusResponse

class CampusRepository {
    suspend fun getCampuses(token: String): Result<List<GetCampusResponse.Campus>> {
        return try {
            val request = GetCampusRequest(token = token)
            val response = RetrofitClient.instance.getCampus(request)

            if (response.isSuccessful) {
                Result.success(response.body()?.c ?: emptyList())
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun AddCampuses(token: String) {

    }

    suspend fun EditCampuses(token: String) {

    }

    suspend fun DeleteCampuses(token: String) {

    }

}