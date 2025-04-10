package com.crms.crmsAndroid.ui.updateLoc

import android.os.Bundle
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
import com.crms.crmsAndroid.SharedViewModel
import com.crms.crmsAndroid.api.requestResponse.Room.GetRoomResponse
import com.crms.crmsAndroid.api.requestResponse.campus.GetCampusResponse
import com.crms.crmsAndroid.databinding.FragmentUpdateLocBinding
import com.crms.crmsAndroid.scanner.rfidScanner
import com.crms.crmsAndroid.ui.ITriggerDown
import com.crms.crmsAndroid.ui.ITriggerLongPress
import com.crms.crmsAndroid.api.requestResponse.item.updateLocationByRFIDResponse
import kotlinx.coroutines.launch

class UpdateLocFragment : Fragment(), ITriggerDown, ITriggerLongPress {
    private var _binding: FragmentUpdateLocBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UpdateLocViewModel by viewModels()

    private lateinit var listAdapter: ArrayAdapter<String>
    private lateinit var campusAdapter: ArrayAdapter<String>
    private lateinit var roomAdapter: ArrayAdapter<String>
    private lateinit var mainActivity: MainActivity
    private lateinit var objRfidScanner: rfidScanner

    private val items = mutableListOf<String>()
    private val scannedTags = mutableSetOf<String>()
    private val tagInfoMap = mutableMapOf<String, String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdateLocBinding.inflate(inflater, container, false)
        mainActivity = requireActivity() as MainActivity
        objRfidScanner = mainActivity.objRfidScanner
        val sharedViewModel = ViewModelProvider(mainActivity).get(SharedViewModel::class.java)
        viewModel.sharedViewModel = sharedViewModel

        setupUI()
        setupObservers()
        viewModel.fetchCampuses()

