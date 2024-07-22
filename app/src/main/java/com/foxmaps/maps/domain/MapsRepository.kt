package com.foxmaps.maps.domain


interface MapsRepository {

    suspend fun getPlace(placeId: String): MapPlace
    suspend fun getRoutes(from: Location, to: Location)
}
