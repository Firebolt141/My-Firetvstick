# HANDOVER: everything about this project, for Claude Code on the user's PC

Read this first, then work from it. It replaces chat history you don't have. Everything in this file has already
been done and verified, so don't redo it. Last updated 2026-10-27.

## 1. Who and what
- **The user:** owns `firebolt141/my-firetvstick`, runs **Windows**, and uses Claude Code in VS Code.
  They are not an Android developer.
  - Explain in plain words.
  - Ask before anything that could break the TV.
  - Push straight to `main` when a round of work is done; they asked for that.
- **The device:** Fire TV Stick 4K / 4K Max (2023), Fire OS 8 (Android 11, API 30), on the same Wi-Fi as the PC.
  The stick's IP is whatever the user gave you during setup. If you don't have it, ask.
- **The goal:** make the stick "truly mine". That means no Amazon ads or junk, and the user's own launcher,
  **Firebolt Home**, instead of Amazon's home screen.

## 2. Current state on the stick (as of today)
- **Firebolt Home is installed and working.** The user calls it "a grand success, mostly smooth".
  - App id: `io.github.firebolt141.home`.
  - Home on Fire (`io.github.toolicious.homeonfire`) has it as its target, so the remote's Home button opens it.
- **Couchy Launcher** (`com.conreo.couchytv`) is still installed as a fallback. Leave it until the user says
  otherwise.
- **Installed APK:** a test build, signed with a **throwaway debug key from a cloud machine that no longer
  exists.** See §6 "Signing": the first build you install will need a one-time uninstall.
- **Debloat:** unknown. It may or may not have been run during setup (`STICK-SETUP-FOR-CLAUDE.md` Part B).
  Check with `TV_HOST=<ip> scripts/reboot-check.sh`, or look at `backup/` and `shots/` on this PC.
- **ADB:** it should be OFF when you aren't working. For any device work, the user turns it back on:
  Settings → My Fire TV → Developer Options.

## 3. Open tasks (in priority order)
### 3.1 Wallpaper problems: ask the user for details first
The user says it "had trouble with wallpapers… I think I got it working but still might have issues". Find out
which mode they used and what went wrong before changing code.

How wallpapers work (code in `launcher/app/src/main/java/com/conreo/couchytv/`):
- **Config fields** (`data/Config.kt`):
  - `wallpaper` (preset gradient index into `WALLPAPERS`);
  - `useCustomWallpaper` (photo);
  - `useVideoWallpaper` with `videoUri`;
  - `useBuiltinAerials` with `builtinSource`;
  - `videoSpeed` and the dimming mode.
- **Drawing** is in `ui/LauncherScreen.kt` (~lines 541–680, search for "Wallpaper + readability scrim").
  - Priority: aerials, then local video, then photo, then preset.
  - The photo is copied to `filesDir/wallpaper.jpg` and decoded at 1280 px, plus a 96 px blurred copy.
- **Picking files** is in `ui/SettingsSheet.kt` (~line 110): `photoPicker` and `videoPicker` use
  `ActivityResultContracts.OpenDocument()`. If that fails, a `toast_no_picker` toast shows.
- **Likely cause #1: Fire OS has no system document picker (DocumentsUI)**, so "Choose photo/video" often
  just shows the toast. It only works if a file-manager app that handles `OPEN_DOCUMENT` is installed.
  - The video mode also needs `takePersistableUriPermission`, which many file managers don't grant. Then the
    video stops working after a reboot.
- **Proposed fix (not built yet)**, so it works with no file picker:
  - Add `scripts/wallpaper.sh push <image|video>`. It copies the file to the stick with
    `cleanyourtv/tools/b64push.py` or `tv.py --push`.
    - Destination: `/sdcard/Firebolt/wallpaper.jpg` (or `.mp4`).
  - Then `pm grant io.github.firebolt141.home android.permission.READ_EXTERNAL_STORAGE`. It's a runtime
    permission on API 30, grantable over ADB. Declare it in the manifest.
  - Make the launcher look at `/sdcard/Firebolt/` first, and add a Settings → Wallpaper row "Use the wallpaper
    pushed from the PC".
  - Also have `launcher.sh setup` grant that permission when `LAUNCHER=firebolt`, and revoke it in `undo`.
- **Aerials** stream Apple's videos from `sylvan.apple.com` (`assets/aerials.json`). They're heavy for a stick
  and fail while the network is still coming up; there's already retry logic.
  - If the user sees a black or gradient screen, check whether AdGuard DNS blocks that host:
    `tv.py "ping -c1 sylvan.apple.com"`.
