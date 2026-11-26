package com.example.data.repository

import com.example.data.remote.CuratedCountriesData
import com.example.data.remote.NetworkModule
import com.example.data.remote.RestCountriesApi
import com.example.data.remote.RestCountriesJsonParser
import com.example.model.CountryCurrency
import com.example.model.CountryInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class CountriesRepository(
    private val api: RestCountriesApi = NetworkModule.restCountriesApi,
    initialCountries: List<CountryInfo> = CuratedCountriesData.allCuratedCountries
) {
    private val _countriesFlow = MutableStateFlow(initialCountries)
    val countriesFlow: Flow<List<CountryInfo>> = _countriesFlow.asStateFlow()

    private var hasFetchedFromNetwork = false

    suspend fun getCountries(forceRefresh: Boolean = false): List<CountryInfo> {
        if (!forceRefresh && hasFetchedFromNetwork && _countriesFlow.value.isNotEmpty()) {
            return _countriesFlow.value
        }
        return withContext(Dispatchers.IO) {
            try {
                val raw = api.getAllCountries().use { it.string() }
                val parsed = RestCountriesJsonParser.parseCountries(raw)
                if (parsed.isNotEmpty()) {
                    // Merge with curated to ensure complete translations for top destinations
                    val merged = mergeWithCurated(parsed)
                    _countriesFlow.value = merged
                    hasFetchedFromNetwork = true
                    merged
                } else {
                    _countriesFlow.value
                }
            } catch (e: Exception) {
                // Graceful fallback to cached or curated data
                _countriesFlow.value
            }
        }
    }

    suspend fun getCountryByCode(code: String): CountryInfo? {
        val uppercaseCode = code.uppercase().trim()
        val inMemory = _countriesFlow.value.firstOrNull {
            it.cca2 == uppercaseCode || it.cca3 == uppercaseCode
        }
        if (inMemory != null) return inMemory

        return withContext(Dispatchers.IO) {
            try {
                val raw = api.getCountryByCode(uppercaseCode).use { it.string() }
                RestCountriesJsonParser.parseSingleCountry(raw)
            } catch (e: Exception) {
                CuratedCountriesData.allCuratedCountries.firstOrNull {
                    it.cca2 == uppercaseCode || it.cca3 == uppercaseCode
                }
            }
        }
    }

    fun searchCountries(query: String, languageCode: String): List<CountryInfo> {
        val currentList = _countriesFlow.value
        if (query.isBlank()) return currentList
        val trimmed = query.trim().lowercase()

        return currentList.filter { country ->
            country.cca2.lowercase().contains(trimmed) ||
                country.cca3.lowercase().contains(trimmed) ||
                country.commonName.lowercase().contains(trimmed) ||
                country.officialName.lowercase().contains(trimmed) ||
                country.capital?.lowercase()?.contains(trimmed) == true ||
                country.region.lowercase().contains(trimmed) ||
                country.getLocalizedName(languageCode).lowercase().contains(trimmed) ||
                country.getLocalizedName("ja").lowercase().contains(trimmed) ||
                country.getLocalizedName("en").lowercase().contains(trimmed) ||
                country.currencies.any { it.code.lowercase().contains(trimmed) || it.name.lowercase().contains(trimmed) }
        }
    }

    fun getAvailableCurrencies(): List<CountryCurrency> {
        val allCurrencies = _countriesFlow.value.flatMap { it.currencies }
            .distinctBy { it.code }
            .sortedBy { it.code }

        val priorityCodes = listOf("JPY", "USD", "EUR", "GBP", "CNY", "TWD", "KRW", "AUD", "CAD", "CHF", "SGD", "THB")
        val priorityList = priorityCodes.mapNotNull { code -> allCurrencies.firstOrNull { it.code == code } }
        val rest = allCurrencies.filterNot { priorityCodes.contains(it.code) }
        return priorityList + rest
    }

    private fun mergeWithCurated(networkCountries: List<CountryInfo>): List<CountryInfo> {
        val curatedMap = CuratedCountriesData.allCuratedCountries.associateBy { it.cca2 }
        return networkCountries.map { net ->
            val cur = curatedMap[net.cca2]
            if (cur != null) {
                net.copy(
                    translations = (net.translations + cur.translations),
                    currencies = if (net.currencies.isEmpty()) cur.currencies else net.currencies,
                    languages = if (net.languages.isEmpty()) cur.languages else net.languages
                )
            } else {
                net
            }
        }
    }
}
