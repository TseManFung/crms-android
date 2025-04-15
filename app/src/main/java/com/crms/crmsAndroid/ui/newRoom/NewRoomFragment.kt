package com.crms.crmsAndroid.ui.newRoom

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
import com.crms.crmsAndroid.SharedViewModel
import com.crms.crmsAndroid.api.requestResponse.Room.GetRoomResponse
import com.crms.crmsAndroid.api.requestResponse.campus.GetCampusResponse
import com.crms.crmsAndroid.databinding.FragmentNewRoomBinding
import com.crms.crmsAndroid.scanner.rfidScanner
import com.crms.crmsAndroid.ui.ITriggerDown
import com.crms.crmsAndroid.ui.ITriggerLongPress

class NewRoomFragment : Fragment(), ITriggerDown, ITriggerLongPress {
    private var _binding: FragmentNewRoomBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NewRoomViewModel by viewModels()


    private lateinit var mainActivity: MainActivity
    private lateinit var objRfidScanner: rfidScanner
    private lateinit var campusAdapter: ArrayAdapter<String>
    private lateinit var roomAdapter: ArrayAdapter<String>
    private var currentCampusId: Int = -1
    private var currentRooms: List<GetRoomResponse.SingleRoomResponse> = emptyList()
    private var isScanning = false
    private var isManualStop = false
    private var isLongPress = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewRoomBinding.inflate(inflater, container, false)
        mainActivity = requireActivity() as MainActivity
        objRfidScanner = mainActivity.objRfidScanner
        val sharedViewModel = ViewModelProvider(mainActivity).get(SharedViewModel::class.java)
        viewModel.sharedViewModel = sharedViewModel
        setupUI()
        setupObservers()
        viewModel.fetchCampuses()
        return binding.root
    }

    private lateinit var listAdapter: ArrayAdapter<String>

    private fun setupUI() {

        // Initialize ListView
        listAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1)
        binding.lvSearchResult.adapter = listAdapter


        binding.lvSearchResult.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                val selectedItem = listAdapter.getItem(position)
                selectedItem?.let {
                    showSingleItemConfirmationDialog(it)
                } ?: run {
                    Toast.makeText(context, "Selected item is invalid", Toast.LENGTH_SHORT).show()
                }
            }
        // Initialize Campus SpinnerSea
        campusAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf())
        campusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnCampus.adapter = campusAdapter

        // Initialize Room Spinner
        roomAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf())
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnRoom.adapter = roomAdapter


        // Campus Spinner selection listener
        binding.spnCampus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

                val selectedCampus = viewModel.campuses.value?.get(position)
                Log.d("Fragment", "Selected Campus ID: ${selectedCampus?.campusId}")
                selectedCampus?.campusId?.let { campusId ->
                    currentCampusId = campusId
                    viewModel.fetchRooms(campusId)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.btnAdd.setOnClickListener {
            if (!isScanning) {
                startScanning()
                binding.btnAdd.text = "Stop Scanning"
                isScanning = true
                isManualStop = false
            } else {
                stopScanning()
                binding.btnAdd.text = "Start Scanning"
                isScanning = false
                isManualStop = true
            }
        }




        viewModel.rooms.observe(viewLifecycleOwner) { rooms ->
            currentRooms = rooms

        }

        binding.btnSubmit.setOnClickListener {
            if (currentRooms.isEmpty()) {
                Toast.makeText(context, "Need to choose room ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedRoomPosition = binding.spnRoom.selectedItemPosition
            val roomId = currentRooms.getOrNull(selectedRoomPosition)?.room ?: -1

            if (roomId == -1) {
                Toast.makeText(context, "Invalid room selection", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.submitData(roomId)
        }


        viewModel.submitStatus.observe(viewLifecycleOwner) { (success, message) ->
            message?.takeIf {
                view?.windowToken != null && isAdded
            }?.let {
                if (success) {
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                }
                viewModel.resetSubmitStatus()
            }
        }

        binding.btnClear.setOnClickListener {
            binding.btnAdd.text = "Start Scanning"
            stopScanning()
            viewModel.clearAllData() // 使用新的清理方法
            listAdapter.clear()
            listAdapter.notifyDataSetChanged()
            Toast.makeText(context, "All record deleted", Toast.LENGTH_SHORT).show()
        }


    }


    private fun setupObservers() {
        viewModel.items.observe(viewLifecycleOwner) { newItems ->
            Log.d("Fragment", "Observed items change. Size: ${newItems.size}")
            listAdapter.apply {
                clear()
                addAll(newItems)
                notifyDataSetChanged()
            }
            binding.cardViewList.visibility = if (newItems.isEmpty()) View.GONE else View.VISIBLE
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


    private fun startScanning() {
        objRfidScanner.stopReadTagLoop()

        objRfidScanner.readTagLoop(viewLifecycleOwner.lifecycleScope) { tag ->
            val currentTid = tag.tid
            if (!viewModel.isTagScannedOrSubmitted(currentTid)) {
                viewModel.addScannedTag(currentTid)
                val message = """
                EPC: ${tag.epc}
                TID: $currentTid
                RSSI: ${tag.rssi}
            """.trimIndent()
                viewModel.addItem(message)
            } else {
                Log.d("Fragment", "Skipped duplicate RFID: $currentTid")
            }
        }
    }

    private fun stopScanning() {
        objRfidScanner.stopReadTagLoop()

    }

    override fun onPause() {
        super.onPause()
        objRfidScanner.stopReadTagLoop()
        viewModel.clearAllData()

    }

    override fun onResume() {
        super.onResume()
        viewModel.clearAllData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onTriggerLongPress() {
        if (!isScanning) {
            isLongPress = true
            startScanning()
            binding.btnAdd.text = "Stop Scanning"
            isScanning = true
            isManualStop = false
        }
    }

    override fun onTriggerRelease() {
        if (isLongPress) {
            stopScanning()
            binding.btnAdd.text = "Start Scanning"
            isScanning = false
            isLongPress = false
        }
    }

    override fun onTriggerDown() {
        if (!isScanning && !isLongPress) {
            startScanning()
            binding.btnAdd.text = "Stop Scanning"
            isScanning = true
            isManualStop = false
        } else if (isScanning && !isLongPress) {
            stopScanning()
            binding.btnAdd.text = "Start Scanning"
            isScanning = false
            isManualStop = true
        }
    }


    private fun showSingleItemConfirmationDialog(itemInfo: String) {
        val tid = extractTidFromItem(itemInfo)


        if (tid.isNullOrEmpty()) {
            Toast.makeText(context, "Invalid tag format", Toast.LENGTH_SHORT).show()
            return
        }


        val roomPosition = binding.spnRoom.selectedItemPosition
        val room = currentRooms.getOrNull(roomPosition)


        if (room == null) {
            Toast.makeText(context, "Please select a valid room first", Toast.LENGTH_SHORT).show()
            return
        }


        val roomId = room.room ?: run {
            Toast.makeText(context, "Invalid room ID", Toast.LENGTH_SHORT).show()
            return
        }


        val roomName = room.roomName ?: "Unnamed Room"


        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Submission")
            .setMessage("Confirm to submit this item to ${roomName}?\nrfid: $tid")
            .setPositiveButton("Confirm") { _, _ ->
                viewModel.submitSingleItem(roomId, tid)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun extractTidFromItem(item: String): String? {
        return item.split("\n")
            .find { it.startsWith("TID:") }
            ?.substringAfter(":")
            ?.trim()
    }

}