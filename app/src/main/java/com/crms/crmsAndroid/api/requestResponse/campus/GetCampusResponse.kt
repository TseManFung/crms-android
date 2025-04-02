package com.crms.crmsAndroid.api.requestResponse.campus

import com.crms.crmsAndroid.api.requestResponse.Response

data class GetCampusResponse(
    val c: List<Campus>

) : Response {
    data class Campus(
        val campusId: Int? = null,
        val campusName: String? = null,
        val campusShortName: String? = null,
    )
}
