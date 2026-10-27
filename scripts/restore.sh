#!/usr/bin/env bash
# Undo batches (re-enables exactly what each batch disabled, from the record disable.sh saved in
# ~/.cleanyourtv/). With no argument, undoes everything, last batch first.
# Usage: TV_HOST=192.168.1.50 scripts/restore.sh            (all)
#        TV_HOST=192.168.1.50 scripts/restore.sh 02         (one batch)
set -euo pipefail
source "$(dirname "$0")/common.sh"
for list in $(batches "$@" | sort -r); do
  echo "=== restoring $(basename "$list" .txt)"
  "$TOOLS/restore.sh" "$list" "$STOCK_LAUNCHER"
done
echo "Reboot the stick to finish: TV_HOST=$TV_HOST scripts/reboot-check.sh"
