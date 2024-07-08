package com.foxmaps.maps.presentation

import com.foxmaps.maps.domain.Location
import com.foxmaps.maps.domain.LocationPermission

sealed interface LocationState {

    data object PermissionDenied : LocationState

    data object Loading : LocationState

    data class WithLocation(val location: Location, val updateCamera: Boolean, val animateCamera: Boolean) : LocationState

    companion object {

        fun create(permission: LocationPermission, location: Location?, updateCamera: Boolean, animateCamera: Boolean): LocationState {
            return when (permission) {
                LocationPermission.Denied -> PermissionDenied
                LocationPermission.Granted -> {
                    if (location != null) {
                        WithLocation(location, updateCamera, animateCamera)
                    } else {
                        Loading
                    }
                }
            }
        }
    }
}
