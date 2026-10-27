package com.conreo.couchytv.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.roundToInt

/** What a weather icon shows; drawn by the ribbon home, not loaded from files. */
enum class Sky { CLEAR, PARTLY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, STORM }

@Immutable
data class HourCast(val at: Long, val temp: Int, val sky: Sky, val isDay: Boolean, val rainChance: Int?)

@Immutable
data class Forecast(
    /** the place it was fetched for, so a changed city never shows stale weather */
    val place: String,
    val fetchedAt: Long,
    val temp: Int,
    val sky: Sky,
    val isDay: Boolean,
    val high: Int,
    val low: Int,
    val windKmh: Int,
    val uv: Int?,
    val aqi: Int?,
    /** sunrise/sunset of yesterday, today and tomorrow, epoch millis (may be shorter) */
    val sunrises: List<Long>,
    val sunsets: List<Long>,
    val hours: List<HourCast>,
)

@Immutable
data class Place(val name: String, val lat: Double, val lon: Double)

/**
 * Open-Meteo (open-meteo.com): free, no API key, no account. Two calls per
 * refresh (forecast + air quality); the ribbon refreshes every 30 minutes and
 * only while the home screen is showing.
 */
object Weather {
    private val json = Json { ignoreUnknownKeys = true }

    /** Last good forecast, so a relaunched home screen paints it instantly. */
    @Volatile var cache: Forecast? = null
        private set

    fun searchPlaces(query: String, language: String): List<Place> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val lang = language.ifEmpty { "en" }
        val root = get("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=5&language=$lang&format=json")
            .jsonObject
        val results = root["results"] as? JsonArray ?: return emptyList()
        return results.mapNotNull { el ->
            val o = el.jsonObject
            val lat = o.num("latitude") ?: return@mapNotNull null
            val lon = o.num("longitude") ?: return@mapNotNull null
            val parts = listOfNotNull(o.str("name"), o.str("admin1"), o.str("country"))
            Place(parts.distinct().joinToString(", "), lat, lon)
        }
    }

    fun fetch(place: String, lat: Double, lon: Double, celsius: Boolean, now: Long = System.currentTimeMillis()): Forecast {
        val unit = if (celsius) "" else "&temperature_unit=fahrenheit"
        val root = get(
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,weather_code,is_day,wind_speed_10m" +
                "&hourly=temperature_2m,weather_code,precipitation_probability,is_day" +
                "&daily=temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max" +
                "&timezone=auto&past_days=1&forecast_days=2$unit"
        ).jsonObject
        val aqi = runCatching {
            get("https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$lat&longitude=$lon&current=us_aqi")
                .jsonObject["current"]?.jsonObject?.num("us_aqi")?.roundToInt()
        }.getOrNull()
        return parse(root, aqi, now).copy(place = place).also { cache = it }
    }

    /** Pure parsing, separate from the network so it can be checked offline. */
    fun parse(root: JsonObject, aqi: Int?, now: Long): Forecast {
        val tz = TimeZone.getTimeZone(root.str("timezone") ?: "GMT")
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply { timeZone = tz }
        fun time(s: String?): Long? = s?.let { runCatching { fmt.parse(it)?.time }.getOrNull() }

        val cur = root["current"]?.jsonObject ?: error("no current weather")
        val daily = root["daily"]?.jsonObject ?: error("no daily weather")
        val hourly = root["hourly"]?.jsonObject ?: error("no hourly weather")

        val days = daily.arr("time").map { it.jsonPrimitive.content }
        val todayKey = fmt.format(now).substring(0, 10)
        val todayIdx = days.indexOf(todayKey).takeIf { it >= 0 } ?: (days.size / 2)

        val hTimes = hourly.arr("time").map { time(it.jsonPrimitive.content) }
        val hTemp = hourly.arr("temperature_2m")
        val hCode = hourly.arr("weather_code")
        val hRain = hourly.arr("precipitation_probability")
        val hDay = hourly.arr("is_day")
        val start = hTimes.indexOfFirst { it != null && it + 30 * 60_000L > now }.coerceAtLeast(0)
        val hours = (start until minOf(start + 6, hTimes.size)).mapNotNull { i ->
            val at = hTimes[i] ?: return@mapNotNull null
            HourCast(
                at = at,
                temp = hTemp.numAt(i)?.roundToInt() ?: return@mapNotNull null,
                sky = sky(hCode.numAt(i)?.toInt() ?: 0),
                isDay = (hDay.numAt(i) ?: 1.0) > 0.5,
                rainChance = hRain.numAt(i)?.roundToInt(),
            )
        }

        return Forecast(
            place = "",
            fetchedAt = now,
            temp = cur.num("temperature_2m")?.roundToInt() ?: 0,
            sky = sky(cur.num("weather_code")?.toInt() ?: 0),
            isDay = (cur.num("is_day") ?: 1.0) > 0.5,
            high = daily.arr("temperature_2m_max").numAt(todayIdx)?.roundToInt() ?: 0,
            low = daily.arr("temperature_2m_min").numAt(todayIdx)?.roundToInt() ?: 0,
            windKmh = cur.num("wind_speed_10m")?.roundToInt() ?: 0,
            uv = daily.arr("uv_index_max").numAt(todayIdx)?.roundToInt(),
            aqi = aqi,
            sunrises = daily.arr("sunrise").mapNotNull { time((it as? JsonPrimitive)?.content) },
            sunsets = daily.arr("sunset").mapNotNull { time((it as? JsonPrimitive)?.content) },
            hours = hours,
        )
    }

    /** WMO weather interpretation codes, as Open-Meteo returns them. */
    fun sky(code: Int): Sky = when (code) {
        0 -> Sky.CLEAR
        1, 2 -> Sky.PARTLY
        3 -> Sky.CLOUDY
        45, 48 -> Sky.FOG
        51, 53, 55, 56, 57 -> Sky.DRIZZLE
        61, 63, 65, 66, 67, 80, 81, 82 -> Sky.RAIN
        71, 73, 75, 77, 85, 86 -> Sky.SNOW
        95, 96, 99 -> Sky.STORM
        else -> Sky.CLOUDY
    }

    /** Fraction of the moon that is lit, 0..1 (good to a few percent). */
    fun moonLit(now: Long): Double {
        val days = now / 86_400_000.0 + 2440587.5 - 2451550.1
        val phase = (days / 29.530588853).let { it - kotlin.math.floor(it) }
        return (1 - cos(2 * Math.PI * phase)) / 2
    }

    private fun get(url: String): JsonElement {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("User-Agent", "FireboltHome")
        try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            return json.parseToJsonElement(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }

    private fun JsonObject.num(k: String): Double? = (this[k] as? JsonPrimitive)?.doubleOrNull
    private fun JsonObject.str(k: String): String? = (this[k] as? JsonPrimitive)?.takeIf { it.isString }?.content
    private fun JsonObject.arr(k: String): JsonArray = this[k] as? JsonArray ?: JsonArray(emptyList())
    private fun JsonArray.numAt(i: Int): Double? = (getOrNull(i) as? JsonPrimitive)?.let { it.doubleOrNull ?: it.intOrNull?.toDouble() }
}
