package com.crms.crmsAndroid.ui.updateItem

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class UpdateItemViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is update item Fragment"
    }
    val text: LiveData<String> = _text
}