package com.rakshax.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshax.app.ui.navigation.Screen
import com.rakshax.app.ui.theme.*

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

@Composable
fun RakshaXBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem(Screen.Home.route, "Home", Icons.Default.Home),
        BottomNavItem(Screen.SafetyMap.route, "Map", Icons.Default.Map),
        BottomNavItem(Screen.SosStatus.route, "SOS Live", Icons.Default.Warning),
        BottomNavItem(Screen.Device.route, "Device", Icons.Default.Bluetooth),
        BottomNavItem(Screen.Contacts.route, "Contacts", Icons.Default.People),
        BottomNavItem(Screen.Profile.route, "Settings", Icons.Default.Settings)
    )

    NavigationBar(
        containerColor = SurfaceDark,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = SurfaceBorder, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            val itemColor = if (item.route == Screen.SosStatus.route) {
                if (selected) EmergencyRed else EmergencyRed.copy(alpha = 0.6f)
            } else {
                if (selected) AccentBlue else TextSecondary
            }

            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = itemColor,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 10.sp,
                        color = itemColor
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = SurfaceElevated
                )
            )
        }
    }
}
