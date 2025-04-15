package com.crms.crmsAndroid.algorithm

class KalmanFilter(
    private val processNoise: Double = 1e-5,
    private val measurementNoise: Double = 0.1
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