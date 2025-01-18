package com.example.disastermanagementapp

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sensorManager: SensorManager
    private var proximitySensor: Sensor? = null
    private var lightSensor: Sensor? = null

    private lateinit var proximityTextView: TextView
    private lateinit var lightTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FirebaseApp.initializeApp(this)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Check Google Play Services
        when {
            GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS -> {
                // Handle Google Play Services not available
            }
        }

        // Initialize sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        // Initialize sensor text views
        proximityTextView = findViewById(R.id.proximityTextView)
        lightTextView = findViewById(R.id.lightTextView)

        // Set up bottom navigation
        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_alerts -> {
                    startActivity(Intent(this, AlertsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_report -> {
                    startActivity(Intent(this, ReportActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
        bottomNav.selectedItemId = R.id.nav_home

        // Set up buttons
        val emergencyCallButton: Button = findViewById(R.id.emergencyCallButton)
        val shareLocationButton: Button = findViewById(R.id.shareLocationButton)
        val viewAlertsButton: Button = findViewById(R.id.viewAlertsButton)

        val floodCard: CardView = findViewById(R.id.floodCard)
        floodCard.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("hazardType", "Flood")
            startActivity(intent)
        }

        val landslideCard: CardView = findViewById(R.id.landslideCard)
        landslideCard.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("hazardType", "Landslide")
            startActivity(intent)
        }

        val hazeCard: CardView = findViewById(R.id.hazeCard)
        hazeCard.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("hazardType", "Haze")
            startActivity(intent)
        }

        val tsunamiCard: CardView = findViewById(R.id.tsunamiCard)
        tsunamiCard.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("hazardType", "Tsunami")
            startActivity(intent)
        }


        emergencyCallButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:999")
            startActivity(intent)
        }

        shareLocationButton.setOnClickListener {
            Toast.makeText(this, "Location shared successfully!", Toast.LENGTH_SHORT).show()
        }

        viewAlertsButton.setOnClickListener {
            startActivity(Intent(this, AlertsActivity::class.java))
        }

        // Fetch recent alerts
        val recentAlertsContainer: LinearLayout = findViewById(R.id.recentAlertsContainer)
        fetchRecentAlerts(recentAlertsContainer)
    }


    private fun fetchRecentAlerts(container: LinearLayout) {
        firestore.collection("recent_alerts")
            .orderBy("timestamp") // Optional, to sort by timestamp
            .limit(2) // Limit the number of documents to 2
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


    private fun getTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / 60000
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes mins ago"
            else -> "${minutes / 60} hours ago"
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_PROXIMITY -> {
                val proximityValue = event.values[0]
                proximityTextView.text = "Proximity: $proximityValue cm"
            }
            Sensor.TYPE_LIGHT -> {
                val lightValue = event.values[0]
                lightTextView.text = "Light: $lightValue lx"
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle changes in sensor accuracy if needed
    }

    override fun onResume() {
        super.onResume()
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
}
