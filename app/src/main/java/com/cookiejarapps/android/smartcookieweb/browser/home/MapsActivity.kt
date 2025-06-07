package com.cookiejarapps.android.smartcookieweb.browser.home

import android.graphics.Color
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
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
import com.google.android.gms.maps.model.PolylineOptions
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
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var selectedPoiLatLng: LatLng? = null
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private lateinit var searchInput: AutoCompleteTextView
    private var predictionList: List<AutocompletePrediction> = emptyList()
    private var lastKnownLocation: Location? = null

    // Hårdkodad API-nyckel
    private val API_KEY = "AIzaSyDi-yYdHhrsyvpdl-lrICWv2XNdusxoVz4"
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        Places.initialize(applicationContext, API_KEY)
        placesClient = Places.createClient(this)

        searchInput = findViewById(R.id.search_input)
        val displayMetrics = resources.displayMetrics
        searchInput.dropDownWidth = displayMetrics.widthPixels
        val adapter = ArrayAdapter<String>(this, R.layout.maps_dropdown_item)
        searchInput.setAdapter(adapter)
        searchInput.threshold = 1

        // --- Lokal sökning med location bias ---
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                val loc = lastKnownLocation
                if (query.isNotEmpty() && loc != null) {
                    val center = LatLng(loc.latitude, loc.longitude)
                    val bounds = RectangularBounds.newInstance(
                        LatLng(center.latitude - 0.09, center.longitude - 0.09),
                        LatLng(center.latitude + 0.09, center.longitude + 0.09)
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
                } else if (query.isNotEmpty()) {
                    adapter.clear()
                    adapter.notifyDataSetChanged()
                    Toast.makeText(
                        this@MapsActivity,
                        "Väntar på din position för lokal sökning...",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })

        searchInput.setOnItemClickListener { _, _, position, _ ->
            val placeId = predictionList[position].placeId
            val placeFields = listOf(Place.Field.LAT_LNG)
            placesClient.fetchPlace(FetchPlaceRequest.builder(placeId, placeFields).build())
                .addOnSuccessListener { placeResponse ->
                    placeResponse.place.latLng?.let { destination ->
                        googleMap.clear()
                        googleMap.addMarker(MarkerOptions().position(destination).title("Destination"))
                        moveToMyLocationAndDrawRoute(destination)
                    }
                }
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val btnMapType = findViewById<ImageButton>(R.id.btn_maptype)
        btnMapType.setOnClickListener {
            googleMap.mapType = if (googleMap.mapType == GoogleMap.MAP_TYPE_NORMAL)
                GoogleMap.MAP_TYPE_SATELLITE else GoogleMap.MAP_TYPE_NORMAL
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        googleMap.setOnPoiClickListener { poi ->
            selectedPoiLatLng = poi.latLng // Spara koordinaten!
            googleMap.clear()
            googleMap.addMarker(MarkerOptions().position(poi.latLng).title(poi.name))?.showInfoWindow()
            fetchPoiDetailsAndShow(poi.placeId, poi.name)
        }

        googleMap.setOnMapLongClickListener { latLng ->
            googleMap.clear()
            googleMap.addMarker(MarkerOptions().position(latLng).title("Navigera hit"))
            moveToMyLocationAndDrawRoute(latLng)
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                lastKnownLocation = location
                location?.let {
                    val userLatLng = LatLng(it.latitude, it.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 16f))
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun fetchPoiDetailsAndShow(placeId: String, fallbackName: String) {
        val fields = listOf(
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.PHONE_NUMBER,
            Place.Field.WEBSITE_URI,
            Place.Field.RATING,
            Place.Field.USER_RATINGS_TOTAL,
            Place.Field.OPENING_HOURS,
            Place.Field.TYPES
        )
        val request = FetchPlaceRequest.builder(placeId, fields).build()
        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                val info = StringBuilder()
                info.append("Namn: ${place.name ?: fallbackName}\n")
                info.append("Adress: ${place.address ?: "-"}\n")
                if (place.phoneNumber != null) info.append("Telefon: ${place.phoneNumber}\n")
                if (place.websiteUri != null) info.append("Webb: ${place.websiteUri}\n")
                if (place.rating != null) info.append("Betyg: ${place.rating} (${place.userRatingsTotal} röster)\n")
                if (place.openingHours != null) {
                    info.append("Öppettider:\n")
                    place.openingHours?.weekdayText?.forEach { line -> info.append("$line\n") }
                }
                info.append("\n\nSenaste tillgänliga informationen")

                val message = SpannableString(info.toString())

                // Gör telefonnumret klickbart
                if (place.phoneNumber != null) {
                    val phone = place.phoneNumber!!
                    val start = info.indexOf("Telefon: ") + "Telefon: ".length
                    val end = start + phone.length
                    if (start >= "Namn: ${place.name ?: fallbackName}\nAdress: ${place.address ?: "-"}\n".length && end <= message.length) {
                        message.setSpan(object : ClickableSpan() {
                            override fun onClick(widget: View) {
                                val intent = Intent(Intent.ACTION_DIAL)
                                intent.data = Uri.parse("tel:$phone")
                                widget.context.startActivity(intent)
                            }
                        }, start, end, 0)
                    }
                }

                val dialog = AlertDialog.Builder(this)
                    .setTitle(place.name ?: fallbackName)
                    .setMessage(message)
                    .setNegativeButton("Stäng", null)
                    .create()
                dialog.show()

                // Gör länkar klickbara
                (dialog.findViewById<TextView>(android.R.id.message))?.movementMethod = LinkMovementMethod.getInstance()
            }
            .addOnFailureListener {
                AlertDialog.Builder(this)
                    .setTitle(fallbackName)
                    .setMessage("Det gick inte att hämta mer information om platsen.")
                    .setNegativeButton("Stäng", null)
                    .show()
            }
    }


    private fun moveToMyLocationAndDrawRoute(destination: LatLng) {
        val origin = lastKnownLocation
        if (origin == null) {
            Toast.makeText(this, "Din position hittas inte.", Toast.LENGTH_SHORT).show()
            return
        }
        val originLatLng = LatLng(origin.latitude, origin.longitude)
        googleMap.addMarker(MarkerOptions().position(originLatLng).title("Du"))
        drawRoute(originLatLng, destination)
        googleMap.animateCamera(
            CameraUpdateFactory.newLatLngBounds(
                com.google.android.gms.maps.model.LatLngBounds.Builder()
                    .include(originLatLng).include(destination).build(), 200
            )
        )
    }

    private fun drawRoute(origin: LatLng, destination: LatLng) {
        val originStr = "${origin.latitude},${origin.longitude}"
        val destinationStr = "${destination.latitude},${destination.longitude}"
        val url =
            "https://maps.googleapis.com/maps/api/directions/json?origin=$originStr&destination=$destinationStr&key=$API_KEY"

        thread {
            val conn = URL(url).openConnection() as HttpsURLConnection
            conn.requestMethod = "GET"
            val response = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            val routes = json.optJSONArray("routes")
            if (routes != null && routes.length() > 0) {
                val points = decodePolyline(
                    routes.getJSONObject(0)
                        .getJSONObject("overview_polyline")
                        .getString("points")
                )

                runOnUiThread {
                    googleMap.addPolyline(
                        PolylineOptions()
                            .addAll(points)
                            .color(Color.BLUE)    // Ändra till blå (Androids fördefinierade blå)
                            .width(16f)
                    )
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "Ingen väg hittades.", Toast.LENGTH_SHORT).show()
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

            poly.add(
                LatLng(
                    lat.toDouble() / 1E5,
                    lng.toDouble() / 1E5
                )
            )
        }
        return poly
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            onMapReady(googleMap)
        }
    }
}