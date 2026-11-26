package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AppPreferences
import com.example.data.remote.NationalRailwayRegistry
import com.example.data.remote.TransitKnowledgeBase
import com.example.data.repository.TransitRepository
import com.example.data.repository.TripRepository
import com.example.model.OdptApiState
import com.example.model.ProviderResult
import com.example.model.RailwayStation
import com.example.model.RouteResult
import com.example.model.RouteSearchRequest
import com.example.model.SeatPreference
import com.example.model.SortPreference
import com.example.model.StationFacilityInfo
import com.example.model.StationTimetable
import com.example.model.TimeQueryMode
import com.example.model.Train
import com.example.model.TrainOperationStatus
import com.example.model.TransitRouteOption
import com.example.model.TransportMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TransitSectionTab(val labelJa: String, val testTag: String) {
    ROUTE_SEARCH("乗換案内", "transit_tab_routes"),
    LIVE_POSITIONS("列車走行位置", "transit_tab_live_map"),
    TIMETABLE("駅時刻表", "transit_tab_timetable"),
    COVERAGE("全国対応状況", "transit_tab_coverage")
}

data class TransitSearchUiState(
    val activeTab: TransitSectionTab = TransitSectionTab.ROUTE_SEARCH,
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
    val routeResults: List<RouteResult> = emptyList(),
    val routeSourceSummary: String = "標準ダイヤ参照",
    val routeProviderNotice: String? = null,
    val selectedRouteId: String? = null,
    val odptState: OdptApiState = OdptApiState.Idle,
    val selectedRailwayId: String = "odpt.Railway:TokyoMetro.Ginza",
    val originFacility: StationFacilityInfo = TransitKnowledgeBase.hubs.first().facilityInfo,
    val isSearching: Boolean = false,
    val statusMessage: String? = null,
    // Nationwide live position, operation status, timetable, and operator coverage state
    val operatorSearchQuery: String = "",
    val selectedRegionFilter: String = "すべて",
    val selectedOperatorId: String = "op_tokyo_metro",
    val liveLineStations: List<RailwayStation> = NationalRailwayRegistry.stationsForLine("odpt.Railway:TokyoMetro.Ginza"),
    val liveTrainsResult: ProviderResult<List<Train>>? = null,
    val isLoadingTrains: Boolean = false,
    val selectedTrainId: String? = null,
    val operationStatusResult: ProviderResult<List<TrainOperationStatus>>? = null,
    val autoRefreshEnabled: Boolean = false,
    val refreshCooldownSeconds: Int = 0,
    val timetableStationQuery: String = "渋谷",
    val timetableResult: ProviderResult<List<StationTimetable>>? = null,
    val isLoadingTimetable: Boolean = false,
    val searchDateYyyyMmDd: String = "20260410"
)

