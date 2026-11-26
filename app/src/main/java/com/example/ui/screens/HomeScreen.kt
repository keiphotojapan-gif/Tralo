package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DirectionsTransit
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.remote.TransitKnowledgeBase
import com.example.model.DestinationSpotlight
import com.example.model.OdptApiState
import com.example.model.SavedRoute
import com.example.model.Trip
import com.example.model.WeatherForecast
import com.example.ui.components.BadgeTone
import com.example.ui.components.TraloCard
import com.example.ui.components.TraloEditorialSectionHeader
import com.example.ui.components.TraloEmptyState
import com.example.ui.components.TraloErrorCard
import com.example.ui.components.TraloSkeletonCard
import com.example.ui.components.TraloStatusBadge
import com.example.ui.components.TraloWeatherCard
import com.example.ui.components.drawableForDestinationKey
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.TabularNumericMedium
import com.example.viewmodel.UiState
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeScreen(
    tripsState: UiState<List<Trip>>,
    activeTrip: Trip?,
    weatherState: UiState<WeatherForecast>,
    odptSummaryState: OdptApiState,
    recentSearches: List<Pair<String, String>>,
    savedRoutesState: UiState<List<SavedRoute>>,
    onSearchTransit: (String, String) -> Unit,
    onOpenTransitTab: () -> Unit,
    onOpenItineraryTab: () -> Unit,
    onOpenTodayTab: () -> Unit,
    onOpenConcierge: (String?) -> Unit,
    onSelectTrip: (String) -> Unit,
    onCreateStarterTrip: () -> Unit,
    onRefreshWeather: () -> Unit,
    modifier: Modifier = Modifier
) {
    var originInput by remember { mutableStateOf("東京") }
    var destinationInput by remember { mutableStateOf("") }
    var voiceMessage by remember { mutableStateOf<String?>(null) }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()?.trim().orEmpty()
            if (spoken.isNotBlank()) {
                destinationInput = spoken
                voiceMessage = null
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Editorial Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(224.dp)
                    .clip(MaterialTheme.shapes.large)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_journey),
                    contentDescription = "TRALO 旅の風景",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0x22000000), Color(0xCC12281E))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    TraloStatusBadge(label = "TRALO INTEGRATED TRAVEL", tone = BadgeTone.GOLD)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your Journey,\nIntelligently Planned.",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "乗換・運行状況・予約・天気・AI案内をひとつの空間で。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                }
            }
        }

        // 2. Integrated Origin-Destination Search Card with Voice Input
        item {
            TraloCard {
                Text(
                    text = "出発地・目的地から経路を検索",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = originInput,
                    onValueChange = { originInput = it },
                    label = { Text("出発駅・空港・現在地") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_origin_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = destinationInput,
                    onValueChange = { destinationInput = it },
                    label = { Text("目的地・観光スポット（例：京都、箱根湯本）") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "目的地を話してください")
                                }
                                runCatching { voiceLauncher.launch(intent) }
                                    .onFailure {
                                        voiceMessage = "この端末では音声認識アプリが利用できません。"
                                    }
                            },
                            modifier = Modifier.testTag("home_voice_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Mic,
                                contentDescription = "音声で目的地を入力"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_destination_input")
                )
                if (!voiceMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = voiceMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onSearchTransit(
                                originInput.ifBlank { "東京" },
                                destinationInput.ifBlank { "京都" }
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("home_search_transit_button")
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("経路を検索")
                    }
                    FilledTonalButton(
                        onClick = { onOpenConcierge(null) },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("home_ai_plan_button")
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AIプラン")
                    }
                }
            }
        }

        // 3. Quick Actions Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    icon = Icons.Outlined.DirectionsTransit,
                    label = "乗換案内",
                    onClick = onOpenTransitTab,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_transit")
                )
                QuickActionTile(
                    icon = Icons.Outlined.Today,
                    label = "旅行当日",
                    onClick = onOpenTodayTab,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_today")
                )
                QuickActionTile(
                    icon = Icons.Outlined.FlightTakeoff,
                    label = "旅程管理",
                    onClick = onOpenItineraryTab,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_itinerary")
                )
                QuickActionTile(
                    icon = Icons.Outlined.AutoAwesome,
                    label = "AI相談",
                    onClick = { onOpenConcierge(null) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_concierge")
                )
            }
        }

        // 4. Live Weather Summary (Open-Meteo)
        item {
            TraloEditorialSectionHeader(
                englishOverline = "Live Atmosphere",
                japaneseTitle = "旅行先の天気・気象アドバイス"
            )
            Spacer(modifier = Modifier.height(10.dp))
            when (weatherState) {
                is UiState.Loading -> TraloSkeletonCard()
                is UiState.Error -> TraloErrorCard(
                    message = weatherState.message,
                    onRetry = onRefreshWeather
                )
                is UiState.Success -> TraloWeatherCard(
                    forecast = weatherState.data,
                    onRefresh = onRefreshWeather,
                    onRainyActionClick = {
                        onOpenConcierge("${weatherState.data.locationName}で雨天予報です。屋外スポットの代わりに楽しめる屋内観光・美術館・カフェプランを提案してください。")
                    }
                )
            }
        }

        // 5. Transit Operations Overview (ODPT API v4 honesty)
        item {
            TraloEditorialSectionHeader(
                englishOverline = "Transit Operations",
                japaneseTitle = "交通運行ステータス"
            )
            Spacer(modifier = Modifier.height(10.dp))
            TraloCard(onClick = onOpenTransitTab) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        when (odptSummaryState) {
                            is OdptApiState.NotConfigured -> {
                                TraloStatusBadge(label = "ODPT API未設定", tone = BadgeTone.AMBER)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "AI Studio Secretsで ODPT_API_KEY を設定すると、リアルタイム運行・列車位置が連携されます。",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            is OdptApiState.Available -> {
                                val delayedCount = odptSummaryState.trainInfos.count { it.isDelayed }
                                TraloStatusBadge(
                                    label = if (delayedCount == 0) "ODPT Live 平常運転" else "遅延情報 ${delayedCount}件",
                                    tone = if (delayedCount == 0) BadgeTone.FOREST else BadgeTone.AMBER
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "取得路線 ${odptSummaryState.trainInfos.size}件 ・ 更新 ${odptSummaryState.updatedAt}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            is OdptApiState.Loading -> {
                                Text("運行情報を確認中...", style = MaterialTheme.typography.bodyMedium)
                            }
                            is OdptApiState.Error -> {
                                TraloStatusBadge(label = "通信エラー", tone = BadgeTone.CRIMSON)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = odptSummaryState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            OdptApiState.Idle -> {
                                Text("運行状況を確認するにはタップ", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = "交通検索へ",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 6. Active Trip & Budget Pulse
        item {
            TraloEditorialSectionHeader(
                englishOverline = "Saved Journeys & Budget",
                japaneseTitle = "保存した旅行・予算管理"
            )
            Spacer(modifier = Modifier.height(10.dp))
            when (tripsState) {
                is UiState.Loading -> TraloSkeletonCard()
                is UiState.Error -> TraloErrorCard(message = tripsState.message)
                is UiState.Success -> {
                    val trips = tripsState.data
                    if (trips.isEmpty()) {
                        TraloEmptyState(
                            icon = Icons.Outlined.FlightTakeoff,
                            title = "保存された旅程はまだありません",
                            description = "AIプラン作成またはモデル旅程から、あなただけの旅をスタートできます。",
                            actionLabel = "京都・嵐山モデル旅程を作成",
                            onAction = onCreateStarterTrip
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            trips.take(3).forEach { trip ->
                                SavedTripSummaryCard(
                                    trip = trip,
                                    isSelected = trip.id == activeTrip?.id,
                                    onClick = {
                                        onSelectTrip(trip.id)
                                        onOpenItineraryTab()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. Curated Destinations & Popular Spots
        item {
            TraloEditorialSectionHeader(
                englishOverline = "Curated Destinations",
                japaneseTitle = "おすすめの旅先・人気観光地"
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(TransitKnowledgeBase.spotlights, key = { it.id }) { spot ->
                    DestinationSpotlightCard(
                        spot = spot,
                        onSearchRoute = {
                            onSearchTransit(originInput.ifBlank { "東京" }, spot.recommendedStation)
                        },
                        onPlanWithAi = {
                            onOpenConcierge("${spot.name}を巡る1泊2日・予算${spot.estimatedBudgetJpy}円の洗練された旅程を作成してください。")
                        }
                    )
                }
            }
        }

        // 8. Recent Searches & Favorite Routes
        if (recentSearches.isNotEmpty()) {
            item {
                TraloEditorialSectionHeader(
                    englishOverline = "Recent Searches",
                    japaneseTitle = "最近検索した経路"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentSearches.take(4).forEach { (from, to) ->
                        TraloCard(onClick = { onSearchTransit(from, to) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "$from → $to",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                    contentDescription = "再検索",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        if (savedRoutesState is UiState.Success && savedRoutesState.data.isNotEmpty()) {
            item {
                TraloEditorialSectionHeader(
                    englishOverline = "Pinned Routes",
                    japaneseTitle = "保存済みお気に入り経路"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    savedRoutesState.data.take(3).forEach { route ->
                        TraloCard(onClick = { onSearchTransit(route.origin, route.destination) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${route.origin} → ${route.destination}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = route.summary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "¥${NumberFormat.getNumberInstance(Locale.JAPAN).format(route.fareJpy)}",
                                    style = TabularNumericMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
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
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(74.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SavedTripSummaryCard(
    trip: Trip,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val formatter = NumberFormat.getNumberInstance(Locale.JAPAN)
    val progress = if (trip.budgetJpy > 0L) {
        (trip.spentJpy.toFloat() / trip.budgetJpy.toFloat()).coerceIn(0f, 1f)
    } else 0f

    TraloCard(
        onClick = onClick,
        containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = trip.title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${trip.destination} ・ ${trip.startDate} 〜 ${trip.endDate}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TraloStatusBadge(
                label = trip.statusEnum.labelJa,
                tone = if (trip.statusEnum.name == "ACTIVE") BadgeTone.GOLD else BadgeTone.FOREST
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "予算消化",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "¥${formatter.format(trip.spentJpy)} / ¥${formatter.format(trip.budgetJpy)}",
                style = TabularNumericMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(MaterialTheme.shapes.extraSmall),
            color = ChampagneGold,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun DestinationSpotlightCard(
    spot: DestinationSpotlight,
    onSearchRoute: () -> Unit,
    onPlanWithAi: () -> Unit
) {
    TraloCard(
        modifier = Modifier.width(264.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Image(
            painter = painterResource(id = drawableForDestinationKey(spot.imageKey)),
            contentDescription = spot.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .clip(MaterialTheme.shapes.small)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = spot.subtitleEn,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary
        )
        Text(text = spot.name, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = spot.descriptionJa,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onSearchRoute,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("経路検索", style = MaterialTheme.typography.labelMedium)
            }
            FilledTonalButton(
                onClick = onPlanWithAi,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                colors = ButtonDefaults.filledTonalButtonColors(),
                modifier = Modifier.weight(1f)
            ) {
                Text("AI旅程", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
