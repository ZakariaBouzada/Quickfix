package com.example.bikerepairapp.ui.ratings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.Toast
import com.example.bikerepairapp.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class RateRepairBottomSheet : BottomSheetDialogFragment() {

    override fun getTheme(): Int = R.style.ThemeOverlay_QuickFix_BottomSheet

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var rbConvenience: RatingBar
    private lateinit var rbQuality: RatingBar
    private lateinit var rbMessaging: RatingBar
    private lateinit var btnSubmit: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottomsheet_rate_repair, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val requestId = requireArguments().getString(ARG_REQUEST_ID) ?: run {
            dismiss()
            return
        }

        rbConvenience = view.findViewById(R.id.rbConvenience)
        rbQuality = view.findViewById(R.id.rbQuality)
        rbMessaging = view.findViewById(R.id.rbMessaging)
        btnSubmit = view.findViewById(R.id.btnSubmitRating)

        btnSubmit.setOnClickListener {
            val c = rbConvenience.rating.toInt()
            val q = rbQuality.rating.toInt()
            val m = rbMessaging.rating.toInt()

            if (c == 0 || q == 0 || m == 0) {
                Toast.makeText(requireContext(), "Please rate all 3 ⭐", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            submitRating(requestId, c, q, m)
        }
    }

    private fun submitRating(requestId: String, convenience: Int, quality: Int, messaging: Int) {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val reqRef = firestore.collection("requests").document(requestId)

        // ✅ Minimal + safe: transaction prevents double-submit
        firestore.runTransaction { tx ->
            val reqSnap = tx.get(reqRef)

            val alreadyRated = reqSnap.getBoolean("ratingSubmitted") == true
            if (alreadyRated) return@runTransaction "already"

            val mechanicId = reqSnap.getString("mechanicId").orEmpty()
            if (mechanicId.isBlank()) return@runTransaction "no_mechanic"

            // Store rating on the request (easy to debug)
            tx.update(reqRef, mapOf(
                "status" to "closed",
                "closedAt" to FieldValue.serverTimestamp(),
                "ratingSubmitted" to true,
                "ratingAt" to FieldValue.serverTimestamp(),
                "ratingBy" to uid,
                "ratingConvenience" to convenience,
                "ratingQuality" to quality,
                "ratingMessaging" to messaging
            ))

            // Also store under mechanic (for later averaging on profile)
            val ratingDoc = firestore.collection("users")
                .document(mechanicId)
                .collection("ratings")
                .document()

            tx.set(ratingDoc, mapOf(
                "requestId" to requestId,
                "customerId" to uid,
                "convenience" to convenience,
                "quality" to quality,
                "messaging" to messaging,
                "createdAt" to FieldValue.serverTimestamp()
            ))

            "ok"
        }.addOnSuccessListener { result ->
            when (result) {
                "already" -> Toast.makeText(requireContext(), "Already rated ⭐", Toast.LENGTH_SHORT).show()
                "no_mechanic" -> Toast.makeText(requireContext(), "No mechanic assigned.", Toast.LENGTH_SHORT).show()
                else -> Toast.makeText(requireContext(), "Thanks for rating ⭐", Toast.LENGTH_SHORT).show()
            }
            dismiss()
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val ARG_REQUEST_ID = "requestId"

        fun newInstance(requestId: String): RateRepairBottomSheet {
            return RateRepairBottomSheet().apply {
                arguments = Bundle().apply { putString(ARG_REQUEST_ID, requestId) }
            }
        }
    }
}
