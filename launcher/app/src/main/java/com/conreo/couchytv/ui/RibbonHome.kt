package com.conreo.couchytv.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.conreo.couchytv.R
import com.conreo.couchytv.data.AppEntry
import com.conreo.couchytv.data.CategoryCfg
import com.conreo.couchytv.data.Forecast
import com.conreo.couchytv.data.LauncherConfig
import com.conreo.couchytv.data.NetStatus
import com.conreo.couchytv.data.Place
import com.conreo.couchytv.data.Sky
import com.conreo.couchytv.data.Weather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/*
 * The slanted ribbon home (layout 3): webOS-style tiles in one continuous strip
 * along the bottom, a 2×2 key grid on its left, and a header with the clock,
 * forecast and a sun/moon arc. Sizes come from a 1280×720 design and are scaled
 * to the real screen with [Units], so every TV draws the same picture.
 */

/** Height of the bottom band: section labels on top, then the tiles (focused and moving tiles grow into it). */
private const val BAND = 268

/** Slant of every tile: the top edge sits this fraction of the height to the right. */
private const val SLANT = 0.12f

/** Design units: 1 unit = 1px of the 1280×720 design, converted to this screen. */
@Immutable
private class Units(val unit: Dp, private val density: Density) {
    fun d(n: Number): Dp = unit * n.toFloat()
    fun s(n: Number): TextUnit = with(density) { d(n).toSp() }
    fun px(n: Number): Float = with(density) { d(n).toPx() }
}

/** A "/"-leaning parallelogram, the shape of every tile and key. */
private class SlantShape(private val k: Float = SLANT) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val off = size.height * k
        return Outline.Generic(Path().apply {
            moveTo(off, 0f); lineTo(size.width, 0f); lineTo(size.width - off, size.height); lineTo(0f, size.height); close()
        })
    }
}

private val TileShape = SlantShape()

/** One tile in the ribbon. [key] is unique even when an app appears twice (Recent and its section). */
@Immutable
private data class RibbonItem(val key: String, val app: AppEntry, val section: String, val firstOfSection: Boolean, val recent: Boolean)

