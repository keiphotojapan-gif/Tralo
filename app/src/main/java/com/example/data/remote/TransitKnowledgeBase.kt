package com.example.data.remote

import com.example.core.PartnerLinks
import com.example.model.CarGuidance
import com.example.model.DataProvenance
import com.example.model.DestinationSpotlight
import com.example.model.SeatPreference
import com.example.model.StationFacilityInfo
import com.example.model.TransitLeg
import com.example.model.TransitRouteOption
import com.example.model.TransportMode
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class HubNode(
    val name: String,
    val aliases: List<String>,
    val region: String,
    val lat: Double,
    val lng: Double,
    val hasShinkansen: Boolean = false,
    val hasAirport: Boolean = false,
    val hasFerry: Boolean = false,
    val facilityInfo: StationFacilityInfo
)

object TransitKnowledgeBase {
    val hubs = listOf(
        HubNode(
            name = "東京",
            aliases = listOf("東京駅", "Tokyo", "とうきょう"),
            region = "首都圏",
            lat = 35.6812,
            lng = 139.7671,
            hasShinkansen = true,
            facilityInfo = StationFacilityInfo(
                stationName = "東京駅",
                supported = true,
                platformSummary = "東海道新幹線 14〜19番線 / 山手線 4・5番線 / 中央線 1・2番線",
                gateSummary = "丸の内中央口・八重洲中央口・日本橋口（新幹線乗換改札直結）",
                barrierFreeNote = "各ホーム中央付近に大型エレベーター・多機能トイレ完備",
                transferCorridorNote = "新幹線乗換は中央乗換口（6〜10号車付近エスカレーター）が最短"
            )
        ),
        HubNode(
            name = "品川",
            aliases = listOf("品川駅", "Shinagawa"),
            region = "首都圏",
            lat = 35.6285,
            lng = 139.7388,
            hasShinkansen = true,
            facilityInfo = StationFacilityInfo(
                stationName = "品川駅",
                supported = true,
                platformSummary = "東海道新幹線 21〜24番線 / 京急線 1・2番線（羽田空港直通）",
                gateSummary = "中央改札・港南口・高輪口",
                barrierFreeNote = "京急連絡改札横にエレベーター・スロープ配置",
                transferCorridorNote = "京急線から新幹線北乗換口へは2号車〜3号車付近が便利"
            )
        ),
        HubNode(
            name = "新宿",
            aliases = listOf("新宿駅", "Shinjuku"),
            region = "首都圏",
            lat = 35.6896,
            lng = 139.7006,
            facilityInfo = StationFacilityInfo(
                stationName = "新宿駅",
                supported = true,
                platformSummary = "JR中央線・山手線・小田急ロマンスカー・バスタ新宿直結",
                gateSummary = "南口・新南改札（バスタ新宿直結）・西口",
                barrierFreeNote = "新南改札〜バスタ新宿4Fはエレベーター直通",
                transferCorridorNote = "小田急ロマンスカーへは南口乗換改札（7〜8号車付近）"
            )
        ),
        HubNode(
            name = "渋谷",
            aliases = listOf("渋谷駅", "Shibuya"),
            region = "首都圏",
            lat = 35.6580,
            lng = 139.7016,
            facilityInfo = StationFacilityInfo(
                stationName = "渋谷駅",
                supported = true,
                platformSummary = "JR山手線 1・2番線 / 東京メトロ銀座線・半蔵門線・副都心線",
                gateSummary = "ハチ公改札・中央改札・宮益坂中央改札",
                barrierFreeNote = "ヒカリエ側アーバンコアに縦動線エレベーター集約",
                transferCorridorNote = "銀座線へはJR山手線渋谷寄り（1〜2号車）エスカレーター利用"
            )
        ),
        HubNode(
            name = "羽田空港",
            aliases = listOf("羽田", "HND", "羽田空港第1・第2ターミナル", "Haneda"),
            region = "首都圏",
            lat = 35.5494,
            lng = 139.7798,
            hasAirport = true,
            facilityInfo = StationFacilityInfo(
                stationName = "羽田空港",
                supported = true,
                platformSummary = "京急空港線・東京モノレール・高速/空港連絡バス各ターミナル",
                gateSummary = "第1・第2・第3ターミナル地下直結改札",
                barrierFreeNote = "全改札〜出発ロビー階まで大型エレベーター・案内カウンター有",
                transferCorridorNote = "モノレール浜松町乗換は先頭車両・京急品川方面は3号車推奨"
            )
        ),
        HubNode(
            name = "成田空港",
            aliases = listOf("成田", "NRT", "Narita"),
            region = "首都圏",
            lat = 35.7720,
            lng = 140.3929,
            hasAirport = true,
            facilityInfo = StationFacilityInfo(
                stationName = "成田空港",
                supported = false,
                unsupportedReason = "構内詳細マッピングは主要ターミナル案内のみ対応"
            )
        ),
        HubNode(
            name = "箱根湯本",
            aliases = listOf("箱根", "Hakone", "箱根湯本駅"),
            region = "箱根・富士",
            lat = 35.2334,
            lng = 139.1039,
            facilityInfo = StationFacilityInfo(
                stationName = "箱根湯本駅",
                supported = true,
                platformSummary = "小田急ロマンスカー 1・2番線 / 箱根登山電車 3・4番線",
                gateSummary = "改札1箇所（登山バス乗場歩道橋直結）",
                barrierFreeNote = "登山電車乗換ホームは同一平面移動可（エレベーター有）",
                transferCorridorNote = "箱根登山線乗換はロマンスカー後方車両側が階段・連絡通路近接"
            )
        ),
        HubNode(
            name = "京都",
            aliases = listOf("京都駅", "Kyoto", "嵐山", "河原町"),
            region = "関西",
            lat = 34.9858,
            lng = 135.7588,
            hasShinkansen = true,
            facilityInfo = StationFacilityInfo(
                stationName = "京都駅",
                supported = true,
                platformSummary = "東海道新幹線 11〜14番線 / JR在来線・嵯峨野線・市営地下鉄烏丸線",
                gateSummary = "新幹線中央口・八条東口・烏丸中央口",
                barrierFreeNote = "中央コンコース・地下鉄連絡通路にエレベーター完備",
                transferCorridorNote = "嵐山方面（嵯峨野線31〜33番線）へは新幹線西口方面（11〜13号車）が便利"
            )
        ),
        HubNode(
            name = "新大阪",
            aliases = listOf("大阪", "Osaka", "新大阪駅", "梅田"),
            region = "関西",
            lat = 34.7335,
            lng = 135.5003,
            hasShinkansen = true,
            hasAirport = true,
            facilityInfo = StationFacilityInfo(
                stationName = "新大阪駅",
                supported = true,
                platformSummary = "新幹線 20〜27番線 / JR京都線・Osaka Metro御堂筋線",
                gateSummary = "新幹線中央口・南口・御堂筋線中改札",
                barrierFreeNote = "全乗り換え通路バリアフリー対応",
                transferCorridorNote = "御堂筋線乗換は新幹線西寄り（1〜4号車側）が最短"
            )
        ),
        HubNode(
            name = "金沢",
            aliases = listOf("金沢駅", "Kanazawa"),
            region = "北陸",
            lat = 36.5781,
            lng = 136.6486,
            hasShinkansen = true,
            facilityInfo = StationFacilityInfo(
                stationName = "金沢駅",
                supported = true,
                platformSummary = "北陸新幹線 11〜14番線 / IRいしかわ鉄道・北鉄バス東口ターミナル",
                gateSummary = "新幹線中央改札・兼六園口（東口）",
                barrierFreeNote = "鼓門側バスロータリーへバリアフリー直結",
                transferCorridorNote = "新幹線中央付近（6〜7号車）が改札正面"
            )
        ),
        HubNode(
            name = "広島",
            aliases = listOf("広島駅", "Hiroshima", "宮島"),
            region = "中国",
            lat = 34.3977,
            lng = 132.4753,
            hasShinkansen = true,
            hasFerry = true,
            facilityInfo = StationFacilityInfo(
                stationName = "広島駅",
                supported = true,
                platformSummary = "山陽新幹線 11〜14番線 / 広島電鉄路面電車・JR山陽本線",
                gateSummary = "新幹線口（北口）・南口（路面電車乗場直結）",
                barrierFreeNote = "ペデストリアンデッキにエレベーター併設",
                transferCorridorNote = "路面電車へは中央階段または南口エレベーター経由"
            )
        ),
        HubNode(
            name = "高松",
            aliases = listOf("高松駅", "Takamatsu", "直島", "瀬戸内"),
            region = "四国・瀬戸内",
            lat = 34.3507,
            lng = 134.0465,
            hasFerry = true,
            facilityInfo = StationFacilityInfo(
                stationName = "高松駅",
                supported = false,
                unsupportedReason = "高松港連絡フェリー桟橋案内のみ対応・構内立体図未対応"
            )
        ),
        HubNode(
            name = "札幌",
            aliases = listOf("札幌駅", "Sapporo", "新千歳"),
            region = "北海道",
            lat = 43.0687,
            lng = 141.3508,
            hasAirport = true,
            facilityInfo = StationFacilityInfo(
                stationName = "札幌駅",
                supported = false,
                unsupportedReason = "JR北海道・地下鉄さっぽろ駅の構内図データは未対応です"
            )
        ),
        HubNode(
            name = "博多",
            aliases = listOf("博多駅", "福岡", "Hakata", "Fukuoka"),
            region = "九州",
            lat = 33.5897,
            lng = 130.4207,
            hasShinkansen = true,
            hasAirport = true,
            facilityInfo = StationFacilityInfo(
                stationName = "博多駅",
                supported = true,
                platformSummary = "山陽・九州新幹線 11〜16番線 / 福岡市地下鉄空港線",
                gateSummary = "博多口・筑紫口・新幹線中央改札",
                barrierFreeNote = "地下鉄空港線までエレベーター乗換可",
                transferCorridorNote = "空港線（福岡空港方面）へは博多口側階段・EVが最短"
            )
        )
    )

