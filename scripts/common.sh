# Sourced by the other scripts. Sets ROOT, TOOLS, PY and checks TV_HOST.
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOOLS="$ROOT/cleanyourtv/tools"
PY="$TOOLS/venv/bin/python"
[ -x "$PY" ] || PY="$TOOLS/venv/Scripts/python.exe"
[ -x "$PY" ] || { echo "Run scripts/setup.sh first (creates the Python venv with the ADB client)." >&2; exit 1; }
: "${TV_HOST:?Set TV_HOST to the IP of the stick, e.g. TV_HOST=192.168.1.50 $0}"
export TV_HOST
STOCK_LAUNCHER=com.amazon.tv.launcher
IFS=$'\n'  # paths with spaces survive the for-loops below
tv() { "$PY" -I "$TOOLS/tv.py" "$@"; }
# Batch files in order; "all" or a list of numbers like "01 03" selects them.
batches() {
  if [ $# -eq 0 ] || [ "$1" = all ]; then
    ls "$ROOT"/debloat/[0-9][0-9]-*.txt
  else
    for n in "$@"; do ls "$ROOT"/debloat/"$n"-*.txt; done
  fi
}