@Composable
fun RibbonHome(
    apps: List<AppEntry>,
    sections: List<Pair<CategoryCfg, List<AppEntry>>>,
    config: LauncherConfig,
    accent: Color,
    net: NetStatus,
    time: String,
    movePkg: String?,
    moveFocus: FocusRequester,
    onLaunch: (AppEntry) -> Unit,
    onMenu: (AppEntry) -> Unit,
    onSettings: () -> Unit,
    onNetwork: () -> Unit,
    onVpn: () -> Unit,
    onSetPlace: (Place?) -> Unit,
    /** Screenshot tests pass a fixed forecast instead of fetching one. */
    previewForecast: Forecast? = null,
) {
    val recentLabel = stringResource(R.string.ribbon_recent)
    // Recent first (not movable), then every section once, in the dock's order
    // (an app in several sections shows in the first), so move mode's flat
    // reordering (moveDock) matches what is on screen.
    val items = remember(apps, sections, config.recent, config.hidden, recentLabel) {
        val byPkg = apps.associateBy { it.pkg }
        val out = ArrayList<RibbonItem>()
        config.recent.asSequence().mapNotNull { byPkg[it] }.filter { it.pkg !in config.hidden }.take(3)
            .forEachIndexed { i, app -> out += RibbonItem("r:${app.pkg}", app, recentLabel, i == 0, true) }
        val seen = HashSet<String>()
        for ((cat, list) in sections) {
            var first = true
            for (app in list) {
                if (!seen.add(app.pkg)) continue
                out += RibbonItem("c:${app.pkg}", app, cat.name, first, false)
                first = false
            }
        }
        out
    }

    var focusedItem by remember { mutableStateOf<RibbonItem?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var showCity by remember { mutableStateOf(false) }

    val locale = androidx.core.os.ConfigurationCompat.getLocales(LocalConfiguration.current).get(0) ?: Locale.getDefault()
    val forecast = previewForecast ?: rememberForecast(config)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val u = Units(maxWidth / 1280f, LocalDensity.current)

        // Readability over any wallpaper: darker at the left and right edges of the header.
        Box(
            Modifier.fillMaxWidth().height(u.d(720 - BAND)).background(
                Brush.horizontalGradient(
                    0f to Color(0xA004060C), 0.42f to Color(0x4004060C), 0.6f to Color(0x1A04060C), 1f to Color(0x6604060C)
                )
            )
        )

        if (movePkg == null) {
            Header(u, config, time, locale, forecast, accent, onSetCity = { showCity = true })
            focusedItem?.let { item ->
                Row(
                    Modifier.offset(u.d(60), u.d(720 - BAND - 56)),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(u.d(14)),
                ) {
                    Text(item.app.label, fontSize = u.s(30), fontWeight = FontWeight.Medium, color = Color.White, style = shadowed, maxLines = 1)
                    Text(
                        "${item.section} · ${stringResource(R.string.ribbon_up_options)}",
                        fontSize = u.s(16), color = Color.White.copy(alpha = 0.88f), style = shadowed,
                        modifier = Modifier.padding(bottom = u.d(4)),
                    )
                }
            }
        } else {
            Box(Modifier.fillMaxWidth().height(u.d(720 - BAND)).background(Color(0xB0101114)))
            Column(
                Modifier.fillMaxWidth().offset(y = u.d(110)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(u.d(16)),
            ) {
                Text(stringResource(R.string.ribbon_edit_title), fontSize = u.s(64), fontWeight = FontWeight.Light, color = Color.White, letterSpacing = u.s(4))
                Text(stringResource(R.string.ribbon_edit_hint), fontSize = u.s(19), color = Color.White.copy(alpha = 0.85f))
            }
        }

        // The band the ribbon sits on.
        Box(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().height(u.d(BAND))
                .background(Color(0xED08090D))
                .drawWithContent { drawContent(); drawLine(Color.White.copy(alpha = 0.08f), Offset.Zero, Offset(size.width, 0f), 1f) }
        )

        KeyGrid(
            u, accent, net,
            modifier = Modifier.align(Alignment.BottomStart),
            onSearch = { showSearch = true },
            onSettings = onSettings,
            onNetwork = onNetwork,
            onVpn = onVpn,
        )

        Ribbon(
            u, items, accent, movePkg, moveFocus,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = u.d(168)).fillMaxWidth().height(u.d(BAND)),
            onFocused = { focusedItem = it },
            onLaunch = onLaunch,
            onMenu = onMenu,
        )
    }

    if (showSearch) SearchDialog(apps.filter { it.pkg !in config.hidden }, onDismiss = { showSearch = false }, onLaunch = { showSearch = false; onLaunch(it) })
    if (showCity) CityDialog(current = config.weatherPlace, onDismiss = { showCity = false }, onPick = { showCity = false; onSetPlace(it) })
}

private val shadowed = TextStyle(shadow = Shadow(Color(0x99000000), Offset(0f, 2f), 14f))

/** Last good forecast for the configured place; refetched every 30 minutes while on screen. */
@Composable
private fun rememberForecast(config: LauncherConfig): Forecast? {
    val place = config.weatherPlace
    val state by produceState(
        initialValue = Weather.cache?.takeIf { it.place == place && place.isNotEmpty() },
        place, config.weatherLat, config.weatherLon, config.celsius,
    ) {
        if (place.isEmpty()) { value = null; return@produceState }
        while (true) {
            val result = runCatching {
                withContext(Dispatchers.IO) { Weather.fetch(place, config.weatherLat, config.weatherLon, config.celsius) }
            }
            result.getOrNull()?.let { value = it }
            delay(if (result.isSuccess) 30 * 60_000L else 2 * 60_000L)
        }
    }
    return state
}

/* ------------------------------ header ------------------------------ */

