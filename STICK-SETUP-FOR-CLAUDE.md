# Hand-off: put Firebolt Home on the user's Fire TV Stick (for Claude Code on Windows)

You are Claude Code running in VS Code on the user's **Windows** PC. The user is sitting at the TV with the remote.
Your job is to drive this repo's scripts and guide the user through the steps only a person can do.
Do it **one step at a time**, and check the result before moving on.

## What is already true
- **Stick:** Fire TV Stick 4K / 4K Max (2023), Fire OS 8. Expected model `AFTKM`, `AFTKRT` or `AFTMA08C15`.
- **Already set up:** Couchy Launcher (`com.conreo.couchytv`) and Home on Fire (`io.github.toolicious.homeonfire`).
  Pressing Home on the remote opens Couchy today. Don't break that: Couchy is the fallback.
- **Debloat not done yet.** It's optional (Part B). Ask the user before starting it.
- **The user's choices for debloat:** keep Alexa voice, Prime Video and casting/mirroring. Disable Silk.
  Use AdGuard DNS.
- **The APK:** the user has a test build called `firebolt-home.apk` (app id `io.github.firebolt141.home`).
  - It should be in the repo root. If it isn't, ask the user where they saved it.
  - It's signed with a throwaway test key. A GitHub Actions build (`launcher-apk` workflow) uses a different key,
    so that build will later need `pm uninstall io.github.firebolt141.home` first. Tell the user this; don't do it now.
- **Background reading if you need it:** `GETTING-STARTED.md` is the human guide, `SAVEPOINT.md` is the project
  status, and `launcher/FORK.md` describes the launcher.

## Ground rules
- **Never uninstall or disable anything** outside the scripts below. The scripts disable with `pm disable-user`
  and keep undo records. Never touch `com.amazon.tv.launcher`.
- **Before any step that pops something up on the TV or needs the remote, tell the user first** what they'll see
  and what to press. The ADB "Allow" prompt times out after 120 s.
- **Each Bash call is a fresh shell, so `export` doesn't carry over.** Put `TV_HOST=<ip>` in front of every
  command, for example `TV_HOST=192.168.1.50 scripts/launcher.sh status`.
- **Use Git Bash, never PowerShell or cmd.** Claude Code on Windows uses Git Bash for its Bash tool, and needs
  Git for Windows installed. Run commands from the repo root.
- **Scripts that read from the keyboard skip their questions** when run from your tool. In particular,
  `launcher.sh setup` skips its "press Home, then Enter" check. Do that check yourself (step A4).
- **You can see the TV.** This saves a screenshot without pressing any key:
  ```sh
  TV_HOST=<ip> cleanyourtv/tools/venv/Scripts/python.exe -I cleanyourtv/tools/tv.py --screenshot shots/now.png
  ```
  Then open `shots/now.png` with Read to look at it. Don't use an injected `HOME` key to test the Home redirect:
  only the real remote button proves it works.
- **If something fails, stop.** Show the user the exact output, then use the troubleshooting table at the end.
  Don't improvise device commands.

## Step 0: prerequisites (check them; ask the user only for what's missing)
1. Run `python --version` (or `py --version`). It should be 3.8 or newer.
   - If neither works: ask the user to install Python from python.org, tick **"Add python.exe to PATH"**,
     then restart VS Code.
2. **On the TV**, the user must:
   - turn **Settings → My Fire TV → Developer Options → ADB Debugging** ON. If Developer Options is missing:
     go to **About**, highlight the stick's name, press OK 7 times, then go back.
   - read the IP address from **Settings → My Fire TV → About → Network**. Ask the user for it.
3. Run `scripts/setup.sh`. It makes `cleanyourtv/tools/venv` (Windows layout, `venv/Scripts/python.exe`) and
   should end with `Ready.`
4. Tell the user: *"An 'Allow USB debugging?' box will appear on the TV. Tick 'Always allow from this
   computer' and choose Allow."* Then run `TV_HOST=<ip> scripts/backup.sh`.
   - Expected output: `Model: AFTKM…` (or AFTKRT / AFTMA08C15) and `Backup saved in backup/<date>`.
   - A different model is only a warning, but tell the user and **skip Part B**: the debloat lists are written
     for this model.

## Part A: install Firebolt Home and point Home on Fire at it
1. Run `TV_HOST=<ip> scripts/launcher.sh status`. It should show Couchy and Home on Fire installed and the
   redirect wired up. This is the baseline.
