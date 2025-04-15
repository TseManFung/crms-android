package com.crms.crmsAndroid.ui.deleteItem

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.crms.crmsAndroid.MainActivity
import com.crms.crmsAndroid.R
import com.crms.crmsAndroid.SharedViewModel
import com.crms.crmsAndroid.api.requestResponse.Room.GetRoomResponse
import com.crms.crmsAndroid.api.requestResponse.campus.GetCampusResponse
import com.crms.crmsAndroid.api.requestResponse.item.DeleteItemResponse
import com.crms.crmsAndroid.databinding.FragmentDeleteItemBinding
import com.crms.crmsAndroid.scanner.rfidScanner
import com.crms.crmsAndroid.ui.ITriggerDown
import com.crms.crmsAndroid.ui.ITriggerLongPress
import kotlinx.coroutines.launch


class DeleteItemFragment : Fragment(), ITriggerDown, ITriggerLongPress {
    private var _binding: FragmentDeleteItemBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DeleteItemViewModel
    private  var roomID:Int?=null
    private var sendToBackend:Boolean = false


    private lateinit var listAdapter:ArrayAdapter<String>
    private lateinit var mainActivity: MainActivity
    private lateinit var objRfidScanner: rfidScanner
    private lateinit var campusAdapter: ArrayAdapter<String>
    private lateinit var roomAdapter: ArrayAdapter<String>

