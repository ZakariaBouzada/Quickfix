package com.example.bikerepairapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
// --- Enable these when Firebase is properly configured ---
// import com.google.firebase.auth.FirebaseAuth
// import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    // --- Firebase (enable later) ---
    // private lateinit var auth: FirebaseAuth
    // private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // --- Firebase init (enable later) ---
        /*
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // If user is already logged in, skip login
        if (auth.currentUser != null) {
            goToMain()
            return
        }
        */

        val nameInput = findViewById<EditText>(R.id.etName)
        val emailInput = findViewById<EditText>(R.id.etEmail)
        val passwordInput = findViewById<EditText>(R.id.etPassword)
        val loginButton = findViewById<Button>(R.id.btnLogin)
        val signUpButton = findViewById<Button>(R.id.btnSignUp)

        // -------- FAKE LOGIN (no backend yet) --------
        loginButton.setOnClickListener {
            // Real Firebase login – enable later:
            /*
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    Toast.makeText(this, "Welcome back 👋", Toast.LENGTH_SHORT).show()
                    goToMain()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            */

            // Temporary fake behavior:
            Toast.makeText(this, "Fake login – going to app", Toast.LENGTH_SHORT).show()
            goToMain()
        }

        signUpButton.setOnClickListener {
            // Real Firebase signup – enable later:
            /*
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.length < 6) {
                Toast.makeText(
                    this,
                    "Name, email and password (min 6 chars) required",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: return@addOnSuccessListener

                    // Save basic profile in Firestore (backend-friendly)
                    val userDoc = hashMapOf(
                        "name" to name,
                        "email" to email
                    )
                    db.collection("users").document(uid).set(userDoc)

                    Toast.makeText(this, "Account created 🎉", Toast.LENGTH_SHORT).show()
                    goToMain()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Sign up failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            */

            // Temporary fake behavior:
            Toast.makeText(this, "Fake sign up – not saved yet", Toast.LENGTH_SHORT).show()
            goToMain()
        }
    }

    private fun goToMain() {
        val intent = Intent(this, BIkeRepairApp::class.java)
        intent.putExtra("OPEN_BOOKING", true)   // tell main activity to start on Booking
        startActivity(intent)
        finish()
    }
}