2. Run `TV_HOST=<ip> LAUNCHER=firebolt scripts/launcher.sh setup firebolt-home.apk`.
   - It installs the APK through `pm install -r`.
   - It grants `WRITE_SECURE_SETTINGS` to Home on Fire and Firebolt Home. Firebolt Home needs it for the Quick
     panel's ad-block DNS switch.
   - It keeps the accessibility service on.
   - Finally it opens Home on Fire on the TV.
   - If you see `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: an older copy signed with another key is on the stick.
     Ask the user before running `pm uninstall io.github.firebolt141.home` with `tv.py`, then repeat this step.
3. Tell the user: *"Home on Fire is open on the TV. Choose 'Choose target app…', pick **Firebolt Home**, and
   leave 'Launch on boot' on."*
4. Ask the user to **press the real Home button on the remote** and say when they have. Then check:
   - run `TV_HOST=<ip> LAUNCHER=firebolt scripts/launcher.sh status`; "On screen now" should start with
     `io.github.firebolt141.home/`.
   - take a screenshot and look at it. You should see a clock and date at top left, weather or a "Set your city"
     prompt, a slanted ribbon of app tiles, and four slanted keys at bottom left.
5. Walk the user through the 2-minute on-stick check, **one item at a time**, and note the result of each:
   1. Home shows Firebolt Home, with the first tile (under RECENT) highlighted.
   2. Right moves along the ribbon. **Holding** Right jumps to the next section.
   3. Up on a tile opens its menu. Choose Move, press Right twice, then OK: the tile stays where it was put.
   4. Bottom-left ⚙ opens the Quick panel. A yellow ADB warning shows while ADB is on.
      Flip "Ad-block DNS" on, then off.
   5. VPN on/off makes the bottom-left VPN key green, then red. Pressing it opens their VPN app; the VPN app is
      chosen in Quick panel → Home screen settings → Status bar.
   6. Set a city ("Set your city for weather" at the top, or Home screen settings → Display → Weather city).
      Weather, the 6-hour strip and the sun/moon arc appear within a few seconds.
   7. Also try the Quick panel rows Bluetooth, Display and Screensaver. Fire OS may lack some Android settings
      screens; those rows should fall back to Fire TV settings rather than doing nothing.
6. **If a check fails:**
   - Note exactly what happened, and take a screenshot.
   - Grab the log for the crash or problem:
     ```sh
     TV_HOST=<ip> cleanyourtv/tools/venv/Scripts/python.exe -I cleanyourtv/tools/tv.py "logcat -d -t 300 *:E"
     ```
   - Put the findings in a new file `ON-DEVICE-REPORT.md` in the repo root: what passed, what failed, and the log
     lines.
   - The user can switch back to Couchy any time: run `TV_HOST=<ip> scripts/launcher.sh setup` (no APK) and
     pick Couchy Launcher as Home on Fire's target.
   - Don't try to fix the launcher's Kotlin code on this PC. It needs a JDK and the Android SDK. The report goes
     back to the cloud session.

## Part B (optional; ask first): debloat
Run **one batch per step**. After each one, look at the screenshot the script saves in `shots/`, **and** ask the
user to confirm on the TV that Home draws, the remote works, and Prime Video opens.
```sh
TV_HOST=<ip> scripts/debloat.sh 01   # telemetry
TV_HOST=<ip> scripts/debloat.sh 02   # ads / sponsored (includes Live tab; com.amazon.imp is deliberately kept)
TV_HOST=<ip> scripts/debloat.sh 03   # unused Amazon apps incl. Silk
TV_HOST=<ip> scripts/debloat.sh 04   # Alexa extras (mic-button voice search keeps working)
TV_HOST=<ip> scripts/dns.sh on       # AdGuard Private DNS
TV_HOST=<ip> scripts/reboot-check.sh # reboots, then checks everything stayed off and Home still redirects
```
- If a batch breaks something, run `TV_HOST=<ip> scripts/restore.sh <NN>` straight away, and tell the user.
- To undo everything: `scripts/restore.sh` and `scripts/dns.sh off`.
- To keep an app, put a `#` at the start of its line in `debloat/NN-*.txt` before running that batch.
  Ask the user if they're unsure about anything in a list.
- After `reboot-check.sh`, if Home goes back to Amazon, re-run Part A step 2 without the APK:
  `LAUNCHER=firebolt scripts/launcher.sh setup`.

## Last step: close ADB (always)
1. Tell the user to turn **Developer Options → ADB Debugging OFF** and restart the stick.
2. Then run `scripts/close-adb.sh <ip>`. It must say the port is **closed**.
3. Remind the user that the `backup/` folder and `cleanyourtv/tools/adbkey` stay on this PC. They're gitignored;
   don't commit them.

## Troubleshooting
| Symptom | Fix |
|---|---|
| `connection refused` / timeout | ADB Debugging off, wrong IP, or PC and stick on different networks or Wi-Fi bands with client isolation. Re-check the IP. |
| Auth timeout, no "Allow" box | Wake the TV (press a button) and run again. If "Always allow" was ticked earlier, there's no box at all. |
| `Run scripts/setup.sh first` | The venv is missing; run `scripts/setup.sh`. |
| `setup.sh` says to install Python | It found only the Microsoft Store stub. Install from python.org with PATH ticked, restart VS Code. |
| Home opens Amazon after a reboot | Wait 10–20 s for Home on Fire's service. If still Amazon: `LAUNCHER=firebolt scripts/launcher.sh setup`. |
| Home screen black after debloat | `scripts/restore.sh 02` (then whichever batch was last). |
| Anything else | `cleanyourtv/docs/TROUBLESHOOTING.md`, then stop and ask the user. |

When you're done, give the user a short summary:
- what was installed;
- which checks passed or failed;
- whether debloat ran;
- that ADB is closed.

If `ON-DEVICE-REPORT.md` exists, tell the user to paste it into the cloud session so the launcher can be fixed.
