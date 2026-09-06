package de.mm20.launcher2.weather.openmeteo

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
import java.util.Locale

/** Open-Meteo forecast API: free, worldwide, no key. https://open-meteo.com/en/docs */
internal class OpenMeteoApi {

    private val httpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(Json.Lenient)
            }
            defaultRequest {
                url("https://api.open-meteo.com")
            }
        }
    }

    suspend fun forecast(lat: Double, lon: Double): OpenMeteoForecast {
        return httpClient.get {
            url {
                path("v1", "forecast")
                parameter("latitude", String.format(Locale.ROOT, "%.4f", lat))
                parameter("longitude", String.format(Locale.ROOT, "%.4f", lon))
                parameter(
                    "hourly",
                    "temperature_2m,relative_humidity_2m,precipitation,precipitation_probability," +
                            "weather_code,pressure_msl,wind_speed_10m,wind_direction_10m,uv_index,is_day"
                )
                parameter("wind_speed_unit", "ms")
                parameter("timeformat", "unixtime")
                parameter("timezone", "auto")
                parameter("forecast_days", 7)
            }
        }.body()
    }
}

@Serializable
internal data class OpenMeteoForecast(
    val hourly: Hourly? = null,
) {
    @Serializable
    internal data class Hourly(
        val time: List<Long> = emptyList(),
        @SerialName("temperature_2m") val temperature: List<Double?> = emptyList(),
        @SerialName("relative_humidity_2m") val relativeHumidity: List<Double?> = emptyList(),
        val precipitation: List<Double?> = emptyList(),
        @SerialName("precipitation_probability") val precipitationProbability: List<Int?> = emptyList(),
        @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
        @SerialName("pressure_msl") val pressureMsl: List<Double?> = emptyList(),
        @SerialName("wind_speed_10m") val windSpeed: List<Double?> = emptyList(),
        @SerialName("wind_direction_10m") val windDirection: List<Double?> = emptyList(),
        @SerialName("uv_index") val uvIndex: List<Double?> = emptyList(),
        @SerialName("is_day") val isDay: List<Int?> = emptyList(),
    )
}
