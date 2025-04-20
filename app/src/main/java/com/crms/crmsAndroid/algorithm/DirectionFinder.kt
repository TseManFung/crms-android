package com.crms.crmsAndroid.algorithm

import android.util.Log
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Handheld single-antenna RFID / BLE direction helper.
 *
 * A single RSSI sample cannot encode an angle. This class therefore:
 *  1. Smooths RSSI with a per-tag Kalman filter.
 *  2. Maps RSSI → distance with [UniversalDistanceEstimator].
 *  3. Estimates bearing from the *heading at which the target RSSI peaked*
 *     while the scanner rotates. Call [updateScannerRotation] or
 *     [setScannerHeading] from the device gyro / compass.
 *
 * [calculateDirection] returns the estimated target bearing in radians
 * in the same frame as the scanner heading (0 = current forward when
 * heading has never been updated).
 */
class DirectionFinder {

    data class PolarCoordinate(val distance: Double, val angle: Double)
    private data class Sample(val timeMs: Long, val heading: Double, val rssi: Double)

    private val tagHistory = HashMap<String, LinkedList<Sample>>()
    private val kalmanFilters = HashMap<String, KalmanFilter>()
    private val lastBearings = HashMap<String, Double>()
    private val lastDistances = HashMap<String, Double>()
    private val scannerHeading = AtomicReference(0.0)
    private val distanceEstimator = UniversalDistanceEstimator()

    private val maxHistorySize = 40
    private val timeWindowMs = 4_000L
    private val minPeakSamples = 4
    private val minSweepForPeak = Math.toRadians(50.0)
    private val rssiClipMin = -90.0
    private val rssiClipMax = -20.0
    private val bearingSmooth = 0.35

    var targetTag: String = ""

    fun updateScannerRotation(angleDelta: Double) {
        scannerHeading.updateAndGet { normalizeAngle(it + angleDelta) }
    }

    fun setScannerHeading(headingRad: Double) {
        scannerHeading.set(normalizeAngle(headingRad))
    }

    fun getScannerHeading(): Double = scannerHeading.get()

    fun updateTag(tid: String, rssi: Double) {
        if (tid.isEmpty() || !rssi.isFinite()) return
        val clipped = rssi.coerceIn(rssiClipMin, rssiClipMax)
        val filtered = kalmanFilters.getOrPut(tid) { KalmanFilter() }.update(clipped)
        val now = System.currentTimeMillis()
        val heading = scannerHeading.get()

        tagHistory.getOrPut(tid) { LinkedList() }.apply {
            add(Sample(now, heading, filtered))
            while (isNotEmpty() && (size > maxHistorySize || now - first.timeMs > timeWindowMs)) {
                removeFirst()
            }
        }

        lastDistances[tid] = distanceEstimator.rssiToDistance(filtered) // centimetres
    }

    /**
     * Estimated target bearing in radians, [0, 2π).
     * Falls back to a hotter/colder turn hint when the user has not
     * swept enough angle for a peak lock.
     */
    fun calculateDirection(): Double {
        val detailed = calculateDetailedDirection()
        if (detailed != null) return detailed
        return hotterColderHint()
    }

    fun calculateDetailedDirection(): Double? {
        val data = tagHistory[targetTag] ?: return null
        if (data.size < minPeakSamples) return null

        val sweep = angularCoverage(data)
        if (sweep < minSweepForPeak) return lastBearings[targetTag]

        val peak = peakHeading(data) ?: return null
        val previous = lastBearings[targetTag]
        val blended = if (previous == null) {
            peak
        } else {
            circularBlend(previous, peak, bearingSmooth)
        }
        val result = normalizeAngle(blended)
        lastBearings[targetTag] = result
        Log.d(TAG, "bearing=$result sweep=$sweep peak=$peak n=${data.size}")
        return result
    }

    fun estimatePosition(tid: String = targetTag): PolarCoordinate? {
        val dist = lastDistances[tid] ?: run {
            val rssi = tagHistory[tid]?.lastOrNull()?.rssi ?: return null
            distanceEstimator.rssiToDistance(rssi)
        }
        val angle = lastBearings[tid] ?: calculateDirection()
        return PolarCoordinate(dist, angle)
    }

    /** Distance in the same units as the calibration table. */
    fun getEstimatedDistance(): Double {
        val tid = targetTag
        lastDistances[tid]?.let { return it }
        val rssi = tagHistory[tid]?.lastOrNull()?.rssi ?: return Double.NaN
        return distanceEstimator.rssiToDistance(rssi)
    }

