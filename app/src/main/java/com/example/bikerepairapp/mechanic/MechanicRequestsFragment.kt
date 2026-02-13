package com.example.bikerepairapp.ui.mechanic

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bikerepairapp.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MechanicRequestsFragment : Fragment(R.layout.fragment_mechanic_requests) {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var incomingAdapter: IncomingRequestsAdapter
    private lateinit var activeAdapter: ActiveRequestsAdapter

    private lateinit var rvIncoming: RecyclerView
    private lateinit var rvActive: RecyclerView


    private var highlightRequestId: String? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        rvIncoming = view.findViewById(R.id.rvIncoming)
        rvActive = view.findViewById(R.id.rvActive)

        val btnIncoming = view.findViewById<MaterialButton>(R.id.btnTabIncoming)
        val btnActive = view.findViewById<MaterialButton>(R.id.btnTabActive)

        incomingAdapter = IncomingRequestsAdapter(
            onAccept = { acceptRequest(it) },
            onReject = { rejectRequest(it) }
        )

        activeAdapter = ActiveRequestsAdapter(
            onComplete = { completeRequest(it) },
            onRelease = { releaseRequest(it) }
        )

        rvIncoming.layoutManager = LinearLayoutManager(requireContext())
        rvIncoming.adapter = incomingAdapter

        rvActive.layoutManager = LinearLayoutManager(requireContext())
        rvActive.adapter = activeAdapter

        highlightRequestId = arguments?.getString("highlightRequestId")


        // ---------- UI-only tab styling (same look as customer) ----------
        fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

        fun applyTabSelection(isIncoming: Boolean) {
            val yellow = Color.parseColor("#FFFF00")
            val dark = Color.parseColor("#202020")

            fun selected(btn: MaterialButton) {
                btn.backgroundTintList = ColorStateList.valueOf(yellow)
                btn.setTextColor(Color.BLACK)
                btn.strokeWidth = 0
                btn.isAllCaps = false
            }

            fun unselected(btn: MaterialButton) {
                btn.backgroundTintList = ColorStateList.valueOf(dark)
                btn.setTextColor(Color.WHITE)
                btn.strokeColor = ColorStateList.valueOf(yellow)
                btn.strokeWidth = dp(1)
                btn.isAllCaps = false
            }

            if (isIncoming) {
                selected(btnIncoming); unselected(btnActive)
            } else {
                selected(btnActive); unselected(btnIncoming)
            }
        }

        // Default state
        rvIncoming.visibility = View.VISIBLE
        rvActive.visibility = View.GONE
        applyTabSelection(isIncoming = true)

        btnIncoming.setOnClickListener {
            rvIncoming.visibility = View.VISIBLE
            rvActive.visibility = View.GONE
            applyTabSelection(isIncoming = true)
        }
        btnActive.setOnClickListener {
            rvIncoming.visibility = View.GONE
            rvActive.visibility = View.VISIBLE
            applyTabSelection(isIncoming = false)
        }

        listenForIncoming()
        listenForActive()
    }

    private fun currentUid(): String? = auth.currentUser?.uid

    private fun listenForIncoming() {
        val uid = currentUid() ?: return

        db.collection("requests")
            .whereEqualTo("mechanicId", null)
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null) return@addSnapshotListener

                val list = snap.documents.mapNotNull { doc ->
                    val rejectedBy = doc.get("rejectedBy") as? List<*> ?: emptyList<Any>()
                    if (rejectedBy.contains(uid)) return@mapNotNull null

                    val status = doc.getString("status") ?: "pending"
                    if (status != "pending") return@mapNotNull null

                    RepairRequest(
                        id = doc.id,
                        issue = doc.getString("issue") ?: return@mapNotNull null,
                        date = doc.getString("date") ?: "",
                        location = doc.getString("location") ?: "",
                        status = status,
                        customerEmail = doc.getString("customerEmail") ?: "",
                        customerId = doc.getString("customerId") ?: "",
                        mechanicId = doc.getString("mechanicId"),
                        mechanicName = doc.getString("mechanicName"),
                        // ✅ NEW (see note below about RepairRequest)
                        customerName = doc.getString("customerName") ?: ""
                    )
                }

                incomingAdapter.submitList(list)

                highlightRequestId?.let { id ->
                    val index = list.indexOfFirst { it.id == id }

                    if (index != -1) {
                        rvIncoming.scrollToPosition(index)
                        incomingAdapter.highlightItem(id)

                        // Switch to Incoming tab automatically
                        rvIncoming.visibility = View.VISIBLE
                        rvActive.visibility = View.GONE
                    }
                }

            }
    }

    private fun listenForActive() {
        val uid = currentUid() ?: return

        db.collection("requests")
            .whereEqualTo("mechanicId", uid)
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null) return@addSnapshotListener

                val list = snap.documents.mapNotNull { doc ->
                    val status = doc.getString("status") ?: "accepted"

                    // ✅ Active list should show accepted OR waiting-for-confirmation
                    if (status != "accepted" && status != "completed_pending") return@mapNotNull null

                    RepairRequest(
                        id = doc.id,
                        issue = doc.getString("issue") ?: return@mapNotNull null,
                        date = doc.getString("date") ?: "",
                        location = doc.getString("location") ?: "",
                        status = status,
                        customerEmail = doc.getString("customerEmail") ?: "",
                        customerId = doc.getString("customerId") ?: "",
                        mechanicId = doc.getString("mechanicId"),
                        mechanicName = doc.getString("mechanicName"),
                        // ✅ NEW
                        customerName = doc.getString("customerName") ?: ""
                    )
                }

                activeAdapter.submitList(list)
                highlightRequestId?.let { id ->
                    val index = list.indexOfFirst { it.id == id }

                    if (index != -1) {
                        rvActive.scrollToPosition(index)
                        activeAdapter.highlightItem(id)

                        rvIncoming.visibility = View.GONE
                        rvActive.visibility = View.VISIBLE
                    }
                }
            }
    }

    private fun acceptRequest(req: RepairRequest) {
        val uid = currentUid() ?: return
        val name = auth.currentUser?.email ?: "mechanic"

        db.collection("requests").document(req.id)
            .update(
                mapOf(
                    "status" to "accepted",
                    "mechanicId" to uid,
                    "mechanicName" to name,
                    "acceptedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to accept", Toast.LENGTH_SHORT).show()
            }

    }

    private fun rejectRequest(req: RepairRequest) {
        val uid = currentUid() ?: return

        db.collection("requests").document(req.id)
            .update(
                mapOf("rejectedBy" to FieldValue.arrayUnion(uid))
            )
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to reject", Toast.LENGTH_SHORT).show()
            }
    }

    private fun completeRequest(req: RepairRequest) {
        val uid = currentUid() ?: return

        // ✅ IMPORTANT: this must match the customer listener
        db.collection("requests").document(req.id)
            .update(
                mapOf(
                    "status" to "completed_pending",
                    "completedAt" to FieldValue.serverTimestamp(),
                    "completedBy" to uid
                )
            )
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to mark completed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun releaseRequest(req: RepairRequest) {
        val uid = currentUid() ?: return

        db.collection("requests").document(req.id)
            .update(
                mapOf(
                    "status" to "pending",
                    "mechanicId" to null,
                    "mechanicName" to null,
                    "handledAt" to FieldValue.serverTimestamp(),
                    "rejectedBy" to FieldValue.arrayUnion(uid)
                )
            )
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to release", Toast.LENGTH_SHORT).show()
            }
    }
    // Add this inside onViewCreated or as a separate override
    override fun onResume() {
        super.onResume()
        // Check if a new ID was passed while the fragment was already "Active"
        val newId = arguments?.getString("highlightRequestId")
        if (newId != null) {
            highlightRequestId = newId
            // Trigger the scroll/highlight logic again
            listenForIncoming()
            listenForActive()
        }
    }
}
