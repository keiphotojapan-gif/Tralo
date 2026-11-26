package com.example.data.remote

import com.example.model.Train
import com.example.model.TrainPosition
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.http.GET
import retrofit2.http.Query

interface MixwayApi {
    @GET("v1/json/realtime/trip")
    suspend fun getRealtimeTrip(
        @Query("key") key: String,
        @Query("tripCode") tripCode: String
    ): ResponseBody
}

object MixwayJsonParser {
    private fun JSONObject?.asObjectList(key: String): List<JSONObject> {
        if (this == null || !has(key) || isNull(key)) return emptyList()
        return when (val raw = opt(key)) {
            is JSONArray -> buildList {
                for (i in 0 until raw.length()) {
                    raw.optJSONObject(i)?.let { add(it) }
                }
            }
            is JSONObject -> listOf(raw)
            else -> emptyList()
        }
    }

    fun parseRealtimeTrip(
        json: String,
        tripCode: String,
        nowLabel: String
    ): Train? {
        val root = JSONObject(json)
        val resultSet = root.optJSONObject("ResultSet") ?: return null
        val realtimeObj = resultSet.optJSONObject("Realtime")
        val corpObj = resultSet.asObjectList("Corporation").firstOrNull()
            ?: realtimeObj.asObjectList("Corporation").firstOrNull()
        val tripObj = resultSet.asObjectList("Trip").firstOrNull()
            ?: resultSet.asObjectList("RealtimeTrip").firstOrNull()
            ?: realtimeObj.asObjectList("Trip").firstOrNull()
            ?: realtimeObj.asObjectList("RealtimeTrip").firstOrNull()
            ?: return null

        val operatorName = corpObj?.optString("Name").orEmpty().ifBlank { "mixway連携事業者" }
        val lineName = tripObj.optString("LineName").ifBlank {
            tripObj.optString("lineName").ifBlank {
                tripObj.optJSONObject("Line")?.optString("Name").orEmpty().ifBlank { "連携路線" }
            }
        }
        val delayMin = when {
            tripObj.has("delayMinutes") -> tripObj.optInt("delayMinutes", 0)
            tripObj.has("Delay") -> tripObj.optInt("Delay", 0)
            tripObj.has("delay") -> tripObj.optInt("delay", 0)
            else -> 0
        }
        val currentStation = tripObj.optString("CurrentStation")
            .ifBlank { tripObj.optString("currentStation") }
            .ifBlank { "走行中" }
        val nextStation = tripObj.optString("NextStation")
            .ifBlank { tripObj.optString("nextStation") }
        val destination = tripObj.optString("Destination")
            .ifBlank { tripObj.optString("destination") }
        val lat = tripObj.optDouble("Latitude", tripObj.optDouble("latitude", Double.NaN))
        val lng = tripObj.optDouble("Longitude", tripObj.optDouble("longitude", Double.NaN))
        val position = if (!lat.isNaN() && !lng.isNaN()) {
            TrainPosition(
                trainId = tripCode,
                latitude = lat,
                longitude = lng,
                bearing = tripObj.optDouble("Bearing", tripObj.optDouble("bearing", 0.0)).toFloat(),
                currentStation = currentStation,
                nextStation = nextStation,
                progress = 0.5f,
                delayMinutes = delayMin,
                timestamp = nowLabel,
                dataSource = "mixway API ($operatorName)",
                confidence = 0.9f
            )
        } else null

        val status = tripObj.optString("Status")
            .ifBlank { tripObj.optString("status") }
            .ifBlank { if (delayMin > 0) "${delayMin}分遅れ" else "平常運転" }

        return Train(
            trainId = "mixway_$tripCode",
            tripId = tripCode,
            operatorId = operatorName,
            lineId = lineName,
            trainNumber = tripObj.optString("TrainNumber").ifBlank { tripObj.optString("code").ifBlank { tripCode } },
            trainType = tripObj.optString("Kind").ifBlank { tripObj.optString("kind").ifBlank { "列車" } },
            destination = destination,
            direction = tripObj.optString("Direction").ifBlank { "運行中" },
            currentStation = currentStation,
            nextStation = nextStation,
            actualPosition = position,
            delayMinutes = delayMin,
            realtimeStatus = status,
            lastUpdated = nowLabel
        )
    }
}
