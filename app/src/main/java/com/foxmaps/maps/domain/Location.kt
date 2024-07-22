package com.foxmaps.maps.domain

import com.google.android.gms.maps.model.LatLng

data class Location(val latitude: Double, val longitude: Double)

fun LatLng.toLocation(): Location {
    return Location(latitude, longitude)
}
