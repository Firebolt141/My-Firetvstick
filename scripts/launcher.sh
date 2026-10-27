#!/usr/bin/env bash
# Couchy Launcher as Home on Fire OS, through Home on Fire.
#
# Fire OS never lets a third-party app become Home: com.amazon.tv.launcher is protected and set-home-activity
# is ignored (cleanyourtv/docs/TROUBLESHOOTING.md, "Fire TV"). Home on Fire's accessibility service sees Amazon
# Home come up and brings your launcher over it (a ~250 ms flash on Fire OS 8). Amazon Home stays installed and
# enabled underneath; this script never disables it.
#
#   Couchy Launcher   com.conreo.couchytv              https://f-droid.org/packages/com.conreo.couchytv/
#   Firebolt Home     io.github.firebolt141.home       this repo's fork of Couchy (launcher/)
#   Home on Fire      io.github.toolicious.homeonfire  https://github.com/toolicious/home-on-fire/releases
#
# LAUNCHER picks which launcher the checks expect: couchy (default), firebolt, or a package name.
#
# Usage: TV_HOST=192.168.1.50 scripts/launcher.sh status
#        TV_HOST=192.168.1.50 scripts/launcher.sh setup [couchy.apk] [home-on-fire.apk]
#        TV_HOST=192.168.1.50 scripts/launcher.sh undo [--uninstall]
#
# status  what is installed, whether the Home redirect is wired up, and what is on screen. Exit 1 if broken.
# setup   installs/updates any APKs you pass (download them yourself from the links above), grants Home on Fire
#         its one-time permission, enables its accessibility service (keeping any others you have on), and
#         opens it so you can pick Couchy. Run it again after a Fire OS update if Home goes back to Amazon.
# undo    switches the redirect off: removes only Home on Fire's service and revokes its permission. Both apps
#         stay installed unless you pass --uninstall. The remote's Home button opens Amazon Home again.
set -euo pipefail
source "$(dirname "$0")/common.sh"
case "${LAUNCHER:-couchy}" in
  couchy) COUCHY=com.conreo.couchytv; NAME="Couchy Launcher" ;;
  firebolt) COUCHY=io.github.firebolt141.home; NAME="Firebolt Home" ;;
  *) COUCHY="$LAUNCHER"; NAME="$LAUNCHER" ;;
esac
printf '%s' "$COUCHY" | grep -qE '^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$' || { echo "LAUNCHER is not a package name: $COUCHY" >&2; exit 2; }
HOF=io.github.toolicious.homeonfire
SERVICE="$HOF/.HijackService"
SERVICE_FULL="$HOF/$HOF.HijackService"
PERM=android.permission.WRITE_SECURE_SETTINGS

version() { tv "dumpsys package $1" | tr -d '\r' | sed -n 's/^ *versionName=//p' | head -n 1; }
installed() { tv "pm list packages --user 0 $1" | tr -d '\r' | grep -qx "package:$1"; }
# The value goes back into a shell command on the TV, so refuse anything but plain component names.
services() {
  local v
  v="$(tv 'settings get secure enabled_accessibility_services' | tr -d '\r' | sed 's/^null$//')"
  [ -z "$v" ] || printf '%s\n' "$v" | grep -qE '^[A-Za-z0-9._/:]+$' \
    || { echo "unexpected enabled_accessibility_services value, not touching it: $v" >&2; exit 1; }
  printf '%s\n' "$v"
}
has_service() { printf '%s\n' "$1" | tr ':' '\n' | grep -qxF -e "$SERVICE" -e "$SERVICE_FULL"; }
granted() { tv "dumpsys package $HOF" | tr -d '\r' | grep -q "$PERM: granted=true"; }
foreground() {
  tv 'dumpsys activity activities' | tr -d '\r' \
    | sed -n -E 's/.*(mResumedActivity|topResumedActivity).*\{[^ ]+ [^ ]+ ([^ }]+).*/\2/p' | head -n 1
}