@Composable
private fun Header(
    u: Units,
    config: LauncherConfig,
    time: String,
    locale: Locale,
    forecast: Forecast?,
    accent: Color,
    onSetCity: () -> Unit,
) {
    // `time` changes every minute, which also refreshes everything derived from now.
    val now = remember(time) { System.currentTimeMillis() }
    val weekday = remember(time, locale) { SimpleDateFormat("EEEE", locale).format(Date(now)).uppercase(locale) }
    val date = remember(time, locale) { SimpleDateFormat("d MMMM", locale).format(Date(now)) }

    Row(Modifier.offset(u.d(60), u.d(48)), verticalAlignment = Alignment.CenterVertically) {
        Text(time, fontSize = u.s(140), fontWeight = FontWeight.ExtraLight, color = Color.White, letterSpacing = u.s(-4), style = shadowed, maxLines = 1)
        Spacer(Modifier.width(u.d(28)))
        Box(Modifier.width(u.d(2)).height(u.d(120)).background(Color.White.copy(alpha = 0.4f)))
        Spacer(Modifier.width(u.d(28)))
        Column(verticalArrangement = Arrangement.spacedBy(u.d(10))) {
            Column {
                Text(weekday, fontSize = u.s(16), fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.85f), letterSpacing = u.s(3))
                Text(date, fontSize = u.s(26), fontWeight = FontWeight.Light, color = Color.White, style = shadowed)
            }
            when {
                forecast != null -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u.d(12))) {
                    SkyIcon(forecast.sky, forecast.isDay, u.d(42))
                    Text("${forecast.temp}°", fontSize = u.s(44), fontWeight = FontWeight.Light, color = Color.White)
                    Column {
                        Text(skyName(forecast.sky, forecast.isDay), fontSize = u.s(17), color = Color.White)
                        Text(stringResource(R.string.ribbon_hilo, forecast.high, forecast.low), fontSize = u.s(15), color = Color.White.copy(alpha = 0.8f))
                    }
                }
                config.weatherPlace.isEmpty() -> Surface(
                    onClick = onSetCity,
                    shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(RoundedCornerShape(u.d(4))),
                    colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
                        containerColor = Color.White.copy(alpha = 0.12f),
                        focusedContainerColor = accent,
                        focusedContentColor = Color(0xFF1A1100),
                    ),
                ) {
                    Text(stringResource(R.string.ribbon_set_city), fontSize = u.s(16), modifier = Modifier.padding(horizontal = u.d(14), vertical = u.d(8)))
                }
                else -> Text(stringResource(R.string.ribbon_weather_loading), fontSize = u.s(16), color = Color.White.copy(alpha = 0.75f))
            }
        }
    }

    if (forecast == null) return

    // Hourly strip.
    val hourFmt = remember(config.h24, locale) { SimpleDateFormat(if (config.h24) "HH:mm" else "h a", locale) }
    Row(Modifier.offset(u.d(60), u.d(226)), horizontalArrangement = Arrangement.spacedBy(u.d(6))) {
        forecast.hours.forEachIndexed { i, h ->
            Column(
                Modifier.width(u.d(74))
                    .background(Color(if (i == 0) 0x8C060912 else 0x61060912))
                    .drawWithContent { drawContent(); if (i == 0) drawRect(accent, size = Size(size.width, u.px(2))) }
                    .padding(vertical = u.d(12)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(u.d(8)),
            ) {
                Text(if (i == 0) stringResource(R.string.ribbon_now) else hourFmt.format(Date(h.at)).lowercase(locale), fontSize = u.s(15), color = Color.White.copy(alpha = 0.88f))
                SkyIcon(h.sky, h.isDay, u.d(26))
                Text("${h.temp}°", fontSize = u.s(20), fontWeight = FontWeight.Medium, color = Color.White)
                Text(h.rainChance?.takeIf { it > 0 }?.let { "$it%" } ?: "·", fontSize = u.s(13), color = Color(0xFFA8D6FF))
            }
        }
    }

    SkyArcPanel(u, forecast, now, config.h24, locale, accent, Modifier.offset(u.d(744), u.d(44)))
}

@Composable
private fun skyName(sky: Sky, day: Boolean): String = stringResource(
    when (sky) {
        Sky.CLEAR -> if (day) R.string.sky_clear else R.string.sky_clear_night
        Sky.PARTLY -> R.string.sky_partly
        Sky.CLOUDY -> R.string.sky_cloudy
        Sky.FOG -> R.string.sky_fog
        Sky.DRIZZLE -> R.string.sky_drizzle
        Sky.RAIN -> R.string.sky_rain
        Sky.SNOW -> R.string.sky_snow
        Sky.STORM -> R.string.sky_storm
    }
)

