#!/usr/bin/env bash
# Last step. Port 5555 is plaintext: once you're done, switch it off on the stick, then run this to confirm.
#   Settings > My Fire TV > Developer Options > ADB Debugging: OFF   (then restart the stick)
# Usage: scripts/close-adb.sh <stick ip>
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
python3 -I "$ROOT/cleanyourtv/tools/adb-closed.py" "${1:-${TV_HOST:?usage: $0 <stick ip>}}"
