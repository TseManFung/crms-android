package com.crms.crmsAndroid.ui.manInventory

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ManInventoryViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is manInventoryman Fragment"
    }
    val text: LiveData<String> = _text
}