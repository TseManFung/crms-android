package com.crms.crmsAndroid.ui.search

import SearchViewModel
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.crms.crmsAndroid.R
import com.crms.crmsAndroid.databinding.FragmentSearchBinding

class SearchFragment : Fragment() {

    // Declare binding property
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    // Use ViewModel delegation
    private val viewModel: SearchViewModel by viewModels()

    companion object {
        fun newInstance() = SearchFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate the layout using view binding
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        val root: View = binding.root



        // Find the Spinner by its id
        val campusSpinner: Spinner = binding.campusSpinner
        val roomSpinner: Spinner = binding.roomSpinner

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


       /*
       *  // Use ViewModel to observe data and bind it to UI
        viewModel.text.observe(viewLifecycleOwner) { text ->
            binding.searchItem.text = text
        }*/

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear binding reference
        _binding = null
    }
}