package com.example.data.repository

import com.example.core.OdptConfig
import com.example.data.remote.NetworkModule
import com.example.data.remote.OdptApi
import com.example.data.remote.TransitKnowledgeBase
import com.example.model.OdptApiState
import com.example.model.OdptLineStatus
import com.example.model.OdptTrainLocation
import com.example.model.SeatPreference
import com.example.model.SortPreference
import com.example.model.StationFacilityInfo
import com.example.model.TransitRouteOption
import com.example.model.TransportMode
import kotlinx.coroutines.CancellationException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransitRepository(
    private val odptApi: OdptApi = NetworkModule.odptApi
) {
    suspend fun fetchLiveOdptStatus(railwayFilter: String? = null): OdptApiState {
        if (!OdptConfig.isConfigured()) {
            return OdptApiState.NotConfigured
        }
        val key = OdptConfig.apiKey()
        return try {
            val metroInfos = runCatching {
                odptApi.getTrainInformation(consumerKey = key, operatorId = "odpt.Operator:TokyoMetro")
            }.getOrDefault(emptyList())
            val toeiInfos = runCatching {
                odptApi.getTrainInformation(consumerKey = key, operatorId = "odpt.Operator:Toei")
            }.getOrDefault(emptyList())

            val allInfos = (metroInfos + toeiInfos).map { dto ->
                val railway = dto.railwayId.orEmpty()
                val statusJa = dto.statusMap?.get("ja") ?: "平常運転"
                val textJa = dto.textMap?.get("ja") ?: "現在、平常通り運転しています。"
                OdptLineStatus(
                    railwayId = railway,
                    railwayTitle = railway.substringAfterLast("."),
                    operatorTitle = dto.operatorId.orEmpty().substringAfterLast(":"),
                    statusText = statusJa,
                    infoText = textJa,
                    isDelayed = !statusJa.contains("平常") && textJa.contains("遅れ") || textJa.contains("見合わせ"),
                    updatedAt = dto.date?.substringAfter("T")?.take(5) ?: "--:--"
                )
            }

            val targetRailway = railwayFilter ?: "odpt.Railway:TokyoMetro.Ginza"
            val trains = runCatching {
                odptApi.getTrains(consumerKey = key, railwayId = targetRailway)
            }.getOrDefault(emptyList()).take(12).map { dto ->
                OdptTrainLocation(
                    trainNumber = dto.trainNumber ?: "---",
                    railwayId = dto.railwayId.orEmpty().substringAfterLast("."),
                    trainType = dto.trainType.orEmpty().substringAfterLast("."),
                    destinationStation = dto.destinationStations?.firstOrNull()?.substringAfterLast(".").orEmpty(),
                    fromStation = dto.fromStation.orEmpty().substringAfterLast("."),
                    toStation = dto.toStation.orEmpty().substringAfterLast("."),
                    delayMinutes = (dto.delaySeconds ?: 0) / 60,
                    direction = dto.railDirection.orEmpty().substringAfterLast("."),
                    updatedAt = dto.date?.substringAfter("T")?.take(5) ?: "--:--"
                )
            }

            val nowStr = SimpleDateFormat("HH:mm", Locale.JAPAN).format(Date())
            OdptApiState.Available(
                trainInfos = allInfos,
                liveTrains = trains,
                updatedAt = nowStr
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            OdptApiState.Error(e.localizedMessage ?: "ODPT APIへの接続に失敗しました。")
        }
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
}
