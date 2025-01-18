package com.example.disastermanagementapp

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val emailField: EditText = findViewById(R.id.emailField)
        val passwordField: EditText = findViewById(R.id.passwordField)
        val loginButton: Button = findViewById(R.id.loginButton)
        val createAccountText: TextView = findViewById(R.id.createAccountText)
        val passwordToggleIcon: ImageView = findViewById(R.id.passwordToggleIcon)

        // Password toggle logic
        passwordToggleIcon.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                // Show password
                passwordField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                passwordToggleIcon.setImageResource(R.drawable.ic_visibility_on) // Replace with your visible eye icon
            } else {
                // Hide password
                passwordField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                passwordToggleIcon.setImageResource(R.drawable.ic_visibility_off) // Replace with your hidden eye icon
            }
            // Move cursor to the end of the text
            passwordField.setSelection(passwordField.text.length)
        }

        loginButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Fetch user data from Firestore
                            val userId = auth.currentUser!!.uid
                            firestore.collection("users").document(userId)
                                .get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        val name = document.getString("name") ?: "Unknown"
                                        val phone = document.getString("phone") ?: "Unknown"
                                        Toast.makeText(this, "Welcome back, $name!", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, MainActivity::class.java))
                                        finish()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error fetching user data: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill out all fields.", Toast.LENGTH_SHORT).show()
            }
        }

        createAccountText.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}
