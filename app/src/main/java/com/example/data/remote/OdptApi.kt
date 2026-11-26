package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

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
    @Json(name = "dc:date") val date: String? = null,
    @Json(name = "odpt:trainNumber") val trainNumber: String? = null,
    @Json(name = "odpt:railway") val railwayId: String? = null,
    @Json(name = "odpt:trainType") val trainType: String? = null,
    @Json(name = "odpt:railDirection") val railDirection: String? = null,
    @Json(name = "odpt:fromStation") val fromStation: String? = null,
    @Json(name = "odpt:toStation") val toStation: String? = null,
    @Json(name = "odpt:destinationStation") val destinationStations: List<String>? = null,
    @Json(name = "odpt:delay") val delaySeconds: Int? = null
)

interface OdptApi {
    @GET("odpt:TrainInformation")
    suspend fun getTrainInformation(
        @Query("acl:consumerKey") consumerKey: String,
        @Query("odpt:operator") operatorId: String? = null
    ): List<OdptTrainInformationDto>

    @GET("odpt:Train")
    suspend fun getTrains(
        @Query("acl:consumerKey") consumerKey: String,
        @Query("odpt:railway") railwayId: String? = null
    ): List<OdptTrainDto>
}
