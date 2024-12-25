package com.crms.crmsAndroid.ui.deleteItem

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DeleteItemViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is delete item Fragment"
    }
    val text: LiveData<String> = _text
}