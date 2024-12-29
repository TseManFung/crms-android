package com.crms.crmsAndroid

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TestRfidViewModel : ViewModel() {
    private val _items = MutableLiveData<MutableList<String>>(mutableListOf())
    val items: LiveData<MutableList<String>> get() = _items

    fun addItem(item: String) {
        _items.value?.add(item)
        _items.value = _items.value // Trigger LiveData update
    }


}