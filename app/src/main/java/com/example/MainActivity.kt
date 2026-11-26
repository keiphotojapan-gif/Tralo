package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DirectionsTransit
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.OdptConfig
import com.example.core.configureAppCheck
import com.example.data.repository.AuthRepository
import com.example.data.repository.authStateFlow
import com.example.ui.components.BadgeTone
import com.example.ui.components.TraloStatusBadge
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ConciergeScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ItineraryScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.screens.TransitScreen
import com.example.ui.theme.TraloTheme
import com.example.viewmodel.ConciergeViewModel
import com.example.viewmodel.HomeAndTripsViewModel
import com.example.viewmodel.TraloViewModelFactories
import com.example.viewmodel.TransitViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

object TraloRoutes {
    const val HOME = "home"
    const val TRANSIT = "transit"
    const val ITINERARY = "itinerary"
    const val TODAY = "today"
}

private enum class TraloNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME(TraloRoutes.HOME, "ホーム", Icons.Filled.Explore, Icons.Outlined.Explore, "nav_home"),
    TRANSIT(TraloRoutes.TRANSIT, "交通検索", Icons.Filled.DirectionsTransit, Icons.Outlined.DirectionsTransit, "nav_transit"),
    ITINERARY(TraloRoutes.ITINERARY, "旅程", Icons.Filled.FlightTakeoff, Icons.Outlined.FlightTakeoff, "nav_itinerary"),
    TODAY(TraloRoutes.TODAY, "旅行当日", Icons.Filled.Today, Icons.Outlined.Today, "nav_today")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure Firebase App Check BEFORE setContent or any ViewModel/Firebase usage
        configureAppCheck(this, intent)

        setContent {
            TraloTheme {
                TraloAppRoot()
            }
        }
    }
}

