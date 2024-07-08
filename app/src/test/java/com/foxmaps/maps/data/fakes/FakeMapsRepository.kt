package com.foxmaps.maps.data.fakes

import com.foxmaps.maps.domain.MapPlace
import com.foxmaps.maps.domain.MapsRepository

class FakeMapsRepository : MapsRepository {

    override suspend fun getPlace(placeId: String): MapPlace {
        throw NotImplementedError()
    }
}