/** Where the sun (by day) or moon (by night) is between its two horizon events. */
private data class Arc(val day: Boolean, val start: Long, val end: Long, val progress: Float)

private fun arcOf(f: Forecast, now: Long): Arc? {
    val lastRise = f.sunrises.filter { it <= now }.maxOrNull()
    val lastSet = f.sunsets.filter { it <= now }.maxOrNull()
    val day = lastRise != null && (lastSet == null || lastRise > lastSet)
    val start = if (day) lastRise else lastSet
    val end = (if (day) f.sunsets else f.sunrises).filter { it > now }.minOrNull()
    if (start == null || end == null || end <= start) return null
    return Arc(day, start, end, ((now - start).toFloat() / (end - start)).coerceIn(0f, 1f))
}

@Composable
private fun SkyArcPanel(u: Units, f: Forecast, now: Long, h24: Boolean, locale: Locale, accent: Color, modifier: Modifier) {
    val arc = remember(f, now) { arcOf(f, now) } ?: return
    val clock = remember(h24, locale) { SimpleDateFormat(if (h24) "H:mm" else "h:mm a", locale) }
    val left = clock.format(Date(arc.start))
    val right = clock.format(Date(arc.end))
    val mins = ((arc.end - now) / 60_000L).toInt()
    val countdown = if (mins >= 60) stringResource(R.string.ribbon_hm, mins / 60, mins % 60) else stringResource(R.string.ribbon_min, mins)
    val chips = listOf(
        stringResource(R.string.ribbon_rain) to "${f.hours.firstOrNull()?.rainChance ?: 0}%",
        stringResource(R.string.ribbon_wind) to "${f.windKmh} km/h",
        stringResource(R.string.ribbon_air) to (f.aqi?.toString() ?: "–"),
        if (arc.day) stringResource(R.string.ribbon_uv) to (f.uv?.toString() ?: "–")
        else stringResource(R.string.ribbon_moon) to "${(Weather.moonLit(now) * 100).roundToInt()}%",
    )

    Box(modifier.size(u.d(480), u.d(304)).background(Color(0x61060912))) {
        Canvas(Modifier.fillMaxSize()) {
            val p0 = Offset(u.px(36), u.px(196))
            val p1 = Offset(u.px(236), u.px(-104))
            val p2 = Offset(u.px(436), u.px(196))
            val t = arc.progress
            drawLine(Color.White.copy(alpha = 0.25f), Offset(u.px(24), p0.y), Offset(u.px(456), p0.y), u.px(1))
            drawPath(
                Path().apply { moveTo(p0.x, p0.y); quadraticTo(p1.x, p1.y, p2.x, p2.y) },
                Color.White.copy(alpha = 0.5f),
                style = Stroke(u.px(2), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(u.px(3), u.px(8)))),
            )
            val q = p0 + (p1 - p0) * t
            val b = p0 * ((1 - t) * (1 - t)) + p1 * (2 * (1 - t) * t) + p2 * (t * t)
            drawPath(Path().apply { moveTo(p0.x, p0.y); quadraticTo(q.x, q.y, b.x, b.y) }, accent, style = Stroke(u.px(3), cap = StrokeCap.Round))
            drawCircle(Color.White, u.px(4), p0)
            drawCircle(Color.White.copy(alpha = 0.6f), u.px(4), p2)
            drawCircle(if (arc.day) Color(0x52FFD68C) else Color(0x38DCE6FF), u.px(30), b)
            drawCircle(if (arc.day) Color(0xFFFFD27A) else Color(0xFFF2F0E6), u.px(15), b)
        }
        val startName = stringResource(if (arc.day) R.string.ribbon_sunrise else R.string.ribbon_sunset, left)
        val endName = stringResource(if (arc.day) R.string.ribbon_sunset else R.string.ribbon_sunrise, right)
        Text(startName, fontSize = u.s(13), color = Color.White.copy(alpha = 0.88f), modifier = Modifier.offset(u.d(14), u.d(206)))
        Text(endName, fontSize = u.s(13), color = Color.White.copy(alpha = 0.88f), modifier = Modifier.align(Alignment.TopEnd).offset(u.d(-14), u.d(206)))
        Column(Modifier.fillMaxWidth().offset(y = u.d(140)), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(if (arc.day) R.string.ribbon_sunset_in else R.string.ribbon_sunrise_in), fontSize = u.s(13), letterSpacing = u.s(2), color = Color.White.copy(alpha = 0.75f))
            Text(countdown, fontSize = u.s(26), fontWeight = FontWeight.Medium, color = Color(0xFFFFE3B0))
        }
        Row(
            Modifier.align(Alignment.BottomStart).padding(start = u.d(14), end = u.d(14), bottom = u.d(16)).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(u.d(6)),
        ) {
            chips.forEach { (k, v) ->
                Row(
                    Modifier.weight(1f).height(u.d(34)).background(Color.White.copy(alpha = 0.10f)),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(k, fontSize = u.s(14), color = Color.White.copy(alpha = 0.78f))
                    Spacer(Modifier.width(u.d(6)))
                    Text(v, fontSize = u.s(14), fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1)
                }
            }
        }
    }
}

