package com.crms.crmsAndroid.ui.newRoom

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class NewRoomViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is NewRoom Fragment"
    }
    val text: LiveData<String> = _text
}