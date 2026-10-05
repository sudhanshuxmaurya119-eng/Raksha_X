package com.rakshax.app.data.repository

import com.rakshax.app.data.model.*
import com.rakshax.app.data.location.SosLocationPayload
import com.rakshax.app.data.remote.RemoteHotspot
import com.rakshax.app.data.remote.RemoteIncident
import com.rakshax.app.data.remote.RemoteRiskScore
import com.rakshax.app.data.remote.AuthSession
import com.rakshax.app.data.sos.SosEscalationPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object MockDataRepository {

    private val demoUser = User(
        id = "usr_001",
        username = "Aarav Sharma",
        email = "aarav.sharma@rakshax.org",
        phone = "+91 98765 43210"
    )

    // Current User
    private val _currentUser = MutableStateFlow(demoUser)
    val currentUser: StateFlow<User> = _currentUser.asStateFlow()

    fun setCurrentUser(session: AuthSession) {
        _currentUser.update { current ->
            val displayEmail = session.email
                ?.takeUnless { it.endsWith("@rakshax.local", ignoreCase = true) }
                ?: if (!session.phone.isNullOrBlank()) "Phone verified account" else current.email
            current.copy(
                id = session.userId ?: current.id,
                username = session.username ?: current.username,
                email = displayEmail,
                phone = session.phone ?: current.phone,
                age = session.age ?: current.age
            )
        }
    }

    fun resetCurrentUser() {
        _currentUser.value = demoUser
    }

    // Trusted Contacts
    private val _contacts = MutableStateFlow(
        listOf(
            Contact(
                id = "c1",
                name = "Rahul Sharma",
                phone = "+91 98111 22334",
                priority = 1,
                isEnabled = true,
                relation = "Brother",
                acknowledged = true
            ),
            Contact(
                id = "c2",
                name = "Priya Sharma",
                phone = "+91 98222 33445",
                priority = 2,
                isEnabled = true,
                relation = "Spouse",
                acknowledged = false
            ),
            Contact(
                id = "c3",
                name = "Anil Verma",
                phone = "+91 98333 44556",
                priority = 3,
                isEnabled = true,
                relation = "Close Friend",
                acknowledged = false
            )
        )
    )
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    // Device BLE State
    private val _deviceState = MutableStateFlow(DeviceState())
    val deviceState: StateFlow<DeviceState> = _deviceState.asStateFlow()

    // SOS State
    private val _sosState = MutableStateFlow(
        SosState(
            isActive = false,
            triggerSource = "Physical ESP32 Button",
            buttonDetected = true,
            locationFound = true,
            alertSent = true,
            locationAddress = "Connaught Place, New Delhi",
            acknowledgedContactName = "Rahul Sharma (Brother)",
            pendingContactsCount = 1
        )
    )
    val sosState: StateFlow<SosState> = _sosState.asStateFlow()

    // Safety Risk Info
    private val _riskScore = MutableStateFlow(
        RiskScoreInfo(
            score = 0.72f,
            zone = "YELLOW",
            confidence = 0.84f,
            nearbyIncidentsCount = 5,
            nearestHotspotDistanceKm = 0.8f,
            message = "Moderate risk zone in Delhi NCR. Stay in well-lit areas."
        )
    )
    val riskScore: StateFlow<RiskScoreInfo> = _riskScore.asStateFlow()

    // Incidents; seeded values remain available when the backend is offline.
    private val _incidents = MutableStateFlow(listOf(
        IncidentMarker("inc_1", "Suspicious Activity", "suspicious_activity", 6, "YELLOW", 28.6328, 77.2197, "15m ago", "Outer Circle, Connaught Place"),
        IncidentMarker("inc_2", "Severe Harassment", "harassment", 9, "RED", 28.6640, 77.2584, "42m ago", "Seelampur Metro Approach"),
        IncidentMarker("inc_3", "Theft Reported", "theft", 5, "YELLOW", 28.5494, 77.2001, "1h ago", "Hauz Khas Village Lane 2"),
        IncidentMarker("inc_4", "Poor Lighting / Hazard", "other", 4, "GREEN", 28.5921, 77.2281, "2h ago", "Lodhi Road Outer"),
        IncidentMarker("inc_5", "Assault Attempt", "assault", 10, "RED", 28.6425, 77.2119, "3h ago", "Paharganj Market Back Alley")
    ))
    val incidents: StateFlow<List<IncidentMarker>> = _incidents.asStateFlow()
    val mockIncidents: List<IncidentMarker> get() = _incidents.value

    // Hotspot zones; replaced by live AuroraSafe data when available.
    private val _hotspots = MutableStateFlow(listOf(
        HotspotZone("hz_1", "Seelampur High-Risk Cluster", "RED", 1.2f, 8, 28.6640, 77.2584),
        HotspotZone("hz_2", "Paharganj Transit Cluster", "RED", 0.9f, 6, 28.6425, 77.2119),
        HotspotZone("hz_3", "Outer Ring Hotspot", "YELLOW", 1.5f, 4, 28.5293, 77.1557)
    ))
    val hotspots: StateFlow<List<HotspotZone>> = _hotspots.asStateFlow()
    val mockHotspots: List<HotspotZone> get() = _hotspots.value

    // Contact Operations
    fun addContact(name: String, phone: String, priority: Int, relation: String) {
        val newContact = Contact(
            id = "c_${System.currentTimeMillis()}",
            name = name,
            phone = phone,
            priority = priority,
            relation = relation,
            isEnabled = true
        )
        _contacts.update { it + newContact }
    }

    fun updateContact(contact: Contact) {
        _contacts.update { list ->
            list.map { if (it.id == contact.id) contact else it }
        }
    }

    fun deleteContact(contactId: String) {
        _contacts.update { list ->
            list.filterNot { it.id == contactId }
        }
    }

    fun toggleContactEnabled(contactId: String) {
        _contacts.update { list ->
            list.map { if (it.id == contactId) it.copy(isEnabled = !it.isEnabled) else it }
        }
    }

    // Device Operations
    fun toggleDeviceConnection() {
        _deviceState.update { current ->
            if (current.status == ConnectionStatus.CONNECTED) {
                current.copy(status = ConnectionStatus.DISCONNECTED)
            } else {
                current.copy(status = ConnectionStatus.CONNECTED, batteryLevel = 85)
            }
        }
    }

    fun updateDeviceState(deviceState: DeviceState) {
        _deviceState.value = deviceState
    }

    // SOS Operations
    fun triggerSos(
        source: String = "In-App Emergency Button",
        location: SosLocationPayload? = null,
        backendStatus: String = "QUEUED",
        backendEventId: String? = null,
        backendError: String? = null
    ) {
        _sosState.update {
            it.copy(
                isActive = true,
                triggerSource = source,
                buttonDetected = true,
                locationFound = location != null,
                alertSent = backendStatus == "SENT" || backendStatus == "DEMO",
                latitude = location?.latitude ?: it.latitude,
                longitude = location?.longitude ?: it.longitude,
                locationAccuracyMeters = location?.accuracyMeters,
                locationTimestampMillis = location?.timestampMillis,
                locationAddress = location?.let { payload ->
                    "${payload.latitude.formatCoordinate()}, ${payload.longitude.formatCoordinate()}"
                } ?: "Location unavailable",
                acknowledgedContactName = null,
                pendingContactsCount = 2,
                timestamp = "Just now",
                backendStatus = backendStatus,
                backendEventId = backendEventId,
                backendError = backendError,
                escalationStage = 1,
                escalationDeadlineMillis = System.currentTimeMillis() + ESCALATION_WINDOW_MILLIS
            )
        }
    }

    fun setRiskScore(score: RemoteRiskScore) {
        _riskScore.value = RiskScoreInfo(
            score = score.score,
            zone = score.zone,
            confidence = score.confidence,
            nearbyIncidentsCount = score.nearbyIncidents,
            nearestHotspotDistanceKm = _riskScore.value.nearestHotspotDistanceKm,
            message = score.message
        )
    }

    fun setRemoteIncidents(incidents: List<RemoteIncident>) {
        _incidents.value = incidents.map {
            IncidentMarker(
                id = it.id,
                title = it.description,
                incidentType = it.incidentType,
                severity = it.severity,
                zone = it.zone,
                latitude = it.latitude,
                longitude = it.longitude,
                timeAgo = it.createdAt ?: "recently",
                address = it.address ?: "Location unavailable"
            )
        }
    }

    fun setRemoteHotspots(hotspots: List<RemoteHotspot>) {
        _hotspots.value = hotspots.map {
            HotspotZone(
                id = it.id,
                name = it.name,
                riskLevel = it.riskLevel,
                radiusKm = it.radiusKm,
                incidentCount = it.incidentCount,
                latitude = it.latitude,
                longitude = it.longitude
            )
        }
    }

    fun acknowledgeSos(contactName: String) {
        _sosState.update {
            it.copy(
                acknowledgedContactName = contactName,
                pendingContactsCount = 0,
                backendStatus = "ACKNOWLEDGED",
                escalationDeadlineMillis = null
            )
        }
    }

    fun advanceEscalation() {
        _sosState.update { state ->
            if (!state.isActive || state.acknowledgedContactName != null) return@update state
            val nextStage = SosEscalationPolicy.nextStage(
                currentStage = state.escalationStage,
                acknowledged = false
            ) ?: return@update state
            state.copy(
                escalationStage = nextStage,
                pendingContactsCount = SosEscalationPolicy.pendingContacts(nextStage),
                backendStatus = "ESCALATED",
                escalationDeadlineMillis = if (nextStage < 3) {
                    System.currentTimeMillis() + ESCALATION_WINDOW_MILLIS
                } else null
            )
        }
    }

    fun cancelSos() {
        _sosState.update {
            it.copy(isActive = false, escalationDeadlineMillis = null)
        }
    }
}

private const val ESCALATION_WINDOW_MILLIS = SosEscalationPolicy.STAGE_TIMEOUT_MILLIS

private fun Double.formatCoordinate(): String = "%.6f".format(java.util.Locale.US, this)
