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
import com.google.maps.routing.v2.ComputeRoutesRequest
import com.google.maps.routing.v2.RoutesClient
import com.google.maps.routing.v2.Waypoint
import com.google.type.LatLng
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber
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

        val place = placesClient.awaitFetchPlace(placeId, getPlaceRequestPlaceFields).place

        val photos = place.photoMetadatas?.firstOrNull()?.let { photoMetadata ->
            val bitmap = placesClient.awaitFetchPhoto(photoMetadata).bitmap
            listOf(MapPlace.Photo(bitmap, photoMetadata.attributions))
        } ?: listOf()
        val location = Location(place.latLng!!.latitude, place.latLng!!.longitude)
        return MapPlace(place.name!!, location, place.id!!, photos, place.editorialSummary)
    }

    override suspend fun getRoute(origin: Location, destination: Location) {
        val client = RoutesClient.create()
        val newBuilder = ComputeRoutesRequest.newBuilder()
        val originLatLng = LatLng.newBuilder()
            .setLatitude(origin.latitude)
            .setLongitude(origin.longitude)
        val location = com.google.maps.routing.v2.Location.newBuilder()
            .setLatLng(originLatLng)
        newBuilder.origin = Waypoint.newBuilder()
            .setLocation(location)
            .build()
        newBuilder.destination = Waypoint.newBuilder()
            .setLocation(com.google.maps.routing.v2.Location.newBuilder().setLatLng(LatLng.newBuilder().setLatitude(destination.latitude).setLongitude(destination.longitude)))
            .build()

        val response = client.computeRoutes(newBuilder.build())

        response.routesList.forEach { route ->
            Timber.d(route.description)
        }
    }

    companion object {

        private val getPlaceRequestPlaceFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.LAT_LNG,
            Place.Field.ADDRESS,
            Place.Field.PHOTO_METADATAS,
            Place.Field.EDITORIAL_SUMMARY,
        )
    }
}
