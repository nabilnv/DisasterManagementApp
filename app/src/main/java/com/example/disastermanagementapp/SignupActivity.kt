package com.example.disastermanagementapp

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val nameField: EditText = findViewById(R.id.nameField)
        val emailField: EditText = findViewById(R.id.emailField)
        val phoneField: EditText = findViewById(R.id.phoneField)
        val passwordField: EditText = findViewById(R.id.passwordField)
        val signupButton: Button = findViewById(R.id.signupButton)
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

        signupButton.setOnClickListener {
            val name = nameField.text.toString()
            val email = emailField.text.toString()
            val phone = phoneField.text.toString()
            val password = passwordField.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty() && phone.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Save user details to Firestore
                            val user = hashMapOf(
                                "name" to name,
                                "email" to email,
                                "phone" to phone
                            )
                            firestore.collection("users")
                                .document(auth.currentUser!!.uid)
                                .set(user)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, LoginActivity::class.java))
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error saving user data: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(this, "Signup failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill out all fields.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
