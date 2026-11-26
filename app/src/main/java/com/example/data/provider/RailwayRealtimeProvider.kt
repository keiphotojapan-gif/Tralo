package com.example.data.provider

import com.example.model.ProviderCapability
import com.example.model.ProviderResult
import com.example.model.RailwayOperator
import com.example.model.RailwayStation
import com.example.model.RouteResult
import com.example.model.RouteSearchRequest
import com.example.model.StationTimetable
import com.example.model.Train
import com.example.model.TrainOperationStatus
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

interface RailwayRealtimeProvider {
    val providerId: String
    val displayName: String

    fun getCapabilities(): Set<ProviderCapability>
    fun getSupportedOperators(): List<RailwayOperator>

    suspend fun getTrainPositions(lineId: String): ProviderResult<List<Train>> =
        ProviderResult.Unsupported(displayName, "このプロバイダーは列車走行位置の取得に対応していません。")

    suspend fun getTrainPosition(trainId: String): ProviderResult<Train?> =
        ProviderResult.Unsupported(displayName, "このプロバイダーは列車単体の位置取得に対応していません。")

    suspend fun getOperationStatus(operatorId: String? = null): ProviderResult<List<TrainOperationStatus>> =
        ProviderResult.Unsupported(displayName, "このプロバイダーは運行情報の取得に対応していません。")

    suspend fun getStationTimetable(
        stationIdOrName: String,
        lineId: String? = null
    ): ProviderResult<List<StationTimetable>> =
        ProviderResult.Unsupported(displayName, "このプロバイダーは駅時刻表の取得に対応していません。")

    suspend fun getRealtimeTrip(tripCode: String): ProviderResult<Train?> =
        ProviderResult.Unsupported(displayName, "このプロバイダーは便単位リアルタイム情報に対応していません。")

    suspend fun searchRoutes(query: RouteSearchRequest): ProviderResult<List<RouteResult>> =
        ProviderResult.Unsupported(displayName, "このプロバイダーは経路検索に対応していません。")
}

object GeoTrackMath {
    fun bearingDegrees(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double): Float {
        val phi1 = Math.toRadians(fromLat)
        val phi2 = Math.toRadians(toLat)
        val dLambda = Math.toRadians(toLng - fromLng)
        val y = sin(dLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLambda)
        val theta = Math.toDegrees(atan2(y, x))
        return ((theta + 360.0) % 360.0).toFloat()
    }

    fun interpolatePosition(
        from: RailwayStation?,
        to: RailwayStation?,
        progress: Float = 0.5f
    ): Pair<Double, Double>? {
        return when {
            from != null && to != null -> {
                val t = progress.coerceIn(0f, 1f).toDouble()
                val lat = from.latitude + (to.latitude - from.latitude) * t
                val lng = from.longitude + (to.longitude - from.longitude) * t
                lat to lng
            }
            from != null -> from.latitude to from.longitude
            to != null -> to.latitude to to.longitude
            else -> null
        }
    }
}
