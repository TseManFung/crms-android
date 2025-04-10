package com.crms.crmsAndroid

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TestRfidViewModel : ViewModel() {
    private val itemsList:MutableList<String> = mutableListOf("","","")
    private val _items = MutableLiveData<MutableList<String>>(itemsList)
    val items: LiveData<MutableList<String>> get() = _items

    fun addItem(item: String) {
        itemsList.add(item)
        _items.value = _items.value // Trigger LiveData update
    }

    fun updateItem(i: Int, newMessage: String) {
        itemsList[i] = newMessage
        _items.value = itemsList // Trigger LiveData update
    }

}