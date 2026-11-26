package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.TraloLanguage
import com.example.core.TraloLocalization
import com.example.model.AppSettings
import com.example.model.CountryCurrency
import com.example.model.CountryInfo
import com.example.model.DateFormatOption
import com.example.model.DistanceUnit
import com.example.model.TemperatureUnit
import com.example.model.TimeFormatOption
import com.example.ui.components.BadgeTone
import com.example.ui.components.TraloCard
import com.example.ui.components.TraloEditorialSectionHeader
import com.example.ui.components.TraloInfoNotice
import com.example.ui.components.TraloStatusBadge
import com.example.ui.theme.TabularNumericLarge
import com.example.ui.theme.TabularNumericMedium
import com.example.viewmodel.SettingsTab
import com.example.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onCreateTripForCountry: (CountryInfo) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val allCountries by viewModel.allCountries.collectAsStateWithLifecycle()
    val filteredCountries by viewModel.filteredCountries.collectAsStateWithLifecycle()
    val availableCurrencies by viewModel.availableCurrencies.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeCountry by viewModel.activeCountryInfo.collectAsStateWithLifecycle()
    val selectedCountryForInspection by viewModel.selectedCountryForInspection.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    var showInspectorModal by remember { mutableStateOf(false) }

    LaunchedEffect(statusMessage) {
        if (!statusMessage.isNullOrBlank()) {
            kotlinx.coroutines.delay(2500)
            viewModel.clearStatus()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen")
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("settings_back_button")
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "戻る")
            }
            Text(
                text = TraloLocalization.getLabel("settings_title", settings.language),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { viewModel.refreshCountries() },
                modifier = Modifier.testTag("settings_refresh_button")
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.Refresh, contentDescription = "国情報を更新")
                }
            }
        }

        // Sub Tabs
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(SettingsTab.entries) { tab ->
                FilterChip(
                    selected = selectedTab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    label = { Text(tab.labelJa) },
                    modifier = Modifier.testTag("settings_tab_${tab.name.lowercase()}")
                )
            }
        }

        AnimatedVisibility(visible = !statusMessage.isNullOrBlank()) {
            statusMessage?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // General Overview Card
            if (selectedTab == SettingsTab.GENERAL || selectedTab == SettingsTab.REGION) {
                item {
                    SettingsSummaryCard(
                        settings = settings,
                        activeCountry = activeCountry,
                        onOpenLanguage = { viewModel.selectTab(SettingsTab.LANGUAGE) },
                        onOpenCountry = { viewModel.selectTab(SettingsTab.REGION) },
                        onOpenCurrency = { viewModel.selectTab(SettingsTab.CURRENCY) }
                    )
                }
            }

            // Language Settings Section
            if (selectedTab == SettingsTab.GENERAL || selectedTab == SettingsTab.LANGUAGE) {
                item {
                    TraloEditorialSectionHeader(
                        englishOverline = "Multilingual Localization",
                        japaneseTitle = TraloLocalization.getLabel("language_section", settings.language)
                    )
                }
                item {
                    LanguageSelectionCard(
                        selectedLanguageCode = settings.language,
                        onSelectLanguage = { viewModel.setLanguage(it) }
                    )
                }
            }

            // Country / Region Settings Section
            if (selectedTab == SettingsTab.GENERAL || selectedTab == SettingsTab.REGION) {
                item {
                    TraloEditorialSectionHeader(
                        englishOverline = "Country & Regional Scope",
                        japaneseTitle = TraloLocalization.getLabel("region_country_section", settings.language)
                    )
                }
                item {
                    CountrySearchAndPickerCard(
                        activeCountry = activeCountry,
                        searchQuery = searchQuery,
                        countries = filteredCountries,
                        currentLanguage = settings.language,
                        onQueryChange = viewModel::onSearchQueryChange,
                        onSelectCountry = { country ->
                            viewModel.setCountry(country)
                        },
                        onInspectCountry = { country ->
                            viewModel.selectCountryForInspection(country)
                            showInspectorModal = true
                        }
                    )
                }
            }

            // Currency Settings Section
            if (selectedTab == SettingsTab.GENERAL || selectedTab == SettingsTab.CURRENCY) {
                item {
                    TraloEditorialSectionHeader(
                        englishOverline = "Currency & Price Units",
                        japaneseTitle = TraloLocalization.getLabel("currency_section", settings.language)
                    )
                }
                item {
                    CurrencySelectionCard(
                        currentCurrencyCode = settings.currencyCode,
                        currentCurrencySymbol = settings.currencySymbol,
                        activeCountry = activeCountry,
                        currencies = availableCurrencies,
                        onSelectCurrency = { code, symbol ->
                            viewModel.setCurrency(code, symbol)
                        }
                    )
                }
            }

            // Date & Time Format Section
            if (selectedTab == SettingsTab.GENERAL || selectedTab == SettingsTab.DATE_TIME) {
                item {
                    TraloEditorialSectionHeader(
                        englishOverline = "Temporal Formatting",
                        japaneseTitle = TraloLocalization.getLabel("datetime_section", settings.language)
                    )
                }
                item {
                    DateTimeFormatCard(
                        settings = settings,
                        onDateFormatChange = viewModel::setDateFormat,
                        onTimeFormatChange = viewModel::setTimeFormat
                    )
                }
            }

            // Units Section
            if (selectedTab == SettingsTab.GENERAL || selectedTab == SettingsTab.UNITS) {
                item {
                    TraloEditorialSectionHeader(
                        englishOverline = "Measurement & Units",
                        japaneseTitle = TraloLocalization.getLabel("units_section", settings.language)
                    )
                }
                item {
                    UnitsSelectionCard(
                        settings = settings,
                        onDistanceUnitChange = viewModel::setDistanceUnit,
                        onTemperatureUnitChange = viewModel::setTemperatureUnit
                    )
                }
            }

            // World Explorer Section
            if (selectedTab == SettingsTab.GENERAL || selectedTab == SettingsTab.COUNTRY_INFO) {
                item {
                    TraloEditorialSectionHeader(
                        englishOverline = "REST Countries Explorer",
                        japaneseTitle = TraloLocalization.getLabel("country_explorer_section", settings.language)
                    )
                }
                item {
                    WorldCountryExplorerCard(
                        countries = filteredCountries,
                        searchQuery = searchQuery,
                        currentLanguage = settings.language,
                        onQueryChange = viewModel::onSearchQueryChange,
                        onSelectCountry = { country ->
                            viewModel.selectCountryForInspection(country)
                            showInspectorModal = true
                        }
                    )
                }
            }
        }
    }

    if (showInspectorModal && selectedCountryForInspection != null) {
        CountryDetailModal(
            country = selectedCountryForInspection!!,
            currentLanguage = settings.language,
            onDismiss = { showInspectorModal = false },
            onSetAsSelectedCountry = { country ->
                viewModel.setCountry(country)
                showInspectorModal = false
            },
            onCreateTrip = { country ->
                showInspectorModal = false
                onCreateTripForCountry(country)
            }
        )
    }
}

