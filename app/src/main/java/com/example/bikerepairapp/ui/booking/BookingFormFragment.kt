package com.example.bikerepairapp.ui.booking

import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.bikerepairapp.R
import com.example.bikerepairapp.data.database.AppDatabase
import com.example.bikerepairapp.data.model.BookingTicket
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import de.timonknispel.ktloadingbutton.KTLoadingButton
import kotlinx.coroutines.launch

class BookingFormFragment : Fragment(R.layout.fragment_booking_form) {

    private var selectedImageUri: Uri? = null
    private var selectedProblemType: String = "Tires"

    // Keeping these so you don't lose work, but we won't use "confirm completed" now
    private var lastRequestId: String? = null
    private var requestListener: ListenerRegistration? = null

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var successPlayer: MediaPlayer? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri

        val root = view ?: return@registerForActivityResult
        val imageView = root.findViewById<ImageView>(R.id.ivPhotoPreview)

        if (uri != null) {
            imageView.visibility = View.VISIBLE
            imageView.setImageURI(uri)
        } else {
            imageView.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        requestListener?.remove()
        requestListener = null

        successPlayer?.release()
        successPlayer = null

        // Reset ActionBar arrow so other screens don't get stuck with it
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(false)

        super.onDestroyView()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            findNavController().navigateUp()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle device back
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        // PNG back button in your XML
        val backBtn = view.findViewById<ImageButton>(R.id.btnBackBookingForm)
        backBtn.setOnClickListener { findNavController().navigateUp() }

        // (Optional) If you still want the ActionBar arrow too
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)

        val dateTimeInput = view.findViewById<EditText>(R.id.etDateTime)
        val locationInput = view.findViewById<EditText>(R.id.etLocation)
        val descInput = view.findViewById<EditText>(R.id.etDescription)

        val confirmButton = view.findViewById<KTLoadingButton>(R.id.btnConfirm)
        confirmButton.reset()

        // These exist in your XML right now — keep but hide on this screen
        val myBookingHeader = view.findViewById<TextView>(R.id.tvMyBookingHeader)
        val statusTitle = view.findViewById<TextView>(R.id.tvStatusTitle)
        val statusDetails = view.findViewById<TextView>(R.id.tvStatusDetails)

        myBookingHeader.visibility = View.GONE
        statusTitle.visibility = View.GONE
        statusDetails.visibility = View.GONE

        val addPhotoButton = view.findViewById<MaterialButton>(R.id.btnAddPhoto)
        addPhotoButton.setOnClickListener { pickImageLauncher.launch("image/*") }

        val btnTires = view.findViewById<MaterialButton>(R.id.btnTires)
        val btnChain = view.findViewById<MaterialButton>(R.id.btnChain)
        val btnOther = view.findViewById<MaterialButton>(R.id.btnOther)

        // Confirm-completed exists in XML — keep but hide for now
        val btnConfirmCompleted = view.findViewById<MaterialButton>(R.id.btnConfirmCompleted)
        btnConfirmCompleted.visibility = View.GONE

        val yellow = Color.parseColor("#FFFF00")
        val dark = Color.parseColor("#202020")

        fun styleSelected(b: MaterialButton) {
            b.backgroundTintList = ColorStateList.valueOf(yellow)
            b.setTextColor(Color.BLACK)
            b.strokeWidth = 0
        }

        fun styleUnselected(b: MaterialButton) {
            b.backgroundTintList = ColorStateList.valueOf(dark)
            b.setTextColor(Color.WHITE)
            b.strokeWidth = 2
            b.strokeColor = ColorStateList.valueOf(yellow)
        }

        fun applyProblemSelection(type: String) {
            selectedProblemType = type
            styleUnselected(btnTires)
            styleUnselected(btnChain)
            styleUnselected(btnOther)

            when (type) {
                "Tires" -> styleSelected(btnTires)
                "Chain" -> styleSelected(btnChain)
                "Other" -> styleSelected(btnOther)
            }
        }

        applyProblemSelection("Tires")
        btnTires.setOnClickListener { applyProblemSelection("Tires") }
        btnChain.setOnClickListener { applyProblemSelection("Chain") }
        btnOther.setOnClickListener { applyProblemSelection("Other") }

        val db = AppDatabase.getDatabase(requireContext())
        val ticketDao = db.ticketDao()

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Keeping your local ticket logic (hidden UI, but not deleting work)
        lifecycleScope.launch {
            val latestTicket = ticketDao.getLatestTicket()
            if (latestTicket != null) {
                showStatus(myBookingHeader, statusTitle, statusDetails, latestTicket)
            }
        }

