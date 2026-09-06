package de.mm20.launcher2.weather.tempest

import android.content.Context
import android.util.Log
import de.mm20.launcher2.crashreporter.CrashReporter
import de.mm20.launcher2.preferences.weather.WeatherLocation
import de.mm20.launcher2.preferences.weather.WeatherSettings
import de.mm20.launcher2.weather.Forecast
import de.mm20.launcher2.weather.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos

/**
 * Weather from the account's own Tempest station, via WeatherFlow's better_forecast endpoint.
 *
 * The station is the "location": [findLocation] lists the account's stations, and automatic
 * location picks the station nearest to the device. The first forecast entry is the live
 * station observation so the widget's "now" reading comes from the roof, not a model.
 *
 * The token comes from the provider settings (Weather settings > Tempest token). A build-time
 * `tempest_token` string resource (data/weather/src/main/res/values/config.xml, git-ignored)
 * acts as a fallback so a personal build works with zero setup.
 */
class TempestProvider internal constructor(
    private val context: Context,
    private val weatherSettings: WeatherSettings,
) : de.mm20.launcher2.weather.WeatherProvider {

    private val api = TempestApi()

    override suspend fun getUpdateInterval(): Long = 15 * 60 * 1000L

    private suspend fun token(): String? {
        weatherSettings.providerApiKey(Id).first()?.takeIf { it.isNotBlank() }?.let { return it }
        return getBuiltInToken(context)
    }

    override suspend fun findLocation(query: String): List<WeatherLocation> {
        val token = token() ?: return emptyList()
        val stations = try {
            withContext(Dispatchers.IO) { api.stations(token).stations }
        } catch (e: Exception) {
            CrashReporter.logException(e)
            return emptyList()
        }
        val needle = query.trim()
        return stations
            .filter { needle.isEmpty() || it.displayName.contains(needle, ignoreCase = true) }
            .map { WeatherLocation.Id(name = it.displayName, locationId = it.stationId.toString()) }
    }

    override suspend fun getWeatherData(location: WeatherLocation): List<Forecast>? {
        return when (location) {
            is WeatherLocation.Id -> {
                val stationId = location.locationId.toLongOrNull() ?: return null
                forecastFor(stationId, location.name)
            }
            is WeatherLocation.LatLon -> getWeatherData(location.lat, location.lon)
            else -> {
                Log.e(TAG, "Unsupported location type: $location")
                null
            }
        }
    }

    override suspend fun getWeatherData(lat: Double, lon: Double): List<Forecast>? {
        val token = token() ?: return null
        val stations = try {
            withContext(Dispatchers.IO) { api.stations(token).stations }
        } catch (e: Exception) {
            CrashReporter.logException(e)
            return null
        }
        // Nearest station by a flat-earth approximation; stations of one account are close enough.
        val station = stations.minByOrNull {
            val dLat = (it.latitude ?: 0.0) - lat
            val dLon = ((it.longitude ?: 0.0) - lon) * cos(Math.toRadians(lat))
            dLat * dLat + dLon * dLon
        } ?: return null
        return forecastFor(station.stationId, station.displayName)
    }

    private suspend fun forecastFor(stationId: Long, stationName: String): List<Forecast>? {
        val token = token() ?: return null
        val data = try {
            withContext(Dispatchers.IO) { api.betterForecast(stationId, token) }
        } catch (e: Exception) {
            CrashReporter.logException(e)
            return null
        }

        val now = System.currentTimeMillis()
        val providerName = context.getString(R.string.provider_tempest)
        val providerUrl = "https://tempestwx.com/station/$stationId"
        val locationName = stationName.ifBlank { data.locationName ?: "Tempest $stationId" }
        val daily = data.forecast?.daily.orEmpty()

        fun isNight(timestampMs: Long, icon: String?): Boolean {
            if (icon != null && icon.endsWith("-night")) return true
            if (icon != null && icon.endsWith("-day")) return false
            val ts = timestampMs / 1000
            val day = daily.lastOrNull { it.dayStartLocal <= ts } ?: return false
            val rise = day.sunrise ?: return false
            val set = day.sunset ?: return false
            return ts < rise || ts > set
        }

        val forecasts = mutableListOf<Forecast>()

        data.currentConditions?.let { cc ->
            val temp = cc.airTemperature
            val time = cc.time
            if (temp != null && time != null) {
                forecasts.add(
                    Forecast(
                        timestamp = time * 1000,
                        temperature = temp + 273.15,
                        pressure = cc.seaLevelPressure ?: cc.stationPressure,
                        humidity = cc.relativeHumidity,
                        icon = iconFor(cc.icon),
                        condition = cc.conditions ?: "",
                        windSpeed = cc.windAvg,
                        windDirection = cc.windDirection,
                        precipitation = null,
                        precipProbability = cc.precipProbability,
                        uvIndex = cc.uv,
                        night = isNight(time * 1000, cc.icon),
                        location = locationName,
                        provider = providerName,
                        providerUrl = providerUrl,
                        updateTime = now,
                    )
                )
            }
        }

        val hourlyCutoff = (now / 1000) - 3600
        for (h in data.forecast?.hourly.orEmpty()) {
            if (h.time < hourlyCutoff) continue
            val temp = h.airTemperature ?: continue
            // Skip an hourly entry that would sit on top of the live observation.
            if (forecasts.any { abs(it.timestamp - h.time * 1000) < 5 * 60 * 1000 }) continue
            forecasts.add(
                Forecast(
                    timestamp = h.time * 1000,
                    temperature = temp + 273.15,
                    pressure = h.seaLevelPressure,
                    humidity = h.relativeHumidity,
                    icon = iconFor(h.icon),
                    condition = h.conditions ?: "",
                    windSpeed = h.windAvg,
                    windDirection = h.windDirection,
                    precipitation = h.precip,
                    precipProbability = h.precipProbability,
                    uvIndex = h.uv,
                    night = isNight(h.time * 1000, h.icon),
                    location = locationName,
                    provider = providerName,
                    providerUrl = providerUrl,
                    updateTime = now,
                )
            )
        }

        forecasts.sortBy { it.timestamp }
        return forecasts.takeIf { it.isNotEmpty() }
    }

    private fun iconFor(icon: String?): Int {
        val i = icon?.lowercase() ?: return Forecast.UNKNOWN
        return when {
            i.startsWith("clear") -> Forecast.CLEAR
            i.startsWith("partly-cloudy") -> Forecast.PARTLY_CLOUDY
            i == "cloudy" -> Forecast.OVERCAST
            i == "foggy" -> Forecast.FOG
            i.startsWith("possibly-rainy") -> Forecast.LIGHT_RAIN
            i == "rainy" -> Forecast.RAIN
            i.startsWith("possibly-sleet") || i == "sleet" -> Forecast.SLEET
            i.startsWith("possibly-snow") || i == "snow" -> Forecast.SNOW
            i.startsWith("possibly-thunderstorm") -> Forecast.THUNDER
            i.startsWith("thunderstorm") -> Forecast.THUNDERSTORM
            i == "windy" -> Forecast.WIND
            else -> Forecast.UNKNOWN
        }
    }

    private val StationsResponse.Station.displayName: String
        get() = name?.takeIf { it.isNotBlank() } ?: publicName?.takeIf { it.isNotBlank() } ?: "Station $stationId"

    companion object {
        const val Id = "tempest"
        private const val TAG = "TempestProvider"

        /** Token compiled in from the git-ignored config.xml, if any. */
        fun getBuiltInToken(context: Context): String? {
            val resId = context.resources.getIdentifier("tempest_token", "string", context.packageName)
            if (resId == 0) return null
            return context.getString(resId).takeIf { it.isNotBlank() }
        }
    }
}
