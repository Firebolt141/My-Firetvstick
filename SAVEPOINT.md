# Savepoint: where Firebolt Home stands (2026-10-27)

Design canvas (private): https://claude.ai/artifact/JJBnJQNc2JzJvGw8aSFPQ4

## Done
- **Debloat** (`debloat/`, `scripts/`): 4 batches, 57 packages. Includes backup, restore, AdGuard DNS, a check after reboot, and closing ADB. Lists are checked in CI.
- **Home redirect**: `scripts/launcher.sh` (status/setup/undo) for Home on Fire plus Couchy, or the fork (`LAUNCHER=firebolt`).
  For the fork, `setup` also grants the permission the Quick panel's DNS switch needs, and `undo` revokes it.
- **Fork** `launcher/` (Couchy 1.0.6, app id `io.github.firebolt141.home`), ribbon home = layout 3, the default:
  - `ui/RibbonHome.kt`: slanted ribbon. Recent comes first, then sections; each app shows once (`data/RibbonOrder.kt`).
    - The focused tile lifts and widens and the rest dim. Up opens the app menu, and holding Left/Right jumps a section.
    - Edit mode: ‹ ✕ › on the lifted tile. Left/Right moves it one visible step, Up hides it, OK drops it.
  - 2×2 slanted keys: Search, Quick panel, Wi-Fi, and VPN (green or red from Android's VPN state).
  - `ui/QuickPanel.kt` + `data/SystemSwitches.kt`:
    - ad-block Private DNS switch (needs the ADB grant above);
    - Wi-Fi, Bluetooth, display, screensaver, home screen settings and Fire TV settings shortcuts;
    - a warning while ADB debugging is on.
  - Header: clock, date, weather, a 6-hour strip, and the sun/moon arc with chips. Data from `data/Weather.kt` (Open-Meteo, no key).
  - Brand tile colours are sampled from each banner's edges.
  - Settings → Display: Ribbon mode, weather city, °C/°F.
  - The ribbon's text is English only (`res/values/strings_ribbon.xml`).
- **Tests** (`launcher/app/src/test`, 17 passing):
  - `RibbonLogicTest`: ordering, the move step past hidden Recent apps, Open-Meteo parsing in the city's time zone, the day/night arc, weather codes, moon.
  - `RibbonInteractionTest`: remote keys. Focus starts on Recent; Right moves; OK launches; Up opens the menu; holding Right/Left jumps sections; no duplicate tiles.
  - `RibbonScreenshotTest`: day, night, edit mode, and Quick panel. Renders are in `launcher/docs/img/ribbon-*.jpg`.
  - CI: `.github/workflows/launcher-tests.yml` runs them plus lint whenever `launcher/` changes, and keeps the renders.
- **Builds**: `lintRelease` + `assembleRelease` pass (2.4 MB APK). The signed build is the manual `launcher-apk` workflow.

## Open
- **Ran on the real stick on 2026-10-27:** installed, Home on Fire points at it, and it mostly works.
- The next work is listed in **HANDOVER.md §3**:
  - wallpaper problems;
  - Firebolt branding for the setup wizard, logo and About screen;
  - removing the old Couchy layouts;
  - a permanent signing key.

## Rebuild locally in a new session
JDK 17+ and Android SDK 34 are needed. In the cloud box this worked:
```sh
mkdir -p ~/android-sdk/cmdline-tools && cd ~/android-sdk
curl -sSfLo clt.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q clt.zip -d cmdline-tools && mv cmdline-tools/cmdline-tools cmdline-tools/latest
yes | cmdline-tools/latest/bin/sdkmanager --licenses; cmdline-tools/latest/bin/sdkmanager "platforms;android-34" "build-tools;34.0.0"
cd <repo>/launcher && echo "sdk.dir=$HOME/android-sdk" > local.properties
./gradlew recordRoborazziDebug lintRelease --max-workers=1   # Maven Central rate-limits (429): just retry
```
