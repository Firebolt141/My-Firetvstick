#!/usr/bin/env bash
# Offline sanity check of debloat/*.txt; no stick needed. Run before editing a list goes to the TV, and in CI.
# Fails on: a line that is not a package name, a package in two batches, a core Android package
# (same rule as cleanyourtv's disable.sh), a package Fire OS 8 protects, or one this setup must keep.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CORE_RE='^(android|com\.android\.systemui|com\.android\.shell|com\.android\.phone|com\.android\.settings|com\.android\.tv\.settings|com\.(google\.)?android\.(permissioncontroller|packageinstaller|ext\.services|ext\.shared|modulemetadata)|com\.google\.android\.overlay\.modules.*|com\.android\.providers\..*)$'
# pm refuses these on Fire OS 8 ("Cannot disable a protected package").
PROTECTED='com.amazon.device.metrics com.amazon.device.logmanager com.amazon.device.crashmanager com.amazon.diode
com.fireos.usagestats.proxy com.amazon.ftvads.deeplinking com.amazon.tv.csapp'
# Home, settings, Appstore, keyboard, sign-in, OTA, Prime Video, Alexa voice, casting. imp: Home goes black without it.
# Couchy + Home on Fire (scripts/launcher.sh), and the quick-settings panel Home on Fire looks for.
KEEP='com.amazon.tv.launcher com.amazon.firehomestarter com.amazon.tv.settings.v2 com.amazon.venezia com.amazon.tigris
com.amazon.tv.ime com.amazon.imp com.amazon.tv.ottssocompanionapp com.amazon.tv.oobe com.amazon.device.software.ota
com.amazon.device.software.ota.override com.amazon.tv.forcedotaupdater.v2 com.amazon.firebat com.amazon.tv.support
com.amazon.vizzini com.amazon.aria com.amazon.cardinal com.amazon.tcomm com.amazon.alexa.datastore
com.amazon.cast.sink com.amazon.dialservice com.amazon.awvflingreceiver com.amazon.whisperplay com.amazon.tv.matter
com.amazon.ftv.screensaver com.amazon.logan
com.conreo.couchytv io.github.toolicious.homeonfire com.amazon.tv.quicksettings.ui'
fail=0
all=""
for list in "$ROOT"/debloat/[0-9][0-9]-*.txt; do
  name="$(basename "$list")"
  # Strip a BOM and CRLF the way disable.sh does, then comments.
  pkgs="$(LC_ALL=C awk 'NR==1{sub(/^\357\273\277/,"")} {sub(/\r$/,""); print}' "$list" \
    | grep -vE '^[[:space:]]*(#|$)' | sed -E 's/[[:space:]]+#.*$//' | awk '{print $1}' || true)"
  [ -n "$pkgs" ] || { echo "$name: no packages"; fail=1; continue; }
  while IFS= read -r p; do
    if ! printf '%s' "$p" | grep -qE '^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$'; then echo "$name: not a package name: $p"; fail=1; fi
    if printf '%s' "$p" | grep -qE "$CORE_RE"; then echo "$name: core Android package: $p"; fail=1; fi
    for x in $PROTECTED; do [ "$p" = "$x" ] && { echo "$name: protected by Fire OS, pm refuses it: $p"; fail=1; }; done
    for x in $KEEP; do [ "$p" = "$x" ] && { echo "$name: must stay enabled: $p"; fail=1; }; done
    all="$all$p $name"$'\n'
  done <<< "$pkgs"
done
dups="$(printf '%s' "$all" | awk '{print $1}' | sort | uniq -d)"
for d in $dups; do echo "in more than one batch: $d ($(printf '%s' "$all" | awk -v p="$d" '$1==p{print $2}' | tr '\n' ' '))"; fail=1; done
total="$(printf '%s' "$all" | grep -c . || true)"
[ $fail = 0 ] && echo "OK: $total packages in $(ls "$ROOT"/debloat/[0-9][0-9]-*.txt | wc -l | tr -d ' ') batches."
exit $fail
