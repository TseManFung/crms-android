package com.crms.crmsAndroid.ui.setting

import androidx.lifecycle.ViewModel

class SettingViewModel : ViewModel() {
    private var power: Int = 5

    fun getPower(): Int {
        return power
    }

    fun setPower(newPower: Int) {
        power = newPower
    }
}