class TransitViewModel(
    private val transitRepository: TransitRepository,
    private val tripRepository: TripRepository,
    private val preferences: AppPreferences,
    private val currentUserId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransitSearchUiState())
    val uiState: StateFlow<TransitSearchUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null
    private var cooldownJob: Job? = null

    init {
        viewModelScope.launch {
            val savedSort = runCatching {
                SortPreference.valueOf(preferences.defaultSort.first())
            }.getOrDefault(SortPreference.FASTEST)
            _uiState.update { it.copy(sortPreference = savedSort) }
            refreshOdptAndSearch()
        }
    }

    fun setActiveTab(tab: TransitSectionTab) {
        _uiState.update { it.copy(activeTab = tab) }
        if (tab == TransitSectionTab.LIVE_POSITIONS && _uiState.value.liveTrainsResult == null) {
            refreshLiveTrains()
        } else if (tab == TransitSectionTab.TIMETABLE && _uiState.value.timetableResult == null) {
            loadStationTimetable()
        }
        if (tab != TransitSectionTab.LIVE_POSITIONS && _uiState.value.autoRefreshEnabled) {
            toggleAutoRefresh(false)
        }
    }

    fun setOperatorSearchQuery(query: String) {
        _uiState.update { it.copy(operatorSearchQuery = query) }
    }

    fun setRegionFilter(region: String) {
        _uiState.update { it.copy(selectedRegionFilter = region) }
    }

    fun selectOperator(operatorId: String) {
        val opLines = NationalRailwayRegistry.linesForOperator(operatorId)
        val firstLine = opLines.firstOrNull()
        _uiState.update {
            it.copy(
                selectedOperatorId = operatorId,
                selectedRailwayId = firstLine?.lineId ?: it.selectedRailwayId,
                liveLineStations = firstLine?.let { line ->
                    NationalRailwayRegistry.stationsForLine(line.lineId)
                } ?: emptyList(),
                selectedTrainId = null
            )
        }
        if (firstLine != null) {
            refreshLiveTrains()
        } else {
            val op = NationalRailwayRegistry.findOperator(operatorId)
            _uiState.update {
                it.copy(
                    liveTrainsResult = ProviderResult.Unsupported(
                        providerName = op?.operatorName ?: operatorId,
                        reason = op?.statusReason ?: "この鉄道事業者のリアルタイム列車位置APIは公開されていません。"
                    )
                )
            }
        }
    }

    fun selectLineForMap(lineId: String) {
        val line = NationalRailwayRegistry.findLine(lineId)
        _uiState.update {
            it.copy(
                selectedRailwayId = lineId,
                selectedOperatorId = line?.operatorId ?: it.selectedOperatorId,
                liveLineStations = NationalRailwayRegistry.stationsForLine(lineId),
                selectedTrainId = null
            )
        }
        refreshLiveTrains()
    }

    fun selectTrain(trainId: String?) {
        _uiState.update { it.copy(selectedTrainId = trainId) }
    }

    fun refreshLiveTrains() {
        viewModelScope.launch {
            val lineId = _uiState.value.selectedRailwayId
            val opId = _uiState.value.selectedOperatorId
            _uiState.update { it.copy(isLoadingTrains = true) }
            val stations = transitRepository.fetchLineStations(lineId)
            val trainsRes = transitRepository.fetchLiveTrainsForLine(lineId)
            val statusRes = transitRepository.fetchOperationStatuses(opId)
            _uiState.update {
                it.copy(
                    liveLineStations = if (stations.isNotEmpty()) stations else it.liveLineStations,
                    liveTrainsResult = trainsRes,
                    operationStatusResult = statusRes,
                    isLoadingTrains = false
                )
            }
            if (trainsRes is ProviderResult.Success && !trainsRes.fromCache) {
                startCooldownTicker(20)
            } else if (trainsRes is ProviderResult.RateLimited) {
                startCooldownTicker(trainsRes.retryAfterSeconds.toInt().coerceIn(5, 60))
            }
        }
    }

    fun toggleAutoRefresh(enabled: Boolean) {
        _uiState.update { it.copy(autoRefreshEnabled = enabled) }
        autoRefreshJob?.cancel()
        if (enabled) {
            autoRefreshJob = viewModelScope.launch {
                while (isActive) {
                    delay(25_000L)
                    if (_uiState.value.autoRefreshEnabled && _uiState.value.activeTab == TransitSectionTab.LIVE_POSITIONS) {
                        refreshLiveTrains()
                    }
                }
            }
        }
    }

    private fun startCooldownTicker(seconds: Int) {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0 && isActive) {
                _uiState.update { it.copy(refreshCooldownSeconds = remaining) }
                delay(1_000L)
                remaining -= 1
            }
            _uiState.update { it.copy(refreshCooldownSeconds = 0) }
        }
    }

    fun setTimetableStationQuery(query: String) {
        _uiState.update { it.copy(timetableStationQuery = query) }
    }

    fun loadStationTimetable(
        stationQuery: String = _uiState.value.timetableStationQuery,
        lineId: String? = null
    ) {
        viewModelScope.launch {
            val q = stationQuery.trim().ifBlank { "渋谷" }
            _uiState.update { it.copy(timetableStationQuery = q, isLoadingTimetable = true) }
            val result = transitRepository.fetchStationTimetable(q, lineId)
            _uiState.update {
                it.copy(
                    timetableResult = result,
                    isLoadingTimetable = false
                )
            }
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
        selectLineForMap(railwayId)
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
            val statuses = transitRepository.fetchOperationStatuses(null)
            _uiState.update {
                it.copy(
                    odptState = odpt,
                    operationStatusResult = statuses
                )
            }
            runRouteQuery(recordRecent = false)
        }
    }

    fun search() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            runRouteQuery(recordRecent = true)
        }
    }

    fun setSearchDate(dateStr: String) {
        _uiState.update { it.copy(searchDateYyyyMmDd = dateStr) }
        search()
    }

    private suspend fun runRouteQuery(recordRecent: Boolean) {
        val s = _uiState.value
        val baseMins = s.hour * 60 + s.minute
        val dateStr = s.searchDateYyyyMmDd.ifBlank { SimpleDateFormat("yyyyMMdd", Locale.JAPAN).format(Date()) }
        val req = RouteSearchRequest(
            origin = s.origin.ifBlank { "東京" },
            destination = s.destination.ifBlank { "京都" },
            dateYyyyMmDd = dateStr,
            timeHhMm = "%02d%02d".format(s.hour, s.minute),
            timeMode = s.timeMode,
            sortPreference = s.sortPreference,
            seatPreference = s.seatPreference,
            enabledModes = s.enabledModes,
            maxTransfers = s.maxTransfers
        )
        val liveStatuses = (s.operationStatusResult as? ProviderResult.Success)?.data.orEmpty()
        val bundle = transitRepository.searchNationwideRoutes(
            request = req,
            baseMinutesOfDay = baseMins,
            odptState = s.odptState,
            liveStatuses = liveStatuses
        )
        _uiState.update {
            it.copy(
                routes = bundle.legacyOptions,
                routeResults = bundle.routeResults,
                routeSourceSummary = bundle.sourceSummary,
                routeProviderNotice = bundle.providerNotice,
                selectedRouteId = bundle.legacyOptions.firstOrNull()?.id,
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
