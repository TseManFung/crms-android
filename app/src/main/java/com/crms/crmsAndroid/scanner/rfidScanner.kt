package com.crms.crmsAndroid.scanner

import com.rscja.deviceapi.RFIDWithUHFUART

class rfidScanner {
    private lateinit var scanner: RFIDWithUHFUART;

    init {
        this.scanner = RFIDWithUHFUART.getInstance();
        val isInit =this. scanner.init();
        if (!isInit) {
            throw Exception("Failed to initialize RFID scanner");
        }
    }

    fun free(): Boolean {
        return this.scanner.free();
    }

    fun getScanner(): RFIDWithUHFUART {
        return this.scanner;
    }


}