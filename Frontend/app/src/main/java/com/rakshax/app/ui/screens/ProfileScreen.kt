package com.rakshax.app.ui.screens

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.rakshax.app.data.ble.BleServicePreferences
import com.rakshax.app.data.repository.MockDataRepository
import com.rakshax.app.service.BleSosListenerService
import com.rakshax.app.ui.components.Call112Button
import com.rakshax.app.ui.theme.*

@Composable
fun ProfileScreen(
    onNavigateToContacts: () -> Unit,
    onNavigateToDevice: () -> Unit,
    onLogout: () -> Unit
) {
    val user by MockDataRepository.currentUser.collectAsState()
    val context = LocalContext.current
    val locationGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val bluetoothGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || (
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    )
    val bluetoothEnabled = context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true
    val notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    val batteryOptimizationExempt = runCatching {
        context.getSystemService(PowerManager::class.java)
            ?.isIgnoringBatteryOptimizations(context.packageName) == true
    }.getOrDefault(false)

    var callPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        callPermissionGranted = granted
    }

    var smsPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        smsPermissionGranted = granted
    }

    var escalationTimeoutSec by remember { mutableStateOf(45) }
    var audioAlarmOnSos by remember { mutableStateOf(true) }
    var backgroundBleMonitoring by remember {
        mutableStateOf(BleServicePreferences.isEnabled(context))
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            BleServicePreferences.setEnabled(context, true)
            backgroundBleMonitoring = true
            BleSosListenerService.start(context)
        }
    }

    fun setBackgroundMonitoring(enabled: Boolean) {
        if (!enabled) {
            BleServicePreferences.setEnabled(context, false)
            backgroundBleMonitoring = false
            BleSosListenerService.stop(context)
            return
        }
        if (!notificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            BleServicePreferences.setEnabled(context, true)
            backgroundBleMonitoring = true
            BleSosListenerService.start(context)
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
        // User Profile Summary Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(AccentBlue.copy(alpha = 0.2f))
                        .border(1.dp, AccentBlue, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = user.username,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = user.email,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = user.phone,
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Appearance & Theme Mode Selection
        SettingsSectionHeader(title = "APPEARANCE & THEME")

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                val currentTheme by ThemeManager.themeMode.collectAsState()
                val context = LocalContext.current

                Text(
                    text = "Display Theme",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Choose your preferred interface appearance",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceElevated)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val options = listOf(
                        Triple(AppThemeMode.SYSTEM, "System", Icons.Filled.BrightnessAuto),
                        Triple(AppThemeMode.DARK, "Dark", Icons.Filled.DarkMode),
                        Triple(AppThemeMode.LIGHT, "Light", Icons.Filled.LightMode)
                    )

                    options.forEach { (mode, label, icon) ->
                        val isSelected = currentTheme == mode
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) EmergencyRed else Color.Transparent)
                                .clickable { ThemeManager.setThemeMode(context, mode) }
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Emergency Escalation Settings
        SettingsSectionHeader(title = "EMERGENCY & ESCALATION")

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToContacts() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.People, contentDescription = null, tint = AccentBlue)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Manage Trusted Contacts & Priority", color = TextPrimary, fontSize = 13.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                }

                Divider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = WarningAmber)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Escalation Timeout", color = TextPrimary, fontSize = 13.sp)
                            Text("Advance to next priority if unacknowledged", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    Text("${escalationTimeoutSec}s", color = TextPrimary, fontWeight = FontWeight.Bold)
                }

                Divider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = EmergencyRed)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Sound Siren on SOS Trigger", color = TextPrimary, fontSize = 13.sp)
                    }
                    Switch(
                        checked = audioAlarmOnSos,
                        onCheckedChange = { audioAlarmOnSos = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmergencyRed)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Device & Hardware Settings
        SettingsSectionHeader(title = "ESP32-C3 HARDWARE SETTINGS")

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToDevice() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = SafeGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Device Pairing & Telemetry", color = TextPrimary, fontSize = 13.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                }

                Divider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LockClock, contentDescription = null, tint = InfoCyan)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Background Foreground Service", color = TextPrimary, fontSize = 13.sp)
                            Text("Listen for SOS even when phone is locked", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = backgroundBleMonitoring,
                        onCheckedChange = ::setBackgroundMonitoring,
                        colors = SwitchDefaults.colors(checkedThumbColor = SafeGreen)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        SettingsSectionHeader(title = "BATTERY RELIABILITY")

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Battery optimization", color = TextPrimary, fontSize = 13.sp)
                    Text(
                        text = if (batteryOptimizationExempt) {
                            "RakshaX may keep listening while the screen is locked"
                        } else {
                            "Allow unrestricted battery use for reliable background SOS"
                        },
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                TextButton(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}")
                        )
                        runCatching { context.startActivity(intent) }
                            .onFailure {
                                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                            }
                    },
                    enabled = !batteryOptimizationExempt
                ) {
                    Text(if (batteryOptimizationExempt) "READY" else "OPEN SETTINGS")
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Permission & Privacy Status
        SettingsSectionHeader(title = "PERMISSIONS & SAFETY COMPLIANCE")

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                PermissionStatusRow(
                    "GPS High Precision Location",
                    if (locationGranted) "GRANTED" else "MISSING",
                    if (locationGranted) SafeGreen else WarningAmber
                )
                Divider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 8.dp))
                PermissionStatusRow(
                    "Bluetooth Scan & Connect",
                    when {
                        !bluetoothGranted -> "MISSING"
                        !bluetoothEnabled -> "OFF"
                        else -> "READY"
                    },
                    if (bluetoothGranted && bluetoothEnabled) SafeGreen else WarningAmber
                )
                Divider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 8.dp))
                PermissionStatusRow(
                    "Background SOS listener",
                    if (backgroundBleMonitoring) "ACTIVE" else "OFF",
                    if (backgroundBleMonitoring) SafeGreen else TextMuted
                )
                Divider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 8.dp))
                PermissionStatusRow(
                    "SOS notification permission",
                    if (notificationsGranted) "READY" else "MISSING",
                    if (notificationsGranted) SafeGreen else WarningAmber
                )
                Divider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Direct SOS Phone Call", color = TextSecondary, fontSize = 12.sp)
                        Text(
                            text = if (callPermissionGranted) "Immediate direct calling enabled" else "Dialer confirmation (Tap to allow direct call)",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                    if (!callPermissionGranted) {
                        TextButton(
                            onClick = { callPermissionLauncher.launch(Manifest.permission.CALL_PHONE) },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("GRANT", color = SafeGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("GRANTED", color = SafeGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Divider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Emergency Confirmation SMS", color = TextSecondary, fontSize = 12.sp)
                        Text(
                            text = if (smsPermissionGranted) "Background confirmation SMS enabled" else "Composer fallback (Tap to allow auto-send)",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                    if (!smsPermissionGranted) {
                        TextButton(
                            onClick = { smsPermissionLauncher.launch(Manifest.permission.SEND_SMS) },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("GRANT", color = SafeGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("GRANTED", color = SafeGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Divider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 8.dp))
                PermissionStatusRow("Emergency Call 112 Intent", "READY", SafeGreen)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Mandatory Safety Disclaimer
        Surface(
            color = SurfaceElevated,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Gavel, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SAFETY & LEGAL DISCLAIMER",
                        color = WarningAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "RakshaX assists with emergency alerting and situational risk awareness using the AuroraSafe platform. RakshaX does NOT guarantee emergency rescue or personal safety and is NOT a substitute for official national emergency services. In immediate life-threatening danger, dial 112 directly.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Call112Button()

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onLogout,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed)
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("SIGN OUT / RESET DEMO")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "RakshaX v1.0.0 • Delhi NCR Edition • AuroraSafe 2.0 Integration",
            color = TextMuted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp, start = 4.dp)
    ) {
        Text(
            text = title,
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun PermissionStatusRow(name: String, status: String, statusColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, color = TextSecondary, fontSize = 12.sp)
        Text(status, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
