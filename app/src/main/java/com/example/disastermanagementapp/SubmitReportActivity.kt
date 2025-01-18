package com.example.disastermanagementapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SubmitReportActivity : AppCompatActivity() {
    private lateinit var descriptionInput: EditText
    private lateinit var locationInput: EditText
    private lateinit var submitButton: Button

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    private var selectedHazard = ""
    private var selectedSeverity = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submit_report)

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initialize UI components
        descriptionInput = findViewById(R.id.descriptionInput)
        locationInput = findViewById(R.id.locationInput)
        submitButton = findViewById(R.id.submitButton)

        // Initialize hazard cards and TextView IDs
        val cardTsunami: CardView = findViewById(R.id.cardTsunami)
        val cardFlood: CardView = findViewById(R.id.cardFlood)
        val cardLandslide: CardView = findViewById(R.id.cardLandslide)
        val cardHaze: CardView = findViewById(R.id.cardHaze)

        val textTsunami: TextView = findViewById(R.id.textTsunami)
        val textFlood: TextView = findViewById(R.id.textFlood)
        val textLandslide: TextView = findViewById(R.id.textLandslide)
        val textHaze: TextView = findViewById(R.id.textHaze)

        val allHazardCards = listOf(cardTsunami, cardFlood, cardLandslide, cardHaze)
        val allHazardTexts = listOf(textTsunami, textFlood, textLandslide, textHaze)

        // Initialize severity cards and TextView IDs
        val cardMinor: CardView = findViewById(R.id.cardMinor)
        val cardModerate: CardView = findViewById(R.id.cardModerate)
        val cardSevere: CardView = findViewById(R.id.cardSevere)

        val textMinor: TextView = findViewById(R.id.textMinor)
        val textModerate: TextView = findViewById(R.id.textModerate)
        val textSevere: TextView = findViewById(R.id.textSevere)

        val allSeverityCards = listOf(cardMinor, cardModerate, cardSevere)
        val allSeverityTexts = listOf(textMinor, textModerate, textSevere)

        // Set click listeners for hazard cards
        for ((index, card) in allHazardCards.withIndex()) {
            card.setOnClickListener {
                updateTextAppearance(index, allHazardTexts)
                selectedHazard = allHazardTexts[index].text.toString()
            }
        }

        // Set click listeners for severity cards
        for ((index, card) in allSeverityCards.withIndex()) {
            card.setOnClickListener {
                updateTextAppearance(index, allSeverityTexts)
                selectedSeverity = allSeverityTexts[index].text.toString()
            }
        }

        // Set click listener for the submit button
        submitButton.setOnClickListener {
            val description = descriptionInput.text.toString()
            val location = locationInput.text.toString()

            if (description.isEmpty() || location.isEmpty() || selectedHazard.isEmpty() || selectedSeverity.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val report = hashMapOf(
                "typeOfHazard" to selectedHazard,
                "severityLevel" to selectedSeverity,
                "description" to description,
                "location" to location,
                "timestamp" to Timestamp.now()
            )

            db.collection("community_reports").add(report)
                .addOnSuccessListener {
                    Toast.makeText(this, "Report submitted successfully!", Toast.LENGTH_SHORT)
                        .show()
                    finish() // Close activity
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Failed to submit report: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    // Method to update text color appearance
    private fun updateTextAppearance(selectedIndex: Int, allTexts: List<TextView>) {
        for ((index, textView) in allTexts.withIndex()) {
            val color = if (index == selectedIndex) R.color.primaryColor else R.color.default_text_color
            textView.setTextColor(ContextCompat.getColor(this, color))
            textView.setTypeface(null, if (index == selectedIndex) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
    }
}
