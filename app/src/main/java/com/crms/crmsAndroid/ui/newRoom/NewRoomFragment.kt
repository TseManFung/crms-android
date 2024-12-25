package com.crms.crmsAndroid.ui.newRoom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.crms.crmsAndroid.databinding.FragmentNewRoomBinding

class NewRoomFragment : Fragment() {

    private var _binding: FragmentNewRoomBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val manNewRoomViewModel =
            ViewModelProvider(this).get(NewRoomViewModel::class.java)

        _binding = FragmentNewRoomBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textHome
        manNewRoomViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}