    private val items = mutableListOf<String>()
    private val scannedTags = mutableSetOf<String>()
    private val tagInfoMap = mutableMapOf<String, String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeleteItemBinding.inflate(inflater, container, false)
        mainActivity = requireActivity() as MainActivity
        objRfidScanner = mainActivity.objRfidScanner
        viewModel = ViewModelProvider(this).get(DeleteItemViewModel::class.java)
        val sharedViewModel = ViewModelProvider(mainActivity).get(SharedViewModel::class.java)
        viewModel.sharedViewModel = sharedViewModel
        setupUI()
        setupObservers()
        viewModel.fetchCampuses()
        return binding.root
    }

    private fun setupUI() {
        showScanButton()
        // Initialize ListView
        listAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, items)
        binding.lvSearchResult.adapter = listAdapter



        binding.lvSearchResult.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedItem = listAdapter.getItem(position)
            selectedItem?.let {
                showSingleItemConfirmationDialog(it)
            } ?: run {
                Toast.makeText(context, "Selected item is invalid", Toast.LENGTH_SHORT).show()
            }
        }


        // Initialize Campus Spinner
        campusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf())
        campusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnCampus.adapter = campusAdapter

        // Initialize Room Spinner
        roomAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf())
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnRoom.adapter = roomAdapter

        // Set up send to backend button


        // Campus Spinner selection listener
        binding.spnCampus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                resetAllData()
                StopScanning()
                val selectedCampus = viewModel.campuses.value?.get(position)
                Log.d("Fragment", "Selected Campus ID: ${selectedCampus?.campusId}")
                selectedCampus?.campusId?.let { campusId ->
                    viewModel.fetchRooms(campusId)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        //Rooms Spinner selection listener
        binding.spnRoom.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                clearAllData()
                StopScanning()
                roomID = viewModel.rooms.value?.get(position)?.room ?: 0
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }










        // Set up buttons
        binding.btnSearch.setOnClickListener {

            handleBtnScanClick(objRfidScanner)
        }
        binding.btnStop.setOnClickListener {
            if (binding.btnStop.text == "Stop") {
                objRfidScanner.stopReadTagLoop()
                binding.btnStop.text = "Clear"
            } else {
                clearAllData()

                sendToBackend = true
            }
        }

    }



    private fun showScanButton() {
        binding.btnSearch.visibility = View.VISIBLE
        binding.linearLayoutStopClear.visibility = View.GONE

    }

    private fun updateButtonStates() {
        binding.linearLayoutStopClear.visibility = if (objRfidScanner.loopFlag) View.VISIBLE else View.GONE

    }
    private fun setupObservers() {
        viewModel.items.observe(viewLifecycleOwner) { newItems ->
            items.clear()
            items.addAll(newItems)
            listAdapter.notifyDataSetChanged()
            binding.lvSearchResult.visibility = if (newItems.isNotEmpty()) View.VISIBLE else View.GONE
        }


        viewModel.campuses.observe(viewLifecycleOwner) { campuses ->
            campuses?.let {
                Log.d("Fragment", "Received ${campuses.size} campuses")
                updateCampusSpinner(campuses)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.rooms.observe(viewLifecycleOwner) { rooms ->
            rooms?.let {
                Log.d("Fragment", "Received ${rooms.size} rooms: ${rooms.map { it.roomName }}")
                updateRoomSpinner(rooms)
            }
        }


    }

    private fun updateCampusSpinner(campuses: List<GetCampusResponse.Campus>) {
        val campusShortName = campuses.map { it.campusShortName ?: "Unknown" }
        campusAdapter.clear()
        campusAdapter.addAll(campusShortName)
        campusAdapter.notifyDataSetChanged()

        if (campusShortName.isNotEmpty()) {
            binding.spnCampus.setSelection(0)
        }
    }

    private fun updateRoomSpinner(rooms: List<GetRoomResponse.SingleRoomResponse>) {
        val roomNames = rooms.map { "${it.roomName} (Room ${it.roomNumber})" ?: "Unknown" }
        Log.d("Fragment", "Room names: $roomNames")
        roomAdapter.clear()
        roomAdapter.addAll(roomNames)
        roomAdapter.notifyDataSetChanged()

        if (roomNames.isNotEmpty()) {
            binding.spnRoom.setSelection(0)
        }
    }



    private fun handleBtnScanClick(rfidScanner: rfidScanner) {
        try {
            if (sendToBackend) {
                clearAllData()
                sendToBackend = false
            }

            // Get the selected room ID
            val selectedRoomPosition = binding.spnRoom.selectedItemPosition
            val selectedRoomID = viewModel.rooms.value?.get(selectedRoomPosition)?.room

            if (selectedRoomID == null) {
                Toast.makeText(context, "Please select a room", Toast.LENGTH_SHORT).show()
                return
            }

            rfidScanner.readTagLoop(viewLifecycleOwner.lifecycleScope) { tag ->
                val currentTid = tag.tid
                val currentRFID = tag.epc
                if (!scannedTags.contains(currentTid)) {
                    scannedTags.add(currentTid)
                    lifecycleScope.launch {
                        val result = viewModel.getDeviceByRFID(currentTid)
                        result.onSuccess { response ->
                            // Check if the item's roomID matches the selected roomID
                            if (response.roomID == selectedRoomID) {
                                val displayText = "DeviceID:${response.deviceID}-${response.deviceName}-${response.devicePartName}-${response.deviceState}"
                                tagInfoMap[currentTid] = displayText
                                viewModel.addItem(displayText)
                                sortItemsByDeviceID()
                                listAdapter.notifyDataSetChanged()
                            }
                        }.onFailure { exception ->
                            Log.e("Fragment", "Error fetching device by RFID: ${exception.message}")
                        }
                    }
                }
            }
            binding.linearLayoutStopClear.visibility = View.VISIBLE
        } catch (e: Exception) {
            appendTextToList("Error: ${e.message}")
        }
    }

    private fun sortItemsByDeviceID() {
        items.sortWith(compareBy { extractDeviceIDFromItem(it) ?: Int.MAX_VALUE })
    }



    private fun sendDataToBackend() {
        val rfidList = scannedTags.toList()
        val selectedRoomPosition = binding.spnRoom.selectedItemPosition
        roomID = viewModel.rooms.value?.get(selectedRoomPosition)?.room ?: run {
            Toast.makeText(context, "Please select a room", Toast.LENGTH_SHORT).show()
            return
        }


    }






    private inner class CustomAdapter : ArrayAdapter<Triple<String, Char, Int>>(
        requireContext(),
        android.R.layout.simple_list_item_1
    ) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)
            val item = getItem(position)
            view.setBackgroundResource(item?.third ?: android.R.color.transparent)
            return view
        }
    }

    private fun resetAllData() {
        scannedTags.clear()
        tagInfoMap.clear()
        viewModel.clearItems()
        items.clear()
        listAdapter.notifyDataSetChanged()

        binding.btnStop.text = "Stop"
        binding.linearLayoutStopClear.visibility = View.GONE
        binding.lvSearchResult.visibility = View.GONE

        roomAdapter.clear()
        roomAdapter.notifyDataSetChanged()

        objRfidScanner.stopReadTagLoop()
    }

    //delete item
    private fun showSingleItemConfirmationDialog(itemInfo: String) {
        val deviceID = extractDeviceIDFromItem(itemInfo)

        if (deviceID == null) {
            Toast.makeText(context, "Invalid item format", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to delete this item?\nDevice ID: $deviceID")
            .setPositiveButton("Confirm") { _, _ ->
                deleteSingleItem(deviceID)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }



    private fun deleteSingleItem(deviceID: Int) {
        lifecycleScope.launch {
            try {
                val success = viewModel.deleteItem(deviceID)
                if (success) {
                    Toast.makeText(context, "Item deleted successfully", Toast.LENGTH_SHORT).show()
                    // remove the item from the list
                    val filteredItems = items.filterNot { extractDeviceIDFromItem(it) == deviceID }
                    items.clear()
                    items.addAll(filteredItems)
                    listAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(context, "Failed to delete item", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun extractDeviceIDFromItem(item: String): Int? {
        return item.split("-")
            .find { it.startsWith("DeviceID:") }
            ?.substringAfter(":")
            ?.trim()
            ?.toIntOrNull()
    }



    private fun clearAllData() {
        scannedTags.clear()
        tagInfoMap.clear()
        viewModel.clearItems()
        items.clear()
        binding.lvSearchResult.visibility = View.GONE
        binding.linearLayoutStopClear.visibility = View.GONE
        binding.btnStop.text = "Stop"

    }



    private fun appendTextToList(text: String) {
        items.add(text)
        listAdapter.notifyDataSetChanged()
    }

    override fun onPause() {
        super.onPause()
        objRfidScanner.stopReadTagLoop()
        scannedTags.clear()
        tagInfoMap.clear()
    }

    override fun onResume() {
        super.onResume()
        viewModel.clearItems()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onTriggerLongPress() {
        if(sendToBackend == true){
            clearAllData()
            sendToBackend = false
        }
        if (!objRfidScanner.loopFlag) {
            handleBtnScanClick(objRfidScanner)
        }
    }



    override fun onTriggerRelease() {
        objRfidScanner.stopReadTagLoop()
        updateButtonStates()

    }

    override fun onTriggerDown() {
        if(sendToBackend == true){
            clearAllData()
            sendToBackend = false
        }
        handleBtnScanClick(objRfidScanner)
        updateButtonStates()

    }

    private fun StopScanning() {
        objRfidScanner.stopReadTagLoop()
    }
}