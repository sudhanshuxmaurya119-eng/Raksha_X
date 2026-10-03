package com.rakshax.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshax.app.ui.theme.*

data class OnboardingPageData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconColor: Color,
    val badge: String
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    val pages = listOf(
        OnboardingPageData(
            title = "Physical Hardware SOS",
            description = "A miniature ESP32-C3 BLE device in your pocket or keychain. Press and hold the discreet button for 2 seconds to trigger an instant emergency alert—even if your phone is locked.",
            icon = Icons.Default.Bluetooth,
            iconColor = EmergencyRed,
            badge = "ESP32-C3 HARDWARE"
        ),
        OnboardingPageData(
            title = "Location-Aware Safety",
            description = "High-precision GPS coordinates paired with AuroraSafe's AI intelligence to evaluate local threat levels, nearby hotspots, and safest routes.",
            icon = Icons.Default.LocationOn,
            iconColor = WarningAmber,
            badge = "AURORASAFE INTELLIGENCE"
        ),
        OnboardingPageData(
            title = "Escalation & 112 Dialing",
            description = "Sequential priority alerts to your trusted circle with live acknowledgement tracking, backed by an instant 112 national emergency button.",
            icon = Icons.Default.Phone,
            iconColor = SafeGreen,
            badge = "TRUSTED CONTACTS & 112"
        )
    )

    var currentPage by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Skip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onFinished) {
                Text(
                    text = "SKIP",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Center Content
        val page = pages[currentPage]
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(page.iconColor.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = page.iconColor,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                color = SurfaceElevated,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = page.badge,
                    color = page.iconColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = page.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = page.description,
                fontSize = 14.sp,
                color = TextSecondary,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            )
        }

        // Bottom Controls: Indicators + Button
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pager Dots
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 28.dp)
            ) {
                pages.indices.forEach { index ->
                    val isSelected = index == currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(6.dp)
                            .width(if (isSelected) 24.dp else 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isSelected) EmergencyRed else SurfaceBorder)
                    )
                }
            }

            Button(
                onClick = {
                    if (currentPage < pages.size - 1) {
                        currentPage++
                    } else {
                        onFinished()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmergencyRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (currentPage == pages.size - 1) "GET STARTED" else "CONTINUE",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
