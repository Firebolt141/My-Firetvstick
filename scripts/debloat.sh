#!/usr/bin/env bash
# Disable one or more batches from debloat/ using cleanyourtv's disable.sh (safety checks + undo record),
# then take a screenshot into shots/. Run one batch at a time and check the TV between batches.
# Usage: TV_HOST=192.168.1.50 scripts/debloat.sh 01        (or: 01 02, or: all)
set -euo pipefail
source "$(dirname "$0")/common.sh"
ls -d "$ROOT"/backup/*/ >/dev/null 2>&1 || { echo "No backup yet. Run scripts/backup.sh first." >&2; exit 1; }
mkdir -p "$ROOT/shots"
for list in $(batches "$@"); do
  name="$(basename "$list" .txt)"
  echo "=== $name"
  if [ "${name%%-*}" = 02 ]; then
    # Live tab (com.amazon.tv.livetv) answers the channel intent; on a stick there is no HDMI/tuner behind it.
    HANDLERS_ALLOW=1 "$TOOLS/disable.sh" "$list"
  else
    "$TOOLS/disable.sh" "$list"
  fi
  sleep 3
  tv --screenshot "$ROOT/shots/after-$name.png" HOME --wait 3 || \
    echo "(screenshot failed or came back empty; look at the TV itself)" >&2
  echo "Check the TV now: Home draws, the remote works, Prime Video / your apps open."
  echo "If something broke: TV_HOST=$TV_HOST scripts/restore.sh ${name%%-*}"
done
