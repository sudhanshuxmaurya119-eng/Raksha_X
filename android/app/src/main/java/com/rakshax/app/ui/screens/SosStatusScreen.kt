package com.rakshax.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.rakshax.app.data.repository.MockDataRepository
import com.rakshax.app.data.remote.AuroraSafeRepository
import com.rakshax.app.service.SosEscalationService
import com.rakshax.app.ui.components.Call112Button
import com.rakshax.app.ui.theme.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun SosStatusScreen(auroraSafeRepository: AuroraSafeRepository) {
    val sosState by MockDataRepository.sosState.collectAsState()
    val contacts by MockDataRepository.contacts.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(sosState.backendEventId) {
        val eventId = sosState.backendEventId ?: return@LaunchedEffect
        while (isActive && sosState.isActive && sosState.acknowledgedContactName == null) {
            val result = auroraSafeRepository.fetchSosStatus(eventId)
            val acknowledgedContact = result.value?.acknowledgedContact
            if (acknowledgedContact != null) {
                MockDataRepository.acknowledgeSos(acknowledgedContact)
                SosEscalationService.stop(context)
                return@LaunchedEffect
            }
            delay(10_000L)
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
        // Status Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = if (sosState.isActive) EmergencyRedDark.copy(alpha = 0.2f) else SafeGreenDark.copy(alpha = 0.2f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (sosState.isActive) EmergencyRed else SafeGreen
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (sosState.isActive) EmergencyRed else SafeGreen)
                ) {
                    Icon(
                        imageVector = if (sosState.isActive) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = if (sosState.isActive) "EMERGENCY SOS BROADCAST ACTIVE" else "ALL CLEAR — SYSTEM NORMAL",
                        color = if (sosState.isActive) EmergencyRed else SafeGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (sosState.isActive) "Triggered via ${sosState.triggerSource}"
                        else "No emergency in progress",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "AuroraSafe status: ${sosState.backendStatus}" +
                (sosState.backendError?.let { " • $it" } ?: ""),
            color = if (sosState.backendStatus == "SENT" || sosState.backendStatus == "ACKNOWLEDGED") {
                SafeGreen
            } else WarningAmber,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // SOS Pipeline Progression Checklist Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "EMERGENCY LIFELINE PIPELINE",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Step 1: Button Detection
                PipelineStepRow(
                    title = "Hardware Button Detected",
                    subtitle = "ESP32-C3 GPIO4 held for 2s — Debounced",
                    isComplete = sosState.buttonDetected,
                    isPending = false
                )

                StepConnector()

                // Step 2: Location
                PipelineStepRow(
                    title = "Accurate Location Captured",
                    subtitle = if (sosState.locationFound) {
                        "${sosState.locationAddress} • ${sosState.locationAccuracyMeters?.toInt() ?: 0} m accuracy"
                    } else {
                        "No GPS fix captured; alert continues without coordinates"
                    },
                    isComplete = sosState.locationFound,
                    isPending = false
                )

                StepConnector()

                // Step 3: Backend & FCM
                PipelineStepRow(
                    title = "AuroraSafe Incident Logged & Alert Sent",
                    subtitle = when (sosState.backendStatus) {
                        "SENT" -> "Incident logged and push delivery requested"
                        "ACKNOWLEDGED" -> "Incident logged and acknowledged by a contact"
                        "QUEUED" -> "Offline queue retained the alert for retry"
                        else -> "Waiting for AuroraSafe delivery"
                    },
                    isComplete = sosState.alertSent,
                    isPending = false
                )

                StepConnector()

                // Step 4: Contact 1 Acknowledged
                PipelineStepRow(
                    title = "Priority ${sosState.escalationStage} trusted contact",
                    subtitle = if (sosState.acknowledgedContactName != null) "ACKNOWLEDGED at 19:42 — Help on the way"
                    else "Pending response; automatic escalation is active",
                    isComplete = sosState.acknowledgedContactName != null,
                    isPending = sosState.acknowledgedContactName == null
                )

                StepConnector()

                // Step 5: Contact 2 Status
                PipelineStepRow(
                    title = "Contact 2 (Priya Sharma - Spouse)",
                    subtitle = if (sosState.acknowledgedContactName != null) "Escalation paused — Alert already acknowledged"
                    else "Queued for Priority 2 escalation",
                    isComplete = false,
                    isPending = true
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // The same action is usable from the app when the notification action is unavailable.
        if (sosState.isActive && sosState.acknowledgedContactName == null) {
            Button(
                onClick = {
                    scope.launch {
                        val contactName = contacts.firstOrNull { it.isEnabled }?.name ?: "Trusted contact"
                        val eventId = sosState.backendEventId
                        val result = if (eventId == null) null else {
                            auroraSafeRepository.acknowledgeSos(eventId, contactName)
                        }
                        if (result == null || result.isSuccess) {
                            MockDataRepository.acknowledgeSos(contactName)
                            SosEscalationService.stop(context)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ACKNOWLEDGE SOS ALERT")
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Cancel / Resolve Action
        if (sosState.isActive) {
            OutlinedButton(
                onClick = {
                    MockDataRepository.cancelSos()
                    SosEscalationService.stop(context)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                Text("RESOLVE & DEACTIVATE SOS", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Button(
                onClick = {
                    MockDataRepository.triggerSos("In-App Test Trigger", backendStatus = "DEMO")
                    SosEscalationService.start(context)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("TEST SOS PIPELINE")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Emergency Call 112
        Call112Button()
    }
}

@Composable
private fun PipelineStepRow(
    title: String,
    subtitle: String,
    isComplete: Boolean,
    isPending: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isComplete -> SafeGreen
                        isPending -> SurfaceElevated
                        else -> TextMuted
                    }
                )
                .border(
                    1.dp,
                    if (isPending) WarningAmber else Color.Transparent,
                    CircleShape
                )
        ) {
            if (isComplete) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            } else if (isPending) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(WarningAmber)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = if (isComplete) SafeGreen else TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun StepConnector() {
    Box(
        modifier = Modifier
            .padding(start = 13.dp)
            .height(20.dp)
            .width(2.dp)
            .background(SurfaceBorder)
    )
}
