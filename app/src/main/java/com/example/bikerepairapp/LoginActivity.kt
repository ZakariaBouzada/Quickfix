package com.example.bikerepairapp

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.timonknispel.ktloadingbutton.KTLoadingButton

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val emailInput = findViewById<EditText>(R.id.etEmail)
        val passwordInput = findViewById<EditText>(R.id.etPassword)
        val loginButton = findViewById<KTLoadingButton>(R.id.btnLogin)
        val goSignupText = findViewById<TextView>(R.id.tvGoSignup)

        // Already logged in -> go straight to correct UI
        auth.currentUser?.let { user ->
            loadRoleAndNavigate(user.uid)
            return
        }

        // ---------- LOGIN ----------
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (!isValidEmail(email)) {
                toast("Enter a valid email")
                loginButton.doResult(false) { it.reset() }
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                toast("Enter password")
                loginButton.doResult(false) { it.reset() }
                return@setOnClickListener
            }

            loginButton.startLoading()

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: return@addOnSuccessListener
                    loginButton.doResult(true) { btn ->
                        loadRoleAndNavigate(uid)
                        btn.reset()
                    }
                }
                .addOnFailureListener { e ->
                    loginButton.doResult(false) { btn -> btn.reset() }
                    toast("Login failed: ${e.message}")
                }
        }

        // ---------- GO TO SIGNUP ----------
        goSignupText.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun loadRoleAndNavigate(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { snap ->
                val role = snap.getString("role") ?: "customer"
                if (role == "mechanic") {
                    startActivity(Intent(this, MechanicActivity::class.java))
                } else {
                    val intent = Intent(this, BIkeRepairApp::class.java)
                    intent.putExtra("OPEN_BOOKING", true)
                    startActivity(intent)
                }
                finish()
            }
            .addOnFailureListener {
                startActivity(Intent(this, BIkeRepairApp::class.java))
                finish()
            }
    }

    private fun isValidEmail(email: String) =
        email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
