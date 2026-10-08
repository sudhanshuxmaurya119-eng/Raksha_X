package com.rakshax.app.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.rakshax.app.data.location.SosLocationPayload
import com.rakshax.app.data.model.*
import com.rakshax.app.data.remote.AuroraSafeRepository
import com.rakshax.app.data.remote.RemoteSafeRoute
import com.rakshax.app.data.remote.RoutePoint
import com.rakshax.app.data.repository.MockDataRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SafetyMapViewModel(
    private val auroraSafeRepository: AuroraSafeRepository
) : ViewModel() {

    private val _facilities = MutableStateFlow<List<SafetyFacility>>(emptyList())
    val facilities: StateFlow<List<SafetyFacility>> = _facilities.asStateFlow()

    private val _riskAreas = MutableStateFlow<List<SafetyArea>>(emptyList())
    val riskAreas: StateFlow<List<SafetyArea>> = _riskAreas.asStateFlow()

    private val _cases = MutableStateFlow<List<SafetyCase>>(emptyList())
    val cases: StateFlow<List<SafetyCase>> = _cases.asStateFlow()

    private val _filterState = MutableStateFlow(MapFilterState())
    val filterState: StateFlow<MapFilterState> = _filterState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFacility = MutableStateFlow<SafetyFacility?>(null)
    val selectedFacility: StateFlow<SafetyFacility?> = _selectedFacility.asStateFlow()

    private val _selectedArea = MutableStateFlow<SafetyArea?>(null)
    val selectedArea: StateFlow<SafetyArea?> = _selectedArea.asStateFlow()

    private val _isNearbySheetVisible = MutableStateFlow(false)
    val isNearbySheetVisible: StateFlow<Boolean> = _isNearbySheetVisible.asStateFlow()

    private val _isFilterSheetVisible = MutableStateFlow(false)
    val isFilterSheetVisible: StateFlow<Boolean> = _isFilterSheetVisible.asStateFlow()

    private val _isLayersSheetVisible = MutableStateFlow(false)
    val isLayersSheetVisible: StateFlow<Boolean> = _isLayersSheetVisible.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _activeRoute = MutableStateFlow<RemoteSafeRoute?>(null)
    val activeRoute: StateFlow<RemoteSafeRoute?> = _activeRoute.asStateFlow()

    private val _nearbyCategory = MutableStateFlow<FacilityType?>(null)
    val nearbyCategory: StateFlow<FacilityType?> = _nearbyCategory.asStateFlow()

    // Filtered facilities based on filters and search
    val displayedFacilities: StateFlow<List<SafetyFacility>> = combine(
        _facilities,
        _filterState,
        _searchQuery
    ) { facs, filters, query ->
        if (!filters.showFacilities) return@combine emptyList()
        facs.filter { f ->
            val matchesType = f.type in filters.facilityTypes
            val matchesSearch = query.isBlank() ||
                    f.name.contains(query, ignoreCase = true) ||
                    f.address.contains(query, ignoreCase = true) ||
                    (f.area?.contains(query, ignoreCase = true) == true) ||
                    f.services.any { it.contains(query, ignoreCase = true) }
            matchesType && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Filtered risk areas based on filters and search
    val displayedAreas: StateFlow<List<SafetyArea>> = combine(
        _riskAreas,
        _filterState,
        _searchQuery
    ) { areas, filters, query ->
        if (!filters.showHeatmapAreas) return@combine emptyList()
        areas.filter { a ->
            val matchesRisk = a.riskLevel in filters.riskLevels
            val matchesSearch = query.isBlank() || a.name.contains(query, ignoreCase = true)
            matchesRisk && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Filtered cases
    val displayedCases: StateFlow<List<SafetyCase>> = combine(
        _cases,
        _filterState
    ) { casesList, filters ->
        if (!filters.showCases) return@combine emptyList()
        casesList.filter { c ->
            filters.caseCategories.any { c.category.contains(it, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadData()
    }

    fun loadData(userLat: Double? = null, userLng: Double? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val facs = auroraSafeRepository.getMapFacilities(lat = userLat, lng = userLng)
                val areas = auroraSafeRepository.getMapRiskAreas(_filterState.value.timeFilter.queryValue)
                val casesList = auroraSafeRepository.getMapCases(timeRange = _filterState.value.timeFilter.queryValue)

                _facilities.value = facs
                _riskAreas.value = areas
                _cases.value = casesList
                _isOffline.value = false
                _statusMessage.value = "Safety intelligence active • ${facs.size} facilities"
            } catch (e: Exception) {
                _isOffline.value = true
                _statusMessage.value = "Offline mode • Showing cached safety data"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilterState(newState: MapFilterState) {
        _filterState.value = newState
        loadData()
    }

    fun setTimeFilter(filter: MapTimeFilter) {
        _filterState.value = _filterState.value.copy(timeFilter = filter)
        loadData()
    }

    fun selectFacility(facility: SafetyFacility?) {
        _selectedFacility.value = facility
        if (facility != null) {
            _selectedArea.value = null
            _isNearbySheetVisible.value = false
        }
    }

    fun selectArea(area: SafetyArea?) {
        _selectedArea.value = area
        if (area != null) {
            _selectedFacility.value = null
            _isNearbySheetVisible.value = false
        }
    }

    fun toggleNearbySheet(show: Boolean) {
        _isNearbySheetVisible.value = show
    }

    fun toggleFilterSheet(show: Boolean) {
        _isFilterSheetVisible.value = show
    }

    fun toggleLayersSheet(show: Boolean) {
        _isLayersSheetVisible.value = show
    }

    fun setNearbyCategory(type: FacilityType?) {
        _nearbyCategory.value = type
    }

    fun calculateDirections(userLat: Double, userLng: Double, destLat: Double, destLng: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            val route = auroraSafeRepository.fetchSafeRoute(
                origin = RoutePoint(userLat, userLng),
                destination = RoutePoint(destLat, destLng)
            ).value
            _activeRoute.value = route
            _isLoading.value = false
        }
    }

    fun clearDirections() {
        _activeRoute.value = null
    }

    fun triggerEmergencySos(location: LatLng?, onSosTriggered: () -> Unit) {
        viewModelScope.launch {
            val contacts = MockDataRepository.contacts.value
                .filter { it.isEnabled }
                .map { it.name to it.phone }
            val payload = location?.let {
                SosLocationPayload(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    accuracyMeters = 5f,
                    timestampMillis = System.currentTimeMillis()
                )
            }
            auroraSafeRepository.triggerSos(
                location = payload,
                locationText = location?.let { "${it.latitude}, ${it.longitude}" } ?: "Location unavailable",
                contacts = contacts,
                userId = MockDataRepository.currentUser.value.id
            )
            onSosTriggered()
        }
    }
}
