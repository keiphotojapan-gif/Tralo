package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DirectionsTransit
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.core.PartnerLinks
import com.example.model.ChecklistItem
import com.example.model.ItineraryCategory
import com.example.model.ItineraryItem
import com.example.model.OdptApiState
import com.example.model.Trip
import com.example.model.WeatherForecast
import com.example.ui.components.BadgeTone
import com.example.ui.components.TraloCard
import com.example.ui.components.TraloEditorialSectionHeader
import com.example.ui.components.TraloEmptyState
import com.example.ui.components.TraloErrorCard
import com.example.ui.components.TraloInfoNotice
import com.example.ui.components.TraloSkeletonCard
import com.example.ui.components.TraloStatusBadge
import com.example.ui.components.TraloWeatherCard
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.TabularNumericLarge
import com.example.ui.theme.TabularNumericMedium
import com.example.viewmodel.UiState

@Composable
fun TodayScreen(
    activeTrip: Trip?,
    itemsState: UiState<List<ItineraryItem>>,
    checklistState: UiState<List<ChecklistItem>>,
    weatherState: UiState<WeatherForecast>,
    odptState: OdptApiState,
    onCreateStarterTrip: () -> Unit,
    onOpenTransitTab: () -> Unit,
    onRefreshOdpt: () -> Unit,
    onRefreshWeather: () -> Unit,
    onAddChecklist: (String, String) -> Unit,
    onToggleChecklist: (String, String, Boolean) -> Unit,
    onDeleteChecklist: (String, String) -> Unit,
    onOpenConcierge: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var newChecklistInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("today_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TraloEditorialSectionHeader(
                englishOverline = "Travel Day Cockpit",
                japaneseTitle = "旅行当日・移動優先ビュー"
            )
        }

        if (activeTrip == null) {
            item {
                TraloEmptyState(
                    icon = Icons.Outlined.Today,
                    title = "当日の進行中旅程がありません",
                    description = "旅程タブで旅行を作成するか、京都・嵐山モデル旅程を作成して旅行当日モードを体験できます。",
                    actionLabel = "モデル旅程を作成",
                    onAction = onCreateStarterTrip
                )
            }
            item { Spacer(modifier = Modifier.height(72.dp)) }
            return@LazyColumn
        }

        val allItems = (itemsState as? UiState.Success)?.data.orEmpty()
        val todayItems = allItems.filter { it.dayIndex == 1L }.ifEmpty { allItems }
        val nextTransit = todayItems.firstOrNull { it.categoryEnum == ItineraryCategory.TRANSIT }
        val stayItem = allItems.firstOrNull { it.categoryEnum == ItineraryCategory.STAY }
        val ticketItems = allItems.filter {
            it.categoryEnum == ItineraryCategory.ACTIVITY || !it.bookingRef.isNullOrBlank()
        }
        val checklist = (checklistState as? UiState.Success)?.data.orEmpty()

        // 1. PRIORITY TRANSIT CARD (First thing seen on Travel Day)
        item {
            if (nextTransit != null) {
                TodayPriorityTransitCard(
                    item = nextTransit,
                    onOpenTransitTab = onOpenTransitTab
                )
            } else {
                TraloCard(containerColor = ForestGreen) {
                    Text(
                        text = "NEXT TRANSIT",
                        style = MaterialTheme.typography.labelSmall,
                        color = ChampagneGold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "本日の移動区間が未登録です",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "交通検索から乗車予定の列車・バスを旅程へ追加すると、発車時刻・番線・号車案内がここに優先表示されます。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FilledTonalButton(onClick = onOpenTransitTab) {
                        Icon(Icons.Outlined.DirectionsTransit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("交通検索を開く")
                    }
                }
            }
        }

        // 2. ODPT Live Status & Train Position
        item {
            TraloCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Train, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("運行情報・列車走行位置", style = MaterialTheme.typography.titleMedium)
                    }
                    IconButton(onClick = onRefreshOdpt, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "運行状況を更新")
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                when (odptState) {
                    OdptApiState.NotConfigured -> TraloInfoNotice(
                        title = "ODPT APIキー未設定",
                        body = "Secretsパネルに ODPT_API_KEY を設定すると、対象路線のリアルタイム列車位置と遅延情報が表示されます。"
                    )
                    OdptApiState.Loading -> TraloSkeletonCard()
                    is OdptApiState.Error -> TraloErrorCard(message = odptState.message, onRetry = onRefreshOdpt)
                    is OdptApiState.Available -> {
                        val delayed = odptState.trainInfos.filter { it.isDelayed }
                        if (delayed.isEmpty()) {
                            TraloStatusBadge(label = "取得路線 平常運転 (${odptState.updatedAt})", tone = BadgeTone.FOREST)
                        } else {
                            delayed.take(3).forEach { line ->
                                TraloStatusBadge(
                                    label = "${line.railwayTitle}: ${line.statusText}",
                                    tone = BadgeTone.AMBER
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                        if (odptState.liveTrains.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            val firstTrain = odptState.liveTrains.first()
                            Text(
                                text = "走行位置サンプル: 列車${firstTrain.trainNumber} (${firstTrain.fromStation}→${firstTrain.toStation})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OdptApiState.Idle -> Text("更新を押して運行状況を確認", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // 3. Trip Progress
        item {
            val doneCheckCount = checklist.count { it.checked }
            val totalProgress = if (checklist.isNotEmpty()) {
                doneCheckCount.toFloat() / checklist.size.toFloat()
            } else 0.5f

            TraloCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = activeTrip.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${activeTrip.destination} ・ 今日の予定 ${todayItems.size}件",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${(totalProgress * 100).toInt()}%",
                        style = TabularNumericMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { totalProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(MaterialTheme.shapes.extraSmall),
                    color = ChampagneGold,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        // 4. Today's Schedule Sequence
        item {
            TraloEditorialSectionHeader(
                englishOverline = "Today's Schedule",
                japaneseTitle = "本日のスケジュール"
            )
        }
        items(todayItems, key = { it.id }) { item ->
            TraloCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = item.startTime, style = TabularNumericMedium, color = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(text = item.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = item.locationName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TraloStatusBadge(label = item.bookingStatusEnum.labelJa, tone = BadgeTone.FOREST)
                }
            }
        }

        // 5. Stay & Reserved Tickets
        if (stayItem != null || ticketItems.isNotEmpty()) {
            item {
                TraloEditorialSectionHeader(
                    englishOverline = "Stays & Passes",
                    japaneseTitle = "宿泊・予約済みチケット"
                )
            }
            stayItem?.let { stay ->
                item {
                    TraloCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.Hotel, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("本日の宿泊", style = MaterialTheme.typography.titleMedium)
                            TraloStatusBadge(label = stay.bookingStatusEnum.labelJa, tone = BadgeTone.GOLD)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = stay.title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "${stay.locationName} ・ チェックイン目安 ${stay.startTime}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!stay.bookingRef.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "確認番号: ${stay.bookingRef}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val url = stay.bookingUrl ?: PartnerLinks.bookingComSearchUrl(
                                    activeTrip.destination, activeTrip.startDate, activeTrip.endDate
                                )
                                PartnerLinks.openExternalUrl(context, url)
                            }
                        ) {
                            Text("宿泊予約を確認")
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            items(ticketItems, key = { "ticket_${it.id}" }) { ticket ->
                TraloCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.QrCode2, contentDescription = "チケット", tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(text = ticket.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = ticket.providerName ?: ticket.categoryEnum.labelJa,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Outlined.ConfirmationNumber,
                            contentDescription = "予約済み",
                            tint = ChampagneGold
                        )
                    }
                }
            }
        }

        // 6. Live Weather on Travel Day
        item {
            when (weatherState) {
                is UiState.Success -> TraloWeatherCard(
                    forecast = weatherState.data,
                    onRefresh = onRefreshWeather,
                    onRainyActionClick = {
                        onOpenConcierge("${activeTrip.destination}の今日の天気に合わせた屋内回避ルート・代替スポットを提案してください。")
                    }
                )
                is UiState.Error -> TraloErrorCard(message = weatherState.message, onRetry = onRefreshWeather)
                UiState.Loading -> TraloSkeletonCard()
            }
        }

        // 7. Packing & Preparation Checklist
        item {
            TraloEditorialSectionHeader(
                englishOverline = "Packing Checklist",
                japaneseTitle = "持ち物チェックリスト"
            )
        }
        item {
            TraloCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newChecklistInput,
                        onValueChange = { newChecklistInput = it },
                        label = { Text("持ち物を追加（例：乗車券・傘）") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("checklist_input")
                    )
                    Button(
                        onClick = {
                            if (newChecklistInput.isNotBlank()) {
                                onAddChecklist(activeTrip.id, newChecklistInput)
                                newChecklistInput = ""
                            }
                        },
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("checklist_add_button")
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "追加")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                when (checklistState) {
                    is UiState.Loading -> TraloSkeletonCard()
                    is UiState.Error -> TraloErrorCard(message = checklistState.message)
                    is UiState.Success -> {
                        if (checklist.isEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.Checklist, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "必要な持ち物を追加して忘れ物を防ぎましょう。",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Column {
                                checklist.forEach { chk ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = chk.checked,
                                            onCheckedChange = { checked ->
                                                onToggleChecklist(activeTrip.id, chk.id, checked)
                                            }
                                        )
                                        Text(
                                            text = chk.label,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                textDecoration = if (chk.checked) TextDecoration.LineThrough else TextDecoration.None
                                            ),
                                            color = if (chk.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { onDeleteChecklist(activeTrip.id, chk.id) },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.DeleteOutline,
                                                contentDescription = "削除",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun TodayPriorityTransitCard(
    item: ItineraryItem,
    onOpenTransitTab: () -> Unit
) {
    TraloCard(containerColor = ForestGreen) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NEXT BOARDING ・ 本日の乗車予定",
                style = MaterialTheme.typography.labelSmall,
                color = ChampagneGold
            )
            TraloStatusBadge(label = item.bookingStatusEnum.labelJa, tone = BadgeTone.GOLD)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${item.startTime}${item.endTime?.let { " → $it" } ?: ""}",
            style = TabularNumericLarge,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.locationName + (item.lineName?.let { " ・ $it" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.88f)
        )

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BoardingMetricBox(
                label = "番線",
                value = item.platform ?: "駅案内確認",
                modifier = Modifier.weight(1f)
            )
            BoardingMetricBox(
                label = "号車",
                value = item.carNumber ?: "自由・確認",
                modifier = Modifier.weight(1f)
            )
            BoardingMetricBox(
                label = "座席",
                value = item.seatNumber ?: "未指定",
                modifier = Modifier.weight(1f)
            )
        }

        if (!item.notes.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "乗換・案内メモ: ${item.notes}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        FilledTonalButton(onClick = onOpenTransitTab) {
            Text("代替経路・乗換案内を確認")
        }
    }
}

@Composable
private fun BoardingMetricBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = Color.White.copy(alpha = 0.14f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = ChampagneGold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = TabularNumericMedium,
                color = Color.White
            )
        }
    }
}
