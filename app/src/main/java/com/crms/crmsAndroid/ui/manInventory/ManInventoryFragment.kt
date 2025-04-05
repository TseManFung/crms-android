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

    private lateinit var listAdapter: CustomAdapter
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
        showScanButton()
        // Initialize ListView
        listAdapter = CustomAdapter()
        binding.lvSearchResult.adapter = listAdapter
        binding.lvSearchResult.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            items
        ).apply {
            setNotifyOnChange(true)
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
        binding.btnSendToBackend.setOnClickListener {
            sendDataToBackend()
        }

        // Campus Spinner selection listener
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
                binding.btnSendToBackend.visibility = View.GONE // Hide btnSendToBackend
            }
        }

        appendTextToList("RFID 版本: ${objRfidScanner.getVersion()}")
    }

    private fun showScanButton() {
        binding.btnSearch.visibility = View.VISIBLE
        binding.linearLayoutStopClear.visibility = View.GONE
        binding.btnSendToBackend.visibility = View.GONE
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
                    viewModel.addItem(message)
                } else {
                    viewModel.updateItem(currentTid, message)
                }
            }
            binding.linearLayoutStopClear.visibility = View.VISIBLE
            binding.btnSendToBackend.visibility = View.VISIBLE // Show btnSendToBackend
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
        val itemsWithStatus = response.manualInventoryLists.map { item ->
            when {
                item.preState == 'A' && item.afterState == 'A' ->
                    "✅ ${item.deviceName} (${item.RFID}) - 正常" to R.color.green_state
                item.preState == 'B' && item.afterState == 'A' ->
                    "🔄 ${item.deviceName} (${item.RFID}) - 已归还" to R.color.green_state
                item.preState == 'A' && item.afterState == 'C' ->
                    "❌ ${item.deviceName} (${item.RFID}) - 未找到" to R.color.gray_state
                item.preState == 'B' && item.afterState == 'B' ->
                    "⚠️ ${item.deviceName} (${item.RFID}) - 借出中" to R.color.yellow_state
                else ->
                    "❓ ${item.deviceName} (${item.RFID}) - 状态未知" to R.color.gray_state
            }
        }

        val sortedItems = itemsWithStatus.sortedWith(compareBy {
            when (it.second) {
                R.color.green_state -> 0
                R.color.gray_state -> 1
                else -> 2
            }
        })

        items.clear()
        items.addAll(sortedItems.map { it.first })
        (binding.lvSearchResult.adapter as ArrayAdapter<String>).notifyDataSetChanged()

        binding.lvSearchResult.setOnItemClickListener { _, view, position, _ ->
            view.setBackgroundResource(sortedItems[position].second)
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
        scannedTags.clear()
        tagInfoMap.clear()
        viewModel.clearItems()
        items.clear()
        listAdapter.notifyDataSetChanged()

        binding.btnStop.text = "Stop"
        binding.linearLayoutStopClear.visibility = View.GONE
        binding.cardViewList.visibility = View.GONE

        roomAdapter.clear()
        roomAdapter.notifyDataSetChanged()

        objRfidScanner.stopReadTagLoop()
    }

    private fun clearAllData() {
        scannedTags.clear()
        tagInfoMap.clear()
        viewModel.clearItems()
        binding.cardViewList.visibility = View.GONE
        binding.linearLayoutStopClear.visibility = View.GONE
        binding.btnStop.text = "Stop"
        binding.btnSendToBackend.visibility = View.GONE // Hide btnSendToBackend
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