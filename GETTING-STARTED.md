# Getting started: a plain-English guide

This guide walks through everything step by step, with no experience needed. There are three parts. Do them in order:
part 1 cleans up the Fire TV Stick, part 2 replaces Amazon's home screen, and part 3 builds your own launcher (optional).

## What you need
- Your **Fire TV Stick** and its remote.
- A **computer** (Windows, Mac or Linux) on the **same Wi-Fi** as the stick.
- This repo downloaded to the computer. On GitHub, click **Code → Download ZIP** and unzip it.
- **Python 3** on the computer (python.org → Download). On Windows, also install **Git for Windows**
  (it includes "Git Bash"). Type every command below into **Git Bash** (Windows) or **Terminal** (Mac/Linux),
  in the folder you unzipped.

## Step 0: let the computer talk to the stick
1. On the TV, go to **Settings → My Fire TV → About**. Highlight the stick's name and press the **middle button 7 times**.
   A message says you're a developer now.
2. Go **back**, open **Developer Options**, and turn **ADB Debugging: ON**.
3. Find the stick's address: **Settings → My Fire TV → About → Network**. Write down the **IP address**
   (looks like `192.168.1.50`).
4. On the computer, run (put **your** IP in place of the example):
   ```sh
   scripts/setup.sh
   export TV_HOST=192.168.1.50
   ```
   `export` only lasts until you close the window. If you open a new one, run that line again.

## Part 1: clean the stick (removes Amazon junk; everything can be undone)
```sh
scripts/backup.sh          # a box pops up on the TV: choose "Allow". This saves a copy of how things are now.
scripts/debloat.sh 01      # tracking and telemetry
scripts/debloat.sh 02      # ads and sponsored stuff
scripts/debloat.sh 03      # Amazon apps you don't use (Silk, Music, Photos, Kids, Luna)
scripts/debloat.sh 04      # Alexa extras (voice search on the mic button keeps working)
scripts/dns.sh on          # blocks ads on the home screen (not YouTube ads)
scripts/reboot-check.sh    # restarts the stick and checks that everything stayed off
```
After **each** line, look at the TV: the home screen should show and your apps should open.
If something looks broken, undo that step. For example `scripts/restore.sh 02` undoes step 02.
To undo everything: `scripts/restore.sh` and `scripts/dns.sh off`.

Want to keep an app? Before running its batch, open the file in `debloat/` and put a `#` at the start of that app's line.

## Part 2: your own home screen (Couchy + Home on Fire)
Amazon doesn't let other apps *be* the home screen. **Home on Fire** works around that: when you press Home, it opens your launcher instead.
1. On the computer, download two files:
   - **Couchy Launcher**: https://f-droid.org/packages/com.conreo.couchytv/ → "Download APK".
   - **Home on Fire**: https://github.com/toolicious/home-on-fire/releases → the `.apk` under the newest release.
2. Put both files in this folder and run (use the real file names):
   ```sh
   scripts/launcher.sh setup couchy.apk home-on-fire.apk
   ```
3. Home on Fire opens on the TV. Choose **"Choose target app…" → Couchy Launcher**.
4. Press the **Home button on the real remote**, then press Enter on the computer. The script checks it worked.

Later, `scripts/launcher.sh status` shows whether everything is still set up. If a Fire TV update ever sends Home back to
Amazon, run `scripts/launcher.sh setup` again.

## Part 3 (optional): Firebolt Home, our own launcher with the slanted ribbon
It's a version of Couchy with the design from the canvas. It's **new and not yet tested on a real stick**, so keep Couchy as a backup.

**One-time setup: a signing key.** Android only accepts updates signed with the same key.
1. Install a JDK on the computer (adoptium.net → Temurin 17).
2. Run `scripts/launcher-keystore.sh`, pick a password, and keep the `.jks` file it makes somewhere safe.
3. On GitHub, open your repo → **Settings → Secrets and variables → Actions → New repository secret**, and add the
   four values the script printed.

**Build it**
1. On GitHub, open **Actions → launcher-apk → Run workflow**. Wait about 5–10 minutes for a green tick.
2. Open the finished run and download **firebolt-home-apk**. Unzip it to get `firebolt-home-….apk`.

**Install it**
```sh
LAUNCHER=firebolt scripts/launcher.sh setup firebolt-home-0.1.0-couchy1.0.6.apk
```
In Home on Fire, choose **Firebolt Home** as the target. The same command also gives the launcher the one-time
permission its ad-block switch needs.

**Using it with the remote**
- **Left / Right** moves along the ribbon. **Hold Right** (or Left) jumps a whole section.
- **OK** opens the app. The apps you opened last appear first, under **RECENT**.
- **Up** on a tile opens its options: Open, Move, Hide, App info, Uninstall.
- **Moving a tile:** choose **Move**. The tile lifts and shows ‹ ✕ ›.
  - **Left / Right** carries it along the ribbon.
  - **OK** puts it down.
  - **Up** takes it off the ribbon. It's hidden, not uninstalled; bring it back in Settings → Apps.
- **The four keys at the bottom left:**
  - 🔍 **Search:** type part of an app's name.
  - ⚙ **Quick panel:** ad-block DNS on/off, Wi-Fi, Bluetooth, display and sound, start the screensaver,
    home screen settings and Fire TV settings. It also warns you while ADB debugging is still on.
  - **Wi-Fi:** opens network settings.
  - **VPN:** green when connected, red when not. Pressing it opens your VPN app; pick which app in
    Quick panel → Home screen settings → Status bar.
- **Weather:** press **Set your city for weather** at the top, or go to Quick panel → Home screen settings → Display →
  Weather city. Type your city, then pick it from the list. °C/°F is in the same place.
- **Sections:** the group names (Streaming, Music, Apps…) are Couchy's sections. Rename them, or move apps between
  them, in Home screen settings → Sections. For example, name the first one "Pinned".

**A 2-minute check on the stick** (things only a real remote can prove)
1. Press **Home**: Firebolt Home appears and the first RECENT tile is highlighted.
2. Press Right a few times, then hold Right: it jumps to the next section.
3. Press Up on a tile: the options menu opens. Choose Move, press Right twice, then OK. The tile stays where you put it.
4. Open the Quick panel. With ADB still on, the yellow warning shows. Flip Ad-block DNS on, then off.
5. Turn your VPN on and off: the bottom-left key turns green, then red.
6. Set your city: the forecast and the sun or moon arc appear within a few seconds.

If any step misbehaves, Couchy is still installed: set it back as Home on Fire's target and tell me what happened.

Don't like it? `LAUNCHER=firebolt scripts/launcher.sh undo --uninstall`, then set Couchy as Home on Fire's target again.

## Last step: lock the door
ADB lets anything on your Wi-Fi control the stick, so turn it off when you're done:
**Settings → My Fire TV → Developer Options → ADB Debugging: OFF**, restart the stick, then run
`scripts/close-adb.sh 192.168.1.50`. It should say the port is **closed**.

## If something goes wrong
- **"connection refused" / nothing happens:** check ADB Debugging is ON and the IP is right. The stick and computer must be on the same Wi-Fi.
- **The "Allow" box never appeared:** wake the TV and run the command again.
- **Home screen black after part 1:** `scripts/restore.sh 02`.
- More help: `cleanyourtv/docs/TROUBLESHOOTING.md` and `README.md`.
