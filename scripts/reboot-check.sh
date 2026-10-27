#!/usr/bin/env bash
# Flush package state to disk, reboot, wait for the stick, then report any listed package that came back.
# (Android writes package state ~10 s after a change; a reboot straight after a batch can lose it.)
# Usage: TV_HOST=192.168.1.50 scripts/reboot-check.sh
set -euo pipefail
source "$(dirname "$0")/common.sh"
tv sync
sleep 15
tv --reboot
echo "Rebooting. Waiting for the stick to come back (up to ~3 minutes)..."
sleep 30
booted=""
for _ in $(seq 1 30); do
  if tv 'getprop sys.boot_completed' 2>/dev/null | grep -q 1; then booted=1; break; fi
  sleep 5
done
[ -n "$booted" ] || { echo "The stick did not answer after the reboot. Check it is on and ADB Debugging is still ON, then run this again." >&2; exit 1; }
# One query for every enabled package, instead of one connection per listed package.
ENABLED="$(tv 'pm list packages -e --user 0' | tr -d '\r' | sed -n 's/^package://p')"
[ -n "$ENABLED" ] || { echo "Could not read the package list from the stick." >&2; exit 1; }
back=0
for list in $(batches all); do
  for p in $(grep -vE '^[[:space:]]*(#|$)' "$list" | awk '{print $1}'); do
    if printf '%s\n' "$ENABLED" | grep -qxF "$p"; then
      echo "enabled again after reboot: $p ($(basename "$list"))"; back=1
    fi
  done
done
[ $back = 0 ] && echo "All listed packages are still disabled."
tv 'dumpsys activity activities' | grep -m1 -E 'mResumedActivity|topResumedActivity' || true
