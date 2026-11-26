package com.example.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.traloDataStore: DataStore<Preferences> by preferencesDataStore(name = "tralo_prefs")

object OdptConfig {
    fun apiKey(): String = BuildConfig.ODPT_API_KEY.trim()

    fun isConfigured(): Boolean {
        val key = apiKey()
        return key.isNotBlank() &&
            key != "ODPT_API_KEY_UNCONFIGURED" &&
            !key.startsWith("YOUR_")
    }
}

class AppPreferences(private val context: Context) {
    private val recentSearchesKey = stringPreferencesKey("recent_searches")
    private val defaultSortKey = stringPreferencesKey("default_sort")

    val recentSearches: Flow<List<Pair<String, String>>> =
        context.traloDataStore.data.map { prefs ->
            val raw = prefs[recentSearchesKey].orEmpty()
            if (raw.isBlank()) {
                emptyList()
            } else {
                raw.split(";;").mapNotNull { entry ->
                    val parts = entry.split("->")
                    if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                        parts[0] to parts[1]
                    } else null
                }
            }
        }

    val defaultSort: Flow<String> =
        context.traloDataStore.data.map { prefs ->
            prefs[defaultSortKey] ?: "FASTEST"
        }

    suspend fun recordSearch(origin: String, destination: String) {
        if (origin.isBlank() || destination.isBlank()) return
        context.traloDataStore.edit { prefs ->
            val current = prefs[recentSearchesKey].orEmpty()
                .split(";;")
                .filter { it.contains("->") }
            val newEntry = "${origin.trim()}->${destination.trim()}"
            val updated = (listOf(newEntry) + current.filterNot { it == newEntry }).take(6)
            prefs[recentSearchesKey] = updated.joinToString(";;")
        }
    }

    suspend fun saveDefaultSort(sortName: String) {
        context.traloDataStore.edit { prefs ->
            prefs[defaultSortKey] = sortName
        }
    }
}

object PartnerLinks {
    private fun encodeParam(value: String): String =
        java.net.URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    fun bookingComSearchUrl(destination: String, checkIn: String? = null, checkOut: String? = null): String {
        val params = buildList {
            add("ss=${encodeParam(destination)}")
            if (!checkIn.isNullOrBlank()) add("checkin=${encodeParam(checkIn)}")
            if (!checkOut.isNullOrBlank()) add("checkout=${encodeParam(checkOut)}")
        }.joinToString("&")
        return "https://www.booking.com/searchresults.ja.html?$params"
    }

    fun klookSearchUrl(keyword: String): String {
        return "https://www.klook.com/ja/search/result/?query=${encodeParam(keyword)}"
    }

    fun mapsSearchUrl(placeName: String): String {
        return "https://www.google.com/maps/search/?api=1&query=${encodeParam(placeName)}"
    }

    fun openExternalUrl(context: Context, url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            if (uri.scheme != "https" && uri.scheme != "http") return false
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w("PartnerLinks", "Unable to open URL: $url", e)
            false
        }
    }
}
