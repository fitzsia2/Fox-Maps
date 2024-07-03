package com.foxmaps.maps.data.google

import android.content.Context
import com.foxmaps.BuildConfig
import com.foxmaps.maps.data.MapsRemoteSource
import com.foxmaps.maps.domain.Location
import com.foxmaps.maps.domain.MapPlace
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.ktx.api.net.awaitFetchPhoto
import com.google.android.libraries.places.ktx.api.net.awaitFetchPlace
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class GoogleMapsRemoteSource @Inject constructor(
    context: Context,
) : MapsRemoteSource {

    private var placesClient: PlacesClient

    init {
        Places.initialize(context, BuildConfig.MAPS_API_KEY)
        placesClient = Places.createClient(context)
    }

    override suspend fun getPlace(placeId: String): MapPlace {
        val fields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.LAT_LNG,
            Place.Field.ADDRESS,
            Place.Field.PHOTO_METADATAS,
            Place.Field.EDITORIAL_SUMMARY,
        )

        val place = placesClient.awaitFetchPlace(placeId, fields).place

        val photos = place.photoMetadatas?.firstOrNull()?.let { photoMetadata ->
            val bitmap = placesClient.awaitFetchPhoto(photoMetadata).bitmap
            listOf(MapPlace.Photo(bitmap, photoMetadata.attributions))
        } ?: listOf()
        val location = Location(place.latLng!!.latitude, place.latLng!!.longitude)
        return MapPlace(place.name!!, location, place.id!!, photos, place.editorialSummary)
    }
}
