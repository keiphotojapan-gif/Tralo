package com.example.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class TraloLanguage(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val flagEmoji: String
) {
    JA("ja", "日本語", "Japanese", "🇯🇵"),
    EN("en", "English", "English", "🇺🇸"),
    ZH_CN("zh-CN", "简体中文", "Simplified Chinese", "🇨🇳"),
    ZH_TW("zh-TW", "繁體中文", "Traditional Chinese", "🇹🇼"),
    KO("ko", "한국어", "Korean", "🇰🇷"),
    FR("fr", "Français", "French", "🇫🇷"),
    ES("es", "Español", "Spanish", "🇪🇸"),
    DE("de", "Deutsch", "German", "🇩🇪"),
    IT("it", "Italiano", "Italian", "🇮🇹"),
    PT("pt", "Português", "Portuguese", "🇧🇷"),
    AR("ar", "العربية", "Arabic", "🇸🇦"),
    HI("hi", "हिन्दी", "Hindi", "🇮🇳");

    companion object {
        fun fromCode(code: String?): TraloLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: JA
    }
}

object TraloLocalization {

    fun getNavLabel(route: String, langCode: String): String {
        return when (route) {
            "home" -> when (langCode.lowercase()) {
                "en" -> "Home"
                "zh-cn" -> "首页"
                "zh-tw" -> "首頁"
                "ko" -> "홈"
                "fr" -> "Accueil"
                "es" -> "Inicio"
                "de" -> "Startseite"
                "it" -> "Home"
                "pt" -> "Início"
                "ar" -> "الرئيسية"
                "hi" -> "होम"
                else -> "ホーム"
            }
            "transit" -> when (langCode.lowercase()) {
                "en" -> "Transit"
                "zh-cn" -> "交通查询"
                "zh-tw" -> "交通查詢"
                "ko" -> "교통 검색"
                "fr" -> "Transports"
                "es" -> "Transporte"
                "de" -> "Verkehr"
                "it" -> "Trasporti"
                "pt" -> "Transporte"
                "ar" -> "المواصلات"
                "hi" -> "यातायात"
                else -> "交通検索"
            }
            "itinerary" -> when (langCode.lowercase()) {
                "en" -> "Itinerary"
                "zh-cn" -> "行程规划"
                "zh-tw" -> "行程規劃"
                "ko" -> "일정"
                "fr" -> "Itinéraire"
                "es" -> "Itinerario"
                "de" -> "Reiseplan"
                "it" -> "Itinerario"
                "pt" -> "Roteiro"
                "ar" -> "جدول الرحلة"
                "hi" -> "यात्रा कार्यक्रम"
                else -> "旅程"
            }
            "today" -> when (langCode.lowercase()) {
                "en" -> "Today"
                "zh-cn" -> "当日出行"
                "zh-tw" -> "當日出行"
                "ko" -> "오늘의 여행"
                "fr" -> "Aujourd'hui"
                "es" -> "Hoy"
                "de" -> "Heute"
                "it" -> "Oggi"
                "pt" -> "Hoje"
                "ar" -> "اليوم"
                "hi" -> "आज"
                else -> "旅行当日"
            }
            "settings" -> when (langCode.lowercase()) {
                "en" -> "Settings"
                "zh-cn" -> "设置"
                "zh-tw" -> "設定"
                "ko" -> "설정"
                "fr" -> "Paramètres"
                "es" -> "Ajustes"
                "de" -> "Einstellungen"
                "it" -> "Impostazioni"
                "pt" -> "Configurações"
                "ar" -> "الإعدادات"
                "hi" -> "सेटिंग्स"
                else -> "設定"
            }
            else -> route
        }
    }

    fun getLabel(key: String, langCode: String): String {
        return translations[key]?.get(langCode)
            ?: translations[key]?.get("en")
            ?: translations[key]?.get("ja")
            ?: key
    }

