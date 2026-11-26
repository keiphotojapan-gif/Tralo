package com.example.model

import androidx.annotation.Keep

@Keep
data class CountryCurrency(
    val code: String,
    val name: String,
    val symbol: String
)

@Keep
data class CountryInfo(
    val cca2: String,
    val cca3: String,
    val commonName: String,
    val officialName: String,
    val capital: String? = null,
    val region: String = "",
    val subregion: String? = null,
    val flagEmoji: String = "",
    val flagPngUrl: String? = null,
    val currencies: List<CountryCurrency> = emptyList(),
    val languages: Map<String, String> = emptyMap(),
    val timezones: List<String> = emptyList(),
    val latLng: Pair<Double, Double> = 0.0 to 0.0,
    val translations: Map<String, String> = emptyMap(),
    val iddRoot: String? = null,
    val iddSuffixes: List<String> = emptyList()
) {
    fun getLocalizedName(languageCode: String): String {
        val key = when (languageCode.lowercase()) {
            "ja" -> "jpn"
            "en" -> "eng"
            "zh-cn", "zh" -> "zho"
            "zh-tw" -> "zho"
            "ko" -> "kor"
            "fr" -> "fra"
            "es" -> "spa"
            "de" -> "deu"
            "it" -> "ita"
            "pt" -> "por"
            "ar" -> "ara"
            "hi" -> "hin"
            else -> languageCode.lowercase()
        }
        return translations[key]
            ?: translations["jpn"]
            ?: translations["eng"]
            ?: commonName
    }

    val primaryCurrency: CountryCurrency?
        get() = currencies.firstOrNull()

    val callingCode: String?
        get() {
            val root = iddRoot ?: return null
            val suffix = iddSuffixes.firstOrNull().orEmpty()
            return "$root$suffix".trim().takeIf { it.isNotBlank() }
        }

    val primaryTimezone: String?
        get() = timezones.firstOrNull()
}

enum class DateFormatOption(
    val id: String,
    val labelJa: String,
    val labelEn: String,
    val patternExample: String
) {
    YYYY_MM_DD("YYYY_MM_DD", "年/月/日 (YYYY/MM/DD)", "Year/Month/Day", "2026/04/10"),
    DD_MM_YYYY("DD_MM_YYYY", "日/月/年 (DD/MM/YYYY)", "Day/Month/Year", "10/04/2026"),
    MM_DD_YYYY("MM_DD_YYYY", "月/日/年 (MM/DD/YYYY)", "Month/Day/Year", "04/10/2026");

    companion object {
        fun fromId(id: String?): DateFormatOption =
            entries.firstOrNull { it.id == id } ?: YYYY_MM_DD
    }
}

enum class TimeFormatOption(
    val id: String,
    val labelJa: String,
    val labelEn: String,
    val example: String
) {
    HOUR_24("HOUR_24", "24時間表示 (14:30)", "24-hour format", "14:30"),
    HOUR_12("HOUR_12", "12時間表示 (2:30 PM)", "12-hour format", "2:30 PM");

    companion object {
        fun fromId(id: String?): TimeFormatOption =
            entries.firstOrNull { it.id == id } ?: HOUR_24
    }
}

enum class DistanceUnit(
    val id: String,
    val labelJa: String,
    val unitSymbol: String
) {
    KM("KM", "キロメートル (km)", "km"),
    MILE("MILE", "マイル (mile)", "mi");

    companion object {
        fun fromId(id: String?): DistanceUnit =
            entries.firstOrNull { it.id == id } ?: KM
    }
}

enum class TemperatureUnit(
    val id: String,
    val labelJa: String,
    val unitSymbol: String
) {
    CELSIUS("CELSIUS", "摂氏 (℃)", "℃"),
    FAHRENHEIT("FAHRENHEIT", "華氏 (℉)", "℉");

    companion object {
        fun fromId(id: String?): TemperatureUnit =
            entries.firstOrNull { it.id == id } ?: CELSIUS
    }
}

@Keep
data class AppSettings(
    val language: String = "ja",
    val countryCode: String = "JP",
    val currencyCode: String = "JPY",
    val currencySymbol: String = "¥",
    val dateFormat: DateFormatOption = DateFormatOption.YYYY_MM_DD,
    val timeFormat: TimeFormatOption = TimeFormatOption.HOUR_24,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS
) {
    fun formatTemperature(tempC: Double): String = when (temperatureUnit) {
        TemperatureUnit.CELSIUS -> "%.1f℃".format(tempC)
        TemperatureUnit.FAHRENHEIT -> "%.1f℉".format(tempC * 9.0 / 5.0 + 32.0)
    }

    fun formatDistance(km: Double): String = when (distanceUnit) {
        DistanceUnit.KM -> "%.1f km".format(km)
        DistanceUnit.MILE -> "%.1f mi".format(km * 0.621371)
    }

    fun formatCurrency(amount: Long): String {
        val formattedNumber = java.text.NumberFormat.getIntegerInstance().format(amount)
        return when (currencyCode) {
            "JPY" -> "¥$formattedNumber"
            "USD" -> "$$formattedNumber"
            "EUR" -> "€$formattedNumber"
            "GBP" -> "£$formattedNumber"
            "KRW" -> "₩$formattedNumber"
            else -> "$currencySymbol$formattedNumber ($currencyCode)"
        }
    }

    fun formatDate(yyyyMmDd: String): String {
        val clean = yyyyMmDd.replace("-", "").replace("/", "").trim()
        if (clean.length < 8) return yyyyMmDd
        val y = clean.substring(0, 4)
        val m = clean.substring(4, 6)
        val d = clean.substring(6, 8)
        return when (dateFormat) {
            DateFormatOption.YYYY_MM_DD -> "$y/$m/$d"
            DateFormatOption.DD_MM_YYYY -> "$d/$m/$y"
            DateFormatOption.MM_DD_YYYY -> "$m/$d/$y"
        }
    }

    fun formatTime(hour: Int, minute: Int): String = when (timeFormat) {
        TimeFormatOption.HOUR_24 -> "%02d:%02d".format(hour, minute)
        TimeFormatOption.HOUR_12 -> {
            val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            val amPm = if (hour < 12) "AM" else "PM"
            "%d:%02d %s".format(h, minute, amPm)
        }
    }
}
