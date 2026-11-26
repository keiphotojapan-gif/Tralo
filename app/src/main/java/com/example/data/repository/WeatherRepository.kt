package com.example.data.repository

import com.example.data.remote.NetworkModule
import com.example.data.remote.OpenMeteoApi
import com.example.model.DailyWeatherPoint
import com.example.model.HourlyWeatherPoint
import com.example.model.WeatherForecast
import com.example.model.WmoWeatherInterpreter
import kotlinx.coroutines.CancellationException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherRepository(
    private val api: OpenMeteoApi = NetworkModule.openMeteoApi
) {
    private data class CacheEntry(val forecast: WeatherForecast, val timestampMs: Long)

    private val cache = mutableMapOf<String, CacheEntry>()

    suspend fun getWeather(
        locationName: String,
        latitude: Double,
        longitude: Double,
        forceRefresh: Boolean = false
    ): Result<WeatherForecast> {
        val key = "%.2f,%.2f".format(Locale.US, latitude, longitude)
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            val cached = cache[key]
            if (cached != null && now - cached.timestampMs < 10 * 60 * 1000L) {
                return Result.success(cached.forecast.copy(locationName = locationName))
            }
        }

        return try {
            val res = api.getForecast(latitude = latitude, longitude = longitude)
            val current = res.current
            val code = current?.weatherCode ?: 0
            val hourlyTimes = res.hourly?.time.orEmpty()
            val hourlyTemps = res.hourly?.temperature2m.orEmpty()
            val hourlyPops = res.hourly?.precipitationProbability.orEmpty()
            val hourlyCodes = res.hourly?.weatherCode.orEmpty()

            val hourlyPoints = hourlyTimes.take(12).mapIndexed { idx, isoTime ->
                val hhmm = isoTime.substringAfter("T", isoTime).take(5)
                HourlyWeatherPoint(
                    timeLabel = hhmm,
                    temperatureC = hourlyTemps.getOrNull(idx) ?: current?.temperature2m ?: 16.0,
                    precipitationProbability = hourlyPops.getOrNull(idx) ?: 0,
                    weatherCode = hourlyCodes.getOrNull(idx) ?: code
                )
            }

            val dailyTimes = res.daily?.time.orEmpty()
            val dailyMax = res.daily?.temperature2mMax.orEmpty()
            val dailyMin = res.daily?.temperature2mMin.orEmpty()
            val dailyPop = res.daily?.precipitationProbabilityMax.orEmpty()
            val dailyUv = res.daily?.uvIndexMax.orEmpty()
            val dailyCode = res.daily?.weatherCode.orEmpty()

            val dailyPoints = dailyTimes.mapIndexed { idx, dateStr ->
                DailyWeatherPoint(
                    dateLabel = dateStr.drop(5),
                    maxTempC = dailyMax.getOrNull(idx) ?: 18.0,
                    minTempC = dailyMin.getOrNull(idx) ?: 9.0,
                    precipitationProbabilityMax = dailyPop.getOrNull(idx) ?: 0,
                    uvIndexMax = dailyUv.getOrNull(idx) ?: 2.0,
                    weatherCode = dailyCode.getOrNull(idx) ?: code
                )
            }

            val maxPopToday = dailyPoints.firstOrNull()?.precipitationProbabilityMax
                ?: hourlyPoints.maxOfOrNull { it.precipitationProbability } ?: 0
            val rainyRisk = WmoWeatherInterpreter.isRainOrStorm(code) || maxPopToday >= 50
            val wind = current?.windSpeed10m ?: 0.0
            val advisory = when {
                code in 95..99 -> "雷雨注意：屋外移動は控え、地下連絡通路や屋内施設への変更を推奨します。"
                wind >= 40.0 -> "強風注意：新幹線・在来線・ロープウェイや船舶の運行状況をご確認ください。"
                rainyRisk -> "降水予報（${maxPopToday}%）：雨天対応の屋内観光プランへの切替が可能です。"
                else -> null
            }

            val fetchedLabel = SimpleDateFormat("HH:mm", Locale.JAPAN).format(Date())
            val forecast = WeatherForecast(
                locationName = locationName,
                currentTempC = current?.temperature2m ?: 15.0,
                apparentTempC = current?.apparentTemperature ?: current?.temperature2m ?: 15.0,
                humidityPercent = current?.relativeHumidity2m ?: 55,
                windSpeedKmh = wind,
                uvIndexMax = dailyPoints.firstOrNull()?.uvIndexMax ?: 2.0,
                precipitationMm = current?.precipitation ?: 0.0,
                weatherCode = code,
                conditionLabelJa = WmoWeatherInterpreter.labelFor(code),
                advisoryJa = advisory,
                isRainyRisk = rainyRisk,
                hourly = hourlyPoints,
                daily = dailyPoints,
                fetchedAtLabel = fetchedLabel
            )
            cache[key] = CacheEntry(forecast, now)
            Result.success(forecast)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
