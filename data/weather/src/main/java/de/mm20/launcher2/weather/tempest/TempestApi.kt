package de.mm20.launcher2.weather.tempest

import de.mm20.launcher2.serialization.Json
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Tempest (WeatherFlow) REST API. A personal access token from
 * tempestwx.com > Settings > Data Authorizations grants access to the account's own stations.
 * Units are requested in SI so the values map straight onto [de.mm20.launcher2.weather.Forecast].
 */
internal class TempestApi {

    private val httpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(Json.Lenient)
            }
            defaultRequest {
                url("https://swd.weatherflow.com")
            }
        }
    }

    suspend fun stations(token: String): StationsResponse {
        return httpClient.get {
            url {
                path("swd", "rest", "stations")
                parameter("token", token)
            }
        }.body()
    }

    suspend fun betterForecast(stationId: Long, token: String): BetterForecast {
        return httpClient.get {
            url {
                path("swd", "rest", "better_forecast")
                parameter("station_id", stationId)
                parameter("token", token)
                parameter("units_temp", "c")
                parameter("units_wind", "mps")
                parameter("units_pressure", "mb")
                parameter("units_precip", "mm")
                parameter("units_distance", "km")
            }
        }.body()
    }
}

@Serializable
internal data class StationsResponse(
    val stations: List<Station> = emptyList(),
) {
    @Serializable
    internal data class Station(
        @SerialName("station_id") val stationId: Long,
        val name: String? = null,
        @SerialName("public_name") val publicName: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val timezone: String? = null,
    )
}

@Serializable
internal data class BetterForecast(
    @SerialName("current_conditions") val currentConditions: CurrentConditions? = null,
    val forecast: ForecastBlock? = null,
    @SerialName("location_name") val locationName: String? = null,
    val timezone: String? = null,
) {
    @Serializable
    internal data class CurrentConditions(
        val time: Long? = null,
        val conditions: String? = null,
        val icon: String? = null,
        @SerialName("air_temperature") val airTemperature: Double? = null,
        @SerialName("sea_level_pressure") val seaLevelPressure: Double? = null,
        @SerialName("station_pressure") val stationPressure: Double? = null,
        @SerialName("relative_humidity") val relativeHumidity: Double? = null,
        @SerialName("wind_avg") val windAvg: Double? = null,
        @SerialName("wind_direction") val windDirection: Double? = null,
        @SerialName("wind_gust") val windGust: Double? = null,
        val uv: Double? = null,
        @SerialName("feels_like") val feelsLike: Double? = null,
        @SerialName("precip_accum_local_day") val precipAccumLocalDay: Double? = null,
        @SerialName("precip_probability") val precipProbability: Int? = null,
    )

    @Serializable
    internal data class ForecastBlock(
        val hourly: List<Hourly> = emptyList(),
        val daily: List<Daily> = emptyList(),
    )

    @Serializable
    internal data class Hourly(
        val time: Long,
        val conditions: String? = null,
        val icon: String? = null,
        @SerialName("air_temperature") val airTemperature: Double? = null,
        @SerialName("sea_level_pressure") val seaLevelPressure: Double? = null,
        @SerialName("relative_humidity") val relativeHumidity: Double? = null,
        val precip: Double? = null,
        @SerialName("precip_probability") val precipProbability: Int? = null,
        @SerialName("wind_avg") val windAvg: Double? = null,
        @SerialName("wind_direction") val windDirection: Double? = null,
        val uv: Double? = null,
    )

    @Serializable
    internal data class Daily(
        @SerialName("day_start_local") val dayStartLocal: Long,
        val conditions: String? = null,
        val icon: String? = null,
        val sunrise: Long? = null,
        val sunset: Long? = null,
        @SerialName("air_temp_high") val airTempHigh: Double? = null,
        @SerialName("air_temp_low") val airTempLow: Double? = null,
        @SerialName("precip_probability") val precipProbability: Int? = null,
    )
}
