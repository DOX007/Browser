package com.cookiejarapps.android.smartcookieweb.browser.home

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cookiejarapps.android.smartcookieweb.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.thread

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private lateinit var searchInput: AutoCompleteTextView
    private var predictionList: List<AutocompletePrediction> = emptyList()

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val directionsApiKey = "AIzaSyDi-yYdHhrsyvpdl-lrICWv2XNdusxoVz4" // samma API-nyckel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        Places.initialize(applicationContext, directionsApiKey)
        placesClient = Places.createClient(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        searchInput = findViewById(R.id.search_input)
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line)
        searchInput.setAdapter(adapter)
        searchInput.threshold = 1

        searchInput.setOnItemClickListener { _, _, position, _ ->
            val prediction = predictionList[position]
            val placeId = prediction.placeId
            val placeFields = listOf(Place.Field.LAT_LNG, Place.Field.NAME)

            val request = FetchPlaceRequest.builder(placeId, placeFields).build()
            placesClient.fetchPlace(request).addOnSuccessListener { response ->
                val place = response.place
                place.latLng?.let { destination ->
                    googleMap.clear()
                    googleMap.addMarker(MarkerOptions().position(destination).title(place.name))
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(destination, 16f))
                    drawRouteTo(destination)
                }
            }
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                if (query.isNotEmpty()) {
                    val myLocation = googleMap.myLocation
                    if (myLocation != null) {
                        val center = LatLng(myLocation.latitude, myLocation.longitude)
                        val bounds = RectangularBounds.newInstance(
                            LatLng(center.latitude - 0.05, center.longitude - 0.05),
                            LatLng(center.latitude + 0.05, center.longitude + 0.05)
                        )
                        val request = FindAutocompletePredictionsRequest.builder()
                            .setQuery(query)
                            .setLocationBias(bounds)
                            .build()
                        placesClient.findAutocompletePredictions(request)
                            .addOnSuccessListener { response ->
                                predictionList = response.autocompletePredictions
                                adapter.clear()
                                adapter.addAll(predictionList.map { it.getFullText(null).toString() })
                                adapter.notifyDataSetChanged()
                            }
                    }
                }
            }
        })

        searchInput.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = v.text.toString()
                if (query.isNotEmpty()) {
                    val request = FindAutocompletePredictionsRequest.builder()
                        .setQuery(query)
                        .build()
                    placesClient.findAutocompletePredictions(request)
                        .addOnSuccessListener { response ->
                            predictionList = response.autocompletePredictions
                            if (predictionList.isNotEmpty()) {
                                val top = predictionList[0]
                                val placeId = top.placeId
                                val placeFields = listOf(Place.Field.LAT_LNG, Place.Field.NAME)
                                val fetchRequest = FetchPlaceRequest.builder(placeId, placeFields).build()
                                placesClient.fetchPlace(fetchRequest)
                                    .addOnSuccessListener { fetchResponse ->
                                        val place = fetchResponse.place
                                        place.latLng?.let { destination ->
                                            googleMap.clear()
                                            googleMap.addMarker(
                                                MarkerOptions().position(destination).title(place.name)
                                            )
                                            googleMap.animateCamera(
                                                CameraUpdateFactory.newLatLngZoom(destination, 16f)
                                            )
                                            drawRouteTo(destination)
                                        }
                                    }
                            }
                        }
                }
                true
            } else {
                false
            }
        })

        val btnMapType = findViewById<ImageButton>(R.id.btn_maptype)
        btnMapType.setImageResource(R.drawable.outline_navigation_24)

        btnMapType.setOnClickListener {
            if (::googleMap.isInitialized) {
                if (googleMap.mapType == GoogleMap.MAP_TYPE_NORMAL) {
                    googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                    btnMapType.setImageResource(R.drawable.baseline_satellite_alt_24)
                } else {
                    googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                    btnMapType.setImageResource(R.drawable.outline_navigation_24)
                }
            }
        }

    private fun drawRouteTo(destination: LatLng) {
        val origin = googleMap.myLocation
        if (origin != null) {
            val originStr = "${origin.latitude},${origin.longitude}"
            val destinationStr = "${destination.latitude},${destination.longitude}"
            val url = "https://maps.googleapis.com/maps/api/directions/json?origin=$originStr&destination=$destinationStr&key=$directionsApiKey"

            thread {
                val conn = URL(url).openConnection() as HttpsURLConnection
                conn.requestMethod = "GET"
                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val points = mutableListOf<LatLng>()
                val steps = json
                    .getJSONArray("routes")
                    .getJSONObject(0)
                    .getJSONObject("overview_polyline")
                    .getString("points")
                points.addAll(decodePolyline(steps))

                runOnUiThread {
                    val polylineOptions = PolylineOptions()
                        .addAll(points)
                        .color(ContextCompat.getColor(this, R.color.teal_700))
                        .width(10f)
                    googleMap.addPolyline(polylineOptions)
                }
            }
        }
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val latLng = LatLng(
                lat.toDouble() / 1E5,
                lng.toDouble() / 1E5
            )
            poly.add(latLng)
        }

        return poly
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        googleMap.setOnPoiClickListener { poi ->
            googleMap.clear()
            val marker = googleMap.addMarker(
                MarkerOptions().position(poi.latLng).title(poi.name)
            )
            marker?.showInfoWindow()
            drawRouteTo(poi.latLng)
        }

        googleMap.setOnMapLongClickListener { latLng ->
            googleMap.clear()
            googleMap.addMarker(
                MarkerOptions().position(latLng).title("Navigera hit")
            )?.showInfoWindow()
            drawRouteTo(latLng)
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 16f))
                } else {
                    val fallback = LatLng(59.3293, 18.0686)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fallback, 12f))
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    googleMap.isMyLocationEnabled = true
                    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            val userLatLng = LatLng(location.latitude, location.longitude)
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 16f))
                        } else {
                            val fallback = LatLng(59.3293, 18.0686)
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fallback, 12f))
                        }
                    }
                }
            } else {
                val fallback = LatLng(59.3293, 18.0686)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fallback, 12f))
            }
        }
    }
}

