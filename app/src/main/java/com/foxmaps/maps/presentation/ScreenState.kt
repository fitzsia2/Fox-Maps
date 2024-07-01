package com.foxmaps.maps.presentation

sealed interface ScreenState {

    val mapLoading: Boolean

    data object Loading : ScreenState {

        override val mapLoading = true
    }

    data class Loaded(val locationState: LocationState, override val mapLoading: Boolean) : ScreenState
}