        return binding.root
    }

    private fun setupUI() {
        // Initialize ListView
        listAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, items)
        binding.lvMain.adapter = listAdapter

        // Initialize Campus Spinner
        campusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf())
        campusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.campusSpinner.adapter = campusAdapter

        // Initialize Room Spinner
        roomAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf())
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.roomSpinner.adapter = roomAdapter

        // Set up buttons
        binding.btnScan.setOnClickListener {
            handleBtnScanClick(objRfidScanner)

        }
        binding.btnStop.setOnClickListener {
            if (binding.btnStop.text == "Stop") {
                objRfidScanner.stopReadTagLoop()
                binding.btnStop.text = "Clear"
            } else {
                clearAllData()
                binding.btnUpdateLocation.visibility = View.GONE
            }
        }

        // Set up update location button
        binding.btnUpdateLocation.setOnClickListener {
            updateItemLocation()
        }

        // Campus Spinner selection listener
        binding.campusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                resetAllData()
                val selectedCampus = viewModel.campuses.value?.get(position)
                selectedCampus?.campusId?.let { campusId ->
                    viewModel.fetchRooms(campusId)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        appendTextToList("RFID 版本: ${objRfidScanner.getVersion()}")
    }

    private fun setupObservers() {
        viewModel.items.observe(viewLifecycleOwner) { newItems ->
            items.clear()
            items.addAll(newItems)
            listAdapter.notifyDataSetChanged()
            binding.btnUpdateLocation.visibility = if (newItems.isNotEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.campuses.observe(viewLifecycleOwner) { campuses ->
            campuses?.let {
                updateCampusSpinner(campuses)
            }
        }

        viewModel.rooms.observe(viewLifecycleOwner) { rooms ->
            rooms?.let {
                updateRoomSpinner(rooms)
            }
        }

        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                handleUpdateResult(it)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateCampusSpinner(campuses: List<GetCampusResponse.Campus>) {
        val campusShortName = campuses.map { it.campusShortName ?: "Unknown" }
        campusAdapter.clear()
        campusAdapter.addAll(campusShortName)
        campusAdapter.notifyDataSetChanged()

        if (campusShortName.isNotEmpty()) {
            binding.campusSpinner.setSelection(0)
        }
    }

    private fun updateRoomSpinner(rooms: List<GetRoomResponse.SingleRoomResponse>) {
        val roomNames = rooms.map { it.roomName ?: "Unknown" }
        roomAdapter.clear()
        roomAdapter.addAll(roomNames)
        roomAdapter.notifyDataSetChanged()

        if (roomNames.isNotEmpty()) {
            binding.roomSpinner.setSelection(0)
        }
    }

    private fun handleBtnScanClick(rfidScanner: rfidScanner) {
        try {

            if (!objRfidScanner.loopFlag) {
                rfidScanner.readTagLoop(viewLifecycleOwner.lifecycleScope) { tag ->
                    val currentTid = tag.tid
                    val message =
                        """ |EPC: ${tag.epc} |TID: ${tag.tid} |RSSI: ${tag.rssi} |Antenna: ${tag.ant} |Index: ${tag.index} |PC: ${tag.pc} |Remain: ${tag.remain} |Reserved: ${tag.reserved} |User: ${tag.user} """.trimMargin()

                    if (!scannedTags.contains(currentTid)) {
                        scannedTags.add(currentTid)
                        tagInfoMap[currentTid] = message
                        viewModel.addItem(message)
                    } else {
                        viewModel.updateItem(currentTid, message)
                    }
                }
                binding.btnStop.text = "Stop"
                binding.btnStop.visibility = View.VISIBLE

            }


        } catch (e: Exception) {
            appendTextToList("Error: ${e.message}")
        }
    }

    private fun updateItemLocation() {
        val rfidList = scannedTags.toList()
        val selectedRoomPosition = binding.roomSpinner.selectedItemPosition
        val roomId = viewModel.rooms.value?.get(selectedRoomPosition)?.room ?: run {
            Toast.makeText(context, "Please select a room", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            viewModel.updateItemLocation(roomId, rfidList)
        }
    }

    private fun handleUpdateResult(result: Result<updateLocationByRFIDResponse>) {
        result.onSuccess { response ->
            showUpdateResults(response.updateLists)
            Toast.makeText(context, "Location updated successfully", Toast.LENGTH_SHORT).show()
        }.onFailure { exception ->
            Toast.makeText(context, "Error: ${exception.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showUpdateResults(updateLists: List<updateLocationByRFIDResponse.updateList>) {
        items.clear()

        updateLists.forEach { item ->
            val statusMessage = when {
                item.successData != null ->
                    "✅ ${item.deviceName} - Success: ${item.successData.something}"
                item.failData != null ->
                    "❌ ${item.deviceName} - Failed: ${item.failData.reason}"
                else ->
                    "❓ ${item.deviceName} - Unknown status"
            }
            items.add(statusMessage)
        }

        listAdapter.notifyDataSetChanged()
    }

    private fun resetAllData() {
        scannedTags.clear()
        tagInfoMap.clear()
        viewModel.clearItems()
        items.clear()
        listAdapter.notifyDataSetChanged()
        binding.btnStop.text = "Stop"
        binding.btnUpdateLocation.visibility = View.GONE
        objRfidScanner.stopReadTagLoop()
    }

    private fun clearAllData() {
        scannedTags.clear()
        tagInfoMap.clear()
        viewModel.clearItems()
        binding.btnStop.visibility = View.GONE
        binding.btnUpdateLocation.visibility = View.GONE
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
        clearAllData()
    }

    override fun onResume() {
        super.onResume()
        viewModel.clearItems()
        clearAllData()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    override fun onTriggerLongPress() {
        if (!objRfidScanner.loopFlag) {
            handleBtnScanClick(objRfidScanner)
        }
    }

    override fun onTriggerRelease() {
        objRfidScanner.stopReadTagLoop()
    }

    override fun onTriggerDown() {
        if (!objRfidScanner.loopFlag) {
            handleBtnScanClick(objRfidScanner)
        }
    }
}