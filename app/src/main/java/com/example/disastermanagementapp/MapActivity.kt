package com.example.disastermanagementapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.location.Geocoder
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline


class MapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var currentLocationButton: Button
    private lateinit var mapController: IMapController
    private lateinit var firestore: FirebaseFirestore
    private lateinit var searchEditText: EditText

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Fetch and display evacuation route
        fetchAllEvacuationRoutes()

        // Initialize map
        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapController = mapView.controller

        // Set initial zoom level and center
        mapController.setZoom(12.0) // City-level zoom
        mapController.setCenter(GeoPoint(3.1390, 101.6869)) // Example: Kuala Lumpur

        // Add current location button
        currentLocationButton = findViewById(R.id.currentLocationButton)
        currentLocationButton.setOnClickListener {
            showCurrentLocation()
        }



        // Draw initial empty route (optional, in case no data is available)
        drawEvacuationRoute(emptyList())



        // Initialize search bar
        searchEditText = findViewById(R.id.searchEditText)
        searchEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || event?.action == MotionEvent.ACTION_UP) {
                performSearch(v.text.toString())
                true
            } else {
                false
            }
        }

        // Fetch and display hazard alerts
        fetchHazardAlerts()
    }

    private fun fetchAllEvacuationRoutes() {
        firestore.collection("state")
            .get()
            .addOnSuccessListener { querySnapshot ->
                querySnapshot.documents.forEach { stateDocument ->
                    val stateId = stateDocument.id // Auto-generated document ID for the state
                    stateDocument.reference.collection("evacuation_routes")
                        .get()
                        .addOnSuccessListener { routeSnapshot ->
                            val points = mutableListOf<org.osmdroid.util.GeoPoint>()

                            routeSnapshot.documents.forEach { routeDocument ->
                                val geoPoint = routeDocument.getGeoPoint("evacuationRoute")
                                geoPoint?.let { gp ->
                                    // Convert Firestore GeoPoint to osmdroid GeoPoint
                                    val lat = gp.latitude
                                    val lon = gp.longitude
                                    points.add(org.osmdroid.util.GeoPoint(lat, lon))
                                }
                            }

                            // Check if there are enough points to draw a line
                            if (points.size > 1) {
                                // Draw the evacuation route on the map
                                drawEvacuationRoute(points)
                            } else {
                                Toast.makeText(
                                    this,
                                    "Not enough points to draw a route for state $stateId",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                                // Draw the evacuation route on the map
                                if (points.isNotEmpty()) {
                                    drawEvacuationRoute(points)
                                }
                            }

                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Failed to load evacuation routes for $stateId: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
    }
    private fun drawEvacuationRoute(routePoints: List<GeoPoint>) {
        println("Drawing evacuation route with ${routePoints.size} points")
        val polyline = Polyline().apply {
            setPoints(routePoints)  // Set the points for the polyline
            setColor(Color.BLUE)    // Set the color of the polyline
            setWidth(8f)            // Set the width of the polyline
        }
        println("Route Points: $routePoints")

        // Add the polyline to the map
        mapView.overlayManager.add(polyline)

        // Refresh the map to apply changes
        mapView.invalidate()
    }


    private fun performSearch(query: String) {
        if (query.isNotEmpty()) {
            // Use Geocoding API (e.g., Nominatim for OpenStreetMap) to get the location from the search query
            val geocoder = Geocoder(this)
            val results = geocoder.getFromLocationName(query, 1)
            if (results != null) {
                if (results.isNotEmpty()) {
                    val result = results?.get(0)
                    val geoPoint = result?.let { GeoPoint(it.latitude, result.longitude) }

                    // Zoom to the location and center the map
                    mapController.setCenter(geoPoint)
                    mapController.setZoom(16.0) // Adjust zoom level for the searched location
                } else {
                    Toast.makeText(this, "No results found for \"$query\"", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLocation = GeoPoint(location.latitude, location.longitude)

                    // Add a custom overlay for the user's current location
                    val currentLocationOverlay = object : Overlay() {
                        override fun draw(c: Canvas, osmv: MapView, shadow: Boolean) {
                            if (!shadow) {
                                val projection = osmv.projection
                                val screenPoint = projection.toPixels(currentLocation, null)

                                // Draw the custom icon at the user's location
                                val drawable = ContextCompat.getDrawable(
                                    this@MapActivity,
                                    R.drawable.current_location_icon
                                ) as BitmapDrawable
                                val bitmap: Bitmap = drawable.bitmap
                                c.drawBitmap(
                                    bitmap,
                                    screenPoint.x - bitmap.width / 2f,
                                    screenPoint.y - bitmap.height / 2f,
                                    Paint()
                                )
                            }
                        }
                    }

                    // Clear existing overlays and add the custom one
                    mapView.overlays.add(currentLocationOverlay)

                    // Zoom in closely to the user's current location
                    mapController.setZoom(18.0) // Street level zoom
                    mapController.setCenter(currentLocation)

                    mapView.invalidate() // Refresh the map to apply changes
                } else {
                    Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchHazardAlerts() {
        firestore.collection("recent_alerts")
            .get()
            .addOnSuccessListener { querySnapshot ->
                querySnapshot.documents.forEach { document ->
                    val location = document.getString("Location") ?: ""
                    val typeOfHazards = document.getString("typeOfHazards") ?: "Unknown"

                    val latLong = location.split(",")
                    if (latLong.size == 2) {
                        val latitude = latLong[0].toDoubleOrNull() ?: 0.0
                        val longitude = latLong[1].toDoubleOrNull() ?: 0.0
                        displayFixedSizeHazardMarker(latitude, longitude, typeOfHazards)
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load alerts: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayFixedSizeHazardMarker(latitude: Double, longitude: Double, typeOfHazards: String) {
        val point = GeoPoint(latitude, longitude)

        // Custom overlay to ensure fixed size icons
        val markerOverlay = object : Overlay() {
            override fun draw(c: Canvas, osmv: MapView, shadow: Boolean) {
                if (!shadow) {
                    val projection = osmv.projection
                    val screenPoint = projection.toPixels(point, null)

                    // Choose the appropriate icon
                    val drawable = when (typeOfHazards) {
                        "Flood" -> ContextCompat.getDrawable(this@MapActivity, R.drawable.flood_icon)
                        "Landslide" -> ContextCompat.getDrawable(this@MapActivity, R.drawable.landslide_icon)
                        "Haze" -> ContextCompat.getDrawable(this@MapActivity, R.drawable.haze_icon)
                        "Tsunami" -> ContextCompat.getDrawable(this@MapActivity, R.drawable.tsunami_icon)
                        else -> ContextCompat.getDrawable(this@MapActivity, R.drawable.default_icon)
                    }

                    // Convert drawable to Bitmap and ensure it's a fixed size (e.g., 100x100)
                    val bitmap = (drawable as BitmapDrawable).bitmap
                    val fixedSizeBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false)

                    // Draw the fixed size icon at the location
                    c.drawBitmap(
                        fixedSizeBitmap,
                        screenPoint.x - fixedSizeBitmap.width / 2f,
                        screenPoint.y - fixedSizeBitmap.height / 2f,
                        Paint()
                    )

                    // Draw the hazard type text above the icon
                    val paintText = Paint().apply {
                        color = Color.BLACK
                        textSize = 30f
                        isAntiAlias = true
                    }
                    c.drawText(
                        typeOfHazards,
                        screenPoint.x - fixedSizeBitmap.width / 2f,
                        screenPoint.y - fixedSizeBitmap.height / 2f - 10f,
                        paintText
                    )
                }
            }
        }

        // Add the marker to the map
        mapView.overlays.add(markerOverlay)
        mapView.invalidate() // Refresh the map to apply changes
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showCurrentLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}