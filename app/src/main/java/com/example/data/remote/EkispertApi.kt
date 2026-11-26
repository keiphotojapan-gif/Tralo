package com.example.data.remote

import com.example.model.RouteLegDetail
import com.example.model.RouteResult
import com.example.model.StationTimetable
import com.example.model.TransportMode
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.http.GET
import retrofit2.http.Query

interface EkispertApi {
    @GET("v1/json/search/course/extreme")
    suspend fun searchCourseExtreme(
        @Query("key") key: String,
        @Query("viaList") viaList: String,
        @Query("date") date: String? = null,
        @Query("time") time: String? = null,
        @Query("searchType") searchType: String = "departure",
        @Query("answerCount") answerCount: Int = 5
    ): ResponseBody

    @GET("v1/json/station")
    suspend fun searchStations(
        @Query("key") key: String,
        @Query("name") name: String
    ): ResponseBody

    @GET("v1/json/operationLine/timetable")
    suspend fun getOperationLineTimetable(
        @Query("key") key: String,
        @Query("stationName") stationName: String? = null,
        @Query("stationCode") stationCode: String? = null,
        @Query("code") timetableCode: String? = null,
        @Query("date") date: String? = null,
        @Query("addTrainInformation") addTrainInformation: Boolean = true
    ): ResponseBody
}

object EkispertJsonParser {
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

    fun parseStationCodes(json: String): List<Pair<String, String>> {
        val root = JSONObject(json)
        val resultSet = root.optJSONObject("ResultSet") ?: return emptyList()
        return resultSet.asObjectList("Point").mapNotNull { point ->
            val station = point.optJSONObject("Station") ?: return@mapNotNull null
            val code = station.optString("code").trim()
            val name = station.optString("Name").trim()
            if (code.isNotBlank() && name.isNotBlank()) code to name else null
        }
    }

    fun parseCourses(
        json: String,
        fallbackOrigin: String,
        fallbackDestination: String
    ): List<RouteResult> {
        val root = JSONObject(json)
        val resultSet = root.optJSONObject("ResultSet") ?: return emptyList()
        if (resultSet.has("Error")) {
            val errObj = resultSet.optJSONObject("Error")
            val msg = errObj?.optString("Message").orEmpty()
            if (msg.isNotBlank()) throw IllegalStateException(msg)
        }

        val courses = resultSet.asObjectList("Course")
        return courses.mapIndexedNotNull { idx, course ->
            val routeObj = course.optJSONObject("Route") ?: return@mapIndexedNotNull null
            val lines = routeObj.asObjectList("Line")
            val points = routeObj.asObjectList("Point")
            val prices = course.asObjectList("Price")

            val totalFare = prices.firstOrNull {
                it.optString("kind") == "FareSummary" || it.optString("Type") == "FareSummary"
            }?.optString("Oneway")?.toIntOrNull()
                ?: prices.sumOf { it.optString("Oneway").toIntOrNull() ?: 0 }

            val timeOnBoard = routeObj.optString("timeOnBoard").toIntOrNull() ?: 0
            val timeWalk = routeObj.optString("timeWalk").toIntOrNull() ?: 0
            val timeOther = routeObj.optString("timeOther").toIntOrNull() ?: 0
            val totalDuration = (timeOnBoard + timeWalk + timeOther).coerceAtLeast(1)
            val transfers = routeObj.optString("transferCount").toIntOrNull()
                ?: (lines.size - 1).coerceAtLeast(0)

            val legDetails = lines.mapIndexed { lineIdx, line ->
                val lineName = line.optString("Name").ifBlank { "区間移動" }
                val typeObj = line.optJSONObject("Type")
                val modeStr = (typeObj?.optString("text") ?: line.optString("Type")).lowercase()
                val mode = mapMode(modeStr, lineName)
                val depState = line.optJSONObject("DepartureState")
                val arrState = line.optJSONObject("ArrivalState")
                val depDt = depState?.optJSONObject("Datetime")?.optString("text").orEmpty()
                val arrDt = arrState?.optJSONObject("Datetime")?.optString("text").orEmpty()

                val depStationName = points.getOrNull(lineIdx)
                    ?.optJSONObject("Station")?.optString("Name").orEmpty()
                    .ifBlank { if (lineIdx == 0) fallbackOrigin else "乗換駅" }
                val arrStationName = points.getOrNull(lineIdx + 1)
                    ?.optJSONObject("Station")?.optString("Name").orEmpty()
                    .ifBlank { if (lineIdx == lines.lastIndex) fallbackDestination else "乗換駅" }

                val legTime = (line.optString("timeOnBoard").toIntOrNull() ?: 0)
                    .coerceAtLeast(1)

                RouteLegDetail(
                    mode = mode,
                    lineName = lineName,
                    operatorName = line.optJSONObject("Corporation")?.optString("Name").orEmpty()
                        .ifBlank { "鉄道・交通事業者" },
                    tripCode = line.optString("trainID").ifBlank { null },
                    trainType = line.optString("TypicalName").ifBlank { mode.shortLabel },
                    departureStation = depStationName,
                    arrivalStation = arrStationName,
                    transferStation = if (lineIdx < lines.lastIndex) arrStationName else null,
                    departureTime = formatIsoToHm(depDt, "09:00"),
                    arrivalTime = formatIsoToHm(arrDt, "09:30"),
                    durationMinutes = legTime,
                    fareJpy = 0,
                    walkDistanceMeters = if (mode == TransportMode.WALK) legTime * 75 else 0,
                    platform = depState?.optString("No").orEmpty()
                )
            }

            val depTime = legDetails.firstOrNull()?.departureTime ?: "--:--"
            val arrTime = legDetails.lastOrNull()?.arrivalTime ?: "--:--"

            RouteResult(
                id = "ekispert_course_${idx + 1}",
                origin = fallbackOrigin,
                destination = fallbackDestination,
                departureTime = depTime,
                arrivalTime = arrTime,
                durationMinutes = totalDuration,
                fare = totalFare,
                transferCount = transfers,
                walkMeters = timeWalk * 75,
                legs = legDetails,
                realtimeStatus = "駅すぱあと標準ダイヤ",
                dataSources = listOf("駅すぱあと API"),
                isRealtimeEnriched = false,
                tags = listOf("駅すぱあと実検索"),
                reliabilityNote = "駅すぱあと API 経路探索結果（運行遅延・位置はODPT/mixway連携時に自動重畳）"
            )
        }
    }

