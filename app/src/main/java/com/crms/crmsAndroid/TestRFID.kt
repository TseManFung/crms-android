package com.crms.crmsAndroid

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.activity.ComponentActivity
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
        try {
            val RFIDscanner: RFIDWithUHFUART = RFIDWithUHFUART.getInstance()
            if (RFIDscanner == null) {
                rfidData = "無法取得 RFIDscanner 物件"
            } else {
                rfidData = "取得 RFIDscanner 物件"
            }
            appendTextToList(rfidData, adapter)

            val b:Boolean = RFIDscanner.init()
            rfidData = "初始化 RFIDscanner 物件: $b"
            appendTextToList(rfidData, adapter)
            var Bank = 2
            when (Bank) {
                0 -> Bank = IUHF.Bank_RESERVED
                1 -> Bank = IUHF.Bank_EPC
                2 -> Bank = IUHF.Bank_TID
                3 -> Bank = IUHF.Bank_USER
            }
            rfidData = RFIDscanner.readData("00000000",Bank,0,6)
            appendTextToList(rfidData, adapter)
        } catch (e: Exception) {
            rfidData = e.message.toString() // 捕獲異常
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