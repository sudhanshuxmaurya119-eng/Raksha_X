package com.rakshax.app.ui.screens.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshax.app.ui.theme.*

@Composable
fun SafetyLegendPill(
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { expanded = !expanded },
        color = SurfaceDark.copy(alpha = 0.92f),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Low
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SafeGreen))
                    Text("LOW", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // Medium
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(WarningAmber))
                    Text("MED", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // High
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EmergencyRed))
                    Text("HIGH", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Legend Info",
                    tint = TextSecondary,
                    modifier = Modifier.size(13.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .widthIn(max = 240.dp)
                ) {
                    Divider(color = SurfaceBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Text("RISK THRESHOLD SYSTEM", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))

                    Text("🟢 0–5 cases: Few registered cases (LOW)", color = SafeGreen, fontSize = 10.sp)
                    Text("🟡 6–15 cases: Moderate registered cases (MEDIUM)", color = WarningAmber, fontSize = 10.sp)
                    Text("🔴 16+ cases: High registered cases (HIGH)", color = EmergencyRed, fontSize = 10.sp)

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Based on official incident reports. Individual victim data is strictly protected.",
                        color = TextSecondary,
                        fontSize = 9.sp,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}
