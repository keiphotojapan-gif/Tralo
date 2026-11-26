package com.example

import com.example.core.OdptConfig
import com.example.data.remote.OdptApi
import com.example.data.remote.OdptTrainDto
import com.example.data.remote.OdptTrainInformationDto
import com.example.data.remote.TransitKnowledgeBase
import com.example.data.repository.TransitRepository
import com.example.model.OdptApiState
import com.example.model.SeatPreference
import com.example.model.SortPreference
import com.example.model.TransportMode
import com.example.model.WmoWeatherInterpreter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private val fakeOdptApi = object : OdptApi {
        override suspend fun getTrainInformation(
            consumerKey: String,
            operatorId: String?
        ): List<OdptTrainInformationDto> = emptyList()

        override suspend fun getTrains(
            consumerKey: String,
            railwayId: String?
        ): List<OdptTrainDto> = emptyList()
    }

    @Test
    fun odptConfig_defaultPlaceholderIsNotConfigured() {
        assertFalse(OdptConfig.isConfigured())
    }

    @Test
    fun transitRepository_returnsNotConfiguredWhenKeyMissing() = runTest {
        val repo = TransitRepository(fakeOdptApi)
        val result = repo.fetchLiveOdptStatus()
        assertEquals(OdptApiState.NotConfigured, result)
    }

    @Test
    fun buildRouteCandidates_tokyoToKyotoIncludesRailAndBus() {
        val routes = TransitKnowledgeBase.buildRouteCandidates(
            originRaw = "東京",
            destinationRaw = "京都",
            baseMinutesOfDay = 9 * 60,
            seatPreference = SeatPreference.RESERVED,
            enabledModes = setOf(
                TransportMode.RAIL,
                TransportMode.SUBWAY,
                TransportMode.HIGHWAY_BUS,
                TransportMode.FLIGHT
            )
        )
        assertTrue(routes.size >= 2)
        val railRoute = routes.firstOrNull { it.id == "route_rail_fast" }
        assertTrue(railRoute != null)
        assertTrue(railRoute!!.legs.any { it.carGuidance.supported })
        assertTrue(railRoute.legs.any { !it.carGuidance.supported })
    }

    @Test
    fun searchRoutes_respectsSortingAndTransferLimit() {
        val repo = TransitRepository(fakeOdptApi)
        val fastest = repo.searchRoutes(
            origin = "東京",
            destination = "京都",
            baseMinutesOfDay = 540,
            sortPreference = SortPreference.FASTEST,
            seatPreference = SeatPreference.ANY,
            enabledModes = TransportMode.entries.toSet(),
            maxTransfers = null,
            odptState = OdptApiState.NotConfigured
        )
        assertTrue(fastest.zipWithNext().all { (a, b) -> a.totalMinutes <= b.totalMinutes })

        val cheapest = repo.searchRoutes(
            origin = "東京",
            destination = "京都",
            baseMinutesOfDay = 540,
            sortPreference = SortPreference.CHEAPEST,
            seatPreference = SeatPreference.ANY,
            enabledModes = TransportMode.entries.toSet(),
            maxTransfers = null,
            odptState = OdptApiState.NotConfigured
        )
        assertTrue(cheapest.zipWithNext().all { (a, b) -> a.totalFareJpy <= b.totalFareJpy })

        val directOnly = repo.searchRoutes(
            origin = "東京",
            destination = "京都",
            baseMinutesOfDay = 540,
            sortPreference = SortPreference.FEWEST_TRANSFERS,
            seatPreference = SeatPreference.ANY,
            enabledModes = TransportMode.entries.toSet(),
            maxTransfers = 0,
            odptState = OdptApiState.NotConfigured
        )
        assertTrue(directOnly.all { it.transferCount == 0 })
    }

    @Test
    fun stationFacility_distinguishesSupportedAndUnsupportedStations() {
        val repo = TransitRepository(fakeOdptApi)
        val tokyo = repo.stationFacilityFor("東京")
        val sapporo = repo.stationFacilityFor("札幌")
        val unknown = repo.stationFacilityFor("架空の駅")

        assertTrue(tokyo.supported)
        assertFalse(sapporo.supported)
        assertFalse(unknown.supported)
        assertTrue(!unknown.unsupportedReason.isNullOrBlank())
    }

    @Test
    fun wmoWeatherInterpreter_mapsClearAndRainCorrectly() {
        assertEquals("快晴", WmoWeatherInterpreter.labelFor(0))
        assertFalse(WmoWeatherInterpreter.isRainOrStorm(0))
        assertEquals("雨", WmoWeatherInterpreter.labelFor(61))
        assertTrue(WmoWeatherInterpreter.isRainOrStorm(61))
    }

    @Test
    fun formatHm_wrapsMidnightProperly() {
        assertEquals("09:05", TransitKnowledgeBase.formatHm(9 * 60 + 5))
        assertEquals("00:15", TransitKnowledgeBase.formatHm(24 * 60 + 15))
    }
}
