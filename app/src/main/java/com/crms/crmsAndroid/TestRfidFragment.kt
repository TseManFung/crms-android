package com.crms.crmsAndroid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.crms.crmsAndroid.algorithm.DirectionFinder
import com.crms.crmsAndroid.databinding.FragmentTestRfidBinding
import com.crms.crmsAndroid.scanner.rfidScanner
import com.crms.crmsAndroid.ui.ITriggerDown
import com.crms.crmsAndroid.ui.ITriggerLongPress
import com.crms.crmsAndroid.utils.CompassManager
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
    private lateinit var compassManager: CompassManager
    val targetTag = "E2801170200001D37340092B"
    val directionCalculator = DirectionFinder()
    val targetTagScannedRecord = mutableListOf<Double>()
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

        compassManager = CompassManager(requireContext()).apply {
            onHeadingChanged = { headingRad ->
                directionCalculator.setScannerHeading(headingRad)
            }
        }
        compassManager.start()

        appendTextToList("RFID 版本: ${objRfidScanner.getVersion()}")

    }

    private fun setupObservers() {
        viewModel.items.observe(viewLifecycleOwner) { newItems ->
            items.clear()
            items.addAll(newItems)
            listAdapter.notifyDataSetChanged()
        }
    }

    private fun handleBtnScanClick() {
        directionCalculator.clearData()
        directionCalculator.targetTag = targetTag
        targetTagScannedRecord.clear()
        try {
            objRfidScanner.readTagLoop(viewLifecycleOwner.lifecycleScope) { tag ->
                processTag(tag)

                if (tag.tid == targetTag) {
                    val message =
                        """|EPC: ${tag.epc} |TID: ${tag.tid} |RSSI: ${tag.rssi} |Antenna: ${tag.ant} |Index: ${tag.index} |PC: ${tag.pc} |Remain: ${tag.remain} |Reserved: ${tag.reserved} |User: ${tag.user} """.trimMargin()
                    viewModel.updateItem(0, message)
                    targetTagScannedRecord.add(tag.rssi.toDouble())
                    val distanceCm = directionCalculator.getEstimatedDistance()
                    val distanceText = if (distanceCm.isFinite()) {
                        String.format("%.1f cm", distanceCm)
                    } else {
                        "-- cm"
                    }
                    viewModel.updateItem(
                        2,
                        "平均target tag rssi: ${targetTagScannedRecord.average()} | 距離: $distanceText"
                    )
                }
                updateDirection()
            }

        } catch (e: Exception) {
            appendTextToList("Error: ${e.message}")
        }
    }

    private fun processTag(tag: UHFTAGInfo) {
        val tid = tag.tid ?: return
        val rssi = tag.rssi.toDouble()
        directionCalculator.setScannerHeading(compassManager.getHeadingRad())
        directionCalculator.updateTag(tid, rssi)
    }

    var lastDirectionDeg = 0.0F
    private fun updateDirection() {
        val bearingRad = directionCalculator.calculateDirection()
        if (!bearingRad.isFinite()) return

        val relativeDeg = Math.toDegrees(
            wrapToPi(bearingRad - directionCalculator.getScannerHeading())
        ).toFloat()
        val d = kotlin.math.abs(wrapDeg(relativeDeg - lastDirectionDeg))
        if (d < 3f) return

        lastDirectionDeg = relativeDeg
        val distanceCm = directionCalculator.getEstimatedDistance()
        val distanceText = if (distanceCm.isFinite()) {
            String.format("%.1f cm", distanceCm)
        } else {
            "-- cm"
        }
        val message = "方向：%.1f° | 距離: %s | 弧度: %.3f".format(
            relativeDeg,
            distanceText,
            bearingRad
        )
        arrow.rotation = relativeDeg + 90f
        viewModel.updateItem(1, message)
    }

    private fun wrapToPi(rad: Double): Double {
        var a = rad
        val pi = Math.PI
        while (a > pi) a -= 2.0 * pi
        while (a < -pi) a += 2.0 * pi
        return a
    }

    private fun wrapDeg(deg: Float): Float {
        var d = deg % 360f
        if (d > 180f) d -= 360f
        if (d < -180f) d += 360f
        return d
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

    override fun onResume() {
        super.onResume()
        if (::compassManager.isInitialized) {
            compassManager.start()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::compassManager.isInitialized) {
            compassManager.stop()
        }
        objRfidScanner.stopReadTagLoop()
    }

    override fun onDestroyView() {
        if (::compassManager.isInitialized) {
            compassManager.stop()
        }
        super.onDestroyView()
        _binding = null
    }

    override fun onTriggerDown() {
        appendTextToList("call by trigger down")
    }

    override fun onTriggerLongPress() {
        appendTextToList("call by trigger long press")
        if (!objRfidScanner.loopFlag) {
            handleBtnScanClick()

        }
    }

    override fun onTriggerRelease() {
        appendTextToList("call by trigger release")
        handleBtnStopClick()

    }
}