- **The ribbon draws its own scrims on top of any wallpaper** (`ui/RibbonHome.kt`):
  - a header gradient (~line 204);
  - a night overlay (`0xB0101114`, ~line 230);
  - a nearly opaque band behind the tiles (`0xED08090D`, ~line 244).

  If the user says the wallpaper looks too dark or is hidden, these are the knobs. Check whether Settings'
  dimming mode stacks on top of them.

### 3.2 Remove Couchy branding from the setup wizard and app ("truly make it mine")
The first-run wizard still says "Welcome to Couchy Launcher". It shows a couch logo and Couchy's Google TV and
AOSP "set as default" steps, none of which apply on Fire TV.
- **Code:** `ui/SetupWizard.kt`.
  - Steps: 0 welcome; 1 set default (Google TV carousel or AOSP role request); 2 pick a VPN app; 3 done.
  - The couch logo is `R.drawable.ic_couch`, shown on every step.
- **Strings:** `res/values/strings.xml` lines ~163–192, the `wizard_*` entries and `github_url`. Upstream
  translations (`res/values-xx/strings.xml`) contain the same text.
- **Other Couchy traces:**
  - the settings backup file name `CouchyBackup.json` (`ui/SettingsSheet.kt` ~843);
  - the About screen (`AboutScreen` in `SettingsSheet.kt` ~393), with the upstream GitHub URL and a
    Buy-me-a-coffee QR (`drawable-nodpi/qr_coffee.png`, Cookie font);
  - `ic_couch` also appears in `LauncherScreen.kt` ~1606;
  - `app_icon.xml`, `app_banner.xml` and `splash.xml` under `res/drawable/`. Check them for the couch.
- **Suggested Fire-TV-only wizard.** Same composable, new steps:
  1. **Welcome to Firebolt Home**, with a Firebolt logo.
  2. **Home button check:** say Home on Fire handles it, and show whether it's wired. Read
     `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` for `io.github.toolicious.homeonfire`. If it isn't wired,
     show the one command: `LAUNCHER=firebolt scripts/launcher.sh setup`.
  3. **Pick a VPN app** (keep the existing step 2).
  4. **Weather city** (reuse `CityDialog` from `ui/RibbonHome.kt`).
  5. **Done:** the remote tips from GETTING-STARTED Part 3.

  Drop the Google TV and AOSP branches.
- **Logo:** make a simple vector `ic_firebolt.xml`. A slanted bolt in the amber accent `0xFFFFB547` fits the
  ribbon's "/" look. Use it for the wizard, the app icon and the TV banner (`app_banner.xml` is what Fire TV shows
  in its app row).
- **Licence: keep it.** It's GPLv3: you may rebrand, but **keep the credit**. The About screen should still say
  "Based on Couchy Launcher by conreo (GPLv3)", link to upstream, and point to this repo's source. Removing the
  coffee QR is fine.
- **Translations:** the user said **no translations**. The ribbon strings are English-only already.
  - Removing upstream's `values-xx/strings.xml` too is in the spirit of that. Ask first.
  - If you do, also remove or trim the language picker (`data/LANGUAGES`, `SettingsSheet.kt` ~931,
    `MainActivity.persistLocale`) so it doesn't offer languages that no longer exist.
- **Don't rename the Kotlin package** `com.conreo.couchytv` unless the user asks. It's invisible to them and keeps
  upstream merges easy (`launcher/FORK.md`). The user-visible id is already `io.github.firebolt141.home`.
- **After the change:** bump `versionCode` and `versionName` in `launcher/app/build.gradle.kts`. `versionName` is
  currently `0.1.0-couchy1.0.6`; make it `0.2.0`. Re-run the wizard on the stick: Settings → Launcher → "Run setup
  wizard again", which sets `setupDone=false`. Also fix `wizard_rerun_note`, which wrongly says "Settings > About".

### 3.3 Smaller follow-ups
- **Remove the old Couchy layouts.** The user asked for this during design, but it isn't done in code yet.
  - Settings → Display still offers Carousel, Grid, Dock and Ribbon (`SettingsSheet.kt` ~502–520).
  - Ribbon is the default (`layout = 3`). Make Ribbon the only layout: drop the picker, and force `LAYOUT_RIBBON`
    when loading old configs.
  - The old layout code in `LauncherScreen.kt` can be deleted once nothing reaches it. Check that the tests
    still pass.
- Check the user's notes from the on-stick check list. If the earlier session wrote `ON-DEVICE-REPORT.md`, read it.
  Quick panel rows Bluetooth, Display and Screensaver may not exist on Fire OS; they fall back to Fire TV settings.
- Update `SAVEPOINT.md`: "not yet run on a real Fire TV" is now done.

