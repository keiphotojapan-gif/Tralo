package com.example

import com.example.data.provider.JrShikokuRealtimeAdapter
import com.example.data.provider.JrTrainformationAdapter
import com.example.data.remote.ApiCacheAndRateLimiter
import com.example.data.remote.NationalRailwayRegistry
import com.example.data.remote.NetworkModule
import com.example.data.repository.TransitRepository
import com.example.model.ProviderResult
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JrAdaptersTest {

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
    fun jrTrainformationAdapter_connectsDirectlyToJrWestPublicApiWhenProxyUnconfigured() = runTest {
        val client = mockClient { req ->
            val url = req.url.toString()
            if (url.contains("train-guide.westjr.co.jp/api/v3/kyotokobe.json")) {
                jsonResponse(
                    req, 200,
                    """{
                      "update":"10:25",
                      "trains":[
                        {
                          "no":"3456M",
                          "displayType":"新快速",
                          "dest":{"text":"京都"},
                          "pos":"JR-A46_JR-A38",
                          "direction":0,
                          "delayMinutes":4
                        }
                      ]
                    }"""
                )
            } else {
                jsonResponse(req, 404, """{"error":"not found"}""")
            }
        }

        val adapter = JrTrainformationAdapter(
            apiFactory = { baseUrl -> NetworkModule.createJrTrainformationApi(baseUrl, client) },
            proxyUrlProvider = { "" },
            isProxyConfiguredCheck = { false },
            timeFormatter = { "10:25" }
        )

        val jrwRes = adapter.getTrainPositions("jrw.KyotoLine")
        assertTrue(jrwRes is ProviderResult.Success)
        val train = (jrwRes as ProviderResult.Success).data.single()
        assertEquals("3456M", train.trainNumber)
        assertEquals("新快速", train.trainType)
        assertEquals("京都", train.destination)
        assertEquals(4, train.delayMinutes)
        val pos = train.actualPosition!!
        assertTrue(pos.latitude in 34.73..34.86)
        assertTrue(pos.bearing in 0f..360f)
    }

    @Test
    fun jrShikokuAdapter_unconfiguredReturnsEstimatedTrains() = runTest {
        var networkCalled = false
        val trapClient = mockClient { _ ->
            networkCalled = true
            throw AssertionError("Network must not be called when unconfigured")
        }

        val jrShikoku = JrShikokuRealtimeAdapter(
            apiFactory = { NetworkModule.createJrShikokuRealtimeApi("https://authorized.example/", trapClient) },
            isAuthorizedEndpointConfigured = { false }
        )

        val jrsRes = jrShikoku.getTrainPositions("jrs.YosanLine")
        assertTrue(!networkCalled)
        assertTrue(jrsRes is ProviderResult.Success)
        val trains = (jrsRes as ProviderResult.Success).data
        assertTrue(trains.isNotEmpty())
        assertEquals("jrs.YosanLine", trains.first().lineId)
    }

    @Test
    fun transitRepository_routesJrLinesCorrectlyAndExposesOfficialUrls() = runTest {
        val repo = TransitRepository()
        val shikokuOp = NationalRailwayRegistry.findOperator("op_jr_shikoku")
        assertEquals("https://www.jr-shikoku.co.jp/train_info/", shikokuOp?.officialInfoUrl)
        assertTrue(NationalRailwayRegistry.stationsForLine("jrs.YosanLine").size >= 5)
        assertTrue(NationalRailwayRegistry.stationsForLine("jrh.HakodateChitose").size >= 5)
    }
}
