package com.foxmaps.maps.data

import com.foxmaps.maps.domain.Location
import com.foxmaps.maps.domain.MapPlace
import com.foxmaps.maps.domain.MapsRepository
import javax.inject.Inject

class MapsRepositoryImpl @Inject constructor(
    private val mapsRemoteSource: MapsRemoteSource
) : MapsRepository {

    override suspend fun getPlace(placeId: String): MapPlace {
        return mapsRemoteSource.getPlace(placeId)
    }

    override suspend fun getRoutes(from: Location, to: Location) {
        mapsRemoteSource.getRoute(from, to)
    }
}
