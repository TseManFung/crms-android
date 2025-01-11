package com.crms.crmsAndroid.algorithm

object Snowflake {
    private const val START_TIMESTAMP: Long = 1577836800000L // 自選定的時期
    private const val WORK_ID: Long = 1L // 機器 ID
    private const val DATA_CENTER_ID: Long = 1L // 數據中心 ID

    private var sequence: Long = 0L // 當前序列號
    private var lastTimestamp: Long = -1L // 上次生成 ID 的時間戳

    private const val WORK_ID_BITS: Int = 10 // 機器 ID 位數
    private const val SEQUENCE_BITS: Int = 12 // 序列號位數
    private const val MAX_WORK_ID: Long = (1L shl WORK_ID_BITS) - 1 // 最大機器 ID
    private const val SEQUENCE_MASK: Long = (1L shl SEQUENCE_BITS) - 1 // 序列號掩碼

    private var _token: Long? = null

    val token: Long
        get() {
            if (_token == null) {
                _token = generateToken()
            }
            return _token!!
        }

    init {
        require(WORK_ID in 0..MAX_WORK_ID) { "WORK_ID must be between 0 and $MAX_WORK_ID" }
    }

    @Synchronized
    fun generateToken(): Long {
        var now = System.currentTimeMillis()

        // 當前時間小於上次記錄時間，拋出異常
        if (now < lastTimestamp) {
            throw RuntimeException("Clock moved backwards. Refusing to generate ID")
        }

        // 如果當前時間等於上次時間，增加序列號
        if (now == lastTimestamp) {
            sequence = (sequence + 1) and SEQUENCE_MASK // 確保序列號不會溢出
            if (sequence == 0L) {
                now = waitNextMillis(lastTimestamp) // 等待下一毫秒
            }
        } else {
            // 當前時間不同，重置序列號
            sequence = 0L
        }

        lastTimestamp = now // 記錄當前時間

        // 計算 ID
        val timestampPart = (now - START_TIMESTAMP) shl (WORK_ID_BITS + SEQUENCE_BITS) // 左移 22 位
        val workIdPart = (DATA_CENTER_ID shl SEQUENCE_BITS) or timestampPart // 左移 12 位
        val id = workIdPart or (WORK_ID shl SEQUENCE_BITS) or sequence // 組合 ID

        return id // 返回生成的 ID
    }

    private fun waitNextMillis(lastTimestamp: Long): Long {
        var timestamp = System.currentTimeMillis()
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis()
        }
        return timestamp
    }
}