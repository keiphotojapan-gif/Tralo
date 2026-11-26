package com.example.data.provider

import com.example.core.TransitApiConfigs
import com.example.data.remote.NationalRailwayRegistry
import com.example.model.ProviderCapability
import com.example.model.ProviderResult
import com.example.model.RailwayOperator
import com.example.model.RouteResult
import com.example.model.RouteSearchRequest
import com.example.model.Train
import com.example.model.TrainOperationStatus

class JorudanProvider(
    private val isConfiguredCheck: () -> Boolean = { TransitApiConfigs.isJorudanConfigured() }
) : RailwayRealtimeProvider {
    override val providerId: String = "jorudan"
    override val displayName: String = "ジョルダン 乗換案内Biz API / オープンAPI"

    override fun getCapabilities(): Set<ProviderCapability> = setOf(
        ProviderCapability.ROUTE_SEARCH,
        ProviderCapability.OPERATION_STATUS
    )

    override fun getSupportedOperators(): List<RailwayOperator> =
        NationalRailwayRegistry.operators

    override suspend fun searchRoutes(query: RouteSearchRequest): ProviderResult<List<RouteResult>> {
        return if (!isConfiguredCheck()) {
            ProviderResult.NotConfigured(
                providerName = displayName,
                reason = "ジョルダン 乗換案内Biz API 契約キー（JORUDAN_API_KEY）または公開Web審査済みオープンAPI登録が必要です。"
            )
        } else {
            ProviderResult.Unsupported(
                providerName = displayName,
                reason = "ジョルダンBiz API契約仕様に基づくエンドポイントURL構成を接続してください。"
            )
        }
    }
}

class NavitimeProvider(
    private val isConfiguredCheck: () -> Boolean = { TransitApiConfigs.isNavitimeConfigured() }
) : RailwayRealtimeProvider {
    override val providerId: String = "navitime"
    override val displayName: String = "NAVITIME API 2.0"

    override fun getCapabilities(): Set<ProviderCapability> = setOf(
        ProviderCapability.ROUTE_SEARCH,
        ProviderCapability.OPERATION_STATUS,
        ProviderCapability.STATION_TIMETABLE
    )

    override fun getSupportedOperators(): List<RailwayOperator> =
        NationalRailwayRegistry.operators

    override suspend fun searchRoutes(query: RouteSearchRequest): ProviderResult<List<RouteResult>> {
        return if (!isConfiguredCheck()) {
            ProviderResult.NotConfigured(
                providerName = displayName,
                reason = "NAVITIME API 2.0 法人契約（またはRapidAPI契約）と NAVITIME_API_KEY の設定が必要です。"
            )
        } else {
            ProviderResult.Unsupported(
                providerName = displayName,
                reason = "NAVITIME API 2.0 の契約ホストURL（CID紐付け）構成が未指定です。"
            )
        }
    }
}

class OperatorOfficialProvider : RailwayRealtimeProvider {
    override val providerId: String = "operator_official"
    override val displayName: String = "JR・各鉄道事業者公式連携基盤 (RT-DIP等)"

    override fun getCapabilities(): Set<ProviderCapability> = setOf(
        ProviderCapability.OPERATION_STATUS
    )

    override fun getSupportedOperators(): List<RailwayOperator> =
        NationalRailwayRegistry.operators.filter { it.odptOperatorId.isNullOrBlank() }

    override suspend fun getTrainPositions(lineId: String): ProviderResult<List<Train>> {
        val line = NationalRailwayRegistry.findLine(lineId)
        val op = line?.let { NationalRailwayRegistry.findOperator(it.operatorId) }
        val reason = line?.unsupportedReason
            ?: op?.statusReason
            ?: "この鉄道事業者は一般向けリアルタイム列車位置APIを公開していません（RT-DIP等の法人契約または公式Web限定）。"
        return ProviderResult.Unsupported(displayName, reason)
    }

    override suspend fun getOperationStatus(operatorId: String?): ProviderResult<List<TrainOperationStatus>> {
        val op = operatorId?.let { NationalRailwayRegistry.findOperator(it) }
        return ProviderResult.Unsupported(
            providerName = displayName,
            reason = op?.statusReason ?: "JR各社（東海・西日本・北海道・四国・九州）等の公式運行情報APIは一般公開されていません。"
        )
    }
}

class GtfsRealtimeProvider : RailwayRealtimeProvider {
    override val providerId: String = "gtfs_rt"
    override val displayName: String = "GTFS / GTFS-Realtime (AIGID / 自治体OD)"

    override fun getCapabilities(): Set<ProviderCapability> = setOf(
        ProviderCapability.STATION_TIMETABLE,
        ProviderCapability.OPERATION_STATUS
    )

    override fun getSupportedOperators(): List<RailwayOperator> =
        NationalRailwayRegistry.operators.filter {
            it.apiProvider.contains("GTFS", ignoreCase = true)
        }

    override suspend fun getTrainPositions(lineId: String): ProviderResult<List<Train>> {
        return ProviderResult.Unsupported(
            providerName = displayName,
            reason = "対象事業者の公開データは静的GTFS（時刻表）中心であり、GTFS-RT VehiclePositions の常時公開フィードが確認できていません。"
        )
    }
}
