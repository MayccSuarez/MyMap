package com.maycc.mymap

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.gson.Gson

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private val accessFineLocation = android.Manifest.permission.ACCESS_FINE_LOCATION
    private val accessCoarseLocation = android.Manifest.permission.ACCESS_COARSE_LOCATION
    private val permissionRequestCode = 100

    private val REQUEST_CHECK_SETTINGS = 500

    private lateinit var map: Map

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val locationRequest = LocationRequest()
    private var locationCallback: LocationCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationProviderClient = FusedLocationProviderClient(this)
        initLocationRequest()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a myMarker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = Map(googleMap, this)
        map.showLocationButton()
        map.addStaticsMarkers()
        map.setListenersMap()
    }

    private fun initLocationRequest() {
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private fun initLocationCallback() {
        locationCallback = object: LocationCallback() {

            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)

                for (location in locationResult?.locations!!) {
                    val myLocation = LatLng(location.latitude, location.longitude)
                    map.showMarkerCurrentLocation(myLocation)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocationUpdates() {
        initLocationCallback()
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    override fun onStart() {
        super.onStart()

        if (validatePermissions()) {
            checkLocationSettings()
        } else {
            askPermissions()
        }
    }

    private fun checkLocationSettings() {
        val builder = LocationSettingsRequest.Builder()
                                                .addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            locationSettingsResponse ->  getLocationUpdates()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(this@MapsActivity,
                            REQUEST_CHECK_SETTINGS)

                } catch (sendEx: IntentSender.SendIntentException) {

                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode) {
            REQUEST_CHECK_SETTINGS -> {

                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(applicationContext, "GPS HABILITADO", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(applicationContext, "GPS DESHABILITADO", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun validatePermissions(): Boolean {
        val isPermissionFineLocation = ActivityCompat.checkSelfPermission(this, accessFineLocation) == PackageManager.PERMISSION_GRANTED
        val isPermissionCoarseLocation= ActivityCompat.checkSelfPermission(this, accessCoarseLocation) == PackageManager.PERMISSION_GRANTED

        return isPermissionFineLocation && isPermissionCoarseLocation
    }

    private fun askPermissions() {
        val shouldShowMsj = ActivityCompat.shouldShowRequestPermissionRationale(this, accessFineLocation)

        if (shouldShowMsj) {
            showAlertOfExplication("Necesitamos saber tu ubicación para mostrarla en el mapa")
        } else {
            requestThePermissions()
        }
    }

    private fun requestThePermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(accessFineLocation, accessCoarseLocation), permissionRequestCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            permissionRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLocationUpdates()
                }
            }
        }
    }

    private fun showAlertOfExplication(txt: String) {
        AlertDialog.Builder(this).apply {
            title = "Explicación del permiso"
            setMessage(txt)

            setNegativeButton("CANCELAR", null)

            setPositiveButton("ACEPTAR") {dialog, which ->
                requestThePermissions()
            }

            show()
        }
    }
}
