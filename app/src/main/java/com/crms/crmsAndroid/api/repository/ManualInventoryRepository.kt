package com.crms.crmsAndroid.api.repository

import com.crms.crmsAndroid.api.RetrofitClient
import com.crms.crmsAndroid.api.requestResponse.item.ManualInventoryRequest
import com.crms.crmsAndroid.api.requestResponse.item.ManualInventoryResponse


class ManualInventoryRepository {
    suspend fun manualInventory(
        token: String,
        manualInventoryLists: List<String>,
        roomID: Int
    ): Result<ManualInventoryResponse> {
        return try {
            val request = ManualInventoryRequest(
                token = token,
                manualInventoryLists = manualInventoryLists,
                roomID = roomID
            )
            val response = RetrofitClient.instance.manualinventory(request)

            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(
                    Exception(
                        "API error: ${response.code()} ${
                            response.errorBody()?.string()
                        }"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}