package com.example.data.remote

import com.example.data.provider.GeoTrackMath
import com.example.model.RailwayStation
import com.example.model.StationTimetable
import com.example.model.Train
import com.example.model.TrainOperationStatus
import com.example.model.TrainPosition
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit contract for JR West public train-guide JSON endpoints
 * (https://www.train-guide.westjr.co.jp/api/v3/{lineCode}.json).
 * Operates without API keys and connects directly when proxy URL is unconfigured.
 */
interface JrTrainformationApi {
    @GET("api/v3/{lineCode}.json")
    suspend fun getLineTrains(
        @Path("lineCode") lineCode: String
    ): ResponseBody
}

object JrTrainformationJsonParser {

    fun parseLineTrains(
        rawJson: String,
        operatorId: String,
        lineId: String,
        sourceName: String,
        fallbackTime: String
    ): List<Train> {
        val root = JSONObject(rawJson)
        val updatedAt = root.optString("update").ifBlank {
            root.optString("lastUpdated").ifBlank { fallbackTime }
        }
        val trainsArray = root.optJSONArray("trains") ?: JSONArray()
        val lineStations = NationalRailwayRegistry.stationsForLine(lineId)

        val parsed = mutableListOf<Train>()
        for (i in 0 until trainsArray.length()) {
            val obj = trainsArray.optJSONObject(i) ?: continue
            val trainNumber = obj.optString("no").ifBlank {
                obj.optString("trainNumber").ifBlank { "---" }
            }
            val trainType = obj.optString("displayType").ifBlank {
                obj.optString("trainType").ifBlank { "普通" }
            }
            val destObj = obj.optJSONObject("dest")
            val destination = destObj?.optString("text")?.takeIf { it.isNotBlank() }
                ?: obj.optString("destination").ifBlank {
                    obj.optString("dest").takeIf { it.isNotBlank() && !it.startsWith("{") }.orEmpty()
                }
            val directionRaw = obj.optString("direction")
            val direction = when (directionRaw) {
                "0" -> "上り"
                "1" -> "下り"
                "" -> destination.ifBlank { "方面" }
                else -> directionRaw
            }
            val delayMinutes = obj.optInt("delayMinutes", obj.optInt("delay", 0)).coerceAtLeast(0)
            val posToken = obj.optString("pos").trim()
            val explicitCurrent = obj.optString("currentStation").trim()
            val explicitNext = obj.optString("nextStation").trim()

            val (fromToken, toToken) = resolvePositionTokens(posToken, explicitCurrent, explicitNext)
            val fromStation = resolveStation(fromToken, lineStations)
            val toStation = resolveStation(toToken, lineStations)

            val fromName = fromStation?.stationName ?: fromToken.ifBlank { "走行中" }
            val toName = toStation?.stationName ?: toToken

            val progress = when {
                fromStation != null && toStation != null -> 0.5f
                fromStation != null -> 0f
                else -> 0f
            }
            val coords = GeoTrackMath.interpolatePosition(fromStation, toStation, progress)
            val bearing = if (fromStation != null && toStation != null) {
                GeoTrackMath.bearingDegrees(
                    fromStation.latitude, fromStation.longitude,
                    toStation.latitude, toStation.longitude
                )
            } else 0f

            val trainId = "${lineId}_$trainNumber"
            val actualPosition = coords?.let { (lat, lng) ->
                TrainPosition(
                    trainId = trainId,
                    latitude = lat,
                    longitude = lng,
                    bearing = bearing,
                    currentStation = fromName,
                    nextStation = toName,
                    progress = progress,
                    delayMinutes = delayMinutes,
                    timestamp = updatedAt,
                    dataSource = sourceName,
                    confidence = if (fromStation != null && toStation != null) 0.85f else 0.6f
                )
            }

            parsed += Train(
                trainId = trainId,
                tripId = obj.optString("tripId").takeIf { it.isNotBlank() },
                operatorId = operatorId,
                lineId = lineId,
                trainNumber = trainNumber,
                trainType = trainType,
                destination = destination,
                direction = direction,
                currentStation = fromName,
                nextStation = toName,
                scheduledDeparture = obj.optString("scheduledDeparture").takeIf { it.isNotBlank() },
                scheduledArrival = obj.optString("scheduledArrival").takeIf { it.isNotBlank() },
                actualPosition = actualPosition,
                delayMinutes = delayMinutes,
                realtimeStatus = if (delayMinutes > 0) "${delayMinutes}分遅れ" else "平常",
                lastUpdated = updatedAt
            )
        }
        return parsed
    }

    private fun resolvePositionTokens(
        posToken: String,
        explicitCurrent: String,
        explicitNext: String
    ): Pair<String, String> {
        if (explicitCurrent.isNotBlank()) {
            return explicitCurrent to explicitNext
        }
        if (posToken.contains("_")) {
            val parts = posToken.split("_", limit = 2)
            val a = parts.getOrNull(0).orEmpty().trim()
            val b = parts.getOrNull(1).orEmpty().trim()
            return if (b == "null" || b == "####") a to "" else a to b
        }
        return posToken to ""
    }

    private fun resolveStation(token: String, lineStations: List<RailwayStation>): RailwayStation? {
        if (token.isBlank()) return null
        return lineStations.firstOrNull {
            it.stationCode.equals(token, ignoreCase = true) ||
                it.stationId.equals(token, ignoreCase = true) ||
                it.stationName == token
        } ?: NationalRailwayRegistry.findStationByIdOrName(token)
    }
}
