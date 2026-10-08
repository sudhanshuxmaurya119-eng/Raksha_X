package com.rakshax.app.ui.screens

import android.Manifest
import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*
import com.rakshax.app.data.location.LocationRepository
import com.rakshax.app.data.model.*
import com.rakshax.app.data.remote.AuroraSafeRepository
import com.rakshax.app.ui.screens.map.*
import com.rakshax.app.ui.theme.*

@Composable
fun SafetyMapScreen(
    locationRepository: LocationRepository,
    auroraSafeRepository: AuroraSafeRepository
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val themeColors = LocalRakshaXColors.current

    // ViewModel
    val viewModel = remember { SafetyMapViewModel(auroraSafeRepository) }

    // Collect ViewModel state
    val displayedFacilities by viewModel.displayedFacilities.collectAsState()
    val displayedAreas by viewModel.displayedAreas.collectAsState()
    val displayedCases by viewModel.displayedCases.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFacility by viewModel.selectedFacility.collectAsState()
    val selectedArea by viewModel.selectedArea.collectAsState()
    val isNearbySheetVisible by viewModel.isNearbySheetVisible.collectAsState()
    val isFilterSheetVisible by viewModel.isFilterSheetVisible.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    // Location state
    val locationState by locationRepository.state.collectAsState()
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    // Map state
    val mapView = remember { MapView(context).also { it.onCreate(null) } }
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var hasInitialCameraMoved by remember { mutableStateOf(false) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Request permission on launch
    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Update user location from LocationRepository
    LaunchedEffect(locationState.latitude, locationState.longitude) {
        val lat = locationState.latitude ?: return@LaunchedEffect
        val lng = locationState.longitude ?: return@LaunchedEffect
        userLocation = LatLng(lat, lng)
        viewModel.loadData(lat, lng)
    }

    // MapView lifecycle management
    DisposableEffect(mapView, lifecycleOwner) {
        var destroyed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> {
                    mapView.onDestroy()
                    destroyed = true
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.getMapAsync { map ->
            googleMap = map
            // Apply theme-based styling
            val style = if (themeColors.isDark) MapStyleUtils.DARK_MAP_STYLE else MapStyleUtils.LIGHT_MAP_STYLE
            map.setMapStyle(style)

            map.uiSettings.isZoomControlsEnabled = false
            map.uiSettings.isCompassEnabled = true
            map.uiSettings.isMapToolbarEnabled = false
            map.uiSettings.isMyLocationButtonEnabled = false

            // Default camera position (Delhi NCR)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(28.6139, 77.2090), 11.5f))
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (!destroyed) mapView.onDestroy()
        }
    }

    // Draw map overlays when data changes
    LaunchedEffect(
        googleMap, displayedFacilities, displayedAreas, displayedCases,
        userLocation, filterState.mapTypeIndex
    ) {
        val map = googleMap ?: return@LaunchedEffect
        map.clear()

        // Map type
        map.mapType = when (filterState.mapTypeIndex) {
            1 -> GoogleMap.MAP_TYPE_SATELLITE
            2 -> GoogleMap.MAP_TYPE_TERRAIN
            else -> GoogleMap.MAP_TYPE_NORMAL
        }

        // Draw risk area circles
        displayedAreas.forEach { area ->
            val (fillColor, strokeColor) = when (area.riskLevel) {
                RiskLevel.LOW -> Pair(
                    AndroidColor.argb(45, 16, 185, 129),
                    AndroidColor.argb(180, 16, 185, 129)
                )
                RiskLevel.MEDIUM -> Pair(
                    AndroidColor.argb(45, 245, 158, 11),
                    AndroidColor.argb(180, 245, 158, 11)
                )
                RiskLevel.HIGH -> Pair(
                    AndroidColor.argb(45, 225, 29, 72),
                    AndroidColor.argb(180, 225, 29, 72)
                )
            }
            map.addCircle(
                CircleOptions()
                    .center(LatLng(area.centerLat, area.centerLng))
                    .radius(area.radiusMeters)
                    .fillColor(fillColor)
                    .strokeColor(strokeColor)
                    .strokeWidth(2.5f)
                    .clickable(true)
            )
        }

        // Draw facility markers
        displayedFacilities.forEach { facility ->
            val marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(facility.latitude, facility.longitude))
                    .title(facility.name)
                    .snippet(facility.type.label)
                    .icon(MapMarkerUtils.getFacilityMarker(context, facility.type))
            )
            marker?.tag = facility
        }

        // Draw case markers
        displayedCases.forEach { case ->
            map.addMarker(
                MarkerOptions()
                    .position(LatLng(case.latitude, case.longitude))
                    .title("Case: ${case.category}")
                    .snippet("${case.severity} severity • ${case.area}")
                    .icon(MapMarkerUtils.getCaseMarker(context, case.severity))
                    .alpha(0.85f)
            )
        }

        // Draw user location
        userLocation?.let { loc ->
            map.addMarker(
                MarkerOptions()
                    .position(loc)
                    .title("Your Location")
                    .icon(MapMarkerUtils.getUserLocationMarker(context))
                    .zIndex(10f)
                    .anchor(0.5f, 0.5f)
            )

            // Move camera on first load
            if (!hasInitialCameraMoved) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 13f))
                hasInitialCameraMoved = true
            }
        }

        // Marker click listener for facilities
        map.setOnMarkerClickListener { marker ->
            val facility = marker.tag as? SafetyFacility
            if (facility != null) {
                viewModel.selectFacility(facility)
                map.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
                true
            } else {
                false
            }
        }

        // Circle click listener for risk areas
        map.setOnCircleClickListener { circle ->
            val area = displayedAreas.minByOrNull { a ->
                val dist = FloatArray(1)
                android.location.Location.distanceBetween(
                    a.centerLat, a.centerLng,
                    circle.center.latitude, circle.center.longitude,
                    dist
                )
                dist[0]
            }
            area?.let { viewModel.selectArea(it) }
        }
    }

    // ========== UI LAYOUT ==========
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Full-screen Google Map
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView }
        )

        // Loading indicator
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = EmergencyRed,
                trackColor = SurfaceDark.copy(alpha = 0.5f)
            )
        }

        // Offline banner
        AnimatedVisibility(
            visible = isOffline,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = WarningAmber.copy(alpha = 0.9f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "Offline • Showing cached data",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ===== TOP: Search Bar =====
        MapSearchBar(
            query = searchQuery,
            onQueryChange = { viewModel.updateSearchQuery(it) },
            onFilterClick = { viewModel.toggleFilterSheet(true) },
            onLayersClick = { viewModel.toggleFilterSheet(true) },  // Layers also opens filter sheet
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp)
        )

        // ===== BOTTOM-LEFT: Safety Legend =====
        SafetyLegendPill(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 80.dp)
        )

        // ===== BOTTOM-RIGHT: FABs Column =====
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            // My Location FAB
            SmallFloatingActionButton(
                onClick = {
                    userLocation?.let { loc ->
                        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 14f))
                    } ?: run {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                containerColor = SurfaceDark,
                contentColor = AccentBlue,
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = "My Location",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Nearby FAB
            SmallFloatingActionButton(
                onClick = { viewModel.toggleNearbySheet(!isNearbySheetVisible) },
                containerColor = SurfaceDark,
                contentColor = TextPrimary,
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.NearMe,
                    contentDescription = "Nearby",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Emergency SOS FAB
            FloatingActionButton(
                onClick = {
                    viewModel.triggerEmergencySos(userLocation) {
                        // SOS triggered callback — could show confirmation
                    }
                },
                containerColor = EmergencyRed,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Emergency SOS",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // ===== BOTTOM: Status Bar — drawn BEFORE sheets so sheets appear on top =====
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            color = SurfaceDark.copy(alpha = 0.92f),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isOffline) WarningAmber else SafeGreen)
                    )
                    Text(
                        text = statusMessage ?: "Safety intelligence loading...",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                Text(
                    text = "${displayedAreas.size} zones • ${displayedFacilities.size} services",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ===== OVERLAYS: Selected Facility Card — above status bar =====
        AnimatedVisibility(
            visible = selectedFacility != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 56.dp)
        ) {
            selectedFacility?.let { fac ->
                FacilityDetailCard(
                    facility = fac,
                    onClose = { viewModel.selectFacility(null) },
                    onGetDirections = { lat, lng ->
                        userLocation?.let { loc ->
                            viewModel.calculateDirections(
                                loc.latitude, loc.longitude, lat, lng
                            )
                        }
                    }
                )
            }
        }

        // ===== OVERLAYS: Area Safety Report — above status bar =====
        AnimatedVisibility(
            visible = selectedArea != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 56.dp)
        ) {
            selectedArea?.let { area ->
                AreaSafetyReportSheet(
                    area = area,
                    onClose = { viewModel.selectArea(null) }
                )
            }
        }

        // ===== OVERLAYS: Nearby Sheet — above status bar =====
        AnimatedVisibility(
            visible = isNearbySheetVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) {
            NearbyFacilitiesSheet(
                facilities = displayedFacilities,
                onFacilityClick = { fac ->
                    viewModel.selectFacility(fac)
                    viewModel.toggleNearbySheet(false)
                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(fac.latitude, fac.longitude), 15f
                        )
                    )
                },
                onClose = { viewModel.toggleNearbySheet(false) }
            )
        }

        // ===== OVERLAYS: Filter Sheet — drawn LAST so it appears on top of everything =====
        AnimatedVisibility(
            visible = isFilterSheetVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) {
            MapFilterBottomSheet(
                filterState = filterState,
                onFilterChanged = { viewModel.updateFilterState(it) },
                onClose = { viewModel.toggleFilterSheet(false) }
            )
        }
    }
}
