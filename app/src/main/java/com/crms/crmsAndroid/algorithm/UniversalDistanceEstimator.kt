package com.crms.crmsAndroid.algorithm

import kotlin.math.abs
import kotlin.math.pow

class UniversalDistanceEstimator {
    private var coefficients = doubleArrayOf(3.8e-5, -0.0032, 0.083, -1.08, 4.25, -32.8)
    private val calibrationPoints = mutableListOf<Pair<Double, Double>>()

    fun addCalibrationPoint(measuredRssi: Double, knownDistance: Double) {
        // 使用提供的校准数据预加载
        if (calibrationPoints.size < 10) {
            calibrationPoints.addAll(listOf(
                Pair(-34.0, 0.0),
                Pair(-39.0, 10.0),
                Pair(-50.0, 20.0),
                Pair(-64.0, 30.0),
                Pair(-66.5, 40.0),
                Pair(-70.0, 50.0),
                Pair(-74.9, 60.0),
                Pair(-73.8, 70.0),
                Pair(-81.6, 80.0),
                Pair(-77.9, 100.0)
            ))
            updateCoefficients()
        }
    }

    private fun updateCoefficients() {
        val n = calibrationPoints.size
        val degree = 4 // 二次多项式

        // 构建矩阵 X^T * X 和 X^T * Y
        val XtX = Array(degree + 1) { DoubleArray(degree + 1) }
        val XtY = DoubleArray(degree + 1)

        calibrationPoints.forEach { (x, y) ->
            for (i in 0..degree) {
                for (j in 0..degree) {
                    XtX[i][j] += x.pow(i + j)
                }
                XtY[i] += x.pow(i) * y
            }
        }

        // 高斯消元法求解
        for (i in 0..degree) {
            // 归一化
            val pivot = XtX[i][i]
            for (j in i..degree) XtX[i][j] /= pivot
            XtY[i] /= pivot

            // 消元
            for (k in 0..degree) {
                if (k != i) {
                    val factor = XtX[k][i]
                    for (j in i..degree) XtX[k][j] -= factor * XtX[i][j]
                    XtY[k] -= factor * XtY[i]
                }
            }
        }

        // 更新系数
        coefficients = XtY
    }

    fun rssiToDistance(rssi: Double): Double {
        return coefficients.foldIndexed(0.0) { i, acc, coeff ->
            acc + coeff * rssi.pow(i)
        }
    }
}