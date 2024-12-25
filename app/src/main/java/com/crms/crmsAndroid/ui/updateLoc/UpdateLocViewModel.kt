package com.crms.crmsAndroid.ui.updateLoc

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class UpdateLocViewModel : ViewModel() {
    private val _items = MutableLiveData<MutableList<String>>(mutableListOf())
    val items: LiveData<MutableList<String>> get() = _items

    fun addItem(item: String) {
        _items.value?.add(item)
        _items.value = _items.value
    }

    fun updateItem(tid: String, newMessage: String) {
        _items.value?.let { currentItems ->
            for (i in currentItems.indices) {
                if (currentItems[i].contains(tid)) {
                    currentItems[i] = newMessage
                    break
                }
            }
            _items.value = currentItems
        }
    }

    fun clearItems() {
        _items.value?.clear()
        _items.postValue(_items.value)
    }

}