/** Weather glyphs drawn from shapes: no image assets, sharp at any size. */
@Composable
private fun SkyIcon(sky: Sky, day: Boolean, size: Dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val sunColor = Color(0xFFFFD27A)
        val cloudColor = Color(0xFFDCE3EF)
        fun sun(c: Offset, r: Float) {
            drawCircle(sunColor, r, c)
            for (i in 0 until 8) {
                val a = Math.toRadians(i * 45.0)
                val dir = Offset(kotlin.math.cos(a).toFloat(), kotlin.math.sin(a).toFloat())
                drawLine(sunColor, c + dir * (r * 1.35f), c + dir * (r * 1.8f), r * 0.28f, StrokeCap.Round)
            }
        }
        fun moon(c: Offset, r: Float) {
            val disc = Path().apply { addOval(Rect(c, r)) }
            val bite = Path().apply { addOval(Rect(c + Offset(r * 0.55f, -r * 0.35f), r * 0.85f)) }
            drawPath(Path.combine(PathOperation.Difference, disc, bite), Color(0xFFFFE6A6))
        }
        fun cloud(c: Offset, w: Float) {
            drawCircle(cloudColor, w * 0.22f, c + Offset(-w * 0.18f, w * 0.02f))
            drawCircle(cloudColor, w * 0.28f, c + Offset(w * 0.08f, -w * 0.08f))
            drawRoundRect(
                cloudColor, topLeft = c + Offset(-w * 0.42f, 0f), size = Size(w * 0.84f, w * 0.26f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.13f),
            )
        }
        val center = Offset(s / 2, s / 2)
        when (sky) {
            Sky.CLEAR -> if (day) sun(center, s * 0.22f) else moon(center, s * 0.36f)
            Sky.PARTLY -> {
                if (day) sun(Offset(s * 0.36f, s * 0.36f), s * 0.17f) else moon(Offset(s * 0.38f, s * 0.36f), s * 0.24f)
                cloud(Offset(s * 0.56f, s * 0.6f), s * 0.78f)
            }
            Sky.CLOUDY -> cloud(Offset(s * 0.5f, s * 0.5f), s * 0.92f)
            Sky.FOG -> {
                cloud(Offset(s * 0.5f, s * 0.38f), s * 0.86f)
                for (i in 0..1) drawLine(cloudColor, Offset(s * 0.15f, s * (0.78f + i * 0.12f)), Offset(s * 0.85f, s * (0.78f + i * 0.12f)), s * 0.06f, StrokeCap.Round)
            }
            Sky.DRIZZLE, Sky.RAIN, Sky.SNOW, Sky.STORM -> {
                cloud(Offset(s * 0.5f, s * 0.36f), s * 0.9f)
                when (sky) {
                    Sky.SNOW -> for (i in 0..2) drawCircle(Color.White, s * 0.05f, Offset(s * (0.3f + i * 0.2f), s * 0.82f))
                    Sky.STORM -> drawPath(Path().apply {
                        moveTo(s * 0.52f, s * 0.6f); lineTo(s * 0.38f, s * 0.82f); lineTo(s * 0.5f, s * 0.82f)
                        lineTo(s * 0.42f, s); lineTo(s * 0.66f, s * 0.74f); lineTo(s * 0.54f, s * 0.74f); close()
                    }, sunColor)
                    else -> for (i in 0..2) {
                        val x = s * (0.32f + i * 0.18f)
                        drawLine(Color(0xFF8CC8FF), Offset(x, s * 0.7f), Offset(x - s * 0.06f, s * (if (sky == Sky.RAIN) 0.92f else 0.84f)), s * 0.06f, StrokeCap.Round)
                    }
                }
            }
        }
    }
}

