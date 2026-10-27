#!/usr/bin/env bash
# One-time setup on your computer: a Python venv with the pure-Python ADB client cleanyourtv uses.
# No adb binary needed. Run from anywhere.
set -euo pipefail
TOOLS="$(cd "$(dirname "$0")/../cleanyourtv/tools" && pwd)"
python3 -m venv "$TOOLS/venv"
"$TOOLS/venv/bin/pip" install -q -r "$TOOLS/requirements.txt"
echo "Ready. Next: TV_HOST=<stick ip> scripts/backup.sh"
