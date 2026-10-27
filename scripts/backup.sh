#!/usr/bin/env bash
# Save the stick's state before changing anything: packages, disabled packages, getprop, all three
# settings namespaces and the current Home. Also confirms the connection and the model.
# Usage: TV_HOST=192.168.1.50 scripts/backup.sh
set -euo pipefail
source "$(dirname "$0")/common.sh"
echo "Connecting to $TV_HOST. Accept the \"Allow USB debugging?\" prompt on the TV if it appears."
tv --probe
MODEL="$(tv 'getprop ro.product.model' | tr -d '\r')"
case "$MODEL" in
  AFTKM|AFTKRT|AFTMA08C15) ;;
  *) echo "Warning: model is '$MODEL', not a Fire TV Stick 4K / 4K Max 2023 (AFTKM / AFTKRT)." >&2
     echo "These lists were written for Fire OS 8 on the karat board; check cleanyourtv's examples first." >&2 ;;
esac
DIR="$ROOT/backup/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$DIR"
tv 'pm list packages -f'            > "$DIR/packages.txt"
tv 'pm list packages -d --user 0'   > "$DIR/disabled.txt"
tv 'getprop'                        > "$DIR/getprop.txt"
tv 'settings list global'           > "$DIR/settings-global.txt"
tv 'settings list secure'           > "$DIR/settings-secure.txt"
tv 'settings list system'           > "$DIR/settings-system.txt"
tv 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME' > "$DIR/home.txt"
echo "Model: $MODEL, $(sed -n 's/^\[ro.build.version.name\]: \[\(.*\)\].*/\1/p' "$DIR/getprop.txt" | head -n 1)"
echo "Backup saved in $DIR ($(wc -l < "$DIR/packages.txt") packages, $(grep -c . "$DIR/disabled.txt" || true) already disabled)."
echo "Next: TV_HOST=$TV_HOST scripts/debloat.sh 01"
