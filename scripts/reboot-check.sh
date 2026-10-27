#!/usr/bin/env bash
# Flush package state to disk, reboot, wait for the stick, then report any listed package that came back.
# (Android writes package state ~10 s after a change; a reboot straight after a batch can lose it.)
# Usage: TV_HOST=192.168.1.50 scripts/reboot-check.sh
set -euo pipefail
source "$(dirname "$0")/common.sh"
tv sync
sleep 15
tv --reboot
echo "Rebooting. Waiting for the stick to come back..."
sleep 30
for i in $(seq 1 30); do
  tv 'getprop sys.boot_completed' 2>/dev/null | grep -q 1 && break
  sleep 5
done
DISABLED="$(tv 'pm list packages -d --user 0' | tr -d '\r' | sed -n 's/^package://p')"
back=0
for list in $(batches all); do
  for p in $(grep -vE '^\s*(#|$)' "$list" | awk '{print $1}'); do
    if tv "pm list packages --user 0 $p" | tr -d '\r' | grep -qx "package:$p" \
       && ! printf '%s\n' "$DISABLED" | grep -qx "$p"; then
      echo "enabled again after reboot: $p ($(basename "$list"))"; back=1
    fi
  done
done
[ $back = 0 ] && echo "All listed packages are still disabled."
tv 'dumpsys activity activities' | grep -m1 -E 'mResumedActivity|topResumedActivity' || true
