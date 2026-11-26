package com.example.model

enum class TransportMode(val labelJa: String, val shortLabel: String) {
    RAIL("鉄道・新幹線", "鉄道"),
    SUBWAY("地下鉄", "地下鉄"),
    TRAM("路面電車", "市電"),
    LOCAL_BUS("路線バス", "バス"),
    HIGHWAY_BUS("高速バス", "高速バス"),
    AIRPORT_BUS("空港連絡バス", "連絡バス"),
    FLIGHT("航空機", "空路"),
    FERRY("フェリー", "船"),
    WALK("徒歩", "徒歩"),
    BICYCLE("自転車", "自転車"),
    SHARE_CYCLE("シェアサイクル", "シェア自転車"),
    TAXI("タクシー", "タクシー")
}

enum class SortPreference(val labelJa: String) {
    FASTEST("所要時間優先"),
    CHEAPEST("料金優先"),
    FEWEST_TRANSFERS("乗換回数優先"),
    LEAST_WALKING("徒歩距離優先")
}

enum class SeatPreference(val labelJa: String) {
    ANY("指定なし"),
    RESERVED("指定席"),
    GREEN_CAR("グリーン車"),
    NON_RESERVED("自由席")
}

enum class TimeQueryMode(val labelJa: String) {
    DEPART_AT("出発時刻"),
    ARRIVE_BY("到着時刻"),
    FIRST_TRAIN("始発"),
    LAST_TRAIN("終電")
}

enum class DataProvenance(val labelJa: String) {
    ODPT_LIVE("ODPT Live"),
    SCHEDULE_REF("標準ダイヤ参照"),
    ESTIMATE_ONLY("概算ルート")
}

data class CarGuidance(
    val supported: Boolean,
    val formationCars: Int = 0,
    val recommendedCar: String = "",
    val doorPosition: String = "",
    val facilityNote: String = "",
    val accessibilityNote: String = "",
    val unsupportedReason: String? = null
)

data class StationFacilityInfo(
    val stationName: String,
    val supported: Boolean,
    val platformSummary: String = "",
    val gateSummary: String = "",
    val barrierFreeNote: String = "",
    val transferCorridorNote: String = "",
    val unsupportedReason: String? = null
)

data class TransitLeg(
    val mode: TransportMode,
    val lineName: String,
    val operatorName: String,
    val odptRailwayId: String? = null,
    val departureStation: String,
    val arrivalStation: String,
    val departureTime: String,
    val arrivalTime: String,
    val durationMinutes: Int,
    val fareJpy: Int,
    val walkDistanceMeters: Int = 0,
    val platform: String = "",
    val carGuidance: CarGuidance = CarGuidance(
        supported = false,
        unsupportedReason = "号車・扉位置データは未対応の路線です"
    ),
    val bookingPartnerName: String? = null,
    val bookingUrl: String? = null
)

data class TransitRouteOption(
    val id: String,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val arrivalTime: String,
    val totalMinutes: Int,
    val totalFareJpy: Int,
    val transferCount: Int,
    val totalWalkMeters: Int,
    val legs: List<TransitLeg>,
    val tags: List<String>,
    val provenance: DataProvenance,
    val delayAlert: String? = null,
    val reliabilityNote: String = "標準ダイヤおよび事業者公開データに基づく経路候補"
)

sealed interface OdptApiState {
    data object Idle : OdptApiState
    data object Loading : OdptApiState
    data object NotConfigured : OdptApiState
    data class Available(
        val trainInfos: List<OdptLineStatus>,
        val liveTrains: List<OdptTrainLocation>,
        val updatedAt: String
    ) : OdptApiState
    data class Error(val message: String) : OdptApiState
}

data class OdptLineStatus(
    val railwayId: String,
    val railwayTitle: String,
    val operatorTitle: String,
    val statusText: String,
    val infoText: String,
    val isDelayed: Boolean,
    val updatedAt: String
)

data class OdptTrainLocation(
    val trainNumber: String,
    val railwayId: String,
    val trainType: String,
    val destinationStation: String,
    val fromStation: String,
    val toStation: String,
    val delayMinutes: Int,
    val direction: String,
    val updatedAt: String
)

data class DestinationSpotlight(
    val id: String,
    val name: String,
    val subtitleEn: String,
    val descriptionJa: String,
    val imageKey: String,
    val latitude: Double,
    val longitude: Double,
    val recommendedStation: String,
    val estimatedBudgetJpy: Long
)
