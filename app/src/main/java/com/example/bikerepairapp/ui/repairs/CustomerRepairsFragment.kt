package com.example.bikerepairapp.ui.repairs

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bikerepairapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import androidx.navigation.fragment.findNavController
import android.widget.ImageButton

class CustomerRepairsFragment : Fragment(R.layout.fragment_customer_repairs) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var adapter: CustomerRepairsAdapter

    private fun confirmCompleted(reqId: String) {
        firestore.collection("requests").document(reqId)
            .update(
                mapOf(
                    "status" to "closed",
                    "closedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {
                if (isAdded) Toast.makeText(requireContext(), "Confirmed ✅", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                if (isAdded) Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val backBtn = view.findViewById<ImageButton>(R.id.btnBackCustomerRepairs)
        backBtn.setOnClickListener { findNavController().navigateUp() }

        val rv = view.findViewById<RecyclerView>(R.id.rvCustomerRepairs)
        rv.layoutManager = LinearLayoutManager(requireContext())

        adapter = CustomerRepairsAdapter { row ->
            confirmCompleted(row.id)
        }
        rv.adapter = adapter

        val uid = auth.currentUser?.uid ?: return

        // Backwards compatible:
        // old mode="accepted" will now behave like "active"
        val modeRaw = arguments?.getString("mode") ?: "all"
        val mode = if (modeRaw == "accepted") "active" else modeRaw

        val base = firestore.collection("requests")
            .whereEqualTo("customerId", uid)

        val query = when (mode) {
            "closed" -> base.whereEqualTo("status", "closed")

            "active" -> base.whereIn("status", listOf("pending", "accepted", "completed_pending"))

            else -> base // all
        }

        query
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener

                val list = snap.documents.mapNotNull { d ->
                    val issue = d.getString("issue") ?: return@mapNotNull null
                    val date = d.getString("date") ?: ""
                    val location = d.getString("location") ?: ""
                    val status = d.getString("status") ?: ""
                    val mechanicName = d.getString("mechanicName") ?: ""

                    CustomerRepairRow(d.id, issue, date, location, status, mechanicName)
                }

                adapter.submitList(list)
            }
    }
}

data class CustomerRepairRow(
    val id: String,
    val issue: String,
    val date: String,
    val location: String,
    val status: String,
    val mechanicName: String
)
