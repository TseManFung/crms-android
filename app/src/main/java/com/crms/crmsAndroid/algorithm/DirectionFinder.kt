// DirectionFinder.kt
package com.crms.crmsAndroid.algorithm

import android.util.Log
import androidx.lifecycle.AtomicReference
import java.util.LinkedList
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class DirectionFinder() {
    private val tagHistory = HashMap<String, LinkedList<Pair<Long, Double>>>()
    private val kalmanFilters = HashMap<String, KalmanFilter>()
    private val tagPositions = HashMap<String, PolarCoordinate>()
    private val scannerDirection = AtomicReference(0.0)

    // 配置参数
    private val maxHistorySize = 20
    private val timeWindow = 3000L
    private val minValidData = 3 // 降低最小数据要求
    private val staticRssiThreshold = -30.0 // 调整静态阈值
    private val scannerFOV = Math.toRadians(170.0) // 扩大视野范围
    private val pathLossExponent = 2.5
    private val referenceDistance = 1.0
    private val rssiAtReference = -45.0

    // 状态保持
    private var currentAngle = 0.0
    private var distanceEstimator = UniversalDistanceEstimator()
    var targetTag: String = ""
    data class PolarCoordinate(val distance: Double, val angle: Double)


    //    fun updateScannerRotation(angleDelta: Double) {
//        scannerDirection.updateAndGet { normalizeAngle(it + angleDelta) }
//    }
    private fun processReferenceTag(tid: String) {
        if (tid != targetTag && !tagPositions.containsKey(tid)) {
            // 动态生成参考标签初始位置
            tagPositions[tid] = PolarCoordinate(
                2.0 + Random().nextDouble() * 3.0, // 随机距离1-5米
                Random().nextDouble() * 2 * PI    // 随机方向
            ).also {
                Log.d("DirectionFinder", "Auto-registered reference tag $tid at $it")
            }
        }
    }

    fun updateTag(tid: String, rssi: Double) {
        processReferenceTag(tid)
        val validRssi = when {
            rssi > -20.0 -> -20.0 // 处理贴脸异常值
            rssi < -80.0 -> -80.0 // 过滤过弱信号
            else -> rssi
        }
        val currentTime = System.currentTimeMillis()
        val filteredRssi = kalmanFilters.getOrPut(tid) { KalmanFilter() }.update(rssi)

        tagHistory.getOrPut(tid) { LinkedList() }.apply {
            add(currentTime to filteredRssi)
            while (size > maxHistorySize || (first?.let { currentTime - it.first > timeWindow } == true)) {
                removeFirst()
            }
        }
    }

    fun calculateDirection(): Double {
        return calculateDetailedDirection() ?: run {
            // 备用算法：基于最近3次RSSI变化
            val targetData = tagHistory[targetTag]?.takeLast(3) ?: return 0.0
            val trend = targetData.last().second - targetData.first().second
            Log.d(
                "DirectionFinderA",
                "Calculated direction: $currentAngle + ${(trend.coerceIn(-2.0, 2.0) * PI / 3)}"
            )
            currentAngle + (trend.coerceIn(-2.0, 2.0) * PI / 12)

        }
    }

    fun calculateDetailedDirection(): Double? {
        // 原有算法逻辑，但降低判断阈值
        val targetData = tagHistory[targetTag] ?: return null
        if (targetData.size < 2) return null

        // 修改为至少需要1个参考标签
        val references = tagHistory.filterKeys { it != targetTag }
        if (references.size < 1) return null

        // 计算目标位置
        val targetPosition = estimatePosition(targetTag, targetData) ?: return null

        // 计算参考标签相对位置变化
        val vectors = references.mapNotNull { (tid, data) ->
            estimatePosition(tid, data)?.let { pos ->
                val refPos = tagPositions[tid] ?: return@mapNotNull null
                val dx = pos.distance * cos(pos.angle) - refPos.distance * cos(refPos.angle)
                val dy = pos.distance * sin(pos.angle) - refPos.distance * sin(refPos.angle)
                atan2(dy, dx) to calculateWeight(tid)
            }
        }

        if (vectors.isEmpty()) return null

        // 加权平均方向
        val totalWeight = vectors.sumOf { it.second }
        var rawDirection = vectors.sumOf { it.first * it.second } / totalWeight

        // 放宽扫描范围约束
        rawDirection = when {
            isInScanRange(rawDirection) -> rawDirection
            else -> normalizeAngle(rawDirection + PI) // 总是允许反向扫描
        }

        Log.d("DirectionFinderA", "Calculated direction: $rawDirection")
        return rawDirection
    }

    private fun estimateSingleTarget(data: List<Pair<Long, Double>>): Double? {
        val avgRssi = data.map { it.second }.average()
        val rssiTrend = data.takeLast(3).let { it.last().second - it.first().second }

        return when {
            avgRssi > -35.0 -> scannerDirection.get() // 视为正前方
            rssiTrend > 2.0 -> normalizeAngle(scannerDirection.get() + PI / 6) // 信号增强向右转
            rssiTrend < -2.0 -> normalizeAngle(scannerDirection.get() - PI / 6) // 信号减弱向左转
            else -> null
        }
    }

    private fun estimatePosition(tid: String, data: List<Pair<Long, Double>>): PolarCoordinate? {
        if (data.isEmpty()) return null // 增加空数据保护

        // 明确计算平均RSSI
        val avgRssi = data.map { it.second }.average()

        // 静态标签处理
        if (data.all { it.second >= staticRssiThreshold }) {
            return PolarCoordinate(0.1, scannerDirection.get())
        }

        // 改进距离估算稳定性
        val distance = distanceEstimator.rssiToDistance1(avgRssi)
            .coerceIn(0.1, 150.0) // 扩大有效范围

        // 改进方向估算逻辑
        val basePosition = tagPositions.getOrPut(tid) {
            PolarCoordinate(
                2.0 + Random().nextDouble() * 3.0,
                scannerDirection.get() + Random().nextDouble() * PI - PI / 2
            )
        }

        // 改进方向变化检测
        val rssiChanges = data.windowed(2).map { it[1].second - it[0].second }
        val directionDelta = when {
            rssiChanges.average() > 1.0 -> PI / 4  // 信号增强明显时右转
            rssiChanges.average() < -1.0 -> -PI / 4 // 信号减弱明显时左转
            else -> (Random().nextDouble() - 0.5) * PI / 8 // 添加随机扰动
        }

        return PolarCoordinate(
            distance.coerceIn(0.1, 15.0), // 限制最大距离
            normalizeAngle(basePosition.angle + directionDelta)
        )
    }

    private fun isInScanRange(angle: Double): Boolean {
        val relativeAngle = normalizeAngle(angle - scannerDirection.get())
        return relativeAngle <= scannerFOV / 2 || relativeAngle >= (2 * PI - scannerFOV / 2)
    }

    private fun shouldConsiderBackside(data: List<Pair<Long, Double>>): Boolean {
        val lastThree = data.takeLast(3).map { it.second }
        return lastThree.size >= 3 && lastThree.windowed(2).all { it[1] < it[0] }
    }

    private fun calculateWeight(tid: String): Double {
        val dataSizeWeight = (tagHistory[tid]?.size?.toDouble() ?: 0.0) / maxHistorySize
        val data = tagHistory[tid] ?: return 0.0
        val freshness =
            1.0 - (System.currentTimeMillis() - data.last().first) / timeWindow.toDouble()
        val stability =
            1.0 - data.windowed(2).map { abs(it[1].second - it[0].second) }.average() / 10.0
        return (freshness + stability + dataSizeWeight).coerceIn(0.3, 1.5)
    }

    private fun normalizeAngle(rad: Double): Double {
        return (rad % (2 * PI)).let { if (it < 0) it + 2 * PI else it }
    }

    fun normalizeAngle(deg: Float): Float {
        return (deg % 360).let { if (it < 0) it + 360 else it }
    }

    fun clearData() {
        targetTag = ""
        tagHistory.clear()
        kalmanFilters.clear()
        currentAngle = 0.0
    }

    fun getEstimatedDirection(Rssi : Double): Double {
        return distanceEstimator.rssiToDistance(Rssi)
    }
}