package com.example.disastermanagementapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

data class Report(
    val typeOfHazard: String = "",
    val severityLevel: String = "",
    val description: String = "",
    val location: String = "",
    val timestamp: Timestamp? = null
)

class ReportAdapter(private val reports: List<Report>) :
    RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private val reportsList = mutableListOf<Report>()

    class ReportViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val type: TextView = view.findViewById(R.id.typeText)
        val severity: TextView = view.findViewById(R.id.severityText)
        val description: TextView = view.findViewById(R.id.descriptionText)
        val location: TextView = view.findViewById(R.id.locationText)
        val timestamp: TextView = view.findViewById(R.id.timestampText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]
        holder.type.text = report.typeOfHazard
        holder.severity.text = report.severityLevel
        holder.description.text = report.description
        holder.location.text = report.location

        // Format timestamp
        report.timestamp?.let {
            val dateFormat = SimpleDateFormat("h:mm a, MMM d", Locale.getDefault())
            holder.timestamp.text = dateFormat.format(it.toDate())
        }
    }

    override fun getItemCount() = reports.size
}