/* ------------------------------ key grid ------------------------------ */

@Composable
private fun KeyGrid(
    u: Units,
    accent: Color,
    net: NetStatus,
    modifier: Modifier,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onNetwork: () -> Unit,
    onVpn: () -> Unit,
) {
    val kw = 66; val kh = 56; val gap = 6; val base = 41
    val kOff = kh * SLANT
    // The top row sits one row's slant to the right, so the slanted edges read as one line.
    val shift = (kh + gap) * SLANT
    Box(modifier.size(u.d(170), u.d(BAND))) {
        @Composable
        fun key(col: Int, row: Int, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, bg: Color, edge: Color, onClick: () -> Unit) {
            var focused by remember { mutableStateOf(false) }
            val x = 24 + (if (row == 0) shift else 0f) + col * (kw - kOff + gap)
            val y = BAND - base - kh - (if (row == 0) kh + gap else 0)
            Box(
                Modifier.offset(u.d(x), u.d(y)).size(u.d(kw), u.d(kh))
                    .zIndex(if (focused) 1f else 0f)
                    .graphicsLayer { val sc = if (focused) 1.08f else 1f; scaleX = sc; scaleY = sc }
                    .clip(TileShape).background(bg)
                    .border(if (focused) u.d(3) else u.d(1.5f), if (focused) accent else edge, TileShape)
                    .onFocusChanged { focused = it.isFocused }
                    .semantics { contentDescription = label }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(u.d(22)))
            }
        }
        val plain = Color(0xFF1B1E26)
        val plainEdge = Color.White.copy(alpha = 0.12f)
        key(0, 0, stringResource(R.string.key_search), AppIcons.Search, plain, plainEdge, onSearch)
        key(1, 0, stringResource(R.string.key_settings), AppIcons.Gear, plain, plainEdge, onSettings)
        key(
            0, 1, stringResource(R.string.key_network),
            when { net.ethernet -> AppIcons.Ethernet; net.wifi -> AppIcons.Wifi; else -> AppIcons.WifiOff },
            plain, plainEdge, onNetwork,
        )
        key(
            1, 1, stringResource(if (net.vpn) R.string.key_vpn_on else R.string.key_vpn_off), AppIcons.Vpn,
            if (net.vpn) Color(0xFF1C8A4C) else Color(0xFFB3261E),
            if (net.vpn) Color(0xFF3DDC84) else Color(0xFFFF6B5E),
            onVpn,
        )
    }
}

