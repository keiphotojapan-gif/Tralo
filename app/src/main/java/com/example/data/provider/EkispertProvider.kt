package com.example.data.provider

import com.example.core.TransitApiConfigs
import com.example.data.remote.ApiCacheAndRateLimiter
import com.example.data.remote.EkispertApi
import com.example.data.remote.EkispertJsonParser
import com.example.data.remote.NationalRailwayRegistry
import com.example.data.remote.NetworkModule
import com.example.data.remote.ProviderRatePolicies
import com.example.model.ProviderCapability
import com.example.model.ProviderResult
import com.example.model.RailwayOperator
import com.example.model.RouteResult
import com.example.model.RouteSearchRequest
import com.example.model.StationTimetable
import com.example.model.TimeQueryMode
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EkispertProvider(
    private val ekispertApi: EkispertApi = NetworkModule.ekispertApi,
    private val cacheLimiter: ApiCacheAndRateLimiter = ApiCacheAndRateLimiter(),
    private val apiKeyProvider: () -> String = { TransitApiConfigs.ekispertApiKey() },
    private val isConfiguredCheck: () -> Boolean = { TransitApiConfigs.isEkispertConfigured() },
    private val timeFormatter: () -> String = {
        SimpleDateFormat("HH:mm", Locale.JAPAN).format(Date())
    }
) : RailwayRealtimeProvider {

    override val providerId: String = "ekispert"
    override val displayName: String = "駅すぱあと API"

    override fun getCapabilities(): Set<ProviderCapability> = setOf(
        ProviderCapability.ROUTE_SEARCH,
        ProviderCapability.STATION_TIMETABLE
    )

    override fun getSupportedOperators(): List<RailwayOperator> =
        NationalRailwayRegistry.operators.filter {
            ProviderCapability.ROUTE_SEARCH in it.capabilities
        }

    override suspend fun searchRoutes(query: RouteSearchRequest): ProviderResult<List<RouteResult>> {
        if (!isConfiguredCheck()) {
            return ProviderResult.NotConfigured(
                providerName = displayName,
                reason = "EKISPERT_API_KEY が未設定のため、標準ダイヤ・概算ルートを表示しています。"
            )
        }

        val cacheKey = "eki_route_${query.origin}_${query.destination}_${query.dateYyyyMmDd}_${query.timeHhMm}_${query.timeMode.name}"
        cacheLimiter.getFresh<List<RouteResult>>(cacheKey, ProviderRatePolicies.ekispertRoute)?.let { cached ->
            return ProviderResult.Success(cached.value, displayName, cached.updatedAtLabel, fromCache = true)
        }

        cacheLimiter.recordAttempt(cacheKey)
        return try {
            val key = apiKeyProvider()
            val originCode = resolveStationCode(key, query.origin) ?: query.origin
            val destCode = resolveStationCode(key, query.destination) ?: query.destination
            val searchType = when (query.timeMode) {
                TimeQueryMode.ARRIVE_BY -> "arrival"
                TimeQueryMode.FIRST_TRAIN -> "firstTrain"
                TimeQueryMode.LAST_TRAIN -> "lastTrain"
                TimeQueryMode.DEPART_AT -> "departure"
            }
            val rawJson = ekispertApi.searchCourseExtreme(
                key = key,
                viaList = "$originCode:$destCode",
                date = query.dateYyyyMmDd,
                time = query.timeHhMm,
                searchType = searchType
            ).string()

            val parsed = EkispertJsonParser.parseCourses(
                json = rawJson,
                fallbackOrigin = query.origin,
                fallbackDestination = query.destination
            )
            val nowStr = timeFormatter()
            cacheLimiter.put(cacheKey, parsed, nowStr)
            ProviderResult.Success(parsed, displayName, nowStr)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            when (e.code()) {
                401, 403 -> ProviderResult.Error(displayName, "駅すぱあとAPI認証エラー (HTTP ${e.code()})", isAuthError = true)
                429 -> ProviderResult.RateLimited(displayName, 30L)
                else -> ProviderResult.Error(displayName, "駅すぱあとAPI通信エラー (HTTP ${e.code()})")
            }
        } catch (e: IOException) {
            val stale = cacheLimiter.getStale<List<RouteResult>>(cacheKey, ProviderRatePolicies.ekispertRoute)
            if (stale != null) {
                ProviderResult.Success(stale.value, displayName, stale.updatedAtLabel, fromCache = true, isStale = true)
            } else {
                ProviderResult.Error(displayName, "駅すぱあとAPIへの接続がタイムアウトまたは失敗しました。")
            }
        } catch (e: Exception) {
            ProviderResult.Error(displayName, "駅すぱあとレスポンス解析に失敗しました: ${e.localizedMessage ?: "不正なJSON"}")
        }
    }

    override suspend fun getStationTimetable(
        stationIdOrName: String,
        lineId: String?
    ): ProviderResult<List<StationTimetable>> {
        if (!isConfiguredCheck()) {
            return ProviderResult.NotConfigured(
                providerName = displayName,
                reason = "EKISPERT_API_KEY を設定すると全国の駅時刻表を取得できます。"
            )
        }
        val cleanName = NationalRailwayRegistry.findStationByIdOrName(stationIdOrName)?.stationName
            ?: stationIdOrName.substringAfterLast(".")
        val cacheKey = "eki_tt_${cleanName}_${lineId ?: "all"}"
        cacheLimiter.getFresh<List<StationTimetable>>(cacheKey, ProviderRatePolicies.staticCatalog)?.let { cached ->
            return ProviderResult.Success(cached.value, displayName, cached.updatedAtLabel, fromCache = true)
        }

        cacheLimiter.recordAttempt(cacheKey)
        return try {
            val key = apiKeyProvider()
            val firstJson = ekispertApi.getOperationLineTimetable(
                key = key,
                stationName = cleanName
            ).string()
            val directEntries = EkispertJsonParser.parseTimetableEntries(firstJson, stationIdOrName, cleanName)
            val entries = if (directEntries.isNotEmpty()) {
                directEntries
            } else {
                val codes = EkispertJsonParser.parseTimetableCodes(firstJson)
                val targetCode = codes.firstOrNull()
                if (targetCode != null) {
                    val detailJson = ekispertApi.getOperationLineTimetable(
                        key = key,
                        stationName = cleanName,
                        timetableCode = targetCode
                    ).string()
                    EkispertJsonParser.parseTimetableEntries(detailJson, stationIdOrName, cleanName)
                } else emptyList()
            }
            val nowStr = timeFormatter()
            cacheLimiter.put(cacheKey, entries, nowStr)
            ProviderResult.Success(entries, displayName, nowStr)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            when (e.code()) {
                401, 403 -> ProviderResult.Error(displayName, "駅すぱあと時刻表認証エラー (HTTP ${e.code()})", isAuthError = true)
                429 -> ProviderResult.RateLimited(displayName, 30L)
                else -> ProviderResult.Error(displayName, "駅すぱあと時刻表エラー (HTTP ${e.code()})")
            }
        } catch (e: IOException) {
            ProviderResult.Error(displayName, "駅すぱあと時刻表通信に失敗しました。")
        } catch (e: Exception) {
            ProviderResult.Error(displayName, "駅すぱあと時刻表解析に失敗しました: ${e.localizedMessage ?: "不正なJSON"}")
        }
    }

    private suspend fun resolveStationCode(key: String, name: String): String? {
        return runCatching {
            val json = ekispertApi.searchStations(key = key, name = name).string()
            EkispertJsonParser.parseStationCodes(json).firstOrNull()?.first
        }.getOrNull()
    }
}