@Composable
fun TraloAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            AuthRepository.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                scope = scope,
                onAuthSuccess = {},
                onUnauthenticated = {}
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            TraloAuthenticatedShell(
                currentUserId = user.uid,
                userEmail = user.email ?: user.displayName ?: "TRALO メンバー",
                onSignOut = {
                    AuthRepository.signOut(credentialManager, scope)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraloAuthenticatedShell(
    currentUserId: String,
    userEmail: String,
    onSignOut: () -> Unit
) {
    val homeTripsVm: HomeAndTripsViewModel = viewModel(
        key = "home_trips_$currentUserId",
        factory = TraloViewModelFactories.homeAndTripsFactory(currentUserId)
    )
    val transitVm: TransitViewModel = viewModel(
        key = "transit_$currentUserId",
        factory = TraloViewModelFactories.transitFactory(currentUserId)
    )
    val conciergeVm: ConciergeViewModel = viewModel(
        key = "concierge_$currentUserId",
        factory = TraloViewModelFactories.conciergeFactory(currentUserId)
    )

    val tripsState by homeTripsVm.tripsState.collectAsStateWithLifecycle()
    val activeTrip by homeTripsVm.activeTrip.collectAsStateWithLifecycle()
    val activeItemsState by homeTripsVm.activeTripItemsState.collectAsStateWithLifecycle()
    val activeChecklistState by homeTripsVm.activeChecklistState.collectAsStateWithLifecycle()
    val weatherState by homeTripsVm.weatherState.collectAsStateWithLifecycle()
    val odptSummaryState by homeTripsVm.odptSummaryState.collectAsStateWithLifecycle()
    val recentSearches by homeTripsVm.recentSearches.collectAsStateWithLifecycle()
    val savedRoutesState by homeTripsVm.savedRoutesState.collectAsStateWithLifecycle()
    val actionBanner by homeTripsVm.actionBanner.collectAsStateWithLifecycle()

    val transitState by transitVm.uiState.collectAsStateWithLifecycle()

    val conciergeMessages by conciergeVm.messages.collectAsStateWithLifecycle()
    val conciergeUiState by conciergeVm.uiState.collectAsStateWithLifecycle()
    val pendingProposal by conciergeVm.pendingConfirmProposal.collectAsStateWithLifecycle()
    val conciergeSavedBanner by conciergeVm.savedConfirmationBanner.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: TraloRoutes.HOME

    var showConcierge by remember { mutableStateOf(false) }
    var conciergeInitialPrompt by remember { mutableStateOf<String?>(null) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionBanner) {
        val msg = actionBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            homeTripsVm.clearBanner()
        }
    }

    fun navigateTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openConcierge(prompt: String? = null) {
        conciergeInitialPrompt = prompt
        showConcierge = true
    }

    if (showConcierge) {
        BackHandler { showConcierge = false }
        val weatherDesc = (weatherState as? UiState.Success)?.data?.let {
            "${it.locationName}: ${it.conditionLabelJa} ${it.currentTempC.toInt()}°C"
        } ?: "天気情報取得中"
        val tripDesc = activeTrip?.let {
            "旅程: ${it.title} (${it.destination}, ${it.startDate}〜${it.endDate}, 予算¥${it.budgetJpy})"
        } ?: "選択中旅程なし"
        val contextSummary = "$tripDesc / $weatherDesc / 検索中経路: ${transitState.origin}→${transitState.destination}"

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            ConciergeScreen(
                messages = conciergeMessages,
                uiState = conciergeUiState,
                contextSummary = contextSummary,
                pendingProposal = pendingProposal,
                savedBanner = conciergeSavedBanner,
                initialPrompt = conciergeInitialPrompt,
                onSendChat = { prompt, ctx -> conciergeVm.sendChatMessage(prompt, ctx) },
                onGenerateTripDraft = { prompt -> conciergeVm.generateTripDraft(prompt) },
                onRequestSaveProposal = { id, prop -> conciergeVm.requestSaveConfirmation(id, prop) },
                onConfirmSaveProposal = { conciergeVm.confirmAndSaveProposal() },
                onDismissSaveProposal = { conciergeVm.dismissSaveConfirmation() },
                onClearBanner = { conciergeVm.clearBanner() },
                onClearError = { conciergeVm.clearError() },
                onClose = { showConcierge = false }
            )
        }
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(292.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(20.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "TRALO",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TraloStatusBadge(
                        label = if (OdptConfig.isConfigured()) "ODPT API 接続設定済" else "ODPT API 未設定",
                        tone = if (OdptConfig.isConfigured()) BadgeTone.FOREST else BadgeTone.AMBER
                    )
                    HorizontalDivider()
                    TraloNavItem.entries.forEach { item ->
                        NavigationDrawerItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navigateTab(item.route)
                                drawerScope.launch { drawerState.close() }
                            },
                            icon = { Icon(item.unselectedIcon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                    NavigationDrawerItem(
                        selected = false,
                        onClick = {
                            drawerScope.launch { drawerState.close() }
                            openConcierge(null)
                        },
                        icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = "AIコンシェルジュ") },
                        label = { Text("AI トラベルコンシェルジュ") }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    HorizontalDivider()
                    NavigationDrawerItem(
                        selected = false,
                        onClick = {
                            drawerScope.launch { drawerState.close() }
                            onSignOut()
                        },
                        icon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "サインアウト") },
                        label = { Text("サインアウト") },
                        modifier = Modifier.testTag("sign_out_button")
                    )
                }
            }
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 760.dp

            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        navigationIcon = {
                            IconButton(
                                onClick = { drawerScope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("menu_drawer_button")
                            ) {
                                Icon(Icons.Outlined.Menu, contentDescription = "メニューを開く")
                            }
                        },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("TRALO", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                                activeTrip?.let { trip ->
                                    TraloStatusBadge(label = trip.destination, tone = BadgeTone.GOLD)
                                }
                            }
                        }
                    )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { openConcierge(null) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null) },
                        text = { Text("AIコンシェルジュ") },
                        modifier = Modifier
                            .navigationBarsPadding()
                            .testTag("concierge_fab")
                    )
                },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar {
                            TraloNavItem.entries.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTab(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = { Text(item.label) },
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail(
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TraloNavItem.entries.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTab(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = { Text(item.label) },
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = TraloRoutes.HOME,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable(TraloRoutes.HOME) {
                            HomeScreen(
                                tripsState = tripsState,
                                activeTrip = activeTrip,
                                weatherState = weatherState,
                                odptSummaryState = odptSummaryState,
                                recentSearches = recentSearches,
                                savedRoutesState = savedRoutesState,
                                onSearchTransit = { origin, dest ->
                                    transitVm.setOrigin(origin)
                                    transitVm.setDestination(dest)
                                    transitVm.search()
                                    navigateTab(TraloRoutes.TRANSIT)
                                },
                                onOpenTransitTab = { navigateTab(TraloRoutes.TRANSIT) },
                                onOpenItineraryTab = { navigateTab(TraloRoutes.ITINERARY) },
                                onOpenTodayTab = { navigateTab(TraloRoutes.TODAY) },
                                onOpenConcierge = { prompt -> openConcierge(prompt) },
                                onSelectTrip = { id -> homeTripsVm.selectTrip(id) },
                                onCreateStarterTrip = { homeTripsVm.createKyotoStarterTrip() },
                                onRefreshWeather = {
                                    val trip = activeTrip
                                    if (trip != null) {
                                        homeTripsVm.refreshWeather(trip.destination, trip.latitude, trip.longitude, true)
                                    } else {
                                        homeTripsVm.refreshWeather("東京", 35.6812, 139.7671, true)
                                    }
                                }
                            )
                        }

                        composable(TraloRoutes.TRANSIT) {
                            TransitScreen(
                                state = transitState,
                                onOriginChange = transitVm::setOrigin,
                                onDestinationChange = transitVm::setDestination,
                                onSwapStations = transitVm::swapStations,
                                onTimeModeChange = transitVm::setTimeMode,
                                onAdjustMinutes = transitVm::adjustTimeMinutes,
                                onSortChange = transitVm::setSortPreference,
                                onSeatChange = transitVm::setSeatPreference,
                                onMaxTransfersChange = transitVm::setMaxTransfers,
                                onToggleMode = transitVm::toggleMode,
                                onSearch = transitVm::search,
                                onRefreshOdpt = transitVm::refreshOdptAndSearch,
                                onSelectOdptRailway = transitVm::selectOdptRailway,
                                onSelectRoute = transitVm::selectRoute,
                                onSaveRoute = transitVm::saveRouteToCloud,
                                onAddRouteToTrip = { option -> homeTripsVm.addRouteToActiveTrip(option) },
                                onCoordinatesResolved = transitVm::setOriginFromCoordinates,
                                onClearStatusMessage = transitVm::clearStatusMessage
                            )
                        }

                        composable(TraloRoutes.ITINERARY) {
                            ItineraryScreen(
                                tripsState = tripsState,
                                activeTrip = activeTrip,
                                itemsState = activeItemsState,
                                weatherState = weatherState,
                                onSelectTrip = homeTripsVm::selectTrip,
                                onCreateTrip = homeTripsVm::createTrip,
                                onCreateStarterTrip = homeTripsVm::createKyotoStarterTrip,
                                onUpdateTripStatus = homeTripsVm::setTripStatus,
                                onDeleteTrip = homeTripsVm::deleteTrip,
                                onAddItem = homeTripsVm::addItineraryItem,
                                onUpdateBookingStatus = homeTripsVm::updateBookingStatus,
                                onDeleteItem = homeTripsVm::deleteItineraryItem,
                                onRefreshWeather = {
                                    val trip = activeTrip
                                    if (trip != null) {
                                        homeTripsVm.refreshWeather(trip.destination, trip.latitude, trip.longitude, true)
                                    }
                                },
                                onOpenConciergeWithPrompt = { prompt -> openConcierge(prompt) }
                            )
                        }

                        composable(TraloRoutes.TODAY) {
                            TodayScreen(
                                activeTrip = activeTrip,
                                itemsState = activeItemsState,
                                checklistState = activeChecklistState,
                                weatherState = weatherState,
                                odptState = transitState.odptState,
                                onCreateStarterTrip = homeTripsVm::createKyotoStarterTrip,
                                onOpenTransitTab = { navigateTab(TraloRoutes.TRANSIT) },
                                onRefreshOdpt = transitVm::refreshOdptAndSearch,
                                onRefreshWeather = {
                                    val trip = activeTrip
                                    if (trip != null) {
                                        homeTripsVm.refreshWeather(trip.destination, trip.latitude, trip.longitude, true)
                                    }
                                },
                                onAddChecklist = homeTripsVm::addChecklistItem,
                                onToggleChecklist = homeTripsVm::toggleChecklistItem,
                                onDeleteChecklist = homeTripsVm::deleteChecklistItem,
                                onOpenConcierge = { prompt -> openConcierge(prompt) }
                            )
                        }
                    }
                }
            }
        }
    }
}