    val spotlights = listOf(
        DestinationSpotlight(
            id = "kyoto",
            name = "京都・嵐山",
            subtitleEn = "Kyoto & Arashiyama",
            descriptionJa = "静かな朝の白川筋と嵐山渓谷を巡る、新幹線と路面電車の旅。",
            imageKey = "kyoto",
            latitude = 35.0116,
            longitude = 135.7681,
            recommendedStation = "京都",
            estimatedBudgetJpy = 85000L
        ),
        DestinationSpotlight(
            id = "hakone",
            name = "箱根・富士",
            subtitleEn = "Hakone & Mt. Fuji",
            descriptionJa = "ロマンスカーと登山電車・ロープウェイで抜ける高原リゾート。",
            imageKey = "hakone",
            latitude = 35.2334,
            longitude = 139.1039,
            recommendedStation = "箱根湯本",
            estimatedBudgetJpy = 52000L
        ),
        DestinationSpotlight(
            id = "tokyo",
            name = "東京丸の内",
            subtitleEn = "Tokyo Marunouchi",
            descriptionJa = "赤レンガ駅舎から始める都心メトロ・シェアサイクルの都市散策。",
            imageKey = "tokyo",
            latitude = 35.6812,
            longitude = 139.7671,
            recommendedStation = "東京",
            estimatedBudgetJpy = 38000L
        )
    )

