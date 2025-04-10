package com.crms.crmsAndroid

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.crms.crmsAndroid.algorithm.RelativeDirectionCalculator
import com.crms.crmsAndroid.databinding.FragmentTestRfidBinding
import com.crms.crmsAndroid.scanner.rfidScanner
import com.crms.crmsAndroid.ui.ITriggerDown
import com.crms.crmsAndroid.ui.ITriggerLongPress
import com.rscja.deviceapi.entity.UHFTAGInfo

class TestRfidFragment : Fragment(), ITriggerDown, ITriggerLongPress {

    private var _binding: FragmentTestRfidBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TestRfidViewModel
    private lateinit var listAdapter: ArrayAdapter<String>
    private val items = mutableListOf<String>()
    private lateinit var mainActivity: MainActivity
    private lateinit var objRfidScanner: rfidScanner

    private lateinit var arrow: ImageView
    val targetTag = "E2801170200001D37340092B"
    val directionCalculator = RelativeDirectionCalculator(targetTag)
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

        // get data way from MainActivity
        objRfidScanner = mainActivity.objRfidScanner

        // put data to be shared between fragments here
        // val sharedViewModel = ViewModelProvider(mainActivity).get(SharedViewModel::class.java)
        // put
        // sharedViewModel.someData = "Your data here"
        // get
        // val data = sharedViewModel.someData

        // Initialize ListView
        listAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, items)
        binding.lvMain.adapter = listAdapter

        // Set up buttons
        binding.btnScan.setOnClickListener {
            handleBtnScanClick()
        }
        binding.btnStop.setOnClickListener {
            handleBtnStopClick()
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

        arrow = binding.arrow

        binding.seekSensitivity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                directionCalculator.distanceSensitivity = progress / 100.0
                Log.d("s", "distanceSensitivity: $directionCalculator.distanceSensitivity")
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.seekSmoothing.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                directionCalculator.angleSmoothingFactor = progress / 100.0
                Log.d("s", "angleSmoothingFactor: $directionCalculator.angleSmoothingFactor")
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        appendTextToList("RFID 版本: ${objRfidScanner.getVersion()}")

    }

    private fun setupObservers() {
        viewModel.items.observe(viewLifecycleOwner) { newItems ->
            items.clear()
            items.addAll(newItems)
            listAdapter.notifyDataSetChanged()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun handleBtnScanClick() {
        directionCalculator.clearData()
        try {
            objRfidScanner.readTagLoop(viewLifecycleOwner.lifecycleScope) { tag ->


                if (tag.tid == targetTag) {
                    val message =
                        """|EPC: ${tag.epc} |TID: ${tag.tid} |RSSI: ${tag.rssi} |Antenna: ${tag.ant} |Index: ${tag.index} |PC: ${tag.pc} |Remain: ${tag.remain} |Reserved: ${tag.reserved} |User: ${tag.user} """.trimMargin()
                    viewModel.updateItem(0, message)
                }
                processTag(tag)
                updateDirection()


            }

        } catch (e: Exception) {
            appendTextToList("Error: ${e.message}")
        }
    }

    private fun processTag(tag: UHFTAGInfo) {
        // 假设tag对象包含tid和rssi属性
        val tid = tag.tid ?: return
        val rssi = tag.rssi.toDouble()

        directionCalculator.updateTag(tid, rssi)
    }

    private fun updateDirection() {
        val directionRad = directionCalculator.calculateDirection() ?: return
        val directionDeg = Math.toDegrees(directionRad).toFloat()
        val message = "方向：%.1f° | 弧度: $directionRad".format(directionDeg)
        arrow.rotation = directionDeg+90
        viewModel.updateItem(1, message)
//        runOnUiThread {
//            val degrees = Math.toDegrees(directionRad).toFloat()
//            arrowView.rotation = degrees
//            tvDirection.text = "方向：%.1f°".format(degrees)
//        }
    }

    private fun handleBtnStopClick() {
        try {
            objRfidScanner.stopReadTagLoop()
            appendTextToList("Stop scanning")
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

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onTriggerDown() {
        appendTextToList("call by trigger down")
    }

    override fun onTriggerLongPress() {
        appendTextToList("call by trigger long press")
        handleBtnScanClick()
    }

    override fun onTriggerRelease() {
        appendTextToList("call by trigger release")
        handleBtnStopClick()

    }
}