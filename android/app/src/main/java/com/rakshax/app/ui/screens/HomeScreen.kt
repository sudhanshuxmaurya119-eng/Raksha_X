package com.rakshax.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.rakshax.app.data.location.LocationRepository
import com.rakshax.app.data.location.LocationStatus
import com.rakshax.app.data.location.SosLocationPayload
import com.rakshax.app.data.model.ConnectionStatus
import com.rakshax.app.data.network.NetworkStatusMonitor
import com.rakshax.app.data.repository.MockDataRepository
import com.rakshax.app.ui.components.*
import com.rakshax.app.ui.theme.*

@Composable
fun HomeScreen(
    onNavigateToMap: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToDevice: () -> Unit,
    onNavigateToSosLive: () -> Unit,
    locationRepository: LocationRepository,
    networkStatusMonitor: NetworkStatusMonitor,
    onTriggerSos: (SosLocationPayload?) -> Unit
) {
    val deviceState by MockDataRepository.deviceState.collectAsState()
    val sosState by MockDataRepository.sosState.collectAsState()
    val riskInfo by MockDataRepository.riskScore.collectAsState()
    val contacts by MockDataRepository.contacts.collectAsState()
    val locationState by locationRepository.state.collectAsState()
    val networkOnline by networkStatusMonitor.isOnline.collectAsState()
    val scope = rememberCoroutineScope()
    var pendingSos by remember { mutableStateOf(false) }
    var showSosConfirmation by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            locationRepository.startUpdates()
            if (pendingSos) {
                pendingSos = false
                scope.launch { onTriggerSos(locationRepository.getCurrentLocation()) }
            }
        } else {
            pendingSos = false
            locationRepository.refreshStatus()
        }
    }

    LaunchedEffect(Unit) {
        if (locationRepository.hasPermission()) locationRepository.startUpdates()
        else locationRepository.refreshStatus()
    }

    val activeContactsCount = contacts.count { it.isEnabled }

    fun requestSos() {
        if (locationRepository.hasPermission()) {
            scope.launch { onTriggerSos(locationRepository.getCurrentLocation()) }
        } else {
            pendingSos = true
            locationPermissionLauncher.launch(locationPermissions())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!networkOnline) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = WarningAmber.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = WarningAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Offline mode: SOS events will queue and retry automatically",
                        color = WarningAmber,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 1. Hardware Device Status Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceDark)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
                .clickable { onNavigateToDevice() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (deviceState.status == ConnectionStatus.CONNECTED) SafeGreen.copy(alpha = 0.15f)
                            else EmergencyRed.copy(alpha = 0.15f)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = if (deviceState.status == ConnectionStatus.CONNECTED) SafeGreen else EmergencyRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = deviceState.name,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (deviceState.status == ConnectionStatus.CONNECTED) "Connected (Battery ${deviceState.batteryLevel}%)"
                        else "Disconnected — Tap to pair",
                        color = if (deviceState.status == ConnectionStatus.CONNECTED) SafeGreen else TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Safety Status & Location Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = EmergencyRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = locationState.latitude?.let {
                                "${locationState.latitude}, ${locationState.longitude}"
                            } ?: "Location not available",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    RiskZoneBadge(zone = riskInfo.zone)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "SAFETY INDEX",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${(riskInfo.score * 100).toInt()}% Risk Factor",
                            color = WarningAmber,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = "NEARBY ALERTS",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${riskInfo.nearbyIncidentsCount} Incidents in 1km",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = "HOTSPOT",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${riskInfo.nearestHotspotDistanceKm} km away",
                            color = EmergencyRed,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = locationState.message,
                        color = when (locationState.status) {
                            LocationStatus.READY -> SafeGreen
                            LocationStatus.POOR_ACCURACY -> WarningAmber
                            else -> TextSecondary
                        },
                        fontSize = 11.sp
                    )
                    if (locationState.status == LocationStatus.PERMISSION_REQUIRED) {
                        TextButton(
                            onClick = {
                                locationPermissionLauncher.launch(locationPermissions())
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("ENABLE", color = AccentBlue, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // 3. Central Prominent Emergency SOS Button
        Text(
            text = "CONFIRM BEFORE SENDING EMERGENCY ALERT",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        EmergencySosButton(
            isPulsing = true,
            onClick = {
                showSosConfirmation = true
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Emergency Call 112 Action
        Call112Button()

        Spacer(modifier = Modifier.height(20.dp))

        // 5. Quick Access Grid: Safety Map & Trusted Contacts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Safety Map Card
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onNavigateToMap() },
                color = SurfaceDark,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentBlue.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Safety Map",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Heatmap & Zones",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Trusted Contacts Card
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onNavigateToContacts() },
                color = SurfaceDark,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SafeGreen.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = SafeGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Trusted Circle",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$activeContactsCount Contacts Active",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 6. Recent SOS Pipeline Snapshot (if triggered or historical)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onNavigateToSosLive() },
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (sosState.isActive) EmergencyRed.copy(alpha = 0.15f)
                                else SafeGreen.copy(alpha = 0.15f)
                            )
                    ) {
                        Icon(
                            imageVector = if (sosState.isActive) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (sosState.isActive) EmergencyRed else SafeGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (sosState.isActive) "SOS Active — Live Tracking" else "Last SOS Acknowledged",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (sosState.isActive) "Awaiting circle acknowledgement..."
                            else "Rahul Sharma acknowledged • System normal",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (showSosConfirmation) {
            AlertDialog(
                onDismissRequest = { showSosConfirmation = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = EmergencyRed) },
                title = { Text("Send SOS alert?") },
                text = {
                    Text(
                        "AuroraSafe will notify your trusted circle and share your latest available location."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSosConfirmation = false
                            requestSos()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                    ) {
                        Text("SEND SOS")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSosConfirmation = false }) {
                        Text("CANCEL")
                    }
                }
            )
        }
    }
}

private fun locationPermissions(): Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
} else {
    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
}
