package com.example.disastermanagementapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.type.Date
import java.text.SimpleDateFormat
import java.util.Locale

class AlertsActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alerts)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Fetch and display detailed alerts
        val alertsContainer: LinearLayout = findViewById(R.id.alertsContainer) // LinearLayout to contain the cards
        fetchAlerts(alertsContainer)

        // Set up bottom navigation bar
        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNavigation)
        // Handle navigation item clicks
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_alerts -> {
                    true
                }

                R.id.nav_report -> {
                    startActivity(Intent(this, ReportActivity::class.java))
                    finish() // Finish AlertsActivity
                    true
                }

                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish() // Finish AlertsActivity
                    true
                }

                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish() // Finish AlertsActivity
                    true
                }

                else -> false
            }
        }

        // Set the selected item based on the current activity
        bottomNav.selectedItemId = R.id.nav_alerts  // For AlertsActivity
    }

    private fun fetchAlerts(container: LinearLayout) {
        firestore.collection("recent_alerts")
            .orderBy("timestamp") // Optional, to sort by timestamp
            .get()
            .addOnSuccessListener { querySnapshot ->
                container.removeAllViews() // Clear any previous views

                for (document in querySnapshot.documents) {
                    val typeOfHazards = document.getString("typeOfHazards") ?: "Unknown Hazard"
                    val place = document.getString("place") ?: "Unknown Place"
                    val safeZone = document.getString("safe_zone") ?: "Unknown"
                    val risk = document.getString("risk") ?: "Unknown Risk"
                    val timestamp = document.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis()
                    // Inflate the card layout
                    val alertCard = LayoutInflater.from(this).inflate(R.layout.alert_card, container, false)

                    // Format timestamp to desired date-time format
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())  // Customize the format as needed
                    val formattedTime = dateFormat.format(java.util.Date(timestamp))

                    // Set the data into the views inside the card
                    alertCard.findViewById<TextView>(R.id.alertTypeText).text = typeOfHazards
                    alertCard.findViewById<TextView>(R.id.alertPlaceText).text = place
                    alertCard.findViewById<TextView>(R.id.alertSafeZoneText).text = safeZone
                    alertCard.findViewById<TextView>(R.id.alertRiskText).text = risk
                    alertCard.findViewById<TextView>(R.id.alertTimeText).text = formattedTime


                    // Set text color based on the safe_zone value
                    val safeZoneText: TextView = alertCard.findViewById(R.id.alertSafeZoneText)

                    when (safeZone) {
                        "Low" -> safeZoneText.setTextColor(resources.getColor(R.color.green))  // Low = Green
                        "Medium" -> safeZoneText.setTextColor(resources.getColor(R.color.blue)) // Medium = Blue
                        "High" -> safeZoneText.setTextColor(resources.getColor(R.color.red))   // High = Red
                        else -> safeZoneText.setTextColor(resources.getColor(R.color.black))  // Default color (black)
                    }

                    // Set icons based on the safe_zone and risk values
                    val safeZoneIcon: ImageView = alertCard.findViewById(R.id.safeZoneIcon)
                    val riskIcon: ImageView = alertCard.findViewById(R.id.riskIcon)

                    // Example logic for icons (you can customize this)
                    when (safeZone) {
                        "Safe" -> safeZoneIcon.setImageResource(R.drawable.ic_safe_zone)
                        else -> safeZoneIcon.setImageResource(R.drawable.ic_danger_zone)
                    }

                    when (risk) {
                        "High" -> riskIcon.setImageResource(R.drawable.ic_high_risk)
                        "Low" -> riskIcon.setImageResource(R.drawable.ic_low_risk)
                        else -> riskIcon.setImageResource(R.drawable.ic_medium_risk)
                    }

                    // Add the card to the container
                    container.addView(alertCard)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load alerts: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
