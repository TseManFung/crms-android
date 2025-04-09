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
            val successList = mutableListOf<String>()
            val errorMessages = mutableListOf<String>()
            var processedCount = 0

            rfids.forEach { tagInfo ->
                val tid = extractTidFromItem(tagInfo)
                if (tid == null) {
                    errorMessages.add("Invalid tag format: ${tagInfo.take(20)}...")
                    allSuccess = false
                    processedCount++
                    return@forEach
                }

                try {
                    val result = roomRepository.newRoom(token, roomID, tid)
                    result.fold(
                        onSuccess = { success ->
                            if (success) {
                                successList.add(tid)
                            } else {
                                errorMessages.add("Tag $tid save failed (server rejected)")
                                allSuccess = false
                            }
                        },
                        onFailure = { exception ->
                            errorMessages.add("Tag $tid error: ${exception.message ?: "Unknown network error"}")
                            allSuccess = false
                        }
                    )
                } catch (e: Exception) {
                    errorMessages.add("Tag $tid processing exception: ${e.message ?: "Unknown error"}")
                    allSuccess = false
                } finally {
                    processedCount++
                }

                if (processedCount % 5 == 0) {
                    _submitStatus.postValue(
                        Pair(
                            false,
                            "Processing... ($processedCount/${rfids.size})"
                        )
                    )
                }
            }

            _submitStatus.value = if (allSuccess) {
                val remainingItems = _items.value?.filter { item ->
                    !successList.any { tid ->
                        item.contains("TID: $tid")
                    }
                } ?: emptyList()
                _items.postValue(remainingItems)

                Pair(
                    true,
                    "Successfully submitted ${successList.size} items!\n" +
                            "• Room ID: $roomID\n" +
                            "• Successful tags: ${successList.take(3).joinToString()}${if (successList.size > 3) "..." else ""}"
                )
            } else {
                val errorSummary = when {
                    errorMessages.size == rfids.size -> "All submissions failed"
                    errorMessages.isNotEmpty() -> "${errorMessages.size} failures"
                    else -> "Unknown error"
                }

                Pair(
                    false,
                    "Submission completed (partial failures)\n" +
                            "• Success: ${successList.size}\n" +
                            "• Failures: ${errorMessages.size}\n" +
                            "Error details:\n${errorMessages.take(3).joinToString("\n")}${if (errorMessages.size > 3) "\n..." else ""}"
                )
            }
        }
    }

    fun submitSingleItem(roomID: Int, tagId: String) {
        viewModelScope.launch {
            try {
                val success = submitItem(roomID, tagId)
                val message = if (success) {
                    "Submission successful! TID: $tagId"
                } else {
                    "Submission failed: Server error"
                }
                _submitStatus.postValue(Pair(success, message))
            } catch (e: Exception) {
                _submitStatus.postValue(Pair(false, "Submission failed: ${e.message ?: "Network error"}"))
            }
        }
    }

    private fun extractTidFromItem(item: String): String? {
        return item.split("\n")
            .find { it.startsWith("TID:") }
            ?.substringAfter(":")
            ?.trim()
    }
}