    private val translations: Map<String, Map<String, String>> = mapOf(
        "settings_title" to mapOf(
            "ja" to "アプリ設定・地域・言語",
            "en" to "Settings & Regional",
            "zh-CN" to "应用设置与区域语言",
            "zh-TW" to "應用設定與地區語言",
            "ko" to "앱 설정 및 지역·언어",
            "fr" to "Paramètres et Région",
            "es" to "Ajustes e Idioma",
            "de" to "Einstellungen & Region",
            "it" to "Impostazioni e Lingua",
            "pt" to "Configurações e Região",
            "ar" to "الإعدادات واللغة",
            "hi" to "सेटिंग्स और भाषा"
        ),
        "language_section" to mapOf(
            "ja" to "言語設定 (Language)",
            "en" to "Language (言語)",
            "zh-CN" to "语言设置 (Language)",
            "zh-TW" to "語言設定 (Language)",
            "ko" to "언어 설정 (Language)",
            "fr" to "Langue (Language)",
            "es" to "Idioma (Language)",
            "de" to "Sprache (Language)",
            "it" to "Lingua (Language)",
            "pt" to "Idioma (Language)",
            "ar" to "إعدادات اللغة (Language)",
            "hi" to "भाषा सेटिंग्स (Language)"
        ),
        "region_country_section" to mapOf(
            "ja" to "地域・国設定 (Country & Region)",
            "en" to "Country & Region",
            "zh-CN" to "国家与地区",
            "zh-TW" to "國家與地區",
            "ko" to "국가 및 지역",
            "fr" to "Pays et Région",
            "es" to "País y Región",
            "de" to "Land & Region",
            "it" to "Paese e Regione",
            "pt" to "País e Região",
            "ar" to "البلد والمنطقة",
            "hi" to "देश और क्षेत्र"
        ),
        "currency_section" to mapOf(
            "ja" to "表示通貨設定 (Currency)",
            "en" to "Display Currency",
            "zh-CN" to "显示货币设置",
            "zh-TW" to "顯示貨幣設定",
            "ko" to "표시 통화 설정",
            "fr" to "Devise d'affichage",
            "es" to "Moneda de visualización",
            "de" to "Währungseinstellungen",
            "it" to "Valuta di visualizzazione",
            "pt" to "Moeda de exibição",
            "ar" to "عملة العرض",
            "hi" to "मुद्रा प्रदर्शन"
        ),
        "datetime_section" to mapOf(
            "ja" to "日付・時刻形式 (Date & Time Format)",
            "en" to "Date & Time Format",
            "zh-CN" to "日期与时间格式",
            "zh-TW" to "日期與時間格式",
            "ko" to "날짜 및 시간 형식",
            "fr" to "Format Date et Heure",
            "es" to "Formato de Fecha y Hora",
            "de" to "Datums- & Zeitformat",
            "it" to "Formato Data e Ora",
            "pt" to "Formato de Data e Hora",
            "ar" to "صيغة التاريخ والوقت",
            "hi" to "तारीख और समय प्रारूप"
        ),
        "units_section" to mapOf(
            "ja" to "数値・単位設定 (Units)",
            "en" to "Units & Measurement",
            "zh-CN" to "数值与单位设置",
            "zh-TW" to "數值與單位設定",
            "ko" to "수치 및 단위 설정",
            "fr" to "Unités de mesure",
            "es" to "Unidades de medida",
            "de" to "Maßeinheiten",
            "it" to "Unità di misura",
            "pt" to "Unidades de medida",
            "ar" to "وحدات القياس",
            "hi" to "इकाई सेटिंग्स"
        ),
        "country_explorer_section" to mapOf(
            "ja" to "世界・国情報インスペクター (REST Countries)",
            "en" to "World Country Explorer (REST Countries)",
            "zh-CN" to "国家信息详情 (REST Countries)",
            "zh-TW" to "國家資訊詳情 (REST Countries)",
            "ko" to "세계 국가 정보 탐색기 (REST Countries)",
            "fr" to "Explorateur de Pays (REST Countries)",
            "es" to "Explorador de Países (REST Countries)",
            "de" to "Länder-Explorer (REST Countries)",
            "it" to "Esploratore Paesi (REST Countries)",
            "pt" to "Explorador de Países (REST Countries)",
            "ar" to "مستكشف دول العالم (REST Countries)",
            "hi" to "विश्व देश एक्सप्लोरर (REST Countries)"
        )
    )

    fun calculateDestinationLocalTime(timezoneStr: String?): String {
        if (timezoneStr.isNullOrBlank()) {
            return SimpleDateFormat("HH:mm", Locale.JAPAN).format(Date())
        }
        val targetTz = resolveTimeZone(timezoneStr)
        val sdf = SimpleDateFormat("HH:mm (zzz)", Locale.JAPAN)
        sdf.timeZone = targetTz
        return sdf.format(Date())
    }

    fun calculateTimeDifference(timezoneStr: String?): String {
        if (timezoneStr.isNullOrBlank()) return "時差なし (端末時間同等)"
        val targetTz = resolveTimeZone(timezoneStr)
        val deviceTz = TimeZone.getDefault()
        val now = System.currentTimeMillis()
        val diffMillis = targetTz.getOffset(now) - deviceTz.getOffset(now)
        val diffHours = diffMillis / (1000 * 60 * 60)
        return when {
            diffHours == 0 -> "端末と同じタイムゾーン (時差0時間)"
            diffHours > 0 -> "日本/端末より +${diffHours}時間 進んでいます"
            else -> "日本/端末より ${diffHours}時間 遅れています"
        }
    }

    private fun resolveTimeZone(tzStr: String): TimeZone {
        val trimmed = tzStr.trim()
        if (trimmed.startsWith("UTC+") || trimmed.startsWith("UTC-")) {
            val formatted = "GMT" + trimmed.substring(3)
            return TimeZone.getTimeZone(formatted)
        }
        return TimeZone.getTimeZone(trimmed)
    }
}