    fun getEstimatedDistance(rssi: Double): Double =
        distanceEstimator.rssiToDistance(rssi)

    /**
     * Old name, old behaviour: RSSI → distance (not an angle).
     * Kept so existing call sites compile.
     */
    fun getEstimatedDirection(rssi: Double): Double = getEstimatedDistance(rssi)

    fun addCalibrationPoint(measuredRssi: Double, knownDistance: Double) {
        distanceEstimator.addCalibrationPoint(measuredRssi, knownDistance)
    }

    fun clearData() {
        targetTag = ""
        tagHistory.clear()
        kalmanFilters.clear()
        lastBearings.clear()
        lastDistances.clear()
        scannerHeading.set(0.0)
    }

    fun normalizeAngle(deg: Float): Float {
        val m = deg % 360f
        return if (m < 0f) m + 360f else m
    }

    // --- internals ----------------------------------------------------------

    /**
     * If RSSI is rising while the device rotates one way, keep turning
     * that way; if falling, reverse. Returns an absolute heading hint.
     */
    private fun hotterColderHint(): Double {
        val data = tagHistory[targetTag] ?: return scannerHeading.get()
        if (data.size < 2) return scannerHeading.get()

        val recent = data.takeLast(6)
        var rssiDelta = 0.0
        var headingDelta = 0.0
        for (i in 1 until recent.size) {
            rssiDelta += recent[i].rssi - recent[i - 1].rssi
            headingDelta += wrappedDelta(recent[i].heading, recent[i - 1].heading)
        }
        val heading = scannerHeading.get()
        val hint = when {
            abs(rssiDelta) < 0.4 -> lastBearings[targetTag] ?: heading
            // RSSI up in the direction we were already turning → stay on that heading
            rssiDelta > 0.0 && abs(headingDelta) < 1e-3 -> heading
            rssiDelta > 0.0 -> heading
            // RSSI down → tag is more likely the other way; step 30° opposite to last turn
            headingDelta > 0.0 -> normalizeAngle(heading - PI / 6.0)
            headingDelta < 0.0 -> normalizeAngle(heading + PI / 6.0)
            else -> normalizeAngle(heading + PI)
        }
        lastBearings[targetTag] = hint
        return hint
    }

    private fun peakHeading(data: List<Sample>): Double? {
        if (data.isEmpty()) return null
        // Weight samples toward stronger RSSI so a noisy single spike
        // does not dominate, but the lobe still wins.
        var sinSum = 0.0
        var cosSum = 0.0
        val maxRssi = data.maxOf { it.rssi }
        val minRssi = data.minOf { it.rssi }
        val span = (maxRssi - minRssi).coerceAtLeast(1.0)
        for (s in data) {
            val w = ((s.rssi - minRssi) / span).coerceAtLeast(0.0).pow(2.0)
            sinSum += w * sin(s.heading)
            cosSum += w * cos(s.heading)
        }
        if (sinSum == 0.0 && cosSum == 0.0) {
            return data.maxByOrNull { it.rssi }?.heading
        }
        return normalizeAngle(atan2(sinSum, cosSum))
    }

    private fun angularCoverage(data: List<Sample>): Double {
        if (data.size < 2) return 0.0
        val headings = data.map { normalizeAngle(it.heading) }.sorted()
        var maxGap = 0.0
        for (i in 1 until headings.size) {
            maxGap = maxOf(maxGap, headings[i] - headings[i - 1])
        }
        maxGap = maxOf(maxGap, headings.first() + 2.0 * PI - headings.last())
        val covered = 2.0 * PI - maxGap
        return covered.coerceIn(0.0, 2.0 * PI)
    }

    private fun circularBlend(previous: Double, next: Double, alpha: Double): Double {
        val d = wrappedDelta(next, previous)
        return normalizeAngle(previous + alpha * d)
    }

    private fun wrappedDelta(a: Double, b: Double): Double {
        var d = normalizeAngle(a) - normalizeAngle(b)
        if (d > PI) d -= 2.0 * PI
        if (d < -PI) d += 2.0 * PI
        return d
    }

    private fun normalizeAngle(rad: Double): Double {
        var a = rad % (2.0 * PI)
        if (a < 0.0) a += 2.0 * PI
        return a
    }

    companion object {
        private const val TAG = "DirectionFinder"
    }
}
