package com.rakshax.app.ui.screens.map

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshax.app.data.model.FacilityType
import com.rakshax.app.data.model.SafetyFacility
import com.rakshax.app.ui.theme.*

private data class TabDef(
    val label: String,
    val type: FacilityType?,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun NearbyFacilitiesSheet(
    facilities: List<SafetyFacility>,
    onFacilityClick: (SafetyFacility) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        TabDef("All", null, Icons.Default.Explore, AccentBlue),
        TabDef("Police", FacilityType.POLICE, Icons.Default.LocalPolice, Color(0xFF1E88E5)),
        TabDef("NGO", FacilityType.NGO, Icons.Default.VolunteerActivism, SafeGreen),
        TabDef("Hospital", FacilityType.HOSPITAL, Icons.Default.LocalHospital, EmergencyRed),
        TabDef("Fire", FacilityType.FIRE_STATION, Icons.Default.LocalFireDepartment, Color(0xFFFF8F00)),
        TabDef("Gov", FacilityType.GOV_CENTER, Icons.Default.AccountBalance, Color(0xFF8E24AA))
    )

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val filteredFacilities = remember(selectedTabIndex, facilities) {
        val tab = tabs[selectedTabIndex]
        val list = if (tab.type == null) facilities else facilities.filter { it.type == tab.type }
        list.sortedBy { it.distanceKm ?: Float.MAX_VALUE }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.55f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
        shadowElevation = 16.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Drag handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(SurfaceBorder)
                )
            }

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "NEARBY SERVICES",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AccentBlue.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${filteredFacilities.size}",
                            color = AccentBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                edgePadding = 12.dp,
                divider = { HorizontalDivider(color = SurfaceBorder, thickness = 0.5.dp) },
                indicator = {}
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selected = index == selectedTabIndex
                    Tab(
                        selected = selected,
                        onClick = { selectedTabIndex = index },
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) tab.color.copy(alpha = 0.15f) else Color.Transparent,
                            border = if (selected) {
                                androidx.compose.foundation.BorderStroke(1.dp, tab.color.copy(alpha = 0.4f))
                            } else null,
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (selected) tab.color else TextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = tab.label,
                                    color = if (selected) tab.color else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // Facility List
            if (filteredFacilities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No facilities found nearby",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredFacilities, key = { it.id }) { facility ->
                        NearbyFacilityRow(
                            facility = facility,
                            onClick = { onFacilityClick(facility) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NearbyFacilityRow(
    facility: SafetyFacility,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val (badgeColor, typeIcon) = when (facility.type) {
        FacilityType.POLICE -> Pair(Color(0xFF1E88E5), Icons.Default.LocalPolice)
        FacilityType.NGO -> Pair(SafeGreen, Icons.Default.VolunteerActivism)
        FacilityType.HOSPITAL -> Pair(EmergencyRed, Icons.Default.LocalHospital)
        FacilityType.FIRE_STATION -> Pair(Color(0xFFFF8F00), Icons.Default.LocalFireDepartment)
        FacilityType.GOV_CENTER -> Pair(Color(0xFF8E24AA), Icons.Default.AccountBalance)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, SurfaceBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type Icon
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = badgeColor.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name, Address, Distance
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = facility.name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = facility.address,
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                facility.distanceKm?.let { dist ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.DirectionsWalk,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${"%.1f".format(dist)} km",
                            color = AccentBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (facility.emergencyAvailable) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(SafeGreen)
                            )
                            Text("24/7", color = SafeGreen, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Quick Actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!facility.phone.isNullOrBlank()) {
                    IconButton(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_DIAL,
                                Uri.parse("tel:${facility.phone.replace(" ", "")}")
                            )
                            context.startActivity(intent)
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "Call",
                            tint = SafeGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val uri = Uri.parse(
                            "google.navigation:q=${facility.latitude},${facility.longitude}&mode=d"
                        )
                        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(mapIntent)
                        } else {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${facility.latitude},${facility.longitude}")
                                )
                            )
                        }
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Default.Directions,
                        contentDescription = "Directions",
                        tint = AccentBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
