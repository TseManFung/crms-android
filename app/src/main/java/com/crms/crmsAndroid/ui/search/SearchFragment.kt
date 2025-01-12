package com.crms.crmsAndroid.ui.search

import SearchViewModel
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
import androidx.fragment.app.viewModels
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

class SearchFragment : Fragment() , ITriggerDown, ITriggerLongPress {

    // Declare binding property
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel:SearchViewModel
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

        // Inflate the layout using view binding
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(SearchViewModel::class.java)

        //setupUI()
        setUpUI()

        setupObservers()

        // Flag to track spinner initialization
         var isSpinnerInitialized = false

        // Find the Spinner by its id
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

        // Create an ArrayAdapter using the string array and a default spinner layout
        val campusAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.campus_array,
            android.R.layout.simple_spinner_item
        )
        // Specify the layout to use when the list of choices appears
        campusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Apply the adapter to the spinner
        campusSpinner.adapter = campusAdapter

        //------------------------------------------------------------------------------------------
        // Create an ArrayAdapter for the room spinner
        val roomAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.cw_room_array,
            android.R.layout.simple_spinner_item
        )
        // Specify the layout to use when the list of choices appears
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Apply the adapter to the spinner
        roomSpinner.adapter = roomAdapter

        //------------------------------------------------------------------------------------------
        // Create an ArrayAdapter for the item spinner
        val itemAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.cw_room_349_item_array,
            android.R.layout.simple_spinner_item
        )

        // Specify the layout to use when the list of choices appears
        itemAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Apply the adapter to the spinner
        itemSpinner.adapter = itemAdapter

        //------------------------------------------------------------------------------------------
        // Create an ArrayAdapter for the part spinner
        val partAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.cw_room_349_item_robotdog_part_array,
            android.R.layout.simple_spinner_item
        )
        // Specify the layout to use when the list of choices appears
        partAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Apply the adapter to the spinner
        partSpinner.adapter = partAdapter

        //------------------------------------------------------------------------------------------
        // Set the onClickListener for the search button

        // Campus Spinner onItemSelectedListener
        campusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {

                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true
                    return
                }

                val selectedItem = parent.getItemAtPosition(position).toString()
                Toast.makeText(requireContext(), "Selected: $selectedItem", Toast.LENGTH_SHORT).show()
                roomText.visibility = View.VISIBLE
                roomSpinner.visibility = View.VISIBLE
                isSpinnerInitialized = false
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        // Room Spinner onItemSelectedListener
        roomSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {

                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true
                    return
                }

                val selectedItem = parent.getItemAtPosition(position).toString()
                Toast.makeText(requireContext(), "Selected: $selectedItem", Toast.LENGTH_SHORT).show()
                itemText.visibility = View.VISIBLE
                itemSpinner.visibility = View.VISIBLE
                isSpinnerInitialized = false
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        // Item Spinner onItemSelectedListener
        itemSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {

                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true
                    return
                }

                val selectedItem = parent.getItemAtPosition(position).toString()
                Toast.makeText(requireContext(), "Selected: $selectedItem", Toast.LENGTH_SHORT).show()
                partText.visibility = View.VISIBLE
                partSpinner.visibility = View.VISIBLE
                isSpinnerInitialized = false
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        // Part Spinner onItemSelectedListener
        partSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {

                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true
                    return
                }



                val selectedItem = parent.getItemAtPosition(position).toString()
                Toast.makeText(requireContext(), "Selected: $selectedItem", Toast.LENGTH_SHORT).show()
                startPauseBtn.visibility = View.VISIBLE
                cardView.visibility = View.VISIBLE
                isSpinnerInitialized = false
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
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

        // Initialize ListView
        listAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, items)
        binding.searchResult.adapter = listAdapter

        // Set up buttons
        binding.startPauseBtn.setOnClickListener{

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

    override fun onResume() {
        super.onResume()
        viewModel.clearItems()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear binding reference
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
            val rssi = value.toFloat() // Parse the value as a Float
            val progressBar: ProgressBar = binding.progressBar
            val percentage = binding.startPercentage
            var progress = ((rssi + 100) * 100 / 60).toInt() // Calculate the progress and convert to Int
            progress = progress.coerceAtMost(100) // Ensure the progress does not exceed 100
            progressBar.progress = progress
            percentage.text = "$progress%" // Set the text to show the percentage
        } catch (e: NumberFormatException) {
            appendTextToList("Error: Invalid number format for RSSI value: $value")
        }
    }

}
