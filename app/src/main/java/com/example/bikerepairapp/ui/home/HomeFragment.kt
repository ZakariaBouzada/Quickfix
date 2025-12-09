package com.example.bikerepairapp.ui.home

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bikerepairapp.R

// Firebase KTX imports
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase


class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find the button by ID from the inflated view
        val testButton: Button? = view.findViewById(R.id.btnTestFirebase)

        // reference to Firestore
        val db = Firebase.firestore

        testButton?.setOnClickListener {
            val testData = hashMapOf(
                "message" to "Hello Firebase!",
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("tests")
                .add(testData)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Uploaded to Firebase!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}
