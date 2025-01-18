package com.example.disastermanagementapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ReportActivity : AppCompatActivity() {

    private lateinit var reportAdapter: ReportAdapter
    private val reports = mutableListOf<Report>()
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.report)

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Set up RecyclerView
        val reportsRecyclerView: RecyclerView = findViewById(R.id.reportsRecyclerView)
        reportAdapter = ReportAdapter(reports)
        reportsRecyclerView.adapter = reportAdapter
        reportsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Load reports from Firestore
        loadReportsFromFirestore()

        // Handle submit button click
        val submitReportButton: Button = findViewById(R.id.submitReportButton)
        submitReportButton.setOnClickListener {
            openSubmissionForm()
        }

        // Set up bottom navigation bar
        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_report -> true
                R.id.nav_alerts -> {
                    startActivity(Intent(this, AlertsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
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
        bottomNav.selectedItemId = R.id.nav_report
    }

    private fun loadReportsFromFirestore() {
        firestore.collection("community_reports")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ReportActivity", "Error fetching reports", error)
                    Toast.makeText(this, "Error loading reports: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    reports.clear()
                    for (document in snapshot.documents) {
                        val report = document.toObject(Report::class.java)
                        if (report != null) {
                            reports.add(report)
                        }
                    }
                    reportAdapter.notifyDataSetChanged()
                } else {
                    Log.e("ReportActivity", "Snapshot is null")
                }
            }
    }

    private fun openSubmissionForm() {
        val intent = Intent(this, SubmitReportActivity::class.java)
        startActivity(intent)
    }
}
