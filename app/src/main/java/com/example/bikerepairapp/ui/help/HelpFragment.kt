package com.example.bikerepairapp.ui.help

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.bikerepairapp.R
import com.google.android.material.appbar.MaterialToolbar

class HelpFragment : Fragment(R.layout.fragment_help) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btnBackHelp).setOnClickListener {
            findNavController().popBackStack()
        }
    }

}