@Composable
private fun SettingsSummaryCard(
    settings: AppSettings,
    activeCountry: CountryInfo?,
    onOpenLanguage: () -> Unit,
    onOpenCountry: () -> Unit,
    onOpenCurrency: () -> Unit
) {
    TraloCard(modifier = Modifier.testTag("settings_summary_card")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("現在のアプリ構成", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("旅行計画・交通案内・金額表示に連動中", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TraloStatusBadge(
                label = activeCountry?.commonName ?: settings.countryCode,
                tone = BadgeTone.FOREST
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SummaryItem(
                label = "表示言語",
                value = TraloLanguage.fromCode(settings.language).nativeName,
                badge = TraloLanguage.fromCode(settings.language).flagEmoji,
                onClick = onOpenLanguage
            )
            SummaryItem(
                label = "対象国",
                value = activeCountry?.getLocalizedName(settings.language) ?: settings.countryCode,
                badge = activeCountry?.flagEmoji ?: "🌐",
                onClick = onOpenCountry
            )
            SummaryItem(
                label = "表示通貨",
                value = "${settings.currencyCode} (${settings.currencySymbol})",
                badge = settings.currencySymbol,
                onClick = onOpenCurrency
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SummaryItem(label = "日付形式", value = settings.dateFormat.labelJa.substringBefore(" "))
            SummaryItem(label = "時刻形式", value = settings.timeFormat.example)
            SummaryItem(label = "単位", value = "${settings.distanceUnit.unitSymbol} / ${settings.temperatureUnit.unitSymbol}")
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    badge: String? = null,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (badge != null) Text(badge, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LanguageSelectionCard(
    selectedLanguageCode: String,
    onSelectLanguage: (String) -> Unit
) {
    TraloCard(modifier = Modifier.testTag("language_selection_card")) {
        Text("対応言語 (12言語)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "選択した言語はアプリ内のメニュー、設定、旅行情報に即時適用されます。国名はREST Countries APIの多言語翻訳データを優先します。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TraloLanguage.entries.forEach { lang ->
                val isSelected = lang.code.equals(selectedLanguageCode, ignoreCase = true)
                Surface(
                    onClick = { onSelectLanguage(lang.code) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("language_option_${lang.code}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(lang.flagEmoji, style = MaterialTheme.typography.titleMedium)
                            Column {
                                Text(lang.nativeName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(lang.englishName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectLanguage(lang.code) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountrySearchAndPickerCard(
    activeCountry: CountryInfo?,
    searchQuery: String,
    countries: List<CountryInfo>,
    currentLanguage: String,
    onQueryChange: (String) -> Unit,
    onSelectCountry: (CountryInfo) -> Unit,
    onInspectCountry: (CountryInfo) -> Unit
) {
    TraloCard(modifier = Modifier.testTag("country_picker_card")) {
        Text("地域・国設定 (REST Countries)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "旅行計画の初期国や地域情報として使用します。国の変更と表示言語の選択は完全に独立しています。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Currently Selected Country Banner
        if (activeCountry != null) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(activeCountry.flagEmoji, style = MaterialTheme.typography.headlineMedium)
                        Column {
                            Text(
                                text = "選択中: ${activeCountry.getLocalizedName(currentLanguage)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "首都: ${activeCountry.capital ?: "未指定"} / 地域: ${activeCountry.region}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { onInspectCountry(activeCountry) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("詳細", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            label = { Text("国名・首都・コードを検索 (例: 日本, USA, Paris, KR)") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = "クリア")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("country_search_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            countries.take(8).forEach { country ->
                val isSelected = activeCountry?.cca2 == country.cca2
                Surface(
                    onClick = { onSelectCountry(country) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("country_item_${country.cca2}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(country.flagEmoji, style = MaterialTheme.typography.titleLarge)
                            Column {
                                Text(
                                    text = "${country.getLocalizedName(currentLanguage)} (${country.cca2})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "首都: ${country.capital ?: "---"}  通貨: ${country.primaryCurrency?.code ?: "---"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = { onInspectCountry(country) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("詳細", style = MaterialTheme.typography.labelSmall)
                            }
                            if (isSelected) {
                                Icon(Icons.Outlined.Check, contentDescription = "選択中", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencySelectionCard(
    currentCurrencyCode: String,
    currentCurrencySymbol: String,
    activeCountry: CountryInfo?,
    currencies: List<CountryCurrency>,
    onSelectCurrency: (code: String, symbol: String) -> Unit
) {
    TraloCard(modifier = Modifier.testTag("currency_selection_card")) {
        Text("表示通貨設定 (REST Countries Currencies)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TraloInfoNotice(
            title = "為替レートと表示通貨について",
            body = "REST Countries APIから提供される通貨コード・通貨名・通貨記号を反映しています。最新の為替レートは変動するため固定の架空レートは適用せず、アプリ内の予算・費用の表示記号を統一管理します。"
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Candidate Currency Suggestion
        val countryCurr = activeCountry?.primaryCurrency
        if (countryCurr != null && countryCurr.code != currentCurrencyCode) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("訪問国の通貨候補", style = MaterialTheme.typography.labelSmall)
                        Text("${countryCurr.code} (${countryCurr.symbol} - ${countryCurr.name})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onSelectCurrency(countryCurr.code, countryCurr.symbol) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("この通貨に変更", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Major Currencies List
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            currencies.take(10).forEach { curr ->
                val isSelected = curr.code == currentCurrencyCode
                Surface(
                    onClick = { onSelectCurrency(curr.code, curr.symbol) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("currency_option_${curr.code}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(curr.symbol, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Column {
                                Text("${curr.code} - ${curr.name}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("表示例: ${curr.symbol}12,500", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectCurrency(curr.code, curr.symbol) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateTimeFormatCard(
    settings: AppSettings,
    onDateFormatChange: (DateFormatOption) -> Unit,
    onTimeFormatChange: (TimeFormatOption) -> Unit
) {
    TraloCard(modifier = Modifier.testTag("datetime_format_card")) {
        Text("日付形式 (Date Format)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DateFormatOption.entries.forEach { format ->
                val isSelected = settings.dateFormat == format
                Surface(
                    onClick = { onDateFormatChange(format) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(format.labelJa, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("プレビュー: ${settings.copy(dateFormat = format).formatDate("20260410")}", style = TabularNumericMedium)
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { onDateFormatChange(format) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text("時刻形式 (Time Format)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TimeFormatOption.entries.forEach { format ->
                val isSelected = settings.timeFormat == format
                Surface(
                    onClick = { onTimeFormatChange(format) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(format.labelJa, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("プレビュー: ${settings.copy(timeFormat = format).formatTime(14, 30)}", style = TabularNumericMedium)
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { onTimeFormatChange(format) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitsSelectionCard(
    settings: AppSettings,
    onDistanceUnitChange: (DistanceUnit) -> Unit,
    onTemperatureUnitChange: (TemperatureUnit) -> Unit
) {
    TraloCard(modifier = Modifier.testTag("units_selection_card")) {
        Text("距離単位 (Distance Unit)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DistanceUnit.entries.forEach { unit ->
                FilterChip(
                    selected = settings.distanceUnit == unit,
                    onClick = { onDistanceUnitChange(unit) },
                    label = { Text(unit.labelJa) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Text("プレビュー (京都→大阪 42.8km): ${settings.formatDistance(42.8)}", style = TabularNumericMedium)

        Spacer(modifier = Modifier.height(14.dp))
        Text("温度単位 (Temperature Unit)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TemperatureUnit.entries.forEach { unit ->
                FilterChip(
                    selected = settings.temperatureUnit == unit,
                    onClick = { onTemperatureUnitChange(unit) },
                    label = { Text(unit.labelJa) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Text("プレビュー (気温 21.0℃): ${settings.formatTemperature(21.0)}", style = TabularNumericMedium)
    }
}

@Composable
private fun WorldCountryExplorerCard(
    countries: List<CountryInfo>,
    searchQuery: String,
    currentLanguage: String,
    onQueryChange: (String) -> Unit,
    onSelectCountry: (CountryInfo) -> Unit
) {
    TraloCard(modifier = Modifier.testTag("world_explorer_card")) {
        Text("世界・国情報インスペクター", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "REST Countries APIを活用し、世界各国の首都・タイムゾーン・現地時刻・公用語・通貨・国際電話番号を確認できます。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            countries.take(6).forEach { country ->
                Surface(
                    onClick = { onSelectCountry(country) },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(country.flagEmoji, style = MaterialTheme.typography.titleLarge)
                            Column {
                                Text(
                                    text = "${country.getLocalizedName(currentLanguage)} (${country.commonName})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "タイムゾーン: ${country.primaryTimezone ?: "UTC"}  時差: ${TraloLocalization.calculateTimeDifference(country.primaryTimezone)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { onSelectCountry(country) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("開く", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountryDetailModal(
    country: CountryInfo,
    currentLanguage: String,
    onDismiss: () -> Unit,
    onSetAsSelectedCountry: (CountryInfo) -> Unit,
    onCreateTrip: (CountryInfo) -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(country.flagEmoji, style = MaterialTheme.typography.headlineMedium)
                Column {
                    Text(country.getLocalizedName(currentLanguage), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(country.officialName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("国コード", "${country.cca2} / ${country.cca3}")
                DetailRow("地域・大陸", "${country.region} (${country.subregion ?: "---"})")
                DetailRow("首都", country.capital ?: "未指定")
                DetailRow("主要通貨", country.currencies.joinToString { "${it.code} (${it.symbol} ${it.name})" }.ifBlank { "---" })
                DetailRow("公用語", country.languages.values.joinToString().ifBlank { "---" })
                DetailRow("国際電話番号", country.callingCode ?: "---")
                HorizontalDivider()
                DetailRow("タイムゾーン", country.timezones.joinToString().ifBlank { "UTC" })
                DetailRow("現地現在時刻", TraloLocalization.calculateDestinationLocalTime(country.primaryTimezone))
                DetailRow("時差", TraloLocalization.calculateTimeDifference(country.primaryTimezone))
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreateTrip(country) },
                modifier = Modifier.testTag("create_trip_for_country_button")
            ) {
                Icon(Icons.Outlined.FlightTakeoff, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("この国への旅程を作成")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { onSetAsSelectedCountry(country) }) {
                Text("この国を設定")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
