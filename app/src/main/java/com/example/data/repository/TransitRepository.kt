package com.example.data.repository

import com.example.core.TransitApiConfigs
import com.example.data.provider.EkispertProvider
import com.example.data.provider.GtfsRealtimeProvider
import com.example.data.provider.JorudanProvider
import com.example.data.provider.JrShikokuRealtimeAdapter
import com.example.data.provider.JrTrainformationAdapter
import com.example.data.provider.MixwayRealtimeProvider
import com.example.data.provider.NavitimeProvider
import com.example.data.provider.OdptRealtimeProvider
import com.example.data.provider.OperatorOfficialProvider
import com.example.data.remote.NationalRailwayRegistry
import com.example.data.remote.NetworkModule
import com.example.data.remote.OdptApi
import com.example.data.remote.TransitKnowledgeBase
import com.example.model.DataProvenance
import com.example.model.OdptApiState
import com.example.model.OdptLineStatus
import com.example.model.OdptTrainLocation
import com.example.model.ProviderCapability
import com.example.model.ProviderResult
import com.example.model.RailwayStation
import com.example.model.RouteLegDetail
import com.example.model.RouteResult
import com.example.model.RouteSearchRequest
import com.example.model.SeatPreference
import com.example.model.SortPreference
import com.example.model.StationFacilityInfo
import com.example.model.StationTimetable
import com.example.model.Train
import com.example.model.TrainOperationStatus
import com.example.model.TransitLeg
import com.example.model.TransitRouteOption
import com.example.model.TransportMode

data class RouteSearchBundle(
    val legacyOptions: List<TransitRouteOption>,
    val routeResults: List<RouteResult>,
    val sourceSummary: String,
    val providerNotice: String? = null
)

