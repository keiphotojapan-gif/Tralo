package com.example.model

data class HourlyWeatherPoint(
    val timeLabel: String,
    val temperatureC: Double,
    val precipitationProbability: Int,
    val weatherCode: Int
)

data class DailyWeatherPoint(
    val dateLabel: String,
    val maxTempC: Double,
    val minTempC: Double,
    val precipitationProbabilityMax: Int,
    val uvIndexMax: Double,
    val weatherCode: Int
)

data class WeatherForecast(
    val locationName: String,
    val currentTempC: Double,
    val apparentTempC: Double,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val uvIndexMax: Double,
    val precipitationMm: Double,
    val weatherCode: Int,
    val conditionLabelJa: String,
    val advisoryJa: String?,
    val isRainyRisk: Boolean,
    val hourly: List<HourlyWeatherPoint>,
    val daily: List<DailyWeatherPoint>,
    val fetchedAtLabel: String,
    val provenance: String = "Open-Meteo Live"
)

object WmoWeatherInterpreter {
    fun labelFor(code: Int): String = when (code) {
        0 -> "快晴"
        1, 2 -> "晴れ・薄曇り"
        3 -> "くもり"
        45, 48 -> "霧"
        51, 53, 55, 56, 57 -> "霧雨"
        61, 63, 65, 66, 67 -> "雨"
        71, 73, 75, 77 -> "雪"
        80, 81, 82 -> "にわか雨"
        85, 86 -> "雪・ふぶき"
        95, 96, 99 -> "雷雨"
        else -> "天候変化"
    }

    fun isRainOrStorm(code: Int): Boolean = code in 51..99
}
