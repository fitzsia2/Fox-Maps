package com.foxmaps.maps.presentation

import com.foxmaps.maps.domain.MapPlace

sealed interface ScreenState {

    val mapLoading: Boolean

    data object Loading : ScreenState {

        override val mapLoading = true
    }

    data class Loaded(
        val locationState: LocationState,
        override val mapLoading: Boolean,
        val mapBottomSheetState: MapBottomSheetState,
    ) : ScreenState

    companion object {

        fun create(
            mapLoading: Boolean,
            locationState: LocationState,
            mapBottomSheetState: MapBottomSheetState,
        ): ScreenState {
            return Loaded(locationState, mapLoading, mapBottomSheetState)
        }
    }
}

sealed interface MapBottomSheetState {

    val showProgressBar get() = false

    val showImage get() = false

    data object Closed : MapBottomSheetState

    data object Loading : MapBottomSheetState {

        override val showProgressBar = true
    }

    data class Error(val exception: Exception) : MapBottomSheetState

    data class Loaded(val mapPlace: MapPlace) : MapBottomSheetState {

        override val showImage: Boolean = mapPlace.photos.firstOrNull() != null
    }
}
