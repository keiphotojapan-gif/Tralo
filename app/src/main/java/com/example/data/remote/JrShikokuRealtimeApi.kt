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
import retrofit2.http.Query

/**
 * Independent Retrofit interface for an authorized JR Shikoku gateway endpoint.
 * Strictly avoids direct unauthorized access to train.jr-shikoku.co.jp or personal APIs.
 */
interface JrShikokuRealtimeApi {
    @GET("v1/jr-shikoku/lines/{lineCode}/trains.json")
    suspend fun getLineTrains(
        @Path("lineCode") lineCode: String
    ): ResponseBody

    @GET("v1/jr-shikoku/status.json")
    suspend fun getOperationStatus(): ResponseBody

    @GET("v1/jr-shikoku/stations/{stationQuery}/timetable.json")
    suspend fun getStationTimetable(
        @Path("stationQuery") stationQuery: String,
        @Query("line") lineCode: String? = null
    ): ResponseBody
}

object JrShikokuJsonParser {

    fun parseTrains(
        rawJson: String,
        lineId: String,
        sourceName: String,
        fallbackTime: String
    ): List<Train> {
        val root = JSONObject(rawJson)
        val updatedAt = root.optString("lastUpdated").ifBlank {
            root.optString("updatedAt").ifBlank { fallbackTime }
        }
        val arr = root.optJSONArray("trains") ?: JSONArray()
        val lineStations = NationalRailwayRegistry.stationsForLine(lineId)

        val result = mutableListOf<Train>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val trainNumber = obj.optString("trainNumber").ifBlank {
                obj.optString("number").ifBlank { "---" }
            }
            val trainType = obj.optString("trainType").ifBlank { "普通" }
            val destination = obj.optString("destination").ifBlank { "方面" }
            val direction = obj.optString("direction").ifBlank { destination }
            val currentToken = obj.optString("currentStation").trim()
            val nextToken = obj.optString("nextStation").trim()
            val delayMinutes = obj.optInt("delayMinutes", 0).coerceAtLeast(0)
            val opStatusText = obj.optString("operationStatus").ifBlank {
                if (delayMinutes > 0) "${delayMinutes}分遅れ" else "平常"
            }
            val trainUpdated = obj.optString("lastUpdated").ifBlank { updatedAt }

            val currentSt = resolveStation(currentToken, lineStations)
            val nextSt = resolveStation(nextToken, lineStations)
            val currentName = currentSt?.stationName ?: currentToken.ifBlank { "走行中" }
            val nextName = nextSt?.stationName ?: nextToken

            val explicitLat = obj.optDouble("latitude", Double.NaN)
            val explicitLng = obj.optDouble("longitude", Double.NaN)
            val progress = obj.optDouble("progress", if (currentSt != null && nextSt != null) 0.5 else 0.0)
                .toFloat()
                .coerceIn(0f, 1f)

            val coords: Pair<Double, Double>? = if (!explicitLat.isNaN() && !explicitLng.isNaN()) {
                explicitLat to explicitLng
            } else {
                GeoTrackMath.interpolatePosition(currentSt, nextSt, progress)
            }
            val bearing = if (currentSt != null && nextSt != null) {
                GeoTrackMath.bearingDegrees(
                    currentSt.latitude, currentSt.longitude,
                    nextSt.latitude, nextSt.longitude
                )
            } else 0f

            val trainId = "jrs_${lineId}_$trainNumber"
            val actualPosition = coords?.let { (lat, lng) ->
                TrainPosition(
                    trainId = trainId,
                    latitude = lat,
                    longitude = lng,
                    bearing = bearing,
                    currentStation = currentName,
                    nextStation = nextName,
                    progress = progress,
                    delayMinutes = delayMinutes,
                    timestamp = trainUpdated,
                    dataSource = sourceName,
                    confidence = if (!explicitLat.isNaN() || (currentSt != null && nextSt != null)) 0.9f else 0.65f
                )
            }

            result += Train(
                trainId = trainId,
                tripId = obj.optString("tripId").takeIf { it.isNotBlank() },
                operatorId = "op_jr_shikoku",
                lineId = lineId,
                trainNumber = trainNumber,
                trainType = trainType,
                destination = destination,
                direction = direction,
                currentStation = currentName,
                nextStation = nextName,
                scheduledDeparture = obj.optString("scheduledDeparture").takeIf { it.isNotBlank() },
                scheduledArrival = obj.optString("scheduledArrival").takeIf { it.isNotBlank() },
                actualPosition = actualPosition,
                delayMinutes = delayMinutes,
                realtimeStatus = opStatusText,
                lastUpdated = trainUpdated
            )
        }
        return result
    }

    fun parseOperationStatuses(
        rawJson: String,
        sourceName: String,
        fallbackTime: String
    ): List<TrainOperationStatus> {
        val root = JSONObject(rawJson)
        val updatedAt = root.optString("lastUpdated").ifBlank { fallbackTime }
        val arr = root.optJSONArray("operationStatus")
            ?: root.optJSONArray("statuses")
            ?: JSONArray()
        val list = mutableListOf<TrainOperationStatus>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val lineId = obj.optString("lineId").ifBlank { "jrs.YosanLine" }
            val lineName = obj.optString("lineName").ifBlank {
                NationalRailwayRegistry.findLine(lineId)?.lineName ?: "JR四国線"
            }
            val status = obj.optString("status").ifBlank { "平常運転" }
            val delay = obj.optInt("delayMinutes", 0).coerceAtLeast(0)
            val msg = obj.optString("message").ifBlank { status }
            val isDelayed = obj.optBoolean("isDelayed", delay > 0 || status.contains("遅れ") || status.contains("見合わせ"))
            val disruption = when {
                status.contains("見合わせ") || msg.contains("見合わせ") -> "運転見合わせ"
                status.contains("運休") || msg.contains("運休") -> "一部運休"
                isDelayed -> "遅延"
                else -> "平常"
            }
            list += TrainOperationStatus(
                operatorId = "op_jr_shikoku",
                lineId = lineId,
                lineName = lineName,
                status = status,
                delayMinutes = delay,
                disruptionType = disruption,
                message = msg,
                updatedAt = obj.optString("lastUpdated").ifBlank { updatedAt },
                source = sourceName,
                isDelayed = isDelayed
            )
        }
        return list
    }

    fun parseTimetable(
        rawJson: String,
        stationIdOrName: String,
        lineId: String?,
        sourceName: String
    ): List<StationTimetable> {
        val root = JSONObject(rawJson)
        val arr = root.optJSONArray("timetable") ?: JSONArray()
        val station = NationalRailwayRegistry.findStationByIdOrName(stationIdOrName)
        val stId = root.optString("stationId").ifBlank { station?.stationId ?: stationIdOrName }
        val stName = root.optString("stationName").ifBlank { station?.stationName ?: stationIdOrName }
        val defaultLine = root.optString("lineId").ifBlank { lineId.orEmpty() }

        val result = mutableListOf<StationTimetable>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            result += StationTimetable(
                stationId = stId,
                stationName = stName,
                lineId = obj.optString("lineId").ifBlank { defaultLine },
                direction = obj.optString("direction").ifBlank { "方面" },
                departureTime = obj.optString("departureTime").ifBlank { "--:--" },
                trainType = obj.optString("trainType").ifBlank { "普通" },
                destination = obj.optString("destination").ifBlank { "終着" },
                platform = obj.optString("platform"),
                trainNumber = obj.optString("trainNumber"),
                calendarType = obj.optString("calendarType").ifBlank { "平日" },
                source = sourceName
            )
        }
        return result
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
