package com.crms.crmsAndroid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.crms.crmsAndroid.databinding.FragmentTestRfidBinding
import com.crms.crmsAndroid.scanner.rfidScanner
import com.crms.crmsAndroid.ui.ITriggerDown
import com.crms.crmsAndroid.ui.ITriggerLongPress
import com.rscja.deviceapi.interfaces.IUHF.Bank_TID

class TestRfidFragment : Fragment(), ITriggerDown, ITriggerLongPress {

    private var _binding: FragmentTestRfidBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TestRfidViewModel
    private lateinit var listAdapter: ArrayAdapter<String>
    private val items = mutableListOf<String>()
    private lateinit var mainActivity: MainActivity
    private lateinit var objRfidScanner: rfidScanner
    override fun onTriggerDown() {
        val TID = objRfidScanner.readTag(Bank_TID)
        appendTextToList("call by trigger\nTID: ${TID}")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTestRfidBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(TestRfidViewModel::class.java)

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

        // Set up Spinner
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.bank_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spnBank.adapter = adapter
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
                val message =
                    """ |EPC: ${tag.epc} |TID: ${tag.tid} |RSSI: ${tag.rssi} |Antenna: ${tag.ant} |Index: ${tag.index} |PC: ${tag.pc} |Remain: ${tag.remain} |Reserved: ${tag.reserved} |User: ${tag.user} """.trimMargin()
                viewModel.addItem(message)
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onTriggerLongPress() {
        appendTextToList("call by long press")
    }

    override fun onTriggerRelease() {
        appendTextToList("call by trigger release")
    }
}