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
