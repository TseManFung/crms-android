package com.crms.crmsAndroid.algorithm

import kotlin.math.log10
import kotlin.math.pow

/**
 * RSSI → distance (centimetres) using the Group 5 lab table.
 *
 * Experiment: passive UHF label, 0–100 cm, two Android readers at 30 dBm,
 * tag parallel to the antenna plane. The published 4th-order polynomial
 * was fitted on the *average* of Scanner A and Scanner B:
 *
 *   d_cm = -107.6 - 4.647 r - 0.01867 r² + 0.001100 r³ + 1.229e-5 r⁴
 *
 * That curve is kept as [rssiToDistancePoly] for report compatibility.
 * [rssiToDistance] prefers piecewise interpolation on the measured
 * average table (the polynomial oscillates around the 70 cm standing-wave
 * bump) and falls back to a log-distance model outside the table.
 */
class UniversalDistanceEstimator(
    private var pathLossExponent: Double = 4.1,
    private var referenceDistanceCm: Double = 10.0,
    private var rssiAtReference: Double = -41.9,
    private val minDistanceCm: Double = 0.5,
    private val maxDistanceCm: Double = 120.0
) {
    /** Scanner A raw averages from the report table. */
    val scannerA = listOf(
        -34.0 to 0.0,
        -39.0 to 10.0,
        -50.0 to 20.0,
        -64.0 to 30.0,
        -66.5 to 40.0,
        -70.0 to 50.0,
        -74.9 to 60.0,
        -73.8 to 70.0,
        -81.6 to 80.0,
        -77.9 to 100.0
    )

    /** Scanner B raw averages from the report table. */
    val scannerB = listOf(
        -31.0 to 0.0,
        -44.8 to 10.0,
        -51.8 to 20.0,
        -64.2 to 30.0,
        -75.9 to 40.0,
        -79.2 to 50.0,
        -79.6 to 60.0,
        -71.8 to 70.0,
        -79.8 to 80.0,
        -80.4 to 100.0
    )

    /** Report polynomial, low degree first. Fitted on (A+B)/2. */
    private val polyCoeffs = doubleArrayOf(
        -1.07557144e+02,
        -4.64697454e+00,
        -1.86683434e-02,
        1.10018281e-03,
        1.22946053e-05
    )

    private val calibrationPoints = mutableListOf<Pair<Double, Double>>()

    init {
        loadDefaultCalibration()
    }

    fun loadDefaultCalibration() {
        calibrationPoints.clear()
        val n = minOf(scannerA.size, scannerB.size)
        for (i in 0 until n) {
            val d = scannerA[i].second
            val rssi = 0.5 * (scannerA[i].first + scannerB[i].first)
            calibrationPoints.add(rssi to maxOf(d, 0.5))
        }
        fitPathLoss()
    }

    fun loadScannerA() {
        calibrationPoints.clear()
        scannerA.forEach { (r, d) -> calibrationPoints.add(r to maxOf(d, 0.5)) }
        fitPathLoss()
    }

    fun loadScannerB() {
        calibrationPoints.clear()
        scannerB.forEach { (r, d) -> calibrationPoints.add(r to maxOf(d, 0.5)) }
        fitPathLoss()
    }

    fun clearCalibration() {
        calibrationPoints.clear()
        pathLossExponent = 4.1
        rssiAtReference = -41.9
        referenceDistanceCm = 10.0
    }

    fun addCalibrationPoint(measuredRssi: Double, knownDistanceCm: Double) {
        if (!knownDistanceCm.isFinite() || knownDistanceCm <= 0.0) return
        if (!measuredRssi.isFinite()) return
        calibrationPoints.add(measuredRssi to knownDistanceCm)
        fitPathLoss()
    }

    fun calibrationSize(): Int = calibrationPoints.size

    /** Production entry: interpolation + log-distance, units = cm. */
    fun rssiToDistance(rssi: Double): Double {
        val clipped = rssi.coerceIn(-92.0, -25.0)
        val interpolated = interpolate(clipped)
        val modeled = pathLossDistance(clipped)
        val distance = when (interpolated) {
            null -> modeled
            else -> 0.70 * interpolated + 0.30 * modeled
        }
        return distance.coerceIn(minDistanceCm, maxDistanceCm)
    }

    /** Same as [rssiToDistance]; old DirectionFinder name. */
    fun rssiToDistance1(rssi: Double): Double = rssiToDistance(rssi)

    /** Exact report polynomial. Prefer [rssiToDistance] at runtime. */
    fun rssiToDistancePoly(rssi: Double): Double {
        val y = polyCoeffs.foldIndexed(0.0) { i, acc, c -> acc + c * rssi.pow(i) }
        return y.coerceIn(minDistanceCm, maxDistanceCm)
    }

    fun pathLossDistance(rssi: Double): Double {
        val n = pathLossExponent.coerceAtLeast(1.2)
        val exponent = (rssiAtReference - rssi) / (10.0 * n)
        val raw = referenceDistanceCm * 10.0.pow(exponent)
        return raw.coerceIn(minDistanceCm, maxDistanceCm)
    }

    private fun interpolate(rssi: Double): Double? {
        if (calibrationPoints.size < 2) return null
        val sorted = calibrationPoints.sortedBy { it.first }
        if (rssi <= sorted.first().first) return sorted.first().second
        if (rssi >= sorted.last().first) return sorted.last().second
        for (i in 0 until sorted.lastIndex) {
            val (r0, d0) = sorted[i]
            val (r1, d1) = sorted[i + 1]
            if (rssi in r0..r1 || rssi in r1..r0) {
                val t = if (r1 == r0) 0.0 else (rssi - r0) / (r1 - r0)
                return d0 + t * (d1 - d0)
            }
        }
        return null
    }

    private fun fitPathLoss() {
        val usable = calibrationPoints.filter { it.second >= 1.0 && it.first.isFinite() }
        if (usable.size < 2) return
        val d0 = referenceDistanceCm
        val xs = usable.map { log10(it.second / d0) }
        val ys = usable.map { it.first }
        val meanX = xs.average()
        val meanY = ys.average()
        var sxx = 0.0
        var sxy = 0.0
        for (i in xs.indices) {
            val dx = xs[i] - meanX
            sxx += dx * dx
            sxy += dx * (ys[i] - meanY)
        }
        if (sxx < 1e-9) return
        val b = sxy / sxx
        if (b >= -1e-3) return
        rssiAtReference = meanY - b * meanX
        pathLossExponent = (-b / 10.0).coerceIn(1.5, 6.0)
    }
}
