package com.example.bikerepairapp.ui.mechanic

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bikerepairapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.appbar.MaterialToolbar
import androidx.navigation.fragment.findNavController

class MechanicCompletedFragment : Fragment(R.layout.fragment_mechanic_completed) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: CompletedRequestsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbarCompleted)
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }



        adapter = CompletedRequestsAdapter()

        val rv = view.findViewById<RecyclerView>(R.id.rvCompleted)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        listenForCompleted()
    }

    private fun listenForCompleted() {
        val uid = auth.currentUser?.uid ?: return

        // Using your existing "closed" as completed.
        // If later you switch to "completed_by_mechanic", change this filter only.
        db.collection("requests")
            .whereEqualTo("mechanicId", uid)
            .whereEqualTo("status", "closed")
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener

                val list = snap.documents.mapNotNull { doc ->
                    RepairRequest(
                        id = doc.id,
                        issue = doc.getString("issue") ?: return@mapNotNull null,
                        date = doc.getString("date") ?: "",
                        location = doc.getString("location") ?: "",
                        status = doc.getString("status") ?: "closed",
                        customerEmail = doc.getString("customerEmail") ?: "",
                        customerId = doc.getString("customerId") ?: "",
                        mechanicId = doc.getString("mechanicId"),
                        mechanicName = doc.getString("mechanicName")
                    )
                }

                adapter.submitList(list)
            }
    }
}
