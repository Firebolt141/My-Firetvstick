# My Fire TV Stick

A debloat of my **Fire TV Stick 4K / 4K Max (2nd gen, 2023)** running Fire OS 8 (Android 11). It's built on
[cleanyourtv](https://github.com/Gathin23/cleanyourtv) and uses its tested
[Fire TV Stick 4K 2023 list](cleanyourtv/examples/amazon-fire-tv-stick-4k-2023/debloat.txt) along with its
safety-checked `disable.sh` / `restore.sh`.

**What changes:** 57 Amazon packages get **disabled, not uninstalled**. They cover telemetry, content recognition (ACR),
ads and sponsored previews, shopping, the Live tab, promos and tutorials, Silk, Music, Photos, Kids and Luna, plus the
Alexa extras. Ad-blocking DNS (AdGuard) is also turned on. All of it can be undone.

**What stays:** the Fire TV home screen (Fire OS doesn't let a third-party launcher take Home), Appstore,
Settings, keyboard, remote and Bluetooth, **Alexa voice search** on the mic button, **Prime Video**,
**casting and screen mirroring**, firmware updates and the screensaver.

## Layout

| Path | What |
|---|---|
| `debloat/01-telemetry.txt` | Batch 1: metrics, logging, ACR, Wi-Fi password sync, presence sensing |
| `debloat/02-ads.txt` | Batch 2: ad sync, sponsored previews, ad ID, recommendations, shopping, Live tab, promos |
| `debloat/03-preloads.txt` | Batch 3: Silk, Luna, Music, Photos, Kids, tutorials, setup helpers, Ambient |
| `debloat/04-alexa-extras.txt` | Batch 4: Alexa alarms, timers, music, calling, smart-home dashboard (voice search stays) |
| `scripts/` | The wrappers you run (below) |
| `cleanyourtv/` | Files vendored from upstream: `tools/`, the reference list, TROUBLESHOOTING, LICENSE (MIT), `UPSTREAM_COMMIT` |

To keep an app, put a `#` in front of its line before you run that batch.

## Run it

You need a computer on the same Wi-Fi as the stick, with `bash` and `python3` (macOS, Linux, or Git Bash/WSL on
Windows). No `adb` binary is needed because the tools use a pure-Python ADB client.

**1. Turn on ADB on the stick.** Go to Settings → My Fire TV → About and press Select on the device name 7 times.
Go back, then open Developer Options → **ADB Debugging: ON**. You'll find the IP under Settings → My Fire TV → About →
Network.

**2. Set up once and back up.**
```sh
scripts/setup.sh
export TV_HOST=192.168.1.50          # your stick's IP
scripts/backup.sh                    # accept "Allow USB debugging?" on the TV
```
This saves the packages, getprop and settings to `backup/<timestamp>/`, and warns you if the model isn't AFTKM or AFTKRT.

**3. Debloat one batch at a time.** After each batch, look at the TV: Home should draw, the remote should work, and
Prime Video and your apps should open.
```sh
scripts/debloat.sh 01
scripts/debloat.sh 02
scripts/debloat.sh 03
scripts/debloat.sh 04
```
Each run writes an undo record to `~/.cleanyourtv/` and saves a screenshot to `shots/`. A screenshot can come back
black or identical to the last one on Fire OS 8, so trust the TV over the PNG.
If **Home goes black after batch 02**, run `scripts/restore.sh 02`. It recovers without a reboot (`cleanyourtv`
saw this once on a 4K Max).

**4. Ad-blocking DNS.**
```sh
scripts/dns.sh on        # Private DNS -> dns.adguard-dns.com
```
This blocks the hero banner and "Sponsored" row that the debloat can't reach. It does **not** block YouTube ads.
After turning it on, open Prime Video and check that it plays. Undo it with `scripts/dns.sh off`. If your router
already filters DNS (Pi-hole or AdGuard Home), skip this step, because Private DNS goes around the router.

**5. Reboot and verify.**
```sh
scripts/reboot-check.sh
```
This syncs and waits 15 s so Android writes the package state, then reboots and lists any package that came back
on.

**6. Turn off the tracking toggles in the menus.** These sync with your Amazon account, so set them on the TV:
- Settings → Preferences → Privacy Settings: **Device Usage Data, Collect App Usage Data, Interest-based
  Ads: OFF**
- Settings → Preferences → Featured Content: **autoplay video and audio: OFF**
- Leave Developer Options → animation scales at **1.0**. At 0.5, the ☰ side menu stops opening anything on this
  firmware.

**7. Close ADB.** Port 5555 is plaintext.
- Developer Options → **ADB Debugging: OFF**, then restart the stick.
- `scripts/close-adb.sh $TV_HOST` must report the port **closed**.

## Undo

Turn ADB Debugging back on first, then run:
```sh
scripts/restore.sh        # everything, last batch first
scripts/restore.sh 03     # just one batch
scripts/dns.sh off
```
`restore.sh` re-enables exactly what each batch disabled. Anything that was already disabled before stays disabled.
To bring back a single app instead:
```sh
cleanyourtv/tools/tv.py 'pm enable com.amazon.bueller.music'
```

## Notes

- Fire OS 8 protects some Amazon packages, and `pm` refuses to disable them: `device.metrics`, `logmanager`,
  `crashmanager`, `diode`, `usagestats.proxy`, `ftvads.deeplinking` and `csapp`. They aren't in the lists. DNS
  blocking covers their traffic.
- `com.amazon.imp` is left on on purpose. Disabling it with the batch 02 packages left Home blank on one unit.
- A firmware update can re-enable packages. If it does, run `scripts/reboot-check.sh` to see which ones, then run that
  batch again.
- If something goes wrong, read [cleanyourtv/docs/TROUBLESHOOTING.md](cleanyourtv/docs/TROUBLESHOOTING.md).
- To update the vendored tools, copy `tools/` from a newer cleanyourtv checkout and update `cleanyourtv/UPSTREAM_COMMIT`.
