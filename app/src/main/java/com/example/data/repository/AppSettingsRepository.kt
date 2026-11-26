package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppSettings
import com.example.model.DateFormatOption
import com.example.model.DistanceUnit
import com.example.model.TemperatureUnit
import com.example.model.TimeFormatOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "tralo_app_settings")

class AppSettingsRepository(
    private val context: Context,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    companion object {
        private val KEY_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_COUNTRY_CODE = stringPreferencesKey("app_country_code")
        private val KEY_CURRENCY_CODE = stringPreferencesKey("app_currency_code")
        private val KEY_CURRENCY_SYMBOL = stringPreferencesKey("app_currency_symbol")
        private val KEY_DATE_FORMAT = stringPreferencesKey("app_date_format")
        private val KEY_TIME_FORMAT = stringPreferencesKey("app_time_format")
        private val KEY_DISTANCE_UNIT = stringPreferencesKey("app_distance_unit")
        private val KEY_TEMPERATURE_UNIT = stringPreferencesKey("app_temperature_unit")

        @Volatile
        private var INSTANCE: AppSettingsRepository? = null

        fun getInstance(context: Context): AppSettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppSettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val settingsFlow: StateFlow<AppSettings> = context.appSettingsDataStore.data
        .map { prefs ->
            AppSettings(
                language = prefs[KEY_LANGUAGE] ?: "ja",
                countryCode = prefs[KEY_COUNTRY_CODE] ?: "JP",
                currencyCode = prefs[KEY_CURRENCY_CODE] ?: "JPY",
                currencySymbol = prefs[KEY_CURRENCY_SYMBOL] ?: "¥",
                dateFormat = DateFormatOption.fromId(prefs[KEY_DATE_FORMAT]),
                timeFormat = TimeFormatOption.fromId(prefs[KEY_TIME_FORMAT]),
                distanceUnit = DistanceUnit.fromId(prefs[KEY_DISTANCE_UNIT]),
                temperatureUnit = TemperatureUnit.fromId(prefs[KEY_TEMPERATURE_UNIT])
            )
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = AppSettings()
        )

    suspend fun setLanguage(language: String) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = language
        }
    }

    suspend fun setCountry(countryCode: String, currencyCode: String? = null, currencySymbol: String? = null) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[KEY_COUNTRY_CODE] = countryCode.uppercase()
            if (!currencyCode.isNullOrBlank()) {
                prefs[KEY_CURRENCY_CODE] = currencyCode
            }
            if (!currencySymbol.isNullOrBlank()) {
                prefs[KEY_CURRENCY_SYMBOL] = currencySymbol
            }
        }
    }

    suspend fun setCurrency(currencyCode: String, currencySymbol: String) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[KEY_CURRENCY_CODE] = currencyCode
            prefs[KEY_CURRENCY_SYMBOL] = currencySymbol
        }
    }

    suspend fun setDateFormat(format: DateFormatOption) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[KEY_DATE_FORMAT] = format.id
        }
    }

    suspend fun setTimeFormat(format: TimeFormatOption) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[KEY_TIME_FORMAT] = format.id
        }
    }

    suspend fun setDistanceUnit(unit: DistanceUnit) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[KEY_DISTANCE_UNIT] = unit.id
        }
    }

    suspend fun setTemperatureUnit(unit: TemperatureUnit) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[KEY_TEMPERATURE_UNIT] = unit.id
        }
    }

    suspend fun resetToDefaults() {
        context.appSettingsDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
