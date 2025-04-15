package com.crms.crmsAndroid.api.requestResponse.item

import com.crms.crmsAndroid.api.requestResponse.Response

import java.math.BigDecimal

data class GetItemResponse(
    val device: List<Devices>
) : Response {
    data class Devices(
        val deviceID: Int? = 0,
        val deviceName: String? = null,
        val price: BigDecimal? = null,
        val orderDate: String? = null,
        val arriveDate: String? = null,
        val maintenanceDate: String? = null,
        val roomID: Int? = null,
        val state: Char? = null,
        val remark: String? = null,
        val docs: List<DeviceDoc>,
        val partID: List<DevicePartID>,
        val deviceRFID: List<DeviceRFID>,
    )

    data class DeviceDoc(
        val deviceID: Int? = null,
        val docPath: String? = null
    )

    data class DevicePartID(
        val deviceID: Int? = null,
        val devicePartID: Int? = null,
        val devicePartName: String? = null
    )

    data class DeviceRFID(
        val deviceID: Int? = null,
        val devicePartID: Int? = null,
        val rfid: String? = null
    )
}