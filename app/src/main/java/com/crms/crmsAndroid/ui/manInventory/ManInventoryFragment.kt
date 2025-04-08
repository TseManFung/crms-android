// ManInventoryFragment.kt
package com.crms.crmsAndroid.ui.manInventory

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
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
    private val scannedTags = mutableSetOf<String>()
    private val tagInfoList = mutableListOf<String>()

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
        // Initialize ListView
        listAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, tagInfoList)
        binding.lvSearchResult.adapter = listAdapter

        // Initialize Spinners
        campusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf())
        campusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnCampus.adapter = campusAdapter

        roomAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf())
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnRoom.adapter = roomAdapter

        // Button setup
        binding.btnSearch.setOnClickListener { startScanning() }
        binding.btnStop.apply {
            setOnClickListener {
                if (objRfidScanner.loopFlag) {
                    stopScanning()
                    text = "Clear"
                } else {
                    clearAllData()
                }
            }
        }

        binding.btnSendToBackend.setOnClickListener {
            if (scannedTags.isEmpty()) {
                Toast.makeText(context, "No items scanned", Toast.LENGTH_SHORT).show()
            } else {
                sendDataToBackend()
            }
        }

        // Campus selection listener
        binding.spnCampus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                resetAllData()
                viewModel.campuses.value?.get(position)?.campusId?.let { campusId ->
                    viewModel.fetchRooms(campusId)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        updateButtonStates()
    }

    private fun setupObservers() {
        viewModel.campuses.observe(viewLifecycleOwner) { campuses ->
            campuses?.let { updateCampusSpinner(campuses) }
        }

        viewModel.rooms.observe(viewLifecycleOwner) { rooms ->
            rooms?.let { updateRoomSpinner(rooms) }
        }

        viewModel.tagInfo.observe(viewLifecycleOwner) { info ->
            info?.let {
                if (!scannedTags.contains(it.rfid)) {
                    scannedTags.add(it.rfid)
                    tagInfoList.add("${it.deviceName} - ${it.devicePartName} - ${it.state}")
                    listAdapter.notifyDataSetChanged()
                }
            }
        }

        viewModel.manualInventoryResult.observe(viewLifecycleOwner) { result ->
            result?.let { handleManualInventoryResult(it) }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun startScanning() {
        try {
            objRfidScanner.readTagLoop(viewLifecycleOwner.lifecycleScope) { tag ->
                lifecycleScope.launch {
                    viewModel.getItemByRFID(tag.epc)
                }
            }
            updateButtonStates()
        } catch (e: Exception) {
            Toast.makeText(context, "Scan error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopScanning() {
        objRfidScanner.stopReadTagLoop()
        updateButtonStates()
    }

    private fun clearAllData() {
        scannedTags.clear()
        tagInfoList.clear()
        listAdapter.notifyDataSetChanged()
        binding.btnSendToBackend.isVisible = false
        updateButtonStates()
    }

    private fun resetAllData() {
        clearAllData()
        campusAdapter.clear()
        roomAdapter.clear()
        objRfidScanner.stopReadTagLoop()
        binding.spnCampus.setSelection(0)
        updateButtonStates()
    }

    private fun updateButtonStates() {
        val isScanning = objRfidScanner.loopFlag
        binding.btnSearch.isEnabled = !isScanning
        binding.btnStop.isVisible = isScanning || scannedTags.isNotEmpty()
        binding.btnStop.text = if (isScanning) "Stop" else "Clear"
        binding.btnSendToBackend.isVisible = scannedTags.isNotEmpty()
        binding.linearLayoutStopClear.isVisible = isScanning || scannedTags.isNotEmpty()
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

    private fun sendDataToBackend() {
        val selectedRoom = viewModel.rooms.value?.get(binding.spnRoom.selectedItemPosition)?.room
        selectedRoom?.let { roomId ->
            lifecycleScope.launch {
                viewModel.sendManualInventory(scannedTags.toList(), roomId)
            }
        } ?: Toast.makeText(context, "Please select a room", Toast.LENGTH_SHORT).show()
    }

    private fun handleManualInventoryResult(result: Result<ManualInventoryResponse>) {
        result.onSuccess {
            Toast.makeText(context, "Inventory updated!", Toast.LENGTH_SHORT).show()
            clearAllData()
        }.onFailure {
            Toast.makeText(context, "Update failed: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        resetAllData()
        objRfidScanner.stopReadTagLoop()
    }

    override fun onPause() {
        super.onPause()
        objRfidScanner.stopReadTagLoop()
        clearAllData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onTriggerLongPress() = startScanning()
    override fun onTriggerRelease() = stopScanning()
    override fun onTriggerDown() = startScanning()
}