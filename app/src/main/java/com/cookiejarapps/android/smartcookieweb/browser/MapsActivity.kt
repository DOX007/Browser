package com.cookiejarapps.android.smartcookieweb.browser

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
        // Använder tema utan ActionBar, enligt manifest
        setContentView(R.layout.activity_maps)

        // Hämta SupportMapFragment från layouten
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Kontrollera om plats‐tillstånd redan är beviljat
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            googleMap.isMyLocationEnabled = true

            // Zooma in på användarens position om tillgänglig, annars Stockholmskoordinater som fallback
            val fallback = LatLng(59.3293, 18.0686)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fallback, 12f))

        } else {
            // Be om plats‐tillstånd
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
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    googleMap.isMyLocationEnabled = true
                    val userLocation: Location? = googleMap.myLocation
                    if (userLocation != null) {
                        val userLatLng = LatLng(userLocation.latitude, userLocation.longitude)
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 14f))
                    } else {
                        val fallback = LatLng(59.3293, 18.0686)
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fallback, 12f))
                    }
                }
            } else {
                val fallback = LatLng(59.3293, 18.0686)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fallback, 12f))
            }
        }
    }
}