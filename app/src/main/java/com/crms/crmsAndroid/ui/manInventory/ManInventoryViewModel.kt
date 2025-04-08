package com.crms.crmsAndroid.ui.manInventory

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crms.crmsAndroid.SharedViewModel
import com.crms.crmsAndroid.api.repository.*
import com.crms.crmsAndroid.api.requestResponse.Room.GetRoomResponse
import com.crms.crmsAndroid.api.requestResponse.campus.GetCampusResponse
import com.crms.crmsAndroid.api.requestResponse.item.*
import kotlinx.coroutines.launch
import java.lang.Exception

class ManInventoryViewModel : ViewModel() {
    private val repository = CampusRepository()
    private val roomRepo = RoomRepository()
    private val deviceRepo = DeviceRepository()
    private val manualInventoryRepo = ManualInventoryRepository()

    private val _campuses = MutableLiveData<List<GetCampusResponse.Campus>>()
    val campuses: LiveData<List<GetCampusResponse.Campus>> = _campuses

    private val _rooms = MutableLiveData<List<GetRoomResponse.SingleRoomResponse>>()
    val rooms: LiveData<List<GetRoomResponse.SingleRoomResponse>> = _rooms

    private val _deviceInfo = MutableLiveData<Pair<String, String>>()
    val deviceInfo: LiveData<Pair<String, String>> = _deviceInfo

    private val _manualInventoryResult = MutableLiveData<Result<ManualInventoryResponse>>()
    val manualInventoryResult: LiveData<Result<ManualInventoryResponse>> = _manualInventoryResult

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    lateinit var sharedViewModel: SharedViewModel
    private val token: String get() = sharedViewModel.token

    fun fetchCampuses() {
        viewModelScope.launch {
            repository.getCampuses(token).fold(
                onSuccess = { _campuses.value = it },
                onFailure = { _errorMessage.value = "Failed to load campuses: ${it.message}" }
            )
        }
    }

    fun fetchRooms(campusID: Int) {
        viewModelScope.launch {
            roomRepo.getRooms(token, campusID).fold(
                onSuccess = { _rooms.value = it },
                onFailure = { _errorMessage.value = "Failed to load rooms: ${it.message}" }
            )
        }
    }

    fun fetchDeviceInfo(rfid: String) {
        viewModelScope.launch {
            try {
                val response = deviceRepo.getItemByRFID(token, rfid)
                response.onSuccess {
                    val displayText = "${it.deviceName} - ${it.devicePartName} - ${it.deviceState}"
                    // Do something with displayText
                }.onFailure { e ->
                    val displayText = "Error: ${e.message}"
                    // Handle the error
                }
            } catch (e: Exception) {
                _deviceInfo.postValue(Pair(rfid, "Unknown Device - ${e.message}"))
            }
        }
    }

    fun sendManualInventory(rfidList: List<String>, roomId: Int) {
        viewModelScope.launch {
            manualInventoryRepo.manualInventory(token, rfidList, roomId).fold(
                onSuccess = { _manualInventoryResult.value = Result.success(it) },
                onFailure = { _manualInventoryResult.value = Result.failure(it) }
            )
        }
    }
}