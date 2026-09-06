package de.mm20.launcher2.weather.openmeteo

import android.content.Context
import android.util.Log
import de.mm20.launcher2.crashreporter.CrashReporter
import de.mm20.launcher2.preferences.weather.WeatherLocation
import de.mm20.launcher2.weather.Forecast
import de.mm20.launcher2.weather.GeocoderWeatherProvider
import de.mm20.launcher2.weather.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Open-Meteo: free worldwide forecasts keyed only on coordinates, so automatic location just
 * works with no account or key. Hourly data for seven days; WMO weather codes map onto the
 * launcher's icon and condition set.
 */
internal class OpenMeteoProvider(
    private val context: Context,
) : GeocoderWeatherProvider(context) {

    private val api = OpenMeteoApi()

    override suspend fun getWeatherData(location: WeatherLocation): List<Forecast>? {
        return when (location) {
            is WeatherLocation.LatLon -> fetch(location.lat, location.lon, location.name)
            else -> {
                Log.e(TAG, "Unsupported location type: $location")
                null
            }
        }
    }

    override suspend fun getWeatherData(lat: Double, lon: Double): List<Forecast>? {
        return fetch(lat, lon, getLocationName(lat, lon))
    }

    private suspend fun fetch(lat: Double, lon: Double, locationName: String): List<Forecast>? {
        val data = try {
            withContext(Dispatchers.IO) { api.forecast(lat, lon) }
        } catch (e: Exception) {
            CrashReporter.logException(e)
            return null
        }
        val hourly = data.hourly ?: return null
        val now = System.currentTimeMillis()
        val cutoff = now / 1000 - 3600
        val providerName = context.getString(R.string.provider_openmeteo)

        val forecasts = mutableListOf<Forecast>()
        for (i in hourly.time.indices) {
            val time = hourly.time[i]
            if (time < cutoff) continue
            val temp = hourly.temperature.getOrNull(i) ?: continue
            val code = hourly.weatherCode.getOrNull(i) ?: -1
            val night = hourly.isDay.getOrNull(i) == 0
            forecasts.add(
                Forecast(
                    timestamp = time * 1000,
                    temperature = temp + 273.15,
                    pressure = hourly.pressureMsl.getOrNull(i),
                    humidity = hourly.relativeHumidity.getOrNull(i),
                    icon = iconForCode(code),
                    condition = context.getString(conditionForCode(code)),
                    windSpeed = hourly.windSpeed.getOrNull(i),
                    windDirection = hourly.windDirection.getOrNull(i),
                    precipitation = hourly.precipitation.getOrNull(i),
                    precipProbability = hourly.precipitationProbability.getOrNull(i),
                    uvIndex = hourly.uvIndex.getOrNull(i),
                    night = night,
                    location = locationName,
                    provider = providerName,
                    providerUrl = "https://open-meteo.com/",
                    updateTime = now,
                )
            )
        }
        return forecasts.takeIf { it.isNotEmpty() }
    }

    /** WMO 4677 weather interpretation codes, as documented by Open-Meteo. */
    private fun iconForCode(code: Int): Int = when (code) {
        0, 1 -> Forecast.CLEAR
        2 -> Forecast.PARTLY_CLOUDY
        3 -> Forecast.OVERCAST
        45, 48 -> Forecast.FOG
        51, 53, 55, 61, 80 -> Forecast.LIGHT_RAIN
        63, 81 -> Forecast.RAIN
        65, 82 -> Forecast.HEAVY_RAIN
        56, 57, 66, 67 -> Forecast.SLEET
        71, 73, 75, 77, 85, 86 -> Forecast.SNOW
        95 -> Forecast.THUNDER
        96, 99 -> Forecast.THUNDERSTORM
        else -> Forecast.UNKNOWN
    }

    private fun conditionForCode(code: Int): Int = when (code) {
        0 -> R.string.weather_condition_clearsky
        1 -> R.string.weather_condition_fair
        2 -> R.string.weather_condition_partlycloudy
        3 -> R.string.weather_condition_cloudy
        45, 48 -> R.string.weather_condition_fog
        51, 53, 55 -> R.string.weather_condition_lightrain
        56, 57 -> R.string.weather_condition_lightsleet
        61 -> R.string.weather_condition_lightrain
        63 -> R.string.weather_condition_rain
        65 -> R.string.weather_condition_heavyrain
        66, 67 -> R.string.weather_condition_sleet
        71 -> R.string.weather_condition_lightsnow
        73, 77 -> R.string.weather_condition_snow
        75 -> R.string.weather_condition_heavysnow
        80 -> R.string.weather_condition_lightrainshowers
        81 -> R.string.weather_condition_rainshowers
        82 -> R.string.weather_condition_heavyrainshowers
        85 -> R.string.weather_condition_lightsnowshowers
        86 -> R.string.weather_condition_heavysnowshowers
        95 -> R.string.weather_condition_thunder
        96, 99 -> R.string.weather_condition_thunderstorm
        else -> R.string.weather_condition_unknown
    }

    companion object {
        const val Id = "openmeteo"
        private const val TAG = "OpenMeteoProvider"
    }
}
