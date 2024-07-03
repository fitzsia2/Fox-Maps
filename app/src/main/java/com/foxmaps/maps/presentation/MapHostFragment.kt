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
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class MapHostFragment : Fragment() {

    private var binding: MapHostFragmentBinding? = null

    private var systemBarInsets: Insets = Insets.NONE

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

    private val ViewBinding.peekHeight get() = (root.height * 0.4).toInt()

    private val bottomSheetBehavior get() = binding?.mapBottomSheet?.let { BottomSheetBehavior.from(it) }

    private val bottomSheetCallback = object : BottomSheetCallback() {

        override fun onStateChanged(bottomSheet: View, newState: Int) = Unit

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            binding?.updateMapPadding(slideOffset)
            binding?.updateBottomSheetPadding(slideOffset)

            val isHidden = slideOffset == -1f
            if (isHidden) {
                viewModel.clearPointOfInterest()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val locationPermission = inflater.context.getLocationPermission()
        viewModel.setLocationPermission(locationPermission)
        viewModel.setMapLoading(true)

        val binding = MapHostFragmentBinding.inflate(inflater)
        this.binding = binding

        binding.initBottomSheet()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.applyWindowInsets(insets)
            insets
        }

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

        val bottomSheetSlideOffset = bottomSheetBehavior?.calculateSlideOffset() ?: 0f
        binding?.updateMapPadding(bottomSheetSlideOffset)
        binding?.updateBottomSheetPadding(bottomSheetSlideOffset)
    }

    private fun MapHostFragmentBinding.bind(screenState: ScreenState) {
        progressBar.isVisible = screenState.showSpinner
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
        imgSelectedPlace.isVisible = mapBottomSheetState.showImage
        bindBottomSheetBehavior(mapBottomSheetState)
        when (mapBottomSheetState) {
            is MapBottomSheetState.Loaded -> {
                txtSelectedPlaceName.text = mapBottomSheetState.mapPlace.name
                mapBottomSheetState.mapPlace.photos.firstOrNull()?.let { photo ->
                    imgSelectedPlace.setImageBitmap(photo.bitmap)
                }
            }
            is MapBottomSheetState.Error -> {
                txtSelectedPlaceName.text = getString(R.string.unknown_error)
            }
            is MapBottomSheetState.Loading -> {
                txtSelectedPlaceName.text = mapBottomSheetState.name
            }
            is MapBottomSheetState.Closed -> Unit
        }
    }

    private fun bindBottomSheetBehavior(mapBottomSheetState: MapBottomSheetState) {
        if (bottomSheetBehavior?.state == BottomSheetBehavior.STATE_HIDDEN && mapBottomSheetState.isVisible) {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        if (bottomSheetBehavior?.state != BottomSheetBehavior.STATE_HIDDEN && mapBottomSheetState.isHidden) {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.let { binding ->
            binding.initMap()
            binding.withMapAsync { map ->
                map.setOnMapLoadedCallback {
                    viewModel.setMapLoading(false)
                }
            }

            viewModel.screenStateStream
                .flowWithLifecycle(lifecycle)
                .onEach { screenState -> binding.bind(screenState) }
                .launchIn(viewLifecycleOwner.lifecycleScope)
        }
    }

    private fun MapHostFragmentBinding.initBottomSheet() {
        root.doOnLayout { bottomSheetBehavior?.peekHeight = peekHeight }
        bottomSheetBehavior?.isFitToContents = false
        bottomSheetBehavior?.isHideable = true
        bottomSheetBehavior?.addBottomSheetCallback(bottomSheetCallback)
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun MapHostFragmentBinding.updateMapPadding(slideOffset: Float) {
        val isCollapsedOrSmaller = slideOffset <= 0
        if (isCollapsedOrSmaller) {
            val bottomSheetRootY = mapBottomSheet.height - mapBottomSheet.y
            val bottomPadding = maxOf(systemBarInsets.bottom.toFloat(), bottomSheetRootY)
            withMapAsync { map ->
                map.setPadding(systemBarInsets.left, systemBarInsets.top, systemBarInsets.right, bottomPadding.toInt())
            }
        }
    }

    private fun MapHostFragmentBinding.updateBottomSheetPadding(slideOffset: Float) {
        val compensation = maxOf(slideOffset - 0.9, 0.0) * 10
        val topPadding = compensation * systemBarInsets.top
        mapBottomSheet.setPadding(systemBarInsets.left, topPadding.toInt(), systemBarInsets.right, systemBarInsets.bottom)
    }

    private fun MapHostFragmentBinding.initMap() {
        withMapAsync { map ->
            map.moveCamera(CameraUpdateFactory.zoomTo(DEFAULT_CAMERA_ZOOM))
            map.setOnPoiClickListener { poi ->
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

        private val LOCATION_UPDATE_INTERVAL = 3.seconds
        private const val DEFAULT_CAMERA_ZOOM = 15f

        fun newInstance(): MapHostFragment {
            return MapHostFragment()
        }
    }
}
