package com.example.data.remote

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import retrofit2.http.GET
import retrofit2.http.Query

data class OdptLocalizedTitle(
    val ja: String = "",
    val en: String = ""
) {
    fun bestJa(): String = ja.ifBlank { en }
}

class OdptLocalizedTitleAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): OdptLocalizedTitle? {
        return when (reader.peek()) {
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                null
            }
            JsonReader.Token.STRING -> {
                val value = reader.nextString()
                OdptLocalizedTitle(ja = value, en = value)
            }
            JsonReader.Token.BEGIN_OBJECT -> {
                var ja = ""
                var en = ""
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "ja" -> ja = if (reader.peek() == JsonReader.Token.STRING) reader.nextString() else { reader.skipValue(); "" }
                        "en" -> en = if (reader.peek() == JsonReader.Token.STRING) reader.nextString() else { reader.skipValue(); "" }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
                OdptLocalizedTitle(ja = ja, en = en)
            }
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: OdptLocalizedTitle?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.beginObject()
            writer.name("ja").value(value.ja)
            writer.name("en").value(value.en)
            writer.endObject()
        }
    }
}

@JsonClass(generateAdapter = true)
data class OdptTrainInformationDto(
    @Json(name = "dc:date") val date: String? = null,
    @Json(name = "odpt:operator") val operatorId: String? = null,
    @Json(name = "odpt:railway") val railwayId: String? = null,
    @Json(name = "odpt:trainInformationStatus") val statusMap: Map<String, String>? = null,
    @Json(name = "odpt:trainInformationText") val textMap: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class OdptTrainDto(
    @Json(name = "owl:sameAs") val sameAs: String? = null,
    @Json(name = "dc:date") val date: String? = null,
    @Json(name = "dct:valid") val validUntil: String? = null,
    @Json(name = "odpt:operator") val operatorId: String? = null,
    @Json(name = "odpt:trainNumber") val trainNumber: String? = null,
    @Json(name = "odpt:railway") val railwayId: String? = null,
    @Json(name = "odpt:trainType") val trainType: String? = null,
    @Json(name = "odpt:railDirection") val railDirection: String? = null,
    @Json(name = "odpt:fromStation") val fromStation: String? = null,
    @Json(name = "odpt:toStation") val toStation: String? = null,
    @Json(name = "odpt:originStation") val originStations: List<String>? = null,
    @Json(name = "odpt:destinationStation") val destinationStations: List<String>? = null,
    @Json(name = "odpt:delay") val delaySeconds: Int? = null
)

@JsonClass(generateAdapter = true)
data class OdptStationDto(
    @Json(name = "owl:sameAs") val sameAs: String? = null,
    @Json(name = "dc:title") val title: String? = null,
    @Json(name = "odpt:stationTitle") val stationTitle: OdptLocalizedTitle? = null,
    @Json(name = "odpt:stationCode") val stationCode: String? = null,
    @Json(name = "odpt:operator") val operatorId: String? = null,
    @Json(name = "odpt:railway") val railwayId: String? = null,
    @Json(name = "geo:lat") val latitude: Double? = null,
    @Json(name = "geo:long") val longitude: Double? = null
)

@JsonClass(generateAdapter = true)
data class OdptStationOrderDto(
    @Json(name = "odpt:index") val index: Int? = null,
    @Json(name = "odpt:station") val stationId: String? = null,
    @Json(name = "odpt:stationTitle") val stationTitle: OdptLocalizedTitle? = null
)

@JsonClass(generateAdapter = true)
data class OdptRailwayDto(
    @Json(name = "owl:sameAs") val sameAs: String? = null,
    @Json(name = "dc:title") val title: String? = null,
    @Json(name = "odpt:railwayTitle") val railwayTitle: OdptLocalizedTitle? = null,
    @Json(name = "odpt:operator") val operatorId: String? = null,
    @Json(name = "odpt:lineCode") val lineCode: String? = null,
    @Json(name = "odpt:color") val color: String? = null,
    @Json(name = "odpt:stationOrder") val stationOrder: List<OdptStationOrderDto>? = null
)

@JsonClass(generateAdapter = true)
data class OdptTimetableEntryDto(
    @Json(name = "odpt:departureTime") val departureTime: String? = null,
    @Json(name = "odpt:destinationStation") val destinationStations: List<String>? = null,
    @Json(name = "odpt:trainType") val trainType: String? = null,
    @Json(name = "odpt:trainNumber") val trainNumber: String? = null,
    @Json(name = "odpt:platformNumber") val platformNumber: String? = null
)

@JsonClass(generateAdapter = true)
data class OdptStationTimetableDto(
    @Json(name = "owl:sameAs") val sameAs: String? = null,
    @Json(name = "dc:date") val date: String? = null,
    @Json(name = "odpt:operator") val operatorId: String? = null,
    @Json(name = "odpt:railway") val railwayId: String? = null,
    @Json(name = "odpt:station") val stationId: String? = null,
    @Json(name = "odpt:stationTitle") val stationTitle: OdptLocalizedTitle? = null,
    @Json(name = "odpt:railDirection") val railDirection: String? = null,
    @Json(name = "odpt:calendar") val calendar: String? = null,
    @Json(name = "odpt:weekdays") val weekdays: List<OdptTimetableEntryDto>? = null,
    @Json(name = "odpt:saturdays") val saturdays: List<OdptTimetableEntryDto>? = null,
    @Json(name = "odpt:holidays") val holidays: List<OdptTimetableEntryDto>? = null,
    @Json(name = "odpt:stationTimetableObject") val timetableObjects: List<OdptTimetableEntryDto>? = null
)

interface OdptApi {
    @GET("./odpt:TrainInformation")
    suspend fun getTrainInformation(
        @Query("acl:consumerKey") consumerKey: String,
        @Query("odpt:operator") operatorId: String? = null
    ): List<OdptTrainInformationDto>

    @GET("./odpt:Train")
    suspend fun getTrains(
        @Query("acl:consumerKey") consumerKey: String,
        @Query("odpt:railway") railwayId: String? = null
    ): List<OdptTrainDto>

    @GET("./odpt:Station")
    suspend fun getStations(
        @Query("acl:consumerKey") consumerKey: String,
        @Query("odpt:railway") railwayId: String? = null
    ): List<OdptStationDto>

    @GET("./odpt:Railway")
    suspend fun getRailways(
        @Query("acl:consumerKey") consumerKey: String,
        @Query("odpt:operator") operatorId: String? = null
    ): List<OdptRailwayDto>

    @GET("./odpt:StationTimetable")
    suspend fun getStationTimetables(
        @Query("acl:consumerKey") consumerKey: String,
        @Query("odpt:railway") railwayId: String? = null,
        @Query("odpt:station") stationId: String? = null
    ): List<OdptStationTimetableDto>
}
