# Savepoint: where Firebolt Home stopped (2026-10-27)

Resume from here. Design canvas (private): https://claude.ai/artifact/JJBnJQNc2JzJvGw8aSFPQ4

## Done
- **Debloat** (`debloat/`, `scripts/`): 4 batches, 57 packages. Includes backup, restore, AdGuard DNS, a check after reboot, and closing ADB. Lists are checked in CI.
- **Home redirect**: `scripts/launcher.sh` (status/setup/undo) for Home on Fire plus Couchy, or the fork (`LAUNCHER=firebolt`).
- **Fork** `launcher/` (Couchy 1.0.6, app id `io.github.firebolt141.home`), with the new **ribbon home** as layout 3, the default:
  - `ui/RibbonHome.kt`: slanted ribbon (Recent, then sections, one continuous strip). The focused tile lifts and widens and the rest dim. Up opens the app menu, holding Left/Right jumps a section, and move mode reorders.
  - 2×2 slanted keys: Search, Settings, Wi-Fi, VPN. The VPN key is green or red from Android's VPN state.
  - Header: clock, date, weather, a 6-hour strip, and the sun/moon arc with rain/wind/air/UV (or moon) chips.
  - `data/Weather.kt`: Open-Meteo (no key). City search is in Settings → Display, or on the "Set your city" button.
  - Brand tile colour is sampled from each banner's edges (`AppRepository.edgeColor`).
  - Settings → Display: "Ribbon" mode, weather city, °C/°F.
- **Build status**: `./gradlew assembleDebug` **compiled successfully** in the cloud session (APK not committed). It has **not been run on a real Fire TV yet.**

## Not finished / next steps
1. **Screenshot test** `app/src/test/.../RibbonScreenshotTest.kt` (Roborazzi + Robolectric) was added but **never ran**. The session stopped while it downloaded dependencies. Run `./gradlew recordRoborazziDebug` and look at `app/build/outputs/roborazzi/*.png`. If the new test plugins break the build, remove the roborazzi lines from both `build.gradle.kts` files.
2. Install on the stick and check:
   - focus starts on the Recent tile;
   - Up opens the menu;
   - holding Right jumps;
   - edit mode works;
   - the VPN colour is right;
   - the weather loads.
3. Edit-mode extras from the design (an × above the lifted tile, arrows) are not built; Couchy's own move mode is used.
4. Quick panel (ad-block DNS switch, ADB warning) is not built; the Settings key opens Couchy's settings.
5. Polish: translations of `strings_ribbon.xml`, picking a name, accent choice (amber default).

## Rebuild locally in a new session
JDK 17+ and Android SDK 34 are needed. In the cloud box this worked:
```sh
mkdir -p ~/android-sdk/cmdline-tools && cd ~/android-sdk
curl -sSfLo clt.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q clt.zip -d cmdline-tools && mv cmdline-tools/cmdline-tools cmdline-tools/latest
yes | cmdline-tools/latest/bin/sdkmanager --licenses; cmdline-tools/latest/bin/sdkmanager "platforms;android-34" "build-tools;34.0.0"
cd <repo>/launcher && echo "sdk.dir=$HOME/android-sdk" > local.properties
./gradlew assembleDebug --max-workers=1   # Maven Central rate-limits (429): just retry
```
