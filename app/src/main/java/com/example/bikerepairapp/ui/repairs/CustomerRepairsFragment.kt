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

    private lateinit var rv: RecyclerView

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

        rv = view.findViewById(R.id.rvCustomerRepairs)
        rv.layoutManager = LinearLayoutManager(requireContext())

        // Setup Adapter with Confirm AND Delete logic
        adapter = CustomerRepairsAdapter(
            onConfirm = { row -> confirmCompleted(row.id) },
            onDelete = { row -> showDeleteConfirmation(row) }
        )
        rv.adapter = adapter

        val uid = auth.currentUser?.uid ?: return
        val highlightId = arguments?.getString("highlightRequestId")

        // Firestore Query
        firestore.collection("requests")
            .whereEqualTo("customerId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener

                val list = snap.documents.mapNotNull { d ->
                    CustomerRepairRow(
                        id = d.id,
                        issue = d.getString("issue") ?: "",
                        date = d.getString("date") ?: "",
                        location = d.getString("location") ?: "",
                        status = d.getString("status") ?: "",
                        mechanicName = d.getString("mechanicName") ?: ""
                    )
                }

                adapter.submitList(list)

                // HIGHLIGHT LOGIC: Scroll to the item from the map
                if (highlightId != null) {
                    val index = list.indexOfFirst { it.id == highlightId }
                    if (index != -1) {
                        rv.post {
                            rv.smoothScrollToPosition(index) // smoothScroll is nicer than scrollTo
                            adapter.setHighlight(highlightId)

                            // REMOVE HIGHLIGHT AFTER 3 SECONDS
                            rv.postDelayed({
                                adapter.setHighlight(null)
                            }, 3000)
                        }
                    }
                }
            }
    }
    private fun showDeleteConfirmation(row: CustomerRepairRow) {
        if (row.status != "pending") {
            Toast.makeText(requireContext(), "Mechanic has already accepted this!", Toast.LENGTH_SHORT).show()
            return
        }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Cancel Request")
            .setMessage("Are you sure?")
            .setPositiveButton("Delete") { _, _ ->
                // 1. CALL THE DELETE
                firestore.collection("requests").document(row.id).delete()
                    .addOnSuccessListener {
                        if (isAdded) Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
                    }

                // 2. IMMEDIATE UI FIX: Remove it from the local list manually
                // This makes it vanish the millisecond you hit the button
                val currentList = adapter.getItems().toMutableList() // We'll add this helper to adapter
                currentList.removeAll { it.id == row.id }
                adapter.submitList(currentList)
            }
            .setNegativeButton("Back", null)
            .show()
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
