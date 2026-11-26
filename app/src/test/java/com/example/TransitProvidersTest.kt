package com.example

import com.example.data.provider.EkispertProvider
import com.example.data.provider.GeoTrackMath
import com.example.data.provider.JorudanProvider
import com.example.data.provider.MixwayRealtimeProvider
import com.example.data.provider.NavitimeProvider
import com.example.data.provider.OdptRealtimeProvider
import com.example.data.provider.OperatorOfficialProvider
import com.example.data.remote.ApiCacheAndRateLimiter
import com.example.data.remote.NationalRailwayRegistry
import com.example.data.remote.NetworkModule
import com.example.data.repository.TransitRepository
import com.example.model.OdptApiState
import com.example.model.ProviderCapability
import com.example.model.ProviderResult
import com.example.model.RouteSearchRequest
import com.example.model.TimeQueryMode
import com.example.model.Train
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.net.SocketTimeoutException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransitProvidersTest {

    private fun mockClient(handler: (okhttp3.Request) -> Response): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain -> handler(chain.request()) })
            .build()

    private fun jsonResponse(request: okhttp3.Request, code: Int, body: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code in 200..299) "OK" else "Error")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()

    @Test
    fun odptProvider_parsesValidTrainsInterpolatesCoordinatesAndBearing() = runTest {
        val client = mockClient { req ->
            val url = req.url.toString()
            when {
                url.contains("odpt:Station") -> jsonResponse(
                    req, 200,
                    """[
                      {"owl:sameAs":"odpt.Station:TokyoMetro.Ginza.Shibuya","dc:title":"渋谷","odpt:stationTitle":{"ja":"渋谷","en":"Shibuya"},"geo:lat":35.6580,"geo:long":139.7016},
                      {"owl:sameAs":"odpt.Station:TokyoMetro.Ginza.Omotesando","dc:title":"表参道","odpt:stationTitle":{"ja":"表参道","en":"Omotesando"},"geo:lat":35.6652,"geo:long":139.7125}
                    ]"""
                )
                url.contains("odpt:Train") -> jsonResponse(
                    req, 200,
                    """[
                      {
                        "owl:sameAs":"odpt.Train:TokyoMetro.Ginza.A1201",
                        "dc:date":"2026-04-10T09:12:00+09:00",
                        "odpt:operator":"odpt.Operator:TokyoMetro",
                        "odpt:trainNumber":"A1201",
                        "odpt:railway":"odpt.Railway:TokyoMetro.Ginza",
                        "odpt:trainType":"odpt.TrainType:TokyoMetro.Local",
                        "odpt:railDirection":"odpt.RailDirection:TokyoMetro.Asakusa",
                        "odpt:fromStation":"odpt.Station:TokyoMetro.Ginza.Shibuya",
                        "odpt:toStation":"odpt.Station:TokyoMetro.Ginza.Omotesando",
                        "odpt:destinationStation":["odpt.Station:TokyoMetro.Ginza.Asakusa"],
                        "odpt:delay":180
                      }
                    ]"""
                )
                else -> jsonResponse(req, 200, "[]")
            }
        }
        val provider = OdptRealtimeProvider(
            odptApi = NetworkModule.createOdptApi(client = client),
            apiKeyProvider = { "valid-test-key" },
            isConfiguredCheck = { true },
            timeFormatter = { "09:12" }
        )

        val result = provider.getTrainPositions("odpt.Railway:TokyoMetro.Ginza")
        assertTrue(result is ProviderResult.Success)
        val trains = (result as ProviderResult.Success).data
        assertEquals(1, trains.size)
        val train = trains.first()
        assertEquals("odpt.Train:TokyoMetro.Ginza.A1201", train.trainId)
        assertEquals("odpt.Railway:TokyoMetro.Ginza", train.lineId)
        assertEquals("A1201", train.trainNumber)
        assertEquals("浅草", train.destination)
        assertEquals(3, train.delayMinutes)
        assertEquals("渋谷", train.currentStation)
        assertEquals("表参道", train.nextStation)
        val pos = train.actualPosition!!
        assertTrue(pos.latitude in 35.658..35.666)
        assertTrue(pos.longitude in 139.701..139.713)
        assertTrue(pos.bearing in 30f..70f)
    }

    @Test
    fun odptProvider_handlesEmptyTrainsSuspendedStatusAndMalformedJson() = runTest {
        val emptyClient = mockClient { req ->
            when {
                req.url.toString().contains("odpt:TrainInformation") -> jsonResponse(
                    req, 200,
                    """[{
                      "dc:date":"2026-04-10T09:15:00+09:00",
                      "odpt:operator":"odpt.Operator:TokyoMetro",
                      "odpt:railway":"odpt.Railway:TokyoMetro.Ginza",
                      "odpt:trainInformationStatus":{"ja":"運転見合わせ"},
                      "odpt:trainInformationText":{"ja":"人身事故のため運転見合わせ中です。"}
                    }]"""
                )
                else -> jsonResponse(req, 200, "[]")
            }
        }
        val provider = OdptRealtimeProvider(
            odptApi = NetworkModule.createOdptApi(client = emptyClient),
            apiKeyProvider = { "valid-key" },
            isConfiguredCheck = { true }
        )

        val trainsRes = provider.getTrainPositions("odpt.Railway:TokyoMetro.Ginza")
        assertTrue(trainsRes is ProviderResult.Success && trainsRes.data.isEmpty())

        val statusRes = provider.getOperationStatus("odpt.Operator:TokyoMetro")
        assertTrue(statusRes is ProviderResult.Success)
        val statuses = (statusRes as ProviderResult.Success).data
        assertEquals("運転見合わせ", statuses.first().disruptionType)
        assertTrue(statuses.first().isDelayed)

        // Malformed JSON
        val badClient = mockClient { req -> jsonResponse(req, 200, "{broken_json:") }
        val badProvider = OdptRealtimeProvider(
            odptApi = NetworkModule.createOdptApi(client = badClient),
            apiKeyProvider = { "valid-key" },
            isConfiguredCheck = { true }
        )
        val errRes = badProvider.getTrainPositions("odpt.Railway:TokyoMetro.Ginza")
        assertTrue(errRes is ProviderResult.Error)
    }

    @Test
    fun odptProvider_handlesNotConfiguredAuthError401TimeoutAndRateLimit429() = runTest {
        val unconfigured = OdptRealtimeProvider(isConfiguredCheck = { false })
        assertTrue(unconfigured.getTrainPositions("odpt.Railway:TokyoMetro.Ginza") is ProviderResult.NotConfigured)

        val auth401 = OdptRealtimeProvider(
            odptApi = NetworkModule.createOdptApi(client = mockClient { req -> jsonResponse(req, 401, """{"error":"unauthorized"}""") }),
            apiKeyProvider = { "bad-key" },
            isConfiguredCheck = { true }
        )
        val authRes = auth401.getTrainPositions("odpt.Railway:TokyoMetro.Ginza")
        assertTrue(authRes is ProviderResult.Error && authRes.isAuthError)

        val timeoutProvider = OdptRealtimeProvider(
            odptApi = NetworkModule.createOdptApi(client = mockClient { throw SocketTimeoutException("read timed out") }),
            apiKeyProvider = { "valid" },
            isConfiguredCheck = { true }
        )
        assertTrue(timeoutProvider.getTrainPositions("odpt.Railway:TokyoMetro.Ginza") is ProviderResult.Error)

        val rateLimitProvider = OdptRealtimeProvider(
            odptApi = NetworkModule.createOdptApi(client = mockClient { req -> jsonResponse(req, 429, """{"error":"too many requests"}""") }),
            apiKeyProvider = { "valid" },
            isConfiguredCheck = { true }
        )
        assertTrue(rateLimitProvider.getTrainPositions("odpt.Railway:TokyoMetro.Ginza") is ProviderResult.RateLimited)
    }

    @Test
    fun odptProvider_usesStaleCacheOnNetworkErrorAfterExpiry() = runTest {
        var nowMs = 1_000_000L
        var shouldFail = false
        val cache = ApiCacheAndRateLimiter(clockMs = { nowMs })
        val client = mockClient { req ->
            if (shouldFail) throw IOException("offline")
            val url = req.url.toString()
            if (url.contains("odpt:Train")) {
                jsonResponse(req, 200, """[{"owl:sameAs":"odpt.Train:TokyoMetro.Ginza.B99","odpt:trainNumber":"B99","odpt:railway":"odpt.Railway:TokyoMetro.Ginza","odpt:fromStation":"odpt.Station:TokyoMetro.Ginza.Ginza","odpt:delay":0}]""")
            } else {
                jsonResponse(req, 200, "[]")
            }
        }
        val provider = OdptRealtimeProvider(
            odptApi = NetworkModule.createOdptApi(client = client),
            cacheLimiter = cache,
            apiKeyProvider = { "valid" },
            isConfiguredCheck = { true },
            timeFormatter = { "09:00" }
        )

        val first = provider.getTrainPositions("odpt.Railway:TokyoMetro.Ginza")
        assertTrue(first is ProviderResult.Success && !first.isStale)

        // Advance beyond 30s TTL and 20s cooldown, then simulate offline IOException
        nowMs += 45_000L
        shouldFail = true
        val fallback = provider.getTrainPositions("odpt.Railway:TokyoMetro.Ginza")
        assertTrue(fallback is ProviderResult.Success)
        val staleSuccess = fallback as ProviderResult.Success
        assertTrue(staleSuccess.isStale)
        assertTrue(staleSuccess.fromCache)
        assertEquals(1, staleSuccess.data.size)
    }

    @Test
    fun ekispertAndMixwayAndAggregator_searchRoutesEnrichDelaysAndSuggestDetours() = runTest {
        val ekiClient = mockClient { req ->
            val url = req.url.toString()
            when {
                url.contains("/station") -> jsonResponse(
                    req, 200,
                    """{"ResultSet":{"Point":{"Station":{"code":"22828","Name":"東京"}}}}"""
                )
                url.contains("/search/course/extreme") -> jsonResponse(
                    req, 200,
                    """{
                      "ResultSet": {
                        "Course": {
                          "Price": {"kind":"FareSummary","Oneway":"14170"},
                          "Route": {
                            "timeOnBoard": 135,
                            "transferCount": 1,
                            "Line": [
                              {
                                "Name": "JR山手線",
                                "Type": "train",
                                "tripCode": "TRIP-JY-100",
                                "timeOnBoard": 12,
                                "DepartureState": {"Datetime":{"text":"2026-04-10T09:00:00+09:00"}},
                                "ArrivalState": {"Datetime":{"text":"2026-04-10T09:12:00+09:00"}}
                              },
                              {
                                "Name": "東海道新幹線 のぞみ",
                                "Type": "train.shinkansen",
                                "timeOnBoard": 123,
                                "DepartureState": {"Datetime":{"text":"2026-04-10T09:20:00+09:00"}},
                                "ArrivalState": {"Datetime":{"text":"2026-04-10T11:23:00+09:00"}}
                              }
                            ],
                            "Point": [
                              {"Station":{"Name":"渋谷"}},
                              {"Station":{"Name":"品川"}},
                              {"Station":{"Name":"京都"}}
                            ]
                          }
                        }
                      }
                    }"""
                )
                url.contains("/operationLine/timetable") -> jsonResponse(
                    req, 200,
                    """{"ResultSet":{"TimeTable":{"code":"tt_1","Station":{"Name":"京都"},"Line":{"Name":"東海道新幹線"},"HourTable":{"MinuteTable":{"Minute":"10","Extra":"25"}}}}}"""
                )
                else -> jsonResponse(req, 200, "{}")
            }
        }

        val odptClient = mockClient { req ->
            if (req.url.toString().contains("odpt:TrainInformation")) {
                jsonResponse(
                    req, 200,
                    """[{
                      "dc:date":"2026-04-10T08:55:00+09:00",
                      "odpt:operator":"odpt.Operator:JR-East",
                      "odpt:railway":"odpt.Railway:JR-East.Yamanote",
                      "odpt:trainInformationStatus":{"ja":"遅延"},
                      "odpt:trainInformationText":{"ja":"山手線は安全確認のため一部列車に遅れが出ています。"}
                    }]"""
                )
            } else {
                jsonResponse(req, 200, "[]")
            }
        }

        val mixwayClient = mockClient { req ->
            jsonResponse(
                req, 200,
                """{"ResultSet":{"Realtime":{"Trip":{"code":"TRIP-JY-100","delay":15,"status":"遅延","currentStation":"渋谷","nextStation":"恵比寿"}}}}"""
            )
        }

        val ekiProvider = EkispertProvider(
            ekispertApi = NetworkModule.createEkispertApi(client = ekiClient),
            apiKeyProvider = { "eki-key" },
            isConfiguredCheck = { true }
        )
        val odptProvider = OdptRealtimeProvider(
            odptApi = NetworkModule.createOdptApi(client = odptClient),
            apiKeyProvider = { "odpt-key" },
            isConfiguredCheck = { true }
        )
        val mixwayProvider = MixwayRealtimeProvider(
            mixwayApi = NetworkModule.createMixwayApi(client = mixwayClient),
            apiKeyProvider = { "mixway-key" },
            isConfiguredCheck = { true }
        )

        // Verify mixway realtime trip
        val tripRes = mixwayProvider.getRealtimeTrip("TRIP-JY-100")
        assertTrue(tripRes is ProviderResult.Success)
        assertEquals(15, (tripRes as ProviderResult.Success<Train?>).data?.delayMinutes)

        // Verify Ekispert station timetable
        val ttRes = ekiProvider.getStationTimetable("京都")
        assertTrue(ttRes is ProviderResult.Success)
        assertTrue((ttRes as ProviderResult.Success).data.isNotEmpty())

        // Verify aggregator route search + delay enrichment + detour notice
        val repo = TransitRepository(
            odptApi = NetworkModule.createOdptApi(client = odptClient),
            odptProvider = odptProvider,
            ekispertProvider = ekiProvider,
            mixwayProvider = mixwayProvider
        )
        val bundle = repo.searchNationwideRoutes(
            request = RouteSearchRequest(
                origin = "渋谷",
                destination = "京都",
                dateYyyyMmDd = "20260410",
                timeHhMm = "0900",
                timeMode = TimeQueryMode.DEPART_AT
            ),
            baseMinutesOfDay = 9 * 60,
            odptState = OdptApiState.NotConfigured
        )
        assertEquals(1, bundle.routeResults.size)
        val r = bundle.routeResults.first()
        assertEquals(14170, r.fare)
        assertEquals(1, r.transferCount)
        assertTrue(r.alternativeSuggested)
        assertTrue(r.tags.contains("迂回候補推奨"))
        assertTrue(r.legs.first().delayMinutes >= 10)
        assertEquals(false, r.legs.first().transferFeasible)
    }

    @Test
    fun unsupportedOperatorsAndContractProviders_reportAccurateStatusesWithoutFakingData() = runTest {
        val official = OperatorOfficialProvider()
        val jrcPositions = official.getTrainPositions("jrc.TokaidoShinkansen")
        assertTrue(jrcPositions is ProviderResult.Unsupported)
        assertTrue((jrcPositions as ProviderResult.Unsupported).reason.contains("JR東海"))

        val jorudan = JorudanProvider(isConfiguredCheck = { false })
        assertTrue(jorudan.searchRoutes(RouteSearchRequest("東京", "大阪", "20260410", "0900")) is ProviderResult.NotConfigured)

        val navitime = NavitimeProvider(isConfiguredCheck = { false })
        assertTrue(navitime.searchRoutes(RouteSearchRequest("東京", "大阪", "20260410", "0900")) is ProviderResult.NotConfigured)

        // Verify registry includes all 55 operators and bearing calculation is valid
        assertEquals(55, NationalRailwayRegistry.operators.size)
        assertTrue(NationalRailwayRegistry.operators.count { ProviderCapability.TRAIN_POSITIONS in it.capabilities } >= 10)
        val bearing = GeoTrackMath.bearingDegrees(35.6580, 139.7016, 35.7107, 139.7977)
        assertTrue(bearing in 0f..360f)
    }
}
