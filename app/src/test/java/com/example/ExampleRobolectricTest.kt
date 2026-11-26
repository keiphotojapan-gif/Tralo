package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.core.PartnerLinks
import com.example.data.remote.TransitKnowledgeBase
import com.example.model.BookingStatus
import com.example.model.ChecklistItem
import com.example.model.DailyWeatherPoint
import com.example.model.HourlyWeatherPoint
import com.example.model.ItineraryCategory
import com.example.model.ItineraryItem
import com.example.model.OdptApiState
import com.example.model.SeatPreference
import com.example.model.TransportMode
import com.example.model.Trip
import com.example.model.TripStatus
import com.example.model.WeatherForecast
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.screens.TransitScreen
import com.example.ui.theme.TraloTheme
import com.example.viewmodel.TransitSearchUiState
import com.example.viewmodel.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-420dpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleWeather = WeatherForecast(
        locationName = "京都",
        currentTempC = 19.0,
        apparentTempC = 18.0,
        humidityPercent = 58,
        windSpeedKmh = 9.5,
        uvIndexMax = 4.2,
        precipitationMm = 0.0,
        weatherCode = 1,
        conditionLabelJa = "晴れ・薄曇り",
        advisoryJa = null,
        isRainyRisk = false,
        hourly = listOf(HourlyWeatherPoint("09:00", 17.0, 10, 1)),
        daily = listOf(DailyWeatherPoint("2026-04-10", 21.0, 11.0, 15, 4.5, 1)),
        fetchedAtLabel = "08:30"
    )

    @Test
    fun appResources_matchTraloAndFirebaseDatabaseId() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("TRALO", context.getString(R.string.app_name))
        val dbId = context.getString(R.string.firestore_database_id)
        assertTrue(dbId.isNotBlank())
    }

    @Test
    fun partnerLinks_buildValidBookingAndKlookUrls() {
        val bookingUrl = PartnerLinks.bookingComSearchUrl("京都", "2026-04-10", "2026-04-12")
        val klookUrl = PartnerLinks.klookSearchUrl("嵐山 観光パス")
        val mapsUrl = PartnerLinks.mapsSearchUrl("京都駅")

        assertTrue(bookingUrl.startsWith("https://www.booking.com/"))
        assertTrue(bookingUrl.contains("checkin=2026-04-10"))
        assertTrue(klookUrl.startsWith("https://www.klook.com/"))
        assertTrue(mapsUrl.startsWith("https://www.google.com/maps/"))
    }

    @Test
    fun authScreen_displaysEditorialTitleAndSignInButton() {
        composeRule.setContent {
            TraloTheme {
                AuthScreen(onAuthSuccess = {})
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("google_sign_in_button")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Googleでサインインして始める")
            .assertIsDisplayed()
    }

    @Test
    fun homeScreen_displaysSearchBarWeatherAndTriggersSearch() {
        var searchedPair: Pair<String, String>? = null
        composeRule.setContent {
            TraloTheme {
                HomeScreen(
                    tripsState = UiState.Success(emptyList()),
                    activeTrip = null,
                    weatherState = UiState.Success(sampleWeather),
                    odptSummaryState = OdptApiState.NotConfigured,
                    recentSearches = emptyList(),
                    savedRoutesState = UiState.Success(emptyList()),
                    onSearchTransit = { origin, dest -> searchedPair = origin to dest },
                    onOpenTransitTab = {},
                    onOpenItineraryTab = {},
                    onOpenTodayTab = {},
                    onOpenConcierge = {},
                    onSelectTrip = {},
                    onCreateStarterTrip = {},
                    onRefreshWeather = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_list")
            .performScrollToNode(hasTestTag("home_destination_input"))
        composeRule.onNodeWithTag("home_origin_input").assertIsDisplayed()
        composeRule.onNodeWithTag("home_destination_input").performTextInput("京都")
        composeRule.onNodeWithTag("home_list")
            .performScrollToNode(hasTestTag("home_search_transit_button"))
        composeRule.onNodeWithTag("home_search_transit_button").performClick()
        composeRule.waitForIdle()

        assertEquals("東京" to "京都", searchedPair)
    }

    @Test
    fun transitScreen_rendersRouteResultsAndOdptNotConfiguredState() {
        val candidates = TransitKnowledgeBase.buildRouteCandidates(
            originRaw = "東京",
            destinationRaw = "京都",
            baseMinutesOfDay = 9 * 60,
            seatPreference = SeatPreference.RESERVED,
            enabledModes = setOf(TransportMode.RAIL, TransportMode.SUBWAY, TransportMode.HIGHWAY_BUS)
        )
        composeRule.setContent {
            TraloTheme {
                TransitScreen(
                    state = TransitSearchUiState(
                        routes = candidates,
                        selectedRouteId = candidates.first().id,
                        odptState = OdptApiState.NotConfigured
                    ),
                    onOriginChange = {},
                    onDestinationChange = {},
                    onSwapStations = {},
                    onTimeModeChange = {},
                    onAdjustMinutes = {},
                    onSortChange = {},
                    onSeatChange = {},
                    onMaxTransfersChange = {},
                    onToggleMode = {},
                    onSearch = {},
                    onRefreshOdpt = {},
                    onSelectOdptRailway = {},
                    onSelectRoute = {},
                    onSaveRoute = { _, _ -> },
                    onAddRouteToTrip = {},
                    onCoordinatesResolved = { _, _ -> },
                    onClearStatusMessage = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("transit_list")
            .performScrollToNode(hasTestTag("transit_search_button"))
        composeRule.onNodeWithTag("transit_search_button").assertIsDisplayed()

        composeRule.onNodeWithTag("transit_list")
            .performScrollToNode(hasTestTag("route_card_route_rail_fast"))
        composeRule.onNodeWithTag("route_card_route_rail_fast").assertIsDisplayed()
    }

    @Test
    fun todayScreen_rendersPriorityTransitAndAddsChecklistItem() {
        var addedChecklist: Pair<String, String>? = null
        val trip = Trip(
            id = "trip_kyoto",
            userId = "user_1",
            title = "春の京都・嵐山 2泊3日",
            destination = "京都",
            startDate = "2026-04-10",
            endDate = "2026-04-12",
            budgetJpy = 95000L,
            spentJpy = 42000L,
            status = TripStatus.ACTIVE.name
        )
        val items = listOf(
            ItineraryItem(
                id = "item_1",
                userId = "user_1",
                tripId = "trip_kyoto",
                dayIndex = 1L,
                startTime = "08:30",
                endTime = "10:45",
                title = "東海道新幹線 のぞみ",
                category = ItineraryCategory.TRANSIT.name,
                locationName = "東京駅 → 京都駅",
                platform = "16番線",
                carNumber = "7号車",
                seatNumber = "12D",
                bookingStatus = BookingStatus.CONFIRMED.name
            )
        )
        val checklist = listOf(
            ChecklistItem(id = "chk_1", userId = "user_1", tripId = "trip_kyoto", label = "EX乗車票", checked = true)
        )

        composeRule.setContent {
            TraloTheme {
                TodayScreen(
                    activeTrip = trip,
                    itemsState = UiState.Success(items),
                    checklistState = UiState.Success(checklist),
                    weatherState = UiState.Success(sampleWeather),
                    odptState = OdptApiState.NotConfigured,
                    onCreateStarterTrip = {},
                    onOpenTransitTab = {},
                    onRefreshOdpt = {},
                    onRefreshWeather = {},
                    onAddChecklist = { tripId, label -> addedChecklist = tripId to label },
                    onToggleChecklist = { _, _, _ -> },
                    onDeleteChecklist = { _, _ -> },
                    onOpenConcierge = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("NEXT BOARDING ・ 本日の乗車予定").assertIsDisplayed()
        composeRule.onAllNodesWithText("東海道新幹線 のぞみ")[0].assertIsDisplayed()
        composeRule.onNodeWithTag("today_list")
            .performScrollToNode(hasTestTag("checklist_input"))
        composeRule.onNodeWithTag("checklist_input").performTextInput("折りたたみ傘")
        composeRule.onNodeWithTag("today_list")
            .performScrollToNode(hasTestTag("checklist_add_button"))
        composeRule.onNodeWithTag("checklist_add_button").performClick()
        composeRule.waitForIdle()

        assertEquals("trip_kyoto" to "折りたたみ傘", addedChecklist)
    }

    @Test
    fun transitScreen_liveMapTabRendersTrainsAndInspectsDetailsOnTap() {
        val sampleTrain = com.example.model.Train(
            trainId = "odpt.Train:TokyoMetro.Ginza.A1001",
            operatorId = "TokyoMetro",
            lineId = "odpt.Railway:TokyoMetro.Ginza",
            trainNumber = "A1001",
            trainType = "各駅停車",
            destination = "浅草",
            direction = "Asakusa",
            currentStation = "渋谷",
            nextStation = "表参道",
            actualPosition = com.example.model.TrainPosition(
                trainId = "odpt.Train:TokyoMetro.Ginza.A1001",
                latitude = 35.6612,
                longitude = 139.7065,
                bearing = 48f,
                currentStation = "渋谷",
                nextStation = "表参道",
                progress = 0.45f,
                delayMinutes = 2,
                timestamp = "09:15",
                dataSource = "ODPT",
                confidence = 0.9f
            ),
            delayMinutes = 2,
            realtimeStatus = "2分遅れ",
            lastUpdated = "09:15"
        )
        var tappedTrainId: String? = null
        composeRule.setContent {
            TraloTheme {
                TransitScreen(
                    state = TransitSearchUiState(
                        activeTab = com.example.viewmodel.TransitSectionTab.LIVE_POSITIONS,
                        liveTrainsResult = com.example.model.ProviderResult.Success(
                            data = listOf(sampleTrain),
                            sourceName = "ODPT",
                            updatedAt = "09:15"
                        ),
                        selectedTrainId = sampleTrain.trainId
                    ),
                    onOriginChange = {},
                    onDestinationChange = {},
                    onSwapStations = {},
                    onTimeModeChange = {},
                    onAdjustMinutes = {},
                    onSortChange = {},
                    onSeatChange = {},
                    onMaxTransfersChange = {},
                    onToggleMode = {},
                    onSearch = {},
                    onRefreshOdpt = {},
                    onSelectOdptRailway = {},
                    onSelectRoute = {},
                    onSaveRoute = { _, _ -> },
                    onAddRouteToTrip = {},
                    onCoordinatesResolved = { _, _ -> },
                    onClearStatusMessage = {},
                    onSelectTrain = { tappedTrainId = it }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("live_train_map_canvas").assertIsDisplayed()
        composeRule.onNodeWithTag("map_train_marker_A1001").performClick()
        composeRule.waitForIdle()
        assertEquals(null, tappedTrainId)
        composeRule.onNodeWithTag("transit_live_map_list")
            .performScrollToNode(hasTestTag("selected_train_detail_card"))
        composeRule.onNodeWithTag("selected_train_detail_card").assertIsDisplayed()
    }

    @Test
    fun transitScreen_coverageAndTimetableTabsDisplayAccurately() {
        var currentTab = com.example.viewmodel.TransitSectionTab.TIMETABLE
        composeRule.setContent {
            TraloTheme {
                TransitScreen(
                    state = TransitSearchUiState(
                        activeTab = currentTab,
                        timetableResult = com.example.model.ProviderResult.Success(
                            data = listOf(
                                com.example.model.StationTimetable(
                                    stationId = "odpt.Station:TokyoMetro.Ginza.Shibuya",
                                    stationName = "渋谷",
                                    lineId = "odpt.Railway:TokyoMetro.Ginza",
                                    direction = "浅草方面",
                                    departureTime = "09:04",
                                    trainType = "各停",
                                    destination = "浅草",
                                    platform = "2",
                                    trainNumber = "A0904",
                                    source = "ODPT"
                                )
                            ),
                            sourceName = "ODPT",
                            updatedAt = "09:00"
                        )
                    ),
                    onOriginChange = {},
                    onDestinationChange = {},
                    onSwapStations = {},
                    onTimeModeChange = {},
                    onAdjustMinutes = {},
                    onSortChange = {},
                    onSeatChange = {},
                    onMaxTransfersChange = {},
                    onToggleMode = {},
                    onSearch = {},
                    onRefreshOdpt = {},
                    onSelectOdptRailway = {},
                    onSelectRoute = {},
                    onSaveRoute = { _, _ -> },
                    onAddRouteToTrip = {},
                    onCoordinatesResolved = { _, _ -> },
                    onClearStatusMessage = {},
                    onSelectTab = { currentTab = it }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("transit_tab_timetable").assertIsDisplayed()
        composeRule.onNodeWithTag("transit_tab_coverage").assertIsDisplayed()
        composeRule.onNodeWithTag("transit_timetable_list")
            .performScrollToNode(hasTestTag("timetable_results_card"))
        composeRule.onNodeWithTag("timetable_results_card").assertIsDisplayed()
    }
}
