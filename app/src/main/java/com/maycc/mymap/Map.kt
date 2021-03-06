package com.maycc.mymap

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.gson.Gson

class Map(var map: GoogleMap, var context: Context) : GoogleMap.OnMarkerClickListener{

    private var myMarker: Marker? = null
    private var myLocation: LatLng? = null
    private var route: Polyline? = null
    private var destinationMarker: Marker? = null

    @SuppressLint("MissingPermission")
    fun showLocationButton() {
        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true
    }

    fun showMarkerCurrentLocation(latLng: LatLng) {
        myLocation = latLng

        if (myMarker != null) {
            deleteMarkerPrevious(myMarker!!)
        }

        myMarker = map.addMarker(MarkerOptions().position(latLng).title("Tú"))
        myMarker?.snippet = "Estas aquí"
    }

    private fun deleteMarkerPrevious(marker: Marker) {
        marker.remove()
    }

    fun addStaticsMarkers() {
        val catamayoAirport = LatLng(-3.996828, -79.369961)
        val bolivarPark = LatLng(-3.995094, -79.204755)

        val markerCatamayoAirport = map.addMarker(MarkerOptions().position(catamayoAirport)
                .title("Aeropuerto Catamayo")) as Marker
                markerCatamayoAirport.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                markerCatamayoAirport.tag = 0

        map.addMarker(MarkerOptions().position(bolivarPark)
                .title("Parque Bolivar"))
                .tag = 0

        drawLine(catamayoAirport, bolivarPark)
    }

    private fun drawLine(locationOne: LatLng, locationTwo: LatLng) {
        val polylineOptions = PolylineOptions()
                .add(locationOne)
                .add(locationTwo)
                .color(Color.CYAN)

        map.addPolyline(polylineOptions)
    }

    fun setListenersMap() {
        map.setOnMarkerClickListener(this)
        addMarkerWithLongClick()
    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        var numberClicks = marker?.tag as? Int

        if (numberClicks != null) {
            numberClicks++
            marker?.tag = numberClicks
            Toast.makeText(context, "$numberClicks clicks", Toast.LENGTH_SHORT).show()
        }

        return false
    }

    private fun addMarkerWithLongClick() {
        map.setOnMapLongClickListener {
            location: LatLng? ->

                if (destinationMarker != null) {
                    deleteMarkerPrevious(destinationMarker!!)
                }

                destinationMarker = map.addMarker(MarkerOptions().position(location!!))
                destinationMarker?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                destinationMarker?.isDraggable = true

                val origin = "${myLocation?.latitude},${myLocation?.longitude}"
                val destination = "${location.latitude},${location.longitude}"
                val params = "origin=$origin&destination=$destination&mode=driving"

                val url = "https://maps.googleapis.com/maps/api/directions/json?$params"

                if (isAvailableNetwork(context)) {
                    makeRequestApiMaps(url)
                } else {
                    Toast.makeText(context, "No hay conección a internet para determinar la ruta", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun makeRequestApiMaps(url: String) {
        val requestQueue = Volley.newRequestQueue(context)

        val request = StringRequest(Request.Method.GET, url,
                Response.Listener<String> {
                    response ->  Log.d("RESPONSE", response)

                    if (route != null) {
                        route?.remove()
                    }

                    val routeResponse = mapOutJson(response)

                    val coordinates = getCoordinates(routeResponse)
                    traceRoute(coordinates)

                    showToastDistanceDurationTraceRoute(routeResponse)


                }, Response.ErrorListener {
                    error ->  Log.d("ERROR_RESPONSE", error.toString())
        })

        requestQueue.add(request)
    }

    private fun mapOutJson(jsonResponse: String): RouteResponse{
        val gSon = Gson()
        return gSon.fromJson(jsonResponse, RouteResponse::class.java)
    }

    private fun getCoordinates(routeResponse: RouteResponse): PolylineOptions {
        val coordinates = PolylineOptions()

        val points = routeResponse.routes[0].legs[0].steps

        for (point in points) {
            coordinates.add(point.start_location.toLatLng())
            coordinates.add(point.end_location.toLatLng())
        }

        return coordinates
    }

    private fun traceRoute(coordinates: PolylineOptions) {
        route = map.addPolyline(coordinates)
    }

    private fun showToastDistanceDurationTraceRoute(routeResponse: RouteResponse) {
        val distance = routeResponse.routes[0].legs[0].distance.text
        val duration = routeResponse.routes[0].legs[0].duration.text

        Toast.makeText(context, "$distance $duration", Toast.LENGTH_SHORT).show()
    }

}