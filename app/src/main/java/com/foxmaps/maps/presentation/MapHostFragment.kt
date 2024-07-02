package com.foxmaps.maps.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.foxmaps.R
import com.foxmaps.databinding.MapHostFragmentBinding
import com.foxmaps.maps.domain.Location
import com.foxmaps.maps.domain.LocationPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnCameraMoveStartedListener
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class MapHostFragment : Fragment() {

    private var binding: MapHostFragmentBinding? = null

    private val viewModel: MapHostViewModel by viewModels()

    private var locationProviderClient: FusedLocationProviderClient? = null

    private val contract = ActivityResultContracts.RequestMultiplePermissions()

    private val locationPermissionRequest = registerForActivityResult(contract) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                viewModel.setLocationPermission(LocationPermission.Granted)
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                viewModel.setLocationPermission(LocationPermission.Granted)
            }
        }
    }

    private val locationCallback = object : LocationCallback() {

        override fun onLocationAvailability(locationAvailability: LocationAvailability) {
            super.onLocationAvailability(locationAvailability)
            viewModel.setLocation(null)
        }

        override fun onLocationResult(locationResult: LocationResult) {
            val lastLocation = locationResult.lastLocation
            val location = lastLocation?.let { Location(lastLocation.latitude, lastLocation.longitude) }
            viewModel.setLocation(location)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val locationPermission = inflater.context.getLocationPermission()
        viewModel.setLocationPermission(locationPermission)
        viewModel.setMapLoading(true)

        val binding = MapHostFragmentBinding.inflate(inflater)
        binding.btnClose.setOnClickListener { viewModel.clearPointOfInterest() }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            binding.applyWindowInsets(insets)
            insets
        }

        this.binding = binding
        return binding.root
    }

    private fun Context.getLocationPermission(): LocationPermission {
        val permission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        return if (permission == PackageManager.PERMISSION_GRANTED) {
            LocationPermission.Granted
        } else {
            LocationPermission.Denied
        }
    }

    private fun MapHostFragmentBinding.applyWindowInsets(insets: WindowInsetsCompat) {
        val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        mapOverlay.setPadding(systemBarsInsets)
        mapBottomSheetFrame.setPadding(systemBarsInsets)
        withMapAsync { it.setPadding(systemBarsInsets) }
    }

    private fun MapHostFragmentBinding.bind(screenState: ScreenState) {
        progressBar.isVisible = screenState.mapLoading
        when (screenState) {
            is ScreenState.Loaded -> bindLoadedScreenState(screenState)
            else -> Unit
        }
    }

    private fun MapHostFragmentBinding.bindLoadedScreenState(screenState: ScreenState.Loaded) {
        txtPermissionMessage.isVisible = screenState.locationState is LocationState.PermissionDenied
        when (screenState.locationState) {
            is LocationState.PermissionDenied -> {
                txtPermissionMessage.text = getString(R.string.no_permission)
                requestLocationPermissions()
            }
            is LocationState.Loading -> initLocationTracking()
            is LocationState.WithLocation -> {
                bindLocation(screenState.locationState)
                bindBottomSheet(screenState.mapBottomSheetState)
            }
        }
        mapFragment.isVisible = screenState.locationState != LocationState.Loading
    }

    @SuppressLint("MissingPermission")
    private fun MapHostFragmentBinding.bindLocation(locationState: LocationState.WithLocation) {
        initLocationTracking()
        withMapAsync { map ->
            if (locationState.updateCamera) {
                val location = locationState.location
                val update = CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude))
                map.animateCamera(update)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun MapHostFragmentBinding.initLocationTracking() {
        if (locationProviderClient == null) {
            startLocationTracking()
            withMapAsync { map ->
                map.isMyLocationEnabled = true
                val uiSettings = map.uiSettings
                uiSettings.isCompassEnabled = true
            }
        }
    }

    private fun MapHostFragmentBinding.withMapAsync(block: (GoogleMap) -> Unit) {
        mapFragment.getFragment<SupportMapFragment?>()?.getMapAsync { map -> block(map) }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationTracking() {
        val client = LocationServices.getFusedLocationProviderClient(requireContext())
        val request = LocationRequest.create()
        request.interval = LOCATION_UPDATE_INTERVAL.inWholeMilliseconds
        client.requestLocationUpdates(request, locationCallback, null)
        locationProviderClient = client
    }

    private fun requestLocationPermissions() {
        locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
    }

    private fun MapHostFragmentBinding.bindBottomSheet(mapBottomSheetState: MapBottomSheetState) {
        mapBottomSheetRoot.isVisible = mapBottomSheetState !is MapBottomSheetState.Closed
        bottomSheetProgressBar.isVisible = mapBottomSheetState.showProgressBar
        mapBottomSheetContent.isVisible = mapBottomSheetState is MapBottomSheetState.Loaded
        imgSelectedPlace.isVisible = mapBottomSheetState.showImage
        when (mapBottomSheetState) {
            is MapBottomSheetState.Loaded -> {
                txtSelectedPlaceName.text = mapBottomSheetState.mapPlace.name
                mapBottomSheetState.mapPlace.photos.firstOrNull()?.let { photo ->
                    imgSelectedPlace.setImageBitmap(photo.bitmap)
                }
            }
            is MapBottomSheetState.Error -> {
                txtSelectedPlaceName.text = getString(R.string.unknown_error)
                Timber.e("bindBottomSheet: ${mapBottomSheetState.exception.stackTrace}")
            }
            is MapBottomSheetState.Closed,
            is MapBottomSheetState.Loading -> Unit
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.initMap()

        binding?.withMapAsync { map ->
            map.setOnMapLoadedCallback {
                viewModel.setMapLoading(false)
            }
        }

        viewModel.screenStateStream
            .flowWithLifecycle(lifecycle)
            .onEach { screenState -> binding?.bind(screenState) }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun MapHostFragmentBinding.initMap() {
        withMapAsync { map ->
            map.moveCamera(CameraUpdateFactory.zoomTo(DEFAULT_CAMERA_ZOOM))
            map.setOnPoiClickListener { poi ->
                viewModel.setFollowUser(false)
                viewModel.selectPointOfInterest(poi)
            }
            map.setOnMapClickListener { _ ->
                viewModel.setFollowUser(false)
                viewModel.clearPointOfInterest()
            }
            map.setOnMyLocationButtonClickListener {
                viewModel.setFollowUser(true)
                false
            }
            map.setOnCameraMoveStartedListener { reason ->
                if (reason == OnCameraMoveStartedListener.REASON_GESTURE) {
                    viewModel.setFollowUser(false)
                }
            }
        }
    }

    private fun View.setPadding(insetsCompat: Insets) {
        setPadding(insetsCompat.left, insetsCompat.top, insetsCompat.right, insetsCompat.bottom)
    }

    private fun GoogleMap.setPadding(insetsCompat: Insets) {
        setPadding(insetsCompat.left, insetsCompat.top, insetsCompat.right, insetsCompat.bottom)
    }

    companion object {

        private val LOCATION_UPDATE_INTERVAL = 2.milliseconds
        private const val DEFAULT_CAMERA_ZOOM = 15f

        fun newInstance(): MapHostFragment {
            return MapHostFragment()
        }
    }
}
