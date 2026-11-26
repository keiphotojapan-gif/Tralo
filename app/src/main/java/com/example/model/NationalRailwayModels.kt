package com.example.model

enum class ProviderCapability(val labelJa: String) {
    TRAIN_POSITIONS("列車走行位置"),
    OPERATION_STATUS("運行情報"),
    STATION_TIMETABLE("駅時刻表"),
    ROUTE_SEARCH("経路検索"),
    REALTIME_TRIP("便リアルタイム")
}

enum class CoverageStatus(val labelJa: String) {
    IMPLEMENTED("実装済み"),
    AVAILABLE("利用可能（一部未連携）"),
    AUTH_REQUIRED("APIキー登録が必要"),
    CONTRACT_REQUIRED("法人契約が必要"),
    OPERATOR_PERMISSION_REQUIRED("事業者許諾が必要"),
    LIMITED("一部路線・条件のみ対応"),
    UNAVAILABLE("公式API非公開・未対応"),
    INVESTIGATING("調査中")
}

data class LatLngPoint(
    val latitude: Double,
    val longitude: Double
)

data class RailwayOperator(
    val operatorId: String,
    val operatorName: String,
    val operatorCode: String,
    val country: String = "JP",
    val supportedRegions: List<String>,
    val apiProvider: String,
    val capabilities: Set<ProviderCapability>,
    val coverageStatus: CoverageStatus,
    val statusReason: String = "",
    val odptOperatorId: String? = null,
    val officialInfoUrl: String? = null
)

data class RailwayStation(
    val stationId: String,
    val stationName: String,
    val stationCode: String = "",
    val latitude: Double,
    val longitude: Double,
    val operatorIds: List<String> = emptyList(),
    val lineIds: List<String> = emptyList()
)

data class RailwayLine(
    val lineId: String,
    val operatorId: String,
    val lineName: String,
    val lineCode: String = "",
    val lineColorHex: String = "#1B4332",
    val stationIds: List<String> = emptyList(),
    val routeGeometry: List<LatLngPoint> = emptyList(),
    val realtimeSupported: Boolean = false,
    val timetableSupported: Boolean = false,
    val coverageStatus: CoverageStatus = CoverageStatus.UNAVAILABLE,
    val unsupportedReason: String? = null
)

data class TrainPosition(
    val trainId: String,
    val latitude: Double,
    val longitude: Double,
    val bearing: Float,
    val currentStation: String,
    val nextStation: String,
    val progress: Float,
    val delayMinutes: Int,
    val timestamp: String,
    val dataSource: String,
    val confidence: Float
)

data class Train(
    val trainId: String,
    val tripId: String? = null,
    val operatorId: String,
    val lineId: String,
    val trainNumber: String,
    val trainType: String,
    val destination: String,
    val direction: String,
    val currentStation: String,
    val nextStation: String,
    val scheduledDeparture: String? = null,
    val scheduledArrival: String? = null,
    val actualPosition: TrainPosition? = null,
    val delayMinutes: Int = 0,
    val realtimeStatus: String = "平常",
    val lastUpdated: String
)

data class TrainOperationStatus(
    val operatorId: String,
    val lineId: String,
    val lineName: String,
    val status: String,
    val delayMinutes: Int,
    val disruptionType: String,
    val message: String,
    val updatedAt: String,
    val source: String,
    val isDelayed: Boolean = false
)

data class StationTimetable(
    val stationId: String,
    val stationName: String,
    val lineId: String,
    val direction: String,
    val departureTime: String,
    val trainType: String,
    val destination: String,
    val platform: String = "",
    val trainNumber: String = "",
    val calendarType: String = "平日",
    val source: String = ""
)

data class RouteLegDetail(
    val mode: TransportMode,
    val lineName: String,
    val operatorName: String,
    val odptRailwayId: String? = null,
    val tripCode: String? = null,
    val trainType: String = "",
    val departureStation: String,
    val arrivalStation: String,
    val transferStation: String? = null,
    val departureTime: String,
    val arrivalTime: String,
    val predictedDepartureTime: String? = null,
    val predictedArrivalTime: String? = null,
    val durationMinutes: Int,
    val fareJpy: Int,
    val walkDistanceMeters: Int = 0,
    val platform: String = "",
    val operationStatus: String? = null,
    val delayMinutes: Int = 0,
    val transferFeasible: Boolean = true,
    val carGuidance: CarGuidance = CarGuidance(
        supported = false,
        unsupportedReason = "号車・扉位置データは未対応の路線です"
    ),
    val bookingPartnerName: String? = null,
    val bookingUrl: String? = null
)

data class RouteResult(
    val id: String,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val arrivalTime: String,
    val durationMinutes: Int,
    val fare: Int,
    val transferCount: Int,
    val walkMeters: Int = 0,
    val legs: List<RouteLegDetail>,
    val realtimeStatus: String,
    val dataSources: List<String>,
    val isRealtimeEnriched: Boolean = false,
    val tags: List<String> = emptyList(),
    val alternativeSuggested: Boolean = false,
    val reliabilityNote: String = ""
)

data class RouteSearchRequest(
    val origin: String,
    val destination: String,
    val dateYyyyMmDd: String,
    val timeHhMm: String,
    val timeMode: TimeQueryMode = TimeQueryMode.DEPART_AT,
    val sortPreference: SortPreference = SortPreference.FASTEST,
    val seatPreference: SeatPreference = SeatPreference.ANY,
    val enabledModes: Set<TransportMode> = TransportMode.entries.toSet(),
    val maxTransfers: Int? = null
)

sealed interface ProviderResult<out T> {
    data class Success<T>(
        val data: T,
        val sourceName: String,
        val updatedAt: String,
        val fromCache: Boolean = false,
        val isStale: Boolean = false
    ) : ProviderResult<T>

    data class NotConfigured(
        val providerName: String,
        val reason: String
    ) : ProviderResult<Nothing>

    data class Unsupported(
        val providerName: String,
        val reason: String
    ) : ProviderResult<Nothing>

    data class RateLimited(
        val providerName: String,
        val retryAfterSeconds: Long,
        val cachedFallback: @UnsafeVariance Any? = null
    ) : ProviderResult<Nothing>

    data class Error(
        val providerName: String,
        val message: String,
        val isAuthError: Boolean = false
    ) : ProviderResult<Nothing>
}
