// DirectionFinder.kt
package com.crms.crmsAndroid.algorithm

import android.util.Log
import java.util.LinkedList
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

class RelativeDirectionCalculator(private val targetTag: String) {
    private val tagHistory = HashMap<String, LinkedList<Pair<Long, Double>>>()
    private val kalmanFilters = HashMap<String, KalmanFilter>()

    // 配置参数
    private val maxHistorySize = 20
    private val timeWindow = 3000L
    private val minValidData = 5
    private val staticRssiThreshold = -25.0

    var distanceSensitivity = 0.3 // 距离变化敏感度
    var angleSmoothingFactor = 0.2 // 角度平滑系数
    private var pathLoss = 25.0

    // 状态保持
    private var lastPosition = Pair(0.0, 0.0)
    private var currentAngle = 0.0


    fun updateTag(tid: String, rssi: Double) {
        val currentTime = System.currentTimeMillis()
        val filteredRssi = kalmanFilters.getOrPut(tid) { KalmanFilter() }.update(rssi)

        tagHistory.getOrPut(tid) { LinkedList() }.apply {
            add(currentTime to filteredRssi)
            while (size > maxHistorySize || (first?.let { currentTime - it.first > timeWindow } == true)) {
                removeFirst()
            }
        }
    }

    fun calculateDirection(): Double? {
        val targetData = tagHistory[targetTag] ?: return null
        if (targetData.size < minValidData) return null

        val otherTags = tagHistory.filterKeys { it != targetTag }
        if (otherTags.isEmpty()) return null

        // 计算目标标签的相对移动向量
        val targetVector = calculateMovementVector(targetTag) ?: return null

        // 计算其他标签的相对移动模式
        val referenceVectors = otherTags.mapNotNull { (tid, _) ->
            calculateMovementVector(tid)?.let { tid to it }
        }

        // 动态构建参考坐标系
        val (xAxisTag, yAxisTag) = selectReferenceTags(referenceVectors)

        Log.d(
            "DirectionCalculatorR",
            "Reference Tags: $xAxisTag, $yAxisTag, Vectors: $referenceVectors"
        )
        // 计算方向角度
        val rawAngle = when {
            xAxisTag != null && yAxisTag != null -> {
                val xVec = referenceVectors.find { it.first == xAxisTag }!!.second
                val yVec = referenceVectors.find { it.first == yAxisTag }!!.second
                calculateAngleFromVectors(targetVector, xVec, yVec)
            }

            referenceVectors.isNotEmpty() -> {
                val mainRefVec = referenceVectors.maxBy { it.second.magnitude }.second
                atan2(targetVector.dy, targetVector.dx) - atan2(mainRefVec.dy, mainRefVec.dx)
            }

            else -> atan2(targetVector.dy, targetVector.dx)
        }
        Log.d(
            "DirectionCalculatorA",
            "(1 - $angleSmoothingFactor) * $currentAngle + $angleSmoothingFactor * $rawAngle"
        )
        // 应用角度平滑
        currentAngle = (1 - angleSmoothingFactor) * currentAngle + angleSmoothingFactor * rawAngle
        Log.d("DirectionCalculatorA", "Current Angle: $currentAngle")
        return normalizeAngle(currentAngle)
    }

    private data class MovementVector(val dx: Double, val dy: Double, val magnitude: Double)

    // 修改后的MovementVector计算逻辑
    private fun calculateMovementVector(tid: String): MovementVector? {
        val data = tagHistory[tid] ?: return null
        if (data.size < 2) return null

        val oldRssi = data.first().second
        val newRssi = data.last().second
        val deltaRssi = newRssi - oldRssi

        // 估算距离变化（考虑静态标签情况）
        val deltaDistance = when {
            newRssi >= staticRssiThreshold -> 0.0
            else -> ((10.0).pow(deltaRssi / pathLoss)) * distanceSensitivity
        }

        // 新增转向角度估算
        val deltaTheta = estimateRotationAngle()
        Log.d(
            "DirectionCalculatorT",
            "DeltaTheta: $deltaTheta, DeltaDistance: $deltaDistance, DeltaRssi: $deltaRssi"
        )

        // 改进方向计算：综合当前角度和转向变化
        return MovementVector(
            dx = deltaDistance * cos(currentAngle + deltaTheta),
            dy = deltaDistance * sin(currentAngle + deltaTheta),
            magnitude = abs(deltaRssi)
        )
    }

    // 新增转向角度估算函数
    private fun estimateRotationAngle(): Double {
        val vectors = tagHistory.mapNotNull { (tid, data) ->
            if (data.size < 2) null else {
                val deltaRssi = data.last().second - data.first().second
                tid to deltaRssi
            }
        }

        // 假设左侧标签信号增强时，右侧标签信号减弱（反之亦然）
        val maxTag = vectors.maxByOrNull { it.second }?.first
        val minTag = vectors.minByOrNull { it.second }?.first

        return if (maxTag != null && minTag != null) {
            val maxVector = tagHistory[maxTag]?.lastOrNull()?.second ?: 0.0
            val minVector = tagHistory[minTag]?.lastOrNull()?.second ?: 0.0
            val deltaRssi = maxVector - minVector

            // 估算转向角度（假设线性关系）
            atan2(minVector, maxVector) * (deltaRssi / (maxVector + minVector))
        } else {
            0.0
        }
    }

    private fun selectReferenceTags(vectors: List<Pair<String, MovementVector>>): Pair<String?, String?> {
        // 寻找正交性最好的两个向量作为参考轴
        var bestPair: Pair<String?, String?> = null to null
        var maxOrthogonality = 0.0

        vectors.forEach { (tid1, vec1) ->
            vectors.forEach { (tid2, vec2) ->
                if (tid1 != tid2) {
                    val dotProduct = vec1.dx * vec2.dx + vec1.dy * vec2.dy
                    val orthogonality = 1 - abs(dotProduct / (vec1.magnitude * vec2.magnitude))
                    if (orthogonality > maxOrthogonality) {
                        maxOrthogonality = orthogonality
                        bestPair = tid1 to tid2
                    }
                }
            }
        }
        return bestPair
    }

    private fun calculateAngleFromVectors(
        target: MovementVector, xAxis: MovementVector, yAxis: MovementVector
    ): Double {
        // 构建变换矩阵
        val det = xAxis.dx * yAxis.dy - xAxis.dy * yAxis.dx
        if (det == 0.0) return atan2(target.dy, target.dx)

        // 坐标变换
        val invDet = 1.0 / det
        val tx = (yAxis.dy * target.dx - yAxis.dx * target.dy) * invDet
        val ty = (-xAxis.dy * target.dx + xAxis.dx * target.dy) * invDet

        return atan2(ty, tx)
    }

    private fun normalizeAngle(rad: Double): Double {
        return (rad % (2 * PI)).let { if (it < 0) it + 2 * PI else it }
    }

    fun clearData() {
        tagHistory.clear()
        kalmanFilters.clear()
        lastPosition = Pair(0.0, 0.0)
        currentAngle = 0.0
    }
}

// 保留原有KalmanFilter类
class KalmanFilter(
    private val processNoise: Double = 1e-5, private val measurementNoise: Double = 0.1
) {
    private var estimate: Double = 0.0
    private var errorCovariance: Double = 1.0

    fun update(measurement: Double): Double {
        val predErrorCovariance = errorCovariance + processNoise
        val kalmanGain = predErrorCovariance / (predErrorCovariance + measurementNoise)
        estimate += kalmanGain * (measurement - estimate)
        errorCovariance = (1 - kalmanGain) * predErrorCovariance
        return estimate
    }
}