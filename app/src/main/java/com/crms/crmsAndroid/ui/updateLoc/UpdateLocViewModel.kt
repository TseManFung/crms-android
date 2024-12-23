package com.crms.crmsAndroid.ui.updateLoc

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class UpdateLocViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is update loc Fragment"
    }
    val text: LiveData<String> = _text
}