package com.conreo.couchytv

import android.view.KeyEvent as NativeKeyEvent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyPress
import com.conreo.couchytv.data.AppEntry
import com.conreo.couchytv.data.CategoryCfg
import com.conreo.couchytv.data.LauncherConfig
import com.conreo.couchytv.data.NetStatus
import com.conreo.couchytv.ui.ACCENTS
import com.conreo.couchytv.ui.LiteTvTheme
import com.conreo.couchytv.ui.RibbonHome
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Drives the ribbon with remote keys: where focus starts, Right/Left, Up for
 * the menu, OK to launch, holding Right to jump a section.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
class RibbonInteractionTest {
    @get:Rule val compose = createComposeRule()

    private fun app(pkg: String, label: String) =
        AppEntry(pkg, label, null, null, "apps", Color.DarkGray, Color.DarkGray, 0, 0)

    private val netflix = app("n", "Netflix")
    private val hotstar = app("h", "Hotstar")
    private val prime = app("p", "Prime Video")
    private val youtube = app("y", "YouTube")
    private val kodi = app("k", "Kodi")
    private val apps = listOf(netflix, hotstar, prime, youtube, kodi)

    private val launched = mutableListOf<String>()
    private val menus = mutableListOf<String>()

    private fun show() {
        val sections = listOf(
            CategoryCfg("streaming", "Pinned") to listOf(netflix, prime, youtube),
            CategoryCfg("apps", "All apps") to listOf(hotstar, kodi),
        )
        compose.setContent {
            val inputMode = LocalInputModeManager.current
            LaunchedEffect(Unit) {
                @OptIn(ExperimentalComposeUiApi::class)
                inputMode.requestInputMode(InputMode.Keyboard)
            }
            LiteTvTheme(accent = ACCENTS[6]) {
                RibbonHome(
                    apps = apps, sections = sections,
                    config = LauncherConfig(layout = 3, setupDone = true, recent = listOf("n", "h")),
                    accent = ACCENTS[6], net = NetStatus(wifi = true), time = "16:30",
                    movePkg = null, moveFocus = FocusRequester(),
                    onLaunch = { launched += it.pkg }, onMenu = { menus += it.pkg },
                    onSettings = {}, onNetwork = {}, onVpn = {}, onSetPlace = {},
                )
            }
        }
        compose.waitForIdle()
    }

    private fun key(code: Int, repeat: Int = 0) {
        compose.onRoot().performKeyPress(KeyEvent(NativeKeyEvent(0, 0, NativeKeyEvent.ACTION_DOWN, code, repeat)))
        compose.onRoot().performKeyPress(KeyEvent(NativeKeyEvent(0, 0, NativeKeyEvent.ACTION_UP, code, 0)))
        compose.waitForIdle()
    }

    @Test fun focusStartsOnTheLastOpenedApp() {
        show()
        compose.onNodeWithContentDescription("Netflix").assertIsFocused()
    }

    @Test fun rightMovesAlongTheRibbonAndOkLaunches() {
        show()
        key(NativeKeyEvent.KEYCODE_DPAD_RIGHT)
        compose.onNodeWithContentDescription("Hotstar").assertIsFocused()
        key(NativeKeyEvent.KEYCODE_DPAD_CENTER)
        assertEquals(listOf("h"), launched)
    }

    @Test fun upOpensTheMenuForTheFocusedTile() {
        show()
        key(NativeKeyEvent.KEYCODE_DPAD_UP)
        assertEquals(listOf("n"), menus)
    }

    @Test fun holdingRightJumpsToTheNextSection() {
        show()
        // Recent: Netflix, Hotstar · Pinned: Prime Video, YouTube (Netflix shows once, in Recent) · All apps: Kodi
        key(NativeKeyEvent.KEYCODE_DPAD_RIGHT, repeat = 1)
        compose.onNodeWithContentDescription("Prime Video").assertIsFocused()
        key(NativeKeyEvent.KEYCODE_DPAD_RIGHT, repeat = 1)
        compose.onNodeWithContentDescription("Kodi").assertIsFocused()
        key(NativeKeyEvent.KEYCODE_DPAD_LEFT, repeat = 1)
        compose.onNodeWithContentDescription("Prime Video").assertIsFocused()
    }

    @Test fun recentAppsAreNotRepeatedInTheirSections() {
        show()
        // assertExists fails on duplicates (it requires exactly one match).
        compose.onNodeWithContentDescription("Netflix").assertExists()
        compose.onNodeWithContentDescription("Hotstar").assertExists()
    }
}
