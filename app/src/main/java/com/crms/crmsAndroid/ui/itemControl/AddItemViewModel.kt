package com.crms.crmsAndroid.ui.itemControl

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AddItemViewModel : ViewModel() {
    // TODO: Implement the ViewModel
    private val _text = MutableLiveData<String>().apply {
        value = "This is the AddItemFragment"
    }
    val text: LiveData<String> = _text
}