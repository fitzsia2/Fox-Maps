package com.foxmaps.maps.domain

interface MapsRepository {

    suspend fun getPlace(placeId: String): MapPlace
}
