package com.example.core

import com.example.BuildConfig

object TransitApiConfigs {
    private fun isValidKey(raw: String, placeholder: String): Boolean {
        val k = raw.trim()
        return k.isNotBlank() &&
            k != placeholder &&
            !k.endsWith("_UNCONFIGURED") &&
            !k.startsWith("YOUR_") &&
            !k.startsWith("MY_")
    }

    fun odptApiKey(): String = BuildConfig.ODPT_API_KEY.trim()
    fun isOdptConfigured(): Boolean = isValidKey(odptApiKey(), "ODPT_API_KEY_UNCONFIGURED")

    fun ekispertApiKey(): String = BuildConfig.EKISPERT_API_KEY.trim()
    fun isEkispertConfigured(): Boolean = isValidKey(ekispertApiKey(), "EKISPERT_API_KEY_UNCONFIGURED")

    fun mixwayApiKey(): String = BuildConfig.MIXWAY_API_KEY.trim()
    fun isMixwayConfigured(): Boolean = isValidKey(mixwayApiKey(), "MIXWAY_API_KEY_UNCONFIGURED")

    fun jorudanApiKey(): String = BuildConfig.JORUDAN_API_KEY.trim()
    fun isJorudanConfigured(): Boolean = isValidKey(jorudanApiKey(), "JORUDAN_API_KEY_UNCONFIGURED")

    fun navitimeApiKey(): String = BuildConfig.NAVITIME_API_KEY.trim()
    fun isNavitimeConfigured(): Boolean = isValidKey(navitimeApiKey(), "NAVITIME_API_KEY_UNCONFIGURED")

    private fun isValidHttpsEndpoint(raw: String, placeholder: String): Boolean {
        val trimmed = raw.trim()
        return isValidKey(trimmed, placeholder) &&
            (trimmed.startsWith("https://") || trimmed.startsWith("http://localhost") || trimmed.startsWith("http://127.0.0.1"))
    }

    fun jrTrainformationProxyUrl(): String = BuildConfig.JR_TRAINFORMATION_PROXY_URL.trim().let {
        if (it.endsWith("/")) it else "$it/"
    }

    fun isJrTrainformationConfigured(): Boolean =
        isValidHttpsEndpoint(BuildConfig.JR_TRAINFORMATION_PROXY_URL, "JR_TRAINFORMATION_PROXY_URL_UNCONFIGURED")

    fun jrShikokuProxyUrl(): String = BuildConfig.JR_SHIKOKU_PROXY_URL.trim().let {
        if (it.endsWith("/")) it else "$it/"
    }

    fun isJrShikokuConfigured(): Boolean =
        isValidHttpsEndpoint(BuildConfig.JR_SHIKOKU_PROXY_URL, "JR_SHIKOKU_PROXY_URL_UNCONFIGURED")
}
