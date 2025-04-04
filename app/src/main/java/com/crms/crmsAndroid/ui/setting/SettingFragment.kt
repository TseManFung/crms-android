package com.crms.crmsAndroid.ui.setting

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.crms.crmsAndroid.MainActivity
import com.crms.crmsAndroid.databinding.FragmentSettingBinding
import com.crms.crmsAndroid.scanner.rfidScanner

class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SettingViewModel
    private lateinit var mainActivity: MainActivity
    private lateinit var objRfidScanner: rfidScanner
    private var selectedPower: Int = 5

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(SettingViewModel::class.java)

        mainActivity = requireActivity() as MainActivity
        objRfidScanner = mainActivity.objRfidScanner

        setupUI()

        return binding.root
    }

    private fun setupUI() {
        val seekBarPower = binding.seekBarPower
        val tvCurrentPower = binding.tvCurrentPower
        val btnConfirm = binding.btnConfirm

        // 获取保存的功率值
        val sharedPreferences = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        selectedPower = sharedPreferences.getInt("power", 5)
        objRfidScanner.setPower(selectedPower)
        tvCurrentPower.text = "Current Power: $selectedPower"
        seekBarPower.progress = selectedPower - 5

        // 设置SeekBar监听器
        seekBarPower.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedPower = progress + 5
                tvCurrentPower.text = "Current Power: $selectedPower"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // 不需要实现
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 不需要实现
            }
        })

        // 设置确认按钮监听器
        btnConfirm.setOnClickListener {
            try {
                objRfidScanner.setPower(selectedPower)
                viewModel.setPower(selectedPower)
                sharedPreferences.edit().putInt("power", selectedPower).apply()
                Toast.makeText(requireContext(), "Power set to $selectedPower", Toast.LENGTH_SHORT).show()
            } catch (e: IllegalArgumentException) {
                tvCurrentPower.text = "Invalid Power Value"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}