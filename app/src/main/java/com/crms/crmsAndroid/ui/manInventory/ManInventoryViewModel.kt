package com.crms.crmsAndroid.ui.manInventory

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crms.crmsAndroid.api.repository.CampusRepository
import com.crms.crmsAndroid.api.repository.RoomRepository
import com.crms.crmsAndroid.api.requestResponse.Room.GetRoomResponse
import com.crms.crmsAndroid.api.requestResponse.campus.GetCampusResponse
import kotlinx.coroutines.launch

class ManInventoryViewModel : ViewModel() {
    private val _items = MutableLiveData<List<String>>()
    val items: LiveData<List<String>> get() = _items

    private val repository = CampusRepository()
    //Campus data
    private val _campuses = MutableLiveData<List<GetCampusResponse.Campus>>()
    val campuses: LiveData<List<GetCampusResponse.Campus>> = _campuses

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    //Room data
    private val roomRepository = RoomRepository()
    private val _rooms = MutableLiveData<List<GetRoomResponse.SingleRoomResponse>>()
    val rooms: LiveData<List<GetRoomResponse.SingleRoomResponse>> = _rooms
    // Hardcoded token
    private val hardcodedToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyMzAxMDQ1NzciLCJhY2Nlc3NMZXZlbCI6MTAwLCJpYXQiOjE3NDM1MTQ1NzcsImV4cCI6MTc0NDUxNDU3N30.5Quz04a15xC7JbfKDOJ5i_zygjW_6zXMLEr4hptyVpE"

    init {
        fetchCampuses()
    }

    fun fetchCampuses() {
        viewModelScope.launch {
            val result = repository.getCampuses(hardcodedToken)
            result.onSuccess { campuses ->
                Log.d("ViewModel", "Fetched ${campuses.size} campuses")
                _campuses.value = campuses
            }.onFailure { exception ->
                Log.e("ViewModel", "Error fetching campuses", exception)
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