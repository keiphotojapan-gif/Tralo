package com.example.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.core.PartnerLinks
import com.example.data.remote.NationalRailwayRegistry
import com.example.model.CoverageStatus
import com.example.model.ProviderResult
import com.example.model.RailwayLine
import com.example.model.RailwayStation
import com.example.model.Train
import com.example.model.TrainOperationStatus
import com.example.ui.theme.StatusDelayAmber
import com.example.ui.theme.TabularNumericMedium
import kotlin.math.roundToInt

@Composable
fun LiveTrainMapPanel(
    selectedLineId: String,
    stations: List<RailwayStation>,
    trainsResult: ProviderResult<List<Train>>?,
    operationStatusResult: ProviderResult<List<TrainOperationStatus>>?,
    isLoading: Boolean,
    selectedTrainId: String?,
    autoRefreshEnabled: Boolean,
    cooldownSeconds: Int,
    onSelectLine: (String) -> Unit,
    onSelectTrain: (String?) -> Unit,
    onToggleAutoRefresh: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedLine = remember(selectedLineId) {
        NationalRailwayRegistry.findLine(selectedLineId)
    }
    val selectedOperator = remember(selectedLine?.operatorId) {
        selectedLine?.let { NationalRailwayRegistry.findOperator(it.operatorId) }
    }
    var mapZoom by remember(selectedLineId) { mutableFloatStateOf(1f) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TraloCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedLine?.lineName ?: "全国鉄道リアルタイム走行位置",
                        style = MaterialTheme.typography.titleLarge
                    )
                    val opName = selectedLine?.let {
                        NationalRailwayRegistry.findOperator(it.operatorId)?.operatorName
                    } ?: "鉄道事業者"
                    Text(
                        text = opName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("自動更新", style = MaterialTheme.typography.labelMedium)
                        Switch(
                            checked = autoRefreshEnabled,
                            onCheckedChange = onToggleAutoRefresh,
                            modifier = Modifier.testTag("live_map_auto_refresh_switch")
                        )
                    }
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isLoading,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("live_map_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "列車位置を更新"
                        )
                    }
                }
            }

            if (cooldownSeconds > 0) {
                Text(
                    text = "APIレート制限保護のため次回更新まで約 ${cooldownSeconds}秒（キャッシュ有効）",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("路線切替（実データ対応路線 / 非公開検証路線）", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                items(NationalRailwayRegistry.lines, key = { it.lineId }) { line ->
                    val badge = if (line.realtimeSupported) "" else " [API無]"
                    FilterChip(
                        selected = line.lineId == selectedLineId,
                        onClick = { onSelectLine(line.lineId) },
                        label = { Text("${line.lineName}$badge") },
                        modifier = Modifier.testTag("line_chip_${line.lineCode}")
                    )
                }
            }
        }

        // Interactive Railway Route Canvas
        val activeTrains = (trainsResult as? ProviderResult.Success)?.data.orEmpty()
        val selectedTrain = activeTrains.firstOrNull { it.trainId == selectedTrainId }

        TraloCard(modifier = Modifier.testTag("live_train_map_card")) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    TraloStatusBadge(
                        label = when (selectedLine?.coverageStatus) {
                            CoverageStatus.IMPLEMENTED -> "位置API対応"
                            CoverageStatus.LIMITED -> "一部線区対応"
                            else -> "位置API非公開"
                        },
                        tone = when (selectedLine?.coverageStatus) {
                            CoverageStatus.IMPLEMENTED -> BadgeTone.FOREST
                            CoverageStatus.LIMITED -> BadgeTone.GOLD
                            else -> BadgeTone.NEUTRAL
                        }
                    )
                    if (trainsResult is ProviderResult.Success) {
                        TraloStatusBadge(
                            label = "在線 ${trainsResult.data.size}本 (${trainsResult.updatedAt})",
                            tone = if (trainsResult.isStale) BadgeTone.AMBER else BadgeTone.BLUE
                        )
                    }
                }
                Row {
                    IconButton(
                        onClick = { mapZoom = (mapZoom - 0.2f).coerceAtLeast(0.8f) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Outlined.ZoomOut, contentDescription = "地図を縮小")
                    }
                    IconButton(
                        onClick = { mapZoom = (mapZoom + 0.25f).coerceAtMost(2.2f) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Outlined.ZoomIn, contentDescription = "地図を拡大")
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            TrainGeoCanvas(
                line = selectedLine,
                stations = stations,
                trains = activeTrains,
                selectedTrainId = selectedTrainId,
                zoomFactor = mapZoom,
                onSelectTrain = { train ->
                    onSelectTrain(if (selectedTrainId == train.trainId) null else train.trainId)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MapLegendDot(color = MaterialTheme.colorScheme.primary, label = "平常運行")
                MapLegendDot(color = StatusDelayAmber, label = "遅延 (1〜9分)")
                MapLegendDot(color = MaterialTheme.colorScheme.error, label = "大幅遅延 (10分以上)")
            }
        }

        // Selected Train Detail Panel
        if (selectedTrain != null) {
            SelectedTrainDetailCard(
                train = selectedTrain,
                onClose = { onSelectTrain(null) }
            )
        }

        // Provider State & Honest Banners
        if (isLoading) {
            TraloSkeletonCard()
        } else {
            when (trainsResult) {
                null -> {
                    TraloInfoNotice(
                        title = "データ待機中",
                        body = "更新ボタンを押すと選択路線のリアルタイム列車位置を取得します。"
                    )
                }
                is ProviderResult.NotConfigured -> {
                    TraloInfoNotice(
                        title = "${trainsResult.providerName} 未設定（仮想列車は表示しません）",
                        body = trainsResult.reason
                    )
                    selectedOperator?.officialInfoUrl?.let { url ->
                        OutlinedButton(
                            onClick = { PartnerLinks.openExternalUrl(context, url) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("live_map_official_web_button")
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${selectedOperator.operatorName} 公式運行・走行位置Webを開く")
                        }
                    }
                }
                is ProviderResult.Unsupported -> {
                    TraloInfoNotice(
                        title = "列車走行位置API 非公開・利用規約保護路線",
                        body = trainsResult.reason
                    )
                    selectedOperator?.officialInfoUrl?.let { url ->
                        OutlinedButton(
                            onClick = { PartnerLinks.openExternalUrl(context, url) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("live_map_official_web_button")
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${selectedOperator.operatorName} 公式走行位置Webをブラウザで開く")
                        }
                    }
                }
                is ProviderResult.RateLimited -> {
                    TraloInfoNotice(
                        title = "APIリクエスト間隔制限",
                        body = "${trainsResult.providerName} の更新頻度を保護しています（約${trainsResult.retryAfterSeconds}秒後に再取得可能）。"
                    )
                }
                is ProviderResult.Error -> {
                    TraloErrorCard(message = trainsResult.message, onRetry = onRefresh)
                }
                is ProviderResult.Success -> {
                    if (trainsResult.isStale) {
                        TraloInfoNotice(
                            title = "キャッシュデータを表示中（最終更新 ${trainsResult.updatedAt}）",
                            body = "オフラインまたはAPI制限中のため一時保存データを表示しています。回復後に更新してください。"
                        )
                    }
                    if (trainsResult.data.isEmpty()) {
                        TraloEmptyState(
                            icon = Icons.Outlined.Train,
                            title = "現在取得可能な列車在線データがありません",
                            description = "営業時間外・回送時間帯、または選択路線で現在リアルタイム在線列車が配信されていません。"
                        )
                    } else {
                        TraloCard {
                            Text(
                                text = "在線列車一覧（タップで詳細表示・${trainsResult.data.size}本）",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            trainsResult.data.forEachIndexed { idx, train ->
                                val isSel = train.trainId == selectedTrainId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.small)
                                        .background(
                                            if (isSel) MaterialTheme.colorScheme.secondaryContainer
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            onSelectTrain(if (isSel) null else train.trainId)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 10.dp)
                                        .testTag("live_train_row_${train.trainNumber}"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${train.trainNumber} [${train.trainType}] → ${train.destination.ifBlank { train.direction }}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        val seg = if (train.nextStation.isNotBlank()) {
                                            "${train.currentStation} → ${train.nextStation}"
                                        } else {
                                            "${train.currentStation} 付近"
                                        }
                                        Text(
                                            text = "$seg ・ 更新 ${train.lastUpdated}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TraloStatusBadge(
                                        label = train.realtimeStatus,
                                        tone = when {
                                            train.delayMinutes >= 10 -> BadgeTone.AMBER
                                            train.delayMinutes > 0 -> BadgeTone.GOLD
                                            else -> BadgeTone.FOREST
                                        }
                                    )
                                }
                                if (idx < trainsResult.data.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Operation Status List
        if (operationStatusResult is ProviderResult.Success && operationStatusResult.data.isNotEmpty()) {
            TraloCard {
                Text(
                    text = "運行情報（${operationStatusResult.sourceName}）",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                operationStatusResult.data.take(8).forEach { status ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = status.lineName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = status.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TraloStatusBadge(
                            label = status.status,
                            tone = if (status.isDelayed) BadgeTone.AMBER else BadgeTone.FOREST
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainGeoCanvas(
    line: RailwayLine?,
    stations: List<RailwayStation>,
    trains: List<Train>,
    selectedTrainId: String?,
    zoomFactor: Float,
    onSelectTrain: (Train) -> Unit
) {
    val lineColor = remember(line?.lineColorHex) {
        parseHexColorOrDefault(line?.lineColorHex, Color(0xFF1B4332))
    }
    val surfaceBg = MaterialTheme.colorScheme.surfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val stationTextColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    val allPoints = remember(stations, trains) {
        val stPts = stations.map { it.latitude to it.longitude }
        val trPts = trains.mapNotNull { it.actualPosition?.let { p -> p.latitude to p.longitude } }
        stPts + trPts
    }

    val bounds = remember(allPoints, zoomFactor) {
        if (allPoints.isEmpty()) {
            QuadBounds(35.62, 35.73, 139.68, 139.82)
        } else {
            val minLat = allPoints.minOf { it.first }
            val maxLat = allPoints.maxOf { it.first }
            val minLng = allPoints.minOf { it.second }
            val maxLng = allPoints.maxOf { it.second }
            val centerLat = (minLat + maxLat) / 2.0
            val centerLng = (minLng + maxLng) / 2.0
            val spanLat = ((maxLat - minLat).coerceAtLeast(0.015) * 1.28) / zoomFactor
            val spanLng = ((maxLng - minLng).coerceAtLeast(0.015) * 1.28) / zoomFactor
            QuadBounds(
                minLat = centerLat - spanLat / 2.0,
                maxLat = centerLat + spanLat / 2.0,
                minLng = centerLng - spanLng / 2.0,
                maxLng = centerLng + spanLng / 2.0
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(276.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(surfaceBg)
            .border(1.dp, gridColor, MaterialTheme.shapes.medium)
            .testTag("live_train_map_canvas")
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val padPx = with(density) { 26.dp.toPx() }

        fun project(lat: Double, lng: Double): Offset {
            val xRatio = ((lng - bounds.minLng) / (bounds.maxLng - bounds.minLng).coerceAtLeast(0.0001)).toFloat()
            val yRatio = (1.0 - (lat - bounds.minLat) / (bounds.maxLat - bounds.minLat).coerceAtLeast(0.0001)).toFloat()
            val x = padPx + xRatio * (widthPx - padPx * 2f)
            val y = padPx + yRatio * (heightPx - padPx * 2f)
            return Offset(x, y)
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Subtle topographic coordinate grid
            val cols = 4
            val rows = 4
            for (i in 1 until cols) {
                val x = size.width * i / cols
                drawLine(gridColor.copy(alpha = 0.45f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
            }
            for (j in 1 until rows) {
                val y = size.height * j / rows
                drawLine(gridColor.copy(alpha = 0.45f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }

            // Route polyline through stations
            if (stations.size >= 2) {
                val path = Path()
                stations.forEachIndexed { idx, st ->
                    val pt = project(st.latitude, st.longitude)
                    if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // Station nodes and labels
            val textPaint = Paint().apply {
                color = stationTextColor
                textSize = 11.dp.toPx()
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            val labelStep = if (stations.size > 10) 2 else 1
            stations.forEachIndexed { idx, st ->
                val pt = project(st.latitude, st.longitude)
                drawCircle(color = Color.White, radius = 5.5.dp.toPx(), center = pt)
                drawCircle(color = lineColor, radius = 3.5.dp.toPx(), center = pt)
                if (idx % labelStep == 0 || idx == stations.lastIndex) {
                    drawContext.canvas.nativeCanvas.drawText(
                        st.stationName,
                        pt.x,
                        (pt.y + 15.dp.toPx()).coerceAtMost(size.height - 6.dp.toPx()),
                        textPaint
                    )
                }
            }
        }

        // Interactive train markers with minimum 48dp accessible touch target
        val markerHalfPx = with(density) { 24.dp.toPx() }
        trains.forEach { train ->
            val pos = train.actualPosition ?: return@forEach
            val offset = project(pos.latitude, pos.longitude)
            if (offset.x in -20f..(widthPx + 20f) && offset.y in -20f..(heightPx + 20f)) {
                val isSelected = train.trainId == selectedTrainId
                val badgeColor = when {
                    train.delayMinutes >= 10 -> errorColor
                    train.delayMinutes > 0 -> StatusDelayAmber
                    else -> primaryColor
                }
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (offset.x - markerHalfPx).roundToInt(),
                                (offset.y - markerHalfPx).roundToInt()
                            )
                        }
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onSelectTrain(train) }
                        .semantics {
                            contentDescription = "列車 ${train.trainNumber} ${train.destination}行き ${train.realtimeStatus}"
                        }
                        .testTag("map_train_marker_${train.trainNumber}"),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = badgeColor,
                        shadowElevation = if (isSelected) 6.dp else 2.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.5.dp else 1.5.dp,
                            color = Color.White
                        ),
                        modifier = Modifier.size(if (isSelected) 30.dp else 24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Navigation,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(14.dp)
                                    .rotate(pos.bearing)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedTrainDetailCard(
    train: Train,
    onClose: () -> Unit
) {
    TraloCard(modifier = Modifier.testTag("selected_train_detail_card")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "列車 ${train.trainNumber} ・ ${train.trainType}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${train.destination.ifBlank { train.direction }} 方面",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TraloStatusBadge(
                    label = train.realtimeStatus,
                    tone = if (train.delayMinutes > 0) BadgeTone.AMBER else BadgeTone.FOREST
                )
                IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "列車詳細を閉じる")
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        val sectionText = if (train.nextStation.isNotBlank()) {
            "${train.currentStation} → ${train.nextStation} 走行中"
        } else {
            "${train.currentStation} 駅停車・付近"
        }
        Text(text = "現在区間: $sectionText", style = MaterialTheme.typography.bodyMedium)
        train.actualPosition?.let { pos ->
            Text(
                text = "座標: %.4f, %.4f ・ 進行方向 %.0f°".format(pos.latitude, pos.longitude, pos.bearing),
                style = TabularNumericMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "取得元: ${pos.dataSource} ・ 更新 ${pos.timestamp}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MapLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

private data class QuadBounds(
    val minLat: Double,
    val maxLat: Double,
    val minLng: Double,
    val maxLng: Double
)

private fun parseHexColorOrDefault(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrDefault(fallback)
}
