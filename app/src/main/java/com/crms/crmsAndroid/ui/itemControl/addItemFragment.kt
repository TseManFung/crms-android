package com.crms.crmsAndroid.ui.itemControl

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.crms.crmsAndroid.R
import com.crms.crmsAndroid.databinding.FragmentSearchBinding

class addItemFragment : Fragment() {

    // Declare binding property
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    // Use ViewModel delegation 分配;委派;授權
    private val viewModel: AddItemViewModel by viewModels()

    // 伴生 object
    companion object {
        fun newInstance() = addItemFragment()
    }

    // onCreateView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate the layout using view binding
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    // onDestroyView
    override fun onDestroyView() {
        super.onDestroyView()
        // Clear binding reference
        _binding = null
    }
}