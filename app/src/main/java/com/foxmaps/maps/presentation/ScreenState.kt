package com.foxmaps.maps.presentation

import com.foxmaps.maps.domain.MapPlace

sealed interface ScreenState {

    val showSpinner: Boolean

    data object Loading : ScreenState {

        override val showSpinner = true
    }

    data class Loaded(
        val locationState: LocationState,
        val mapLoading: Boolean,
        val mapBottomSheetState: MapBottomSheetState,
    ) : ScreenState {

        override val showSpinner get() = mapLoading || mapBottomSheetState is MapBottomSheetState.Loading
    }

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
