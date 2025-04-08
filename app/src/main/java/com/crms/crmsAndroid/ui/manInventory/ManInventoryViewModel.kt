// ManInventoryViewModel.kt
package com.crms.crmsAndroid.ui.manInventory

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crms.crmsAndroid.SharedViewModel
import com.crms.crmsAndroid.api.repository.CampusRepository
import com.crms.crmsAndroid.api.repository.RoomRepository
import com.crms.crmsAndroid.api.repository.ManualInventoryRepository
import com.crms.crmsAndroid.api.repository.DeviceRepository
import com.crms.crmsAndroid.api.requestResponse.Room.GetRoomResponse
import com.crms.crmsAndroid.api.requestResponse.campus.GetCampusResponse
import com.crms.crmsAndroid.api.requestResponse.item.ManualInventoryResponse
import kotlinx.coroutines.launch

class ManInventoryViewModel : ViewModel() {
    private val campusRepo = CampusRepository()
    private val roomRepo = RoomRepository()
    private val deviceRepo = DeviceRepository()
    private val manualInventoryRepo = ManualInventoryRepository()

    private val _campuses = MutableLiveData<List<GetCampusResponse.Campus>>()
    val campuses: LiveData<List<GetCampusResponse.Campus>> = _campuses

    private val _rooms = MutableLiveData<List<GetRoomResponse.SingleRoomResponse>>()
    val rooms: LiveData<List<GetRoomResponse.SingleRoomResponse>> = _rooms

    private val _tagInfo = MutableLiveData<TagInfo>()
    val tagInfo: LiveData<TagInfo> = _tagInfo

    private val _manualInventoryResult = MutableLiveData<Result<ManualInventoryResponse>>()
    val manualInventoryResult: LiveData<Result<ManualInventoryResponse>> = _manualInventoryResult

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    lateinit var sharedViewModel: SharedViewModel
    private val token: String get() = sharedViewModel.token

    fun fetchCampuses() {
        viewModelScope.launch {
            campusRepo.getCampuses(token).fold(
                onSuccess = { _campuses.value = it },
                onFailure = { _errorMessage.value = "Failed to load campuses: ${it.message}" }
            )
        }
    }

    fun fetchRooms(campusId: Int) {
        viewModelScope.launch {
            roomRepo.getRooms(token, campusId).fold(
                onSuccess = { _rooms.value = it },
                onFailure = { _errorMessage.value = "Failed to load rooms: ${it.message}" }
            )
        }
    }

    fun getItemByRFID(rfid: String) {
        viewModelScope.launch {
            deviceRepo.getItemByRFID(token, rfid).fold(
                onSuccess = {
                    _tagInfo.value = TagInfo(
                        rfid = rfid,
                        deviceName = it.deviceName,
                        devicePartName = it.devicePartName,
                        state = it.deviceState
                    )
                },
                onFailure = {
                    _tagInfo.value = TagInfo(
                        rfid = rfid,
                        deviceName = "Unknown",
                        devicePartName = "N/A",
                        state = "Not Found"
                    )
                }
            )
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

// TagInfo.kt
data class TagInfo(
    val rfid: String,
    val deviceName: String,
    val devicePartName: String,
    val state: String
)