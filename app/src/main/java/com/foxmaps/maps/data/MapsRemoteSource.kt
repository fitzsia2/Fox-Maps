package com.foxmaps.maps.data

import com.foxmaps.maps.domain.MapPlace

interface MapsRemoteSource {

    suspend fun getPlace(placeId: String): MapPlace
}
