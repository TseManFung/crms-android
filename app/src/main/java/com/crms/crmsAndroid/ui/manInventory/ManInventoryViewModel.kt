package com.crms.crmsAndroid.ui.manInventory

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.crms.crmsAndroid.SharedViewModel
import com.crms.crmsAndroid.api.repository.CampusRepository
import com.crms.crmsAndroid.api.repository.ManualInventoryRepository
import com.crms.crmsAndroid.api.repository.RoomRepository
import com.crms.crmsAndroid.api.requestResponse.Room.GetRoomResponse
import com.crms.crmsAndroid.api.requestResponse.campus.GetCampusResponse
import com.crms.crmsAndroid.api.requestResponse.item.ManualInventoryResponse
import kotlinx.coroutines.launch

class ManInventoryViewModel : ViewModel() {
    //scanned data
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
    //Manual Inventory Data
    private val manualInventoryRepo = ManualInventoryRepository()
    private var _manualInventoryResult:MutableLiveData<Result<ManualInventoryResponse>> = MutableLiveData<Result<ManualInventoryResponse>>()
    val manualInventoryResult: LiveData<Result<ManualInventoryResponse>> get() = _manualInventoryResult

    lateinit var sharedViewModel: SharedViewModel
    private val token: String get() = sharedViewModel.token



    fun fetchCampuses() {
        viewModelScope.launch {
            val result = repository.getCampuses(token)
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
            val result = roomRepository.getRooms(token, campusID)
            result.onSuccess { rooms ->
                Log.d("ViewModel", "API Response Rooms: $rooms") // 添加此行
                _rooms.value = rooms
            }.onFailure { exception ->
                Log.e("ViewModel", "API Error: ${exception.message}") // 添加此行
                _errorMessage.value = "Failed to load rooms: ${exception.message}"
            }
        }
    }

    fun sendManualInventory(rfidList: List<String>, roomId: Int) {
        viewModelScope.launch {
            val result = manualInventoryRepo.manualInventory(
                token = token,
                manualInventoryLists = rfidList,
                roomID = roomId
            )
            _manualInventoryResult.postValue(result)
        }
    }

    fun addItem(item: String) {
        val currentItems = _items.value.orEmpty().toMutableList()
        currentItems.add(item)
        _items.value = currentItems
        Log.d("ViewModel", "Added item: $item, Total: ${currentItems.size}")
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

    fun resetResult() {
        _manualInventoryResult =MutableLiveData<Result<ManualInventoryResponse>>()
    }

}