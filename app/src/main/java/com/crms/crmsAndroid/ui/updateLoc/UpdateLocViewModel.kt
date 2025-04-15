package com.crms.crmsAndroid.ui.updateLoc

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crms.crmsAndroid.SharedViewModel
import com.crms.crmsAndroid.api.repository.CampusRepository
import com.crms.crmsAndroid.api.repository.DeviceRepository
import com.crms.crmsAndroid.api.repository.RoomRepository
import com.crms.crmsAndroid.api.repository.updateLocationRepository
import com.crms.crmsAndroid.api.requestResponse.Room.GetRoomResponse
import com.crms.crmsAndroid.api.requestResponse.campus.GetCampusResponse
import com.crms.crmsAndroid.api.requestResponse.item.GetItemByRFIDResponse
import com.crms.crmsAndroid.api.requestResponse.item.updateLocationByRFIDResponse
import kotlinx.coroutines.launch

class UpdateLocViewModel : ViewModel() {

    private val deviceRepository = DeviceRepository()


    // Scanned data
    private val _items = MutableLiveData<MutableList<String>>(mutableListOf())
    val items: LiveData<MutableList<String>> get() = _items

    // Campus data
    private val campusRepository = CampusRepository()
    private val _campuses = MutableLiveData<List<GetCampusResponse.Campus>>()
    val campuses: LiveData<List<GetCampusResponse.Campus>> = _campuses

    // Room data
    private val roomRepository = RoomRepository()
    private val _rooms = MutableLiveData<List<GetRoomResponse.SingleRoomResponse>>()
    val rooms: LiveData<List<GetRoomResponse.SingleRoomResponse>> = _rooms

    // Update location result
    private val updateLocationRepo = updateLocationRepository()
    private val _updateResult = MutableLiveData<Result<updateLocationByRFIDResponse>>()
    val updateResult: LiveData<Result<updateLocationByRFIDResponse>> = _updateResult

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    lateinit var sharedViewModel: SharedViewModel
    private val token: String get() = sharedViewModel.token

    fun fetchCampuses() {
        viewModelScope.launch {
            val result = campusRepository.getCampuses(token)
            result.onSuccess { campuses ->
                _campuses.value = campuses
            }.onFailure { exception ->
                _errorMessage.value = "Failed to load campuses: ${exception.message}"
            }
        }
    }

    fun fetchRooms(campusID: Int) {
        viewModelScope.launch {
            val result = roomRepository.getRooms(token, campusID)
            result.onSuccess { rooms ->
                _rooms.value = rooms
            }.onFailure { exception ->
                _errorMessage.value = "Failed to load rooms: ${exception.message}"
            }
        }
    }

    fun updateItemLocation(roomID: Int, itemList: List<String>) {
        viewModelScope.launch {
            val result = updateLocationRepo.updateLocation(token, roomID, itemList)
            _updateResult.postValue(result)
        }
    }

    suspend fun getDeviceByRFID(rfid: String): Result<GetItemByRFIDResponse> {
        return deviceRepository.getItemByRFID(token, rfid)
    }

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