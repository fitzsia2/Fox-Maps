package com.foxmaps.maps.data

import com.foxmaps.maps.domain.Location
import com.foxmaps.maps.domain.MapPlace

interface MapsRemoteSource {

    suspend fun getPlace(placeId: String): MapPlace
    suspend fun getRoute(origin: Location, destination: Location)
}
