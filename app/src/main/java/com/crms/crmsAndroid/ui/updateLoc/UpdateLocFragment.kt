package com.crms.crmsAndroid.ui.updateLoc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.crms.crmsAndroid.MainActivity
import com.crms.crmsAndroid.R
import com.crms.crmsAndroid.scanner.rfidScanner
import com.crms.crmsAndroid.ui.ITriggerDown
import com.crms.crmsAndroid.ui.ITriggerLongPress
import com.crms.crmsAndroid.databinding.FragmentUpdateLocBinding

class UpdateLocFragment : Fragment(), ITriggerDown, ITriggerLongPress {

    private var _binding: FragmentUpdateLocBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: UpdateLocViewModel
    private lateinit var listAdapter: ArrayAdapter<String>
    private val items = mutableListOf<String>()
    private val scannedTags = mutableSetOf<String>()
    private val tagInfoMap = mutableMapOf<String, String>()
    private lateinit var mainActivity: MainActivity
    private lateinit var objRfidScanner: rfidScanner


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdateLocBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(UpdateLocViewModel::class.java)

        setupUI()
        setupObservers()

        return binding.root
    }

    private fun setupUI() {
        mainActivity = requireActivity() as MainActivity
        objRfidScanner = mainActivity.objRfidScanner

        // Initialize ListView
        listAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, items)
        binding.lvMain.adapter = listAdapter

        // Set up buttons
        binding.btnScan.setOnClickListener {
            handleBtnScanClick(objRfidScanner)
        }
        binding.btnStop.setOnClickListener {
            objRfidScanner.stopReadTagLoop()
        }

        // Set up Campus Spinner
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.campus_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.campusSpinner.adapter = adapter
        }

        // Set up Room Spinner
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.cw_room_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.roomSpinner.adapter = adapter
        }

        appendTextToList("RFID 版本: ${objRfidScanner.getVersion()}")
    }

    private fun setupObservers() {
        viewModel.items.observe(viewLifecycleOwner) { newItems ->
            items.clear()
            items.addAll(newItems)
            listAdapter.notifyDataSetChanged()
        }
    }

    private fun handleBtnScanClick(rfidScanner: rfidScanner) {
        try {
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
        } catch (e: Exception) {
            appendTextToList("Error: ${e.message}")
        }
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
