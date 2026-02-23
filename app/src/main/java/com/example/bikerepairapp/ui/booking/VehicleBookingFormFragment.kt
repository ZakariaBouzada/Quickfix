package com.example.bikerepairapp.ui.booking

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import de.timonknispel.ktloadingbutton.KTLoadingButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class VehicleBookingFormFragment : Fragment(R.layout.fragment_booking_form_vehicle) {

    private var vehicleType: String = "Car"

    private var selectedProblemType: String = "Engine"
    private var selectedImageUri: Uri? = null

    private var pickedLat: Double? = null
    private var pickedLng: Double? = null
    private var pickedAddress: String? = null

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var successPlayer: MediaPlayer? = null

    // Car brands
    private val carBrands = listOf(
        "Select brand…",
        "Audi", "BMW", "Chevrolet", "Citroën", "Fiat", "Ford", "Honda",
        "Hyundai", "Kia", "Mazda", "Mercedes-Benz", "Mitsubishi", "Nissan",
        "Opel", "Peugeot", "Renault", "Seat", "Skoda", "Subaru", "Suzuki",
        "Tesla", "Toyota", "Volkswagen", "Volvo", "Other"
    )

    // Motorbike brands
    private val motorbikeBrands = listOf(
        "Select brand…",
        "Aprilia", "BMW", "Ducati", "Harley-Davidson", "Honda", "Husqvarna",
        "Indian", "Kawasaki", "KTM", "MV Agusta", "Royal Enfield", "Suzuki",
        "Triumph", "Yamaha", "Other"
    )

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        val iv = view?.findViewById<ImageView>(R.id.ivPhotoPreviewV) ?: return@registerForActivityResult
        if (uri != null) {
            iv.visibility = View.VISIBLE
            iv.setImageURI(uri)
        } else {
            iv.visibility = View.GONE
        }
    }

    private val pickOnMapLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != AppCompatActivity.RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult

        val address = data.getStringExtra(OsmMapPickerActivity.EXTRA_ADDRESS)
        val lat = data.getDoubleExtra(OsmMapPickerActivity.EXTRA_LAT, Double.NaN)
        val lng = data.getDoubleExtra(OsmMapPickerActivity.EXTRA_LNG, Double.NaN)

        pickedAddress = address
        pickedLat = if (!lat.isNaN()) lat else null
        pickedLng = if (!lng.isNaN()) lng else null

        val root = view ?: return@registerForActivityResult
        val hint = root.findViewById<TextView>(R.id.tvPickedAddressHintV)
        val mapBtn = root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPickOnMapV)

        if (!address.isNullOrBlank()) {
            hint.text = "📍 $address"
            hint.setTextColor(android.graphics.Color.parseColor("#CCCCCC"))
            mapBtn.text = "📍  Change location"
        } else {
            hint.text = "Could not resolve address — try another point"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vehicleType = arguments?.getString("vehicleType") ?: "Car"

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Title
        view.findViewById<TextView>(R.id.tvVehicleFormTitle).text = "Book a $vehicleType repair"

        // Back
        view.findViewById<ImageButton>(R.id.btnBackVehicleForm).setOnClickListener {
            findNavController().navigateUp()
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        // Brand spinner — white text on dark bg
        val spinner = view.findViewById<Spinner>(R.id.spinnerBrand)
        val brands = if (vehicleType == "Car") carBrands else motorbikeBrands
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item_white,
            brands
        ).also { it.setDropDownViewResource(R.layout.spinner_dropdown_item_white) }
        spinner.adapter = adapter

        // Problem type buttons
        val btnEngine = view.findViewById<MaterialButton>(R.id.btnEngine)
        val btnBrakes = view.findViewById<MaterialButton>(R.id.btnBrakes)
        val btnTires  = view.findViewById<MaterialButton>(R.id.btnTiresV)
        val btnOther  = view.findViewById<MaterialButton>(R.id.btnOtherV)

        val yellow = Color.parseColor("#FFFF00")
        val dark   = Color.parseColor("#202020")

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
        fun applyProblem(type: String) {
            selectedProblemType = type
            listOf(btnEngine, btnBrakes, btnTires, btnOther).forEach { styleUnselected(it) }
            when (type) {
                "Engine" -> styleSelected(btnEngine)
                "Brakes" -> styleSelected(btnBrakes)
                "Tires"  -> styleSelected(btnTires)
                "Other"  -> styleSelected(btnOther)
            }
        }

        applyProblem("Engine")
        btnEngine.setOnClickListener { applyProblem("Engine") }
        btnBrakes.setOnClickListener { applyProblem("Brakes") }
        btnTires.setOnClickListener  { applyProblem("Tires") }
        btnOther.setOnClickListener  { applyProblem("Other") }

        // Date/time picker
        val dateTimeInput = view.findViewById<TextInputEditText>(R.id.etDateTimeV)
        val cal = Calendar.getInstance()

        fun formatDateTime(c: Calendar): String =
            SimpleDateFormat("EEE d MMM HH:mm", Locale.getDefault()).format(Date(c.timeInMillis))

        fun openTimePicker() {
            val tp = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(cal.get(Calendar.HOUR_OF_DAY))
                .setMinute(cal.get(Calendar.MINUTE))
                .setTitleText("Select time")
                .build()
            tp.addOnPositiveButtonClickListener {
                cal.set(Calendar.HOUR_OF_DAY, tp.hour)
                cal.set(Calendar.MINUTE, tp.minute)
                dateTimeInput.setText(formatDateTime(cal))
            }
            tp.show(parentFragmentManager, "time_picker_v")
        }

        fun openDatePicker() {
            val dp = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .setSelection(cal.timeInMillis)
                .build()
            dp.addOnPositiveButtonClickListener { selection ->
                cal.timeInMillis = selection
                openTimePicker()
            }
            dp.show(parentFragmentManager, "date_picker_v")
        }

        dateTimeInput.setOnClickListener { openDatePicker() }

        // Map picker
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPickOnMapV)
            .setOnClickListener {
                pickOnMapLauncher.launch(Intent(requireContext(), OsmMapPickerActivity::class.java))
            }

        // Photo
        view.findViewById<MaterialButton>(R.id.btnAddPhotoV).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Submit
        val confirmButton = view.findViewById<KTLoadingButton>(R.id.btnConfirmV)
        confirmButton.reset()

        confirmButton.setOnClickListener {
            confirmButton.reset()
            confirmButton.isEnabled = true

            val whenText  = dateTimeInput.text.toString().trim()
            val whereText = pickedAddress ?: ""
            val descText  = view.findViewById<TextInputEditText>(R.id.etDescriptionV).text.toString().trim()
            val brand     = spinner.selectedItem?.toString()?.takeIf { it != "Select brand…" } ?: ""
            val model     = view.findViewById<TextInputEditText>(R.id.etModel).text.toString().trim()
            val year      = view.findViewById<TextInputEditText>(R.id.etYear).text.toString().trim()

            if (whenText.isBlank() || descText.isBlank()) {
                Toast.makeText(requireContext(), "Please fill in date/time and description", Toast.LENGTH_SHORT).show()
                confirmButton.doResult(false)
                confirmButton.postDelayed({ confirmButton.reset(); confirmButton.isEnabled = true }, 600)
                return@setOnClickListener
            }

            if (pickedLat == null || pickedLng == null) {
                Toast.makeText(requireContext(), "Please pick a location on the map", Toast.LENGTH_SHORT).show()
                confirmButton.doResult(false)
                confirmButton.postDelayed({ confirmButton.reset(); confirmButton.isEnabled = true }, 600)
                return@setOnClickListener
            }

            val currentUser  = auth.currentUser
            val customerId   = currentUser?.uid
            val customerEmail = currentUser?.email

            if (customerId == null) {
                Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
                confirmButton.doResult(false)
                confirmButton.postDelayed({ confirmButton.reset(); confirmButton.isEnabled = true }, 600)
                return@setOnClickListener
            }

            confirmButton.isEnabled = false
            confirmButton.startLoading()

            // Build vehicle label for issue string
            val vehicleLabel = buildString {
                append(vehicleType)
                if (brand.isNotBlank()) append(" • $brand")
                if (model.isNotBlank()) append(" $model")
                if (year.isNotBlank()) append(" ($year)")
            }
            val fullIssue = "$vehicleLabel — $selectedProblemType: $descText"

            // Room local ticket
            val db = AppDatabase.getDatabase(requireContext())
            lifecycleScope.launch {
                db.ticketDao().insertTicket(
                    BookingTicket(
                        id = 0,
                        issue = fullIssue,
                        date = whenText,
                        location = whereText,
                        status = "Requested",
                        imageUri = selectedImageUri?.toString()
                    )
                )
            }

            // Firestore
            firestore.collection("users").document(customerId).get()
                .addOnSuccessListener { snap ->
                    val customerName = snap.getString("name")
                        ?: currentUser.displayName
                        ?: (customerEmail ?: "Customer")
                    submitToFirestore(
                        customerId, customerEmail, customerName,
                        fullIssue, whenText, whereText, brand, model, year,
                        confirmButton
                    )
                }
                .addOnFailureListener {
                    val fallback = currentUser.displayName ?: (customerEmail ?: "Customer")
                    submitToFirestore(
                        customerId, customerEmail, fallback,
                        fullIssue, whenText, whereText, brand, model, year,
                        confirmButton
                    )
                }
        }
    }

    private fun submitToFirestore(
        customerId: String,
        customerEmail: String?,
        customerName: String,
        fullIssue: String,
        whenText: String,
        whereText: String,
        brand: String,
        model: String,
        year: String,
        confirmButton: KTLoadingButton
    ) {
        val doc = hashMapOf<String, Any?>(
            "customerId"    to customerId,
            "customerEmail" to customerEmail,
            "customerName"  to customerName,
            "issue"         to fullIssue,
            "vehicleType"   to vehicleType,
            "vehicleBrand"  to brand.ifBlank { null },
            "vehicleModel"  to model.ifBlank { null },
            "vehicleYear"   to year.ifBlank { null },
            "date"          to whenText,
            "location"      to whereText,
            "status"        to "pending",
            "imageUri"      to selectedImageUri?.toString(),
            "createdAt"     to FieldValue.serverTimestamp(),
            "mechanicId"    to null,
            "mechanicName"  to null
        )

        pickedAddress?.let { doc["locationAddress"] = it }
        if (pickedLat != null && pickedLng != null) {
            doc["locationGeo"] = GeoPoint(pickedLat!!, pickedLng!!)
        }

        firestore.collection("requests").add(doc)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Booking sent ✅", Toast.LENGTH_SHORT).show()
                successPlayer?.release()
                successPlayer = MediaPlayer.create(requireContext(), R.raw.booking)
                successPlayer?.setOnCompletionListener { it.release(); successPlayer = null }
                successPlayer?.start()
                confirmButton.doResult(true)
                view?.postDelayed({ findNavController().navigateUp() }, 350)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                confirmButton.doResult(false)
                confirmButton.postDelayed({ confirmButton.reset(); confirmButton.isEnabled = true }, 1200)
            }
    }

    override fun onDestroyView() {
        successPlayer?.release()
        successPlayer = null
        super.onDestroyView()
    }
}
