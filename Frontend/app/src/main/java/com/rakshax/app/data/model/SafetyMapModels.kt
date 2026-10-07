package com.rakshax.app.data.model

enum class FacilityType(val label: String, val categoryTag: String) {
    POLICE("Police Station", "police"),
    NGO("NGO Support", "ngo"),
    HOSPITAL("Hospital", "hospital"),
    FIRE_STATION("Fire Station", "fire_station"),
    GOV_CENTER("Gov Help Center", "government_center");

    companion object {
        fun fromString(type: String): FacilityType = when (type.lowercase().trim()) {
            "police" -> POLICE
            "ngo" -> NGO
            "hospital" -> HOSPITAL
            "fire_station", "fire" -> FIRE_STATION
            "government_center", "gov", "government" -> GOV_CENTER
            else -> POLICE
        }
    }
}

enum class RiskLevel(val label: String, val description: String) {
    LOW("LOW RISK", "Few registered cases"),
    MEDIUM("MEDIUM RISK", "Moderate registered cases"),
    HIGH("HIGH RISK", "High registered cases");

    companion object {
        fun fromString(level: String): RiskLevel = when (level.uppercase().trim()) {
            "LOW" -> LOW
            "MEDIUM" -> MEDIUM
            "HIGH" -> HIGH
            else -> LOW
        }
    }
}

data class SafetyFacility(
    val id: String,
    val name: String,
    val type: FacilityType,
    val address: String,
    val phone: String?,
    val latitude: Double,
    val longitude: Double,
    val services: List<String>,
    val emergencyAvailable: Boolean,
    val verified: Boolean,
    val area: String?,
    val distanceKm: Float? = null
)

data class SafetyArea(
    val id: String,
    val name: String,
    val centerLat: Double,
    val centerLng: Double,
    val radiusMeters: Double,
    val riskLevel: RiskLevel,
    val registeredCases: Int,
    val casesThisMonth: Int,
    val topCategory: String,
    val safetyScore: Int,
    val nearbyPoliceCount: Int,
    val nearbyNgoCount: Int,
    val nearbyHospitalCount: Int
)

data class SafetyCase(
    val caseId: String,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val severity: String,
    val status: String,
    val createdAt: String,
    val area: String
)

enum class MapTimeFilter(val label: String, val queryValue: String) {
    ALL_TIME("All Time", "all"),
    TODAY("Today", "today"),
    THIS_WEEK("This Week", "week"),
    THIS_MONTH("This Month", "month"),
    LAST_3_MONTHS("Last 3 Months", "quarter")
}

data class MapFilterState(
    val riskLevels: Set<RiskLevel> = setOf(RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH),
    val facilityTypes: Set<FacilityType> = setOf(
        FacilityType.POLICE,
        FacilityType.NGO,
        FacilityType.HOSPITAL,
        FacilityType.FIRE_STATION,
        FacilityType.GOV_CENTER
    ),
    val caseCategories: Set<String> = setOf("Harassment", "Theft", "Violence", "Missing Person", "Other"),
    val timeFilter: MapTimeFilter = MapTimeFilter.ALL_TIME,
    val showHeatmapAreas: Boolean = true,
    val showFacilities: Boolean = true,
    val showCases: Boolean = true,
    val mapTypeIndex: Int = 0 // 0: Normal, 1: Satellite, 2: Terrain
)

data class MapThresholdConfig(
    val lowMaxCases: Int = 5,
    val mediumMaxCases: Int = 15,
    val highMinCases: Int = 16
)
