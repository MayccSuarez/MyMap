package com.maycc.mymap

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.gson.Gson

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private val accessFineLocation = android.Manifest.permission.ACCESS_FINE_LOCATION
    private val accessCoarseLocation = android.Manifest.permission.ACCESS_COARSE_LOCATION
    private val permissionRequestCode = 100

    private lateinit var mMap: GoogleMap
    private var marker: Marker? = null

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val locationRequest = LocationRequest()
    private lateinit var locationCallback: LocationCallback

    private var myLocation: LatLng? = null

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
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        showLocationButton()
        setTypeMap()
        addStaticsMarkers()

        setListenersMap()
    }

    private fun setListenersMap() {
        mMap.setOnMarkerClickListener(this)
        addMarkerWithLongClick()
    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        var numberClicks = marker?.tag as? Int

        if (numberClicks != null) {
            numberClicks++
            marker?.tag = numberClicks
            Toast.makeText(this, "$numberClicks clicks", Toast.LENGTH_SHORT).show()
        }



        return false
    }

    private fun addMarkerWithLongClick() {
        mMap.setOnMapLongClickListener {
            location: LatLng? ->
                mMap.addMarker(MarkerOptions().position(location!!)).isDraggable = true

                val origin = "${myLocation?.latitude},${myLocation?.longitude}"
                val destination = "${location.latitude},${location.longitude}"
                val params = "origin=$origin&destination=$destination&mode=driving"

                val url = "https://maps.googleapis.com/maps/api/directions/json?$params"
                makeRequestApiMaps(url)
        }
    }

    private fun makeRequestApiMaps(url: String) {
        val requestQueue = Volley.newRequestQueue(this)

        val request = StringRequest(Request.Method.GET, url,
                        Response.Listener<String> {
                            response ->
                                Log.d("RESPONSE", response)
                                val coordinates = getCoordinates(response)
                                traceRoute(coordinates)

                        }, Response.ErrorListener {

                        })

        requestQueue.add(request)
    }

    private fun getCoordinates(jsonResponse: String): PolylineOptions {
        val coordinates = PolylineOptions()

        val gSon = Gson()
        val routeResponse= gSon.fromJson(jsonResponse, RouteResponse::class.java)

        val points = routeResponse.routes[0].legs[0].steps

        for (point in points) {
            coordinates.add(point.start_location.toLatLng())
            coordinates.add(point.end_location.toLatLng())
        }

        return coordinates
    }

    private fun traceRoute(coordinates: PolylineOptions) {
        mMap.addPolyline(coordinates)
    }

    @SuppressLint("MissingPermission")
    private fun showLocationButton() {
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
    }

    private fun setTypeMap() {
        mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
    }

    private fun addStaticsMarkers() {
        val catamayoAirport = LatLng(-3.996828, -79.369961)
        val bolivarPark = LatLng(-3.995094, -79.204755)

        val markerCatamayoAirport = mMap.addMarker(MarkerOptions().position(catamayoAirport)
                .title("Aeropuerto Catamayo")) as Marker
                markerCatamayoAirport.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                markerCatamayoAirport.tag = 0

        mMap.addMarker(MarkerOptions().position(bolivarPark)
                .title("Parque Bolivar"))
                .tag = 0

        drawLine(catamayoAirport, bolivarPark)
    }

    private fun drawLine(locationOne: LatLng, locationTwo: LatLng) {
        val polylineOptions = PolylineOptions()
                                        .add(locationOne)
                                        .add(locationTwo)
                                        .color(Color.CYAN)

        mMap.addPolyline(polylineOptions)
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

                if (marker != null) {
                    marker?.remove()
                }

                for (location in locationResult?.locations!!) {
                    val latitude = location.latitude
                    val longitude = location.longitude

                    myLocation = LatLng(latitude, longitude)
                    showMarker(myLocation!!)

                    Toast.makeText(applicationContext, "$longitude $latitude", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showMarker(latLng: LatLng) {
        marker = mMap.addMarker(MarkerOptions().position(latLng).title("Tú"))
        marker?.snippet = "Estas aquí"
        marker?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    @SuppressLint("MissingPermission")
    private fun getConstantLocation() {
        initLocationCallback()
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    override fun onStart() {
        super.onStart()

        if (validatePermissions()) {
            getConstantLocation()
        } else {
            askPermissions()
        }
    }

    override fun onPause() {
        super.onPause()

        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
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
                    getConstantLocation()
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