        // SUBMIT booking
        confirmButton.setOnClickListener {
            // Always start from a clean state on click
            // (prevents "stuck spinning" from any earlier attempt)
            confirmButton.reset()
            confirmButton.isEnabled = true

            val whenText = dateTimeInput.text.toString().trim()
            val whereText = locationInput.text.toString().trim()
            val issueText = descInput.text.toString().trim()

            // Validation (if invalid: show toast + keep button normal)
            if (whenText.isBlank() || whereText.isBlank() || issueText.isBlank()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()

                // Optional: flash fail state quickly
                confirmButton.doResult(false)
                confirmButton.postDelayed({
                    confirmButton.reset()
                    confirmButton.isEnabled = true
                }, 600)

                return@setOnClickListener
            }

            val currentUser = auth.currentUser
            val customerId = currentUser?.uid
            val customerEmail = currentUser?.email

            if (customerId == null) {
                Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()

                confirmButton.doResult(false)
                confirmButton.postDelayed({
                    confirmButton.reset()
                    confirmButton.isEnabled = true
                }, 600)

                return@setOnClickListener
            }

            // Now we are actually submitting → start loading
            confirmButton.isEnabled = false
            confirmButton.startLoading()

            // 1) Room (local)
            val ticket = BookingTicket(
                id = 0,
                issue = issueText,
                date = whenText,
                location = whereText,
                status = "Requested",
                imageUri = selectedImageUri?.toString()
            )

            lifecycleScope.launch {
                ticketDao.insertTicket(ticket)
                showStatus(myBookingHeader, statusTitle, statusDetails, ticket)
            }

            // 2) Firestore
            firestore.collection("users").document(customerId).get()
                .addOnSuccessListener { snap ->
                    val customerName =
                        snap.getString("name")
                            ?: currentUser.displayName
                            ?: (customerEmail ?: "Customer")

                    val requestDoc = hashMapOf(
                        "customerId" to customerId,
                        "customerEmail" to customerEmail,
                        "customerName" to customerName,
                        "issue" to issueText,
                        "date" to whenText,
                        "location" to whereText,
                        "status" to "pending",
                        "imageUri" to (selectedImageUri?.toString()),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "mechanicId" to null,
                        "mechanicName" to null
                    )

                    firestore.collection("requests")
                        .add(requestDoc)
                        .addOnSuccessListener { docRef ->
                            lastRequestId = docRef.id
                            handleBookingSuccess(view, confirmButton)
                        }
                        .addOnFailureListener { e ->
                            handleBookingFailure(confirmButton, e.message)
                        }
                }
                .addOnFailureListener { e ->
                    handleBookingFailure(confirmButton, e.message)
                }


            .addOnFailureListener { e ->
                    // fallback: use email/displayName as name
                    val fallbackName = currentUser.displayName ?: (customerEmail ?: "Customer")

                    val requestDoc = hashMapOf(
                        "customerId" to customerId,
                        "customerEmail" to customerEmail,
                        "customerName" to fallbackName,
                        "issue" to issueText,
                        "date" to whenText,
                        "location" to whereText,
                        "status" to "pending",
                        "imageUri" to (selectedImageUri?.toString()),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "mechanicId" to null,
                        "mechanicName" to null
                    )

                    firestore.collection("requests")
                        .add(requestDoc)
                        .addOnSuccessListener { docRef ->
                            lastRequestId = docRef.id
                            handleBookingSuccess(view, confirmButton)
                        }
                        .addOnFailureListener { err ->
                            handleBookingFailure(confirmButton, err.message ?: e.message)
                        }
                }
        }
    }

    private fun handleBookingFailure(confirmButton: KTLoadingButton, msg: String?) {
        Toast.makeText(requireContext(), "Failed: ${msg ?: "Unknown error"}", Toast.LENGTH_LONG).show()

        // KTLoadingButton API
        confirmButton.doResult(false)

        confirmButton.postDelayed({
            confirmButton.reset()
            confirmButton.isEnabled = true
        }, 1200)
    }

    private fun handleBookingSuccess(rootView: View, confirmButton: KTLoadingButton) {
        Toast.makeText(requireContext(), "Booking sent ✅", Toast.LENGTH_SHORT).show()

        // Play sound (booking.mp3 -> res/raw/booking.mp3 => R.raw.booking)
        successPlayer?.release()
        successPlayer = MediaPlayer.create(requireContext(), R.raw.booking)
        successPlayer?.setOnCompletionListener {
            it.release()
            if (successPlayer === it) successPlayer = null
        }
        successPlayer?.start()

        // KTLoadingButton API
        confirmButton.doResult(true)

        rootView.postDelayed({
            findNavController().navigateUp()
        }, 350)
    }

    // Keeping these so you don't lose code; not used right now since "confirm completed" is later
    private fun attachCustomerRequestListener(
        myBookingHeader: TextView,
        statusTitle: TextView,
        statusDetails: TextView,
        btnConfirmCompleted: MaterialButton
    ) {
        val uid = auth.currentUser?.uid ?: return

        requestListener?.remove()
        requestListener = null

        requestListener = firestore.collection("requests")
            .whereEqualTo("customerId", uid)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                if (!isAdded) return@addSnapshotListener

                val latestDoc = snap.documents.maxByOrNull {
                    it.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                } ?: run {
                    btnConfirmCompleted.visibility = View.GONE
                    return@addSnapshotListener
                }

                lastRequestId = latestDoc.id

                val issue = latestDoc.getString("issue") ?: "—"
                val date = latestDoc.getString("date") ?: "—"
                val location = latestDoc.getString("location") ?: "—"
                val status = latestDoc.getString("status") ?: "pending"
                val mechanicName = latestDoc.getString("mechanicName") ?: "Not assigned yet"

                btnConfirmCompleted.visibility = View.GONE

                myBookingHeader.visibility = View.VISIBLE
                statusTitle.visibility = View.VISIBLE
                statusDetails.visibility = View.VISIBLE

                statusDetails.text = """
                    Issue: $issue
                    Time: $date
                    Location: $location

                    Status: $status
                    Mechanic: $mechanicName
                """.trimIndent()
            }
    }

    private fun confirmCompleted(reqId: String) {
        firestore.collection("requests").document(reqId)
            .update(
                mapOf(
                    "status" to "closed",
                    "closedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Confirmed ✅", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showStatus(
        header: TextView,
        title: TextView,
        details: TextView,
        ticket: BookingTicket
    ) {
        // Hidden on this screen, but keeping your logic
        header.visibility = View.GONE
        title.visibility = View.GONE
        details.visibility = View.GONE

        details.text = """
            Issue: ${ticket.issue}
            Time: ${ticket.date}
            Location: ${ticket.location}

            Status: ${ticket.status}
            Mechanic: Not assigned yet (local)
        """.trimIndent()
    }
}
