package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AppPreferences
import com.example.data.remote.TransitKnowledgeBase
import com.example.data.repository.TransitRepository
import com.example.data.repository.TripRepository
import com.example.model.OdptApiState
import com.example.model.SeatPreference
import com.example.model.SortPreference
import com.example.model.StationFacilityInfo
import com.example.model.TimeQueryMode
import com.example.model.TransitRouteOption
import com.example.model.TransportMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransitSearchUiState(
    val origin: String = "東京",
    val destination: String = "京都",
    val hour: Int = 9,
    val minute: Int = 0,
    val timeMode: TimeQueryMode = TimeQueryMode.DEPART_AT,
    val sortPreference: SortPreference = SortPreference.FASTEST,
    val seatPreference: SeatPreference = SeatPreference.ANY,
    val maxTransfers: Int? = null,
    val enabledModes: Set<TransportMode> = setOf(
        TransportMode.RAIL,
        TransportMode.SUBWAY,
        TransportMode.LOCAL_BUS,
        TransportMode.HIGHWAY_BUS,
        TransportMode.AIRPORT_BUS,
        TransportMode.FLIGHT,
        TransportMode.FERRY,
        TransportMode.SHARE_CYCLE
    ),
    val routes: List<TransitRouteOption> = emptyList(),
    val selectedRouteId: String? = null,
    val odptState: OdptApiState = OdptApiState.Idle,
    val selectedRailwayId: String = "odpt.Railway:TokyoMetro.Ginza",
    val originFacility: StationFacilityInfo = TransitKnowledgeBase.hubs.first().facilityInfo,
    val isSearching: Boolean = false,
    val statusMessage: String? = null
)

class TransitViewModel(
    private val transitRepository: TransitRepository,
    private val tripRepository: TripRepository,
    private val preferences: AppPreferences,
    private val currentUserId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransitSearchUiState())
    val uiState: StateFlow<TransitSearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedSort = runCatching {
                SortPreference.valueOf(preferences.defaultSort.first())
            }.getOrDefault(SortPreference.FASTEST)
            _uiState.update { it.copy(sortPreference = savedSort) }
            refreshOdptAndSearch()
        }
    }

    fun setOrigin(value: String) {
        _uiState.update {
            it.copy(
                origin = value,
                originFacility = transitRepository.stationFacilityFor(value)
            )
        }
    }

    fun setDestination(value: String) {
        _uiState.update { it.copy(destination = value) }
    }

    fun swapStations() {
        _uiState.update {
            val newOrigin = it.destination
            it.copy(
                origin = newOrigin,
                destination = it.origin,
                originFacility = transitRepository.stationFacilityFor(newOrigin)
            )
        }
        search()
    }

    fun setTimeMode(mode: TimeQueryMode) {
        val (h, m) = when (mode) {
            TimeQueryMode.FIRST_TRAIN -> 5 to 15
            TimeQueryMode.LAST_TRAIN -> 23 to 15
            else -> _uiState.value.hour to _uiState.value.minute
        }
        _uiState.update { it.copy(timeMode = mode, hour = h, minute = m) }
        search()
    }

    fun adjustTimeMinutes(delta: Int) {
        _uiState.update { state ->
            val total = (state.hour * 60 + state.minute + delta + 1440) % 1440
            state.copy(hour = total / 60, minute = total % 60)
        }
        search()
    }

    fun setSortPreference(sort: SortPreference) {
        _uiState.update { it.copy(sortPreference = sort) }
        viewModelScope.launch { preferences.saveDefaultSort(sort.name) }
        search()
    }

    fun setSeatPreference(seat: SeatPreference) {
        _uiState.update { it.copy(seatPreference = seat) }
        search()
    }

    fun setMaxTransfers(max: Int?) {
        _uiState.update { it.copy(maxTransfers = max) }
        search()
    }

    fun toggleMode(mode: TransportMode) {
        _uiState.update { state ->
            val next = if (mode in state.enabledModes) {
                if (state.enabledModes.size > 1) state.enabledModes - mode else state.enabledModes
            } else {
                state.enabledModes + mode
            }
            state.copy(enabledModes = next)
        }
        search()
    }

    fun selectRoute(routeId: String) {
        _uiState.update { it.copy(selectedRouteId = routeId) }
    }

    fun selectOdptRailway(railwayId: String) {
        _uiState.update { it.copy(selectedRailwayId = railwayId) }
        refreshOdptAndSearch()
    }

    fun setOriginFromCoordinates(lat: Double, lng: Double) {
        val nearest = TransitKnowledgeBase.nearestHub(lat, lng)
        _uiState.update {
            it.copy(
                origin = nearest.name,
                originFacility = nearest.facilityInfo,
                statusMessage = "現在地から最寄りの主要拠点「${nearest.name}」を設定しました。"
            )
        }
        search()
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun refreshOdptAndSearch() {
        viewModelScope.launch {
            _uiState.update { it.copy(odptState = OdptApiState.Loading, isSearching = true) }
            val odpt = transitRepository.fetchLiveOdptStatus(_uiState.value.selectedRailwayId)
            _uiState.update { it.copy(odptState = odpt) }
            runRouteQuery(recordRecent = false)
        }
    }

    fun search() {
        viewModelScope.launch {
            runRouteQuery(recordRecent = true)
        }
    }

    private suspend fun runRouteQuery(recordRecent: Boolean) {
        val s = _uiState.value
        val baseMins = s.hour * 60 + s.minute
        val routes = transitRepository.searchRoutes(
            origin = s.origin,
            destination = s.destination,
            baseMinutesOfDay = baseMins,
            sortPreference = s.sortPreference,
            seatPreference = s.seatPreference,
            enabledModes = s.enabledModes,
            maxTransfers = s.maxTransfers,
            odptState = s.odptState
        )
        _uiState.update {
            it.copy(
                routes = routes,
                selectedRouteId = routes.firstOrNull()?.id,
                isSearching = false
            )
        }
        if (recordRecent && s.origin.isNotBlank() && s.destination.isNotBlank()) {
            preferences.recordSearch(s.origin, s.destination)
        }
    }

    fun saveRouteToCloud(option: TransitRouteOption, favorite: Boolean) {
        viewModelScope.launch {
            tripRepository.saveRoute(currentUserId, option, favorite)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            statusMessage = if (favorite) "お気に入り経路に保存しました。" else "経路履歴を保存しました。"
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(statusMessage = err.localizedMessage ?: "経路の保存に失敗しました。")
                    }
                }
        }
    }
}
