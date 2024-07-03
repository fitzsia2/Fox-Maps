package com.foxmaps.maps.domain

import android.graphics.Bitmap

data class MapPlace(
    val name: String,
    val location: Location,
    val id: String,
    val photos: List<Photo>,
    val description: String?,
) {

    data class Photo(val bitmap: Bitmap, val attributions: String)
}
