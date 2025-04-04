package com.crms.crmsAndroid.ui.manInventory

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
import androidx.lifecycle.lifecycleScope
import com.crms.crmsAndroid.MainActivity
import com.crms.crmsAndroid.R
import com.crms.crmsAndroid.api.requestResponse.Room.GetRoomResponse
import com.crms.crmsAndroid.api.requestResponse.campus.GetCampusResponse
import com.crms.crmsAndroid.api.requestResponse.item.ManualInventoryResponse
import com.crms.crmsAndroid.databinding.FragmentManInventoryBinding
import com.crms.crmsAndroid.scanner.rfidScanner
import com.crms.crmsAndroid.ui.ITriggerDown
import com.crms.crmsAndroid.ui.ITriggerLongPress
import kotlinx.coroutines.launch
import kotlin.math.log

class ManInventoryFragment : Fragment(), ITriggerDown, ITriggerLongPress {
    private var _binding: FragmentManInventoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ManInventoryViewModel by viewModels()

    private lateinit var listAdapter: CustomAdapter

    private val items = mutableListOf<String>()
    private val scannedTags = mutableSetOf<String>()
    private val tagInfoMap = mutableMapOf<String, String>()
    private lateinit var mainActivity: MainActivity
    private lateinit var objRfidScanner: rfidScanner


    private lateinit var campusAdapter: ArrayAdapter<String>
    private lateinit var roomAdapter: ArrayAdapter<String>


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManInventoryBinding.inflate(inflater, container, false)
        setupUI()
        setupObservers()
        return binding.root
    }

    private fun setupUI() {
        mainActivity = requireActivity() as MainActivity
        objRfidScanner = mainActivity.objRfidScanner

        // Initialize ListView
        listAdapter = CustomAdapter()
        binding.lvSearchResult.adapter = listAdapter

        // Initialize Campus Spinner with empty adapter (will be populated by API)
        campusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf())
        campusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnCampus.adapter = campusAdapter
        // init Room Spinner
        roomAdapter = ArrayAdapter(requireContext(),android.R.layout.simple_spinner_item,
            mutableListOf())
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnRoom.adapter = roomAdapter
        //set up send to backend button
        binding.btnSendToBackend.setOnClickListener {
            sendDataToBackend()
        }


        // 设置校区 Spinner 选择监听
        binding.spnCampus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                resetAllData()
                val selectedCampus = viewModel.campuses.value?.get(position)
                Log.d("Fragment", "Selected Campus ID: ${selectedCampus?.campusId}")

                selectedCampus?.campusId?.let { campusId ->
                    viewModel.fetchRooms(campusId)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 观察房间数据变化
        viewModel.rooms.observe(viewLifecycleOwner) { rooms ->
            rooms?.let {
                updateRoomSpinner(rooms)
            }
        }

        // Set up buttons
        binding.btnSearch.setOnClickListener {
            handleBtnScanClick(objRfidScanner)
        }
        binding.btnStop.setOnClickListener {
            if (binding.btnStop.text == "Stop") {
                objRfidScanner.stopReadTagLoop()
                sendDataToBackend()
                binding.btnStop.text = "Clear"
            } else {
                clearAllData()
            }
        }



        appendTextToList("RFID 版本: ${objRfidScanner.getVersion()}")
    }


    private fun setupObservers() {


        viewModel.items.observe(viewLifecycleOwner) { newItems ->
            Log.d("Fragment", "Observed items change. Size: ${newItems.size}")
            items.clear()
            items.addAll(newItems)
            listAdapter.notifyDataSetChanged()
            binding.cardViewList.visibility = if (newItems.isNotEmpty()) View.VISIBLE else View.GONE
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

        viewModel.manualInventoryResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                handleManualInventoryResult(it)
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
        val roomNames = rooms.map { it.roomName ?: "Unknown" }
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
            rfidScanner.readTagLoop(viewLifecycleOwner.lifecycleScope) { tag ->
                val currentTid = tag.tid
                val currentRFID = tag.epc
                val message =
                    """ |EPC: ${tag.epc} |TID: ${tag.tid} |RSSI: ${tag.rssi} |Antenna: ${tag.ant} |Index: ${tag.index} |PC: ${tag.pc} |Remain: ${tag.remain} |Reserved: ${tag.reserved} |User: ${tag.user} """.trimMargin()
                Log.d("Fragment", message)
                if (!scannedTags.contains(currentTid)) {
                    scannedTags.add(currentTid)
                    tagInfoMap[currentTid] = message
                    viewModel.addItem(message) // 确保此方法被调用
                } else {
                    viewModel.updateItem(currentTid, message)
                }
            }
            binding.linearLayoutStopClear.visibility = View.VISIBLE
        } catch (e: Exception) {
            appendTextToList("Error: ${e.message}")
        }
    }

    private fun sendDataToBackend() {
        val rfidList = scannedTags.toList()
        val selectedRoomPosition = binding.spnRoom.selectedItemPosition
        val roomId = viewModel.rooms.value?.get(selectedRoomPosition)?.room ?: run {
            Toast.makeText(context, "Please select a room", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            viewModel.sendManualInventory(rfidList, roomId)
        }
    }

    private fun handleManualInventoryResult(result: Result<ManualInventoryResponse>) {
        result.onSuccess { response ->
            updateUIWithResponse(response)
            Toast.makeText(context, "Inventory updated successfully", Toast.LENGTH_SHORT).show()
        }.onFailure { exception ->
            Toast.makeText(context, "Error: ${exception.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateUIWithResponse(response: ManualInventoryResponse) {
        // 创建包含状态信息的列表
        val itemsWithStatus = response.manualInventoryLists.map { item ->
            Triple(
                "Device: ${item.deviceName} (${item.RFID})",
                item.afterState,
                when(item.afterState) {
                    'A' -> R.color.green_state   // 正常状态
                    'B' -> R.color.yellow_state  // 借出状态
                    else -> R.color.gray_state    // 未找到
                }
            )
        }

        // 排序：绿色在前，灰色在中，黄色在后
        val sortedItems = itemsWithStatus.sortedBy { (_, state, _) ->
            when(state) {
                'A' -> 0
                'C' -> 1
                else -> 2
            }
        }

        // 更新Adapter
        (binding.lvSearchResult.adapter as CustomAdapter).apply {
            clear()
            addAll(sortedItems)
            notifyDataSetChanged()
        }

        binding.cardViewList.visibility = View.VISIBLE
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
        // 重置扫描相关数据
        scannedTags.clear()
        tagInfoMap.clear()
        viewModel.clearItems()
        items.clear()
        listAdapter.notifyDataSetChanged()

        // 重置按钮状态
        binding.btnStop.text = "Stop"
        binding.linearLayoutStopClear.visibility = View.GONE
        binding.cardViewList.visibility = View.GONE

        // 清空房间 Spinner
        roomAdapter.clear()
        roomAdapter.notifyDataSetChanged()

        // 停止扫描
        objRfidScanner.stopReadTagLoop()
    }


    private fun clearAllData() {
        scannedTags.clear()
        tagInfoMap.clear()
        viewModel.clearItems()
        binding.cardViewList.visibility = View.GONE
        binding.linearLayoutStopClear.visibility = View.GONE
        binding.btnStop.text = "Stop"
    }

    private fun showScanButton() {
        binding.btnSearch.visibility = View.VISIBLE
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