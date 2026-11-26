package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.PartnerLinks
import com.example.data.remote.NationalRailwayRegistry
import com.example.model.CoverageStatus
import com.example.model.ProviderCapability
import com.example.model.ProviderResult
import com.example.model.RailwayOperator
import com.example.model.StationTimetable
import com.example.ui.theme.TabularNumericLarge
import com.example.ui.theme.TabularNumericMedium

@Composable
fun StationTimetableSection(
    stationQuery: String,
    timetableResult: ProviderResult<List<StationTimetable>>?,
    isLoading: Boolean,
    onStationQueryChange: (String) -> Unit,
    onSearchTimetable: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickStations = remember {
        listOf(
            "渋谷" to "odpt.Railway:TokyoMetro.Ginza",
            "銀座" to "odpt.Railway:TokyoMetro.Ginza",
            "東京" to "odpt.Railway:TokyoMetro.Marunouchi",
            "新宿" to "odpt.Railway:Odakyu.Odawara",
            "京都" to null,
            "新大阪" to null
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TraloCard {
            Text("駅時刻表検索（ODPT / 駅すぱあとAPI）", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = stationQuery,
                onValueChange = onStationQueryChange,
                label = { Text("駅名または ODPT 駅ID") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("timetable_station_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(quickStations) { (name, lineId) ->
                    FilterChip(
                        selected = stationQuery == name,
                        onClick = {
                            onStationQueryChange(name)
                            onSearchTimetable(name, lineId)
                        },
                        label = { Text(name) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onSearchTimetable(stationQuery, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("timetable_search_button")
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("この駅の時刻表を取得")
            }
        }

        if (isLoading) {
            TraloSkeletonCard()
        } else {
            when (timetableResult) {
                null -> {
                    TraloInfoNotice(
                        title = "駅を選択してください",
                        body = "ODPT対応路線は ODPT_API_KEY、全国私鉄・JR各社は EKISPERT_API_KEY を設定すると実時刻表を取得できます。"
                    )
                }
                is ProviderResult.NotConfigured -> {
                    TraloInfoNotice(
                        title = "${timetableResult.providerName} 未設定",
                        body = timetableResult.reason
                    )
                }
                is ProviderResult.Unsupported -> {
                    TraloInfoNotice(
                        title = "時刻表API対象外または駅特定不可",
                        body = timetableResult.reason
                    )
                }
                is ProviderResult.RateLimited -> {
                    TraloInfoNotice(
                        title = "時刻表リクエスト制限",
                        body = "${timetableResult.retryAfterSeconds}秒後に再取得してください。"
                    )
                }
                is ProviderResult.Error -> {
                    TraloErrorCard(
                        message = timetableResult.message,
                        onRetry = { onSearchTimetable(stationQuery, null) }
                    )
                }
                is ProviderResult.Success -> {
                    if (timetableResult.data.isEmpty()) {
                        TraloEmptyState(
                            icon = Icons.Outlined.Schedule,
                            title = "該当する時刻表データが見つかりませんでした",
                            description = "別の駅名（例: 渋谷・東京・新宿）でお試しください。"
                        )
                    } else {
                        TraloCard(modifier = Modifier.testTag("timetable_results_card")) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${timetableResult.data.first().stationName} 発車時刻表（${timetableResult.data.size}件）",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                TraloStatusBadge(
                                    label = timetableResult.sourceName,
                                    tone = BadgeTone.FOREST
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            timetableResult.data.take(25).forEachIndexed { idx, entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = entry.departureTime,
                                            style = TabularNumericLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Column {
                                            Text(
                                                text = "${entry.trainType} ・ ${entry.destination} 行き",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            val meta = buildList {
                                                add(entry.calendarType)
                                                if (entry.direction.isNotBlank()) add(entry.direction)
                                                if (entry.platform.isNotBlank()) add("${entry.platform}番線")
                                            }.joinToString(" / ")
                                            Text(
                                                text = meta,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (entry.trainNumber.isNotBlank()) {
                                        Text(
                                            text = entry.trainNumber,
                                            style = TabularNumericMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (idx < timetableResult.data.take(25).lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OperatorCoverageSection(
    searchQuery: String,
    selectedRegion: String,
    onSearchQueryChange: (String) -> Unit,
    onRegionChange: (String) -> Unit,
    onInspectOperator: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val regions = remember {
        listOf("すべて", "首都圏", "近畿", "東海", "北海道", "東北", "北陸", "中国", "四国", "九州", "沖縄")
    }
    val allOperators = NationalRailwayRegistry.operators
    val filtered = remember(searchQuery, selectedRegion) {
        allOperators.filter { op ->
            val regionMatch = selectedRegion == "すべて" ||
                op.supportedRegions.any { it.contains(selectedRegion) }
            val q = searchQuery.trim().lowercase()
            val textMatch = q.isBlank() ||
                op.operatorName.lowercase().contains(q) ||
                op.operatorCode.lowercase().contains(q) ||
                op.apiProvider.lowercase().contains(q)
            regionMatch && textMatch
        }
    }
    val positionCapableCount = remember {
        allOperators.count { ProviderCapability.TRAIN_POSITIONS in it.capabilities }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TraloCard {
            Text(
                text = "全国鉄道事業者 API 対応状況（${allOperators.size}社局調査済）",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TraloStatusBadge(label = "位置API連携可能: ${positionCapableCount}社局", tone = BadgeTone.FOREST)
                TraloStatusBadge(label = "表示中: ${filtered.size}社局", tone = BadgeTone.BLUE)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("鉄道会社名・API名で絞り込み") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("coverage_search_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(regions) { region ->
                    FilterChip(
                        selected = selectedRegion == region,
                        onClick = { onRegionChange(region) },
                        label = { Text(region) }
                    )
                }
            }
        }

        filtered.forEach { op ->
            OperatorCoverageCard(
                operator = op,
                onInspectOnMap = { onInspectOperator(op.operatorId) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OperatorCoverageCard(
    operator: RailwayOperator,
    onInspectOnMap: () -> Unit
) {
    val context = LocalContext.current
    val hasLine = NationalRailwayRegistry.linesForOperator(operator.operatorId).isNotEmpty()
    TraloCard(modifier = Modifier.testTag("operator_card_${operator.operatorId}")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = operator.operatorName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${operator.supportedRegions.joinToString("・")} / ${operator.apiProvider}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            TraloStatusBadge(
                label = operator.coverageStatus.labelJa,
                tone = when (operator.coverageStatus) {
                    CoverageStatus.IMPLEMENTED -> BadgeTone.FOREST
                    CoverageStatus.LIMITED, CoverageStatus.AVAILABLE -> BadgeTone.GOLD
                    CoverageStatus.AUTH_REQUIRED, CoverageStatus.CONTRACT_REQUIRED, CoverageStatus.OPERATOR_PERMISSION_REQUIRED -> BadgeTone.AMBER
                    CoverageStatus.UNAVAILABLE, CoverageStatus.INVESTIGATING -> BadgeTone.NEUTRAL
                }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            operator.capabilities.forEach { cap ->
                TraloStatusBadge(label = cap.labelJa, tone = BadgeTone.BLUE)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = operator.statusReason,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (hasLine || !operator.officialInfoUrl.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasLine) {
                    OutlinedButton(
                        onClick = onInspectOnMap,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Outlined.Train, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("走行位置マップで確認")
                    }
                }
                operator.officialInfoUrl?.let { url ->
                    OutlinedButton(
                        onClick = { PartnerLinks.openExternalUrl(context, url) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("公式Web")
                    }
                }
            }
        }
    }
}
