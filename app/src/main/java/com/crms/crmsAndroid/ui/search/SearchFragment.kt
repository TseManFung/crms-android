package com.crms.crmsAndroid.ui.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.crms.crmsAndroid.MainActivity
import com.crms.crmsAndroid.SharedViewModel
import com.crms.crmsAndroid.algorithm.DirectionFinder
import com.crms.crmsAndroid.api.requestResponse.Room.GetRoomResponse
import com.crms.crmsAndroid.api.requestResponse.campus.GetCampusResponse
import com.crms.crmsAndroid.api.requestResponse.item.GetItemResponse
import com.crms.crmsAndroid.databinding.FragmentSearchBinding
import com.crms.crmsAndroid.scanner.rfidScanner
import com.crms.crmsAndroid.ui.ITriggerDown
import com.crms.crmsAndroid.ui.ITriggerLongPress
import com.crms.crmsAndroid.utils.CompassManager
import com.rscja.deviceapi.entity.UHFTAGInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
import kotlin.math.min

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
    private lateinit var arrow: ImageView
    private val startStatus get() = objRfidScanner.loopFlag
    private lateinit var compassManager: CompassManager

    private var selectedItemRFIDs: List<String>? = null

    private val directionCalculator: DirectionFinder = DirectionFinder()
    private var lastDirectionDeg = 0.0F
    private lateinit var itemPartAdapter: ArrayAdapter<String>
    private lateinit var rfidAdapter: ArrayAdapter<String>
    private var selectedRFID: String? = null
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
        viewModel.fetchCampuses()
        return binding.root
    }

    private fun setupUI() {
        compassManager = CompassManager(requireContext())

        // Initialize ListView
        listAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, items)
        binding.searchResult.adapter = listAdapter

        // Initialize Campus Spinner
        campusAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf())
        campusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.campusSpinner.adapter = campusAdapter

        // Initialize Room Spinner
        roomAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf())
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.roomSpinner.adapter = roomAdapter

        // Initialize Device Spinner
        deviceAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf())
        deviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.itemSpinner.adapter = deviceAdapter


        // Campus Spinner selection listener
        binding.campusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedCampus = viewModel.campuses.value?.get(position)
                selectedCampus?.campusId?.let { campusId ->
                    viewModel.fetchRooms(campusId)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Room Spinner selection listener
        binding.roomSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
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
                selectedDevice?.let { device ->
                    loadItemPartsAndRFIDs(device) // 确保调用加载方法
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.itemPartSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedDevice = viewModel.devices.value?.find {
                    it.partID.any { part -> part.devicePartName == binding.itemPartSpinner.selectedItem }
                }
                selectedDevice?.let { device ->
                    val partId = device.partID[position].devicePartID
                    viewModel.selectPart(partId)
                    viewModel.loadRFIDs(device, partId)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.startPauseBtn.setOnClickListener {
            if (!startStatus) {
                handleBtnScanClick(objRfidScanner)
            } else {
                objRfidScanner.stopReadTagLoop()
                binding.startPauseBtn.text = "Start to Search"
            }
        }

        arrow = binding.arrow
        // 初始化 ItemPart Spinner
        itemPartAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf())
        itemPartAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.itemPartSpinner.adapter = itemPartAdapter

        // 初始化 RFID Spinner
        rfidAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf())
        rfidAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.rfidSpinner.adapter = rfidAdapter

        // 新增 RFID Spinner 監聽
        binding.rfidSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedRFID = viewModel.rfids.value?.get(position)?.second
                updateUIVisibility()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadItemPartsAndRFIDs(device: GetItemResponse.Devices) {
        if (device.partID.isEmpty() || device.deviceRFID.isEmpty()) {
            Toast.makeText(context, "No parts or RFID data available", Toast.LENGTH_SHORT).show()
            return
        }
        // 加载零件数据
        val parts = device.partID.map { it.devicePartName ?: "Unnamed Part" }
        itemPartAdapter.clear()
        itemPartAdapter.addAll(parts)
        itemPartAdapter.notifyDataSetChanged()

        // 调用 ViewModel 加载 RFID 数据
        viewModel.loadRFIDs(device)

        // 观察 RFID 数据变化
        viewModel.rfids.observe(viewLifecycleOwner) { rfidPairs ->
            val formattedLabels = rfidPairs.map { it.first }
            rfidAdapter.clear()
            rfidAdapter.addAll(formattedLabels)
            rfidAdapter.notifyDataSetChanged()
        }

        // 显示相关UI元素
        binding.itemPartSpinner.visibility = View.VISIBLE
        binding.itemPartText.visibility = View.VISIBLE
        binding.rfidSpinner.visibility = View.VISIBLE
        binding.rfidText.visibility = View.VISIBLE
    }

    private fun updateUIVisibility() {
        if (selectedRFID != null) {
            binding.startPauseBtn.visibility = View.VISIBLE
            binding.cardView.visibility = View.VISIBLE
        } else {
            binding.startPauseBtn.visibility = View.GONE
            binding.cardView.visibility = View.GONE
        }
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
        val roomNames = rooms.map { "${it.roomName} (Room ${it.roomNumber})" ?: "Unknown" }
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


    private fun handleBtnScanClick(rfidScanner: rfidScanner) {
        binding.startPauseBtn.text = "Pause"
        selectedRFID?.let { targetRfid ->
            try {
                directionCalculator.clearData()
                directionCalculator.targetTag = targetRfid
                Log.d("search1", "handleBtnScanClick: $targetRfid")
                rfidScanner.readTagLoop(viewLifecycleOwner.lifecycleScope) { tag ->


                    if (tag.tid == directionCalculator.targetTag) {
                        controlProgressBar(tag.rssi)
                    }
                    processTag(tag)
                    updateDirection()


                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(context, "Please select an RFID first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processTag(tag: UHFTAGInfo) {
        // 假设tag对象包含tid和rssi属性
        val tid = tag.tid ?: return
        val rssi = tag.rssi.toDouble()

        directionCalculator.updateTag(tid, rssi)
    }

    private fun updateDirection() {
        directionCalculator.calculateDirection().let { directionRad ->
            if (directionRad.isNaN() || directionRad.isInfinite()) {
                return // 如果是 NaN 或無窮大，則直接返回
            }
            val directionDeg = Math.toDegrees(directionRad).toFloat()
            val d = Math.abs(directionDeg - lastDirectionDeg)
            if (d < 20 || d>120) {
                return
            }
            else if (Math.abs(directionDeg - lastDirectionDeg) > 50) {
                lastDirectionDeg += directionDeg
                lastDirectionDeg /= 2
                directionCalculator.normalizeAngle(lastDirectionDeg)
                return
            }
            lastDirectionDeg = directionDeg
            arrow.rotation = directionDeg + 90
        }

    }

    fun controlProgressBar(value: String) {
        try {
            val rssi = value.toDouble()
            val progressBar: ProgressBar = binding.progressBar
            val percentage = binding.startPercentage
            val progress: Int = max(0, min(100, ((rssi+80)*1.81).toInt()))
            progressBar.progress = progress
            percentage.text = "Estimated distance: ${
                String.format(
                    "%.2f",
                    directionCalculator.getEstimatedDirection(rssi)
                )
            } cm"
        } catch (e: NumberFormatException) {
            //appendTextToList("Error: Invalid number format for RSSI value: $value")
        }
    }

    override fun onPause() {
        super.onPause()
        binding.startPauseBtn.text = "Start to Search"
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
        binding.itemPartText.visibility = View.GONE
        binding.itemPartSpinner.visibility = View.GONE
        binding.rfidText.visibility = View.GONE
        binding.rfidSpinner.visibility = View.GONE
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
        binding.startPauseBtn.text = "Start to Search"
    }

    override fun onTriggerDown() {
        handleBtnScanClick(objRfidScanner)
    }
}