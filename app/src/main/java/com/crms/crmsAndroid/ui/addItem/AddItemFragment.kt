package com.crms.crmsAndroid.ui.addItem

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.crms.crmsAndroid.R
import com.crms.crmsAndroid.databinding.FragmentAddItemBinding

class AddItemFragment : Fragment() {

    private var _binding: FragmentAddItemBinding? = null


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val manNewRoomViewModel =
            ViewModelProvider(this).get(AddItemViewModel::class.java)

        _binding = FragmentAddItemBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val campusSpinner: Spinner = binding.campusSpinner
        val roomSpinner: Spinner = binding.roomSpinner
        val itemList: ListView = binding.listView
        val reConBtn: Button = binding.reconfirmbtn
        val scanItemBTN: Button = binding.scanItemBTN
        val resetBTN: Button = binding.resetbtn


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

        scanItemBTN.setOnClickListener {
            // show the reconfirm button when it is clicked
            reConBtn.visibility = View.VISIBLE
        }

        resetBTN.setOnClickListener {
            // hide the reconfirm button when it is clicked
            reConBtn.visibility = View.INVISIBLE
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}