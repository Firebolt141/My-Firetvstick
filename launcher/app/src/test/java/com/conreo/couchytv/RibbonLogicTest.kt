package com.conreo.couchytv

import androidx.compose.ui.graphics.Color
import com.conreo.couchytv.data.AppEntry
import com.conreo.couchytv.data.CategoryCfg
import com.conreo.couchytv.data.Forecast
import com.conreo.couchytv.data.LauncherConfig
import com.conreo.couchytv.data.RibbonOrder
import com.conreo.couchytv.data.Sky
import com.conreo.couchytv.data.Weather
import com.conreo.couchytv.ui.arcOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Plain JVM checks of the ribbon's ordering, the move step, weather parsing and the sky arc. */
class RibbonLogicTest {

    private fun app(pkg: String) = AppEntry(pkg, pkg, null, null, "apps", Color.Black, Color.Black, 0, 0)

    private val pinned = CategoryCfg("streaming", "Pinned")
    private val all = CategoryCfg("apps", "All apps")
    private val a = app("a"); private val b = app("b"); private val c = app("c"); private val d = app("d"); private val e = app("e")
    private val sections = listOf(pinned to listOf(a, b, c), all to listOf(b, d, e))

    @Test fun recentSkipsHiddenMissingAndDuplicates() {
        val cfg = LauncherConfig(recent = listOf("x", "c", "a", "c", "d", "e"), hidden = setOf("a"))
        assertEquals(listOf("c", "d", "e"), RibbonOrder.recent(listOf(a, b, c, d, e), cfg).map { it.pkg })
    }

    @Test fun flatShowsEachAppOnceAndLeavesRecentOut() {
        val flat = RibbonOrder.flat(sections, recent = setOf("c"), movePkg = null)
        assertEquals(listOf("a", "b", "d", "e"), flat.map { it.second.pkg })
        assertEquals(listOf("streaming", "streaming", "apps", "apps"), flat.map { it.first.id })
    }

    @Test fun movingRecentAppStaysVisibleInItsSection() {
        val flat = RibbonOrder.flat(sections, recent = setOf("c"), movePkg = "c")
        assertEquals(listOf("a", "b", "c", "d", "e"), flat.map { it.second.pkg })
    }

    @Test fun stepJumpsOverAppsHiddenBehindRecent() {
        // full order a b c d e, c hidden behind Recent: visible a b d e. Moving b right lands after d.
        val (order, at) = RibbonOrder.step("b", 1, listOf("a", "b", "d", "e"), listOf("a", "b", "c", "d", "e"))!!
        assertEquals(listOf("a", "c", "d", "b", "e"), order)
        assertEquals(3, at)
        val (back, _) = RibbonOrder.step("d", -1, listOf("a", "b", "d", "e"), listOf("a", "b", "c", "d", "e"))!!
        assertEquals(listOf("a", "d", "b", "c", "e"), back)
    }

    @Test fun stepStopsAtTheEnds() {
        assertNull(RibbonOrder.step("a", -1, listOf("a", "b"), listOf("a", "b")))
        assertNull(RibbonOrder.step("b", 1, listOf("a", "b"), listOf("a", "b")))
    }

    // A trimmed Open-Meteo answer (Asia/Kolkata), fetched at 16:30 local time.
    private val sample = """
        {"timezone":"Asia/Kolkata",
         "current":{"time":"2026-10-27T16:30","temperature_2m":29.4,"weather_code":2,"is_day":1,"wind_speed_10m":11.6},
         "hourly":{"time":["2026-10-27T16:00","2026-10-27T17:00","2026-10-27T18:00","2026-10-27T19:00","2026-10-27T20:00","2026-10-27T21:00","2026-10-27T22:00"],
           "temperature_2m":[29.6,28.2,26.1,24.4,23.3,22.8,22.1],"weather_code":[2,0,1,3,61,95,3],
           "precipitation_probability":[0,0,5,10,40,70,20],"is_day":[1,1,0,0,0,0,0]},
         "daily":{"time":["2026-10-26","2026-10-27","2026-10-28"],
           "temperature_2m_max":[30.1,31.2,30.0],"temperature_2m_min":[20.5,21.0,21.4],
           "sunrise":["2026-10-26T06:12","2026-10-27T06:12","2026-10-28T06:13"],
           "sunset":["2026-10-26T18:05","2026-10-27T18:04","2026-10-28T18:04"],
           "uv_index_max":[6.1,6.4,5.9]}}
    """.trimIndent()

    private fun at(s: String) = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("Asia/Kolkata") }.parse(s)!!.time

    @Test fun parsesForecastInThePlacesTimeZone() {
        val now = at("2026-10-27T16:30")
        val f = Weather.parse(Json.parseToJsonElement(sample).jsonObject, aqi = 42, now = now)
        assertEquals(29, f.temp)
        assertEquals(Sky.PARTLY, f.sky)
        assertTrue(f.isDay)
        assertEquals(31, f.high); assertEquals(21, f.low)
        assertEquals(6, f.uv); assertEquals(42, f.aqi); assertEquals(12, f.windKmh)
        // The 16:00 hour still counts as "now" until 16:30; six hours from there.
        assertEquals(6, f.hours.size)
        assertEquals(at("2026-10-27T17:00"), f.hours[0].at)
        assertEquals(Sky.STORM, f.hours[4].sky)
        assertEquals(70, f.hours[4].rainChance)
        assertEquals(3, f.sunrises.size)
    }

    @Test fun arcRunsSunriseToSunsetByDayAndSunsetToSunriseByNight() {
        val f = Weather.parse(Json.parseToJsonElement(sample).jsonObject, aqi = null, now = at("2026-10-27T16:30"))
        val day = arcOf(f, at("2026-10-27T16:30"))!!
        assertTrue(day.day)
        assertEquals(at("2026-10-27T06:12"), day.start)
        assertEquals(at("2026-10-27T18:04"), day.end)
        assertTrue(day.progress in 0.85f..0.89f)

        val night = arcOf(f, at("2026-10-27T20:42"))!!
        assertFalse(night.day)
        assertEquals(at("2026-10-27T18:04"), night.start)
        assertEquals(at("2026-10-28T06:13"), night.end)

        // Before sunrise: the night that started yesterday evening.
        val early = arcOf(f, at("2026-10-27T05:00"))!!
        assertFalse(early.day)
        assertEquals(at("2026-10-26T18:05"), early.start)
    }

    @Test fun weatherCodesAndMoon() {
        assertEquals(Sky.CLEAR, Weather.sky(0))
        assertEquals(Sky.FOG, Weather.sky(48))
        assertEquals(Sky.SNOW, Weather.sky(73))
        assertEquals(Sky.CLOUDY, Weather.sky(1234))
        // 2026-10-26 was full moon (UTC 04:12): nearly fully lit; two weeks later nearly dark.
        val full = at("2026-10-26T09:42")
        assertTrue(Weather.moonLit(full) > 0.97)
        assertTrue(Weather.moonLit(full + 14L * 86_400_000L) < 0.05)
    }

    @Suppress("unused") private fun unused(f: Forecast) = f
}
