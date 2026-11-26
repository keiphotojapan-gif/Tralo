package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AppPreferences
import com.example.data.remote.TransitKnowledgeBase
import com.example.data.repository.TripRepository
import com.example.data.repository.WeatherRepository
import com.example.model.BookingStatus
import com.example.model.ChecklistItem
import com.example.model.ItineraryCategory
import com.example.model.ItineraryItem
import com.example.model.OdptApiState
import com.example.model.SavedRoute
import com.example.model.TransitRouteOption
import com.example.model.Trip
import com.example.model.TripStatus
import com.example.model.WeatherForecast
import com.example.data.repository.TransitRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "TripsVM"

@OptIn(ExperimentalCoroutinesApi::class)
class HomeAndTripsViewModel(
    private val tripRepository: TripRepository,
    private val weatherRepository: WeatherRepository,
    private val transitRepository: TransitRepository,
    private val preferences: AppPreferences,
    private val currentUserId: String
) : ViewModel() {

    val tripsState: StateFlow<UiState<List<Trip>>> = tripRepository.observeTrips(currentUserId)
        .map<List<Trip>, UiState<List<Trip>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing trips", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "旅程の読み込みに失敗しました。"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val savedRoutesState: StateFlow<UiState<List<SavedRoute>>> = tripRepository.observeSavedRoutes(currentUserId)
        .map<List<SavedRoute>, UiState<List<SavedRoute>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing saved routes", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "保存済み経路の読み込みに失敗しました。"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val recentSearches: StateFlow<List<Pair<String, String>>> = preferences.recentSearches
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _selectedTripId = MutableStateFlow<String?>(null)
    val activeTrip: StateFlow<Trip?> = combine(tripsState, _selectedTripId) { state, selectedId ->
        val list = (state as? UiState.Success)?.data.orEmpty()
        list.firstOrNull { it.id == selectedId }
            ?: list.firstOrNull { it.statusEnum == TripStatus.ACTIVE }
            ?: list.firstOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = null
    )

    val activeTripItemsState: StateFlow<UiState<List<ItineraryItem>>> = activeTrip
        .flatMapLatest { trip ->
            if (trip == null || trip.id.isBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                tripRepository.observeTripItems(currentUserId, trip.id)
                    .map<List<ItineraryItem>, UiState<List<ItineraryItem>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "予定の取得に失敗しました。"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val activeChecklistState: StateFlow<UiState<List<ChecklistItem>>> = activeTrip
        .flatMapLatest { trip ->
            if (trip == null || trip.id.isBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                tripRepository.observeChecklist(currentUserId, trip.id)
                    .map<List<ChecklistItem>, UiState<List<ChecklistItem>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "チェックリストの取得に失敗しました。"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _weatherState = MutableStateFlow<UiState<WeatherForecast>>(UiState.Loading)
    val weatherState: StateFlow<UiState<WeatherForecast>> = _weatherState.asStateFlow()

    private val _odptSummaryState = MutableStateFlow<OdptApiState>(OdptApiState.Loading)
    val odptSummaryState: StateFlow<OdptApiState> = _odptSummaryState.asStateFlow()

    private val _actionBanner = MutableStateFlow<String?>(null)
    val actionBanner: StateFlow<String?> = _actionBanner.asStateFlow()

    init {
        refreshWeather("東京", 35.6812, 139.7671)
        refreshOdptSummary()
        viewModelScope.launch {
            activeTrip.collect { trip ->
                if (trip != null) {
                    refreshWeather(trip.destination, trip.latitude, trip.longitude)
                }
            }
        }
    }

    fun selectTrip(tripId: String) {
        _selectedTripId.value = tripId
    }

    fun clearBanner() {
        _actionBanner.value = null
    }

    fun refreshWeather(locationName: String, lat: Double, lng: Double, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _weatherState.value = UiState.Loading
            weatherRepository.getWeather(locationName, lat, lng, forceRefresh)
                .onSuccess { _weatherState.value = UiState.Success(it) }
                .onFailure { _weatherState.value = UiState.Error(it.localizedMessage ?: "天気情報の取得に失敗しました。") }
        }
    }

    fun refreshOdptSummary() {
        viewModelScope.launch {
            _odptSummaryState.value = OdptApiState.Loading
            _odptSummaryState.value = transitRepository.fetchLiveOdptStatus()
        }
    }

    fun createTrip(
        title: String,
        destination: String,
        startDate: String,
        endDate: String,
        companions: String,
        budgetJpy: Long
    ) {
        viewModelScope.launch {
            val hub = TransitKnowledgeBase.resolveHub(destination)
            val coverKey = when {
                destination.contains("京都") -> "kyoto"
                destination.contains("箱根") || destination.contains("富士") -> "hakone"
                else -> "tokyo"
            }
            tripRepository.createTrip(
                userId = currentUserId,
                title = title,
                destination = destination,
                startDate = startDate,
                endDate = endDate,
                companions = companions,
                budgetJpy = budgetJpy,
                status = TripStatus.PLANNING,
                coverKey = coverKey,
                latitude = hub?.lat ?: 35.0116,
                longitude = hub?.lng ?: 135.7681
            ).onSuccess { newId ->
                _selectedTripId.value = newId
                _actionBanner.value = "旅程「$title」をクラウドに保存しました。"
            }.onFailure { err ->
                _actionBanner.value = err.localizedMessage ?: "旅程の作成に失敗しました。"
            }
        }
    }

    fun createKyotoStarterTrip() {
        viewModelScope.launch {
            tripRepository.createKyotoStarterTrip(currentUserId)
                .onSuccess { id ->
                    _selectedTripId.value = id
                    _actionBanner.value = "京都・嵐山モデル旅程を作成しました。"
                }
                .onFailure { err ->
                    _actionBanner.value = err.localizedMessage ?: "サンプル旅程の作成に失敗しました。"
                }
        }
    }

    fun setTripStatus(trip: Trip, newStatus: TripStatus) {
        viewModelScope.launch {
            tripRepository.updateTripStatusAndSpent(currentUserId, trip.id, newStatus, trip.spentJpy)
                .onSuccess { _actionBanner.value = "旅程ステータスを「${newStatus.labelJa}」に更新しました。" }
                .onFailure { _actionBanner.value = it.localizedMessage ?: "更新に失敗しました。" }
        }
    }

    fun deleteTrip(tripId: String) {
        viewModelScope.launch {
            tripRepository.deleteTrip(currentUserId, tripId)
                .onSuccess { _actionBanner.value = "旅程を削除しました。" }
                .onFailure { _actionBanner.value = it.localizedMessage ?: "削除に失敗しました。" }
        }
    }

    fun addItineraryItem(
        tripId: String,
        dayIndex: Long,
        startTime: String,
        title: String,
        category: ItineraryCategory,
        locationName: String,
        lineName: String? = null,
        platform: String? = null,
        carNumber: String? = null,
        seatNumber: String? = null,
        providerName: String? = null,
        bookingRef: String? = null,
        bookingUrl: String? = null,
        costJpy: Long = 0L,
        notes: String? = null,
        bookingStatus: BookingStatus = BookingStatus.PLANNED
    ) {
        viewModelScope.launch {
            tripRepository.addItineraryItem(
                userId = currentUserId,
                tripId = tripId,
                dayIndex = dayIndex,
                startTime = startTime,
                endTime = null,
                title = title,
                category = category,
                locationName = locationName,
                lineName = lineName,
                platform = platform,
                carNumber = carNumber,
                seatNumber = seatNumber,
                providerName = providerName,
                bookingRef = bookingRef,
                bookingUrl = bookingUrl,
                costJpy = costJpy,
                notes = notes,
                bookingStatus = bookingStatus
            ).onSuccess {
                val trip = activeTrip.value
                if (trip != null && costJpy > 0L) {
                    tripRepository.updateTripStatusAndSpent(
                        currentUserId,
                        trip.id,
                        trip.statusEnum,
                        trip.spentJpy + costJpy
                    )
                }
                _actionBanner.value = "予定「$title」を追加しました。"
            }.onFailure {
                _actionBanner.value = it.localizedMessage ?: "予定の追加に失敗しました。"
            }
        }
    }

    fun addRouteToActiveTrip(option: TransitRouteOption) {
        viewModelScope.launch {
            val trip = activeTrip.value
            if (trip == null) {
                _actionBanner.value = "先に旅程画面から旅行プランを作成してください。"
                return@launch
            }
            val mainLeg = option.legs.firstOrNull { it.mode.name != "WALK" } ?: option.legs.firstOrNull()
            tripRepository.addItineraryItem(
                userId = currentUserId,
                tripId = trip.id,
                dayIndex = 1L,
                startTime = option.departureTime,
                endTime = option.arrivalTime,
                title = "${option.origin} → ${option.destination}",
                category = ItineraryCategory.TRANSIT,
                locationName = mainLeg?.departureStation ?: option.origin,
                lineName = mainLeg?.lineName,
                platform = mainLeg?.platform?.ifBlank { null },
                carNumber = mainLeg?.carGuidance?.recommendedCar?.ifBlank { null },
                costJpy = option.totalFareJpy.toLong(),
                notes = "所要${option.totalMinutes}分 / 乗換${option.transferCount}回",
                bookingUrl = mainLeg?.bookingUrl
            ).onSuccess {
                _actionBanner.value = "経路を「${trip.title}」へ追加しました。"
            }.onFailure {
                _actionBanner.value = it.localizedMessage ?: "経路の追加に失敗しました。"
            }
        }
    }

    fun updateBookingStatus(
        tripId: String,
        itemId: String,
        status: BookingStatus,
        bookingRef: String? = null
    ) {
        viewModelScope.launch {
            tripRepository.updateItemBookingStatus(currentUserId, tripId, itemId, status, bookingRef)
                .onSuccess { _actionBanner.value = "予約ステータスを更新しました。" }
                .onFailure { _actionBanner.value = it.localizedMessage ?: "更新に失敗しました。" }
        }
    }

    fun deleteItineraryItem(tripId: String, itemId: String) {
        viewModelScope.launch {
            tripRepository.deleteItineraryItem(currentUserId, tripId, itemId)
                .onSuccess { _actionBanner.value = "予定を削除しました。" }
                .onFailure { _actionBanner.value = it.localizedMessage ?: "削除に失敗しました。" }
        }
    }

    fun addChecklistItem(tripId: String, label: String, category: String = "持ち物") {
        viewModelScope.launch {
            tripRepository.addChecklistItem(currentUserId, tripId, label, category)
                .onFailure { _actionBanner.value = it.localizedMessage ?: "追加に失敗しました。" }
        }
    }

    fun toggleChecklistItem(tripId: String, checkId: String, checked: Boolean) {
        viewModelScope.launch {
            tripRepository.toggleChecklistItem(currentUserId, tripId, checkId, checked)
                .onFailure { _actionBanner.value = it.localizedMessage ?: "更新に失敗しました。" }
        }
    }

    fun deleteChecklistItem(tripId: String, checkId: String) {
        viewModelScope.launch {
            tripRepository.deleteChecklistItem(currentUserId, tripId, checkId)
                .onFailure { _actionBanner.value = it.localizedMessage ?: "削除に失敗しました。" }
        }
    }

    fun toggleFavoriteRoute(route: SavedRoute) {
        viewModelScope.launch {
            tripRepository.toggleFavoriteRoute(currentUserId, route.id, !route.favorite)
        }
    }

    fun deleteSavedRoute(routeId: String) {
        viewModelScope.launch {
            tripRepository.deleteSavedRoute(currentUserId, routeId)
        }
    }
}
