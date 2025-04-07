package com.crms.crmsAndroid.ui.search

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crms.crmsAndroid.SharedViewModel
import com.crms.crmsAndroid.api.repository.CampusRepository
import com.crms.crmsAndroid.api.repository.DeviceRepository
import com.crms.crmsAndroid.api.repository.RoomRepository
import com.crms.crmsAndroid.api.requestResponse.Room.GetRoomResponse
import com.crms.crmsAndroid.api.requestResponse.campus.GetCampusResponse
import com.crms.crmsAndroid.api.requestResponse.item.GetItemResponse
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private val campusRepository = CampusRepository()
    private val roomRepository = RoomRepository()
    private val deviceRepository = DeviceRepository()

    lateinit var sharedViewModel: SharedViewModel
    private val token: String get() = sharedViewModel.token

    private val _campuses = MutableLiveData<List<GetCampusResponse.Campus>>()
    val campuses: LiveData<List<GetCampusResponse.Campus>> = _campuses

    private val _rooms = MutableLiveData<List<GetRoomResponse.SingleRoomResponse>>()
    val rooms: LiveData<List<GetRoomResponse.SingleRoomResponse>> = _rooms

    private val _devices = MutableLiveData<List<GetItemResponse.Devices>>()
    val devices: LiveData<List<GetItemResponse.Devices>> = _devices

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _items = MutableLiveData<List<String>>()
    val items: LiveData<List<String>> get() = _items

    private val hardcodedToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyMzAxMDQ1NzciLCJhY2Nlc3NMZXZlbCI6MTAwLCJpYXQiOjE3NDM4MDI5OTcsImV4cCI6MTc0NDgwMjk5N30.2_YenK__fFXbz6a5Cjt5C3FcO1oG6c1CViqhFNFIIGs"

    init {
        fetchCampuses()
    }

    fun fetchCampuses() {
        viewModelScope.launch {
            val result = campusRepository.getCampuses(hardcodedToken)
            result.onSuccess { campuses ->
                _campuses.value = campuses
            }.onFailure { exception ->
                _errorMessage.value = "Failed to load campuses: ${exception.message}"
            }
        }
    }

    fun fetchRooms(campusID: Int) {
        viewModelScope.launch {
            val result = roomRepository.getRooms(hardcodedToken, campusID)
            result.onSuccess { rooms ->
                _rooms.value = rooms
            }.onFailure { exception ->
                _errorMessage.value = "Failed to load rooms: ${exception.message}"
            }
        }
    }

    fun fetchDevices(roomID: Int) {
        viewModelScope.launch {
            val result = deviceRepository.getItems(hardcodedToken, roomID, emptyList())
            result.onSuccess { devices ->
                _devices.value = devices
            }.onFailure { exception ->
                _errorMessage.value = "Failed to load devices: ${exception.message}"
            }
        }
    }

    fun addItem(item: String) {
        val currentItems = _items.value.orEmpty().toMutableList()
        currentItems.add(item)
        _items.value = currentItems
    }

    fun updateItem(tid: String, item: String) {
        val currentItems = _items.value.orEmpty().toMutableList()
        val index = currentItems.indexOfFirst { it.contains(tid) }
        if (index != -1) {
            currentItems[index] = item
            _items.value = currentItems
        }
    }

    fun clearItems() {
        _items.value = emptyList()
    }
}