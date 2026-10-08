package com.rakshax.app.ui.screens.map

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshax.app.data.model.FacilityType
import com.rakshax.app.data.model.SafetyFacility
import com.rakshax.app.ui.theme.*

@Composable
fun FacilityDetailCard(
    facility: SafetyFacility,
    onClose: () -> Unit,
    onGetDirections: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val (badgeColor, typeIcon, typeLabel) = when (facility.type) {
        FacilityType.POLICE -> Triple(Color(0xFF1E88E5), Icons.Default.LocalPolice, "POLICE STATION")
        FacilityType.NGO -> Triple(SafeGreen, Icons.Default.VolunteerActivism, "WOMEN SAFETY NGO")
        FacilityType.HOSPITAL -> Triple(EmergencyRed, Icons.Default.LocalHospital, "24/7 HOSPITAL & TRAUMA")
        FacilityType.FIRE_STATION -> Triple(Color(0xFFFF8F00), Icons.Default.LocalFireDepartment, "FIRE & RESCUE STATION")
        FacilityType.GOV_CENTER -> Triple(Color(0xFF8E24AA), Icons.Default.AccountBalance, "GOVERNMENT HELP CENTER")
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Type badge & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(imageVector = typeIcon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(14.dp))
                        Text(text = typeLabel, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (facility.verified) {
                        Surface(
                            shape = CircleShape,
                            color = SafeGreen.copy(alpha = 0.15f),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Verified, contentDescription = "Verified", tint = SafeGreen, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Facility Name
            Text(
                text = facility.name,
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Address
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextMuted, modifier = Modifier.size(15.dp).padding(top = 2.dp))
                Text(
                    text = facility.address,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Meta: Distance & 24/7 Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                facility.distanceKm?.let { dist ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                        Text(
                            text = "${"%.1f".format(dist)} km away",
                            color = AccentBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (facility.emergencyAvailable) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SafeGreen))
                        Text("24/7 Open & Active", color = SafeGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Services chips
            if (facility.services.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    facility.services.take(3).forEach { s ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SurfaceElevated
                        ) {
                            Text(
                                text = s,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: CALL, DIRECTIONS, SHARE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Call Button
                if (!facility.phone.isNullOrBlank()) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${facility.phone.replace(" ", "")}"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SafeGreen, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CALL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Directions Button
                Button(
                    onClick = {
                        onGetDirections(facility.latitude, facility.longitude)
                        // Also offer launching external Google Maps app
                        val gmmIntentUri = Uri.parse("google.navigation:q=${facility.latitude},${facility.longitude}&mode=d")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(mapIntent)
                        } else {
                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${facility.latitude},${facility.longitude}"))
                            context.startActivity(webIntent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.3f).height(42.dp)
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DIRECTIONS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
