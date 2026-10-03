package com.rakshax.app.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.rakshax.app.BuildConfig
import com.rakshax.app.data.location.LocationRepository
import com.rakshax.app.data.remote.AuroraSafeRepository
import com.rakshax.app.data.remote.RemoteSafeRoute
import com.rakshax.app.data.remote.RoutePoint
import com.rakshax.app.data.repository.MockDataRepository
import com.rakshax.app.ui.components.IncidentCard
import com.rakshax.app.ui.components.RiskZoneBadge
import com.rakshax.app.ui.theme.*

@Composable
fun SafetyMapScreen(
    locationRepository: LocationRepository,
    auroraSafeRepository: AuroraSafeRepository
) {
    val incidents by MockDataRepository.incidents.collectAsState()
    val hotspots by MockDataRepository.hotspots.collectAsState()
    val riskScore by MockDataRepository.riskScore.collectAsState()
    val locationState by locationRepository.state.collectAsState()

    var showHeatmapLayer by remember { mutableStateOf(true) }
    var showSafeRoute by remember { mutableStateOf(false) }
    var safeRoute by remember { mutableStateOf<RemoteSafeRoute?>(null) }
    var syncMessage by remember { mutableStateOf("Using cached safety intelligence") }

    LaunchedEffect(locationState.latitude, locationState.longitude, showSafeRoute) {
        val latitude = locationState.latitude ?: return@LaunchedEffect
        val longitude = locationState.longitude ?: return@LaunchedEffect
        auroraSafeRepository.fetchRiskScore(latitude, longitude).value?.let {
            MockDataRepository.setRiskScore(it)
            syncMessage = "Live AuroraSafe intelligence synced"
        }
        auroraSafeRepository.fetchIncidents(latitude, longitude, 5.0).value?.let {
            MockDataRepository.setRemoteIncidents(it)
        }
        auroraSafeRepository.fetchHotspots().value?.let {
            MockDataRepository.setRemoteHotspots(it)
        }
        if (showSafeRoute) {
            safeRoute = auroraSafeRepository.fetchSafeRoute(
                origin = RoutePoint(latitude, longitude),
                destination = RoutePoint(28.5494, 77.2001)
            ).value
        } else {
            safeRoute = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Map Canvas Placeholder & Visualizer (Google Maps SDK Architecture)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(SurfaceDark)
                .border(width = 1.dp, color = SurfaceBorder)
        ) {
            if (BuildConfig.GOOGLE_MAPS_API_KEY.isNotBlank()) {
                RealGoogleMapSurface(
                    modifier = Modifier.fillMaxSize(),
                    incidents = incidents,
                    hotspots = hotspots,
                    location = locationState.latitude?.let { LatLng(it, locationState.longitude ?: 0.0) },
                    safeRoute = safeRoute,
                    showHeatmapLayer = showHeatmapLayer
                )
            }

            // Simulated Map Grid & Geo Visualization
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Map Layer Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RiskZoneBadge(zone = riskScore.zone)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = showHeatmapLayer,
                            onClick = { showHeatmapLayer = !showHeatmapLayer },
                            label = { Text("Heatmap", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmergencyRedDark,
                                selectedLabelColor = Color.White
                            )
                        )

                        FilterChip(
                            selected = showSafeRoute,
                            onClick = { showSafeRoute = !showSafeRoute },
                            label = { Text("Safe Route", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SafeGreenDark,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Center Map Pins Visualization
                if (BuildConfig.GOOGLE_MAPS_API_KEY.isBlank()) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Current User Marker
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AccentBlue.copy(alpha = 0.25f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(AccentBlue)
                                )
                            }
                            Text(
                                text = locationState.latitude?.let {
                                    "You: ${"%.5f".format(it)}, ${"%.5f".format(locationState.longitude ?: 0.0)}"
                                } ?: "You: location unavailable",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Bottom Route Details Overlay (if Safe Route toggled)
                if (showSafeRoute) {
                    Surface(
                        color = SurfaceElevated.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = safeRoute?.let { "${it.routeType} route to Hauz Khas" }
                                        ?: "Calculating safest route...",
                                    color = SafeGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = safeRoute?.let {
                                        "${it.distanceKm} km • ${it.estimatedTimeMinutes} min • ${it.safetyScore * 100}% safety • avoided ${it.avoidedZones} zones"
                                    } ?: "AuroraSafe route service is working",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            Icon(Icons.Default.Navigation, contentDescription = null, tint = SafeGreen)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = syncMessage,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${hotspots.size} live hotspots",
                            color = InfoCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Bottom List: Nearby Incident Feed & Clusters
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NEARBY SAFETY ALERTS (1 KM)",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${incidents.size} Active Reports • ${hotspots.size} hotspots",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(incidents) { inc ->
                    IncidentCard(incident = inc)
                }
            }
        }
    }
}

@Composable
private fun RealGoogleMapSurface(
    modifier: Modifier,
    incidents: List<com.rakshax.app.data.model.IncidentMarker>,
    hotspots: List<com.rakshax.app.data.model.HotspotZone>,
    location: LatLng?,
    safeRoute: RemoteSafeRoute?,
    showHeatmapLayer: Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context).also { it.onCreate(null) } }
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }

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
        mapView.getMapAsync { googleMap = it }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (!destroyed) mapView.onDestroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView }
    )

    LaunchedEffect(googleMap, incidents, hotspots, location, safeRoute, showHeatmapLayer) {
        val map = googleMap ?: return@LaunchedEffect
        map.clear()
        map.uiSettings.isZoomControlsEnabled = false
        map.uiSettings.isCompassEnabled = true

        location?.let {
            map.addMarker(MarkerOptions().position(it).title("Your location"))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 13.5f))
        }

        incidents.forEach { incident ->
            map.addMarker(
                MarkerOptions()
                    .position(LatLng(incident.latitude, incident.longitude))
                    .title(incident.title)
                    .snippet("Severity ${incident.severity}")
            )?.setAlpha(if (incident.zone == "RED") 1f else 0.8f)
        }

        if (showHeatmapLayer) {
            hotspots.forEach { hotspot ->
                val isRed = hotspot.riskLevel == "RED"
                val color = if (isRed) AndroidColor.RED else AndroidColor.YELLOW
                if (hotspot.latitude != 0.0 || hotspot.longitude != 0.0) {
                    map.addCircle(
                        CircleOptions()
                            .center(LatLng(hotspot.latitude, hotspot.longitude))
                            .radius(hotspot.radiusKm * 1000.0)
                            .strokeColor(color)
                            .strokeWidth(2f)
                            .fillColor(AndroidColor.argb(45, AndroidColor.red(color), AndroidColor.green(color), AndroidColor.blue(color)))
                    )
                }
            }
        }

        val routePoints = safeRoute?.waypoints.orEmpty()
        if (routePoints.size > 1) {
            map.addPolyline(
                PolylineOptions()
                    .addAll(routePoints.map { LatLng(it.latitude, it.longitude) })
                    .color(AndroidColor.rgb(40, 190, 120))
                    .width(10f)
            )
        }
    }
}
