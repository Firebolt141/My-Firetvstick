package com.conreo.couchytv.data

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/** The Quick panel's view of system state: Private DNS (ad blocking) and ADB. */
object SystemSwitches {
    /** The ad-blocking resolver the debloat scripts use (scripts/dns.sh). */
    const val ADBLOCK_DNS = "dns.adguard-dns.com"

    // Settings.Global keys (public since Android 9 in AOSP, hidden constants in the SDK).
    private const val DNS_MODE = "private_dns_mode"
    private const val DNS_HOST = "private_dns_specifier"

    /** True once `pm grant <pkg> android.permission.WRITE_SECURE_SETTINGS` was run over ADB. */
    fun canWrite(context: Context): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    /** The hostname Private DNS points at, or null when it is off/automatic. */
    fun privateDnsHost(context: Context): String? {
        val cr = context.contentResolver
        if (Settings.Global.getString(cr, DNS_MODE) != "hostname") return null
        return Settings.Global.getString(cr, DNS_HOST)?.takeIf { it.isNotBlank() }
    }

    /** Ad-block DNS on (hostname mode → [ADBLOCK_DNS]) or back to automatic. False if not permitted. */
    fun setAdblockDns(context: Context, on: Boolean): Boolean = runCatching {
        val cr = context.contentResolver
        if (on) {
            Settings.Global.putString(cr, DNS_HOST, ADBLOCK_DNS)
            Settings.Global.putString(cr, DNS_MODE, "hostname")
        } else {
            Settings.Global.putString(cr, DNS_MODE, "opportunistic")
        }
        true
    }.getOrDefault(false)

    /** USB/network debugging is on: anything on the network can reach port 5555. */
    fun adbOn(context: Context): Boolean =
        Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
}
