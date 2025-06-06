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
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private lateinit var searchInput: AutoCompleteTextView
    private var predictionList: List<AutocompletePrediction> = emptyList()

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // 1. Initiera Google Places SDK
        Places.initialize(applicationContext, "AIzaSyDi-yYdHhrsyvpdl-lrICWv2XNdusxoVz4")
        placesClient = Places.createClient(this)

        // 2. Initiera fusedLocationClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 3. Setup sökfältet
        searchInput = findViewById(R.id.search_input)
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line)
        searchInput.setAdapter(adapter)
        searchInput.threshold = 1

        // 4. Lyssna på val i listan
        searchInput.setOnItemClickListener { _, _, position, _ ->
            val prediction = predictionList[position]
            val placeId = prediction.placeId
            val placeFields = listOf(Place.Field.LAT_LNG, Place.Field.NAME)

            val request = FetchPlaceRequest.builder(placeId, placeFields).build()
            placesClient.fetchPlace(request).addOnSuccessListener { response ->
                val place = response.place
                place.latLng?.let { latLng ->
                    googleMap.clear()
                    googleMap.addMarker(MarkerOptions().position(latLng).title(place.name))
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                    showNavigateDialog(latLng)
                }
            }
        }

        // 5. Textändringar för autosuggest
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                if (query.isNotEmpty()) {
                    val request = FindAutocompletePredictionsRequest.builder()
                        .setQuery(query)
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
        })

        // 6. Karta
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // 7. Karttyp-knapp
        val btnMapType = findViewById<Button>(R.id.btn_maptype)
        btnMapType.text = "Satellit"
        btnMapType.setOnClickListener {
            if (::googleMap.isInitialized) {
                if (googleMap.mapType == GoogleMap.MAP_TYPE_NORMAL) {
                    googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                    btnMapType.text = "Standard"
                } else {
                    googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                    btnMapType.text = "Satellit"
                }
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // POI-klick
        googleMap.setOnPoiClickListener { poi ->
            googleMap.clear()
            val marker = googleMap.addMarker(
                MarkerOptions().position(poi.latLng).title(poi.name)
            )
            marker?.showInfoWindow()

            AlertDialog.Builder(this)
                .setTitle(poi.name)
                .setMessage("Vill du navigera hit?")
                .setPositiveButton("Ja") { _, _ ->
                    openGoogleMapsNavigation(poi.latLng)
                }
                .setNegativeButton("Nej", null)
                .show()
        }

        // Långtryck för att navigera
        googleMap.setOnMapLongClickListener { latLng ->
            googleMap.clear()
            googleMap.addMarker(
                MarkerOptions().position(latLng).title("Navigera hit")
            )?.showInfoWindow()

            showNavigateDialog(latLng)
        }

        // Användarens plats
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

    private fun showNavigateDialog(latLng: LatLng) {
        AlertDialog.Builder(this)
            .setTitle("Navigera hit")
            .setMessage("Vill du starta navigering till denna plats?")
            .setPositiveButton("Ja") { _, _ ->
                openGoogleMapsNavigation(latLng)
            }
            .setNegativeButton("Nej", null)
            .show()
    }

    private fun openGoogleMapsNavigation(latLng: LatLng) {
        val uri = "google.navigation:q=${latLng.latitude},${latLng.longitude}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        intent.setPackage("com.google.android.apps.maps")
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Google Maps är inte installerat.", Toast.LENGTH_SHORT).show()
        }
    }
}