/* ------------------------------ the ribbon ------------------------------ */

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun Ribbon(
    u: Units,
    items: List<RibbonItem>,
    accent: Color,
    movePkg: String?,
    moveFocus: FocusRequester,
    modifier: Modifier,
    onFocused: (RibbonItem?) -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onMenu: (AppEntry) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val requesters = remember { HashMap<String, FocusRequester>() }
    var focusedKey by remember { mutableStateOf<String?>(null) }

    // Land on the first tile (the app opened last) when the home screen appears.
    LaunchedEffect(items.firstOrNull()?.key) {
        val first = items.firstOrNull()?.key ?: return@LaunchedEffect
        repeat(30) {
            val r = requesters[first]
            if (r != null && runCatching { r.requestFocus() }.isSuccess) return@LaunchedEffect
            withFrameNanos {}
        }
    }

    // Holding Left/Right jumps to the start of the previous/next section.
    fun jump(from: Int, dir: Int) {
        val target = if (dir > 0) (from + 1 until items.size).firstOrNull { items[it].firstOfSection }
        else (from - 1 downTo 0).firstOrNull { items[it].firstOfSection && it < from }
        target ?: return
        scope.launch {
            listState.animateScrollToItem((target - 1).coerceAtLeast(0))
            withFrameNanos {}
            runCatching { requesters[items[target].key]?.requestFocus() }
        }
    }

    // Keep the focused tile about a third in from the left instead of at the edge.
    val pivot = remember {
        object : androidx.compose.foundation.gestures.BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float =
                offset - containerSize * 0.3f
        }
    }
    val fade = remember(u) { Brush.horizontalGradient(0f to Color.Transparent, 1f to Color.Black, startX = 0f, endX = u.px(22)) }

    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.foundation.gestures.LocalBringIntoViewSpec provides pivot) {
        LazyRow(
            state = listState,
            modifier = modifier
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent { drawContent(); drawRect(brush = fade, blendMode = BlendMode.DstIn) },
            contentPadding = PaddingValues(start = u.d(6), end = u.d(420)),
            verticalAlignment = Alignment.Bottom,
        ) {
            items(items.size, key = { items[it].key }) { index ->
                val item = items[index]
                val focused = focusedKey == item.key
                val moving = movePkg == item.app.pkg && !item.recent
                val tileW by animateDpAsState(if (focused || moving) u.d(126) else u.d(108), tween(160), label = "tileW")
                val tileH by animateDpAsState(
                    when { moving -> u.d(256); focused -> u.d(230); else -> u.d(200) }, tween(160), label = "tileH",
                )
                val itemH = u.d(BAND)
                val req = requesters.getOrPut(item.key) { FocusRequester() }
                Box(
                    Modifier
                        .zIndex(if (focused || moving) 1f else 0f)
                        // Report the width minus the slant, so the next tile starts where
                        // this one's slanted edge meets the floor: one continuous strip.
                        .layout { measurable, _ ->
                            val w = tileW.roundToPx()
                            val h = itemH.roundToPx()
                            val off = (tileH.toPx() * SLANT).roundToInt()
                            val p = measurable.measure(Constraints.fixed(w, h))
                            layout(w - off, h) { p.place(0, 0) }
                        }
                ) {
                    if (item.firstOfSection) {
                        Text(
                            item.section.uppercase(),
                            fontSize = u.s(13), fontWeight = FontWeight.SemiBold, letterSpacing = u.s(2),
                            color = if (focusedKey != null && items.firstOrNull { it.key == focusedKey }?.section == item.section) accent else Color.White.copy(alpha = 0.6f),
                            softWrap = false, maxLines = 1,
                            modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true).padding(start = u.d(28), top = u.d(10)),
                        )
                    }
                    Tile(
                        u, item.app, focused, moving, accent,
                        dim = focusedKey != null && !focused,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .size(tileW, tileH)
                            .focusRequester(req)
                            .then(if (moving) Modifier.focusRequester(moveFocus) else Modifier)
                            .onFocusChanged {
                                if (it.isFocused) { focusedKey = item.key; onFocused(item) }
                                else if (focusedKey == item.key) { focusedKey = null; onFocused(null) }
                            }
                            .onPreviewKeyEvent { e ->
                                if (movePkg != null || e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                when {
                                    e.key == Key.DirectionUp -> { onMenu(item.app); true }
                                    e.key == Key.DirectionRight && e.nativeKeyEvent.repeatCount > 0 -> { jump(index, 1); true }
                                    e.key == Key.DirectionLeft && e.nativeKeyEvent.repeatCount > 0 -> { jump(index, -1); true }
                                    else -> false
                                }
                            }
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onLaunch(item.app) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Tile(u: Units, app: AppEntry, focused: Boolean, moving: Boolean, accent: Color, dim: Boolean, modifier: Modifier) {
    Box(
        modifier
            .clip(TileShape)
            .background(app.brand)
            .border(if (focused || moving) u.d(4) else u.d(1.5f), if (focused || moving) accent else Color(0x99000000), TileShape)
            .semantics { contentDescription = app.label },
        contentAlignment = Alignment.Center,
    ) {
        val zoom = if (focused || moving) 1.12f else 1f
        when {
            app.banner != null -> Image(
                bitmap = app.banner,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.width(u.d(84)).aspectRatio(16f / 9f).graphicsLayer { scaleX = zoom; scaleY = zoom },
            )
            app.icon != null -> Image(
                bitmap = app.icon,
                contentDescription = null,
                modifier = Modifier.size(u.d(52)).graphicsLayer { scaleX = zoom; scaleY = zoom },
            )
            else -> Text(app.label, fontSize = u.s(14), color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (dim) Box(Modifier.fillMaxSize().background(Color(0x47000000)))
    }
}

/* ------------------------------ dialogs ------------------------------ */

@Composable
private fun SearchDialog(apps: List<AppEntry>, onDismiss: () -> Unit, onLaunch: (AppEntry) -> Unit) {
    var query by remember { mutableStateOf("") }
    val matches = remember(query, apps) {
        val q = query.trim()
        if (q.isEmpty()) apps.take(8) else apps.filter { it.label.contains(q, ignoreCase = true) }.take(8)
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(6.dp), colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.width(420.dp).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.key_search), style = MaterialTheme.typography.titleMedium)
                val f = remember { FocusRequester() }
                LaunchedEffect(Unit) { repeat(30) { if (runCatching { f.requestFocus() }.isSuccess) return@LaunchedEffect; withFrameNanos {} } }
                Box(Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.08f)).padding(12.dp)) {
                    if (query.isEmpty()) Text(stringResource(R.string.ribbon_search_hint), color = Color.White.copy(alpha = 0.5f), fontSize = 15.sp)
                    BasicTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { matches.firstOrNull()?.let(onLaunch) }),
                        modifier = Modifier.fillMaxWidth().focusRequester(f),
                    )
                }
                if (matches.isEmpty()) Text(stringResource(R.string.ribbon_no_match), color = Color.White.copy(alpha = 0.7f))
                LazyColumn(Modifier.height(320.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(matches.size, key = { matches[it].pkg }) { i ->
                        val app = matches[i]
                        Surface(
                            onClick = { onLaunch(app) },
                            shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(RoundedCornerShape(4.dp)),
                            colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
                                containerColor = Color.Transparent,
                                focusedContainerColor = Color(0xFFF1F2F4),
                                focusedContentColor = Color(0xFF14151A),
                            ),
                            scale = androidx.tv.material3.ClickableSurfaceDefaults.scale(focusedScale = 1f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(56.dp, 32.dp).background(app.brand), contentAlignment = Alignment.Center) {
                                    app.banner?.let { Image(it, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) }
                                        ?: app.icon?.let { Image(it, null, modifier = Modifier.size(26.dp)) }
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** City search for the weather. Also opened from Settings → Display. Null = weather off. */
@Composable
fun CityDialog(current: String, onDismiss: () -> Unit, onPick: (Place?) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val language = Locale.getDefault().language
    fun search() {
        if (query.isBlank() || busy) return
        busy = true; failed = false
        scope.launch {
            val r = runCatching { withContext(Dispatchers.IO) { Weather.searchPlaces(query, language) } }
            results = r.getOrNull(); failed = r.isFailure; busy = false
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(6.dp), colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.width(420.dp).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.city_title), style = MaterialTheme.typography.titleMedium)
                if (current.isNotEmpty()) Text(current, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                val f = remember { FocusRequester() }
                LaunchedEffect(Unit) { repeat(30) { if (runCatching { f.requestFocus() }.isSuccess) return@LaunchedEffect; withFrameNanos {} } }
                Box(Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.08f)).padding(12.dp)) {
                    if (query.isEmpty()) Text(stringResource(R.string.city_hint), color = Color.White.copy(alpha = 0.5f), fontSize = 15.sp)
                    BasicTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { search() }),
                        modifier = Modifier.fillMaxWidth().focusRequester(f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { search() }) { Text(stringResource(R.string.city_search)) }
                    if (current.isNotEmpty()) Button(onClick = { onPick(null) }) { Text(stringResource(R.string.city_off)) }
                }
                when {
                    failed -> Text(stringResource(R.string.city_failed), color = Color(0xFFFF9B8F))
                    results?.isEmpty() == true -> Text(stringResource(R.string.city_none), color = Color.White.copy(alpha = 0.7f))
                }
                results.orEmpty().forEach { place ->
                    Button(onClick = { onPick(place) }, modifier = Modifier.fillMaxWidth()) {
                        Text(place.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
