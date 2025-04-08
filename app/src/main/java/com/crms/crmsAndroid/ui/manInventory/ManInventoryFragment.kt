package com.crms.crmsAndroid.ui.manInventory

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.crms.crmsAndroid.MainActivity
import com.crms.crmsAndroid.R
import com.crms.crmsAndroid.SharedViewModel
import com.crms.crmsAndroid.api.requestResponse.Room.GetRoomResponse
import com.crms.crmsAndroid.api.requestResponse.campus.GetCampusResponse
import com.crms.crmsAndroid.api.requestResponse.item.ManualInventoryResponse
import com.crms.crmsAndroid.databinding.FragmentManInventoryBinding
import com.crms.crmsAndroid.scanner.rfidScanner
import com.crms.crmsAndroid.ui.ITriggerDown
import com.crms.crmsAndroid.ui.ITriggerLongPress
import kotlinx.coroutines.launch

class ManInventoryFragment : Fragment(), ITriggerDown, ITriggerLongPress {
    private var _binding: FragmentManInventoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ManInventoryViewModel by viewModels()

    private lateinit var listAdapter: ArrayAdapter<String>
    private lateinit var mainActivity: MainActivity
    private lateinit var objRfidScanner: rfidScanner
    private lateinit var campusAdapter: ArrayAdapter<String>
    private lateinit var roomAdapter: ArrayAdapter<String>

    private val scannedTags = mutableMapOf<String, String>() // Key: RFID, Value: Display text
    private var currentCampusId: Int? = null
    private var currentRoomId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManInventoryBinding.inflate(inflater, container, false)
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
        //initialize ui
        showScanButton()

        // Initialize ListView
        listAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1)
        binding.lvSearchResult.adapter = listAdapter

        // Initialize Spinners
        campusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item)
        campusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnCampus.adapter = campusAdapter

        roomAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item)
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnRoom.adapter = roomAdapter

        // Button setup
        binding.btnSendToBackend.setOnClickListener {
            if (scannedTags.isEmpty()) {
                Toast.makeText(context, "No items scanned", Toast.LENGTH_SHORT).show()
            } else {
                sendDataToBackend()
            }
        }

        binding.btnStop.setOnClickListener {
            handleStopClearClick()
        }

        binding.spnCampus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                resetAllData()
                viewModel.campuses.value?.get(position)?.campusId?.let { campusId ->
                    currentCampusId = campusId
                    viewModel.fetchRooms(campusId)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spnRoom.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                resetAllData()
                currentRoomId = viewModel.rooms.value?.get(position)?.room
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        updateButtonStates()
    }

    private fun setupObservers() {
        viewModel.deviceInfo.observe(viewLifecycleOwner) { (rfid, info) ->
            scannedTags[rfid] = info
            updateList()
        }

        viewModel.campuses.observe(viewLifecycleOwner) { campuses ->
            campuses?.let { updateCampusSpinner(it) }
        }

        viewModel.rooms.observe(viewLifecycleOwner) { rooms ->
            rooms?.let { updateRoomSpinner(it) }
        }

        viewModel.manualInventoryResult.observe(viewLifecycleOwner) { result ->
            result?.let { handleManualInventoryResult(it) }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun updateCampusSpinner(campuses: List<GetCampusResponse.Campus>) {
        campusAdapter.clear()
        campusAdapter.addAll(campuses.map { it.campusShortName ?: "Unknown" })
        if (campuses.isNotEmpty()) binding.spnCampus.setSelection(0)
    }

    private fun updateRoomSpinner(rooms: List<GetRoomResponse.SingleRoomResponse>) {
        roomAdapter.clear()
        roomAdapter.addAll(rooms.map { it.roomName ?: "Unknown" })
        if (rooms.isNotEmpty()) binding.spnRoom.setSelection(0)
    }

    private fun handleStopClearClick() {
        if (objRfidScanner.loopFlag) {
            stopScanning()
            binding.btnStop.text = "Clear"
        } else {
            clearAllData()
        }
        updateButtonStates()
    }

    private fun startScanning() {
        objRfidScanner.readTagLoop(viewLifecycleOwner.lifecycleScope) { tag ->
            if (!scannedTags.containsKey(tag.tid)) {
                viewModel.fetchDeviceInfo(tag.tid)
            }
        }
        updateButtonStates()
    }

    private fun showScanButton(){
        binding.btnSearch.visibility = View.VISIBLE
        binding.linearLayoutStopClear.visibility = View.GONE
        binding.btnSendToBackend.visibility = View.GONE
    }

    private fun stopScanning() {
        objRfidScanner.stopReadTagLoop()
        updateButtonStates()
    }

    private fun sendDataToBackend() {
        currentRoomId?.let { roomId ->
            lifecycleScope.launch {
                viewModel.sendManualInventory(scannedTags.keys.toList(), roomId)
            }
        } ?: Toast.makeText(context, "Please select a room", Toast.LENGTH_SHORT).show()
    }

    private fun handleManualInventoryResult(result: Result<ManualInventoryResponse>) {
        result.onSuccess {
            Toast.makeText(context, "Inventory updated successfully", Toast.LENGTH_SHORT).show()
            clearAllData()
        }.onFailure {
            Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateList() {
        listAdapter.clear()
        listAdapter.addAll(scannedTags.values.sorted())
        binding.cardViewList.visibility = if (scannedTags.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun resetAllData() {
        stopScanning()
        clearAllData()
        updateButtonStates()
    }

    private fun clearAllData() {
        scannedTags.clear()
        updateList()
        binding.btnStop.text = "Stop"
    }

    private fun updateButtonStates() {
        binding.linearLayoutStopClear.visibility = if (objRfidScanner.loopFlag) View.VISIBLE else View.GONE
        binding.btnSendToBackend.visibility = if (scannedTags.isNotEmpty()) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        resetAllData()
        viewModel.fetchCampuses()
    }

    override fun onPause() {
        super.onPause()
        stopScanning()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onTriggerDown() {
        if (!objRfidScanner.loopFlag) startScanning()
    }

    override fun onTriggerLongPress() {
        if (!objRfidScanner.loopFlag) startScanning()
    }

    override fun onTriggerRelease() {
        stopScanning()
    }
}