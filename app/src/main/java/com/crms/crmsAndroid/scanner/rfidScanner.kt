package com.crms.crmsAndroid.scanner

import android.os.Looper
import com.rscja.deviceapi.RFIDWithUHFUART
import com.rscja.deviceapi.interfaces.ConnectionStatus

class rfidScanner {
    lateinit var scanner: RFIDWithUHFUART
        private set
    private var password: String = "12345678"
    private val defaultPassword = "00000000"
    private var filterBank: Int? = null
    private var filterData: String? = null
    var loopFlag = false

    init {
        this.scanner = RFIDWithUHFUART.getInstance();
        val isInit = this.scanner.init();
        if (!isInit) {
            throw Exception("Failed to initialize RFID scanner");
        }
//        this.scanner.setEPCAndTIDUserModeEx(2, 0, 6, 0, 2)
    }

    fun setMode(): Boolean {
        return this.scanner.setEPCAndTIDUserMode(0, 2)
    }

    fun free(): Boolean {
        return this.scanner.free();
    }

    fun setPassword(password: String) {
        this.password = password
    }

    fun getConnectStatus(): ConnectionStatus {
        return this.scanner.getConnectStatus()
    }

    fun getVersion(): String {
        return this.scanner.getVersion()
    }

    private fun makePtrAndCnt(bank: Int): Pair<Int, Int> {
        val ptr: Int
        val cnt: Int

        when (bank) {
            0 -> {
                // Reserved
                ptr = 0
                cnt = 4
            }

            1 -> {
                // EPC
                ptr = 2
                cnt = 6
            }

            2 -> {
                // TID
                ptr = 0
                cnt = 6
            }

            3 -> {
                // User
                ptr = 0
                cnt = 2
            }

            else -> {
                throw IllegalArgumentException("Invalid bank value")
            }
        }

        return Pair(ptr, cnt)
    }

    private fun bankErrorMsg(bank: Int): String {
        when (bank) {
            0 -> return "Reserved Bank"
            1 -> return "EPC Bank"
            2 -> return "TID Bank"
            3 -> return "User Bank"
            else -> return "Bank $bank"
        }
    }

    fun readTag(readBank: Int): String {
        val (ptr, cnt) = makePtrAndCnt(readBank)

        var TagData = this.scanner.readData(password, readBank, ptr, cnt)
        if (TagData != null) {
            return TagData
        } else {
            TagData = this.scanner.readData(defaultPassword, readBank, ptr, cnt)
            if (TagData != null) {
                return TagData
            } else {
                throw Exception("Failed to read tag from ${bankErrorMsg(readBank)}")
            }
        }
    }

    fun setFilter(filterBank: Int?, filterData: String?) {
        this.filterBank = filterBank
        this.filterData = filterData
    }

    fun readTagWithFilter(readBank: Int, filterBank: Int, filterData: String): String {
        if (filterBank == 0) {
            throw IllegalArgumentException("Invalid filter bank value, filter bank cannot be Reserved Bank")
        }
        this.setFilter(filterBank, filterData)
        return this.readTagWithFilter(readBank)
    }

    fun readTagWithFilter(readBank: Int): String {
        if (this.filterBank == null || this.filterData == null) {
            throw IllegalArgumentException("Filter not set")
        }
        val (readPtr, readCnt) = makePtrAndCnt(readBank)
        val (filterPtr, filterCnt) = makePtrAndCnt(filterBank!!)
        var TagData = this.scanner.readData(
            password,
            filterBank!!,
            filterPtr,
            filterCnt,
            filterData!!,
            readBank,
            readPtr,
            readCnt
        )
        if (TagData != null) {
            return TagData
        } else {
            TagData = this.scanner.readData(
                defaultPassword,
                filterBank!!,
                filterPtr,
                filterCnt,
                filterData!!,
                readBank,
                readPtr,
                readCnt
            )
            if (TagData != null) {
                return TagData
            } else {
                throw Exception("Failed to read tag from ${bankErrorMsg(readBank)}")
            }
        }
    }

    fun readTagLoop() {
        // only can read EPC / Tid
        this.scanner.startInventoryTag()
        this.loopFlag = true
    }

    fun stopReadTagLoop() {
        this.loopFlag = false
        this.scanner.stopInventory()
    }

//    pri fun lockTag{
//        this.scanner.lockMem
//    }
//
//

//
//    fun readTagLoop{
//        this.scanner.readData
//    }
//
//    fun readTagLoop{
//        //specific tag
//        this.scanner.readData
//    }
//
//    fun stopReadTagLoop{
//        this.scanner.stopReadData
//    }
//
//    fun writeTagWithFilter{
//        //specific tag
//        this.scanner.writeData
//    }


}