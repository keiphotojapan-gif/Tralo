package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AppSettingsRepository
import com.example.data.repository.CountriesRepository
import com.example.model.AppSettings
import com.example.model.CountryCurrency
import com.example.model.CountryInfo
import com.example.model.DateFormatOption
import com.example.model.DistanceUnit
import com.example.model.TemperatureUnit
import com.example.model.TimeFormatOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SettingsTab(val labelJa: String, val labelEn: String) {
    GENERAL("一般設定", "General"),
    LANGUAGE("言語", "Language"),
    REGION("地域・国", "Country"),
    CURRENCY("通貨", "Currency"),
    DATE_TIME("日時形式", "Date/Time"),
    UNITS("単位", "Units"),
    COUNTRY_INFO("国情報", "World Info")
}

class SettingsViewModel(
    private val settingsRepository: AppSettingsRepository,
    private val countriesRepository: CountriesRepository = CountriesRepository()
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow

    val allCountries: StateFlow<List<CountryInfo>> = countriesRepository.countriesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val searchQuery = MutableStateFlow("")

    val selectedTab = MutableStateFlow(SettingsTab.GENERAL)

    val selectedCountryForInspection = MutableStateFlow<CountryInfo?>(null)

    val statusMessage = MutableStateFlow<String?>(null)

    val isRefreshing = MutableStateFlow(false)

    val filteredCountries: StateFlow<List<CountryInfo>> = combine(
        allCountries,
        searchQuery,
        settings
    ) { countries, query, currentSettings ->
        if (query.isBlank()) {
            countries
        } else {
            countriesRepository.searchCountries(query, currentSettings.language)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableCurrencies: StateFlow<List<CountryCurrency>> = allCountries
        .combine(settings) { _, _ ->
            countriesRepository.getAvailableCurrencies()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCountryInfo: StateFlow<CountryInfo?> = combine(
        allCountries,
        settings
    ) { countries, currentSettings ->
        countries.firstOrNull { it.cca2 == currentSettings.countryCode }
            ?: countries.firstOrNull { it.cca2 == "JP" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        refreshCountries()
    }

    fun selectTab(tab: SettingsTab) {
        selectedTab.value = tab
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun refreshCountries() {
        viewModelScope.launch {
            isRefreshing.value = true
            try {
                countriesRepository.getCountries(forceRefresh = true)
            } catch (e: Exception) {
                // Handled gracefully in repo
            } finally {
                isRefreshing.value = false
            }
        }
    }

    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            settingsRepository.setLanguage(languageCode)
            statusMessage.value = "言語設定を更新しました"
        }
    }

    fun setCountry(country: CountryInfo, autoApplyCurrency: Boolean = true) {
        viewModelScope.launch {
            val curr = country.primaryCurrency
            val currencyCode = if (autoApplyCurrency && curr != null) curr.code else null
            val currencySymbol = if (autoApplyCurrency && curr != null) curr.symbol else null
            settingsRepository.setCountry(country.cca2, currencyCode, currencySymbol)
            statusMessage.value = "地域・国を「${country.commonName}」に更新しました"
        }
    }

    fun setCurrency(code: String, symbol: String) {
        viewModelScope.launch {
            settingsRepository.setCurrency(code, symbol)
            statusMessage.value = "表示通貨を「$code ($symbol)」に更新しました"
        }
    }

    fun setDateFormat(format: DateFormatOption) {
        viewModelScope.launch {
            settingsRepository.setDateFormat(format)
            statusMessage.value = "日付形式を更新しました"
        }
    }

    fun setTimeFormat(format: TimeFormatOption) {
        viewModelScope.launch {
            settingsRepository.setTimeFormat(format)
            statusMessage.value = "時刻形式を更新しました"
        }
    }

    fun setDistanceUnit(unit: DistanceUnit) {
        viewModelScope.launch {
            settingsRepository.setDistanceUnit(unit)
            statusMessage.value = "距離単位を「${unit.unitSymbol}」に更新しました"
        }
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        viewModelScope.launch {
            settingsRepository.setTemperatureUnit(unit)
            statusMessage.value = "温度単位を「${unit.unitSymbol}」に更新しました"
        }
    }

    fun selectCountryForInspection(country: CountryInfo?) {
        selectedCountryForInspection.value = country
    }

    fun clearStatus() {
        statusMessage.value = null
    }
}
