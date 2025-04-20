package com.crms.crmsAndroid.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.PI

/**
 * Device yaw in radians, [0, 2π). 0 is magnetic north, increasing clockwise.
 * Prefer rotation-vector; fall back to the orientation sensor.
 */
class CompassManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    @Suppress("DEPRECATION")
    private val orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)

    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    @Volatile
    private var headingRad = 0.0
    private var listening = false

    var onHeadingChanged: ((headingRad: Double) -> Unit)? = null

    fun start() {
        if (listening) return
        val sensor = rotationSensor ?: orientationSensor ?: return
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        listening = true
    }

    fun stop() {
        if (!listening) return
        sensorManager.unregisterListener(this)
        listening = false
    }

    fun getHeadingRad(): Double = headingRad

    /** Compass heading in degrees, [0, 360). */
    fun getDirection(): Float {
        var deg = Math.toDegrees(headingRad).toFloat() % 360f
        if (deg < 0f) deg += 360f
        return deg
    }

    fun unregister() = stop()

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        val e = event ?: return
        @Suppress("DEPRECATION")
        val rad = when (e.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, e.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                orientation[0].toDouble()
            }
            Sensor.TYPE_ORIENTATION -> Math.toRadians(e.values[0].toDouble())
            else -> return
        }
        headingRad = normalize(rad)
        onHeadingChanged?.invoke(headingRad)
    }

    private fun normalize(rad: Double): Double {
        var a = rad % (2.0 * PI)
        if (a < 0.0) a += 2.0 * PI
        return a
    }
}
