package com.example.data.provider

import com.example.core.TransitApiConfigs
import com.example.data.remote.ApiCacheAndRateLimiter
import com.example.data.remote.MixwayApi
import com.example.data.remote.MixwayJsonParser
import com.example.data.remote.NationalRailwayRegistry
import com.example.data.remote.NetworkModule
import com.example.data.remote.ProviderRatePolicies
import com.example.model.ProviderCapability
import com.example.model.ProviderResult
import com.example.model.RailwayOperator
import com.example.model.Train
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MixwayRealtimeProvider(
    private val mixwayApi: MixwayApi = NetworkModule.mixwayApi,
    private val cacheLimiter: ApiCacheAndRateLimiter = ApiCacheAndRateLimiter(),
    private val apiKeyProvider: () -> String = { TransitApiConfigs.mixwayApiKey() },
    private val isConfiguredCheck: () -> Boolean = { TransitApiConfigs.isMixwayConfigured() },
    private val timeFormatter: () -> String = {
        SimpleDateFormat("HH:mm", Locale.JAPAN).format(Date())
    }
) : RailwayRealtimeProvider {

    override val providerId: String = "mixway"
    override val displayName: String = "mixway API (リアルタイム便情報)"

    override fun getCapabilities(): Set<ProviderCapability> = setOf(
        ProviderCapability.REALTIME_TRIP,
        ProviderCapability.TRAIN_POSITIONS,
        ProviderCapability.OPERATION_STATUS
    )

    override fun getSupportedOperators(): List<RailwayOperator> =
        NationalRailwayRegistry.operators.filter {
            it.operatorId == "op_jr_east" || it.operatorId == "op_tokyu" || it.operatorId == "op_odakyu"
        }

    override suspend fun getRealtimeTrip(tripCode: String): ProviderResult<Train?> {
        if (!isConfiguredCheck()) {
            return ProviderResult.NotConfigured(
                providerName = displayName,
                reason = "MIXWAY_API_KEY および各交通事業者のリアルタイム利用許諾契約が必要です。"
            )
        }
        val cacheKey = "mixway_trip_$tripCode"
        cacheLimiter.getFresh<Train>(cacheKey, ProviderRatePolicies.mixwayRealtime)?.let { cached ->
            return ProviderResult.Success(cached.value, displayName, cached.updatedAtLabel, fromCache = true)
        }
        if (!cacheLimiter.canIssueNetworkRequest(cacheKey, ProviderRatePolicies.mixwayRealtime)) {
            return ProviderResult.RateLimited(
                providerName = displayName,
                retryAfterSeconds = cacheLimiter.remainingCooldownSeconds(cacheKey, ProviderRatePolicies.mixwayRealtime)
            )
        }
        cacheLimiter.recordAttempt(cacheKey)
        return try {
            val rawJson = mixwayApi.getRealtimeTrip(key = apiKeyProvider(), tripCode = tripCode).string()
            val nowStr = timeFormatter()
            val parsed = MixwayJsonParser.parseRealtimeTrip(rawJson, tripCode, nowStr)
            if (parsed != null) {
                cacheLimiter.put(cacheKey, parsed, nowStr)
            }
            ProviderResult.Success(parsed, displayName, nowStr)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            when (e.code()) {
                401, 403 -> ProviderResult.Error(displayName, "mixway API認証または事業者許諾エラー (HTTP ${e.code()})", isAuthError = true)
                429 -> ProviderResult.RateLimited(displayName, 30L)
                else -> ProviderResult.Error(displayName, "mixway API通信エラー (HTTP ${e.code()})")
            }
        } catch (e: IOException) {
            ProviderResult.Error(displayName, "mixway APIへの接続に失敗しました。")
        } catch (e: Exception) {
            ProviderResult.Error(displayName, "mixwayレスポンス解析エラー: ${e.localizedMessage ?: "不正なJSON"}")
        }
    }

    override suspend fun getTrainPosition(trainId: String): ProviderResult<Train?> =
        getRealtimeTrip(trainId)
}
