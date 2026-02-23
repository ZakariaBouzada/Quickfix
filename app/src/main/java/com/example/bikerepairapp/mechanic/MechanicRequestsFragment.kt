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
import com.google.firebase.firestore.ListenerRegistration


class MechanicRequestsFragment : Fragment(R.layout.fragment_mechanic_requests) {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var incomingAdapter: IncomingRequestsAdapter
    private lateinit var activeAdapter: ActiveRequestsAdapter

    private lateinit var rvIncoming: RecyclerView
    private lateinit var rvActive: RecyclerView


    private var highlightRequestId: String? = null
    private var requestedStatus: String? = null
    private var requestListener: ListenerRegistration? = null

    private var lastProcessedId: String? = null

    private lateinit var btnIncoming: MaterialButton
    private lateinit var btnActive: MaterialButton


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        rvIncoming = view.findViewById(R.id.rvIncoming)
        rvActive = view.findViewById(R.id.rvActive)

        btnIncoming = view.findViewById<MaterialButton>(R.id.btnTabIncoming)
        btnActive = view.findViewById<MaterialButton>(R.id.btnTabActive)

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

// NEW LOGIC: Decide which tab to open based on the marker clicked
        if (requestListener == null) {
            listenForIncoming()
            applyTabSelection(true, btnIncoming, btnActive)
        }

        btnIncoming.setOnClickListener {
            if (rvIncoming.visibility == View.VISIBLE) return@setOnClickListener
            rvIncoming.visibility = View.VISIBLE
            rvActive.visibility = View.GONE
            applyTabSelection(true, btnIncoming, btnActive)
            requestListener?.remove()
            listenForIncoming()
        }

        btnActive.setOnClickListener {
            if (rvActive.visibility == View.VISIBLE) return@setOnClickListener
            rvIncoming.visibility = View.GONE
            rvActive.visibility = View.VISIBLE
            applyTabSelection(false, btnIncoming, btnActive)
            requestListener?.remove()
            listenForActive()
        }
    }

    private fun applyTabSelection(isIncoming: Boolean, btnIncoming: MaterialButton, btnActive: MaterialButton) {
        val yellow = Color.parseColor("#FFFF00")
        val dark = Color.parseColor("#202020")
        val dp1 = (1 * resources.displayMetrics.density).toInt()

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
            btn.strokeWidth = dp1
            btn.isAllCaps = false
        }

        if (isIncoming) {
            selected(btnIncoming); unselected(btnActive)
        } else {
            selected(btnActive); unselected(btnIncoming)
        }
    }
    private fun currentUid(): String? = auth.currentUser?.uid

    private fun listenForIncoming() {
        val uid = currentUid() ?: return

        requestListener = db.collection("requests")
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
                        issue = doc.getString("issue") ?: "",
                        date = doc.getString("date") ?: "",
                        location = doc.getString("locationText") ?: "",
                        status = status,
                        customerEmail = doc.getString("customerEmail") ?: "",
                        customerId = doc.getString("customerId") ?: "",
                        mechanicId = doc.getString("mechanicId"),
                        mechanicName = doc.getString("mechanicName"),
                        // ✅ NEW (see note below about RepairRequest)
                        customerName = doc.getString("customerName") ?: "",
                        imageUri = doc.getString("imageUri")
                    )
                }

                incomingAdapter.submitList(list)
                rvIncoming.post {
                    processHighlight(list, isIncoming = true)
                }


            }
    }

    private fun listenForActive() {
        val uid = currentUid() ?: return

        requestListener = db.collection("requests")
            .whereEqualTo("mechanicId", uid)
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null) return@addSnapshotListener

                val list = snap.documents.mapNotNull { doc ->
                    val status = doc.getString("status") ?: "accepted"

                    // Active list should show accepted OR waiting-for-confirmation
                    if (status != "accepted" && status != "completed_pending") return@mapNotNull null

                    RepairRequest(
                        id = doc.id,
                        issue = doc.getString("issue") ?: "",
                        date = doc.getString("date") ?: "",
                        location = doc.getString("locationText") ?: "",
                        status = status,
                        customerEmail = doc.getString("customerEmail") ?: "",
                        customerId = doc.getString("customerId") ?: "",
                        mechanicId = doc.getString("mechanicId"),
                        mechanicName = doc.getString("mechanicName"),
                        // ✅ NEW
                        customerName = doc.getString("customerName") ?: "",
                        imageUri = doc.getString("imageUri")
                    )
                }

                activeAdapter.submitList(list)
                rvActive.post {
                    processHighlight(list, isIncoming = false)
                }

            }
    }

    private fun processHighlight(list: List<RepairRequest>, isIncoming: Boolean) {
        if (highlightRequestId == null) return // Nothing to do

        val index = list.indexOfFirst { it.id == highlightRequestId }
        if (index != -1) {
            if (isIncoming) {
                rvIncoming.scrollToPosition(index)
                incomingAdapter.highlightItem(highlightRequestId!!)
            } else {
                rvActive.scrollToPosition(index)
                activeAdapter.highlightItem(highlightRequestId!!)
            }

            // IMPORTANT: Clear the variable so we don't scroll again on every data update
            // but DON'T clear arguments?.remove here, let onResume handle the "Newness"
            highlightRequestId = null
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
                Toast.makeText(requireContext(), "Failed to mark completed", Toast.LENGTH_SHORT)
                    .show()
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

    override fun onResume() {
        super.onResume()

        // 1. Peek at the arguments without deleting them from the Bundle
        val incomingId = arguments?.getString("highlightRequestId")
        val incomingStatus = arguments?.getString("highlightStatus")

        android.util.Log.d("DEBUG_NAV", "Fragment received Status: $incomingStatus")

        // 2. Check if this ID is different from the one we just processed
        // This prevents the "infinite loop" without breaking the Bundle
        if (incomingId != null && incomingId != lastProcessedId) {
            highlightRequestId = incomingId
            lastProcessedId = incomingId // Store this in a new class variable

            val isAccepted = incomingStatus?.equals("accepted", ignoreCase = true) == true

            if (isAccepted) {
                rvIncoming.visibility = View.GONE
                rvActive.visibility = View.VISIBLE
                applyTabSelection(false, btnIncoming, btnActive)
                requestListener?.remove()
                listenForActive()
            } else {
                rvIncoming.visibility = View.VISIBLE
                rvActive.visibility = View.GONE
                applyTabSelection(true, btnIncoming, btnActive)
                requestListener?.remove()
                listenForIncoming()
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        // Stop listening to Firebase the second the user leaves this screen
        requestListener?.remove()
    }
}
