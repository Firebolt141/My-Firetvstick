#!/usr/bin/env bash
# One-time setup on your computer: a Python venv with the pure-Python ADB client cleanyourtv uses.
# No adb binary needed. Works on macOS, Linux and Git Bash on Windows.
set -euo pipefail
TOOLS="$(cd "$(dirname "$0")/../cleanyourtv/tools" && pwd)"
SYS_PY="$(command -v python3 || command -v python || true)"
[ -n "$SYS_PY" ] || { echo "Install Python 3 first (python.org)." >&2; exit 1; }
"$SYS_PY" -m venv "$TOOLS/venv"
PY="$TOOLS/venv/bin/python"
[ -x "$PY" ] || PY="$TOOLS/venv/Scripts/python.exe"
"$PY" -m pip install -q --upgrade pip
"$PY" -m pip install -q -r "$TOOLS/requirements.txt"
echo "Ready. Next: TV_HOST=<stick ip> scripts/backup.sh"
