package com.crms.crmsAndroid

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.activity.ComponentActivity
import com.crms.crmsAndroid.scanner.rfidScanner
import com.rscja.deviceapi.RFIDWithUHFUART
import com.rscja.deviceapi.entity.*
import com.rscja.deviceapi.interfaces.IUHF

class TestRFID : ComponentActivity() {
    private lateinit var lvMain: ListView
    private lateinit var btnScan: Button
    private val items = mutableListOf<String>() // 用於存放 ListView 的數據
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_testrfid)

        lvMain = findViewById(R.id.lvMain)
        btnScan = findViewById(R.id.btnScan)

        // 設定 ListView 的適配器
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        lvMain.adapter = adapter

        // 設定按鈕點擊事件
        btnScan.setOnClickListener {
            handleBtnScanClick(adapter)
        }
    }

    // 處理 btnScan 按鈕的點擊事件
    private fun handleBtnScanClick(adapter: ArrayAdapter<String>) {
        var rfidData = "無法掃描到 RFID 訊號"
        var sc:rfidScanner? = null
        try {
            sc = rfidScanner()
            val scanner = sc.getScanner()
            val tag = scanner.inventorySingleTag()
            if (tag != null) {
                rfidData = """ |EPC: ${tag.epc} |TID: ${tag.tid} |RSSI: ${tag.rssi} |Antenna: ${tag.ant} |Index: ${tag.index} |PC: ${tag.pc} |Remain: ${tag.remain} |Reserved: ${tag.reserved} |User: ${tag.user} """.trimMargin()

            }else{
                rfidData = "無法掃描到 RFID 訊號"
            }
        } catch (e: Exception) {
            rfidData = e.message.toString() // 捕獲異常
        }finally {
            if(sc != null){
                sc.free()
            }
        }

        val newText = "新掃描項目 $rfidData"
        appendTextToList(newText, adapter)

    }

    // 將文本追加到 ListView
    private fun appendTextToList(text: String, adapter: ArrayAdapter<String>) {
        items.add(text) // 將新文本添加到列表中
        adapter.notifyDataSetChanged() // 通知適配器數據已更新
    }

}
fun byteArrayToHexString(byteArray: ByteArray): String {
    return byteArray.joinToString("") { String.format("%02X", it) }
}