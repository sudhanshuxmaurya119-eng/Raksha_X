package com.rakshax.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.rakshax.app.data.ble.ScannedBleDevice
import com.rakshax.app.data.model.ConnectionStatus
import com.rakshax.app.data.repository.MockDataRepository
import com.rakshax.app.ui.components.ConnectionBadge
import com.rakshax.app.ui.theme.*

@Composable
fun DeviceScreen(
    scanResults: List<ScannedBleDevice> = emptyList(),
    errorMessage: String? = null,
    onScan: () -> Unit = {},
    onConnect: (String) -> Unit = {},
    onDisconnect: () -> Unit = {}
) {
    val deviceState by MockDataRepository.deviceState.collectAsState()
    val isConnected = deviceState.status == ConnectionStatus.CONNECTED
    var showScanDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            onScan()
            showScanDialog = true
        }
    }

    fun startScanWithPermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            emptyList()
        }
        if (permissions.any {
                ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        ) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            onScan()
            showScanDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Device Graphic Card
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(
                    if (isConnected) SafeGreen.copy(alpha = 0.12f)
                    else EmergencyRed.copy(alpha = 0.12f)
                )
                .border(
                    2.dp,
                    if (isConnected) SafeGreen.copy(alpha = 0.4f)
                    else EmergencyRed.copy(alpha = 0.4f),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = "ESP32 Device",
                tint = if (isConnected) SafeGreen else EmergencyRed,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = deviceState.name,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        ConnectionBadge(status = deviceState.status)

        Spacer(modifier = Modifier.height(24.dp))

        // Device Specification & Telemetry Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "HARDWARE TELEMETRY",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                TelemetryRow(
                    label = "BLE MAC Address",
                    value = if (isConnected) deviceState.macAddress else "Unavailable",
                    icon = Icons.Default.SettingsBluetooth
                )

                Divider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 10.dp))

                TelemetryRow(
                    label = "Battery Charge",
                    value = if (isConnected) "${deviceState.batteryLevel}% (Li-Po 3.7V)" else "Disconnected",
                    icon = Icons.Default.BatteryChargingFull,
                    valueColor = if (isConnected) SafeGreen else TextMuted
                )

                Divider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 10.dp))

                TelemetryRow(
                    label = "Signal Strength (RSSI)",
                    value = if (isConnected) "${deviceState.signalStrengthRssi} dBm (Strong)" else "0 dBm",
                    icon = Icons.Default.SignalCellularAlt
                )

                Divider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 10.dp))

                TelemetryRow(
                    label = "Firmware Build",
                    value = deviceState.firmwareVersion,
                    icon = Icons.Default.Code
                )

                Divider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 10.dp))

                TelemetryRow(
                    label = "Trigger Config",
                    value = deviceState.buttonGpio,
                    icon = Icons.Default.RadioButtonChecked,
                    valueColor = EmergencyRed
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions: Connect / Disconnect
        Button(
            onClick = { if (isConnected) onDisconnect() else startScanWithPermission() },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isConnected) EmergencyRedDark else SafeGreen,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.BluetoothDisabled else Icons.Default.BluetoothConnected,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isConnected) "DISCONNECT DEVICE" else "FIND RAKSHAX ESP32",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedButton(
            onClick = { startScanWithPermission() },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("SCAN FOR NEARBY DEVICES")
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = errorMessage,
                color = WarningAmber,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hardware Guide Note
        Surface(
            color = SurfaceElevated,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = InfoCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Hardware Button: Push button wired to GPIO4 -> GND. Hold for 2 seconds to broadcast emergency notification. Background listener active via Foreground Service.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }

    if (showScanDialog) {
        AlertDialog(
            onDismissRequest = { showScanDialog = false },
            title = { Text("Nearby RakshaX buttons") },
            text = {
                if (scanResults.isEmpty()) {
                    Text("Scanning for ESP32-C3 devices…", color = TextSecondary)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(scanResults, key = { it.address }) { device ->
                            TextButton(
                                onClick = {
                                    showScanDialog = false
                                    onConnect(device.address)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(device.name, color = TextPrimary)
                                    Text(
                                        text = "${device.address}  •  ${device.rssi} dBm",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showScanDialog = false }) {
                    Text("CLOSE")
                }
            }
        )
    }
}

@Composable
private fun TelemetryRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    valueColor: Color = Color.Unspecified
) {
    val displayValueColor = if (valueColor != Color.Unspecified) valueColor else TextPrimary
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
        Text(
            text = value,
            color = displayValueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
