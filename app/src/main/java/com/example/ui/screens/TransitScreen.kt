package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.outlined.Accessible
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DirectionsBoat
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsSubway
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.PedalBike
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.core.PartnerLinks
import com.example.model.DataProvenance
import com.example.model.OdptApiState
import com.example.model.SeatPreference
import com.example.model.SortPreference
import com.example.model.StationFacilityInfo
import com.example.model.TimeQueryMode
import com.example.model.TransitLeg
import com.example.model.TransitRouteOption
import com.example.model.TransportMode
import com.example.ui.components.BadgeTone
import com.example.ui.components.TraloCard
import com.example.ui.components.TraloEditorialSectionHeader
import com.example.ui.components.TraloEmptyState
import com.example.ui.components.TraloErrorCard
import com.example.ui.components.TraloInfoNotice
import com.example.ui.components.TraloSkeletonCard
import com.example.ui.components.TraloStatusBadge
import com.example.ui.theme.TabularNumericLarge
import com.example.ui.theme.TabularNumericMedium
import com.example.viewmodel.TransitSearchUiState
import com.google.android.gms.location.LocationServices
import java.text.NumberFormat
import java.util.Locale

@Composable
fun TransitScreen(
    state: TransitSearchUiState,
    onOriginChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onSwapStations: () -> Unit,
    onTimeModeChange: (TimeQueryMode) -> Unit,
    onAdjustMinutes: (Int) -> Unit,
    onSortChange: (SortPreference) -> Unit,
    onSeatChange: (SeatPreference) -> Unit,
    onMaxTransfersChange: (Int?) -> Unit,
    onToggleMode: (TransportMode) -> Unit,
    onSearch: () -> Unit,
    onRefreshOdpt: () -> Unit,
    onSelectOdptRailway: (String) -> Unit,
    onSelectRoute: (String) -> Unit,
    onSaveRoute: (TransitRouteOption, Boolean) -> Unit,
    onAddRouteToTrip: (TransitRouteOption) -> Unit,
    onCoordinatesResolved: (Double, Double) -> Unit,
    onClearStatusMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var locationMessage by remember { mutableStateOf<String?>(null) }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation() {
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    locationMessage = null
                    onCoordinatesResolved(loc.latitude, loc.longitude)
                } else {
                    locationMessage = "端末の現在地が未取得のため、東京駅周辺を基準に表示します。"
                    onCoordinatesResolved(35.6812, 139.7671)
                }
            }
            .addOnFailureListener {
                locationMessage = "現在地の取得に失敗しました。駅名を直接入力してください。"
            }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchCurrentLocation()
        } else {
            locationMessage = "位置情報権限が許可されていません。駅名または空港名を直接入力してください。"
        }
    }

    val voiceDestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val spoken = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                onDestinationChange(spoken)
                onSearch()
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 760.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(0.46f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        TransitSearchControlsCard(
                            state = state,
                            locationMessage = locationMessage,
                            onOriginChange = onOriginChange,
                            onDestinationChange = onDestinationChange,
                            onSwapStations = onSwapStations,
                            onRequestLocation = {
                                val hasFine = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                val hasCoarse = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasFine || hasCoarse) {
                                    fetchCurrentLocation()
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            onVoiceDestination = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
                                }
                                runCatching { voiceDestLauncher.launch(intent) }
                            },
                            onTimeModeChange = onTimeModeChange,
                            onAdjustMinutes = onAdjustMinutes,
                            onSortChange = onSortChange,
                            onSeatChange = onSeatChange,
                            onMaxTransfersChange = onMaxTransfersChange,
                            onToggleMode = onToggleMode,
                            onSearch = onSearch
                        )
                    }
                    item {
                        StationFacilityCard(state.originFacility)
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(0.54f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (!state.statusMessage.isNullOrBlank()) {
                        item {
                            TraloInfoNotice(title = "通知", body = state.statusMessage)
                        }
                    }
                    item {
                        TraloEditorialSectionHeader(
                            englishOverline = "Route Comparison",
                            japaneseTitle = "検索結果（${state.routes.size}件）"
                        )
                    }
                    if (state.isSearching) {
                        item { TraloSkeletonCard() }
                    } else if (state.routes.isEmpty()) {
                        item {
                            TraloEmptyState(
                                icon = Icons.Outlined.Train,
                                title = "条件に一致する経路がありません",
                                description = "移動手段や乗換回数の条件を緩めて再検索してください。"
                            )
                        }
                    } else {
                        items(state.routes, key = { it.id }) { route ->
                            RouteComparisonCard(
                                route = route,
                                isExpanded = route.id == state.selectedRouteId,
                                onToggleExpand = { onSelectRoute(route.id) },
                                onBookmark = { onSaveRoute(route, true) },
                                onAddToTrip = { onAddRouteToTrip(route) }
                            )
                        }
                    }
                    item {
                        OdptLiveStatusPanel(
                            odptState = state.odptState,
                            selectedRailwayId = state.selectedRailwayId,
                            onSelectRailway = onSelectOdptRailway,
                            onRefresh = onRefreshOdpt
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("transit_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    TraloEditorialSectionHeader(
                        englishOverline = "Multimodal Transit",
                        japaneseTitle = "交通検索・リアルタイム乗換案内"
                    )
                }
                item {
                    TransitSearchControlsCard(
                        state = state,
                        locationMessage = locationMessage,
                        onOriginChange = onOriginChange,
                        onDestinationChange = onDestinationChange,
                        onSwapStations = onSwapStations,
                        onRequestLocation = {
                            val hasFine = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            val hasCoarse = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasFine || hasCoarse) {
                                fetchCurrentLocation()
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        onVoiceDestination = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
                            }
                            runCatching { voiceDestLauncher.launch(intent) }
                        },
                        onTimeModeChange = onTimeModeChange,
                        onAdjustMinutes = onAdjustMinutes,
                        onSortChange = onSortChange,
                        onSeatChange = onSeatChange,
                        onMaxTransfersChange = onMaxTransfersChange,
                        onToggleMode = onToggleMode,
                        onSearch = onSearch
                    )
                }

                if (!state.statusMessage.isNullOrBlank()) {
                    item {
                        TraloInfoNotice(
                            title = "ステータス",
                            body = state.statusMessage
                        )
                        TextButton(onClick = onClearStatusMessage) { Text("閉じる") }
                    }
                }

                item {
                    TraloEditorialSectionHeader(
                        englishOverline = "Route Comparison",
                        japaneseTitle = "経路比較（${state.routes.size}件）"
                    )
                }

                if (state.isSearching) {
                    item { TraloSkeletonCard() }
                } else if (state.routes.isEmpty()) {
                    item {
                        TraloEmptyState(
                            icon = Icons.Outlined.Train,
                            title = "該当経路が見つかりません",
                            description = "乗換回数・移動手段フィルタを調整して再度お試しください。"
                        )
                    }
                } else {
                    items(state.routes, key = { it.id }) { route ->
                        RouteComparisonCard(
                            route = route,
                            isExpanded = route.id == state.selectedRouteId,
                            onToggleExpand = { onSelectRoute(route.id) },
                            onBookmark = { onSaveRoute(route, true) },
                            onAddToTrip = { onAddRouteToTrip(route) }
                        )
                    }
                }

                item {
                    StationFacilityCard(state.originFacility)
                }

                item {
                    OdptLiveStatusPanel(
                        odptState = state.odptState,
                        selectedRailwayId = state.selectedRailwayId,
                        onSelectRailway = onSelectOdptRailway,
                        onRefresh = onRefreshOdpt
                    )
                }

                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransitSearchControlsCard(
    state: TransitSearchUiState,
    locationMessage: String?,
    onOriginChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onSwapStations: () -> Unit,
    onRequestLocation: () -> Unit,
    onVoiceDestination: () -> Unit,
    onTimeModeChange: (TimeQueryMode) -> Unit,
    onAdjustMinutes: (Int) -> Unit,
    onSortChange: (SortPreference) -> Unit,
    onSeatChange: (SeatPreference) -> Unit,
    onMaxTransfersChange: (Int?) -> Unit,
    onToggleMode: (TransportMode) -> Unit,
    onSearch: () -> Unit
) {
    TraloCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.origin,
                    onValueChange = onOriginChange,
                    label = { Text("出発（駅・空港・バス停）") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = onRequestLocation,
                            modifier = Modifier.testTag("transit_gps_button")
                        ) {
                            Icon(Icons.Outlined.MyLocation, contentDescription = "現在地から設定")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transit_origin_input")
                )
                OutlinedTextField(
                    value = state.destination,
                    onValueChange = onDestinationChange,
                    label = { Text("到着（駅・空港・観光地）") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = onVoiceDestination) {
                            Icon(Icons.Outlined.Mic, contentDescription = "音声で目的地を入力")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transit_destination_input")
                )
            }
            IconButton(
                onClick = onSwapStations,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("transit_swap_button")
            ) {
                Icon(Icons.Outlined.SwapVert, contentDescription = "出発と到着を入れ替え")
            }
        }

        if (!locationMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = locationMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        // Time specification row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    text = "%02d:%02d %s".format(state.hour, state.minute, state.timeMode.labelJa),
                    style = TabularNumericMedium
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                    onClick = { onAdjustMinutes(-30) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) { Text("-30分") }
                OutlinedButton(
                    onClick = { onAdjustMinutes(30) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) { Text("+30分") }
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(TimeQueryMode.entries) { mode ->
                FilterChip(
                    selected = state.timeMode == mode,
                    onClick = { onTimeModeChange(mode) },
                    label = { Text(mode.labelJa) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("優先条件", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SortPreference.entries.forEach { sort ->
                FilterChip(
                    selected = state.sortPreference == sort,
                    onClick = { onSortChange(sort) },
                    label = { Text(sort.labelJa) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text("乗換回数・座席条件", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                FilterChip(
                    selected = state.maxTransfers == null,
                    onClick = { onMaxTransfersChange(null) },
                    label = { Text("乗換制限なし") }
                )
            }
            item {
                FilterChip(
                    selected = state.maxTransfers == 0,
                    onClick = { onMaxTransfersChange(0) },
                    label = { Text("直通のみ") }
                )
            }
            item {
                FilterChip(
                    selected = state.maxTransfers == 1,
                    onClick = { onMaxTransfersChange(1) },
                    label = { Text("乗換1回まで") }
                )
            }
            items(SeatPreference.entries) { seat ->
                FilterChip(
                    selected = state.seatPreference == seat,
                    onClick = { onSeatChange(seat) },
                    label = { Text(seat.labelJa) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text("利用交通手段", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val selectableModes = listOf(
                TransportMode.RAIL,
                TransportMode.SUBWAY,
                TransportMode.LOCAL_BUS,
                TransportMode.HIGHWAY_BUS,
                TransportMode.AIRPORT_BUS,
                TransportMode.FLIGHT,
                TransportMode.FERRY,
                TransportMode.SHARE_CYCLE
            )
            items(selectableModes) { mode ->
                FilterChip(
                    selected = mode in state.enabledModes,
                    onClick = { onToggleMode(mode) },
                    label = { Text(mode.shortLabel) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onSearch,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("transit_search_button")
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("この条件で経路を比較検索")
        }
    }
}

@Composable
private fun RouteComparisonCard(
    route: TransitRouteOption,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onBookmark: () -> Unit,
    onAddToTrip: () -> Unit
) {
    val context = LocalContext.current
    val fareFmt = NumberFormat.getNumberInstance(Locale.JAPAN).format(route.totalFareJpy)

    TraloCard(
        onClick = onToggleExpand,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("route_card_${route.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "${route.departureTime} → ${route.arrivalTime}", style = TabularNumericLarge)
                TraloStatusBadge(
                    label = route.provenance.labelJa,
                    tone = when (route.provenance) {
                        DataProvenance.ODPT_LIVE -> BadgeTone.FOREST
                        DataProvenance.SCHEDULE_REF -> BadgeTone.BLUE
                        DataProvenance.ESTIMATE_ONLY -> BadgeTone.NEUTRAL
                    }
                )
            }
            Text(
                text = "¥$fareFmt",
                style = TabularNumericLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "所要 ${route.totalMinutes}分 ・ 乗換 ${route.transferCount}回 ・ 徒歩 ${route.totalWalkMeters}m",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (isExpanded) "詳細を閉じる" else "詳細を開く"
            )
        }

        if (route.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                route.tags.take(3).forEach { tag ->
                    TraloStatusBadge(label = tag, tone = BadgeTone.GOLD)
                }
            }
        }

        if (!route.delayAlert.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            TraloStatusBadge(label = "運行情報: ${route.delayAlert}", tone = BadgeTone.AMBER)
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))
                route.legs.forEachIndexed { idx, leg ->
                    TransitLegRow(
                        leg = leg,
                        onOpenPartner = { url -> PartnerLinks.openExternalUrl(context, url) }
                    )
                    if (idx < route.legs.lastIndex) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "出典・信頼性: ${route.reliabilityNote}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onBookmark,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("保存")
                    }
                    Button(
                        onClick = onAddToTrip,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("旅程に追加")
                    }
                }
            }
        }
    }
}

@Composable
private fun TransitLegRow(
    leg: TransitLeg,
    onOpenPartner: (String) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = iconForMode(leg.mode),
                        contentDescription = leg.mode.labelJa,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = leg.lineName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "${leg.departureTime} - ${leg.arrivalTime}",
                    style = TabularNumericMedium
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(leg.departureStation, style = MaterialTheme.typography.bodyMedium)
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                Text(leg.arrivalStation, style = MaterialTheme.typography.bodyMedium)
                if (leg.platform.isNotBlank()) {
                    TraloStatusBadge(label = leg.platform, tone = BadgeTone.NEUTRAL)
                }
            }

            if (leg.carGuidance.supported) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "乗車案内: ${leg.carGuidance.formationCars}両編成 ・ ${leg.carGuidance.recommendedCar}（${leg.carGuidance.doorPosition}）",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        if (leg.carGuidance.facilityNote.isNotBlank()) {
                            Text(
                                text = leg.carGuidance.facilityNote,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        if (leg.carGuidance.accessibilityNote.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.AutoMirrored.Outlined.Accessible, contentDescription = "バリアフリー", modifier = Modifier.size(14.dp))
                                Text(
                                    text = leg.carGuidance.accessibilityNote,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            } else if (!leg.carGuidance.unsupportedReason.isNullOrBlank()) {
                Text(
                    text = leg.carGuidance.unsupportedReason,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!leg.bookingUrl.isNullOrBlank() && !leg.bookingPartnerName.isNullOrBlank()) {
                TextButton(
                    onClick = { onOpenPartner(leg.bookingUrl) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("${leg.bookingPartnerName}で予約・時刻確認")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun StationFacilityCard(facility: StationFacilityInfo) {
    TraloCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${facility.stationName} 構内・乗換案内",
                style = MaterialTheme.typography.titleMedium
            )
            TraloStatusBadge(
                label = if (facility.supported) "構内データ対応" else "未対応",
                tone = if (facility.supported) BadgeTone.FOREST else BadgeTone.NEUTRAL
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (facility.supported) {
            Text("ホーム: ${facility.platformSummary}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("改札口: ${facility.gateSummary}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("乗換動線: ${facility.transferCorridorNote}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "バリアフリー: ${facility.barrierFreeNote}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                text = facility.unsupportedReason ?: "この駅の構内案内は現在未対応です。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OdptLiveStatusPanel(
    odptState: OdptApiState,
    selectedRailwayId: String,
    onSelectRailway: (String) -> Unit,
    onRefresh: () -> Unit
) {
    TraloCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("列車走行位置・運行状況（ODPT API v4）", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "公共交通オープンデータセンター連携",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRefresh, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Outlined.Refresh, contentDescription = "運行状況を更新")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        val odptRailways = listOf(
            "odpt.Railway:TokyoMetro.Ginza" to "東京メトロ銀座線",
            "odpt.Railway:TokyoMetro.Marunouchi" to "東京メトロ丸ノ内線",
            "odpt.Railway:Toei.Asakusa" to "都営浅草線",
            "odpt.Railway:Toei.Oedo" to "都営大江戸線"
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(odptRailways) { (id, label) ->
                FilterChip(
                    selected = selectedRailwayId == id,
                    onClick = { onSelectRailway(id) },
                    label = { Text(label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        when (odptState) {
            OdptApiState.Idle -> {
                Text("更新ボタンを押すと最新の運行情報を取得します。", style = MaterialTheme.typography.bodyMedium)
            }
            OdptApiState.Loading -> TraloSkeletonCard()
            OdptApiState.NotConfigured -> {
                TraloInfoNotice(
                    title = "ODPT APIキー未設定（ライブ位置非表示）",
                    body = "リアルタイムの列車走行位置は ODPT_API_KEY を AI Studio Secrets（または環境変数）に設定すると表示されます。非対応路線や未接続時に架空の列車位置を表示することはありません。"
                )
            }
            is OdptApiState.Error -> {
                TraloErrorCard(message = odptState.message, onRetry = onRefresh)
            }
            is OdptApiState.Available -> {
                TraloStatusBadge(
                    label = "ODPT Live 接続済 ・ 更新 ${odptState.updatedAt}",
                    tone = BadgeTone.FOREST
                )
                Spacer(modifier = Modifier.height(8.dp))
                odptState.trainInfos.take(4).forEach { info ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${info.operatorTitle} ${info.railwayTitle}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        TraloStatusBadge(
                            label = info.statusText,
                            tone = if (info.isDelayed) BadgeTone.AMBER else BadgeTone.FOREST
                        )
                    }
                }
                if (odptState.liveTrains.isEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "選択路線で現在取得可能な列車在線データはありません。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("在線中の列車（${odptState.liveTrains.size}本）", style = MaterialTheme.typography.labelLarge)
                    odptState.liveTrains.take(6).forEach { train ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "列車 ${train.trainNumber} (${train.trainType}) → ${train.destinationStation}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (train.delayMinutes > 0) "+${train.delayMinutes}分遅れ" else "${train.fromStation}付近",
                                style = TabularNumericMedium,
                                color = if (train.delayMinutes > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun iconForMode(mode: TransportMode): ImageVector = when (mode) {
    TransportMode.RAIL -> Icons.Outlined.Train
    TransportMode.SUBWAY, TransportMode.TRAM -> Icons.Outlined.DirectionsSubway
    TransportMode.LOCAL_BUS, TransportMode.HIGHWAY_BUS, TransportMode.AIRPORT_BUS -> Icons.Outlined.DirectionsBus
    TransportMode.FLIGHT -> Icons.Outlined.Flight
    TransportMode.FERRY -> Icons.Outlined.DirectionsBoat
    TransportMode.WALK -> Icons.AutoMirrored.Outlined.DirectionsWalk
    TransportMode.BICYCLE, TransportMode.SHARE_CYCLE -> Icons.Outlined.PedalBike
    TransportMode.TAXI -> Icons.Outlined.LocalTaxi
}
