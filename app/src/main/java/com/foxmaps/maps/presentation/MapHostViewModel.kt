package com.foxmaps.maps.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxmaps.maps.domain.Location
import com.foxmaps.maps.domain.LocationPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MapHostViewModel : ViewModel() {

    private val locationPermissionStream = MutableStateFlow<LocationPermission?>(null)

    private val locationStream = MutableStateFlow<Location?>(null)

    private val followUserStream = MutableStateFlow(true)

    private val mapLoadingStream = MutableStateFlow(true)

    val screenStateStream = MutableStateFlow<ScreenState>(ScreenState.Loading)

    init {
        combine(
            mapLoadingStream,
            locationPermissionStream.filterNotNull(),
            locationStream,
            followUserStream,
        ) { mapLoading, permission, location, followUser ->
            val locationState = LocationState.create(permission, location, updateCamera = followUser)
            ScreenState.Loaded(locationState, mapLoading)
        }
            .onEach { screenStateStream.value = it }
            .launchIn(viewModelScope)
    }

    fun setMapLoading(value: Boolean) {
        mapLoadingStream.value = value
    }

    fun setLocationPermission(locationPermission: LocationPermission) {
        locationPermissionStream.value = locationPermission
    }

    fun setLocation(location: Location?) {
        locationStream.value = location
    }

    fun setFollowUser(value: Boolean) {
        followUserStream.value = value
    }
}
