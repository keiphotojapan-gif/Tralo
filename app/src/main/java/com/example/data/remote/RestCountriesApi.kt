package com.example.data.remote

import com.example.model.CountryCurrency
import com.example.model.CountryInfo
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.http.GET
import retrofit2.http.Path

interface RestCountriesApi {
    @GET("v3.1/all?fields=name,cca2,cca3,flags,currencies,capital,region,subregion,languages,latlng,timezones,idd,translations")
    suspend fun getAllCountries(): ResponseBody

    @GET("v3.1/alpha/{code}?fields=name,cca2,cca3,flags,currencies,capital,region,subregion,languages,latlng,timezones,idd,translations")
    suspend fun getCountryByCode(@Path("code") code: String): ResponseBody
}

object RestCountriesJsonParser {

    fun parseCountries(rawJson: String): List<CountryInfo> {
        val array = JSONArray(rawJson)
        val list = mutableListOf<CountryInfo>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            parseCountryObject(obj)?.let { list.add(it) }
        }
        return list
    }

    fun parseSingleCountry(rawJson: String): CountryInfo? {
        val trimmed = rawJson.trim()
        return if (trimmed.startsWith("[")) {
            val array = JSONArray(trimmed)
            if (array.length() > 0) parseCountryObject(array.getJSONObject(0)) else null
        } else {
            parseCountryObject(JSONObject(trimmed))
        }
    }

    private fun parseCountryObject(obj: JSONObject): CountryInfo? {
        val cca2 = obj.optString("cca2").uppercase()
        if (cca2.isBlank()) return null
        val cca3 = obj.optString("cca3").uppercase()

        val nameObj = obj.optJSONObject("name")
        val commonName = nameObj?.optString("common").orEmpty().ifBlank { cca2 }
        val officialName = nameObj?.optString("official").orEmpty().ifBlank { commonName }

        val region = obj.optString("region").ifBlank { "World" }
        val subregion = obj.optString("subregion").takeIf { it.isNotBlank() }

        val capitalsArr = obj.optJSONArray("capital")
        val capital = if (capitalsArr != null && capitalsArr.length() > 0) {
            capitalsArr.optString(0)
        } else null

        // Currencies
        val currenciesObj = obj.optJSONObject("currencies")
        val currenciesList = mutableListOf<CountryCurrency>()
        if (currenciesObj != null) {
            val keys = currenciesObj.keys()
            while (keys.hasNext()) {
                val code = keys.next()
                val cObj = currenciesObj.optJSONObject(code)
                if (cObj != null) {
                    val cName = cObj.optString("name", code)
                    val cSymbol = cObj.optString("symbol", code)
                    currenciesList.add(CountryCurrency(code = code, name = cName, symbol = cSymbol))
                }
            }
        }

        // Languages
        val languagesObj = obj.optJSONObject("languages")
        val languagesMap = mutableMapOf<String, String>()
        if (languagesObj != null) {
            val keys = languagesObj.keys()
            while (keys.hasNext()) {
                val code = keys.next()
                languagesMap[code] = languagesObj.optString(code)
            }
        }

        // Timezones
        val timezonesArr = obj.optJSONArray("timezones")
        val timezonesList = mutableListOf<String>()
        if (timezonesArr != null) {
            for (j in 0 until timezonesArr.length()) {
                val tz = timezonesArr.optString(j)
                if (tz.isNotBlank()) timezonesList.add(tz)
            }
        }

        // LatLng
        val latLngArr = obj.optJSONArray("latlng")
        val lat = latLngArr?.optDouble(0, 0.0) ?: 0.0
        val lng = latLngArr?.optDouble(1, 0.0) ?: 0.0

        // Flags
        val flagsObj = obj.optJSONObject("flags")
        val flagPngUrl = flagsObj?.optString("png")
        val flagEmoji = countryCodeToEmoji(cca2)

        // IDD
        val iddObj = obj.optJSONObject("idd")
        val iddRoot = iddObj?.optString("root")
        val suffixesArr = iddObj?.optJSONArray("suffixes")
        val suffixes = mutableListOf<String>()
        if (suffixesArr != null) {
            for (k in 0 until suffixesArr.length()) {
                suffixes.add(suffixesArr.optString(k))
            }
        }

        // Translations
        val translationsObj = obj.optJSONObject("translations")
        val translationsMap = mutableMapOf<String, String>()
        if (translationsObj != null) {
            val keys = translationsObj.keys()
            while (keys.hasNext()) {
                val langKey = keys.next()
                val tObj = translationsObj.optJSONObject(langKey)
                val transCommon = tObj?.optString("common")
                if (!transCommon.isNullOrBlank()) {
                    translationsMap[langKey] = transCommon
                }
            }
        }

        return CountryInfo(
            cca2 = cca2,
            cca3 = cca3,
            commonName = commonName,
            officialName = officialName,
            capital = capital,
            region = region,
            subregion = subregion,
            flagEmoji = flagEmoji,
            flagPngUrl = flagPngUrl,
            currencies = currenciesList,
            languages = languagesMap,
            timezones = timezonesList,
            latLng = lat to lng,
            translations = translationsMap,
            iddRoot = iddRoot,
            iddSuffixes = suffixes
        )
    }

    fun countryCodeToEmoji(countryCode: String): String {
        if (countryCode.length != 2) return "🌐"
        val upper = countryCode.uppercase()
        val firstChar = Character.codePointAt(upper, 0) - 0x41 + 0x1F1E6
        val secondChar = Character.codePointAt(upper, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    }
}

