package com.foxmaps.maps.domain

sealed interface LocationPermission {

    data object Denied : LocationPermission

    data object Granted : LocationPermission
}
