package com.maycc.mymap

import com.google.android.gms.maps.model.LatLng

class RouteResponse(var routes: ArrayList<Route>)

class Route(var legs: ArrayList<Leg>)

class Leg(var steps: ArrayList<Step>, var distance: Distance, var duration: Duration)

class Distance(var text: String)

class Duration(var text: String)

class Step(var end_location: LatLon, var start_location: LatLon)

class LatLon(var lat: Double, var lng: Double) {

    fun toLatLng() = LatLng(lat, lng)
}