## 4. Repo map
| Path | What |
|---|---|
| `README.md`, `GETTING-STARTED.md` | Overview and the plain-English human guide (Parts 1–3). |
| `STICK-SETUP-FOR-CLAUDE.md` | The step-by-step install script that was used today. Still valid for reinstalling. |
| `SAVEPOINT.md` | Status snapshot; how to rebuild in a fresh Linux box. |
| `debloat/01..04-*.txt` | 57 packages to disable, from upstream's 4K 2023 list. `com.amazon.imp` is kept on purpose: disabling it blacked out Home on one unit. Batch 02 runs with `HANDLERS_ALLOW=1` (Live TV). |
| `scripts/` | `setup.sh` (Python venv), `backup.sh`, `debloat.sh NN`, `restore.sh [NN]`, `dns.sh on|off|status` (AdGuard Private DNS), `reboot-check.sh`, `close-adb.sh <ip>`, `check-lists.sh` (CI list checks), `launcher.sh status|setup [apk…]|undo [--uninstall]` (`LAUNCHER=couchy|firebolt|<pkg>`), `launcher-keystore.sh`, `build-launcher.sh`. All need `TV_HOST=<ip>`; `common.sh` holds the shared bits. |
| `cleanyourtv/` | Vendored upstream tools (pure-Python ADB `tools/tv.py`, `disable.sh`, `restore.sh`, `b64push.py`, `recovery.py`). Upstream commit in `UPSTREAM_COMMIT`. Don't edit them; wrap them in `scripts/`. |
| `launcher/` | **Firebolt Home**: fork of Couchy Launcher 1.0.6 (upstream `d3dcd70`), Kotlin + Jetpack Compose for TV. `FORK.md` lists changes and the upstream-merge recipe. |
| `.github/workflows/` | `check.yml` (list checks + shellcheck), `launcher-tests.yml` (unit, key-press and screenshot tests + lint on launcher changes), `launcher-apk.yml` (manual signed release build). |
| `backup/`, `shots/`, `cleanyourtv/tools/adbkey`, `venv` | Local only, gitignored. **Never commit.** `adbkey` is a credential. |

## 5. The launcher's code (what's ours vs. upstream's)
All under `launcher/app/src/main/java/com/conreo/couchytv/`.
- **Ours:**
  - `ui/RibbonHome.kt` is the home screen: layout 3, the default.
    - Design units: a 1280×720 design scaled to the screen; `BAND = 268`, `SLANT = 0.12`, `SlantShape`.
    - Header: clock, date, weather, a 6-hour strip, and a sun/moon arc panel (`arcOf`).
    - Bottom left: a 2×2 grid of slanted icon-only keys. Top row Search and Settings (the Settings key opens the
      Quick panel); bottom row Wi-Fi and VPN. VPN is green when connected, red when not.
    - The ribbon is a `LazyRow` of slanted tiles that butt together with no gaps.
      - Order: up to 3 Recent apps first, then sections; each app appears once.
      - The focused tile lifts and widens, and the rest dim.
      - Up opens the app menu; holding Left/Right jumps a section.
      - Edit mode shows ‹ ✕ ›: Left/Right moves, Up hides, OK drops.
    - Also in this file: `SearchDialog` and `CityDialog`.
  - `ui/QuickPanel.kt`: a right-side panel with an ADB warning, an ad-block DNS switch, and settings shortcuts.
  - `data/RibbonOrder.kt`: `recent`, `flat` and `step` (pure functions, tested).
  - `data/Weather.kt`: Open-Meteo forecast, air quality and geocoding (no API key). Times are in the city's time
    zone via SimpleDateFormat; no java.time, because minSdk is 21.
  - `data/SystemSwitches.kt`: Private DNS read/write (needs the `WRITE_SECURE_SETTINGS` grant) and ADB state.
  - `AppRepository.edgeColor`: a tile's brand colour is sampled from its banner's edges.
  - `Config.kt` additions: `recent`, `weatherPlace`, `weatherLat`, `weatherLon`, `celsius`; `layout` defaults to
    3; accent defaults to 6 (amber).
  - `res/values/strings_ribbon.xml`: English only, `tools:ignore="MissingTranslation"`.
- **Upstream Couchy, lightly touched:**
  - `LauncherScreen.kt`: state, key handling, wallpaper, the move logic (`commitFlat`, `moveRibbon`), and the
    `LAYOUT_RIBBON` branch;
  - `SettingsSheet.kt`: Display gained Ribbon mode, weather city and °C/°F;
  - `SetupWizard.kt`, `StatusBar.kt` (not shown in ribbon mode), `AppCard.kt`, `Menus.kt`, `Theme.kt` (amber
    added).
- **Design decisions the user made: don't undo them.**
  - webOS-style slanted "/" tiles in real brand colours.
  - The Recent tile is the same size as the others.
  - No "All apps" button.
  - No black gaps in the ribbon.
  - Bottom-left keys are icon-only and small.
  - The VPN key is "super important".
  - The header combines the forecast strip and the sky arc on a custom wallpaper.
  - The old Couchy layouts are to be removed (§3.3: not done in code yet).
  - No translations.
