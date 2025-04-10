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
    // List of submitted tags
    private val _scannedTags = mutableSetOf<String>()
    val submittedTags = mutableSetOf<String>()

    fun isTagScannedOrSubmitted(tid: String): Boolean {
        return tid in _scannedTags || tid in submittedTags
    }

    fun addScannedTag(tid: String) {
        _scannedTags.add(tid)
    }


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

    private suspend fun submitItem(roomID: Int, tagId: String): Boolean {
        return roomRepository.newRoom(token, roomID, tagId).getOrThrow()
    }

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
        val tid = extractTidFromItem(item)

        // Check if the item is already submitted
        if (tid != null && !submittedTags.contains(tid)) {
            currentItems.add(item)
            _items.value = currentItems
            Log.d("ViewModel", "Added item: $item, Total: ${currentItems.size}")
        } else {
            Log.d("ViewModel", "Skipped already submitted RFID: $tid")
        }
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

    //reset submit status
    fun resetSubmitStatus() {
        _submitStatus.value = Pair(false, null) // 通过 MutableLiveData 修改
    }

    fun submitData(roomID: Int) {
        viewModelScope.launch {
            val rfids = _items.value ?: emptyList()

            if (rfids.isEmpty()) {
                _submitStatus.value = Pair(false, "Please scan RFID tags first")
                return@launch
            }

            val errorMessages = mutableListOf<String>()

            rfids.forEach { tagInfo ->
                val tid = extractTidFromItem(tagInfo)
                if (tid == null) {
                    errorMessages.add("Invalid tag format: ${tagInfo.take(20)}...")
                    return@forEach
                }

                try {
                    val result = roomRepository.newRoom(token, roomID, tid)
                    result.fold(
                        onSuccess = { success ->
                            if (!success) {
                                errorMessages.add("Tag $tid save failed (server rejected)")
                            }
                        },
                        onFailure = { exception ->
                            errorMessages.add("Tag $tid error: ${exception.message ?: "Unknown network error"}")
                        }
                    )
                } catch (e: Exception) {
                    errorMessages.add("Tag $tid processing exception: ${e.message ?: "Unknown error"}")
                }
            }

            val finalMessage = if (errorMessages.isEmpty()) {
                submittedTags.addAll(_scannedTags)
                _scannedTags.clear()
                "Successfully submitted ${rfids.size} tags!"
            } else {
                "Failed to submit tags:\n${errorMessages.joinToString("\n")}"
            }

            _submitStatus.postValue(
                Pair(errorMessages.isEmpty(), finalMessage)
            )

            _items.postValue(emptyList())
        }
    }

    fun submitSingleItem(roomID: Int, tagId: String) {
        viewModelScope.launch {
            try {
                if (submitItem(roomID, tagId)) {

                    submittedTags.add(tagId)
                    _scannedTags.remove(tagId)
                }
            } catch (e: Exception) {
                _submitStatus.postValue(Pair(false, "Submission failed: ${e.message ?: "Network error"}"))
            } finally {

                val remainingItems = _items.value?.filter { item ->
                    !item.contains("TID: $tagId")
                } ?: emptyList()
                _items.postValue(remainingItems)
            }
        }
    }

    private fun extractTidFromItem(item: String): String? {
        return item.split("\n")
            .find { it.startsWith("TID:") }
            ?.substringAfter(":")
            ?.trim()
    }

    fun clearAllData() {
        _scannedTags.clear()
        submittedTags.clear()
        _items.value = emptyList()
    }


}