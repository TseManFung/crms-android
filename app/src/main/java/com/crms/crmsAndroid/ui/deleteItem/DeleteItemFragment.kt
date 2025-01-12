package com.crms.crmsAndroid.ui.deleteItem

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.crms.crmsAndroid.MainActivity
import com.crms.crmsAndroid.R
import com.crms.crmsAndroid.databinding.FragmentDeleteItemBinding
import com.crms.crmsAndroid.scanner.rfidScanner
import com.crms.crmsAndroid.ui.ITriggerDown
import com.crms.crmsAndroid.ui.ITriggerLongPress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class DeleteItemFragment : Fragment(), ITriggerDown, ITriggerLongPress {

    private var _binding: FragmentDeleteItemBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DeleteItemViewModel
    private lateinit var listAdapter: ArrayAdapter<String>
    private val items = mutableListOf<String>()
    private val scannedTags = mutableSetOf<String>()
    private val tagInfoMap = mutableMapOf<String, String>()
    private lateinit var mainActivity: MainActivity
    private lateinit var objRfidScanner: rfidScanner
    private var startStatus = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeleteItemBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(DeleteItemViewModel::class.java)

        setUpUI()
        setupObservers()

        return binding.root
    }

    private fun setUpUI() {
        mainActivity = requireActivity() as MainActivity
        objRfidScanner = mainActivity.objRfidScanner

        chooseRmListener()
        scanRmListener()

        listAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, items)
        binding.scanRmResult.adapter = listAdapter

        binding.scanRmBtn.setOnClickListener {
            if(!startStatus){
                handleBtnScanClick(objRfidScanner)
                startStatus=true
            }
            else{
                objRfidScanner.stopReadTagLoop()
                sendDataToBackend()
                startStatus=false
            }
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
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), 1)
        } else {
            try {
                rfidScanner.readTagLoop(viewLifecycleOwner.lifecycleScope) { tag ->
                    val currentTid = tag.tid
                    val message = """ |EPC: ${tag.epc} |TID: ${tag.tid} |RSSI: ${tag.rssi} |Antenna: ${tag.ant} |Index: ${tag.index} |PC: ${tag.pc} |Remain: ${tag.remain} |Reserved: ${tag.reserved} |User: ${tag.user} """.trimMargin()
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
    }

    private fun appendTextToList(message: String) {
        items.add(message)
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

    private fun chooseRmListener() {
        val chooseRmRadio = binding.chooseRmRadio
        val rmSpin = binding.rmSpin
        val rmText = binding.rmText

        chooseRmRadio.setOnClickListener {
            initializeUI()
            rmText.visibility = View.VISIBLE
            rmSpin.visibility = View.VISIBLE
            rmSpin.adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.cw_room_array,
                android.R.layout.simple_spinner_item
            )
            rmSpin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    Toast.makeText(
                        requireContext(),
                        "Selected: " + parent.getItemAtPosition(position),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Another interface callback
                }
            }
        }
    }

    private fun scanRmListener() {
        val scanRmRadio = binding.scanRmRadio
        val scanRmBtn = binding.scanRmBtn
        val scanRmList = binding.scanRmList

        scanRmRadio.setOnClickListener {
            initializeUI()
            scanRmBtn.visibility = View.VISIBLE
            scanRmList.visibility = View.VISIBLE
        }


    }

    private fun initializeUI() {
        val rmSpin = binding.rmSpin
        val rmText = binding.rmText
        val scanRmBtn = binding.scanRmBtn
        val scanRmList = binding.scanRmList

        rmSpin.visibility = View.GONE
        rmText.visibility = View.GONE
        scanRmBtn.visibility = View.GONE
        scanRmList.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        items.clear()
    }
}