- **Tests:** `launcher/app/src/test/java/com/conreo/couchytv/`, 17 in total. They run on Robolectric 4.13 +
  Roborazzi 1.32.2 at 960×540 dp xhdpi.
  - `RibbonLogicTest`, `RibbonInteractionTest` (remote keys), `RibbonScreenshotTest` (day, night, edit, quick).
  - Renders land in `app/build/outputs/roborazzi/`. Copy good ones to `launcher/docs/img/` when the look changes.
  - Tests must request `InputMode.Keyboard`: they start in touch mode, where tiles can't take focus.

## 6. Building, signing, installing
### Build on this Windows PC (once)
1. Install the **JDK 17**: `winget install EclipseAdoptium.Temurin.17.JDK`.
2. Install the **Android SDK**. Either install Android Studio, or get the command-line tools for Windows from
   developer.android.com/studio#command-tools. Then run:
   `sdkmanager "platforms;android-34" "build-tools;34.0.0"` and accept the licences.
3. Create `launcher/local.properties` containing `sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk`
   (your actual path). The file is gitignored.
4. In Git Bash, from `launcher/`:
   - `./gradlew recordRoborazziDebug lintRelease` runs the tests, renders and lint. View the renders with Read.
   - `../scripts/build-launcher.sh` builds the APK.
   - Gradle 8.9, compileSdk/targetSdk 34, minSdk 21.
   - If Maven Central rate-limits you (HTTP 429), retry with `--max-workers=1`.

### Signing: decide this with the user before the first new install
- Android only updates an app in place if the new APK has **the same signing key**. The installed test build's key
  is gone, so **the first new build needs one uninstall**:
  `tv.py "pm uninstall io.github.firebolt141.home"`.
  - This resets the launcher's settings: order, hidden apps, weather city, VPN app, wallpaper.
  - Tell the user before doing it.
- Do it once, with a **permanent key**:
  1. Run `scripts/launcher-keystore.sh`. It makes `~/.android/firebolt-home.jks` and prints 4 values. The user
     must keep the `.jks` file and its password safe.
  2. Local builds: set `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS=firebolt-home` and `KEY_PASSWORD` before
     `scripts/build-launcher.sh`.
  3. Optional: add the 4 values as GitHub secrets. Then Actions → launcher-apk → Run workflow builds the same
     signed APK in the cloud.
  - After this, local and CI builds update each other with no uninstall.
- **Install / update:** `TV_HOST=<ip> LAUNCHER=firebolt scripts/launcher.sh setup <apk>`.
  - This also re-grants `WRITE_SECURE_SETTINGS`.
  - After an uninstall and reinstall, re-pick Firebolt Home in Home on Fire if it lost the target.

## 7. How to work with the stick (lessons learned)
- **Shell:** use Git Bash, not PowerShell. Each Bash tool call is a fresh shell, so put `TV_HOST=<ip>` in front
  of every command.
- **The ADB tool:** `cleanyourtv/tools/venv/Scripts/python.exe -I cleanyourtv/tools/tv.py "<shell cmd>"`.
  - `--screenshot shots/x.png [KEY…] [--wait N]` gives you eyes on the TV; open the PNG with Read.
  - `"logcat -d -t 300 *:E"` gets errors.
  - `"logcat -d | grep -i couchytv"` gets the launcher's logs; the process name still contains couchytv.
- **The Home button:** an injected HOME key doesn't prove the redirect works; only the real remote does. Ask the
  user to press it, then check `scripts/launcher.sh status` ("On screen now").
- **Never** disable or uninstall `com.amazon.tv.launcher`; Fire OS protects it, and Home on Fire works on top of
  it. Never use `pm uninstall` for debloat, only the scripts (`pm disable-user`, with undo records).
- **Prompts on the TV:** the ADB "Allow" box times out after 120 s, so tell the user before running commands.
  Scripts skip their keyboard prompts when run from your tool.
- **When done:** ADB Debugging OFF on the TV, restart it, then `scripts/close-adb.sh <ip>` must say **closed**.

## 8. Conventions
- **Small commits** with clear messages; push to `main` once the user is happy with a round.
- **Run before pushing:**
  - `scripts/check-lists.sh` and shellcheck when you touch scripts;
  - the launcher tests and lint when you touch `launcher/`.

  CI runs the same checks.
- **Code style:** match the surrounding code. Short comments explain *why*. Keep Kotlin minSdk-21-safe: use
  `ContextCompat` for permission checks, and no `java.time`.
- **User-facing text:** English only, plain words.
- **Keep the docs current:** update `GETTING-STARTED.md` when the user's steps change, and `launcher/FORK.md`
  when the fork diverges further.
