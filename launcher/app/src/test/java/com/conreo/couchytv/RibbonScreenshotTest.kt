package com.conreo.couchytv

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.conreo.couchytv.data.AppEntry
import com.conreo.couchytv.data.CategoryCfg
import com.conreo.couchytv.data.Forecast
import com.conreo.couchytv.data.HourCast
import com.conreo.couchytv.data.LauncherConfig
import com.conreo.couchytv.data.NetStatus
import com.conreo.couchytv.data.Sky
import com.conreo.couchytv.ui.ACCENTS
import com.conreo.couchytv.ui.LiteTvTheme
import com.conreo.couchytv.ui.RibbonHome
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the ribbon home the way a Fire TV Stick 4K at 1080p sees it
 * (960×540 dp at xhdpi) with sample apps and weather, into
 * app/build/outputs/roborazzi/. Run: ./gradlew recordRoborazziDebug
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
class RibbonScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun banner(text: String, bg: Long, fg: Long): Bitmap {
        val bmp = Bitmap.createBitmap(320, 180, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(bg.toInt())
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fg.toInt(); textSize = 52f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
        }
        c.drawText(text, 160f, 108f, p)
        return bmp
    }

    private fun app(pkg: String, label: String, bg: Long, fg: Long, cat: String) =
        AppEntry(
            pkg = pkg, label = label, banner = banner(label, bg, fg).asImageBitmap(), icon = null,
            autoCategory = cat, tile = Color(bg), brand = Color(bg), stamp = 0, firstInstall = 0,
        )

    private val apps = listOf(
        app("com.netflix.ninja", "NETFLIX", 0xFF000000, 0xFFE50914, "streaming"),
        app("in.startv.hotstar", "Hotstar", 0xFF0C1445, 0xFFFFFFFF, "streaming"),
        app("org.videolan.vlc", "VLC", 0xFFFF8800, 0xFFFFFFFF, "apps"),
        app("com.amazon.firebat", "prime video", 0xFF0F79AF, 0xFFFFFFFF, "streaming"),
        app("com.google.android.youtube.tv", "YouTube", 0xFFFFFFFF, 0xFFFF0033, "streaming"),
        app("com.spotify.tv.android", "Spotify", 0xFF1DB954, 0xFF000000, "music"),
        app("com.crunchyroll.crunchyroid", "crunchyroll", 0xFFF47521, 0xFFFFFFFF, "streaming"),
        app("com.plexapp.android", "PLEX", 0xFF282A2E, 0xFFE5A00D, "streaming"),
        app("org.xbmc.kodi", "KODI", 0xFF17B2E7, 0xFFFFFFFF, "apps"),
        app("com.sonyliv", "SonyLIV", 0xFF111216, 0xFFF5C518, "apps"),
        app("com.graymatrix.did", "ZEE5", 0xFF8230C6, 0xFFFFFFFF, "apps"),
        app("tv.twitch.android.viewer", "twitch", 0xFF9146FF, 0xFFFFFFFF, "apps"),
    )

    private fun sections(): List<Pair<CategoryCfg, List<AppEntry>>> {
        val pinned = listOf(3, 4, 5, 6, 7).map { apps[it] }
        return listOf(
            CategoryCfg("streaming", "Pinned") to pinned,
            CategoryCfg("apps", "All apps") to apps.filter { it !in pinned },
        )
    }

    private fun forecast(now: Long, day: Boolean): Forecast {
        val h = 3_600_000L
        val hour = now - now % h
        val dayStart = hour - (hour - java.util.Calendar.getInstance().apply {
            timeInMillis = hour; set(java.util.Calendar.HOUR_OF_DAY, 0)
        }.timeInMillis)
        val rise = dayStart + 6 * h + 12 * 60_000L   // 06:12
        val set = dayStart + 18 * h + 4 * 60_000L    // 18:04
        return Forecast(
            place = "Bengaluru", fetchedAt = now, temp = if (day) 29 else 24,
            sky = if (day) Sky.PARTLY else Sky.CLEAR, isDay = day, high = 31, low = 21, windKmh = 12,
            uv = 3, aqi = 42,
            sunrises = listOf(rise, rise + 24 * h), sunsets = listOf(set, set + 24 * h),
            hours = listOf(
                HourCast(now, if (day) 29 else 24, if (day) Sky.PARTLY else Sky.CLEAR, day, 0),
                HourCast(hour + h, 28, Sky.CLEAR, day, 0),
                HourCast(hour + 2 * h, 26, Sky.PARTLY, false, 5),
                HourCast(hour + 3 * h, 24, Sky.CLOUDY, false, 10),
                HourCast(hour + 4 * h, 23, Sky.RAIN, false, 40),
                HourCast(hour + 5 * h, 22, Sky.STORM, false, 70),
            ),
        )
    }

    private fun shot(name: String, day: Boolean, vpn: Boolean, movePkg: String? = null, quick: Boolean = false) {
        val now = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, if (day) 16 else 20)
            set(java.util.Calendar.MINUTE, if (day) 30 else 42)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val config = LauncherConfig(
            layout = 3, setupDone = true,
            recent = listOf("com.netflix.ninja", "in.startv.hotstar", "org.videolan.vlc"),
            weatherPlace = "Bengaluru",
        )
        compose.setContent {
            // A TV remote puts Compose in keyboard input mode; tests start in touch mode,
            // where clickable tiles can't take focus.
            val inputMode = androidx.compose.ui.platform.LocalInputModeManager.current
            androidx.compose.runtime.LaunchedEffect(Unit) {
                @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
                inputMode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard)
            }
            LiteTvTheme(accent = ACCENTS[6]) {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            if (day) listOf(Color(0xFF2B5FA0), Color(0xFF6C9BCB), Color(0xFFF0C08A), Color(0xFF2B2128))
                            else listOf(Color(0xFF070B1A), Color(0xFF14214A), Color(0xFF2B3666), Color(0xFF0C0F1C))
                        )
                    )
                ) {
                    RibbonHome(
                        apps = apps, sections = sections(), config = config, accent = ACCENTS[6],
                        net = NetStatus(wifi = true, vpn = vpn), time = if (day) "16:30" else "20:42",
                        movePkg = movePkg, moveFocus = FocusRequester(),
                        onLaunch = {}, onMenu = {}, onSettings = {}, onNetwork = {}, onVpn = {}, onSetPlace = {},
                        previewForecast = forecast(now, day),
                        previewNow = now,
                    )
                    if (quick) com.conreo.couchytv.ui.QuickPanel(
                        net = NetStatus(wifi = true, vpn = vpn), accent = ACCENTS[6],
                        onDismiss = {}, onLauncherSettings = {}, onNetwork = {},
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_000)
        // captureScreenRoboImage includes dialog windows (the Quick panel).
        @OptIn(com.github.takahirom.roborazzi.ExperimentalRoborazziApi::class)
        com.github.takahirom.roborazzi.captureScreenRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test fun dayVpnOn() = shot("ribbon-day", day = true, vpn = true)

    @Test fun nightVpnOff() = shot("ribbon-night", day = false, vpn = false)

    @Test fun editMode() = shot("ribbon-edit", day = true, vpn = true, movePkg = "com.spotify.tv.android")

    @Test fun quickPanel() = shot("ribbon-quick", day = false, vpn = true, quick = true)

    @Suppress("unused")
    private fun Color.argb() = toArgb()
}
