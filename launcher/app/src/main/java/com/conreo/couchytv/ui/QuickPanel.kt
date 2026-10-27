package com.conreo.couchytv.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.conreo.couchytv.Actions
import com.conreo.couchytv.R
import com.conreo.couchytv.data.NetStatus
import com.conreo.couchytv.data.SystemSwitches

/**
 * The ribbon's Settings key: system shortcuts, the ad-block DNS switch and a
 * warning while ADB debugging is on. Everything else stays in the launcher's
 * settings sheet and Fire TV's own settings, one press away from here.
 */
@Composable
fun QuickPanel(
    net: NetStatus,
    accent: Color,
    onDismiss: () -> Unit,
    onLauncherSettings: () -> Unit,
    onNetwork: () -> Unit,
) {
    val context = LocalContext.current
    // Re-read system state after each change (and on open).
    var tick by remember { mutableIntStateOf(0) }
    val dnsHost = remember(tick) { SystemSwitches.privateDnsHost(context) }
    val canWrite = remember(tick) { SystemSwitches.canWrite(context) }
    val adbOn = remember(tick) { SystemSwitches.adbOn(context) }
    var dnsFailed by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
            Column(
                Modifier
                    .padding(24.dp)
                    .width(400.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF161A24))
                    .border(1.dp, Color.White.copy(alpha = 0.12f))
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text(stringResource(R.string.quick_title), fontSize = 24.sp, fontWeight = FontWeight.Medium, color = Color.White)
                    Text(stringResource(R.string.quick_back), fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                }

                if (adbOn) {
                    Column(
                        Modifier.fillMaxWidth().background(Color(0xFF3A2A12)).border(1.dp, Color(0xFF8A6420)).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(stringResource(R.string.quick_adb_title), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFFE2B0))
                        Text(stringResource(R.string.quick_adb_body), fontSize = 14.sp, color = Color(0xFFF3DDB8))
                        PanelButton(stringResource(R.string.quick_adb_open), Color(0xFFFFC46B), Color(0xFF2A1A00)) { openDeveloperOptions(context) }
                    }
                }

                val first = remember { FocusRequester() }
                LaunchedEffect(Unit) { repeat(30) { if (runCatching { first.requestFocus() }.isSuccess) return@LaunchedEffect; withFrameNanos {} } }

                PanelRow(
                    title = stringResource(R.string.quick_dns),
                    sub = when {
                        dnsFailed || !canWrite -> stringResource(R.string.quick_dns_setup)
                        dnsHost != null -> stringResource(R.string.quick_dns_on, dnsHost)
                        else -> stringResource(R.string.quick_dns_off)
                    },
                    accent = accent,
                    switch = dnsHost != null,
                    modifier = Modifier.focusRequester(first),
                ) {
                    dnsFailed = !SystemSwitches.setAdblockDns(context, dnsHost == null)
                    tick++
                }
                PanelRow(
                    stringResource(R.string.quick_network),
                    stringResource(
                        when { net.ethernet -> R.string.quick_net_ethernet; net.wifi -> R.string.quick_net_wifi; else -> R.string.quick_net_offline }
                    ),
                    accent,
                ) { onNetwork() }
                PanelRow(stringResource(R.string.quick_bluetooth), stringResource(R.string.quick_bluetooth_sub), accent) {
                    open(context, Settings.ACTION_BLUETOOTH_SETTINGS)
                }
                PanelRow(stringResource(R.string.quick_display), stringResource(R.string.quick_display_sub), accent) {
                    open(context, Settings.ACTION_DISPLAY_SETTINGS)
                }
                PanelRow(stringResource(R.string.quick_screensaver), stringResource(R.string.quick_screensaver_sub), accent) {
                    startScreensaver(context)
                }
                PanelRow(stringResource(R.string.quick_launcher), stringResource(R.string.quick_launcher_sub), accent) { onLauncherSettings() }
                PanelRow(stringResource(R.string.quick_fire_settings), stringResource(R.string.quick_fire_settings_sub), accent) {
                    Actions.openSystemSettings(context)
                }
            }
        }
    }
}

@Composable
private fun PanelRow(
    title: String,
    sub: String,
    accent: Color,
    switch: Boolean? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(4.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.05f),
            contentColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.16f),
            focusedContentColor = Color.White,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = androidx.tv.material3.Border(androidx.compose.foundation.BorderStroke(2.dp, accent)),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        modifier = modifier.fillMaxWidth().heightIn(min = 60.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Text(sub, fontSize = 13.sp, color = Color.White.copy(alpha = 0.72f))
            }
            if (switch != null) {
                Box(
                    Modifier.size(48.dp, 28.dp).background(if (switch) accent else Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp)),
                    contentAlignment = if (switch) Alignment.CenterEnd else Alignment.CenterStart,
                ) {
                    Box(Modifier.padding(3.dp).size(22.dp).background(Color.White, RoundedCornerShape(11.dp)))
                }
            }
        }
    }
}

@Composable
private fun PanelButton(text: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(18.dp)),
        colors = ClickableSurfaceDefaults.colors(containerColor = bg, contentColor = fg, focusedContainerColor = Color.White, focusedContentColor = fg),
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
    }
}

private fun open(context: Context, action: String) {
    runCatching { context.startActivity(Intent(action)) }.onFailure { Actions.openSystemSettings(context) }
}

private fun openDeveloperOptions(context: Context) {
    runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
        .onFailure { Actions.openSystemSettings(context) }
}

/** Starts the screensaver now (AOSP's dream starter); falls back to the screensaver settings. */
private fun startScreensaver(context: Context) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_MAIN).setClassName("com.android.systemui", "com.android.systemui.Somnambulator"))
    }.onFailure { open(context, Settings.ACTION_DREAM_SETTINGS) }
}
