package com.example

import com.example.core.TraloLanguage
import com.example.core.TraloLocalization
import com.example.data.remote.CuratedCountriesData
import com.example.data.remote.RestCountriesApi
import com.example.data.remote.RestCountriesJsonParser
import com.example.data.repository.CountriesRepository
import com.example.model.AppSettings
import com.example.model.DateFormatOption
import com.example.model.DistanceUnit
import com.example.model.TemperatureUnit
import com.example.model.TimeFormatOption
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsAndCountriesTest {

    private val sampleJson = """
    [
      {
        "name": {
          "common": "Japan",
          "official": "Japan"
        },
        "cca2": "JP",
        "cca3": "JPN",
        "currencies": {
          "JPY": {
            "name": "Japanese yen",
            "symbol": "¥"
          }
        },
        "capital": ["Tokyo"],
        "region": "Asia",
        "subregion": "Eastern Asia",
        "languages": {
          "jpn": "Japanese"
        },
        "latlng": [36.0, 138.0],
        "timezones": ["UTC+09:00"],
        "idd": {
          "root": "+8",
          "suffixes": ["1"]
        },
        "translations": {
          "jpn": { "common": "日本" },
          "fra": { "common": "Japon" },
          "kor": { "common": "일본" }
        }
      }
    ]
    """.trimIndent()

    @Test
    fun restCountriesJsonParser_parsesCountryAttributesCorrectly() {
        val countries = RestCountriesJsonParser.parseCountries(sampleJson)
        assertEquals(1, countries.size)
        val jp = countries[0]
        assertEquals("JP", jp.cca2)
        assertEquals("JPN", jp.cca3)
        assertEquals("Japan", jp.commonName)
        assertEquals("Tokyo", jp.capital)
        assertEquals("Asia", jp.region)
        assertEquals("🇯🇵", jp.flagEmoji)
        assertEquals(1, jp.currencies.size)
        assertEquals("JPY", jp.currencies[0].code)
        assertEquals("¥", jp.currencies[0].symbol)
        assertEquals(1, jp.timezones.size)
        assertEquals("UTC+09:00", jp.timezones[0])
        assertEquals("日本", jp.getLocalizedName("ja"))
        assertEquals("Japon", jp.getLocalizedName("fr"))
        assertEquals("일본", jp.getLocalizedName("ko"))
        assertEquals("+81", jp.callingCode)
    }

    @Test
    fun countriesRepository_searchesAndExtractsCurrencies() = runTest {
        val fakeApi = object : RestCountriesApi {
            override suspend fun getAllCountries() =
                sampleJson.toResponseBody("application/json".toMediaTypeOrNull())

            override suspend fun getCountryByCode(code: String) =
                sampleJson.toResponseBody("application/json".toMediaTypeOrNull())
        }

        val repo = CountriesRepository(fakeApi, CuratedCountriesData.allCuratedCountries)
        val countries = repo.getCountries()
        assertTrue(countries.size >= 15)

        // Search in Japanese
        val searchJa = repo.searchCountries("フランス", "ja")
        assertTrue(searchJa.any { it.cca2 == "FR" })

        // Search in English
        val searchEn = repo.searchCountries("United States", "en")
        assertTrue(searchEn.any { it.cca2 == "US" })

        // Search by currency code
        val searchCurr = repo.searchCountries("EUR", "ja")
        assertTrue(searchCurr.any { it.cca2 == "FR" || it.cca2 == "DE" })

        val currencies = repo.getAvailableCurrencies()
        assertTrue(currencies.any { it.code == "JPY" })
        assertTrue(currencies.any { it.code == "USD" })
        assertTrue(currencies.any { it.code == "EUR" })
        assertEquals("JPY", currencies.first().code)
    }

    @Test
    fun traloLocalization_providesAll12Languages() {
        assertEquals(12, TraloLanguage.entries.size)

        val expectedCodes = listOf("ja", "en", "zh-CN", "zh-TW", "ko", "fr", "es", "de", "it", "pt", "ar", "hi")
        expectedCodes.forEach { code ->
            val lang = TraloLanguage.fromCode(code)
            assertNotNull(lang)
            val settingsLabel = TraloLocalization.getNavLabel("settings", code)
            assertTrue(settingsLabel.isNotBlank())
        }
    }

    @Test
    fun appSettings_formatsUnitsAndDatesCorrectly() {
        val settings = AppSettings(
            language = "ja",
            countryCode = "JP",
            currencyCode = "JPY",
            currencySymbol = "¥",
            dateFormat = DateFormatOption.YYYY_MM_DD,
            timeFormat = TimeFormatOption.HOUR_24,
            distanceUnit = DistanceUnit.KM,
            temperatureUnit = TemperatureUnit.CELSIUS
        )

        // Currency
        assertEquals("¥50,000", settings.formatCurrency(50000L))

        val usdSettings = settings.copy(currencyCode = "USD", currencySymbol = "$")
        assertEquals("$50,000", usdSettings.formatCurrency(50000L))

        // Temperature
        assertEquals("20.0℃", settings.formatTemperature(20.0))
        val fahrenheitSettings = settings.copy(temperatureUnit = TemperatureUnit.FAHRENHEIT)
        assertEquals("68.0℉", fahrenheitSettings.formatTemperature(20.0))

        // Distance
        assertEquals("10.0 km", settings.formatDistance(10.0))
        val mileSettings = settings.copy(distanceUnit = DistanceUnit.MILE)
        assertEquals("6.2 mi", mileSettings.formatDistance(10.0))

        // Date formats
        assertEquals("2026/04/10", settings.formatDate("20260410"))
        assertEquals("10/04/2026", settings.copy(dateFormat = DateFormatOption.DD_MM_YYYY).formatDate("20260410"))
        assertEquals("04/10/2026", settings.copy(dateFormat = DateFormatOption.MM_DD_YYYY).formatDate("20260410"))

        // Time formats
        assertEquals("14:30", settings.formatTime(14, 30))
        assertEquals("2:30 PM", settings.copy(timeFormat = TimeFormatOption.HOUR_12).formatTime(14, 30))
        assertEquals("9:05 AM", settings.copy(timeFormat = TimeFormatOption.HOUR_12).formatTime(9, 5))
    }

    @Test
    fun timezoneCalculation_handlesOffsets() {
        val localTime = TraloLocalization.calculateDestinationLocalTime("UTC+09:00")
        assertTrue(localTime.contains(":"))

        val diff = TraloLocalization.calculateTimeDifference("UTC+09:00")
        assertTrue(diff.isNotBlank())
    }
}
