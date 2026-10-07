package com.rakshax.app.ui.screens.map

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshax.app.data.model.*
import com.rakshax.app.ui.theme.*

@Composable
fun MapFilterBottomSheet(
    filterState: MapFilterState,
    onFilterChanged: (MapFilterState) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var localState by remember(filterState) { mutableStateOf(filterState) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.75f),
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
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = EmergencyRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "MAP FILTERS",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        localState = MapFilterState()
                        onFilterChanged(localState)
                    }) {
                        Text("Reset", color = TextMuted, fontSize = 12.sp)
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
            }

            HorizontalDivider(color = SurfaceBorder, thickness = 0.5.dp)

            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // ===== RISK LEVELS =====
                FilterSection(
                    title = "SAFETY LEVEL",
                    icon = Icons.Default.Shield,
                    iconColor = EmergencyRed
                ) {
                    RiskLevel.entries.forEach { level ->
                        val (color, label) = when (level) {
                            RiskLevel.LOW -> Pair(SafeGreen, "Low Risk")
                            RiskLevel.MEDIUM -> Pair(WarningAmber, "Medium Risk")
                            RiskLevel.HIGH -> Pair(EmergencyRed, "High Risk")
                        }
                        val checked = level in localState.riskLevels
                        FilterCheckRow(
                            label = label,
                            checked = checked,
                            color = color,
                            onCheckedChange = {
                                val updated = if (checked) {
                                    localState.riskLevels - level
                                } else {
                                    localState.riskLevels + level
                                }
                                localState = localState.copy(riskLevels = updated)
                                onFilterChanged(localState)
                            }
                        )
                    }
                }

                // ===== FACILITY TYPES =====
                FilterSection(
                    title = "NEARBY SERVICES",
                    icon = Icons.Default.LocationOn,
                    iconColor = AccentBlue
                ) {
                    val facilityDefs = listOf(
                        Triple(FacilityType.POLICE, "Police Stations", Color(0xFF1E88E5)),
                        Triple(FacilityType.NGO, "NGOs & Support", SafeGreen),
                        Triple(FacilityType.HOSPITAL, "Hospitals", EmergencyRed),
                        Triple(FacilityType.FIRE_STATION, "Fire Stations", Color(0xFFFF8F00)),
                        Triple(FacilityType.GOV_CENTER, "Gov Help Centers", Color(0xFF8E24AA))
                    )
                    facilityDefs.forEach { (type, label, color) ->
                        val checked = type in localState.facilityTypes
                        FilterCheckRow(
                            label = label,
                            checked = checked,
                            color = color,
                            onCheckedChange = {
                                val updated = if (checked) {
                                    localState.facilityTypes - type
                                } else {
                                    localState.facilityTypes + type
                                }
                                localState = localState.copy(facilityTypes = updated)
                                onFilterChanged(localState)
                            }
                        )
                    }
                }

                // ===== CASE CATEGORIES =====
                FilterSection(
                    title = "CASE CATEGORIES",
                    icon = Icons.Default.Category,
                    iconColor = WarningAmber
                ) {
                    val categories = listOf("Harassment", "Theft", "Violence", "Missing Person", "Other")
                    categories.forEach { cat ->
                        val checked = cat in localState.caseCategories
                        FilterCheckRow(
                            label = cat,
                            checked = checked,
                            color = WarningAmber,
                            onCheckedChange = {
                                val updated = if (checked) {
                                    localState.caseCategories - cat
                                } else {
                                    localState.caseCategories + cat
                                }
                                localState = localState.copy(caseCategories = updated)
                                onFilterChanged(localState)
                            }
                        )
                    }
                }

                // ===== TIME RANGE =====
                FilterSection(
                    title = "TIME RANGE",
                    icon = Icons.Default.AccessTime,
                    iconColor = InfoCyan
                ) {
                    MapTimeFilter.entries.forEach { tf ->
                        val selected = localState.timeFilter == tf
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) InfoCyan.copy(alpha = 0.15f) else Color.Transparent,
                            border = if (selected) {
                                androidx.compose.foundation.BorderStroke(1.dp, InfoCyan.copy(alpha = 0.4f))
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    localState = localState.copy(timeFilter = tf)
                                    onFilterChanged(localState)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = {
                                        localState = localState.copy(timeFilter = tf)
                                        onFilterChanged(localState)
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = InfoCyan,
                                        unselectedColor = TextMuted
                                    ),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = tf.label,
                                    color = if (selected) InfoCyan else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // ===== MAP LAYERS =====
                FilterSection(
                    title = "MAP LAYERS",
                    icon = Icons.Default.Layers,
                    iconColor = AccentBlue
                ) {
                    FilterSwitchRow(
                        label = "Safety Heatmap Areas",
                        checked = localState.showHeatmapAreas,
                        color = EmergencyRed,
                        onCheckedChange = {
                            localState = localState.copy(showHeatmapAreas = it)
                            onFilterChanged(localState)
                        }
                    )
                    FilterSwitchRow(
                        label = "Facility Markers",
                        checked = localState.showFacilities,
                        color = AccentBlue,
                        onCheckedChange = {
                            localState = localState.copy(showFacilities = it)
                            onFilterChanged(localState)
                        }
                    )
                    FilterSwitchRow(
                        label = "Case Markers",
                        checked = localState.showCases,
                        color = WarningAmber,
                        onCheckedChange = {
                            localState = localState.copy(showCases = it)
                            onFilterChanged(localState)
                        }
                    )
                }

                // ===== MAP TYPE =====
                FilterSection(
                    title = "MAP TYPE",
                    icon = Icons.Default.Map,
                    iconColor = TextSecondary
                ) {
                    val mapTypes = listOf("Normal", "Satellite", "Terrain")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        mapTypes.forEachIndexed { index, label ->
                            val selected = localState.mapTypeIndex == index
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) AccentBlue.copy(alpha = 0.15f) else SurfaceElevated,
                                border = if (selected) {
                                    androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.5f))
                                } else {
                                    androidx.compose.foundation.BorderStroke(0.5.dp, SurfaceBorder)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        localState = localState.copy(mapTypeIndex = index)
                                        onFilterChanged(localState)
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (selected) AccentBlue else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }
        content()
    }
}

@Composable
private fun FilterCheckRow(
    label: String,
    checked: Boolean,
    color: Color,
    onCheckedChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCheckedChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
            colors = CheckboxDefaults.colors(
                checkedColor = color,
                uncheckedColor = TextMuted,
                checkmarkColor = Color.White
            ),
            modifier = Modifier.size(20.dp)
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (checked) color else TextMuted.copy(alpha = 0.4f))
        )
        Text(
            text = label,
            color = if (checked) TextPrimary else TextMuted,
            fontSize = 13.sp,
            fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun FilterSwitchRow(
    label: String,
    checked: Boolean,
    color: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (checked) color else TextMuted.copy(alpha = 0.4f))
            )
            Text(
                text = label,
                color = if (checked) TextPrimary else TextMuted,
                fontSize = 13.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = color,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = SurfaceElevated
            ),
            modifier = Modifier.height(24.dp)
        )
    }
}