object CuratedCountriesData {

    val allCuratedCountries: List<CountryInfo> = listOf(
        CountryInfo(
            cca2 = "JP",
            cca3 = "JPN",
            commonName = "Japan",
            officialName = "Japan",
            capital = "Tokyo",
            region = "Asia",
            subregion = "Eastern Asia",
            flagEmoji = "🇯🇵",
            currencies = listOf(CountryCurrency("JPY", "Japanese yen", "¥")),
            languages = mapOf("jpn" to "日本語"),
            timezones = listOf("UTC+09:00"),
            latLng = 36.2048 to 138.2529,
            translations = mapOf(
                "jpn" to "日本",
                "eng" to "Japan",
                "zho" to "日本",
                "kor" to "일본",
                "fra" to "Japon",
                "spa" to "Japón",
                "deu" to "Japan",
                "ita" to "Giappone",
                "por" to "Japão",
                "ara" to "اليابان",
                "hin" to "जापान"
            ),
            iddRoot = "+8",
            iddSuffixes = listOf("1")
        ),
        CountryInfo(
            cca2 = "US",
            cca3 = "USA",
            commonName = "United States",
            officialName = "United States of America",
            capital = "Washington, D.C.",
            region = "Americas",
            subregion = "North America",
            flagEmoji = "🇺🇸",
            currencies = listOf(CountryCurrency("USD", "United States dollar", "$")),
            languages = mapOf("eng" to "English"),
            timezones = listOf("UTC-05:00", "UTC-06:00", "UTC-07:00", "UTC-08:00"),
            latLng = 38.0 to -97.0,
            translations = mapOf(
                "jpn" to "アメリカ合衆国",
                "eng" to "United States",
                "zho" to "美国",
                "kor" to "미국",
                "fra" to "États-Unis",
                "spa" to "Estados Unidos",
                "deu" to "Vereinigte Staaten",
                "ita" to "Stati Uniti",
                "por" to "Estados Unidos",
                "ara" to "الولايات المتحدة",
                "hin" to "संयुक्त राज्य अमेरिका"
            ),
            iddRoot = "+1",
            iddSuffixes = listOf("")
        ),
        CountryInfo(
            cca2 = "GB",
            cca3 = "GBR",
            commonName = "United Kingdom",
            officialName = "United Kingdom of Great Britain and Northern Ireland",
            capital = "London",
            region = "Europe",
            subregion = "Northern Europe",
            flagEmoji = "🇬🇧",
            currencies = listOf(CountryCurrency("GBP", "British pound", "£")),
            languages = mapOf("eng" to "English"),
            timezones = listOf("UTC+00:00"),
            latLng = 54.0 to -2.0,
            translations = mapOf(
                "jpn" to "イギリス",
                "eng" to "United Kingdom",
                "zho" to "英国",
                "kor" to "영국",
                "fra" to "Royaume-Uni",
                "spa" to "Reino Unido",
                "deu" to "Vereinigtes Königreich",
                "ita" to "Regno Unito",
                "por" to "Reino Unido",
                "ara" to "المملكة المتحدة",
                "hin" to "यूनाइटेड किंगडम"
            ),
            iddRoot = "+4",
            iddSuffixes = listOf("4")
        ),
        CountryInfo(
            cca2 = "FR",
            cca3 = "FRA",
            commonName = "France",
            officialName = "French Republic",
            capital = "Paris",
            region = "Europe",
            subregion = "Western Europe",
            flagEmoji = "🇫🇷",
            currencies = listOf(CountryCurrency("EUR", "Euro", "€")),
            languages = mapOf("fra" to "Français"),
            timezones = listOf("UTC+01:00"),
            latLng = 46.2276 to 2.2137,
            translations = mapOf(
                "jpn" to "フランス",
                "eng" to "France",
                "zho" to "法国",
                "kor" to "프랑스",
                "fra" to "France",
                "spa" to "Francia",
                "deu" to "Frankreich",
                "ita" to "Francia",
                "por" to "França",
                "ara" to "فرنسا",
                "hin" to "फ़्रांस"
            ),
            iddRoot = "+3",
            iddSuffixes = listOf("3")
        ),
        CountryInfo(
            cca2 = "DE",
            cca3 = "DEU",
            commonName = "Germany",
            officialName = "Federal Republic of Germany",
            capital = "Berlin",
            region = "Europe",
            subregion = "Western Europe",
            flagEmoji = "🇩🇪",
            currencies = listOf(CountryCurrency("EUR", "Euro", "€")),
            languages = mapOf("deu" to "Deutsch"),
            timezones = listOf("UTC+01:00"),
            latLng = 51.1657 to 10.4515,
            translations = mapOf(
                "jpn" to "ドイツ",
                "eng" to "Germany",
                "zho" to "德国",
                "kor" to "독일",
                "fra" to "Allemagne",
                "spa" to "Alemania",
                "deu" to "Deutschland",
                "ita" to "Germania",
                "por" to "Alemanha",
                "ara" to "ألمانيا",
                "hin" to "जर्मनी"
            ),
            iddRoot = "+4",
            iddSuffixes = listOf("9")
        ),
        CountryInfo(
            cca2 = "IT",
            cca3 = "ITA",
            commonName = "Italy",
            officialName = "Italian Republic",
            capital = "Rome",
            region = "Europe",
            subregion = "Southern Europe",
            flagEmoji = "🇮🇹",
            currencies = listOf(CountryCurrency("EUR", "Euro", "€")),
            languages = mapOf("ita" to "Italiano"),
            timezones = listOf("UTC+01:00"),
            latLng = 41.8719 to 12.5674,
            translations = mapOf(
                "jpn" to "イタリア",
                "eng" to "Italy",
                "zho" to "意大利",
                "kor" to "이탈리아",
                "fra" to "Italie",
                "spa" to "Italia",
                "deu" to "Italien",
                "ita" to "Italia",
                "por" to "Itália",
                "ara" to "إيطاليا",
                "hin" to "इटली"
            ),
            iddRoot = "+3",
            iddSuffixes = listOf("9")
        ),
        CountryInfo(
            cca2 = "KR",
            cca3 = "KOR",
            commonName = "South Korea",
            officialName = "Republic of Korea",
            capital = "Seoul",
            region = "Asia",
            subregion = "Eastern Asia",
            flagEmoji = "🇰🇷",
            currencies = listOf(CountryCurrency("KRW", "South Korean won", "₩")),
            languages = mapOf("kor" to "한국어"),
            timezones = listOf("UTC+09:00"),
            latLng = 35.9078 to 127.7669,
            translations = mapOf(
                "jpn" to "大韓民国",
                "eng" to "South Korea",
                "zho" to "韩国",
                "kor" to "대한민국",
                "fra" to "Corée du Sud",
                "spa" to "Corea del Sur",
                "deu" to "Südkorea",
                "ita" to "Corea del Sud",
                "por" to "Coreia do Sul",
                "ara" to "كوريا الجنوبية",
                "hin" to "दक्षिण कोरिया"
            ),
            iddRoot = "+8",
            iddSuffixes = listOf("2")
        ),
        CountryInfo(
            cca2 = "CN",
            cca3 = "CHN",
            commonName = "China",
            officialName = "People's Republic of China",
            capital = "Beijing",
            region = "Asia",
            subregion = "Eastern Asia",
            flagEmoji = "🇨🇳",
            currencies = listOf(CountryCurrency("CNY", "Chinese yuan", "¥")),
            languages = mapOf("zho" to "中文"),
            timezones = listOf("UTC+08:00"),
            latLng = 35.8617 to 104.1954,
            translations = mapOf(
                "jpn" to "中国",
                "eng" to "China",
                "zho" to "中国",
                "kor" to "중국",
                "fra" to "Chine",
                "spa" to "China",
                "deu" to "China",
                "ita" to "Cina",
                "por" to "China",
                "ara" to "الصين",
                "hin" to "चीन"
            ),
            iddRoot = "+8",
            iddSuffixes = listOf("6")
        ),
        CountryInfo(
            cca2 = "TW",
            cca3 = "TWN",
            commonName = "Taiwan",
            officialName = "Republic of China (Taiwan)",
            capital = "Taipei",
            region = "Asia",
            subregion = "Eastern Asia",
            flagEmoji = "🇹🇼",
            currencies = listOf(CountryCurrency("TWD", "New Taiwan dollar", "$")),
            languages = mapOf("zho" to "繁體中文"),
            timezones = listOf("UTC+08:00"),
            latLng = 23.6978 to 120.9605,
            translations = mapOf(
                "jpn" to "台湾",
                "eng" to "Taiwan",
                "zho" to "台湾",
                "kor" to "대만",
                "fra" to "Taïwan",
                "spa" to "Taiwán",
                "deu" to "Taiwan",
                "ita" to "Taiwan",
                "por" to "Taiwan",
                "ara" to "تايوان",
                "hin" to "ताइवान"
            ),
            iddRoot = "+8",
            iddSuffixes = listOf("86")
        ),
        CountryInfo(
            cca2 = "AU",
            cca3 = "AUS",
            commonName = "Australia",
            officialName = "Commonwealth of Australia",
            capital = "Canberra",
            region = "Oceania",
            subregion = "Australia and New Zealand",
            flagEmoji = "🇦🇺",
            currencies = listOf(CountryCurrency("AUD", "Australian dollar", "$")),
            languages = mapOf("eng" to "English"),
            timezones = listOf("UTC+08:00", "UTC+09:30", "UTC+10:00"),
            latLng = -25.2744 to 133.7751,
            translations = mapOf(
                "jpn" to "オーストラリア",
                "eng" to "Australia",
                "zho" to "澳大利亚",
                "kor" to "호주",
                "fra" to "Australie",
                "spa" to "Australia",
                "deu" to "Australien",
                "ita" to "Australia",
                "por" to "Austrália",
                "ara" to "أستراليا",
                "hin" to "ऑस्ट्रेलिया"
            ),
            iddRoot = "+6",
            iddSuffixes = listOf("1")
        ),
        CountryInfo(
            cca2 = "CA",
            cca3 = "CAN",
            commonName = "Canada",
            officialName = "Canada",
            capital = "Ottawa",
            region = "Americas",
            subregion = "North America",
            flagEmoji = "🇨🇦",
            currencies = listOf(CountryCurrency("CAD", "Canadian dollar", "$")),
            languages = mapOf("eng" to "English", "fra" to "Français"),
            timezones = listOf("UTC-05:00", "UTC-06:00", "UTC-07:00", "UTC-08:00"),
            latLng = 56.1304 to -106.3468,
            translations = mapOf(
                "jpn" to "カナダ",
                "eng" to "Canada",
                "zho" to "加拿大",
                "kor" to "캐나다",
                "fra" to "Canada",
                "spa" to "Canadá",
                "deu" to "Kanada",
                "ita" to "Canada",
                "por" to "Canadá",
                "ara" to "كندا",
                "hin" to "कनाडा"
            ),
            iddRoot = "+1",
            iddSuffixes = listOf("")
        ),
        CountryInfo(
            cca2 = "SG",
            cca3 = "SGP",
            commonName = "Singapore",
            officialName = "Republic of Singapore",
            capital = "Singapore",
            region = "Asia",
            subregion = "South-Eastern Asia",
            flagEmoji = "🇸🇬",
            currencies = listOf(CountryCurrency("SGD", "Singapore dollar", "$")),
            languages = mapOf("eng" to "English", "msa" to "Malay", "zho" to "Chinese", "tam" to "Tamil"),
            timezones = listOf("UTC+08:00"),
            latLng = 1.3521 to 103.8198,
            translations = mapOf(
                "jpn" to "シンガポール",
                "eng" to "Singapore",
                "zho" to "新加坡",
                "kor" to "싱가포르",
                "fra" to "Singapour",
                "spa" to "Singapur",
                "deu" to "Singapur",
                "ita" to "Singapore",
                "por" to "Singapura",
                "ara" to "سنغافورة",
                "hin" to "सिंगापुर"
            ),
            iddRoot = "+6",
            iddSuffixes = listOf("5")
        ),
        CountryInfo(
            cca2 = "TH",
            cca3 = "THA",
            commonName = "Thailand",
            officialName = "Kingdom of Thailand",
            capital = "Bangkok",
            region = "Asia",
            subregion = "South-Eastern Asia",
            flagEmoji = "🇹🇭",
            currencies = listOf(CountryCurrency("THB", "Thai baht", "฿")),
            languages = mapOf("tha" to "Thai"),
            timezones = listOf("UTC+07:00"),
            latLng = 15.8700 to 100.9925,
            translations = mapOf(
                "jpn" to "タイ",
                "eng" to "Thailand",
                "zho" to "泰国",
                "kor" to "태국",
                "fra" to "Thaïlande",
                "spa" to "Tailandia",
                "deu" to "Thailand",
                "ita" to "Thailandia",
                "por" to "Tailândia",
                "ara" to "تايلاند",
                "hin" to "थाईलैंड"
            ),
            iddRoot = "+6",
            iddSuffixes = listOf("6")
        ),
        CountryInfo(
            cca2 = "ES",
            cca3 = "ESP",
            commonName = "Spain",
            officialName = "Kingdom of Spain",
            capital = "Madrid",
            region = "Europe",
            subregion = "Southern Europe",
            flagEmoji = "🇪🇸",
            currencies = listOf(CountryCurrency("EUR", "Euro", "€")),
            languages = mapOf("spa" to "Español"),
            timezones = listOf("UTC+01:00"),
            latLng = 40.4637 to -3.7492,
            translations = mapOf(
                "jpn" to "スペイン",
                "eng" to "Spain",
                "zho" to "西班牙",
                "kor" to "스페인",
                "fra" to "Espagne",
                "spa" to "España",
                "deu" to "Spanien",
                "ita" to "Spagna",
                "por" to "Espanha",
                "ara" to "إسبانيا",
                "hin" to "स्पेन"
            ),
            iddRoot = "+3",
            iddSuffixes = listOf("4")
        ),
        CountryInfo(
            cca2 = "CH",
            cca3 = "CHE",
            commonName = "Switzerland",
            officialName = "Swiss Confederation",
            capital = "Bern",
            region = "Europe",
            subregion = "Western Europe",
            flagEmoji = "🇨🇭",
            currencies = listOf(CountryCurrency("CHF", "Swiss franc", "CHF")),
            languages = mapOf("deu" to "Deutsch", "fra" to "Français", "ita" to "Italiano"),
            timezones = listOf("UTC+01:00"),
            latLng = 46.8182 to 8.2275,
            translations = mapOf(
                "jpn" to "スイス",
                "eng" to "Switzerland",
                "zho" to "瑞士",
                "kor" to "스위스",
                "fra" to "Suisse",
                "spa" to "Suiza",
                "deu" to "Schweiz",
                "ita" to "Svizzera",
                "por" to "Suíça",
                "ara" to "سويسرا",
                "hin" to "स्विट्जरलैंड"
            ),
            iddRoot = "+4",
            iddSuffixes = listOf("1")
        ),
        CountryInfo(
            cca2 = "VN",
            cca3 = "VNM",
            commonName = "Vietnam",
            officialName = "Socialist Republic of Vietnam",
            capital = "Hanoi",
            region = "Asia",
            subregion = "South-Eastern Asia",
            flagEmoji = "🇻🇳",
            currencies = listOf(CountryCurrency("VND", "Vietnamese đồng", "₫")),
            languages = mapOf("vie" to "Tiếng Việt"),
            timezones = listOf("UTC+07:00"),
            latLng = 14.0583 to 108.2772,
            translations = mapOf(
                "jpn" to "ベトナム",
                "eng" to "Vietnam",
                "zho" to "越南",
                "kor" to "베트남",
                "fra" to "Viêt Nam",
                "spa" to "Vietnam",
                "deu" to "Vietnam",
                "ita" to "Vietnam",
                "por" to "Vietnã",
                "ara" to "فيتنام",
                "hin" to "वियतनाम"
            ),
            iddRoot = "+8",
            iddSuffixes = listOf("4")
        )
    )
}
