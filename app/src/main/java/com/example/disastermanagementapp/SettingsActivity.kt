package com.example.disastermanagementapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SettingsActivity : AppCompatActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Set up bottom navigation bar
        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNavigation)
        // Handle navigation item clicks
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_settings -> {
                    true
                }

                R.id.nav_alerts -> {
                    startActivity(Intent(this, AlertsActivity::class.java))
                    finish() // Finish MainActivity
                    true
                }

                R.id.nav_report -> {
                    startActivity(Intent(this, ReportActivity::class.java))
                    finish() // Finish MainActivity
                    true
                }


                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish() // Finish MainActivity
                    true
                }

                else -> false
            }
        }

        // Set the selected item based on the current activity
        bottomNav.selectedItemId = R.id.nav_settings  // For SettingsActivity


        // Get UI elements
        val profilePicture: ImageView = findViewById(R.id.profilePicture)
        val userName: TextView = findViewById(R.id.userName)
        val userPhone: TextView = findViewById(R.id.userPhone)
        val userEmailTextView: TextView = findViewById(R.id.userEmail)
        val userLocationTextView: TextView = findViewById(R.id.userLocation)
        val addEmergencyContactTextView: TextView = findViewById(R.id.addEmergencyContact)
        val emergencyContactContainer: LinearLayout = findViewById(R.id.emergencyContactContainer)
        val logoutButton: Button = findViewById(R.id.logoutButton)

        // Fetch user details
        val userEmail = auth.currentUser?.email
        if (userEmail != null) {
            firestore.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.documents.isNotEmpty()) {
                        val userDoc = querySnapshot.documents[0]
                        val name = userDoc.getString("name")
                        val phone = userDoc.getString("phone")
                        val profilePicUrl = userDoc.getString("profile_picture")
                        val location = userDoc.getString("location")

                        // Update UI
                        userName.text = name ?: "Name not found"
                        userPhone.text = phone ?: "Phone not found"
                        userEmailTextView.text = userEmail
                        userLocationTextView.text = location ?: "Location not found"

                        if (!profilePicUrl.isNullOrEmpty()) {
                            Glide.with(this)
                                .load(profilePicUrl)
                                .circleCrop()
                                .into(profilePicture)
                        }

                        fetchEmergencyContact(userDoc.id, emergencyContactContainer)

                        addEmergencyContactTextView.setOnClickListener {
                            showAddEmergencyContactDialog(userDoc.id)
                        }
                    } else {
                        showUserNotFound()
                    }
                }
                .addOnFailureListener {
                    showUserNotFound()
                }
        } else {
            showUserNotFound()
        }

        // Handle logout button click
        logoutButton.setOnClickListener {
            logoutUser()
        }
    }

    private fun showUserNotFound() {
        Toast.makeText(this, "User not found or no logged-in user", Toast.LENGTH_SHORT).show()
        findViewById<TextView>(R.id.userName).text = "No logged-in user"
        findViewById<TextView>(R.id.userPhone).text = "N/A"
        findViewById<TextView>(R.id.userEmail).text = "N/A"
        findViewById<TextView>(R.id.userLocation).text = "N/A"
    }

    private fun fetchEmergencyContact(userId: String, emergencyContactContainer: LinearLayout) {
        firestore.collection("users")
            .document(userId)
            .collection("emergency_contacts")
            .get()
            .addOnSuccessListener { querySnapshot ->
                emergencyContactContainer.removeAllViews()

                if (!querySnapshot.isEmpty) {
                    for (contactDoc in querySnapshot.documents) {
                        val name = contactDoc.getString("name") ?: "No Contact Name"
                        val phone = contactDoc.getString("phone") ?: "No Contact Phone"
                        val profilePicUrl = contactDoc.getString("profile_picture")
                        val contactId = contactDoc.id // Get the contact document ID for deletion
                        val contactView = layoutInflater.inflate(R.layout.item_emergency_contact, emergencyContactContainer, false)
                        val contactName: TextView = contactView.findViewById(R.id.contactName)
                        val contactPhone: TextView = contactView.findViewById(R.id.contactPhone)
                        val contactPicture: ImageView = contactView.findViewById(R.id.contactPicture)
                        val deleteContactButton: ImageView = contactView.findViewById(R.id.deleteContact)

                        contactName.text = name
                        contactPhone.text = phone
                        if (!profilePicUrl.isNullOrEmpty()) {
                            Glide.with(this)
                                .load(profilePicUrl)
                                .circleCrop()
                                .into(contactPicture)
                        }

                        // Set delete functionality for each emergency contact
                        deleteContactButton.setOnClickListener {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle("Delete Emergency Contact")
                            builder.setMessage("Are you sure you want to delete this contact?")

                            builder.setPositiveButton("Delete") { dialog, _ ->
                                // Proceed with deleting the contact
                                deleteEmergencyContact(userId, contactId)
                                dialog.dismiss() // Close the dialog after deleting
                            }

                            builder.setNegativeButton("Cancel") { dialog, _ ->
                                dialog.dismiss() // Close the dialog without deleting
                            }

                            builder.create().show() // Display the confirmation dialog
                        }
                        emergencyContactContainer.addView(contactView)
                    }
                } else {
                    Toast.makeText(this, "No Emergency Contacts Found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching emergency contacts", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddEmergencyContactDialog(userId: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add New Emergency Contact")

        val view = layoutInflater.inflate(R.layout.dialog_add_emergency_contact, null)
        val nameEditText = view.findViewById<TextInputEditText>(R.id.nameEditText)
        val phoneEditText = view.findViewById<TextInputEditText>(R.id.phoneEditText)

        builder.setView(view)
        builder.setPositiveButton("Add") { dialog, _ ->
            val name = nameEditText.text.toString()
            val phone = phoneEditText.text.toString()

            if (name.isNotEmpty() && phone.isNotEmpty()) {
                addEmergencyContactToFirestore(userId, name, phone)
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun addEmergencyContactToFirestore(userId: String, name: String, phone: String) {
        val contact = hashMapOf("name" to name, "phone" to phone)
        db.collection("users")
            .document(userId)
            .collection("emergency_contacts")
            .add(contact)
            .addOnSuccessListener {
                Toast.makeText(this, "Emergency contact added", Toast.LENGTH_SHORT).show()
                fetchEmergencyContact(userId, findViewById(R.id.emergencyContactContainer))
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error adding contact", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteEmergencyContact(userId: String, contactId: String) {
        firestore.collection("users")
            .document(userId)
            .collection("emergency_contacts")
            .document(contactId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Contact deleted successfully", Toast.LENGTH_SHORT).show()
                // Re-fetch the updated list of contacts
                fetchEmergencyContact(userId, findViewById(R.id.emergencyContactContainer))
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error deleting contact", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logoutUser() {
        auth.signOut() // Sign out the user
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

        // Redirect to LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Close the current activity
    }
}
