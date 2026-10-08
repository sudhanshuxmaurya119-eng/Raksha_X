package com.rakshax.app.ui.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshax.app.data.model.RiskLevel
import com.rakshax.app.data.model.SafetyArea
import com.rakshax.app.ui.theme.*

@Composable
fun AreaSafetyReportSheet(
    area: SafetyArea,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (riskColor, riskBg) = when (area.riskLevel) {
        RiskLevel.LOW -> Pair(SafeGreen, SafeGreen.copy(alpha = 0.15f))
        RiskLevel.MEDIUM -> Pair(WarningAmber, WarningAmber.copy(alpha = 0.15f))
        RiskLevel.HIGH -> Pair(EmergencyRed, EmergencyRed.copy(alpha = 0.15f))
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
        shadowElevation = 14.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = riskColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "AREA SAFETY REPORT",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Area Name & Risk Level Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = area.name,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Aggregated Locality & Ward Intelligence",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = riskBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, riskColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(riskColor))
                        Text(
                            text = area.riskLevel.label,
                            color = riskColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Safety Score Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Safety Score: ${area.safetyScore} / 100",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Calculated from verified local case density",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = riskColor.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${area.safetyScore}",
                                color = riskColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Key Statistics Grid (2 columns)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Column 1
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(title = "Registered Cases", value = "${area.registeredCases}", icon = Icons.Default.FolderOpen)
                    MetricCard(title = "Cases This Month", value = "${area.casesThisMonth}", icon = Icons.Default.DateRange)
                    MetricCard(title = "Top Category", value = area.topCategory, icon = Icons.Default.Category)
                }

                // Column 2
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(title = "Nearby Police Stations", value = "${area.nearbyPoliceCount}", icon = Icons.Default.LocalPolice)
                    MetricCard(title = "Nearby NGOs", value = "${area.nearbyNgoCount}", icon = Icons.Default.VolunteerActivism)
                    MetricCard(title = "Nearby Hospitals", value = "${area.nearbyHospitalCount}", icon = Icons.Default.LocalHospital)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Transparency & Privacy note
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Privacy Protected",
                    tint = TextMuted,
                    modifier = Modifier.size(13.dp).padding(top = 2.dp)
                )
                Text(
                    text = "Aggregated geographical report. Individual victim identities and sensitive personal details are strictly protected.",
                    color = TextMuted,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceElevated.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, SurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
            Column {
                Text(text = title, color = TextMuted, fontSize = 9.sp)
                Text(text = value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}
