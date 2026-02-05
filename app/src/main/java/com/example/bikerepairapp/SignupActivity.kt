package com.example.bikerepairapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import de.timonknispel.ktloadingbutton.KTLoadingButton

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val back = findViewById<TextView>(R.id.tvBackToLogin)
        val nameInput = findViewById<EditText>(R.id.etName)
        val emailInput = findViewById<EditText>(R.id.etEmail)
        val passwordInput = findViewById<EditText>(R.id.etPassword)
        val roleGroup = findViewById<RadioGroup>(R.id.roleGroup)
        val signUpButton = findViewById<KTLoadingButton>(R.id.btnSignUp)

        // UI pills (same IDs as your XML)
        val rbCustomer = findViewById<RadioButton>(R.id.rbCustomer)
        val rbMechanic = findViewById<RadioButton>(R.id.rbMechanic)

        back.setOnClickListener { finish() }

        // ---- UI ONLY: keep the pills pretty (NO logic change) ----
        fun updateRoleUi() {
            if (rbCustomer.isChecked) {
                rbCustomer.setBackgroundResource(R.drawable.qf_role_selected_bg)
                rbCustomer.setTextColor(android.graphics.Color.BLACK)

                rbMechanic.setBackgroundResource(R.drawable.qf_role_unselected_bg)
                rbMechanic.setTextColor(android.graphics.Color.WHITE)
            } else {
                rbMechanic.setBackgroundResource(R.drawable.qf_role_selected_bg)
                rbMechanic.setTextColor(android.graphics.Color.BLACK)

                rbCustomer.setBackgroundResource(R.drawable.qf_role_unselected_bg)
                rbCustomer.setTextColor(android.graphics.Color.WHITE)
            }
        }

        rbCustomer.setOnCheckedChangeListener { _, _ -> updateRoleUi() }
        rbMechanic.setOnCheckedChangeListener { _, _ -> updateRoleUi() }
        updateRoleUi()
        // ----------------------------------------------------------

        // ---------- SIGN UP (same logic you had) ----------
        signUpButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            val selectedRoleId = roleGroup.checkedRadioButtonId

            // VALIDATION FIRST
            if (name.isEmpty()) {
                toast("Enter your name")
                signUpButton.doResult(false) { it.reset() }
                return@setOnClickListener
            }
            if (!isValidEmail(email)) {
                toast("Enter a valid email")
                signUpButton.doResult(false) { it.reset() }
                return@setOnClickListener
            }
            if (password.length < 6) {
                toast("Password must be at least 6 characters")
                signUpButton.doResult(false) { it.reset() }
                return@setOnClickListener
            }
            if (selectedRoleId == View.NO_ID) {
                toast("Choose role (customer or mechanic)")
                signUpButton.doResult(false) { it.reset() }
                return@setOnClickListener
            }

            val role = when (selectedRoleId) {
                R.id.rbMechanic -> "mechanic"
                else -> "customer"
            }

            // start animation AFTER validation
            signUpButton.startLoading()

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: return@addOnSuccessListener

                    val userDoc = hashMapOf(
                        "name" to name,
                        "email" to email,
                        "role" to role,
                        "createdAt" to FieldValue.serverTimestamp()
                    )

                    db.collection("users").document(uid).set(userDoc)
                        .addOnSuccessListener {
                            signUpButton.doResult(true) { btn ->
                                toast("Account created as $role 🎉")
                                loadRoleAndNavigate(uid)
                                btn.reset()
                            }
                        }
                        .addOnFailureListener { e ->
                            signUpButton.doResult(false) { btn -> btn.reset() }
                            toast("User profile save failed: ${e.message}")
                        }
                }
                .addOnFailureListener { e ->
                    signUpButton.doResult(false) { btn -> btn.reset() }
                    toast("Sign up failed: ${e.message}")
                }
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
                val intent = Intent(this, BIkeRepairApp::class.java)
                startActivity(intent)
                finish()
            }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
