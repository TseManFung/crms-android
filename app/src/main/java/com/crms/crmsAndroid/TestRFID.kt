package com.crms.crmsAndroid

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Spinner
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.crms.crmsAndroid.scanner.rfidScanner
import com.rscja.deviceapi.entity.UHFTAGInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TestRFID : ComponentActivity() {
    private lateinit var lvMain: ListView
    private lateinit var btnScan: Button
    private lateinit var btnStop: Button
    private lateinit var spnBank: Spinner
    private val items = mutableListOf<String>() // 用於存放 ListView 的數據
    private lateinit var listAdapter: ArrayAdapter<String>
    var objRfidScanner: rfidScanner = rfidScanner()

    init {
        System.loadLibrary("IGLBarDecoder") // 加載 libIGLBarDecoder.so
        System.loadLibrary("IGLImageAE")    // 加載 libIGLImageAE.so
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_testrfid)

        lvMain = findViewById(R.id.lvMain)
        btnScan = findViewById(R.id.btnScan)
        btnStop = findViewById(R.id.btnStop)
        spnBank = findViewById(R.id.spnBank)

        // 設定 ListView 的適配器
        listAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        lvMain.adapter = listAdapter

        // 設定按鈕點擊事件
        btnScan.setOnClickListener {
            handleBtnScanClick()
        }
        btnStop.setOnClickListener {
            objRfidScanner.stopReadTagLoop()
        }

        // 設定 Spinner 的適配器
        ArrayAdapter.createFromResource(
            this,
            R.array.bank_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spnBank.adapter = adapter
        }

        appendTextToList("RFID 版本: ${objRfidScanner.getVersion()}") // 獲取 RFID 版本

    }

    override fun onDestroy() {
        super.onDestroy()
        if (objRfidScanner != null) {
            objRfidScanner.free()
        }
    }

    // 處理 btnScan 按鈕的點擊事件
    private fun handleBtnScanClick() {
        var rfidData = "---"

        try {
            // read single tag
//            objRfidScanner = rfidScanner()
//            val scanner = objRfidScanner.scanner;
//            val tag = scanner.inventorySingleTag()
//            tag.reserved = objRfidScanner.readTag(0)
//            tag.tid = objRfidScanner.readTag(2)
//            tag.user = objRfidScanner.readTag(3)
//            if (tag != null) {
//                rfidData =
//                    """ |EPC: ${tag.epc} |TID: ${tag.tid} |RSSI: ${tag.rssi} |Antenna: ${tag.ant} |Index: ${tag.index} |PC: ${tag.pc} |Remain: ${tag.remain} |Reserved: ${tag.reserved} |User: ${tag.user} """.trimMargin()
//            } else {
//                rfidData = "無法掃描到 RFID 訊號"
//            }

            // read loop using handler and thread
//            val handler1: Handler = object : Handler() {
//                override fun handleMessage(msg: Message) {
//                    appendTextToList(msg.obj.toString())
//                }
//            }
//            objRfidScanner.readTagLoop()
//            readLoopForTest(objRfidScanner, handler1).start()


            // read loop using coroutine without lambda
//            objRfidScanner.readTagLoop()
//            lifecycleScope.launch {
//                appendTextToList("Start reading RFID tags...")
//                while (objRfidScanner.loopFlag) {
//                    val tag = objRfidScanner.scanner.readTagFromBuffer()
//                    if (tag != null) {
//                        val message = """ |EPC: ${tag.epc} |TID: ${tag.tid} |RSSI: ${tag.rssi} |Antenna: ${tag.ant} |Index: ${tag.index} |PC: ${tag.pc} |Remain: ${tag.remain} |Reserved: ${tag.reserved} |User: ${tag.user} """.trimMargin()
//                        appendTextToList(message)
//                    } else {
//                        // 如果没有标签，稍作等待
//                        delay(100)
//                    }
//                }
//            }


            // read loop using coroutine with lambda
            // only need to write the code for handling the tag
            objRfidScanner.readTagLoop(lifecycleScope) { tag ->
                val message =
                    """ |EPC: ${tag.epc} |TID: ${tag.tid} |RSSI: ${tag.rssi} |Antenna: ${tag.ant} |Index: ${tag.index} |PC: ${tag.pc} |Remain: ${tag.remain} |Reserved: ${tag.reserved} |User: ${tag.user} """.trimMargin()
                appendTextToList(message)
                // TODO: API, get ___, post ___.
                // TODO: API, get 借出資料, post roomRfid.
            }


        } catch (e: Exception) {
            rfidData = e.message.toString() // 捕獲異常
        } finally {
//            if (objRfidScanner != null) {
//                objRfidScanner.free()
//            }
        }

        val newText = "end line: $rfidData"
        appendTextToList(newText)

    }

    // 將文本追加到 ListView
    fun appendTextToList(text: String) {
        items.add(text) // 將新文本添加到列表中
        listAdapter.notifyDataSetChanged() // 通知適配器數據已更新
    }

    // read loop using handler and thread
//    open class readLoopForTest(private val objRfidScanner: rfidScanner, private val handler: Handler) : Thread() {
//        override fun run() {
//            val msg = handler.obtainMessage()
//            msg.obj = "start"
//            handler.sendMessage(msg)
//            while (objRfidScanner.loopFlag){
//                val tag = objRfidScanner.scanner.readTagFromBuffer()
//                if (tag != null) {
//                    val msg = handler.obtainMessage()
//                    msg.obj = """ |EPC: ${tag.epc} |TID: ${tag.tid} |RSSI: ${tag.rssi} |Antenna: ${tag.ant} |Index: ${tag.index} |PC: ${tag.pc} |Remain: ${tag.remain} |Reserved: ${tag.reserved} |User: ${tag.user} """.trimMargin()
//                    handler.sendMessage(msg)
//                }else{
//                    try {
//                        Thread.sleep(100)
//                    }catch (e: InterruptedException){
//                        e.printStackTrace()
//                    }
//                }
//            }
//        }
//    }

//    this is a sample code for readLoop in real project using handler and thread
//    class readLoop(private val objRfidScanner: rfidScanner): Thread() {
//        init {
//            this.start()
//        }
//        override fun run(){
//            while (objRfidScanner.loopFlag){
//                val tag = objRfidScanner.scanner.readTagFromBuffer()
//                // do something with tag
//            }
//        }
//    }

}

fun byteArrayToHexString(byteArray: ByteArray): String {
    return byteArray.joinToString("") { String.format("%02X", it) }
}