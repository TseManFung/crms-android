package com.crms.crmsAndroid.scanner

import androidx.fragment.app.Fragment
import com.rscja.deviceapi.RFIDWithUHFUART
import com.rscja.deviceapi.entity.UHFTAGInfo
import com.rscja.deviceapi.interfaces.ConnectionStatus
import com.rscja.deviceapi.interfaces.IUHF.Bank_EPC
import com.rscja.deviceapi.interfaces.IUHF.Bank_RESERVED
import com.rscja.deviceapi.interfaces.IUHF.Bank_TID
import com.rscja.deviceapi.interfaces.IUHF.Bank_USER
import com.rscja.deviceapi.interfaces.IUHF.LockBank_ACCESS
import com.rscja.deviceapi.interfaces.IUHF.LockBank_EPC
import com.rscja.deviceapi.interfaces.IUHF.LockBank_KILL
import com.rscja.deviceapi.interfaces.IUHF.LockBank_TID
import com.rscja.deviceapi.interfaces.IUHF.LockBank_USER
import com.rscja.deviceapi.interfaces.IUHF.LockMode_LOCK
import com.rscja.deviceapi.interfaces.IUHFLocationCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class rfidScanner {
    lateinit var scanner: RFIDWithUHFUART
        private set
    private var password: String = "12345678"
    private val defaultPassword = "00000000"
    private var filterBank: Int? = null
    private var filterData: String? = null
    private val lockCode: String
    var loopFlag = false
        private set

    init {
        this.scanner = RFIDWithUHFUART.getInstance()
        val isInit = this.scanner.init()
        if (!isInit) {
            throw Exception("Failed to initialize RFID scanner")
        }
        setMode()
        val lockBank: ArrayList<Int> =
            arrayListOf(LockBank_EPC, LockBank_TID, LockBank_USER, LockBank_ACCESS, LockBank_KILL)
        lockCode = this.scanner.generateLockCode(lockBank, LockMode_LOCK)
    }

    /**
     * Sets the mode for the RFID scanner.
     * @return true if the mode was set successfully, false otherwise.
     */
    fun setMode(): Boolean {
        return this.scanner.setEPCAndTIDUserMode(0, 2)
    }

    /**
     * Releases the resources used by the RFID scanner.
     * @return true if the resources were released successfully, false otherwise.
     */
    fun free(): Boolean {
        return this.scanner.free()
    }

    /**
     * Sets the password for accessing the RFID scanner.
     * @param password New password to be set.
     */
    fun setPassword(password: String) {
        if (password.length != 8) {
            throw IllegalArgumentException("Password must be 8 characters long")
        }
        if (!password.matches(Regex("[0-9A-Fa-f]+"))) {
            throw IllegalArgumentException("Password must be a hexadecimal string")
        }
        this.password = password
    }

    /**
     * Gets the connection status of the RFID scanner.
     * @return the current connection status.
     */
    fun getConnectStatus(): ConnectionStatus {
        return this.scanner.connectStatus
    }

    /**
     * Gets the version of the RFID scanner.
     * @return the version as a String.
     */
    fun getVersion(): String {
        return this.scanner.version
    }

    private fun makePtrAndCnt(bank: Int): Pair<Int, Int> {
        val ptr: Int
        val cnt: Int

        when (bank) {

            Bank_RESERVED -> {
                // Reserved
                ptr = 0
                cnt = 4
            }

            Bank_EPC -> {
                // EPC
                ptr = 2
                cnt = 6
            }

            Bank_TID -> {
                // TID
                ptr = 0
                cnt = 6
            }

            Bank_USER -> {
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

    /**
     * Reads data from the specified memory bank.
     * @param readBank the memory bank to read from.
     * @return the data read from the tag.
     * @throws Exception if reading fails.
     */
    fun readTagData(readBank: Int): String {
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

    /**
     * Reads a single tag.
     * @return the tag information.
     */
    fun readSingleTag(): UHFTAGInfo {
        return this.scanner.inventorySingleTag()
    }

    /**
     * Sets a filter for the RFID scanner. This function allows the user to specify a filter bank and filter data. If either parameter is null, filter will be cancel.
     *
     * @param filterBank the memory bank to apply the filter to, or null to clear the filter.
     * @param filterData the data to filter, or null to clear the filter.
     * @return true if the filter was set successfully, false otherwise.
     */
    fun setFilter(filterBank: Int?, filterData: String?): Boolean {
        if (filterBank == 0) {
            throw IllegalArgumentException("Invalid filter bank value, filter bank cannot be Reserved Bank")
        }
        this.filterBank = filterBank
        this.filterData = filterData
        if (filterBank == null || filterData == null) {
            return this.scanner.setFilter(1, 0, 0, "")
        } else {
            if (!password.matches(Regex("[0-9A-Fa-f]+"))) {
                throw IllegalArgumentException("Filter data must be a hexadecimal string")
            }
            val (filterPtr, filterCnt) = makePtrAndCnt(filterBank)
            return this.scanner.setFilter(filterBank, filterPtr, filterCnt * 16, filterData)
        }
    }

    /**
     * Reads a tag with an applied filter.
     * @param readBank the memory bank to read from.
     * @param filterBank the memory bank to filter on.
     * @param filterData the data to filter.
     * @return the data read from the tag.
     * @throws Exception if reading fails.
     */
    fun readTagDataWithFilter(readBank: Int, filterBank: Int, filterData: String): String {
        if (filterBank == 0) {
            throw IllegalArgumentException("Invalid filter bank value, filter bank cannot be Reserved Bank")
        }
        this.setFilter(filterBank, filterData)
        return this.readTagDataWithFilter(readBank)
    }

    /**
     * Reads a tag with the current applied filter.
     * @param readBank the memory bank to read from.
     * @return the data read from the tag.
     * @throws IllegalArgumentException if filter is not set.
     */
    fun readTagDataWithFilter(readBank: Int): String {
        if (this.filterBank == null || this.filterData == null) {
            throw IllegalArgumentException("Filter not set")
        }
        val (readPtr, readCnt) = makePtrAndCnt(readBank)
        val (filterPtr, filterCnt) = makePtrAndCnt(filterBank!!)
        var tagData = this.scanner.readData(
            password,
            filterBank!!,
            filterPtr,
            filterCnt * 16,
            filterData!!,
            readBank,
            readPtr,
            readCnt
        )
        if (tagData != null) {
            return tagData
        } else {
            tagData = this.scanner.readData(
                defaultPassword,
                filterBank!!,
                filterPtr,
                filterCnt * 16,
                filterData!!,
                readBank,
                readPtr,
                readCnt
            )
            if (tagData != null) {
                return tagData
            } else {
                throw Exception("Failed to read tag from ${bankErrorMsg(readBank)}")
            }
        }
    }

    /**
     * Starts the tag reading loop.
     */
    fun readTagLoop() {
        // only can read EPC / Tid / User
        if (loopFlag) {
            throw RuntimeException("Loop already started")
        }
        this.scanner.startInventoryTag()
        this.loopFlag = true
    }

    /**
     * Starts the tag reading loop and processes each tag read using the provided lambda function.
     *
     * This function initiates the inventory process for reading tags and continuously checks for new tags
     * in a coroutine. When a tag is detected, it calls the provided lambda function with the tag data.
     *
     * @param lifecycleScope The CoroutineScope used to launch the coroutine, typically the lifecycle scope of an Activity or Fragment.
     * @param onTagRead A lambda function that takes a UHFTAGInfo object as a parameter, which is called whenever a tag is read successfully.
     */
    fun readTagLoop(lifecycleScope: CoroutineScope, onTagRead: (tag: UHFTAGInfo) -> Unit) {
        // 开始读取标签
        readTagLoop()

        lifecycleScope.launch {
            while (loopFlag) {
                val tag = scanner.readTagFromBuffer()
                if (tag != null) {
                    onTagRead(tag)
                    delay(0)
                } else {
                    delay(10)
                }
            }
        }
    }

    /**
     * Stops the tag reading loop.
     */
    fun stopReadTagLoop() {
        if (this.loopFlag) {
            this.scanner.stopInventory()
            this.loopFlag = false
        }

    }

    /**
     * Starts the tag reading loop with a filter applied.
     * @param filterBank the memory bank to filter on.
     * @param filterData the data to filter.
     */
    fun readTagLoopWithFilter(filterBank: Int, filterData: String) {
        this.setFilter(filterBank, filterData)
        this.readTagLoop()
    }

    /**
     * Starts the tag reading loop with an applied filter and processes each tag read using the provided lambda function.
     *
     * @param filterBank The memory bank to apply the filter to.
     * @param filterData The data to filter on.
     * @param lifecycleScope The CoroutineScope used for launching the coroutine, typically the lifecycle scope of an Activity or Fragment.
     * @param onTagRead A lambda function that takes a UHFTAGInfo object as a parameter, which is called whenever a tag is read successfully.
     */
    fun readTagLoopWithFilter(
        filterBank: Int,
        filterData: String,
        lifecycleScope: CoroutineScope,
        onTagRead: (tag: UHFTAGInfo) -> Unit
    ) {
        this.setFilter(filterBank, filterData)
        this.readTagLoop(lifecycleScope, onTagRead)
    }

    /**
     * Sets the power level for the RFID scanner.
     * @param power the power level to set (default is 5).
     * @throws IllegalArgumentException if the power value is invalid.
     */
    fun setPower(power: Int = 5) {
        if (power <= 0 || power > 30) {
            throw IllegalArgumentException("Invalid power value")
        }
        this.scanner.setPower(power)
    }

    private fun lockTag(): Boolean {
        return this.scanner.lockMem(this.password, this.lockCode)
    }

    /**
     * Writes data to a specified memory bank.
     * @param writeBank the memory bank to write to.
     * @param data the data to write.
     * @return true if the write operation was successful, false otherwise.
     */
    fun writeTag(writeBank: Int, data: String): Boolean {
        val originalPower: Int = this.scanner.power
        if (this.scanner.power > 5) {
            this.scanner.setPower(5)
        }
        val (ptr, cnt) = makePtrAndCnt(writeBank)
        var result = this.scanner.writeData(password, writeBank, ptr, cnt, data)
        if (result) {
            lockTag()
            this.scanner.setPower(originalPower)
            return true
        } else {
            result = this.scanner.writeData(defaultPassword, writeBank, ptr, cnt, data)
            if (result) {
                this.scanner.setPower(originalPower)
                return true
            } else {
                this.scanner.setPower(originalPower)
                return false
            }
        }
    }

    /**
     * Writes data to a specified memory bank with an applied filter.
     * @param writeBank the memory bank to write to.
     * @param data the data to write.
     * @param filterBank the memory bank to filter on.
     * @param filterData the data to filter.
     * @return true if the write operation was successful, false otherwise.
     * @throws IllegalArgumentException if the filter bank value is invalid.
     */
    fun writeTagWithFilter(
        writeBank: Int,
        data: String,
        filterBank: Int,
        filterData: String
    ): Boolean {
        if (filterBank == 0) {
            throw IllegalArgumentException("Invalid filter bank value, filter bank cannot be Reserved Bank")
        }
        this.setFilter(filterBank, filterData)
        return this.writeTagWithFilter(writeBank, data)
    }

    /**
     * Writes data to a specified memory bank with the current applied filter.
     * @param writeBank the memory bank to write to.
     * @param data the data to write.
     * @return true if the write operation was successful, false otherwise.
     * @throws IllegalArgumentException if the filter is not set.
     */
    fun writeTagWithFilter(writeBank: Int, data: String): Boolean {
        if (this.filterBank == null || this.filterData == null) {
            throw IllegalArgumentException("Filter not set")
        }
        val originalPower: Int = this.scanner.power
        if (this.scanner.power > 5) {
            this.scanner.setPower(5)
        }
        val (writePtr, writeCnt) = makePtrAndCnt(writeBank)
        val (filterPtr, filterCnt) = makePtrAndCnt(filterBank!!)
        var result = this.scanner.writeData(
            password,
            filterBank!!,
            filterPtr,
            filterCnt * 16,
            filterData!!,
            writeBank,
            writePtr,
            writeCnt,
            data
        )
        if (result) {
            lockTag()
            this.scanner.setPower(originalPower)
            return true
        } else {
            result = this.scanner.writeData(
                defaultPassword,
                filterBank!!,
                filterPtr,
                filterCnt * 16,
                filterData!!,
                writeBank,
                writePtr,
                writeCnt,
                data
            )
            if (result) {
                this.scanner.setPower(originalPower)
                return true
            } else {
                this.scanner.setPower(originalPower)
                return false
            }
        }

    }

    /**
     * Locks the tag with the current applied filter.
     * @return true if the lock operation was successful, false otherwise.
     * @throws IllegalArgumentException if the filter is not set.
     */
    fun setTagPassword(newPassword: String): Boolean {
        if (newPassword.length != 8) {
            throw IllegalArgumentException("Password must be 8 characters long")
        }
        if (!newPassword.matches(Regex("[0-9A-Fa-f]+"))) {
            throw IllegalArgumentException("Password must be a hexadecimal string")
        }
        val p = newPassword + newPassword
        return writeTag(Bank_RESERVED, p)
    }

    fun startLocateTag(
        fragment: Fragment,
        bank: Int,
        data: String,
        locationCallback: IUHFLocationCallback
    ) {
        if (loopFlag) {
            throw RuntimeException("Loop already started")
        }
        val (ptr, cnt) = makePtrAndCnt(bank)
        this.scanner.startLocation(fragment.requireContext(), data, bank, ptr, locationCallback)
        this.loopFlag = true

//        lifecycleScope.launch {
//            while (loopFlag) {
//                val tag = scanner.readTagFromBuffer()
//                if (tag != null) {
//                    onTagRead(tag)
//                } else {
//                    delay(100)
//                }
//            }
//        }
    }

    fun stopLocateTag() {
        if (this.loopFlag) {
            this.scanner.stopLocation()
            this.loopFlag = false
        }
    }

    fun getPower(): Int {
        return this.scanner.power
    }
}