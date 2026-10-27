#!/usr/bin/env bash
# One-time setup on your computer: a Python venv with the pure-Python ADB client cleanyourtv uses.
# No adb binary needed. Works on macOS, Linux and Git Bash on Windows.
set -euo pipefail
TOOLS="$(cd "$(dirname "$0")/../cleanyourtv/tools" && pwd)"
# On Windows "python3" is often the Microsoft Store stub, which exists but doesn't run: try each one.
SYS_PY=""
for c in python3 python py; do
  if command -v "$c" >/dev/null 2>&1 && "$c" -c 'import sys; assert sys.version_info >= (3, 8)' >/dev/null 2>&1; then
    SYS_PY="$c"; break
  fi
done
[ -n "$SYS_PY" ] || { echo "Install Python 3.8+ first (python.org; on Windows tick 'Add python.exe to PATH')." >&2; exit 1; }
"$SYS_PY" -m venv "$TOOLS/venv"
PY="$TOOLS/venv/bin/python"
[ -x "$PY" ] || PY="$TOOLS/venv/Scripts/python.exe"
"$PY" -m pip install -q --upgrade pip
"$PY" -m pip install -q -r "$TOOLS/requirements.txt"
echo "Ready. Next: TV_HOST=<stick ip> scripts/backup.sh"
