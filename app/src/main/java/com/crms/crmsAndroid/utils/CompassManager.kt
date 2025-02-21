package com.crms.crmsAndroid.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.atan2
import java.lang.Math.toDegrees

class CompassManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
    private var currentDegree = 0f

    init {
        sensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val degree =
                toDegrees(atan2(event.values[0].toDouble(), event.values[1].toDouble())).toFloat()
            currentDegree = degree
        }
    }

    fun getDirection(): Float {
        return currentDegree
    }

    fun unregister() {
        sensorManager.unregisterListener(this)
    }
}