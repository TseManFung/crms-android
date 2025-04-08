package com.crms.crmsAndroid.api.requestResponse.item

import com.crms.crmsAndroid.api.requestResponse.Response


data class updateLocationByRFIDResponse(
    val updateLists: List<updateList>
) : Response {
    data class updateList(
        val deviceName: String,
        val failData: FailData? = null,
        val successData: SuccessData? = null
    ) : Response

    data class SuccessData(
        val something: String
    ) : Response

    data class FailData(
        val something: String,
        val reason:String
    ) : Response
}