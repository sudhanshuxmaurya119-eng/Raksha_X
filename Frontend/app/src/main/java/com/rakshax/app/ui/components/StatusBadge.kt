package com.rakshax.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshax.app.data.model.ConnectionStatus
import com.rakshax.app.ui.theme.*

@Composable
fun RiskZoneBadge(
    zone: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, borderColor) = when (zone.uppercase()) {
        "RED" -> Triple(EmergencyRed.copy(alpha = 0.15f), EmergencyRed, EmergencyRed.copy(alpha = 0.5f))
        "YELLOW" -> Triple(WarningAmber.copy(alpha = 0.15f), WarningAmber, WarningAmber.copy(alpha = 0.5f))
        else -> Triple(SafeGreen.copy(alpha = 0.15f), SafeGreen, SafeGreen.copy(alpha = 0.5f))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(textColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$zone ZONE",
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun ConnectionBadge(
    status: ConnectionStatus,
    modifier: Modifier = Modifier
) {
    val (dotColor, text) = when (status) {
        ConnectionStatus.CONNECTED -> Pair(SafeGreen, "RakshaX Online")
        ConnectionStatus.CONNECTING -> Pair(WarningAmber, "Connecting...")
        ConnectionStatus.SCANNING -> Pair(InfoCyan, "Scanning...")
        ConnectionStatus.DISCONNECTED -> Pair(EmergencyRed, "Device Disconnected")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceElevated)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
