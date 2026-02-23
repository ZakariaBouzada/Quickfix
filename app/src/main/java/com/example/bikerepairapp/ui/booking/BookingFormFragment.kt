package com.example.bikerepairapp.ui.booking

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

class BookingFormFragment : Fragment(R.layout.fragment_booking_form) {

    private var selectedImageUri: Uri? = null
    private var selectedProblemType: String = "Tires"
    private var lastRequestId: String? = null
    private var requestListener: ListenerRegistration? = null

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var successPlayer: MediaPlayer? = null

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && selectedImageUri != null) {
            val imageView = view?.findViewById<ImageView>(R.id.ivPhotoPreview)
            imageView?.visibility = View.VISIBLE
            imageView?.setImageURI(selectedImageUri)
        }
    }

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
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Now that we have permission, trigger the camera logic again
            val uri = createImageUri()
            if (uri != null) {
                selectedImageUri = uri
                takePictureLauncher.launch(uri)
            }
        } else {
            Toast.makeText(requireContext(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        requestListener?.remove()
        requestListener = null
        successPlayer?.release()
        successPlayer = null
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        val backBtn = view.findViewById<ImageButton>(R.id.btnBackBookingForm)
        backBtn.setOnClickListener { findNavController().navigateUp() }

        (requireActivity() as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)

        val dateTimeInput = view.findViewById<EditText>(R.id.etDateTime)
        val locationInput = view.findViewById<EditText>(R.id.etLocation)
        val descInput = view.findViewById<EditText>(R.id.etDescription)
        // Check if we came from a map pin drop
        val prefilledAddress = arguments?.getString("prefillAddress")
        if (!prefilledAddress.isNullOrEmpty()) {
            locationInput.setText(prefilledAddress)
        }
        val confirmButton = view.findViewById<KTLoadingButton>(R.id.btnConfirm)
        confirmButton.reset()

        val myBookingHeader = view.findViewById<TextView>(R.id.tvMyBookingHeader)
        val statusTitle = view.findViewById<TextView>(R.id.tvStatusTitle)
        val statusDetails = view.findViewById<TextView>(R.id.tvStatusDetails)

        myBookingHeader.visibility = View.GONE
        statusTitle.visibility = View.GONE
        statusDetails.visibility = View.GONE

        val addPhotoButton = view.findViewById<MaterialButton>(R.id.btnAddPhoto)
        addPhotoButton.setOnClickListener {
            val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
            val builder = android.app.AlertDialog.Builder(requireContext())
            builder.setTitle("Add Photo")
            builder.setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(
                                requireContext(), android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            val uri = createImageUri()
                            selectedImageUri = uri
                            takePictureLauncher.launch(uri!!)
                        } else {
                            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    }
                    1 -> pickImageLauncher.launch("image/*")
                    2 -> dialog.dismiss()
                }
            }
            builder.show()
        }

        val btnTires = view.findViewById<MaterialButton>(R.id.btnTires)
        val btnChain = view.findViewById<MaterialButton>(R.id.btnChain)
        val btnOther = view.findViewById<MaterialButton>(R.id.btnOther)

        val yellow = Color.parseColor("#FFFF00")
        val dark = Color.parseColor("#202020")

        fun styleSelected(b: MaterialButton) {
            b.backgroundTintList = ColorStateList.valueOf(yellow)
            b.setTextColor(Color.BLACK)
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

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        confirmButton.setOnClickListener {
            val whenText = dateTimeInput.text.toString().trim()
            val whereText = locationInput.text.toString().trim()
            val issueText = descInput.text.toString().trim()

            val latLng = getLatLngFromAddress(whereText)
            if (latLng == null) {
                Toast.makeText(requireContext(), "Could not find address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (whenText.isBlank() || whereText.isBlank() || issueText.isBlank()) {
                Toast.makeText(requireContext(), "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = auth.currentUser ?: return@setOnClickListener

            confirmButton.isEnabled = false
            confirmButton.startLoading()

            firestore.collection("users").document(currentUser.uid).get()
                .addOnCompleteListener { nameTask ->
                    val snap = if (nameTask.isSuccessful) nameTask.result else null
                    val customerName = snap?.getString("name") ?: currentUser.displayName ?: "Customer"

                    val uriToUpload = selectedImageUri
                    if (uriToUpload != null) {
                        // Using the new direct upload function
                        uploadImageToStorage(uriToUpload) { cloudUrl ->
                            if (cloudUrl != null) {
                                saveBookingToFirestore(cloudUrl, customerName, latLng, whenText, whereText, issueText, confirmButton)
                            } else {
                                handleBookingFailure(confirmButton, "Photo upload failed")
                            }
                        }
                    } else {
                        saveBookingToFirestore(null, customerName, latLng, whenText, whereText, issueText, confirmButton)
                    }
                }
        }
    }

    private fun uploadImageToStorage(uri: Uri, onComplete: (String?) -> Unit) {
        val client = OkHttpClient()
        val context = requireContext()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: throw Exception("File empty")

                val mediaType = "image/jpeg".toMediaTypeOrNull()

                // Replace YOUR_API_KEY_HERE with your key from imgbb.com
                val apiKey = "11b91dd2094a5194a612e6408e0392c1"

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "repair.jpg", bytes.toRequestBody(mediaType))
                    .build()

                val request = Request.Builder()
                    .url("https://api.imgbb.com/1/upload?key=$apiKey")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseString = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(responseString)
                    val url = jsonResponse.getJSONObject("data").getString("url")
                    withContext(Dispatchers.Main) { onComplete(url) }
                } else {
                    Log.e("Upload", "Failed: $responseString")
                    withContext(Dispatchers.Main) { onComplete(null) }
                }
            } catch (e: Exception) {
                Log.e("Upload", "Error: ${e.message}")
                withContext(Dispatchers.Main) { onComplete(null) }
            }
        }
    }

    private fun saveBookingToFirestore(
        imageUrl: String?, name: String, latLng: Pair<Double, Double>,
        whenT: String, whereT: String, issue: String, btn: KTLoadingButton
    ) {
        val uid = auth.currentUser?.uid ?: return
        val requestDoc = hashMapOf(
            "customerId" to uid,
            "customerName" to name,
            "problemType" to selectedProblemType,
            "issue" to issue,
            "date" to whenT,
            "locationText" to whereT,
            "locationLat" to latLng.first,
            "locationLng" to latLng.second,
            "status" to "pending",
            "imageUri" to imageUrl,
            "createdAt" to FieldValue.serverTimestamp(),
            "mechanicId" to null,
            "mechanicName" to null,
            "rejectedBy" to arrayListOf<String>()
        )

        firestore.collection("requests").add(requestDoc)
            .addOnSuccessListener { handleBookingSuccess(requireView(), btn) }
            .addOnFailureListener { e -> handleBookingFailure(btn, e.message) }
    }

    private fun handleBookingFailure(btn: KTLoadingButton, msg: String?) {
        Toast.makeText(requireContext(), msg ?: "Error", Toast.LENGTH_SHORT).show()
        btn.doResult(false)
        btn.postDelayed({ btn.reset(); btn.isEnabled = true }, 1000)
    }

    private fun handleBookingSuccess(view: View, btn: KTLoadingButton) {
        btn.doResult(true)
        successPlayer = MediaPlayer.create(requireContext(), R.raw.booking)
        successPlayer?.start()
        view.postDelayed({ findNavController().navigateUp() }, 500)
    }

    private fun getLatLngFromAddress(address: String): Pair<Double, Double>? {
        return try {
            val geocoder = android.location.Geocoder(requireContext())
            val results = geocoder.getFromLocationName("$address, Turku, Finland", 1)
            if (!results.isNullOrEmpty()) Pair(results[0].latitude, results[0].longitude) else null
        } catch (e: Exception) { null }
    }

    private fun createImageUri(): Uri? {
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "repair_${System.currentTimeMillis()}.jpg")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }
        return requireContext().contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    @SuppressLint("SetTextI18n")
    private fun showStatus(h: TextView, t: TextView, d: TextView, ticket: BookingTicket) {
        h.visibility = View.GONE
    }
}