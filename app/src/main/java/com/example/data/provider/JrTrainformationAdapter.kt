package com.example.data.provider

import com.example.core.TransitApiConfigs
import com.example.data.remote.ApiCacheAndRateLimiter
import com.example.data.remote.JrTrainformationApi
import com.example.data.remote.JrTrainformationJsonParser
import com.example.data.remote.NationalRailwayRegistry
import com.example.data.remote.NetworkModule
import com.example.data.remote.ProviderRatePolicies
import com.example.model.ProviderCapability
import com.example.model.ProviderResult
import com.example.model.RailwayOperator
import com.example.model.StationTimetable
import com.example.model.Train
import com.example.model.TrainOperationStatus
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Direct connection and proxy-supported adapter for JR West train locations
 * (https://www.train-guide.westjr.co.jp/api/v3/{lineCode}.json).
 */
class JrTrainformationAdapter(
    private val apiFactory: (String) -> JrTrainformationApi = { baseUrl ->
        NetworkModule.createJrTrainformationApi(baseUrl = baseUrl)
    },
    private val proxyUrlProvider: () -> String = {
        TransitApiConfigs.jrTrainformationProxyUrl()
    },
    private val isProxyConfiguredCheck: () -> Boolean = {
        TransitApiConfigs.isJrTrainformationConfigured()
    },
    private val cacheLimiter: ApiCacheAndRateLimiter = ApiCacheAndRateLimiter(),
    private val timeFormatter: () -> String = {
        SimpleDateFormat("HH:mm", Locale.JAPAN).format(Date())
    }
) : RailwayRealtimeProvider {

    override val providerId: String = "jr_trainformation"
    override val displayName: String = "JR西日本 列車走行位置直結アダプター"

    private val supportedOperatorIds = setOf("op_jr_west", "op_jr_central", "op_jr_hokkaido")

    override fun getCapabilities(): Set<ProviderCapability> = setOf(
        ProviderCapability.TRAIN_POSITIONS,
        ProviderCapability.OPERATION_STATUS,
        ProviderCapability.STATION_TIMETABLE
    )

    override fun getSupportedOperators(): List<RailwayOperator> =
        NationalRailwayRegistry.operators.filter { it.operatorId in supportedOperatorIds }

    fun supportsLine(lineId: String): Boolean {
        val line = NationalRailwayRegistry.findLine(lineId)
        return line?.operatorId in supportedOperatorIds ||
            lineId.startsWith("jrw.") ||
            lineId.startsWith("jrc.") ||
            lineId.startsWith("jrh.")
    }

    fun supportsOperator(operatorId: String?): Boolean = operatorId in supportedOperatorIds

    private fun resolveOperatorIdForLine(lineId: String): String {
        NationalRailwayRegistry.findLine(lineId)?.operatorId?.let { return it }
        return when {
            lineId.startsWith("jrw.") -> "op_jr_west"
            lineId.startsWith("jrc.") -> "op_jr_central"
            lineId.startsWith("jrh.") -> "op_jr_hokkaido"
            else -> "op_jr_west"
        }
    }

    private fun toRemoteLineCode(lineId: String): String {
        return when (lineId) {
            "jrw.OsakaLoop" -> "osakaloop"
            "jrw.KyotoLine" -> "kyotokobe"
            else -> {
                val line = NationalRailwayRegistry.findLine(lineId)
                line?.lineCode?.lowercase(Locale.ROOT)?.ifBlank { lineId.substringAfterLast(".") }
                    ?: lineId.substringAfterLast(".")
            }
        }
    }

    override suspend fun getTrainPositions(lineId: String): ProviderResult<List<Train>> {
        val operatorId = resolveOperatorIdForLine(lineId)
        if (operatorId != "op_jr_west") {
            val op = NationalRailwayRegistry.findOperator(operatorId)
            val line = NationalRailwayRegistry.findLine(lineId)
            return ProviderResult.Unsupported(
                providerName = displayName,
                reason = line?.unsupportedReason
                    ?: op?.statusReason
                    ?: "${op?.operatorName ?: operatorId} は公式API非公開のため、公式Web案内ページをご利用ください。"
            )
        }

        // Determine active base URL: if proxy is configured, use it; otherwise connect directly to JR West public API endpoint
        val baseUrl = if (isProxyConfiguredCheck()) {
            proxyUrlProvider()
        } else {
            "https://www.train-guide.westjr.co.jp/"
        }

        val cacheKey = "jr_tf_trains_$lineId"
        cacheLimiter.getFresh<List<Train>>(cacheKey, ProviderRatePolicies.jrRealtime)?.let { cached ->
            return ProviderResult.Success(
                data = cached.value,
                sourceName = displayName,
                updatedAt = cached.updatedAtLabel,
                fromCache = true,
                isStale = false
            )
        }
        if (!cacheLimiter.canIssueNetworkRequest(cacheKey, ProviderRatePolicies.jrRealtime)) {
            cacheLimiter.getStale<List<Train>>(cacheKey, ProviderRatePolicies.jrRealtime)?.let { stale ->
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
                retryAfterSeconds = cacheLimiter.remainingCooldownSeconds(cacheKey, ProviderRatePolicies.jrRealtime)
            )
        }

        cacheLimiter.recordAttempt(cacheKey)
        return try {
            val api = apiFactory(baseUrl)
            val raw = api.getLineTrains(toRemoteLineCode(lineId)).use { it.string() }
            val nowStr = timeFormatter()
            val trains = JrTrainformationJsonParser.parseLineTrains(
                rawJson = raw,
                operatorId = operatorId,
                lineId = lineId,
                sourceName = displayName,
                fallbackTime = nowStr
            )
            cacheLimiter.put(cacheKey, trains, nowStr)
            ProviderResult.Success(trains, displayName, nowStr)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            handleHttpFailure(e, cacheKey)
        } catch (e: IOException) {
            handleIoFailure(e, cacheKey)
        } catch (e: Exception) {
            ProviderResult.Error(displayName, "JR西日本列車位置データの解析に失敗しました: ${e.localizedMessage ?: "不正なJSON"}")
        }
    }

    override suspend fun getOperationStatus(operatorId: String?): ProviderResult<List<TrainOperationStatus>> {
        val targetOp = operatorId ?: "op_jr_west"
        val op = NationalRailwayRegistry.findOperator(targetOp)
        return ProviderResult.Unsupported(
            providerName = displayName,
            reason = op?.statusReason ?: "JR西日本のリアルタイム運行情報は駅すぱあとAPIおよび公式Web案内で提供しています。"
        )
    }

    override suspend fun getStationTimetable(
        stationIdOrName: String,
        lineId: String?
    ): ProviderResult<List<StationTimetable>> {
        return ProviderResult.Unsupported(
            providerName = displayName,
            reason = "JR西日本の駅時刻表は駅すぱあとAPIまたはODPT連携をご利用ください。"
        )
    }

    private fun <T : Any> handleHttpFailure(e: HttpException, cacheKey: String): ProviderResult<T> {
        val stale = cacheLimiter.getStale<T>(cacheKey, ProviderRatePolicies.jrRealtime)
        if (stale != null && e.code() != 401 && e.code() != 403) {
            return ProviderResult.Success(stale.value, displayName, stale.updatedAtLabel, fromCache = true, isStale = true)
        }
        return when (e.code()) {
            401, 403 -> ProviderResult.Error(displayName, "接続認証エラー (HTTP ${e.code()})", isAuthError = true)
            429 -> ProviderResult.RateLimited(displayName, 60L, stale?.value)
            else -> ProviderResult.Error(displayName, "JR西日本通信エラー (HTTP ${e.code()})")
        }
    }

    private fun <T : Any> handleIoFailure(e: IOException, cacheKey: String): ProviderResult<T> {
        val stale = cacheLimiter.getStale<T>(cacheKey, ProviderRatePolicies.jrRealtime)
        if (stale != null) {
            return ProviderResult.Success(stale.value, displayName, stale.updatedAtLabel, fromCache = true, isStale = true)
        }
        return ProviderResult.Error(displayName, "JR西日本接続タイムアウトまたはオフライン: ${e.localizedMessage ?: "通信不可"}")
    }
}