    fun resolveHub(query: String): HubNode? {
        val q = query.trim().lowercase()
        if (q.isBlank()) return null
        return hubs.firstOrNull { hub ->
            hub.name.lowercase() == q ||
                hub.aliases.any { it.lowercase() == q } ||
                q.contains(hub.name.lowercase()) ||
                hub.aliases.any { q.contains(it.lowercase()) }
        }
    }

    fun nearestHub(lat: Double, lng: Double): HubNode {
        return hubs.minByOrNull { distanceKm(lat, lng, it.lat, it.lng) } ?: hubs.first()
    }

    fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun buildRouteCandidates(
        originRaw: String,
        destinationRaw: String,
        baseMinutesOfDay: Int,
        seatPreference: SeatPreference,
        enabledModes: Set<TransportMode>,
        liveLineAlerts: Map<String, String> = emptyMap()
    ): List<TransitRouteOption> {
        val origin = originRaw.trim().ifBlank { "東京" }
        val destination = destinationRaw.trim().ifBlank { "京都" }
        val originHub = resolveHub(origin)
        val destHub = resolveHub(destination)
        val distKm = if (originHub != null && destHub != null) {
            distanceKm(originHub.lat, originHub.lng, destHub.lat, destHub.lng).coerceAtLeast(3.0)
        } else {
            42.0
        }

        val seatSurcharge = when (seatPreference) {
            SeatPreference.GREEN_CAR -> 4800
            SeatPreference.RESERVED -> 850
            else -> 0
        }

        val candidates = mutableListOf<TransitRouteOption>()

        // 1. Primary Rail / Shinkansen + Metro Door-to-Door Candidate
        if (distKm >= 80.0 && (enabledModes.contains(TransportMode.RAIL) || enabledModes.isEmpty())) {
            val shinkansenMins = (distKm / 3.4).roundToInt().coerceIn(35, 300)
            val baseFare = ((distKm * 28.5).roundToInt() / 10) * 10 + seatSurcharge
            val leg1Dep = formatHm(baseMinutesOfDay + 6)
            val leg1Arr = formatHm(baseMinutesOfDay + 6 + 12)
            val leg2Dep = formatHm(baseMinutesOfDay + 6 + 19)
            val leg2Arr = formatHm(baseMinutesOfDay + 6 + 19 + shinkansenMins)
            val leg3Dep = formatHm(baseMinutesOfDay + 6 + 19 + shinkansenMins + 5)
            val leg3Arr = formatHm(baseMinutesOfDay + 6 + 19 + shinkansenMins + 14)

            val yamanoteAlert = liveLineAlerts["odpt.Railway:JR-East.Yamanote"]
            candidates += TransitRouteOption(
                id = "route_rail_fast",
                origin = origin,
                destination = destination,
                departureTime = formatHm(baseMinutesOfDay),
                arrivalTime = leg3Arr,
                totalMinutes = 6 + 12 + 7 + shinkansenMins + 5 + 9,
                totalFareJpy = 180 + baseFare + 220,
                transferCount = 2,
                totalWalkMeters = 420,
                tags = listOf("最速", "新幹線・スマートEX"),
                provenance = if (liveLineAlerts.isNotEmpty()) DataProvenance.ODPT_LIVE else DataProvenance.SCHEDULE_REF,
                delayAlert = yamanoteAlert,
                legs = listOf(
                    TransitLeg(
                        mode = TransportMode.WALK,
                        lineName = "駅まで徒歩",
                        operatorName = "徒歩",
                        departureStation = origin,
                        arrivalStation = originHub?.name ?: "$origin 駅",
                        departureTime = formatHm(baseMinutesOfDay),
                        arrivalTime = leg1Dep,
                        durationMinutes = 6,
                        fareJpy = 0,
                        walkDistanceMeters = 320
                    ),
                    TransitLeg(
                        mode = TransportMode.SUBWAY,
                        lineName = "接続メトロ / JR在来線",
                        operatorName = "JR東日本 / 東京メトロ",
                        odptRailwayId = "odpt.Railway:JR-East.Yamanote",
                        departureStation = originHub?.name ?: "$origin 駅",
                        arrivalStation = if (originHub?.hasShinkansen == true) "${originHub.name}（新幹線ホーム）" else "新幹線乗換駅",
                        departureTime = leg1Dep,
                        arrivalTime = leg1Arr,
                        durationMinutes = 12,
                        fareJpy = 180,
                        walkDistanceMeters = 100,
                        platform = "4番線",
                        carGuidance = CarGuidance(
                            supported = true,
                            formationCars = 11,
                            recommendedCar = "7号車",
                            doorPosition = "2番ドア",
                            facilityNote = "中央新幹線乗換改札エスカレーター目の前",
                            accessibilityNote = "9号車付近に大型エレベーターあり"
                        )
                    ),
                    TransitLeg(
                        mode = TransportMode.RAIL,
                        lineName = if (destHub?.name == "金沢") "北陸新幹線 かがやき" else "東海道・山陽新幹線 のぞみ",
                        operatorName = "JR東海 / JR西日本",
                        departureStation = if (originHub?.hasShinkansen == true) originHub.name else "東京",
                        arrivalStation = destHub?.name ?: destination,
                        departureTime = leg2Dep,
                        arrivalTime = leg2Arr,
                        durationMinutes = shinkansenMins,
                        fareJpy = baseFare,
                        platform = "16番線",
                        carGuidance = CarGuidance(
                            supported = true,
                            formationCars = 16,
                            recommendedCar = if (seatPreference == SeatPreference.GREEN_CAR) "8・9・10号車（グリーン）" else "7号車・11号車",
                            doorPosition = "乗降口中央",
                            facilityNote = "多目的室(11号車)・特大荷物スペースつき座席対応",
                            accessibilityNote = "11号車に車椅子対応座席および多機能トイレ完備"
                        ),
                        bookingPartnerName = "JR予約案内",
                        bookingUrl = "https://smart-ex.jp/"
                    ),
                    TransitLeg(
                        mode = TransportMode.LOCAL_BUS,
                        lineName = "到着地接続バス / 在来線",
                        operatorName = "地域交通",
                        departureStation = destHub?.name ?: destination,
                        arrivalStation = destination,
                        departureTime = leg3Dep,
                        arrivalTime = leg3Arr,
                        durationMinutes = 9,
                        fareJpy = 220,
                        carGuidance = CarGuidance(
                            supported = false,
                            unsupportedReason = "地域路線バスは号車・乗車位置案内未対応です"
                        )
                    )
                )
            )
        } else {
            // Urban / Regional Transit Candidate
            val metroMins = (distKm * 2.2).roundToInt().coerceIn(14, 110)
            val fare = ((distKm * 18.0).roundToInt().coerceIn(180, 3200) / 10) * 10 + (if (seatPreference == SeatPreference.RESERVED) 550 else 0)
            val arrTime = formatHm(baseMinutesOfDay + 5 + metroMins)
            candidates += TransitRouteOption(
                id = "route_metro_direct",
                origin = origin,
                destination = destination,
                departureTime = formatHm(baseMinutesOfDay),
                arrivalTime = arrTime,
                totalMinutes = 5 + metroMins,
                totalFareJpy = fare,
                transferCount = if (distKm > 25) 1 else 0,
                totalWalkMeters = 350,
                tags = listOf("乗換少", "標準ルート"),
                provenance = if (liveLineAlerts.isNotEmpty()) DataProvenance.ODPT_LIVE else DataProvenance.SCHEDULE_REF,
                delayAlert = liveLineAlerts.values.firstOrNull(),
                legs = listOf(
                    TransitLeg(
                        mode = TransportMode.WALK,
                        lineName = "最寄り駅へ徒歩",
                        operatorName = "徒歩",
                        departureStation = origin,
                        arrivalStation = originHub?.name ?: origin,
                        departureTime = formatHm(baseMinutesOfDay),
                        arrivalTime = formatHm(baseMinutesOfDay + 5),
                        durationMinutes = 5,
                        fareJpy = 0,
                        walkDistanceMeters = 350
                    ),
                    TransitLeg(
                        mode = if (destHub?.name == "箱根湯本") TransportMode.RAIL else TransportMode.SUBWAY,
                        lineName = if (destHub?.name == "箱根湯本") "小田急ロマンスカー" else "東京メトロ / JR直通快速",
                        operatorName = if (destHub?.name == "箱根湯本") "小田急電鉄" else "東京メトロ・JR東日本",
                        odptRailwayId = "odpt.Railway:TokyoMetro.Ginza",
                        departureStation = originHub?.name ?: origin,
                        arrivalStation = destination,
                        departureTime = formatHm(baseMinutesOfDay + 5),
                        arrivalTime = arrTime,
                        durationMinutes = metroMins,
                        fareJpy = fare,
                        platform = "2番線",
                        carGuidance = CarGuidance(
                            supported = true,
                            formationCars = 10,
                            recommendedCar = "4号車（女性専用車両は1号車・平日朝）",
                            doorPosition = "3番ドア",
                            facilityNote = "到着駅の出口階段・改札付近",
                            accessibilityNote = "7号車付近にエレベーター設置"
                        )
                    )
                )
            )
        }

        // 2. Budget Highway Bus / Ferry Option
        if (enabledModes.contains(TransportMode.HIGHWAY_BUS) || enabledModes.contains(TransportMode.LOCAL_BUS) || enabledModes.contains(TransportMode.FERRY) || enabledModes.isEmpty()) {
            val isFerry = (originHub?.hasFerry == true || destHub?.hasFerry == true) && distKm > 40
            val busMins = (distKm * 1.15).roundToInt().coerceIn(28, 540)
            val busFare = ((distKm * 9.5).roundToInt().coerceIn(220, 7800) / 10) * 10
            val dep = formatHm(baseMinutesOfDay + 12)
            val arr = formatHm(baseMinutesOfDay + 12 + busMins)
            candidates += TransitRouteOption(
                id = if (isFerry) "route_ferry_economy" else "route_bus_economy",
                origin = origin,
                destination = destination,
                departureTime = formatHm(baseMinutesOfDay),
                arrivalTime = arr,
                totalMinutes = 12 + busMins,
                totalFareJpy = busFare,
                transferCount = 0,
                totalWalkMeters = 450,
                tags = if (isFerry) listOf("フェリー連携", "最安") else listOf("直通バス", "最安"),
                provenance = DataProvenance.SCHEDULE_REF,
                legs = listOf(
                    TransitLeg(
                        mode = TransportMode.WALK,
                        lineName = "バスターミナル / 港へ移動",
                        operatorName = "徒歩",
                        departureStation = origin,
                        arrivalStation = "$origin バスターミナル",
                        departureTime = formatHm(baseMinutesOfDay),
                        arrivalTime = dep,
                        durationMinutes = 12,
                        fareJpy = 0,
                        walkDistanceMeters = 450
                    ),
                    TransitLeg(
                        mode = if (isFerry) TransportMode.FERRY else if (distKm > 45) TransportMode.HIGHWAY_BUS else TransportMode.LOCAL_BUS,
                        lineName = if (isFerry) "瀬戸内高速船・フェリー" else if (distKm > 45) "TRALO 連携高速バス（予約制）" else "都営・路線バス",
                        operatorName = if (isFerry) "旅客船事業者" else "高速バス共同運行",
                        departureStation = "$origin バスターミナル",
                        arrivalStation = destination,
                        departureTime = dep,
                        arrivalTime = arr,
                        durationMinutes = busMins,
                        fareJpy = busFare,
                        platform = "A3のりば",
                        carGuidance = CarGuidance(
                            supported = false,
                            unsupportedReason = "バス・船舶便のため車両号車案内は対象外です"
                        ),
                        bookingPartnerName = "Klook / 予約リンク",
                        bookingUrl = PartnerLinks.klookSearchUrl("$destination 交通パス")
                    )
                )
            )
        }

        // 3. Air + Airport Limousine / Express Option for long haul (>= 250 km)
        if (distKm >= 250.0 && (enabledModes.contains(TransportMode.FLIGHT) || enabledModes.contains(TransportMode.AIRPORT_BUS) || enabledModes.isEmpty())) {
            val flightMins = (distKm / 9.5).roundToInt().coerceIn(55, 130)
            val totalMins = 32 + flightMins + 28
            val flightFare = ((distKm * 22.0).roundToInt().coerceIn(9800, 34000) / 100) * 100
            candidates += TransitRouteOption(
                id = "route_air_airportbus",
                origin = origin,
                destination = destination,
                departureTime = formatHm(baseMinutesOfDay),
                arrivalTime = formatHm(baseMinutesOfDay + totalMins),
                totalMinutes = totalMins,
                totalFareJpy = 950 + flightFare + 1100,
                transferCount = 2,
                totalWalkMeters = 520,
                tags = listOf("空路＋連絡バス", "長距離"),
                provenance = DataProvenance.ESTIMATE_ONLY,
                reliabilityNote = "航空便は発着空港の保安検査時間を含めた目安です",
                legs = listOf(
                    TransitLeg(
                        mode = TransportMode.AIRPORT_BUS,
                        lineName = "空港連絡リムジンバス",
                        operatorName = "空港リムジン",
                        departureStation = origin,
                        arrivalStation = "国内線空港ターミナル",
                        departureTime = formatHm(baseMinutesOfDay),
                        arrivalTime = formatHm(baseMinutesOfDay + 32),
                        durationMinutes = 32,
                        fareJpy = 950,
                        walkDistanceMeters = 180,
                        platform = "14番のりば",
                        carGuidance = CarGuidance(supported = false, unsupportedReason = "リムジンバスは号車案内対象外です")
                    ),
                    TransitLeg(
                        mode = TransportMode.FLIGHT,
                        lineName = "国内線航空便",
                        operatorName = "ANA / JAL",
                        departureStation = "出発空港",
                        arrivalStation = "到着空港",
                        departureTime = formatHm(baseMinutesOfDay + 32),
                        arrivalTime = formatHm(baseMinutesOfDay + 32 + flightMins),
                        durationMinutes = flightMins,
                        fareJpy = flightFare,
                        walkDistanceMeters = 240,
                        platform = "搭乗ゲート未定",
                        carGuidance = CarGuidance(supported = false, unsupportedReason = "航空機の座席指定は航空会社サイトで確認してください")
                    ),
                    TransitLeg(
                        mode = TransportMode.RAIL,
                        lineName = "空港アクセス特急",
                        operatorName = "鉄道事業者",
                        departureStation = "到着空港駅",
                        arrivalStation = destination,
                        departureTime = formatHm(baseMinutesOfDay + 32 + flightMins),
                        arrivalTime = formatHm(baseMinutesOfDay + totalMins),
                        durationMinutes = 28,
                        fareJpy = 1100,
                        walkDistanceMeters = 100
                    )
                )
            )
        }

        // 4. Short-Distance Share-Cycle / Taxi / Tram Option (<= 60 km)
        if (distKm <= 60.0) {
            val cycleMins = (distKm * 4.1).roundToInt().coerceIn(12, 95)
            val taxiFare = (500 + distKm * 340).roundToInt()
            candidates += TransitRouteOption(
                id = "route_cycle_taxi",
                origin = origin,
                destination = destination,
                departureTime = formatHm(baseMinutesOfDay),
                arrivalTime = formatHm(baseMinutesOfDay + cycleMins),
                totalMinutes = cycleMins,
                totalFareJpy = 350,
                transferCount = 0,
                totalWalkMeters = 90,
                tags = listOf("徒歩最少", "シェアサイクル / タクシー目安 ¥$taxiFare"),
                provenance = DataProvenance.ESTIMATE_ONLY,
                reliabilityNote = "シェアサイクル移動（タクシー利用時は約¥$taxiFare）",
                legs = listOf(
                    TransitLeg(
                        mode = TransportMode.SHARE_CYCLE,
                        lineName = "シェアサイクル（ポート直行）",
                        operatorName = "地域シェアモビリティ",
                        departureStation = "$origin ポート",
                        arrivalStation = "$destination ポート",
                        departureTime = formatHm(baseMinutesOfDay),
                        arrivalTime = formatHm(baseMinutesOfDay + cycleMins),
                        durationMinutes = cycleMins,
                        fareJpy = 350,
                        walkDistanceMeters = 90,
                        carGuidance = CarGuidance(supported = false, unsupportedReason = "シェアサイクルのため該当なし")
                    )
                )
            )
        }

        return candidates
    }

    fun formatHm(totalMinutes: Int): String {
        val m = ((totalMinutes % 1440) + 1440) % 1440
        val h = m / 60
        val min = m % 60
        return "%02d:%02d".format(h, min)
    }
}
