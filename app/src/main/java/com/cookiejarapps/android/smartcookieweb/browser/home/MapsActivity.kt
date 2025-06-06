package com.cookiejarapps.android.smartcookieweb.browser.home

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cookiejarapps.android.smartcookieweb.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Hämta SupportMapFragment från layouten och registrera callback
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Kontrollera om platsbehörighet är beviljad
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            googleMap.isMyLocationEnabled = true

            // Försök att zooma till användarens position; annars fallback till Stockholm
            val fallback = LatLng(59.3293, 18.0686) // Stockholm
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fallback, 12f))

        } else {
            // Be om platsbehörighet om den inte är beviljad
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
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
                // Om användaren beviljade – aktivera My Location och zooma in
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    googleMap.isMyLocationEnabled = true

                    val userLocation: Location? = googleMap.myLocation
                    if (userLocation != null) {
                        val userLatLng = LatLng(userLocation.latitude, userLocation.longitude)
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 14f))
                    } else {
                        // Om vi inte får aktuell plats omedelbart, zooma till Stockholm som fallback
                        val fallback = LatLng(59.3293, 18.0686)
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fallback, 12f))
                    }
                }
            } else {
                // Om nekad, zooma till Stockholm som fallback
                val fallback = LatLng(59.3293, 18.0686)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fallback, 12f))
            }
        }
    }
}
