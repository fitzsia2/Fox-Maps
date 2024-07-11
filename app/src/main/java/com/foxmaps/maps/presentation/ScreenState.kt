package com.foxmaps.maps.presentation

import com.foxmaps.maps.domain.MapPlace

sealed interface ScreenState {

    val showFullScreenLoader get() = this is Loading

    val mapIsVisible get() = false

    data object Loading : ScreenState

    data class Loaded(
        val locationState: LocationState,
        val mapBottomSheetState: MapBottomSheetState,
    ) : ScreenState {

        val loading = mapBottomSheetState is MapBottomSheetState.Loading

        val showPermissionWarning = locationState is LocationState.PermissionDenied

        override val mapIsVisible: Boolean = locationState is LocationState.WithLocation
    }

    companion object {

        fun create(
            mapLoading: Boolean,
            locationState: LocationState,
            mapBottomSheetState: MapBottomSheetState,
        ): ScreenState {
            return if (mapLoading) {
                return Loading
            } else {
                Loaded(locationState, mapBottomSheetState)
            }
        }
    }
}

sealed interface MapBottomSheetState {

    val showImage get() = false

    val isHidden get() = this is Closed

    val isVisible get() = !this.isHidden

    data object Closed : MapBottomSheetState

    data class Loading(val name: String) : MapBottomSheetState {

        override val showImage = true
    }

    data class Error(val exception: Exception) : MapBottomSheetState

    data class Loaded(val mapPlace: MapPlace) : MapBottomSheetState {

        override val showImage: Boolean = mapPlace.photos.firstOrNull() != null

        val descriptionIsVisible = !mapPlace.description.isNullOrBlank()
    }
}