# Pushes an APK with an md5 check, then installs it with pm (adb install can hang on a confirmation screen).
install_apk() {
  local apk="$1" remote=/data/local/tmp/cyt-install.apk out
  [ -r "$apk" ] || { echo "cannot read $apk" >&2; exit 1; }
  echo "Installing $(basename "$apk")..."
  "$PY" -I "$TOOLS/b64push.py" "$apk" "$remote" >/dev/null
  out="$(tv "pm install -r $remote" | tr -d '\r')"
  tv "rm -f $remote" >/dev/null || true
  echo "$out"
  if printf '%s' "$out" | grep -q INSTALL_FAILED_UPDATE_INCOMPATIBLE; then
    echo "This APK is signed by someone else than the copy on the stick (F-Droid, IzzyOnDroid and GitHub builds" >&2
    echo "each have their own key). Get the update from the same place you got the installed one." >&2
  fi
  printf '%s' "$out" | grep -q '^Success' || exit 1
}

status() {
  local ok=0 svc cv hv
  cv="$(installed $COUCHY && version $COUCHY || true)"
  hv="$(installed $HOF && version $HOF || true)"
  printf '%-17s%s\n' "$NAME:" "${cv:-NOT INSTALLED}"
  echo "Home on Fire:    ${hv:-NOT INSTALLED}"
  [ -n "$cv" ] && [ -n "$hv" ] || ok=1
  if [ -n "$hv" ]; then
    if granted; then echo "Permission:      WRITE_SECURE_SETTINGS granted"
    else echo "Permission:      WRITE_SECURE_SETTINGS missing (setup fixes this)"; ok=1; fi
    svc="$(services)"
    if has_service "$svc"; then echo "Redirect:        accessibility service enabled"
    else echo "Redirect:        accessibility service OFF (setup fixes this)"; ok=1; fi
    if tv "dumpsys activity services $HOF" | tr -d '\r' | grep -q HijackService; then echo "Service:         running"
    else echo "Service:         not running (open Home on Fire once, or reboot)"; ok=1; fi
  fi
  echo "Amazon Home:     $(installed com.amazon.tv.launcher && echo 'installed (stays, it sits underneath)' || echo MISSING)"
  echo "On screen now:   $(foreground)"
  return $ok
}

setup() {
  local apk svc
  for apk in "$@"; do install_apk "$apk"; done
  installed $COUCHY || { echo "$NAME is not installed. Pass its APK: scripts/launcher.sh setup launcher.apk" >&2; exit 1; }
  installed $HOF || { echo "Home on Fire is not installed. Pass its APK: scripts/launcher.sh setup home-on-fire.apk" >&2; exit 1; }
  tv "pm grant $HOF $PERM"
  # Append, never overwrite: other accessibility services (VoiceView, button mappers) stay as they are.
  svc="$(services)"
  if ! has_service "$svc"; then
    tv "settings put secure enabled_accessibility_services ${svc:+$svc:}$SERVICE"
  fi
  tv 'settings put secure accessibility_enabled 1'
  tv "am start -n $HOF/.MainActivity" >/dev/null
  echo
  echo "Home on Fire is open on the TV. If its target isn't $NAME yet: Choose target app... > $NAME."
  echo "Leave 'Launch on boot' on so the stick wakes up in $NAME."
  if [ -t 0 ]; then
    echo
    echo "Now press the real Home button on the remote (an injected key press proves nothing here), then Enter."
    read -r _
    case "$(foreground)" in
      "$COUCHY"/*) echo "$NAME is on screen. Done." ;;
      *) echo "On screen: $(foreground). Not $NAME: check the target in Home on Fire and press Home again." >&2; exit 1 ;;
    esac
  fi
}

undo() {
  local svc kept
  svc="$(services)"
  kept="$(printf '%s\n' "$svc" | tr ':' '\n' | grep -vxF -e "$SERVICE" -e "$SERVICE_FULL" | paste -sd: - || true)"
  if [ -n "$kept" ]; then
    tv "settings put secure enabled_accessibility_services $kept"
  else
    tv 'settings delete secure enabled_accessibility_services'
  fi
  if installed $HOF; then tv "pm revoke $HOF $PERM" || true; fi
  if [ "${1:-}" = --uninstall ]; then
    for p in $HOF $COUCHY; do installed $p && tv "pm uninstall $p"; done
  fi
  echo "Home on the remote opens Amazon Home again."
}

cmd="${1:-status}"; [ $# -gt 0 ] && shift
case "$cmd" in
  status) status ;;
  setup) setup "$@"; echo; status ;;
  undo) undo "$@" ;;
  *) echo "usage: $0 status | setup [apk ...] | undo [--uninstall]" >&2; exit 2 ;;
esac
