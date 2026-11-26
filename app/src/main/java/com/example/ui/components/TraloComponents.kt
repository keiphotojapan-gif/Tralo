package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WindPower
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.WeatherForecast
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneGoldSoft
import com.example.ui.theme.NaturalSlateBlue
import com.example.ui.theme.SageTint
import com.example.ui.theme.SlateBlueSoft
import com.example.ui.theme.StatusDelayAmber
import com.example.ui.theme.StatusDelayAmberBg
import com.example.ui.theme.StatusDisruptionRed
import com.example.ui.theme.StatusDisruptionRedBg
import com.example.ui.theme.StatusNormalGreen
import com.example.ui.theme.TabularNumericLarge
import com.example.ui.theme.TabularNumericMedium

@DrawableRes
fun drawableForDestinationKey(key: String): Int = when (key) {
    "kyoto" -> R.drawable.img_dest_kyoto
    "hakone" -> R.drawable.img_dest_hakone
    "tokyo" -> R.drawable.img_dest_tokyo
    else -> R.drawable.img_hero_journey
}

@Composable
fun TraloCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val colors = CardDefaults.cardColors(containerColor = containerColor)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            colors = colors,
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            colors = colors,
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun TraloEditorialSectionHeader(
    englishOverline: String,
    japaneseTitle: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = englishOverline.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = japaneseTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        if (trailing != null) {
            trailing()
        }
    }
}

enum class BadgeTone {
    FOREST, GOLD, BLUE, AMBER, CRIMSON, NEUTRAL
}

@Composable
fun TraloStatusBadge(
    label: String,
    tone: BadgeTone = BadgeTone.FOREST,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (tone) {
        BadgeTone.FOREST -> SageTint to StatusNormalGreen
        BadgeTone.GOLD -> ChampagneGoldSoft to Color(0xFF7D5D18)
        BadgeTone.BLUE -> SlateBlueSoft to NaturalSlateBlue
        BadgeTone.AMBER -> StatusDelayAmberBg to StatusDelayAmber
        BadgeTone.CRIMSON -> StatusDisruptionRedBg to StatusDisruptionRed
        BadgeTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun TraloWeatherCard(
    forecast: WeatherForecast,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    onRainyActionClick: (() -> Unit)? = null
) {
    TraloCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${forecast.locationName}の天気",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TraloStatusBadge(label = forecast.provenance, tone = BadgeTone.BLUE)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${forecast.conditionLabelJa} ・ 体感 ${forecast.apparentTempC.toInt()}°C ・ 更新 ${forecast.fetchedAtLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = weatherIconForCode(forecast.weatherCode),
                    contentDescription = forecast.conditionLabelJa,
                    tint = if (forecast.isRainyRisk) NaturalSlateBlue else ChampagneGold,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${forecast.currentTempC.toInt()}°",
                    style = TabularNumericLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("weather_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "天気を更新",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WeatherMetricPill(
                icon = Icons.Outlined.WaterDrop,
                label = "湿度 ${forecast.humidityPercent}%",
                modifier = Modifier.weight(1f)
            )
            WeatherMetricPill(
                icon = Icons.Outlined.WindPower,
                label = "風速 ${forecast.windSpeedKmh.toInt()}km/h",
                modifier = Modifier.weight(1f)
            )
            WeatherMetricPill(
                icon = Icons.Outlined.WbSunny,
                label = "UV ${forecast.uvIndexMax.toInt()}",
                modifier = Modifier.weight(1f)
            )
        }

        if (forecast.hourly.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                forecast.hourly.take(5).forEach { point ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = point.timeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${point.temperatureC.toInt()}°",
                            style = TabularNumericMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${point.precipitationProbability}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = NaturalSlateBlue
                        )
                    }
                }
            }
        }

        if (!forecast.advisoryJa.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = MaterialTheme.shapes.small,
                color = StatusDelayAmberBg
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = forecast.advisoryJa,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StatusDelayAmber,
                        fontWeight = FontWeight.Medium
                    )
                    if (onRainyActionClick != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onRainyActionClick,
                            border = BorderStroke(1.dp, StatusDelayAmber),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDelayAmber)
                        ) {
                            Text("AIで雨天・屋内観光へ調整")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherMetricPill(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun weatherIconForCode(code: Int): ImageVector = when (code) {
    0, 1 -> Icons.Outlined.WbSunny
    in 51..99 -> Icons.Outlined.Umbrella
    else -> Icons.Outlined.CloudQueue
}

@Composable
fun TraloErrorCard(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    TraloCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = StatusDisruptionRedBg
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = "エラー",
                tint = StatusDisruptionRed
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = StatusDisruptionRed,
                modifier = Modifier.weight(1f)
            )
            if (onRetry != null) {
                OutlinedButton(onClick = onRetry) {
                    Text("再試行")
                }
            }
        }
    }
}

@Composable
fun TraloInfoNotice(
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    TraloCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TraloEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    TraloCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.testTag("empty_state_action_button")
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun TraloSkeletonCard(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    TraloCard(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(16.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
        )
    }
}
