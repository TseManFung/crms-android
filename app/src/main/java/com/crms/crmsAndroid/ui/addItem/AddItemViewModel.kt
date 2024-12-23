package com.crms.crmsAndroid.ui.addItem

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AddItemViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is add item Fragment"
    }
    val text: LiveData<String> = _text
}