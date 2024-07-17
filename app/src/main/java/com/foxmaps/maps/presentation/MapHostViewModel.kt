package com.foxmaps.maps.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxmaps.maps.domain.Location
import com.foxmaps.maps.domain.LocationPermission
import com.foxmaps.maps.domain.MapsRepository
import com.google.android.gms.maps.model.PointOfInterest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class MapHostViewModel @Inject constructor(
    private val mapsRepository: MapsRepository,
) : ViewModel() {

    private val locationPermissionStream = MutableStateFlow<LocationPermission?>(null)

    private val locationStream = MutableStateFlow<Location?>(null)

    private val followUserStream = MutableStateFlow(true)

    private val mapLoadingStream = MutableStateFlow(true)

    private val animateCameraStream = MutableStateFlow(false)

    private val poiStream = MutableStateFlow<PointOfInterest?>(null)

    private val mapBottomSheetStateStream =
        MutableStateFlow<MapBottomSheetState>(MapBottomSheetState.Closed)

    private val locationStateStream = combine(
        locationPermissionStream.filterNotNull(),
        locationStream,
        followUserStream,
        animateCameraStream,
    ) { permission, location, followUser, animateCamera ->
        LocationState.create(permission, location, updateCamera = followUser, animateCamera)
    }

    init {
        viewModelScope.launch {
            poiStream.distinctUntilChanged { old, new -> old?.placeId == new?.placeId }
                .collectLatest { poi ->
                    if (poi == null) {
                        mapBottomSheetStateStream.value = MapBottomSheetState.Closed
                    } else {
                        launch {
                            try {
                                mapBottomSheetStateStream.value =
                                    MapBottomSheetState.Loading(poi.name)
                                val place = mapsRepository.getPlace(poi.placeId)
                                mapBottomSheetStateStream.value = MapBottomSheetState.Loaded(place)
                            } catch (e: Exception) {
                                mapBottomSheetStateStream.value = MapBottomSheetState.Error(e)
                            }
                        }
                    }
                }
        }
    }

    val screenStateStream = combine(
        mapLoadingStream,
        locationStateStream,
        mapBottomSheetStateStream,
    ) { mapLoading, locationState, mapBottomSheetState ->
        ScreenState.create(mapLoading, locationState, mapBottomSheetState)
    }
        .debounce(screenStateDebounce)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ScreenState.Loading)

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

    fun selectPointOfInterest(pointOfInterest: PointOfInterest) {
        poiStream.value = pointOfInterest
    }

    fun clearPointOfInterest() {
        poiStream.value = null
    }

    fun enableCameraAnimations() {
        animateCameraStream.value = true
    }

    companion object {

        private val screenStateDebounce = 80.milliseconds
    }
}
