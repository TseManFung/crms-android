package com.crms.crmsAndroid.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.crms.crmsAndroid.MainActivity
import com.crms.crmsAndroid.R
import com.crms.crmsAndroid.databinding.FragmentSearchBinding
import com.crms.crmsAndroid.scanner.rfidScanner
import com.crms.crmsAndroid.ui.ITriggerDown
import com.crms.crmsAndroid.ui.ITriggerLongPress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SearchFragment : Fragment(), ITriggerDown, ITriggerLongPress {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SearchViewModel
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
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(SearchViewModel::class.java)
        setUpUI()
        setupObservers()

        var isSpinnerInitialized = false

        val roomText: TextView = binding.roomText
        val itemText: TextView = binding.itemText
        val partText: TextView = binding.partText
        val campusSpinner: Spinner = binding.campusSpinner
        val roomSpinner: Spinner = binding.roomSpinner
        val itemSpinner: Spinner = binding.itemSpinner
        val partSpinner: Spinner = binding.partSpinner
        val progressBar: ProgressBar = binding.progressBar
        val cardView: CardView = binding.cardView
        val startPauseBtn: Button = binding.startPauseBtn

        val campusAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.campus_array,
            android.R.layout.simple_spinner_item
        )
        campusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        campusSpinner.adapter = campusAdapter

        val roomAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.cw_room_array,
            android.R.layout.simple_spinner_item
        )
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        roomSpinner.adapter = roomAdapter

        val itemAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.cw_room_349_item_array,
            android.R.layout.simple_spinner_item
        )
        itemAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        itemSpinner.adapter = itemAdapter

        val partAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.cw_room_349_item_robotdog_part_array,
            android.R.layout.simple_spinner_item
        )
        partAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        partSpinner.adapter = partAdapter

        campusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true
                    return
                }

                if (position == 0) {
                    // Default value selected, do nothing
                    return
                }

                val selectedItem = parent.getItemAtPosition(position).toString()
                Toast.makeText(requireContext(), "Selected: $selectedItem", Toast.LENGTH_SHORT).show()
                roomText.visibility = View.VISIBLE
                roomSpinner.visibility = View.VISIBLE
                isSpinnerInitialized = false
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        roomSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true
                    return
                }

                if (position == 0) {
                    // Default value selected, do nothing
                    return
                }
                val selectedItem = parent.getItemAtPosition(position).toString()
                Toast.makeText(requireContext(), "Selected: $selectedItem", Toast.LENGTH_SHORT).show()
                itemText.visibility = View.VISIBLE
                itemSpinner.visibility = View.VISIBLE
                isSpinnerInitialized = false
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        itemSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true
                    return
                }

                if (position == 0) {
                    // Default value selected, do nothing
                    return
                }

                val selectedItem = parent.getItemAtPosition(position).toString()
                Toast.makeText(requireContext(), "Selected: $selectedItem", Toast.LENGTH_SHORT).show()
                partText.visibility = View.VISIBLE
                partSpinner.visibility = View.VISIBLE
                isSpinnerInitialized = false
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        partSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true
                    return
                }

                if (position == 0) {
                    // Default value selected, do nothing
                    return
                }

                val selectedItem = parent.getItemAtPosition(position).toString()
                Toast.makeText(requireContext(), "Selected: $selectedItem", Toast.LENGTH_SHORT).show()
                startPauseBtn.visibility = View.VISIBLE
                cardView.visibility = View.VISIBLE
                isSpinnerInitialized = false
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        return binding.root
    }

    private fun setupObservers() {
        viewModel.items.observe(viewLifecycleOwner) { newItems ->
            items.clear()
            items.addAll(newItems)
            listAdapter.notifyDataSetChanged()
        }
    }

    private fun setUpUI() {
        mainActivity = requireActivity() as MainActivity
        objRfidScanner = mainActivity.objRfidScanner

        listAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, items)
        binding.searchResult.adapter = listAdapter

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

    override fun onPause() {
        super.onPause()
        objRfidScanner.stopReadTagLoop()
        scannedTags.clear()
        tagInfoMap.clear()
    }

  fun stopAll(){
        binding.roomSpinner.visibility = View.GONE
        binding.itemSpinner.visibility = View.GONE
        binding.partSpinner.visibility = View.GONE
        binding.roomText.visibility = View.GONE
        binding.itemText.visibility = View.GONE
        binding.partText.visibility = View.GONE
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
                    controlProgressBar(tag.rssi)
                } else {
                    viewModel.updateItem(currentTid, message)
                    controlProgressBar(tag.rssi)
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


}