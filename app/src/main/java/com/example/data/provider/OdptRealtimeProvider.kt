package com.example.data.provider

import com.example.core.TransitApiConfigs
import com.example.data.remote.ApiCacheAndRateLimiter
import com.example.data.remote.NationalRailwayRegistry
import com.example.data.remote.NetworkModule
import com.example.data.remote.OdptApi
import com.example.data.remote.ProviderRatePolicies
import com.example.model.ProviderCapability
import com.example.model.ProviderResult
import com.example.model.RailwayOperator
import com.example.model.RailwayStation
import com.example.model.StationTimetable
import com.example.model.Train
import com.example.model.TrainOperationStatus
import com.example.model.TrainPosition
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OdptRealtimeProvider(
    private val odptApi: OdptApi = NetworkModule.odptApi,
    private val cacheLimiter: ApiCacheAndRateLimiter = ApiCacheAndRateLimiter(),
    private val apiKeyProvider: () -> String = { TransitApiConfigs.odptApiKey() },
    private val isConfiguredCheck: () -> Boolean = { TransitApiConfigs.isOdptConfigured() },
    private val timeFormatter: () -> String = {
        SimpleDateFormat("HH:mm", Locale.JAPAN).format(Date())
    }
) : RailwayRealtimeProvider {

    override val providerId: String = "odpt"
    override val displayName: String = "公共交通オープンデータセンター (ODPT)"

    override fun getCapabilities(): Set<ProviderCapability> = setOf(
        ProviderCapability.TRAIN_POSITIONS,
        ProviderCapability.OPERATION_STATUS,
        ProviderCapability.STATION_TIMETABLE
    )

    override fun getSupportedOperators(): List<RailwayOperator> =
        NationalRailwayRegistry.operators.filter { it.odptOperatorId != null }

    suspend fun getLineStations(lineId: String): List<RailwayStation> {
        val localStations = NationalRailwayRegistry.stationsForLine(lineId)
        if (!isConfiguredCheck() || !lineId.startsWith("odpt.Railway:")) {
            return localStations
        }
        val cacheKey = "odpt_stations_$lineId"
        cacheLimiter.getFresh<List<RailwayStation>>(cacheKey, ProviderRatePolicies.staticCatalog)?.let {
            return it.value
        }
        if (!cacheLimiter.canIssueNetworkRequest(cacheKey, ProviderRatePolicies.staticCatalog)) {
            return cacheLimiter.getStale<List<RailwayStation>>(cacheKey, ProviderRatePolicies.staticCatalog)?.value
                ?: localStations
        }
        cacheLimiter.recordAttempt(cacheKey)
        return try {
            val remote = odptApi.getStations(consumerKey = apiKeyProvider(), railwayId = lineId)
                .mapNotNull { dto ->
                    val id = dto.sameAs ?: return@mapNotNull null
                    val lat = dto.latitude
                    val lng = dto.longitude
                    val fallback = NationalRailwayRegistry.findStationByIdOrName(id)
                    val resolvedLat = lat ?: fallback?.latitude ?: return@mapNotNull null
                    val resolvedLng = lng ?: fallback?.longitude ?: return@mapNotNull null
                    RailwayStation(
                        stationId = id,
                        stationName = dto.stationTitle?.bestJa()?.takeIf { it.isNotBlank() }
                            ?: dto.title?.takeIf { it.isNotBlank() }
                            ?: id.substringAfterLast("."),
                        stationCode = dto.stationCode.orEmpty(),
                        latitude = resolvedLat,
                        longitude = resolvedLng,
                        operatorIds = listOfNotNull(dto.operatorId),
                        lineIds = listOf(lineId)
                    )
                }
            val merged = if (remote.isNotEmpty()) remote else localStations
            cacheLimiter.put(cacheKey, merged, timeFormatter())
            merged
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            localStations
        }
    }

    override suspend fun getTrainPositions(lineId: String): ProviderResult<List<Train>> {
        if (!lineId.startsWith("odpt.Railway:")) {
            val line = NationalRailwayRegistry.findLine(lineId)
            return ProviderResult.Unsupported(
                providerName = displayName,
                reason = line?.unsupportedReason ?: "この路線はODPTリアルタイム列車走行位置の対象外です。"
            )
        }
        if (!isConfiguredCheck()) {
            return ProviderResult.NotConfigured(
                providerName = displayName,
                reason = "ODPT_API_KEY が未設定です。AI Studio Secrets で設定すると対応路線の実列車位置を取得します。"
            )
        }

        val cacheKey = "odpt_trains_$lineId"
        cacheLimiter.getFresh<List<Train>>(cacheKey, ProviderRatePolicies.odptPositions)?.let { cached ->
            return ProviderResult.Success(
                data = cached.value,
                sourceName = displayName,
                updatedAt = cached.updatedAtLabel,
                fromCache = true,
                isStale = false
            )
        }

        if (!cacheLimiter.canIssueNetworkRequest(cacheKey, ProviderRatePolicies.odptPositions)) {
            val stale = cacheLimiter.getStale<List<Train>>(cacheKey, ProviderRatePolicies.odptPositions)
            if (stale != null) {
                return ProviderResult.Success(
                    data = stale.value,
                    sourceName = displayName,
                    updatedAt = stale.updatedAtLabel,
                    fromCache = true,
                    isStale = true
                )
            }
            return ProviderResult.RateLimited(
                providerName = displayName,
                retryAfterSeconds = cacheLimiter.remainingCooldownSeconds(cacheKey, ProviderRatePolicies.odptPositions)
            )
        }

        cacheLimiter.recordAttempt(cacheKey)
        return try {
            val stations = getLineStations(lineId)
            val stationMap = stations.associateBy { it.stationId }
            val dtos = odptApi.getTrains(consumerKey = apiKeyProvider(), railwayId = lineId)
            val nowStr = timeFormatter()
            val mapped = dtos.map { dto ->
                val fromId = dto.fromStation.orEmpty()
                val toId = dto.toStation.orEmpty()
                val fromStation = stationMap[fromId] ?: NationalRailwayRegistry.findStationByIdOrName(fromId)
                val toStation = stationMap[toId] ?: NationalRailwayRegistry.findStationByIdOrName(toId)
                val progress = if (toStation != null && fromStation != null) 0.45f else 0f
                val coords = GeoTrackMath.interpolatePosition(fromStation, toStation, progress)
                val bearing = if (fromStation != null && toStation != null) {
                    GeoTrackMath.bearingDegrees(
                        fromStation.latitude, fromStation.longitude,
                        toStation.latitude, toStation.longitude
                    )
                } else 0f
                val delayMin = (dto.delaySeconds ?: 0) / 60
                val updated = dto.date?.substringAfter("T")?.take(5) ?: nowStr
                val trainId = dto.sameAs ?: "${lineId}_${dto.trainNumber ?: "unknown"}"
                val fromLabel = fromStation?.stationName ?: fromId.substringAfterLast(".").ifBlank { "走行中" }
                val toLabel = toStation?.stationName ?: toId.substringAfterLast(".")

                val pos = coords?.let { (lat, lng) ->
                    TrainPosition(
                        trainId = trainId,
                        latitude = lat,
                        longitude = lng,
                        bearing = bearing,
                        currentStation = fromLabel,
                        nextStation = toLabel,
                        progress = progress,
                        delayMinutes = delayMin,
                        timestamp = updated,
                        dataSource = displayName,
                        confidence = if (fromStation != null && toStation != null) 0.9f else 0.65f
                    )
                }

                Train(
                    trainId = trainId,
                    tripId = dto.sameAs,
                    operatorId = dto.operatorId.orEmpty().substringAfterLast(":"),
                    lineId = lineId,
                    trainNumber = dto.trainNumber ?: "---",
                    trainType = dto.trainType.orEmpty().substringAfterLast(".").ifBlank { "列車" },
                    destination = dto.destinationStations?.firstOrNull()?.let {
                        NationalRailwayRegistry.findStationByIdOrName(it)?.stationName
                            ?: it.substringAfterLast(".")
                    }.orEmpty(),
                    direction = dto.railDirection.orEmpty().substringAfterLast("."),
                    currentStation = fromLabel,
                    nextStation = toLabel,
                    actualPosition = pos,
                    delayMinutes = delayMin,
                    realtimeStatus = if (delayMin > 0) "${delayMin}分遅れ" else "平常",
                    lastUpdated = updated
                )
            }
            cacheLimiter.put(cacheKey, mapped, nowStr)
            ProviderResult.Success(
                data = mapped,
                sourceName = displayName,
                updatedAt = nowStr,
                fromCache = false,
                isStale = false
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            handleHttpFailure(e, cacheKey, ProviderRatePolicies.odptPositions)
        } catch (e: IOException) {
            handleIoFailure(e, cacheKey, ProviderRatePolicies.odptPositions)
        } catch (e: Exception) {
            ProviderResult.Error(displayName, "ODPTデータ解析に失敗しました: ${e.localizedMessage ?: "不正なレスポンス"}")
        }
    }

    override suspend fun getTrainPosition(trainId: String): ProviderResult<Train?> {
        val linePrefix = trainId.substringBeforeLast(".")
            .replace("odpt.Train:", "odpt.Railway:")
        return when (val listResult = getTrainPositions(linePrefix)) {
            is ProviderResult.Success -> ProviderResult.Success(
                data = listResult.data.firstOrNull { it.trainId == trainId },
                sourceName = listResult.sourceName,
                updatedAt = listResult.updatedAt,
                fromCache = listResult.fromCache,
                isStale = listResult.isStale
            )
            is ProviderResult.NotConfigured -> listResult
            is ProviderResult.Unsupported -> listResult
            is ProviderResult.RateLimited -> listResult
            is ProviderResult.Error -> listResult
        }
    }

    override suspend fun getOperationStatus(operatorId: String?): ProviderResult<List<TrainOperationStatus>> {
        if (!isConfiguredCheck()) {
            return ProviderResult.NotConfigured(
                providerName = displayName,
                reason = "ODPT_API_KEY が未設定です。"
            )
        }
        val cacheKey = "odpt_status_${operatorId ?: "default"}"
        cacheLimiter.getFresh<List<TrainOperationStatus>>(cacheKey, ProviderRatePolicies.odptStatus)?.let { cached ->
            return ProviderResult.Success(
                data = cached.value,
                sourceName = displayName,
                updatedAt = cached.updatedAtLabel,
                fromCache = true,
                isStale = false
            )
        }
        if (!cacheLimiter.canIssueNetworkRequest(cacheKey, ProviderRatePolicies.odptStatus)) {
            val stale = cacheLimiter.getStale<List<TrainOperationStatus>>(cacheKey, ProviderRatePolicies.odptStatus)
            if (stale != null) {
                return ProviderResult.Success(
                    data = stale.value,
                    sourceName = displayName,
                    updatedAt = stale.updatedAtLabel,
                    fromCache = true,
                    isStale = true
                )
            }
            return ProviderResult.RateLimited(
                providerName = displayName,
                retryAfterSeconds = cacheLimiter.remainingCooldownSeconds(cacheKey, ProviderRatePolicies.odptStatus)
            )
        }

        cacheLimiter.recordAttempt(cacheKey)
        return try {
            val key = apiKeyProvider()
            val targetOperators = if (operatorId != null) {
                listOf(operatorId)
            } else {
                listOf("odpt.Operator:TokyoMetro", "odpt.Operator:Toei")
            }
            val dtos = targetOperators.flatMap { op ->
                runCatching { odptApi.getTrainInformation(consumerKey = key, operatorId = op) }
                    .getOrElse { err -> if (err is HttpException && (err.code() == 401 || err.code() == 403 || err.code() == 429)) throw err else emptyList() }
            }
            val nowStr = timeFormatter()
            val mapped = dtos.map { dto ->
                val railway = dto.railwayId.orEmpty()
                val statusJa = dto.statusMap?.get("ja") ?: "平常運転"
                val textJa = dto.textMap?.get("ja") ?: "現在、平常通り運転しています。"
                val isDelayed = (!statusJa.contains("平常") && (textJa.contains("遅れ") || textJa.contains("見合わせ") || textJa.contains("運休"))) ||
                    statusJa.contains("遅延") || statusJa.contains("見合わせ")
                val disruptionType = when {
                    textJa.contains("見合わせ") || statusJa.contains("見合わせ") -> "運転見合わせ"
                    textJa.contains("運休") -> "一部運休"
                    isDelayed -> "遅延"
                    else -> "平常"
                }
                TrainOperationStatus(
                    operatorId = dto.operatorId.orEmpty().substringAfterLast(":"),
                    lineId = railway,
                    lineName = NationalRailwayRegistry.findLine(railway)?.lineName ?: railway.substringAfterLast("."),
                    status = statusJa,
                    delayMinutes = if (isDelayed) 10 else 0,
                    disruptionType = disruptionType,
                    message = textJa,
                    updatedAt = dto.date?.substringAfter("T")?.take(5) ?: nowStr,
                    source = displayName,
                    isDelayed = isDelayed
                )
            }
            cacheLimiter.put(cacheKey, mapped, nowStr)
            ProviderResult.Success(
                data = mapped,
                sourceName = displayName,
                updatedAt = nowStr,
                fromCache = false,
                isStale = false
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            handleHttpFailure(e, cacheKey, ProviderRatePolicies.odptStatus)
        } catch (e: IOException) {
            handleIoFailure(e, cacheKey, ProviderRatePolicies.odptStatus)
        } catch (e: Exception) {
            ProviderResult.Error(displayName, "ODPT運行情報の解析に失敗しました: ${e.localizedMessage ?: "不正なデータ"}")
        }
    }

    override suspend fun getStationTimetable(
        stationIdOrName: String,
        lineId: String?
    ): ProviderResult<List<StationTimetable>> {
        if (!isConfiguredCheck()) {
            return ProviderResult.NotConfigured(
                providerName = displayName,
                reason = "ODPT_API_KEY を設定すると対応路線の駅時刻表を取得できます。"
            )
        }
        val resolvedStation = NationalRailwayRegistry.findStationByIdOrName(stationIdOrName)?.stationId
            ?: stationIdOrName.takeIf { it.startsWith("odpt.Station:") }
        val resolvedLine = lineId?.takeIf { it.startsWith("odpt.Railway:") }
        if (resolvedStation == null && resolvedLine == null) {
            return ProviderResult.Unsupported(displayName, "ODPTの駅IDまたは路線IDが特定できませんでした。")
        }

        val cacheKey = "odpt_tt_${resolvedStation ?: "any"}_${resolvedLine ?: "any"}"
        cacheLimiter.getFresh<List<StationTimetable>>(cacheKey, ProviderRatePolicies.staticCatalog)?.let { cached ->
            return ProviderResult.Success(cached.value, displayName, cached.updatedAtLabel, fromCache = true)
        }

        cacheLimiter.recordAttempt(cacheKey)
        return try {
            val dtos = odptApi.getStationTimetables(
                consumerKey = apiKeyProvider(),
                railwayId = resolvedLine,
                stationId = resolvedStation
            )
            val nowStr = timeFormatter()
            val list = dtos.flatMap { dto ->
                val direction = dto.railDirection.orEmpty().substringAfterLast(".").ifBlank { "方面" }
                val stId = dto.stationId.orEmpty()
                val stName = dto.stationTitle?.bestJa()?.takeIf { it.isNotBlank() }
                    ?: NationalRailwayRegistry.findStationByIdOrName(stId)?.stationName
                    ?: stId.substringAfterLast(".")
                val entries = dto.weekdays ?: dto.timetableObjects ?: dto.saturdays ?: dto.holidays ?: emptyList()
                val cal = if (dto.weekdays != null) "平日" else if (dto.saturdays != null) "土曜" else "休日"
                entries.take(30).map { entry ->
                    val destId = entry.destinationStations?.firstOrNull().orEmpty()
                    val destName = NationalRailwayRegistry.findStationByIdOrName(destId)?.stationName
                        ?: destId.substringAfterLast(".").ifBlank { direction }
                    StationTimetable(
                        stationId = stId,
                        stationName = stName,
                        lineId = dto.railwayId.orEmpty(),
                        direction = direction,
                        departureTime = entry.departureTime ?: "--:--",
                        trainType = entry.trainType.orEmpty().substringAfterLast(".").ifBlank { "各停" },
                        destination = destName,
                        platform = entry.platformNumber.orEmpty(),
                        trainNumber = entry.trainNumber.orEmpty(),
                        calendarType = cal,
                        source = displayName
                    )
                }
            }
            cacheLimiter.put(cacheKey, list, nowStr)
            ProviderResult.Success(list, displayName, nowStr)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            handleHttpFailure(e, cacheKey, ProviderRatePolicies.staticCatalog)
        } catch (e: IOException) {
            handleIoFailure(e, cacheKey, ProviderRatePolicies.staticCatalog)
        } catch (e: Exception) {
            ProviderResult.Error(displayName, "ODPT駅時刻表の解析に失敗しました: ${e.localizedMessage ?: "不正なデータ"}")
        }
    }

    private fun <T : Any> handleHttpFailure(
        e: HttpException,
        cacheKey: String,
        policy: com.example.data.remote.ProviderRatePolicy
    ): ProviderResult<T> {
        val stale = cacheLimiter.getStale<T>(cacheKey, policy)
        if (stale != null && e.code() != 401 && e.code() != 403) {
            return ProviderResult.Success(
                data = stale.value,
                sourceName = displayName,
                updatedAt = stale.updatedAtLabel,
                fromCache = true,
                isStale = true
            )
        }
        return when (e.code()) {
            401, 403 -> ProviderResult.Error(
                providerName = displayName,
                message = "ODPT API認証エラー (HTTP ${e.code()})。APIキーを確認してください。",
                isAuthError = true
            )
            429 -> ProviderResult.RateLimited(
                providerName = displayName,
                retryAfterSeconds = 60L,
                cachedFallback = stale?.value
            )
            else -> ProviderResult.Error(displayName, "ODPT通信エラー (HTTP ${e.code()})")
        }
    }

    private fun <T : Any> handleIoFailure(
        e: IOException,
        cacheKey: String,
        policy: com.example.data.remote.ProviderRatePolicy
    ): ProviderResult<T> {
        val stale = cacheLimiter.getStale<T>(cacheKey, policy)
        if (stale != null) {
            return ProviderResult.Success(
                data = stale.value,
                sourceName = displayName,
                updatedAt = stale.updatedAtLabel,
                fromCache = true,
                isStale = true
            )
        }
        return ProviderResult.Error(
            providerName = displayName,
            message = "ODPT接続タイムアウトまたはオフラインです: ${e.localizedMessage ?: "通信不可"}"
        )
    }
}
