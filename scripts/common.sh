# shellcheck shell=bash
# Sourced by the other scripts. Sets ROOT, TOOLS, PY, defines tv() and batches(), and checks TV_HOST.
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOOLS="$ROOT/cleanyourtv/tools"
PY="$TOOLS/venv/bin/python"
[ -x "$PY" ] || PY="$TOOLS/venv/Scripts/python.exe"
[ -x "$PY" ] || { echo "Run scripts/setup.sh first (creates the Python venv with the ADB client)." >&2; exit 1; }
: "${TV_HOST:?Set TV_HOST to the IP of the stick, e.g. TV_HOST=192.168.1.50 $0}"
export TV_HOST
# Fire OS Home; restore.sh hands Home back to it.
export STOCK_LAUNCHER=com.amazon.tv.launcher
IFS=$'\n'  # paths with spaces survive the for-loops in the scripts
tv() { "$PY" -I "$TOOLS/tv.py" "$@"; }
# Batch files in order. No argument or "all" selects every batch; otherwise numbers like "01 03" (or "1 3").
# Exits on a number that matches no batch, so a typo can't silently do nothing.
batches() {
  if [ $# -eq 0 ] || [ "$1" = all ]; then
    ls "$ROOT"/debloat/[0-9][0-9]-*.txt
    return
  fi
  local n f
  for n in "$@"; do
    f=""
    if [[ "$n" =~ ^[0-9]{1,2}$ ]]; then
      f="$(ls "$ROOT"/debloat/"$(printf '%02d' "$((10#$n))")"-*.txt 2>/dev/null)" || f=""
    fi
    [ -n "$f" ] || { echo "no batch '$n' in debloat/ (have: $(cd "$ROOT/debloat" && ls | cut -c1-2 | tr '\n' ' '))" >&2; return 1; }
    echo "$f"
  done
}
# Newest backup folder, or nothing.
latest_backup() { ls -d "$ROOT"/backup/*/ 2>/dev/null | sort | tail -n 1; }
