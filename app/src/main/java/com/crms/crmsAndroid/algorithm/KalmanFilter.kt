package com.crms.crmsAndroid.algorithm

/**
 * Scalar Kalman filter for smoothing a 1-D measurement (RSSI).
 *
 * The first [update] seeds the estimate with the measurement so the
 * filter does not get pulled toward 0 dBm.
 */
class KalmanFilter(
    private val processNoise: Double = 1e-4,
    private val measurementNoise: Double = 1.5
) {
    private var estimate: Double = 0.0
    private var errorCovariance: Double = 1.0
    private var initialized: Boolean = false

    fun update(measurement: Double): Double {
        if (!initialized) {
            estimate = measurement
            errorCovariance = measurementNoise
            initialized = true
            return estimate
        }

        val predErrorCovariance = errorCovariance + processNoise
        val kalmanGain = predErrorCovariance / (predErrorCovariance + measurementNoise)
        estimate += kalmanGain * (measurement - estimate)
        errorCovariance = (1.0 - kalmanGain) * predErrorCovariance
        return estimate
    }

    fun value(): Double = estimate

    fun reset() {
        estimate = 0.0
        errorCovariance = 1.0
        initialized = false
    }
}
