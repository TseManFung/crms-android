package com.crms.crmsAndroid.ui.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.crms.crmsAndroid.MainActivity
import com.crms.crmsAndroid.R
import com.crms.crmsAndroid.SharedViewModel
import com.crms.crmsAndroid.api.requestResponse.Room.GetRoomResponse
import com.crms.crmsAndroid.api.requestResponse.campus.GetCampusResponse
import com.crms.crmsAndroid.api.requestResponse.item.GetItemResponse
import com.crms.crmsAndroid.databinding.FragmentSearchBinding
import com.crms.crmsAndroid.scanner.rfidScanner
import com.crms.crmsAndroid.ui.ITriggerDown
import com.crms.crmsAndroid.ui.ITriggerLongPress
import com.crms.crmsAndroid.utils.CompassManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SearchFragment : Fragment(), ITriggerDown, ITriggerLongPress {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()

    private lateinit var campusAdapter: ArrayAdapter<String>
    private lateinit var roomAdapter: ArrayAdapter<String>
    private lateinit var deviceAdapter: ArrayAdapter<String>
    private lateinit var listAdapter: ArrayAdapter<String>
    private val items = mutableListOf<String>()
    private val scannedTags = mutableSetOf<String>()
    private val tagInfoMap = mutableMapOf<String, String>()
    private lateinit var mainActivity: MainActivity
    private lateinit var objRfidScanner: rfidScanner
    private var startStatus = false
    private lateinit var compassManager: CompassManager

    private var selectedItemRFIDs: List<String>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        mainActivity = requireActivity() as MainActivity
        objRfidScanner = mainActivity.objRfidScanner
        val sharedViewModel = ViewModelProvider(mainActivity).get(SharedViewModel::class.java)
        viewModel.sharedViewModel = sharedViewModel
        setupUI()
        setupObservers()
        return binding.root
    }

    private fun setupUI() {
        compassManager = CompassManager(requireContext())

        // Initialize ListView
        listAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, items)
        binding.searchResult.adapter = listAdapter

        // Initialize Campus Spinner
        campusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf())
        campusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.campusSpinner.adapter = campusAdapter

        // Initialize Room Spinner
        roomAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf())
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.roomSpinner.adapter = roomAdapter

        // Initialize Device Spinner
        deviceAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf())
        deviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.itemSpinner.adapter = deviceAdapter



        // Campus Spinner selection listener
        binding.campusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCampus = viewModel.campuses.value?.get(position)
                selectedCampus?.campusId?.let { campusId ->
                    viewModel.fetchRooms(campusId)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Room Spinner selection listener
        binding.roomSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedRoom = viewModel.rooms.value?.get(position)
                selectedRoom?.room?.let { roomId ->
                    viewModel.fetchDevices(roomId)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Device Spinner selection listener
        binding.itemSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedDevice = viewModel.devices.value?.get(position)
                selectedItemRFIDs = selectedDevice?.deviceRFID?.map { it.RFID ?: "" }

                Log.d("SearchFragment", "Selected Item RFIDs: $selectedItemRFIDs")

                binding.startPauseBtn.visibility = View.VISIBLE
                binding.cardView.visibility = View.VISIBLE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }



        binding.startPauseBtn.setOnClickListener {
            if (!startStatus) {
                handleBtnScanClick(objRfidScanner)
                startStatus = true
            } else {
                objRfidScanner.stopReadTagLoop()
                sendDataToBackend()
                startStatus = false
            }
        }

        appendTextToList("RFID 版本: ${objRfidScanner.getVersion()}")
    }

    private fun setupObservers() {
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

        viewModel.devices.observe(viewLifecycleOwner) { devices ->
            devices?.let {
                updateDeviceSpinner(devices)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.items.observe(viewLifecycleOwner) { newItems ->
            items.clear()
            items.addAll(newItems)
            listAdapter.notifyDataSetChanged()
        }
    }

    private fun updateCampusSpinner(campuses: List<GetCampusResponse.Campus>) {
        val campusNames = campuses.map { it.campusShortName ?: "Unknown" }
        campusAdapter.clear()
        campusAdapter.addAll(campusNames)
        campusAdapter.notifyDataSetChanged()

        if (campusNames.isNotEmpty()) {
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
            binding.roomSpinner.visibility = View.VISIBLE
            binding.roomText.visibility = View.VISIBLE
        }
    }

    private fun updateDeviceSpinner(devices: List<GetItemResponse.Devices>) {
        val deviceNames = devices.map { "${it.deviceID}: ${it.deviceName}" }
        deviceAdapter.clear()
        deviceAdapter.addAll(deviceNames)
        deviceAdapter.notifyDataSetChanged()

        if (deviceNames.isNotEmpty()) {
            binding.itemSpinner.setSelection(0)
            binding.itemSpinner.visibility = View.VISIBLE
            binding.itemText.visibility = View.VISIBLE
        }
    }

    private fun appendTextToList(text: String) {
        items.add(text)
        listAdapter.notifyDataSetChanged()
    }

    private fun sendDataToBackend() {
        lifecycleScope.launch {
            try {
                val url = URL("https://your-backend-endpoint.com/api/scan")
                val jsonInputString = JSONObject(tagInfoMap as Map<*, *>?).toString()

                withContext(Dispatchers.IO) {
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json; utf-8")
                    conn.setRequestProperty("Accept", "application/json")
                    conn.doOutput = true

                    conn.outputStream.use { os ->
                        val input = jsonInputString.toByteArray()
                        os.write(input, 0, input.size)
                    }

                    val responseCode = conn.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        appendTextToList("Data sent successfully")
                    } else {
                        appendTextToList("Failed to send data: $responseCode")
                    }
                }
            } catch (e: Exception) {
                appendTextToList("Error: ${e.message}")
            }
        }
    }

    private fun handleBtnScanClick(rfidScanner: rfidScanner) {
        try {
            rfidScanner.readTagLoop(viewLifecycleOwner.lifecycleScope) { tag ->
                val currentTid = tag.tid
                val message =
                    """ |EPC: ${tag.epc} |TID: ${tag.tid} |RSSI: ${tag.rssi} |Antenna: ${tag.ant} |Index: ${tag.index} |PC: ${tag.pc} |Remain: ${tag.remain} |Reserved: ${tag.reserved} |User: ${tag.user} """.trimMargin()


                if (selectedItemRFIDs?.contains(tag.epc) == true) {
                    if (!scannedTags.contains(currentTid)) {
                        scannedTags.add(currentTid)
                        tagInfoMap[currentTid] = message
                        viewModel.addItem(message)
                        controlProgressBar(tag.rssi)
                    } else {
                        viewModel.updateItem(currentTid, message)
                        controlProgressBar(tag.rssi)
                    }
                } else {
                    appendTextToList("Cant find device")
                }
            }
        } catch (e: Exception) {
            appendTextToList("Error: ${e.message}")
        }
    }

    fun controlProgressBar(value: String) {
        try {
            val rssi = value.toFloat()
            val progressBar: ProgressBar = binding.progressBar
            val percentage = binding.startPercentage
            var progress = ((rssi + 100) * 100 / 60).toInt()
            progress = progress.coerceAtMost(100)
            progressBar.progress = progress
            percentage.text = "$progress%"
        } catch (e: NumberFormatException) {
            appendTextToList("Error: Invalid number format for RSSI value: $value")
        }
    }

    private fun calculateDirection(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val angle = Math.toDegrees(Math.atan2(y2 - y1, x2 - x1))
        return if (angle < 0) angle + 360 else angle
    }

    override fun onPause() {
        super.onPause()
        objRfidScanner.stopReadTagLoop()
        scannedTags.clear()
        tagInfoMap.clear()
    }

    fun stopAll() {
        binding.roomSpinner.visibility = View.GONE
        binding.itemSpinner.visibility = View.GONE
        binding.roomText.visibility = View.GONE
        binding.itemText.visibility = View.GONE
        binding.startPauseBtn.visibility = View.GONE
        binding.cardView.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        stopAll()
        viewModel.clearItems()
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
        handleBtnScanClick(objRfidScanner)
    }
}