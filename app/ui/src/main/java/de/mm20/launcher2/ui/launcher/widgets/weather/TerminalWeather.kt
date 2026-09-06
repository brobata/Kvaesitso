package de.mm20.launcher2.ui.launcher.widgets.weather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.mm20.launcher2.preferences.MeasurementSystem
import de.mm20.launcher2.preferences.TimeFormat
import de.mm20.launcher2.ui.utils.formatPercent
import de.mm20.launcher2.ui.utils.formatPrecipitation
import de.mm20.launcher2.ui.utils.formatSpeed
import de.mm20.launcher2.ui.utils.formatTemperature
import de.mm20.launcher2.ui.utils.isTwentyFourHours
import de.mm20.launcher2.weather.DailyForecast
import de.mm20.launcher2.weather.Forecast
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Weather as terminal output: a prompt line naming the station, the current reading, a details
 * line, and the next hours as a compact column strip. No icons, no cards.
 */
@Composable
internal fun TerminalWeather(
    forecast: Forecast,
    dayForecasts: List<Forecast>,
    daily: DailyForecast?,
    measurementSystem: MeasurementSystem,
    timeFormat: TimeFormat,
) {
    val context = LocalContext.current
    val mono = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
    val fg = MaterialTheme.colorScheme.onSurface
    val dim = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "❯ weather  # ${forecast.location}",
            style = mono,
            color = dim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.padding(top = 6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = formatTemperature(context, forecast.temperature.toFloat(), measurementSystem),
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = forecast.condition.lowercase(Locale.getDefault()),
                style = mono.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize),
                color = fg,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (daily != null) {
                Text(
                    text = "↑" + formatTemperature(context, daily.maxTemp.toFloat(), measurementSystem) +
                            " ↓" + formatTemperature(context, daily.minTemp.toFloat(), measurementSystem),
                    style = mono,
                    color = dim,
                )
            }
        }

        val details = buildList {
            forecast.windSpeed?.let {
                val dir = forecast.windDirection?.let { d -> " " + cardinal(d) } ?: ""
                add("wind " + formatSpeed(context, it.toFloat(), measurementSystem) + dir)
            }
            forecast.humidity?.let { add("hum " + formatPercent(it.toFloat())) }
            forecast.uvIndex?.let { add("uv " + String.format(Locale.getDefault(), "%.1f", it)) }
            forecast.precipitation?.let {
                if (it > 0.0) add("rain " + formatPrecipitation(context, it.toFloat(), measurementSystem))
            }
            forecast.precipProbability?.let { if (it > 0) add("pop $it%") }
        }
        if (details.isNotEmpty()) {
            Text(
                text = details.joinToString("  ·  "),
                style = mono,
                color = dim,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        val upcoming = remember(dayForecasts, forecast) {
            dayForecasts.filter { it.timestamp > forecast.timestamp }.take(6)
        }
        if (upcoming.isNotEmpty()) {
            val twentyFourHours = timeFormat.isTwentyFourHours(context)
            val hourFormat = remember(twentyFourHours) {
                SimpleDateFormat(if (twentyFourHours) "HH" else "ha", Locale.getDefault())
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                for (fc in upcoming) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = hourFormat.format(fc.timestamp).lowercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = dim,
                        )
                        Text(
                            text = formatTemperature(context, fc.temperature.toFloat(), measurementSystem),
                            style = mono,
                            color = fg,
                        )
                    }
                }
            }
        }
    }
}

private val Cardinals = listOf(
    "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
    "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW",
)

private fun cardinal(degrees: Double): String {
    val index = ((degrees % 360 + 360) % 360 / 22.5 + 0.5).toInt() % 16
    return Cardinals[index]
}
