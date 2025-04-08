package com.crms.crmsAndroid.api.requestResponse.item

import com.crms.crmsAndroid.api.requestResponse.Response

data class ManualInventoryResponse(
    val manualInventoryLists: List<InventoryItem>
) : Response {
    data class InventoryItem(
        val deviceName: String,
        val rfid: String,
        val preState: Char,
        val afterState: Char
    )
}
