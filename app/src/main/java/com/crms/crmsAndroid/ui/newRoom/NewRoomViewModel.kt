package com.crms.crmsAndroid.ui.newRoom

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crms.crmsAndroid.SharedViewModel
import com.crms.crmsAndroid.api.repository.CampusRepository
import com.crms.crmsAndroid.api.repository.RoomRepository
import com.crms.crmsAndroid.api.requestResponse.Room.GetRoomResponse
import com.crms.crmsAndroid.api.requestResponse.campus.GetCampusResponse
import kotlinx.coroutines.launch
import kotlin.Result

class NewRoomViewModel : ViewModel() {
    // Scanned RFID data
    private val _items = MutableLiveData<List<String>>()
    val items: LiveData<List<String>> get() = _items

    private val repository = CampusRepository()

    // Campus data
    private val _campuses = MutableLiveData<List<GetCampusResponse.Campus>>()
    val campuses: LiveData<List<GetCampusResponse.Campus>> = _campuses

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // Room data
    private val roomRepository = RoomRepository()
    private val _rooms = MutableLiveData<List<GetRoomResponse.SingleRoomResponse>>()
    val rooms: LiveData<List<GetRoomResponse.SingleRoomResponse>> = _rooms

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
                Log.d("ViewModel", "API Response Rooms: $rooms")
                _rooms.value = rooms
            }.onFailure { exception ->
                Log.e("ViewModel", "API Error: ${exception.message}")
                _errorMessage.value = "Failed to load rooms: ${exception.message}"
            }
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

    private val _submitStatus = MutableLiveData<Pair<Boolean, String?>>()
    val submitStatus: LiveData<Pair<Boolean, String?>> = _submitStatus

    fun submitData(roomID: Int) {
        viewModelScope.launch {
            val rfids = _items.value ?: emptyList()
            if (rfids.isEmpty()) {
                _submitStatus.value = Pair(false, "Please scan RFID tags first")
                return@launch
            }

            var allSuccess = true
            val errorMessages = mutableListOf<String>()

            rfids.forEach { tagInfo ->
                val tid = tagInfo.split("\n").find { it.startsWith("TID:") }?.substringAfter(":")?.trim()
                tid?.let { tagId ->
                    roomRepository.newRoom(token, roomID, tagId).fold(
                        onSuccess = { success ->
                            if (!success) {
                                allSuccess = false
                                errorMessages.add("Tag $tagId save failed (server error)")
                            }
                        },
                        onFailure = { exception ->
                            allSuccess = false
                            errorMessages.add("Tag $tagId error: ${exception.message ?: "Unknown network error"}")
                        }
                    )
                }
            }

            _submitStatus.value = if (allSuccess) {
                Pair(true, "All items saved successfully!")
            } else {
                Pair(false, errorMessages.joinToString("\n"))
            }
        }
    }
}