package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.LocalActivity
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.PartnerLinks
import com.example.model.BookingStatus
import com.example.model.ItineraryCategory
import com.example.model.ItineraryItem
import com.example.model.Trip
import com.example.model.TripStatus
import com.example.model.WeatherForecast
import com.example.ui.components.BadgeTone
import com.example.ui.components.TraloCard
import com.example.ui.components.TraloEditorialSectionHeader
import com.example.ui.components.TraloEmptyState
import com.example.ui.components.TraloErrorCard
import com.example.ui.components.TraloSkeletonCard
import com.example.ui.components.TraloStatusBadge
import com.example.ui.components.TraloWeatherCard
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.TabularNumericMedium
import com.example.viewmodel.UiState
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ItineraryScreen(
    tripsState: UiState<List<Trip>>,
    activeTrip: Trip?,
    itemsState: UiState<List<ItineraryItem>>,
    weatherState: UiState<WeatherForecast>,
    onSelectTrip: (String) -> Unit,
    onCreateTrip: (String, String, String, String, String, Long) -> Unit,
    onCreateStarterTrip: () -> Unit,
    onUpdateTripStatus: (Trip, TripStatus) -> Unit,
    onDeleteTrip: (String) -> Unit,
    onAddItem: (
        tripId: String,
        dayIndex: Long,
        startTime: String,
        title: String,
        category: ItineraryCategory,
        locationName: String,
        lineName: String?,
        platform: String?,
        carNumber: String?,
        seatNumber: String?,
        providerName: String?,
        bookingRef: String?,
        bookingUrl: String?,
        costJpy: Long,
        notes: String?,
        bookingStatus: BookingStatus
    ) -> Unit,
    onUpdateBookingStatus: (String, String, BookingStatus, String?) -> Unit,
    onDeleteItem: (String, String) -> Unit,
    onRefreshWeather: () -> Unit,
    onOpenConciergeWithPrompt: (String) -> Unit,
    appSettings: com.example.model.AppSettings = com.example.model.AppSettings(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showCreateTripDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var selectedItemForDetail by remember { mutableStateOf<ItineraryItem?>(null) }
    var selectedDay by remember { mutableIntStateOf(1) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TraloEditorialSectionHeader(
                englishOverline = "Curated Itinerary",
                japaneseTitle = "旅程・宿泊・予約管理",
                trailing = {
                    FilledTonalButton(
                        onClick = { showCreateTripDialog = true },
                        modifier = Modifier.testTag("itinerary_new_trip_button")
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("新規旅程")
                    }
                }
            )
        }

        when (tripsState) {
            is UiState.Loading -> item { TraloSkeletonCard() }
            is UiState.Error -> item { TraloErrorCard(message = tripsState.message) }
            is UiState.Success -> {
                val trips = tripsState.data
                if (trips.isEmpty()) {
                    item {
                        TraloEmptyState(
                            icon = Icons.Outlined.FlightTakeoff,
                            title = "旅程が登録されていません",
                            description = "新規旅程を作成するか、京都・嵐山モデル旅程から編集を始められます。",
                            actionLabel = "京都・嵐山モデル旅程を作成",
                            onAction = onCreateStarterTrip
                        )
                    }
                } else {
                    // Trip Selector Chips
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(trips, key = { it.id }) { trip ->
                                FilterChip(
                                    selected = trip.id == activeTrip?.id,
                                    onClick = {
                                        selectedDay = 1
                                        onSelectTrip(trip.id)
                                    },
                                    label = { Text(trip.title) }
                                )
                            }
                        }
                    }

                    activeTrip?.let { trip ->
                        item {
                            TripHeaderOverviewCard(
                                trip = trip,
                                appSettings = appSettings,
                                onToggleTodayActive = {
                                    val nextStatus = if (trip.statusEnum == TripStatus.ACTIVE) {
                                        TripStatus.UPCOMING
                                    } else {
                                        TripStatus.ACTIVE
                                    }
                                    onUpdateTripStatus(trip, nextStatus)
                                },
                                onOptimizeWithAi = {
                                    onOpenConciergeWithPrompt("旅程「${trip.title}」（${trip.destination}、${trip.startDate}〜${trip.endDate}、予算${appSettings.formatCurrency(trip.budgetJpy)}）の移動順と時間配分を最適化し、混雑回避や代替案を提案してください。")
                                },
                                onDeleteTrip = { onDeleteTrip(trip.id) }
                            )
                        }

                        // Partner Booking Links (Booking.com & Klook)
                        item {
                            TraloCard {
                                Text("宿泊・観光体験の予約連携", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "公式パートナーサイトで最新の空室・体験チケットを検索し、予約番号をこの旅程に記録できます。",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            PartnerLinks.openExternalUrl(
                                                context,
                                                PartnerLinks.bookingComSearchUrl(trip.destination, trip.startDate, trip.endDate)
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Booking.com", style = MaterialTheme.typography.labelMedium)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            PartnerLinks.openExternalUrl(
                                                context,
                                                PartnerLinks.klookSearchUrl(trip.destination)
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Klook体験", style = MaterialTheme.typography.labelMedium)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }

                        // Weather Strip
                        item {
                            when (weatherState) {
                                is UiState.Success -> TraloWeatherCard(
                                    forecast = weatherState.data,
                                    onRefresh = onRefreshWeather,
                                    onRainyActionClick = {
                                        onOpenConciergeWithPrompt("${trip.destination}での雨天予報に合わせて、「${trip.title}」の屋内観光・カフェ・美術館プランを提案してください。")
                                    }
                                )
                                is UiState.Error -> TraloErrorCard(message = weatherState.message, onRetry = onRefreshWeather)
                                UiState.Loading -> TraloSkeletonCard()
                            }
                        }

                        // Day-by-Day Timeline + Route Map Preview
                        when (itemsState) {
                            is UiState.Loading -> item { TraloSkeletonCard() }
                            is UiState.Error -> item { TraloErrorCard(message = itemsState.message) }
                            is UiState.Success -> {
                                val allItems = itemsState.data
                                val days = (allItems.map { it.dayIndex.toInt() } + listOf(1, 2, 3))
                                    .distinct()
                                    .sorted()
                                val dayItems = allItems.filter { it.dayIndex.toInt() == selectedDay }

                                if (dayItems.isNotEmpty()) {
                                    item {
                                        ItineraryRouteCanvasCard(
                                            tripTitle = trip.destination,
                                            items = dayItems,
                                            onOpenMaps = {
                                                PartnerLinks.openExternalUrl(
                                                    context,
                                                    PartnerLinks.mapsSearchUrl(trip.destination)
                                                )
                                            }
                                        )
                                    }
                                }

                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        LazyRow(
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(days) { day ->
                                                FilterChip(
                                                    selected = selectedDay == day,
                                                    onClick = { selectedDay = day },
                                                    label = { Text("Day $day") }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { showAddItemDialog = true },
                                            modifier = Modifier.testTag("itinerary_add_item_button")
                                        ) {
                                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("予定追加")
                                        }
                                    }
                                }

                                if (dayItems.isEmpty()) {
                                    item {
                                        TraloEmptyState(
                                            icon = Icons.Outlined.Map,
                                            title = "Day $selectedDay の予定はまだありません",
                                            description = "「予定追加」から移動区間・ホテル・観光スポット・飲食店を追加してください。",
                                            actionLabel = "Day $selectedDay に予定を追加",
                                            onAction = { showAddItemDialog = true }
                                        )
                                    }
                                } else {
                                    items(dayItems, key = { it.id }) { item ->
                                        ItineraryItemCard(
                                            item = item,
                                            onClick = { selectedItemForDetail = item }
                                        )
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

    if (showCreateTripDialog) {
        CreateTripDialog(
            onDismiss = { showCreateTripDialog = false },
            onConfirm = { title, dest, start, end, companions, budget ->
                onCreateTrip(title, dest, start, end, companions, budget)
                showCreateTripDialog = false
            }
        )
    }

    if (showAddItemDialog && activeTrip != null) {
        AddItineraryItemDialog(
            trip = activeTrip,
            defaultDay = selectedDay.toLong(),
            onDismiss = { showAddItemDialog = false },
            onConfirm = { day, time, title, cat, loc, line, platform, car, seat, provider, ref, url, cost, notes ->
                onAddItem(
                    activeTrip.id, day, time, title, cat, loc, line, platform, car, seat,
                    provider, ref, url, cost, notes,
                    if (!ref.isNullOrBlank()) BookingStatus.CONFIRMED else BookingStatus.PLANNED
                )
                showAddItemDialog = false
            }
        )
    }

    selectedItemForDetail?.let { item ->
        activeTrip?.let { trip ->
            ItineraryItemDetailDialog(
                item = item,
                onDismiss = { selectedItemForDetail = null },
                onMarkStatus = { newStatus, newRef ->
                    onUpdateBookingStatus(trip.id, item.id, newStatus, newRef)
                    selectedItemForDetail = null
                },
                onOpenUrl = { url -> PartnerLinks.openExternalUrl(context, url) },
                onDelete = {
                    onDeleteItem(trip.id, item.id)
                    selectedItemForDetail = null
                }
            )
        }
    }
}

@Composable
private fun TripHeaderOverviewCard(
    trip: Trip,
    appSettings: com.example.model.AppSettings,
    onToggleTodayActive: () -> Unit,
    onOptimizeWithAi: () -> Unit,
    onDeleteTrip: () -> Unit
) {
    val destinationCountry = com.example.data.remote.CuratedCountriesData.allCuratedCountries.firstOrNull {
        trip.destination.contains(it.commonName, ignoreCase = true) ||
            trip.destination.contains(it.getLocalizedName("ja"), ignoreCase = true) ||
            trip.title.contains(it.commonName, ignoreCase = true) ||
            trip.title.contains(it.getLocalizedName("ja"), ignoreCase = true)
    } ?: com.example.data.remote.CuratedCountriesData.allCuratedCountries.first()

    val ratio = if (trip.budgetJpy > 0) (trip.spentJpy.toFloat() / trip.budgetJpy.toFloat()).coerceIn(0f, 1f) else 0f

    TraloCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(destinationCountry.flagEmoji, style = MaterialTheme.typography.bodyMedium)
                    Text(text = destinationCountry.getLocalizedName(appSettings.language), style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
                }
                Text(text = trip.title, style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${trip.startDate} 〜 ${trip.endDate}" +
                        if (trip.companions.isNotBlank()) " ・ 同行: ${trip.companions}" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "現地現在時刻: ${com.example.core.TraloLocalization.calculateDestinationLocalTime(destinationCountry.primaryTimezone)} (${com.example.core.TraloLocalization.calculateTimeDifference(destinationCountry.primaryTimezone)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            IconButton(onClick = onDeleteTrip, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "旅程を削除", tint = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("予算進捗", style = MaterialTheme.typography.labelMedium)
            Text(
                text = "${appSettings.formatCurrency(trip.spentJpy)} / ${appSettings.formatCurrency(trip.budgetJpy)}",
                style = TabularNumericMedium
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(MaterialTheme.shapes.extraSmall),
            color = ForestGreen,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onToggleTodayActive,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (trip.statusEnum == TripStatus.ACTIVE) "旅行当日モード中" else "旅行当日に設定",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            FilledTonalButton(
                onClick = onOptimizeWithAi,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("AI最適化", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ItineraryRouteCanvasCard(
    tripTitle: String,
    items: List<ItineraryItem>,
    onOpenMaps: () -> Unit
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val accentColor = ChampagneGold
    TraloCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$tripTitle ルートマップ概観（${items.size}地点）",
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(onClick = onOpenMaps) {
                Text("地図アプリで開く")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            val count = items.size.coerceAtLeast(1)
            val padX = 28f
            val usableW = (size.width - padX * 2).coerceAtLeast(1f)
            val midY = size.height / 2f
            val points = (0 until count).map { i ->
                val x = if (count == 1) size.width / 2f else padX + usableW * i / (count - 1)
                val y = midY + if (i % 2 == 0) -10f else 10f
                Offset(x, y)
            }
            for (i in 0 until points.lastIndex) {
                drawLine(
                    color = lineColor.copy(alpha = 0.6f),
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 5f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            }
            points.forEachIndexed { idx, pt ->
                drawCircle(
                    color = if (idx == 0) accentColor else lineColor,
                    radius = 10f,
                    center = pt
                )
                drawCircle(
                    color = androidx.compose.ui.graphics.Color.White,
                    radius = 4f,
                    center = pt
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(items.first().locationName, style = MaterialTheme.typography.labelMedium)
            if (items.size > 1) {
                Text(items.last().locationName, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ItineraryItemCard(
    item: ItineraryItem,
    onClick: () -> Unit
) {
    val fmt = NumberFormat.getNumberInstance(Locale.JAPAN)
    TraloCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = item.startTime, style = TabularNumericMedium, color = MaterialTheme.colorScheme.primary)
                Icon(
                    imageVector = categoryIcon(item.categoryEnum),
                    contentDescription = item.categoryEnum.labelJa,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                TraloStatusBadge(label = item.categoryEnum.labelJa, tone = BadgeTone.NEUTRAL)
            }
            TraloStatusBadge(
                label = item.bookingStatusEnum.labelJa,
                tone = when (item.bookingStatusEnum) {
                    BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN -> BadgeTone.FOREST
                    BookingStatus.CANCELLED -> BadgeTone.CRIMSON
                    BookingStatus.PLANNED -> BadgeTone.GOLD
                }
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = item.title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = item.locationName +
                (item.lineName?.let { " ・ $it" } ?: "") +
                (item.platform?.let { " ($it)" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (item.costJpy > 0L || !item.bookingRef.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (!item.bookingRef.isNullOrBlank()) {
                    Text(
                        text = "予約番号: ${item.bookingRef}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                if (item.costJpy > 0L) {
                    Text(
                        text = "¥${fmt.format(item.costJpy)}",
                        style = TabularNumericMedium
                    )
                }
            }
        }
    }
}

private fun categoryIcon(category: ItineraryCategory): ImageVector = when (category) {
    ItineraryCategory.TRANSIT -> Icons.Outlined.Train
    ItineraryCategory.STAY -> Icons.Outlined.Hotel
    ItineraryCategory.ACTIVITY -> Icons.Outlined.LocalActivity
    ItineraryCategory.DINING -> Icons.Outlined.Restaurant
    ItineraryCategory.SPOT -> Icons.Outlined.Map
}

@Composable
private fun CreateTripDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("京都") }
    var startDate by remember { mutableStateOf("2026-12-10") }
    var endDate by remember { mutableStateOf("2026-12-12") }
    var companions by remember { mutableStateOf("2名") }
    var budgetText by remember { mutableStateOf("90000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新しい旅程を作成") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("旅行タイトル") },
                    placeholder = { Text("例：冬の京都・白川ステイ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("目的地") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("開始日") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("終了日") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = companions,
                    onValueChange = { companions = it },
                    label = { Text("同行者") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it.filter(Char::isDigit) },
                    label = { Text("予算（円）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        title.ifBlank { "${destination}の旅" },
                        destination.ifBlank { "東京" },
                        startDate.ifBlank { "2026-12-10" },
                        endDate.ifBlank { "2026-12-12" },
                        companions,
                        budgetText.toLongOrNull() ?: 80000L
                    )
                }
            ) { Text("作成") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}

@Composable
private fun AddItineraryItemDialog(
    trip: Trip,
    defaultDay: Long,
    onDismiss: () -> Unit,
    onConfirm: (
        dayIndex: Long,
        startTime: String,
        title: String,
        category: ItineraryCategory,
        locationName: String,
        lineName: String?,
        platform: String?,
        carNumber: String?,
        seatNumber: String?,
        providerName: String?,
        bookingRef: String?,
        bookingUrl: String?,
        costJpy: Long,
        notes: String?
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("10:00") }
    var location by remember { mutableStateOf(trip.destination) }
    var category by remember { mutableStateOf(ItineraryCategory.SPOT) }
    var provider by remember { mutableStateOf("") }
    var bookingRef by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Day $defaultDay に予定を追加") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ItineraryCategory.entries) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.labelJa) }
                        )
                    }
                }
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("時刻（HH:mm）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("タイトル（駅・ホテル・観光地など）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("場所・駅名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = provider,
                    onValueChange = { provider = it },
                    label = { Text("予約サイト・会社（任意: Booking.com / Klook）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bookingRef,
                    onValueChange = { bookingRef = it },
                    label = { Text("予約番号・チケットID（任意）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it.filter(Char::isDigit) },
                    label = { Text("料金（円）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("メモ・番線・号車（任意）") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val autoUrl = when {
                        category == ItineraryCategory.STAY ->
                            PartnerLinks.bookingComSearchUrl(location, trip.startDate, trip.endDate)
                        category == ItineraryCategory.ACTIVITY ->
                            PartnerLinks.klookSearchUrl(title.ifBlank { location })
                        else -> PartnerLinks.mapsSearchUrl(location)
                    }
                    onConfirm(
                        defaultDay,
                        startTime.ifBlank { "10:00" },
                        title.ifBlank { "${category.labelJa} (${location})" },
                        category,
                        location.ifBlank { trip.destination },
                        null,
                        null,
                        null,
                        null,
                        provider.ifBlank { null },
                        bookingRef.ifBlank { null },
                        autoUrl,
                        costText.toLongOrNull() ?: 0L,
                        notes.ifBlank { null }
                    )
                }
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}

@Composable
private fun ItineraryItemDetailDialog(
    item: ItineraryItem,
    onDismiss: () -> Unit,
    onMarkStatus: (BookingStatus, String?) -> Unit,
    onOpenUrl: (String) -> Unit,
    onDelete: () -> Unit
) {
    var refInput by remember { mutableStateOf(item.bookingRef.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("日時: Day ${item.dayIndex} ${item.startTime}", style = MaterialTheme.typography.bodyMedium)
                Text("場所: ${item.locationName}", style = MaterialTheme.typography.bodyMedium)
                if (!item.lineName.isNullOrBlank()) {
                    Text("路線・座席: ${item.lineName} ${item.carNumber.orEmpty()} ${item.seatNumber.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
                }
                if (!item.notes.isNullOrBlank()) {
                    Text("メモ: ${item.notes}", style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedTextField(
                    value = refInput,
                    onValueChange = { refInput = it },
                    label = { Text("予約確認番号") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { onMarkStatus(BookingStatus.CONFIRMED, refInput.ifBlank { null }) },
                        modifier = Modifier.weight(1f)
                    ) { Text("予約済にする") }
                    OutlinedButton(
                        onClick = { onMarkStatus(BookingStatus.CHECKED_IN, refInput.ifBlank { null }) },
                        modifier = Modifier.weight(1f)
                    ) { Text("利用完了") }
                }
                if (!item.bookingUrl.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = { onOpenUrl(item.bookingUrl) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("予約・地図リンクを開く")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("閉じる") }
        },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Text("削除", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}