    fun parseTimetableCodes(json: String): List<String> {
        val root = JSONObject(json)
        val resultSet = root.optJSONObject("ResultSet") ?: return emptyList()
        return resultSet.asObjectList("TimeTable").mapNotNull {
            it.optString("code").trim().takeIf { code -> code.isNotBlank() }
        }
    }

    fun parseTimetableEntries(
        json: String,
        stationId: String,
        stationName: String
    ): List<StationTimetable> {
        val root = JSONObject(json)
        val resultSet = root.optJSONObject("ResultSet") ?: return emptyList()
        val timeTables = resultSet.asObjectList("TimeTable")
        val results = mutableListOf<StationTimetable>()
        for (tt in timeTables) {
            val lineObj = tt.optJSONObject("Line")
            val lineName = lineObj?.optString("Name").orEmpty().ifBlank { "路線" }
            val direction = lineObj?.optString("Direction").orEmpty().ifBlank { "方面" }
            val stName = tt.optJSONObject("Station")?.optString("Name").orEmpty().ifBlank { stationName }
            val hours = tt.asObjectList("HourTable")
            for (hrObj in hours) {
                val hour = hrObj.optString("Hour").padStart(2, '0')
                val minutes = hrObj.asObjectList("MinuteTable")
                for (minObj in minutes) {
                    val minute = minObj.optString("Minute").padStart(2, '0')
                    val kind = minObj.optString("Kind").ifBlank { "各停" }
                    val dest = minObj.optString("Destination").ifBlank { direction }
                    val trainCode = minObj.optString("code").trim()
                    results += StationTimetable(
                        stationId = stationId,
                        stationName = stName,
                        lineId = lineName,
                        direction = direction,
                        departureTime = "$hour:$minute",
                        trainType = kind,
                        destination = dest,
                        trainNumber = trainCode,
                        source = "駅すぱあと API"
                    )
                }
            }
        }
        return results
    }

    private fun mapMode(typeStr: String, lineName: String): TransportMode = when {
        lineName.contains("新幹線") -> TransportMode.RAIL
        typeStr.contains("walk") -> TransportMode.WALK
        typeStr.contains("plane") -> TransportMode.FLIGHT
        typeStr.contains("ship") -> TransportMode.FERRY
        typeStr.contains("bus") && lineName.contains("高速") -> TransportMode.HIGHWAY_BUS
        typeStr.contains("bus") -> TransportMode.LOCAL_BUS
        lineName.contains("メトロ") || lineName.contains("地下鉄") -> TransportMode.SUBWAY
        else -> TransportMode.RAIL
    }

    private fun formatIsoToHm(raw: String, fallback: String): String {
        val afterT = raw.substringAfter("T", "")
        return if (afterT.length >= 5) afterT.take(5) else fallback
    }
}