class TransitRepository(
    private val odptApi: OdptApi = NetworkModule.odptApi,
    private val odptProvider: OdptRealtimeProvider = OdptRealtimeProvider(odptApi = odptApi),
    private val ekispertProvider: EkispertProvider = EkispertProvider(),
    private val mixwayProvider: MixwayRealtimeProvider = MixwayRealtimeProvider(),
    private val jorudanProvider: JorudanProvider = JorudanProvider(),
    private val navitimeProvider: NavitimeProvider = NavitimeProvider(),
    private val officialProvider: OperatorOfficialProvider = OperatorOfficialProvider(),
    private val gtfsProvider: GtfsRealtimeProvider = GtfsRealtimeProvider(),
    private val jrTrainformationAdapter: JrTrainformationAdapter = JrTrainformationAdapter(),
    private val jrShikokuAdapter: JrShikokuRealtimeAdapter = JrShikokuRealtimeAdapter()
) {
    suspend fun fetchLiveOdptStatus(railwayFilter: String? = null): OdptApiState {
        if (!TransitApiConfigs.isOdptConfigured()) {
            return OdptApiState.NotConfigured
        }
        val statusResult = odptProvider.getOperationStatus(null)
        val targetRailway = railwayFilter ?: "odpt.Railway:TokyoMetro.Ginza"
        val trainsResult = odptProvider.getTrainPositions(targetRailway)

        return when {
            statusResult is ProviderResult.NotConfigured && trainsResult is ProviderResult.NotConfigured ->
                OdptApiState.NotConfigured
            statusResult is ProviderResult.Error && trainsResult is ProviderResult.Error ->
                OdptApiState.Error(statusResult.message)
            statusResult is ProviderResult.RateLimited && trainsResult is ProviderResult.RateLimited ->
                OdptApiState.Error("ODPT API レート制限のため ${statusResult.retryAfterSeconds}秒後に再取得できます。")
            else -> {
                val infos = (statusResult as? ProviderResult.Success)?.data.orEmpty().map { st ->
                    OdptLineStatus(
                        railwayId = st.lineId,
                        railwayTitle = st.lineName,
                        operatorTitle = st.operatorId,
                        statusText = st.status,
                        infoText = st.message,
                        isDelayed = st.isDelayed,
                        updatedAt = st.updatedAt
                    )
                }
                val trains = (trainsResult as? ProviderResult.Success)?.data.orEmpty().take(18).map { tr ->
                    OdptTrainLocation(
                        trainNumber = tr.trainNumber,
                        railwayId = tr.lineId.substringAfterLast("."),
                        trainType = tr.trainType,
                        destinationStation = tr.destination,
                        fromStation = tr.currentStation,
                        toStation = tr.nextStation,
                        delayMinutes = tr.delayMinutes,
                        direction = tr.direction,
                        updatedAt = tr.lastUpdated
                    )
                }
                val updated = (trainsResult as? ProviderResult.Success)?.updatedAt
                    ?: (statusResult as? ProviderResult.Success)?.updatedAt
                    ?: "--:--"
                OdptApiState.Available(
                    trainInfos = infos,
                    liveTrains = trains,
                    updatedAt = updated
                )
            }
        }
    }

    suspend fun fetchLiveTrainsForLine(lineId: String): ProviderResult<List<Train>> {
        val line = NationalRailwayRegistry.findLine(lineId)
        val op = line?.let { NationalRailwayRegistry.findOperator(it.operatorId) }
        return when {
            lineId.startsWith("odpt.Railway:") ||
                (op?.odptOperatorId != null && ProviderCapability.TRAIN_POSITIONS in op.capabilities) ->
                odptProvider.getTrainPositions(lineId)
            jrShikokuAdapter.supportsLine(lineId) ->
                jrShikokuAdapter.getTrainPositions(lineId)
            jrTrainformationAdapter.supportsLine(lineId) ->
                jrTrainformationAdapter.getTrainPositions(lineId)
            else ->
                officialProvider.getTrainPositions(lineId)
        }
    }

    suspend fun fetchLineStations(lineId: String): List<RailwayStation> =
        odptProvider.getLineStations(lineId)

    suspend fun fetchOperationStatuses(operatorId: String? = null): ProviderResult<List<TrainOperationStatus>> {
        val op = operatorId?.let { NationalRailwayRegistry.findOperator(it) }
        return when {
            operatorId == null || op?.odptOperatorId != null ->
                odptProvider.getOperationStatus(op?.odptOperatorId)
            operatorId == "op_jr_shikoku" ->
                jrShikokuAdapter.getOperationStatus(operatorId)
            jrTrainformationAdapter.supportsOperator(operatorId) ->
                jrTrainformationAdapter.getOperationStatus(operatorId)
            else ->
                officialProvider.getOperationStatus(operatorId)
        }
    }

    suspend fun fetchStationTimetable(
        stationIdOrName: String,
        lineId: String? = null
    ): ProviderResult<List<StationTimetable>> {
        val odptStation = NationalRailwayRegistry.findStationByIdOrName(stationIdOrName)?.stationId
        val prefersOdpt = lineId?.startsWith("odpt.Railway:") == true ||
            odptStation?.startsWith("odpt.Station:") == true ||
            stationIdOrName.startsWith("odpt.Station:")

        if (prefersOdpt) {
            val odptRes = odptProvider.getStationTimetable(stationIdOrName, lineId)
            if (odptRes is ProviderResult.Success && odptRes.data.isNotEmpty()) {
                return odptRes
            }
            if (TransitApiConfigs.isEkispertConfigured()) {
                val ekiFallback = ekispertProvider.getStationTimetable(stationIdOrName, lineId)
                if (ekiFallback is ProviderResult.Success) return ekiFallback
            }
            return odptRes
        }
        return ekispertProvider.getStationTimetable(stationIdOrName, lineId)
    }

    suspend fun searchNationwideRoutes(
        request: RouteSearchRequest,
        baseMinutesOfDay: Int,
        odptState: OdptApiState,
        liveStatuses: List<TrainOperationStatus> = emptyList()
    ): RouteSearchBundle {
        val fetchedStatuses = if (liveStatuses.isEmpty()) {
            (odptProvider.getOperationStatus(null) as? ProviderResult.Success)?.data.orEmpty()
        } else {
            liveStatuses
        }

        val liveStatusByLine = buildMap<String, TrainOperationStatus> {
            fetchedStatuses.forEach { st ->
                put(st.lineId, st)
                put(st.lineName, st)
            }
            if (odptState is OdptApiState.Available) {
                odptState.trainInfos.forEach { info ->
                    val status = TrainOperationStatus(
                        operatorId = info.operatorTitle,
                        lineId = info.railwayId,
                        lineName = info.railwayTitle,
                        status = info.statusText,
                        delayMinutes = if (info.isDelayed) 10 else 0,
                        disruptionType = if (info.isDelayed) "遅延" else "平常",
                        message = info.infoText,
                        updatedAt = info.updatedAt,
                        source = "ODPT API v4",
                        isDelayed = info.isDelayed
                    )
                    put(info.railwayId, status)
                    put(info.railwayTitle, status)
                }
            }
        }

        val ekiResult = ekispertProvider.searchRoutes(request)
        if (ekiResult is ProviderResult.Success && ekiResult.data.isNotEmpty()) {
            val enriched = enrichEkispertRoutes(
                routes = ekiResult.data,
                liveStatusByLine = liveStatusByLine,
                request = request,
                isStale = ekiResult.isStale
            )
            if (enriched.isNotEmpty()) {
                val legacy = enriched.map { toLegacyRouteOption(it) }
                val sourceSummary = buildString {
                    append(if (ekiResult.isStale) "駅すぱあとキャッシュ" else "駅すぱあとAPI")
                    if (liveStatusByLine.isNotEmpty()) append(" ＋ ODPT運行情報")
                    if (enriched.any { "mixway" in it.dataSources }) append(" ＋ mixway")
                }
                return RouteSearchBundle(
                    legacyOptions = legacy,
                    routeResults = enriched,
                    sourceSummary = sourceSummary,
                    providerNotice = if (ekiResult.isStale) "一時的に駅すぱあとキャッシュ結果を表示しています（更新 ${ekiResult.updatedAt}）。" else null
                )
            }
        }

        val fallbackLegacy = searchRoutes(
            origin = request.origin,
            destination = request.destination,
            baseMinutesOfDay = baseMinutesOfDay,
            sortPreference = request.sortPreference,
            seatPreference = request.seatPreference,
            enabledModes = request.enabledModes,
            maxTransfers = request.maxTransfers,
            odptState = odptState
        )
        val fallbackRich = fallbackLegacy.map { option ->
            enrichLegacyAsRouteResult(option, liveStatusByLine)
        }
        val notice = when (ekiResult) {
            is ProviderResult.NotConfigured -> ekiResult.reason
            is ProviderResult.RateLimited -> "駅すぱあとAPIレート制限のため標準ダイヤ参照を表示しています（${ekiResult.retryAfterSeconds}秒後に再試行可）。"
            is ProviderResult.Error -> "${ekiResult.message}（標準ダイヤ参照を表示中）"
            else -> "標準ダイヤ参照による経路候補を表示中（リアルタイム経路APIはキー設定で有効化）"
        }

        return RouteSearchBundle(
            legacyOptions = fallbackLegacy,
            routeResults = fallbackRich,
            sourceSummary = if (liveStatusByLine.isNotEmpty()) "標準ダイヤ参照 ＋ ODPT運行情報" else "標準ダイヤ参照",
            providerNotice = notice
        )
    }

    private suspend fun enrichEkispertRoutes(
        routes: List<RouteResult>,
        liveStatusByLine: Map<String, TrainOperationStatus>,
        request: RouteSearchRequest,
        isStale: Boolean
    ): List<RouteResult> {
        val enriched = routes.map { route ->
            var cumulativeDelay = 0
            val enrichedLegs = route.legs.map { leg ->
                val matchedStatus = findMatchingStatus(leg.lineName, leg.odptRailwayId, liveStatusByLine)
                val mixwayResult = if (!leg.tripCode.isNullOrBlank()) {
                    mixwayProvider.getRealtimeTrip(leg.tripCode) as? ProviderResult.Success
                } else null
                val mixwayDelay = mixwayResult?.data?.delayMinutes ?: 0
                val delay = maxOf(matchedStatus?.delayMinutes ?: 0, mixwayDelay, leg.delayMinutes)
                cumulativeDelay = maxOf(cumulativeDelay, delay)
                leg.copy(
                    operationStatus = matchedStatus?.status ?: if (delay > 0) "${delay}分遅れ" else "平常",
                    delayMinutes = delay,
                    predictedDepartureTime = shiftHm(leg.departureTime, delay),
                    predictedArrivalTime = shiftHm(leg.arrivalTime, delay),
                    transferFeasible = delay < 10
                )
            }
            val anyDelayed = cumulativeDelay > 0 || enrichedLegs.any { it.operationStatus?.contains("見合わせ") == true }
            val sources = buildList {
                add(if (isStale) "駅すぱあとAPI(キャッシュ)" else "駅すぱあとAPI")
                if (liveStatusByLine.isNotEmpty()) add("ODPT運行情報")
                if (enrichedLegs.any { !it.tripCode.isNullOrBlank() && it.delayMinutes > 0 }) add("mixway")
            }
            route.copy(
                legs = enrichedLegs,
                realtimeStatus = if (anyDelayed) "一部遅延・運行注意（約+${cumulativeDelay}分）" else "平常運転見込",
                dataSources = sources,
                isRealtimeEnriched = liveStatusByLine.isNotEmpty(),
                alternativeSuggested = anyDelayed,
                tags = if (anyDelayed) (route.tags + "迂回候補推奨").distinct() else route.tags,
                reliabilityNote = if (anyDelayed) {
                    "遅延・運行支障を検知したため予測到着時刻と迂回候補を表示しています。"
                } else {
                    "駅すぱあと探索結果" + if (liveStatusByLine.isNotEmpty()) "・ODPT運行情報照合済" else ""
                }
            )
        }.filter { route ->
            (request.maxTransfers == null || route.transferCount <= request.maxTransfers) &&
                (request.enabledModes.isEmpty() || route.legs.any { it.mode in request.enabledModes || it.mode == TransportMode.WALK })
        }

        return when (request.sortPreference) {
            SortPreference.FASTEST -> enriched.sortedBy { it.durationMinutes }
            SortPreference.CHEAPEST -> enriched.sortedBy { it.fare }
            SortPreference.FEWEST_TRANSFERS -> enriched.sortedWith(compareBy<RouteResult> { it.transferCount }.thenBy { it.durationMinutes })
            SortPreference.LEAST_WALKING -> enriched.sortedWith(compareBy<RouteResult> { it.walkMeters }.thenBy { it.durationMinutes })
        }
    }

    private fun enrichLegacyAsRouteResult(
        option: TransitRouteOption,
        liveStatusByLine: Map<String, TrainOperationStatus>
    ): RouteResult {
        var maxDelay = 0
        val richLegs = option.legs.map { leg ->
            val matchedStatus = findMatchingStatus(leg.lineName, leg.odptRailwayId, liveStatusByLine)
            val delay = matchedStatus?.delayMinutes ?: 0
            maxDelay = maxOf(maxDelay, delay)
            RouteLegDetail(
                mode = leg.mode,
                lineName = leg.lineName,
                operatorName = leg.operatorName,
                odptRailwayId = leg.odptRailwayId,
                departureStation = leg.departureStation,
                arrivalStation = leg.arrivalStation,
                departureTime = leg.departureTime,
                arrivalTime = leg.arrivalTime,
                predictedDepartureTime = if (delay > 0) shiftHm(leg.departureTime, delay) else leg.departureTime,
                predictedArrivalTime = if (delay > 0) shiftHm(leg.arrivalTime, delay) else leg.arrivalTime,
                durationMinutes = leg.durationMinutes,
                fareJpy = leg.fareJpy,
                walkDistanceMeters = leg.walkDistanceMeters,
                platform = leg.platform,
                operationStatus = matchedStatus?.status ?: if (leg.odptRailwayId != null && liveStatusByLine.isNotEmpty()) "平常運転" else null,
                delayMinutes = delay,
                transferFeasible = delay < 10,
                carGuidance = leg.carGuidance,
                bookingPartnerName = leg.bookingPartnerName,
                bookingUrl = leg.bookingUrl
            )
        }
        val anyDelay = maxDelay > 0 || !option.delayAlert.isNullOrBlank()
        val sources = buildList {
            add(option.provenance.labelJa)
            if (liveStatusByLine.isNotEmpty()) add("ODPT運行情報")
        }
        return RouteResult(
            id = option.id,
            origin = option.origin,
            destination = option.destination,
            departureTime = option.departureTime,
            arrivalTime = option.arrivalTime,
            durationMinutes = option.totalMinutes,
            fare = option.totalFareJpy,
            transferCount = option.transferCount,
            walkMeters = option.totalWalkMeters,
            legs = richLegs,
            realtimeStatus = if (anyDelay) option.delayAlert ?: "遅延あり（約+${maxDelay}分・迂回推奨）" else "通常ダイヤ",
            dataSources = sources,
            isRealtimeEnriched = liveStatusByLine.isNotEmpty(),
            tags = if (anyDelay) (option.tags + "迂回ルート確認推奨").distinct() else option.tags,
            alternativeSuggested = anyDelay,
            reliabilityNote = option.reliabilityNote
        )
    }

    private fun toLegacyRouteOption(route: RouteResult): TransitRouteOption {
        val alert = route.legs.firstOrNull { it.delayMinutes > 0 || (it.operationStatus?.contains("見合わせ") == true) }
            ?.let { "${it.lineName}: ${it.operationStatus ?: "${it.delayMinutes}分遅れ"}" }
        return TransitRouteOption(
            id = route.id,
            origin = route.origin,
            destination = route.destination,
            departureTime = route.departureTime,
            arrivalTime = route.arrivalTime,
            totalMinutes = route.durationMinutes,
            totalFareJpy = route.fare,
            transferCount = route.transferCount,
            totalWalkMeters = route.walkMeters,
            legs = route.legs.map { leg ->
                TransitLeg(
                    mode = leg.mode,
                    lineName = leg.lineName,
                    operatorName = leg.operatorName,
                    odptRailwayId = leg.odptRailwayId,
                    departureStation = leg.departureStation,
                    arrivalStation = leg.arrivalStation,
                    departureTime = leg.predictedDepartureTime ?: leg.departureTime,
                    arrivalTime = leg.predictedArrivalTime ?: leg.arrivalTime,
                    durationMinutes = leg.durationMinutes,
                    fareJpy = leg.fareJpy,
                    walkDistanceMeters = leg.walkDistanceMeters,
                    platform = leg.platform,
                    carGuidance = leg.carGuidance,
                    bookingPartnerName = leg.bookingPartnerName,
                    bookingUrl = leg.bookingUrl
                )
            },
            tags = route.tags,
            provenance = if (route.isRealtimeEnriched) DataProvenance.ODPT_LIVE else DataProvenance.SCHEDULE_REF,
            delayAlert = alert,
            reliabilityNote = route.reliabilityNote
        )
    }

    private fun findMatchingStatus(
        lineName: String,
        odptRailwayId: String?,
        statuses: Map<String, TrainOperationStatus>
    ): TrainOperationStatus? {
        if (!odptRailwayId.isNullOrBlank()) {
            statuses[odptRailwayId]?.let { return it }
        }
        return statuses.entries.firstOrNull { (k, v) ->
            lineName.contains(v.lineName) || v.lineName.contains(lineName) || lineName.contains(k.substringAfterLast("."))
        }?.value
    }

    private fun shiftHm(hhMm: String, addMinutes: Int): String {
        if (addMinutes == 0) return hhMm
        val parts = hhMm.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return hhMm
        val m = parts.getOrNull(1)?.toIntOrNull() ?: return hhMm
        return TransitKnowledgeBase.formatHm(h * 60 + m + addMinutes)
    }

    fun searchRoutes(
        origin: String,
        destination: String,
        baseMinutesOfDay: Int,
        sortPreference: SortPreference,
        seatPreference: SeatPreference,
        enabledModes: Set<TransportMode>,
        maxTransfers: Int?,
        odptState: OdptApiState
    ): List<TransitRouteOption> {
        val alerts = if (odptState is OdptApiState.Available) {
            odptState.trainInfos
                .filter { it.isDelayed }
                .associate { it.railwayId to "${it.railwayTitle}: ${it.statusText}" }
        } else {
            emptyMap()
        }

        val raw = TransitKnowledgeBase.buildRouteCandidates(
            originRaw = origin,
            destinationRaw = destination,
            baseMinutesOfDay = baseMinutesOfDay,
            seatPreference = seatPreference,
            enabledModes = enabledModes,
            liveLineAlerts = alerts
        ).filter { option ->
            (maxTransfers == null || option.transferCount <= maxTransfers) &&
                (enabledModes.isEmpty() || option.legs.any { it.mode in enabledModes || it.mode == TransportMode.WALK })
        }

        return when (sortPreference) {
            SortPreference.FASTEST -> raw.sortedBy { it.totalMinutes }
            SortPreference.CHEAPEST -> raw.sortedBy { it.totalFareJpy }
            SortPreference.FEWEST_TRANSFERS -> raw.sortedWith(compareBy<TransitRouteOption> { it.transferCount }.thenBy { it.totalMinutes })
            SortPreference.LEAST_WALKING -> raw.sortedWith(compareBy<TransitRouteOption> { it.totalWalkMeters }.thenBy { it.totalMinutes })
        }
    }

    fun stationFacilityFor(stationQuery: String): StationFacilityInfo {
        val hub = TransitKnowledgeBase.resolveHub(stationQuery)
        return hub?.facilityInfo ?: StationFacilityInfo(
            stationName = stationQuery.ifBlank { "駅未指定" },
            supported = false,
            unsupportedReason = "この駅の構内図・乗換位置詳細データは現在未対応です。"
        )
    }

    fun providerSupportSummaries(): List<Pair<String, ProviderCapability>> = listOf(
        odptProvider.displayName to ProviderCapability.TRAIN_POSITIONS,
        ekispertProvider.displayName to ProviderCapability.ROUTE_SEARCH,
        mixwayProvider.displayName to ProviderCapability.REALTIME_TRIP,
        jrTrainformationAdapter.displayName to ProviderCapability.TRAIN_POSITIONS,
        jrShikokuAdapter.displayName to ProviderCapability.TRAIN_POSITIONS,
        jorudanProvider.displayName to ProviderCapability.ROUTE_SEARCH,
        navitimeProvider.displayName to ProviderCapability.ROUTE_SEARCH,
        officialProvider.displayName to ProviderCapability.OPERATION_STATUS,
        gtfsProvider.displayName to ProviderCapability.STATION_TIMETABLE
    )
}
