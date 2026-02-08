package com.example.bikerepairapp.ui.profile

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.bikerepairapp.LoginActivity
import com.example.bikerepairapp.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult
            uploadProfilePhoto(uri)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back
        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        // Local prefs for toggles (optional)
        val prefs = requireContext().getSharedPreferences("quickfix_prefs", 0)

        // Optional: App info text
        view.findViewById<TextView?>(R.id.tvAppInfo)?.text =
            "QuickFix v${getVersionName()} • Made in Turku"

        // Optional: Notification switches
        view.findViewById<SwitchMaterial?>(R.id.switchBookingUpdates)?.apply {
            isChecked = prefs.getBoolean("notif_booking_updates", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("notif_booking_updates", checked).apply()
            }
        }
        view.findViewById<SwitchMaterial?>(R.id.switchChatNotifications)?.apply {
            isChecked = prefs.getBoolean("notif_chat_messages", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("notif_chat_messages", checked).apply()
            }
        }

        // Optional: Support button -> Help
        view.findViewById<View?>(R.id.btnSupport)?.setOnClickListener {
            findNavController().navigate(R.id.helpFragment)
        }

        // Change profile photo
        view.findViewById<View>(R.id.btnChangeProfilePhoto).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Change email
        view.findViewById<View>(R.id.btnChangeEmail).setOnClickListener {
            promptChangeEmail()
        }

        // Change password
        view.findViewById<View>(R.id.btnChangePassword).setOnClickListener {
            promptChangePassword()
        }

        // Logout
        view.findViewById<View>(R.id.btnLogoutSettings).setOnClickListener {
            auth.signOut()
            goToLogin()
        }
    }

    // -------- Profile Photo Upload (Firebase Storage + Firestore photoUrl) --------

    private fun uploadProfilePhoto(uri: Uri) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        val uid = user.uid

        val ref = storage.reference.child("profile_photos/$uid.jpg")

        Toast.makeText(requireContext(), "Uploading photo…", Toast.LENGTH_SHORT).show()

        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl
                    .addOnSuccessListener { downloadUrl ->
                        db.collection("users").document(uid)
                            .update("photoUrl", downloadUrl.toString())
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Profile photo updated!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Saved photo but failed to update profile: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Upload ok but failed to get URL: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // -------- Change Email / Password (Re-auth required) --------

    private fun promptChangeEmail() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val input = EditText(requireContext()).apply {
            hint = "New email"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Change email")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Update") { _, _ ->
                val newEmail = input.text?.toString()?.trim().orEmpty()
                if (newEmail.isBlank()) {
                    Toast.makeText(requireContext(), "Email required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                promptReauth {
                    user.updateEmail(newEmail)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Email updated!", Toast.LENGTH_SHORT).show()
                            // Keep Firestore mirror field updated (if you store email there)
                            db.collection("users").document(user.uid).update("email", newEmail)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .show()
    }

    private fun promptChangePassword() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val input = EditText(requireContext()).apply {
            hint = "New password (min 6 chars)"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Change password")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Update") { _, _ ->
                val newPass = input.text?.toString()?.trim().orEmpty()
                if (newPass.length < 6) {
                    Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                promptReauth {
                    user.updatePassword(newPass)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Password updated!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .show()
    }

    private fun promptReauth(onSuccess: () -> Unit) {
        val user = auth.currentUser
        val email = user?.email
        if (user == null || email.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Cannot re-authenticate (missing email)", Toast.LENGTH_SHORT).show()
            return
        }

        val input = EditText(requireContext()).apply {
            hint = "Current password"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm password")
            .setMessage("For security, please enter your current password.")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm") { _, _ ->
                val password = input.text?.toString()?.trim().orEmpty()
                if (password.isBlank()) {
                    Toast.makeText(requireContext(), "Password required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val credential = EmailAuthProvider.getCredential(email, password)
                user.reauthenticate(credential)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Re-auth failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .show()
    }

    // -------- Helpers --------

    private fun getVersionName(): String {
        return try {
            val pm = requireContext().packageManager
            val pkg = requireContext().packageName
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0)
            }
            info.versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
    }

    private fun goToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }
}
